/**
 * api-service.js
 * * Provides an interface for communicating with the backend API.
 * This file is designed to be fully browser-compatible and avoids
 * any reliance on Node.js global objects like 'process'.
 * * Features: Authentication, Caching, Retries with Exponential Backoff,
 * Request Interceptors, Offline Queueing, and CSRF support.
 */
class ApiService {
    // =========================================================================
    // CONFIGURATION
    // =========================================================================
    static #config = {
        // NOTE: Replace this with your actual API base URL
        BASE_URL: 'https://api.example.com/v1',
        ENDPOINTS: {
            AUTH: {
                LOGIN: '/auth/login',
                REGISTER: '/auth/register',
                ME: '/auth/me',
                LOGOUT: '/auth/logout',
                REFRESH: '/auth/refresh'
            },
            PRODUCTS: {
                BASE: '/products',
                BY_ID: (id) => `/products/${encodeURIComponent(id)}`,
                SEARCH: (query) => `/products?search=${encodeURIComponent(query)}`
            },
            CART: {
                BASE: '/cart',
                ITEM: (id) => `/cart/${encodeURIComponent(id)}`,
                CHECKOUT: '/cart/checkout'
            }
        },
        DEFAULT_HEADERS: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        },
        TIMEOUT: 10000, // 10 seconds
        MAX_RETRIES: 2,
        CACHE_TTL: 5 * 60 * 1000, // 5 minutes
        DEBOUNCE_DELAY: 300 // ms
    };

    // Cache for storing API responses with TTL
    static #cache = new Map();
    static #pendingRequests = new Map();
    static #requestQueue = [];
    static #isProcessingQueue = false;
    static #refreshTokenPromise = null;

    // Request interceptors
    static #requestInterceptors = [];
    // Response interceptors
    static #responseInterceptors = [];

    // Service worker registration
    static #serviceWorkerRegistration = null;

    /**
     * Get authentication tokens from storage
     * @private
     * @returns {{accessToken: string, refreshToken: string}|null} Tokens object or null if not found
     */
    static #getAuthTokens() {
        try {
            const token = localStorage.getItem('token');
            const refreshToken = localStorage.getItem('refreshToken');
            return token && refreshToken ? { accessToken: token, refreshToken } : null;
        } catch (error) {
            console.error('Error reading auth tokens:', error);
            return null;
        }
    }

    /**
     * Set authentication tokens in storage
     * @private
     * @param {string} accessToken - JWT access token
     * @param {string} refreshToken - JWT refresh token
     */
    static #setAuthTokens(accessToken, refreshToken) {
        try {
            if (accessToken) localStorage.setItem('token', accessToken);
            if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
            this.#cache.clear(); // Clear cache on auth state change
        } catch (error) {
            console.error('Error storing auth tokens:', error);
            this.clearAuth();
        }
    }

    /**
     * Get headers with authentication and CSRF token
     * @private
     * @param {Object} [customHeaders={}] - Additional headers to include
     * @returns {Promise<Object>} Headers object with auth token if available
     */
    static async #getHeaders(customHeaders = {}) {
        const tokens = this.#getAuthTokens();
        const headers = { ...this.#config.DEFAULT_HEADERS };

        // Add auth token if available
        if (tokens?.accessToken) {
            headers['Authorization'] = `Bearer ${tokens.accessToken}`;
        }

        // Add CSRF token if available
        const csrfToken = this.#getCSRFToken();
        if (csrfToken) {
            headers['X-CSRF-Token'] = csrfToken;
        }

        // Apply request interceptors
        for (const interceptor of this.#requestInterceptors) {
            await interceptor(headers);
        }

        return { ...headers, ...customHeaders };
    }

    /**
     * Get CSRF token from meta tag or cookie
     * @private
     * @returns {string|null} CSRF token or null if not found
     */
    static #getCSRFToken() {
        // Try to get from meta tag first
        const metaTag = document.querySelector('meta[name="csrf-token"]');
        if (metaTag) return metaTag.getAttribute('content');

        // Fallback to cookie
        const cookieMatch = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        return cookieMatch ? decodeURIComponent(cookieMatch[1]) : null;
    }

    /**
     * Handle API response with error handling and interceptors
     * @private
     * @param {Response} response - Fetch API response
     * @param {Object} requestOptions - Original request options
     * @returns {Promise<*>} Parsed response data
     * @throws {Error} If response is not ok
     */
    static async #handleResponse(response, requestOptions = {}) {
        let data;
        const contentType = response.headers.get('content-type');

        try {
            data = contentType?.includes('application/json')
                ? await response.json()
                : await response.text();
        } catch (error) {
            console.error('Error parsing response:', error);
            data = {};
        }

        // Apply response interceptors
        for (const interceptor of this.#responseInterceptors) {
            await interceptor(response, data, requestOptions);
        }

        if (!response.ok) {
            const error = new Error(data.message || `Request failed with status ${response.status}`);
            error.status = response.status;
            error.data = data;
            error.response = response;

            // Handle specific error statuses
            if (response.status === 401) {
                // Try to refresh token if this wasn't a refresh request
                if (requestOptions.url && !requestOptions.url.includes('auth/refresh')) {
                    try {
                        await this.#refreshToken();
                        // Retry the original request with new token
                        const retryOptions = { ...requestOptions };
                        delete retryOptions.retryCount; // Reset retry count
                        return this.#fetchWithRetry(requestOptions);
                    } catch (refreshError) {
                        this.clearAuth();
                        window.dispatchEvent(new Event('unauthorized'));
                    }
                } else {
                    this.clearAuth();
                    window.dispatchEvent(new Event('unauthorized'));
                }
            } else if (response.status === 429) {
                // Rate limiting
                const retryAfter = response.headers.get('Retry-After') || '5';
                error.message = `Too many requests. Please try again in ${retryAfter} seconds.`;
                error.retryAfter = parseInt(retryAfter, 10);
            }

            throw error;
        }

        return data;
    }

    /**
     * Make an API request with retry logic, queue management, and offline support
     * @private
     * @param {Object} options - Request options including url, method, etc.
     * @param {number} [retryCount=0] - Current retry attempt
     * @returns {Promise<*>} API response data
     */
    static async #fetchWithRetry(options, retryCount = 0) {
        const { url, method = 'GET', body, headers = {}, signal } = options;
        // Use the original options object to ensure cache key integrity
        const cacheKey = JSON.stringify({ url, method, body });
        const isGetRequest = method === 'GET';
        const requestOptions = { ...options, retryCount, url, method };

        // Check cache for GET requests
        if (isGetRequest && this.#isCacheValid(cacheKey)) {
            return this.#getFromCache(cacheKey);
        }

        // Return existing promise if request is in progress (deduplication)
        if (this.#pendingRequests.has(cacheKey)) {
            return this.#pendingRequests.get(cacheKey);
        }

        // Check if we're offline and queue the request if so
        if (!navigator.onLine && method !== 'GET') { // Don't queue GET requests if offline (use cache)
            return new Promise((resolve, reject) => {
                this.#queueRequest({ ...requestOptions, resolve, reject });
            });
        }

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), this.#config.TIMEOUT);

        // Combine abort signals if both are provided
        const abortSignal = signal
            ? this.#combineSignals(controller.signal, signal)
            : controller.signal;

        try {
            const allHeaders = await this.#getHeaders(headers);

            const requestPromise = (async () => {
                const response = await fetch(url, {
                    method,
                    headers: allHeaders,
                    body: body ? JSON.stringify(body) : undefined,
                    signal: abortSignal,
                    credentials: 'include' // For cookies
                });

                const data = await this.#handleResponse(response, requestOptions);

                // Cache successful GET responses
                if (isGetRequest) {
                    this.#setCache(cacheKey, data);
                }

                return data;
            })();

            this.#pendingRequests.set(cacheKey, requestPromise);

            // Clean up after request completes
            const result = await requestPromise;
            return result;

        } catch (error) {
            // Handle network errors and retries
            if (this.#shouldRetry(error, retryCount)) {
                const delay = this.#calculateRetryDelay(retryCount, error);
                console.log(`Retrying request in ${delay.toFixed(0)}ms... (Attempt ${retryCount + 1}/${this.#config.MAX_RETRIES})`);
                await new Promise(resolve => setTimeout(resolve, delay));
                return this.#fetchWithRetry(options, retryCount + 1);
            }

            // If we have a cached response and the request fails, return the cache (stale-while-revalidate)
            if (isGetRequest && this.#cache.has(cacheKey)) {
                const cached = this.#cache.get(cacheKey);
                if (cached) {
                    // Refresh the cache in the background
                    this.#fetchWithRetry({ ...options, headers: { ...headers, 'Cache-Control': 'no-cache' } })
                        .catch(() => {}); // Ignore errors in background refresh
                    return cached.data;
                }
            }

            throw error;

        } finally {
            clearTimeout(timeoutId);
            this.#pendingRequests.delete(cacheKey);
        }
    }

    /**
     * Check if a request should be retried
     * @private
     */
    static #shouldRetry(error, retryCount) {
        if (retryCount >= this.#config.MAX_RETRIES) return false;

        // Retry on network errors, timeout (AbortError), 5xx server errors, or 429 rate limit
        return error.name === 'AbortError' ||
               error.name === 'TypeError' || // Network error
               (error.status && error.status >= 500) ||
               error.status === 429; // Rate limited
    }

    /**
     * Calculate delay before next retry with exponential backoff and jitter
     * @private
     */
    static #calculateRetryDelay(retryCount, error) {
        // If we have a Retry-After header, use that
        if (error.retryAfter) {
            return error.retryAfter * 1000; // Convert to ms
        }

        // Otherwise use exponential backoff with jitter (max 30s)
        const baseDelay = Math.min(1000 * Math.pow(2, retryCount), 30000);
        return baseDelay * (0.5 + Math.random() * 0.5); // Add jitter (50% to 100% of base)
    }

    /**
     * Combine multiple AbortSignals
     * @private
     */
    static #combineSignals(...signals) {
        const controller = new AbortController();

        const onAbort = () => {
            controller.abort();
            signals.forEach(signal => signal && signal.removeEventListener('abort', onAbort));
        };

        signals.forEach(signal => {
            if (signal) {
                if (signal.aborted) {
                    onAbort();
                } else {
                    signal.addEventListener('abort', onAbort);
                }
            }
        });

        return controller.signal;
    }

    /**
     * Make an API request with proper error handling and logging
     * @private
     * @param {string} path - API path
     * @param {Object} [options={}] - Fetch options
     * @returns {Promise<*>} API response data
     */
    static async #request(path, options = {}) {
        const url = `${this.#config.BASE_URL}${path}`;
        const method = options.method || 'GET';
        const requestId = this.#generateRequestId();
        const startTime = performance.now();

        try {
            const response = await this.#fetchWithRetry({
                url,
                method,
                headers: options.headers,
                body: options.body,
                signal: options.signal
            });

            // Log successful request
            this.#logRequest({
                requestId,
                method,
                url,
                status: 'success',
                duration: performance.now() - startTime
            });

            return response;

        } catch (error) {
            // Log failed request
            this.#logRequest({
                requestId,
                method,
                url,
                status: 'error',
                error: error.message,
                statusCode: error.status,
                duration: performance.now() - startTime
            });

            // Re-throw standardized errors
            if (error.status === 401) {
                // Already handled in #handleResponse (re-thrown after refresh attempt)
                throw new Error('Your session has expired or is invalid.');
            } else if (error.name === 'AbortError') {
                // Thrown by fetch timeout
                throw new Error('Request timed out. Please check your connection and try again.');
            } else if (error.status === 403) {
                throw new Error('You do not have permission to perform this action.');
            } else if (error.status === 404) {
                throw new Error('The requested resource was not found.');
            } else if (error.status === 429) {
                throw new Error('Too many requests. Please try again later.');
            } else if (error.status && error.status >= 500) {
                throw new Error('A server error occurred. Please try again later.');
            } else {
                throw error; // Re-throw with original error
            }
        }
    }

    // =========================================================================
    // CACHE MANAGEMENT
    // =========================================================================

    /**
     * Check if a cached item is still valid
     * @private
     */
    static #isCacheValid(cacheKey) {
        if (!this.#cache.has(cacheKey)) return false;

        const { timestamp } = this.#cache.get(cacheKey);
        return Date.now() - timestamp < this.#config.CACHE_TTL;
    }

    /**
     * Get item from cache
     * @private
     */
    static #getFromCache(cacheKey) {
        const cached = this.#cache.get(cacheKey);
        return cached?.data;
    }

    /**
     * Set item in cache with timestamp
     * @private
     */
    static #setCache(cacheKey, data) {
        this.#cache.set(cacheKey, {
            data,
            timestamp: Date.now()
        });
    }

    /**
     * Clear all cached data.
     */
    static clearCache() {
        this.#cache.clear();
        console.log('API cache cleared.');
    }

    // =========================================================================
    // REQUEST QUEUE (Offline Support)
    // =========================================================================

    /**
     * Add request to queue for later processing
     * @private
     */
    static #queueRequest(request) {
        this.#requestQueue.push(request);
        console.warn(`Device is offline. Request queued: ${request.method} ${request.url}. Queue size: ${this.#requestQueue.length}`);
        // Optionally listen for 'online' event to trigger processQueue immediately
        if (!this.#isProcessingQueue) {
            window.addEventListener('online', this.#processQueue, { once: true });
        }
    }

    /**
     * Process queued requests when back online
     * @private
     */
    static async #processQueue() {
        if (this.#isProcessingQueue || this.#requestQueue.length === 0) return;
        if (!navigator.onLine) {
            window.addEventListener('online', ApiService.#processQueue, { once: true });
            return;
        }

        console.log(`Device back online. Processing ${this.#requestQueue.length} queued requests...`);
        this.#isProcessingQueue = true;

        try {
            while (this.#requestQueue.length > 0) {
                const request = this.#requestQueue.shift();
                try {
                    const result = await ApiService.#fetchWithRetry({
                        url: request.url,
                        method: request.method,
                        body: request.body,
                        headers: request.headers
                    });
                    request.resolve(result);
                } catch (error) {
                    console.error('Failed to process queued request:', request.url, error);
                    request.reject(error);
                }
            }
            console.log('All queued requests processed successfully.');
        } finally {
            this.#isProcessingQueue = false;
        }
    }

    // =========================================================================
    // INTERCEPTOR MANAGEMENT
    // =========================================================================

    /**
     * Register a function to intercept outgoing requests before they are sent.
     * The interceptor receives the headers object and can modify it (e.g., add new tokens).
     * @param {Function} interceptor - async (headers) => { ... }
     */
    static registerRequestInterceptor(interceptor) {
        if (typeof interceptor === 'function') {
            this.#requestInterceptors.push(interceptor);
        }
    }

    /**
     * Register a function to intercept incoming responses.
     * The interceptor receives the raw Response object and the parsed data.
     * @param {Function} interceptor - async (response, data, requestOptions) => { ... }
     */
    static registerResponseInterceptor(interceptor) {
        if (typeof interceptor === 'function') {
            this.#responseInterceptors.push(interceptor);
        }
    }

    // =========================================================================
    // PUBLIC UTILITY METHODS (General Fetch Wrappers)
    // =========================================================================

    /**
     * General GET request.
     * @param {string} path - API path (e.g., '/users/123')
     * @param {Object} [options={}] - Request options (headers, signal, etc.)
     * @returns {Promise<*>} Response data
     */
    static get(path, options = {}) {
        return this.#request(path, { ...options, method: 'GET' });
    }

    /**
     * General POST request.
     * @param {string} path - API path
     * @param {Object} [body] - Request body object
     * @param {Object} [options={}] - Request options (headers, signal, etc.)
     * @returns {Promise<*>} Response data
     */
    static post(path, body, options = {}) {
        return this.#request(path, { ...options, method: 'POST', body });
    }

    /**
     * General PUT request.
     * @param {string} path - API path
     * @param {Object} [body] - Request body object
     * @param {Object} [options={}] - Request options (headers, signal, etc.)
     * @returns {Promise<*>} Response data
     */
    static put(path, body, options = {}) {
        return this.#request(path, { ...options, method: 'PUT', body });
    }

    /**
     * General DELETE request.
     * @param {string} path - API path
     * @param {Object} [options={}] - Request options (headers, signal, etc.)
     * @returns {Promise<*>} Response data
     */
    static delete(path, options = {}) {
        return this.#request(path, { ...options, method: 'DELETE' });
    }

    // =========================================================================
    // AUTHENTICATION
    // =========================================================================

    /**
     * Login user with credentials
     * @param {string} username - Username or email
     * @param {string} password - Password
     * @param {boolean} [rememberMe=false] - Whether to remember the user
     * @returns {Promise<Object>} User data with tokens
     */
    static async login(username, password, rememberMe = false) {
        if (!username || !password) {
            throw new Error('Username and password are required');
        }

        const data = await this.#request(this.#config.ENDPOINTS.AUTH.LOGIN, {
            method: 'POST',
            body: {
                username: this.#sanitizeInput(username),
                password // Password will be hashed before sending
            },
            headers: {
                'X-Remember-Me': rememberMe.toString()
            }
        });

        if (data.accessToken && data.refreshToken) {
            this.#setAuthTokens(data.accessToken, data.refreshToken);
            localStorage.setItem('username', this.#sanitizeInput(username));

            // Broadcast login event
            window.dispatchEvent(new CustomEvent('authChange', {
                detail: { isAuthenticated: true, username }
            }));
        }

        return data;
    }

    /**
     * Register a new user
     * @param {Object} userData - User registration data
     * @param {string} userData.username - Username
     * @param {string} userData.email - Email address
     * @param {string} userData.password - Password
     * @param {string} [userData.firstName] - First name
     * @param {string} [userData.lastName] - Last name
     * @returns {Promise<Object>} Registration result
     */
    static async register(userData) {
        // Validate required fields
        const requiredFields = ['username', 'email', 'password'];
        const missingFields = requiredFields.filter(field => !userData[field]);

        if (missingFields.length > 0) {
            throw new Error(`Missing required fields: ${missingFields.join(', ')}`);
        }

        // Sanitize input
        const sanitizedData = {
            ...userData,
            username: this.#sanitizeInput(userData.username),
            email: this.#sanitizeInput(userData.email),
            firstName: userData.firstName ? this.#sanitizeInput(userData.firstName) : undefined,
            lastName: userData.lastName ? this.#sanitizeInput(userData.lastName) : undefined
        };

        return this.#request(this.#config.ENDPOINTS.AUTH.REGISTER, {
            method: 'POST',
            body: sanitizedData
        });
    }

    /**
     * Refresh authentication token
     * @private
     */
    static async #refreshToken() {
        if (this.#refreshTokenPromise) {
            return this.#refreshTokenPromise;
        }

        const tokens = this.#getAuthTokens();
        if (!tokens?.refreshToken) {
            throw new Error('No refresh token available');
        }

        try {
            this.#refreshTokenPromise = this.#request(this.#config.ENDPOINTS.AUTH.REFRESH, {
                method: 'POST',
                // Explicitly bypasses the automatic Authorization header since the access token is likely invalid
                headers: { 'Authorization': undefined },
                body: { refreshToken: tokens.refreshToken }
            });

            const data = await this.#refreshTokenPromise;

            if (data.accessToken) {
                this.#setAuthTokens(data.accessToken, data.refreshToken || tokens.refreshToken);
                return data.accessToken;
            }

            throw new Error('Invalid token refresh response');

        } catch (error) {
            this.clearAuth();
            throw error;
        } finally {
            this.#refreshTokenPromise = null;
        }
    }

    /**
     * Get current authenticated user's profile
     * @param {boolean} [forceRefresh=false] - Whether to force a fresh request
     * @returns {Promise<Object>} User profile data
     */
    static async getCurrentUser(forceRefresh = false) {
        const cacheKey = 'currentUser';

        if (!forceRefresh && this.#isCacheValid(cacheKey)) {
            return this.#getFromCache(cacheKey);
        }

        try {
            const user = await this.#request(this.#config.ENDPOINTS.AUTH.ME);
            this.#setCache(cacheKey, user);
            return user;
        } catch (error) {
            if (error.status === 401) {
                this.clearAuth();
            }
            throw error;
        }
    }

    /**
     * Logout the current user
     * @param {boolean} [everywhere=false] - Whether to revoke all sessions
     * @returns {Promise<void>}
     */
    static async logout(everywhere = false) {
        try {
            await this.#request(this.#config.ENDPOINTS.AUTH.LOGOUT, {
                method: 'POST',
                body: { everywhere }
            });
        } catch (error) {
            // Log error but proceed to clear auth state client-side regardless
            console.error('Error during logout, clearing client session anyway:', error);
        } finally {
            this.clearAuth();
        }
    }

    /**
     * Clear authentication state
     */
    static clearAuth() {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('username');
        this.#cache.clear();

        // Broadcast logout event
        window.dispatchEvent(new Event('authChange'));
    }

    /**
     * Check if user is authenticated (only checks for token existence, not validity)
     * @returns {boolean} True if user has a token
     */
    static isAuthenticated() {
        return !!this.#getAuthTokens()?.accessToken;
    }

    // =========================================================================
    // PRODUCTS
    // =========================================================================

    /**
     * Get all products with optional search and pagination
     * @param {Object} [options] - Query options
     * @param {string} [options.search=''] - Search term
     * @param {number} [options.page=1] - Page number (1-based)
     * @param {number} [options.limit=20] - Items per page
     * @param {string} [options.sort='name'] - Field to sort by
     * @param {string} [options.order='asc'] - Sort order (asc/desc)
     * @param {string|string[]} [options.category] - Filter by category
     * @returns {Promise<{products: Array, total: number, page: number, pages: number}>} Paginated products
     */
    static async getProducts({
        search = '',
        page = 1,
        limit = 20,
        sort = 'name',
        order = 'asc',
        category
    } = {}) {
        const params = new URLSearchParams();

        if (search) params.append('search', search);
        if (page > 1) params.append('page', page);
        if (limit !== 20) params.append('limit', limit);
        if (sort !== 'name') params.append('sort', sort);
        if (order !== 'asc') params.append('order', order);
        if (category) {
            const categories = Array.isArray(category) ? category : [category];
            categories.forEach(cat => params.append('category', cat));
        }

        const queryString = params.toString();
        const endpoint = queryString
            ? `${this.#config.ENDPOINTS.PRODUCTS.BASE}?${queryString}`
            : this.#config.ENDPOINTS.PRODUCTS.BASE;

        return this.#request(endpoint);
    }

    /**
     * Get product by ID
     * @param {string} productId - Product ID
     * @param {boolean} [forceRefresh=false] - Whether to bypass cache
     * @returns {Promise<Object>} Product details
     */
    static async getProductById(productId, forceRefresh = false) {
        if (!productId) {
            throw new Error('Product ID is required');
        }

        const cacheKey = `product_${productId}`;

        if (!forceRefresh && this.#isCacheValid(cacheKey)) {
            return this.#getFromCache(cacheKey);
        }

        const product = await this.#request(this.#config.ENDPOINTS.PRODUCTS.BY_ID(productId));
        this.#setCache(cacheKey, product);
        return product;
    }

    /**
     * Get multiple products by their IDs
     * @param {string[]} productIds - Array of product IDs
     * @returns {Promise<Object[]>} Array of product details
     */
    static async getProductsByIds(productIds) {
        if (!Array.isArray(productIds) || productIds.length === 0) {
            return [];
        }

        // Get cached products first
        const cachedProducts = [];
        const uncachedIds = [];

        productIds.forEach(id => {
            const cacheKey = `product_${id}`;
            if (this.#isCacheValid(cacheKey)) {
                cachedProducts.push(this.#getFromCache(cacheKey));
            } else {
                uncachedIds.push(id);
            }
        });

        // Fetch uncached products in parallel
        if (uncachedIds.length > 0) {
            const uncachedProducts = await Promise.all(
                // Recursive call to leverage caching logic in getProductById
                uncachedIds.map(id => this.getProductById(id, true))
            );
            cachedProducts.push(...uncachedProducts);
        }

        // Return in the original order, filtering out any IDs that didn't resolve
        return productIds.map(id =>
            cachedProducts.find(p => p?.id === id)
        ).filter(Boolean);
    }

    // =========================================================================
    // CART
    // =========================================================================

    /**
     * Get current user's cart with detailed product information
     * @param {boolean} [forceRefresh=false] - Whether to bypass cache
     * @returns {Promise<Object>} Cart data with product details
     */
    static async getCart(forceRefresh = false) {
        const cacheKey = 'user_cart';

        if (!forceRefresh && this.#isCacheValid(cacheKey)) {
            return this.#getFromCache(cacheKey);
        }

        try {
            const cart = await this.#request(this.#config.ENDPOINTS.CART.BASE);

            // If cart has items, fetch full product details
            if (cart.items?.length > 0) {
                const productIds = cart.items.map(item => item.productId);
                const products = await this.getProductsByIds(productIds);

                // Merge product details with cart items
                cart.items = cart.items.map(item => ({
                    ...item,
                    product: products.find(p => p?.id === item.productId) || null
                }));
            }

            this.#setCache(cacheKey, cart);
            return cart;

        } catch (error) {
            if (error.status === 401) {
                this.clearAuth();
            }
            throw error;
        }
    }

    /**
     * Add item to cart with validation and optimistic UI updates
     * @param {string} productId - Product ID
     * @param {number} [quantity=1] - Quantity to add
     * @param {Object} [options] - Additional options
     * @param {boolean} [options.merge=true] - Whether to merge with existing quantity
     * @returns {Promise<Object>} Updated cart
     */
    static async addToCart(productId, quantity = 1, { merge = true } = {}) {
        if (!productId) {
            throw new Error('Product ID is required');
        }

        quantity = Math.max(1, parseInt(quantity, 10) || 1);

        try {
            // Note: Optimistic UI logic is simplified here; a full implementation
            // would involve storing a rollback state.

            // Check if item exists and merge is true to optimize with updateCartItem
            const previousCart = this.#getFromCache('user_cart');
            const existingItem = previousCart?.items.find(item => item.productId === productId);

            if (existingItem && merge) {
                const newQuantity = existingItem.quantity + quantity;
                return this.updateCartItem(productId, newQuantity);
            } else {
                const cart = await this.#request(this.#config.ENDPOINTS.CART.BASE, {
                    method: 'POST',
                    body: {
                        productId,
                        quantity,
                        merge
                    }
                });

                // Invalidate cart cache to force refresh with product details on next read
                this.#cache.delete('user_cart');
                return cart;
            }
        } catch (error) {
            if (error.status === 401) {
                this.clearAuth();
            } else if (error.status === 400 && error.data?.code === 'INSUFFICIENT_STOCK') {
                // Refresh product data if stock error
                this.#cache.delete(`product_${productId}`);
            }
            throw error;
        }
    }

    /**
     * Update cart item quantity with validation
     * @param {string} productId - Product ID
     * @param {number} quantity - New quantity (0 to remove)
     * @returns {Promise<Object>} Updated cart
     */
    static async updateCartItem(productId, quantity) {
        if (!productId) {
            throw new Error('Product ID is required');
        }

        quantity = parseInt(quantity, 10);

        if (isNaN(quantity) || quantity < 0) {
            throw new Error('Quantity must be a non-negative number');
        }

        // Remove item if quantity is 0
        if (quantity === 0) {
            return this.removeFromCart(productId);
        }

        try {
            // Optimistic UI logic is simplified here.

            const cart = await this.#request(this.#config.ENDPOINTS.CART.ITEM(productId), {
                method: 'PUT',
                body: { quantity }
            });

            // Invalidate cart and product caches that might be affected
            this.#cache.delete('user_cart');
            this.#cache.delete(`product_${productId}`);

            return cart;

        } catch (error) {
            if (error.status === 401) {
                this.clearAuth();
            } else if (error.status === 400 && error.data?.code === 'INSUFFICIENT_STOCK') {
                // Refresh product data if stock error
                this.#cache.delete(`product_${productId}`);
            }
            throw error;
        }
    }

    /**
     * Remove item from cart
     * @param {string} productId - Product ID to remove
     * @returns {Promise<Object>} Updated cart
     */
    static async removeFromCart(productId) {
        if (!productId) {
            throw new Error('Product ID is required');
        }

        try {
            const cart = await this.#request(this.#config.ENDPOINTS.CART.ITEM(productId), {
                method: 'DELETE'
            });

            // Invalidate caches
            this.#cache.delete('user_cart');
            this.#cache.delete(`product_${productId}`);

            return cart;

        } catch (error) {
            if (error.status === 401) {
                this.clearAuth();
            }
            throw error;
        }
    }

    /**
     * Clear user's cart
     * @returns {Promise<Object>} Empty cart
     */
    static async clearCart() {
        try {
            const cart = await this.#request(this.#config.ENDPOINTS.CART.BASE, {
                method: 'DELETE'
            });

            // Invalidate cart cache
            this.#cache.delete('user_cart');

            return cart;

        } catch (error) {
            if (error.status === 401) {
                this.clearAuth();
            }
            throw error;
        }
    }

    /**
     * Proceed to checkout
     * @param {Object} checkoutData - Checkout information
     * @param {string} checkoutData.paymentMethod - Payment method ID
     * @param {Object} checkoutData.shippingAddress - Shipping address
     * @param {Object} [checkoutData.billingAddress] - Billing address (if different)
     * @returns {Promise<Object>} Order confirmation
     */
    static async checkout(checkoutData) {
        try {
            const order = await this.#request(this.#config.ENDPOINTS.CART.CHECKOUT, {
                method: 'POST',
                body: checkoutData
            });

            // Clear cart cache after successful checkout
            this.#cache.delete('user_cart');

            return order;

        } catch (error) {
            if (error.status === 401) {
                this.clearAuth();
            }
            throw error;
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Sanitize input to prevent XSS
     * @private
     */
    static #sanitizeInput(input) {
        if (input == null) return '';

        return String(input)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#x27;')
            .replace(/\//g, '&#x2F;');
    }

    /**
     * Generate a unique request ID
     * @private
     */
    static #generateRequestId() {
        return 'req_' + Math.random().toString(36).substr(2, 9);
    }

    /**
     * Log request details for debugging
     * @private
     */
    static #logRequest({ requestId, method, url, status, error, statusCode, duration }) {
        if (process.env.NODE_ENV === 'production') return;

        const logEntry = {
            id: requestId,
            method,
            url,
            status,
            ...(error && { error }),
            ...(statusCode && { statusCode }),
            duration: duration ? `${Math.round(duration)}ms` : undefined,
            timestamp: new Date().toISOString()
        };

        if (status === 'error') {
            console.error('API Request Error:', logEntry);
        } else if (process.env.NODE_ENV === 'development') {
            console.log('API Request:', logEntry);
        }
    }

    // =========================================================================
    // INTERCEPTORS
    // =========================================================================

    /**
     * Add request interceptor
     * @param {Function} interceptor - Function that modifies the request config
     */
    static addRequestInterceptor(interceptor) {
        if (typeof interceptor === 'function') {
            this.#requestInterceptors.push(interceptor);
        }
    }

    /**
     * Add response interceptor
     * @param {Function} interceptor - Function that handles the response
     */
    static addResponseInterceptor(interceptor) {
        if (typeof interceptor === 'function') {
            this.#responseInterceptors.push(interceptor);
        }
    }

    /**
     * Remove request interceptor
     * @param {Function} interceptor - The interceptor function to remove
     */
    static removeRequestInterceptor(interceptor) {
        const index = this.#requestInterceptors.indexOf(interceptor);
        if (index !== -1) {
            this.#requestInterceptors.splice(index, 1);
        }
    }

    /**
     * Remove response interceptor
     * @param {Function} interceptor - The interceptor function to remove
     */
    static removeResponseInterceptor(interceptor) {
        const index = this.#responseInterceptors.indexOf(interceptor);
        if (index !== -1) {
            this.#responseInterceptors.splice(index, 1);
        }
    }

    // =========================================================================
    // SERVICE WORKER
    // =========================================================================

    /**
     * Register service worker for offline support
     * @param {string} [swPath='/service-worker.js'] - Path to service worker file
     * @returns {Promise<ServiceWorkerRegistration>}
     */
    static async registerServiceWorker(swPath = '/service-worker.js') {
        if (!('serviceWorker' in navigator)) {
            console.warn('Service workers are not supported in this browser');
            return null;
        }

        try {
            this.#serviceWorkerRegistration = await navigator.serviceWorker.register(swPath, {
                scope: '/',
                updateViaCache: 'all'
            });

            // Listen for updates
            this.#serviceWorkerRegistration.addEventListener('updatefound', () => {
                const newWorker = this.#serviceWorkerRegistration.installing;

                newWorker.addEventListener('statechange', () => {
                    if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                        // New update available
                        window.dispatchEvent(new Event('swUpdateAvailable'));
                    }
                });
            });

            // Check for updates periodically
            setInterval(() => {
                this.#serviceWorkerRegistration?.update().catch(console.error);
            }, 60 * 60 * 1000); // Check every hour

            return this.#serviceWorkerRegistration;

        } catch (error) {
            console.error('Service worker registration failed:', error);
            return null;
        }
    }

    /**
     * Update the service worker
     * @returns {Promise<boolean>} True if update was successful
     */
    static async updateServiceWorker() {
        if (!this.#serviceWorkerRegistration?.waiting) {
            return false;
        }

        try {
            // Notify the waiting service worker to take control
            this.#serviceWorkerRegistration.waiting.postMessage({ type: 'SKIP_WAITING' });

            // Reload the page when the new service worker takes control
            const controller = this.#serviceWorkerRegistration.active;
            if (controller) {
                controller.addEventListener('statechange', (e) => {
                    if (e.target.state === 'activated') {
                        window.location.reload();
                    }
                });
            }

            return true;

        } catch (error) {
            console.error('Failed to update service worker:', error);
            return false;
        }
    }

    // =========================================================================
    // UI HELPERS
    // =========================================================================

    /**
     * Display products in the products container with pagination and filtering
     * @param {Object} options - Display options
     * @param {string} options.containerId - ID of the container element
     * @param {string} [options.searchTerm=''] - Search term
     * @param {number} [options.page=1] - Page number
     * @param {string} [options.category] - Filter by category
     * @param {function} [options.onProductClick] - Callback when a product is clicked
     * @returns {Promise<void>}
     */
    static async displayProducts({
        containerId = 'products-container',
        searchTerm = '',
        page = 1,
        category,
        onProductClick
    } = {}) {
        const container = document.getElementById(containerId);
        if (!container) return;

        try {
            // Show loading state
            container.innerHTML = `
                <div class="loading">
                    <div class="spinner"></div>
                    <p>Loading products...</p>
                </div>
            `;

            // Fetch products with pagination and filtering
            const { products, total, pages } = await this.getProducts({
                search: searchTerm,
                page,
                category,
                limit: 12
            });

            if (!products || products.length === 0) {
                container.innerHTML = `
                    <div class="no-results">
                        <i class="icon-search"></i>
                        <h3>No products found</h3>
                        <p>Try adjusting your search or filter criteria</p>
                    </div>
                `;
                return;
            }

            // Render products
            container.innerHTML = `
                <div class="products-grid">
                    ${products.map(product => this.#renderProductCard(product, onProductClick)).join('')}
                </div>
                ${this.#renderPagination(page, pages, containerId, searchTerm, category)}
            `;

            // Initialize lazy loading for images
            this.#initLazyLoading(container);

        } catch (error) {
            console.error('Error loading products:', error);
            container.innerHTML = `
                <div class="error">
                    <i class="icon-error"></i>
                    <h3>Failed to load products</h3>
                    <p>${error.message || 'Please check your connection and try again'}</p>
                    <button class="btn btn-primary"
                            onclick="ApiService.displayProducts({ containerId: '${containerId}' })">
                        <i class="icon-refresh"></i> Try Again
                    </button>
                </div>
            `;
        }
    }

    /**
     * Render a product card
     * @private
     */
    static #renderProductCard(product, onClick) {
        const { id, name, price, imageUrl, stock, rating = 0, category } = product;
        const ratingRounded = Math.min(5, Math.max(0, Math.round(rating)));
        const isLowStock = stock > 0 && stock < 5;
        const isOutOfStock = stock <= 0;

        return `
            <article class="product-card"
                     data-id="${this.#escapeHtml(id)}"
                     data-category="${this.#escapeHtml(category || '')}"
                     tabindex="0"
                     ${onClick ? `onclick="event.preventDefault(); (${onClick.toString()})(event, ${JSON.stringify(product).replace(/"/g, '&quot;')})"` : ''}
                     role="article"
                     aria-labelledby="product-${id}-title">
                <div class="product-image">
                    <img src="${imageUrl || 'img/placeholder.jpg'}"
                         alt=""
                         loading="lazy"
                         data-src="${imageUrl || 'img/placeholder.jpg'}"
                         onerror="this.src='img/placeholder.jpg'; this.onerror=null;"
                         class="lazy">
                    ${isLowStock ? `
                        <span class="stock-badge" aria-label="Low stock">
                            ${stock} left
                        </span>
                    ` : ''}
                    ${isOutOfStock ? `
                        <span class="out-of-stock-badge" aria-label="Out of stock">
                            Sold out
                        </span>
                    ` : ''}
                </div>
                <div class="product-info">
                    <h3 id="product-${id}-title" class="product-title">
                        ${this.#escapeHtml(name)}
                    </h3>
                    <div class="product-meta">
                        <span class="price">$${Number(price).toFixed(2)}</span>
                        <div class="rating" aria-label="Rating: ${ratingRounded} out of 5">
                            ${'<span class="star filled" aria-hidden="true">★</span>'.repeat(ratingRounded)}
                            ${'<span class="star" aria-hidden="true">☆</span>'.repeat(5 - ratingRounded)}
                            <span class="sr-only">${ratingRounded} out of 5 stars</span>
                        </div>
                    </div>
                    <button class="btn add-to-cart"
                            data-product-id="${this.#escapeHtml(id)}"
                            ${isOutOfStock ? 'disabled' : ''}
                            aria-label="Add ${this.#escapeHtml(name)} to cart"
                            ${isOutOfStock ? 'aria-disabled="true"' : ''}>
                        ${isOutOfStock ? 'Out of Stock' : 'Add to Cart'}
                    </button>
                </div>
            </article>
        `;
    }

    /**
     * Render pagination controls
     * @private
     */
    static #renderPagination(currentPage, totalPages, containerId, searchTerm, category) {
        if (totalPages <= 1) return '';

        const pages = [];
        const maxVisible = 5;

        let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
        let endPage = Math.min(totalPages, startPage + maxVisible - 1);

        // Adjust start if we're near the end
        if (endPage - startPage + 1 < maxVisible) {
            startPage = Math.max(1, endPage - maxVisible + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            pages.push(`
                <button class="page-btn ${i === currentPage ? 'active' : ''}"
                        data-page="${i}"
                        onclick="ApiService.displayProducts({
                            containerId: '${containerId}',
                            searchTerm: '${searchTerm}',
                            category: '${category}',
                            page: ${i}
                        })">
                    ${i}
                </button>
            `);
        }

        return `
            <div class="pagination">
                ${currentPage > 1 ? `
                    <button class="page-btn prev"
                            onclick="ApiService.displayProducts({
                                containerId: '${containerId}',
                                searchTerm: '${searchTerm}',
                                category: '${category}',
                                page: ${currentPage - 1}
                            })">
                        ← Previous
                    </button>
                ` : ''}
                ${pages.join('')}
                ${currentPage < totalPages ? `
                    <button class="page-btn next"
                            onclick="ApiService.displayProducts({
                                containerId: '${containerId}',
                                searchTerm: '${searchTerm}',
                                category: '${category}',
                                page: ${currentPage + 1}
                            })">
                        Next →
                    </button>
                ` : ''}
            </div>
        `;
    }

    /**
     * Initialize lazy loading for images
     * @private
     */
    static #initLazyLoading(container) {
        if ('IntersectionObserver' in window) {
            const imageObserver = new IntersectionObserver((entries, observer) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const img = entry.target;
                        if (img.dataset.src) {
                            img.src = img.dataset.src;
                            img.removeAttribute('data-src');
                        }
                        observer.unobserve(img);
                    }
                });
            });

            container.querySelectorAll('img[data-src]').forEach(img => {
                imageObserver.observe(img);
            });
        }
    }

    /**
     * Escape HTML to prevent XSS
     * @private
     */
    static #escapeHtml(unsafe) {
        if (typeof unsafe !== 'string') return '';
        return String(unsafe)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;')
            .replace(/\//g, '&#x2F;');
    }
    // =========================================================================
    // MOCK DATA FOR DEMONSTRATION
    // =========================================================================

    // =========================================================================
    // API CONFIGURATION
    // =========================================================================

    /**
     * API endpoint URL for products
     */
    static PRODUCTS_API_URL = '/api/products';

    /**
     * Hardcoded product data used as a fail-safe fallback, mirroring the Java ShoppingService.
     */
    static #FALLBACK_PRODUCTS = [
        { id: "000001", name: "Garuda Wireless Mouse", price: 9.99, description: "High-precision wireless mouse with ergonomic design", category: "Electronics", rating: 4.5, image: "GarudaHawk.jpg" },
        { id: "000002", name: "Casio Watch", price: 29.50, description: "Classic analog watch with leather strap", category: "Fashion", rating: 4.2, image: "Casio Watch.jpg" },
        { id: "000003", name: "iPhone 16", price: 749.00, description: "Latest iPhone with advanced camera system", category: "Electronics", rating: 4.8, image: "Iphone 16.jpg" },
        { id: "000004", name: "Varsity Jacket", price: 4.49, description: "Classic college-style varsity jacket", category: "Fashion", rating: 4.0, image: "NU jacket.jpg" },
        { id: "000005", name: "Apple Earpods", price: 299.00, description: "Premium wireless earbuds with noise cancellation", category: "Electronics", rating: 4.7, image: "Airpods.jpg" },
        { id: "000006", name: "Crocs", price: 49.00, description: "Comfortable and lightweight casual footwear", category: "Footwear", rating: 4.1, image: "Crocs.jpg" },
        { id: "000007", name: "Adidas Samba", price: 59.00, description: "Classic soccer-inspired casual sneakers", category: "Footwear", rating: 4.3, image: "Adidas_Samba-removebg-preview.png" },
        { id: "000008", name: "Fstoppers Shoulder Bag", price: 19.99, description: "Professional camera bag for photographers", category: "Accessories", rating: 4.4, image: "FstopperBag.jpg" },
        { id: "000009", name: "Sunglasses", price: 9.99, description: "UV protection sunglasses with polarized lenses", category: "Accessories", rating: 3.9, image: "Sunglass.jpg" },
        { id: "000010", name: "Thermos Flask", price: 29.50, description: "Insulated stainless steel water bottle", category: "Home & Kitchen", rating: 4.6, image: "ThermFlask.jpg" },
        { id: "000011", name: "Iconic Socks", price: 4.49, description: "Comfortable cotton blend socks", category: "Clothing", rating: 4.2, image: "Socks.jpg" },
        { id: "000012", name: "Asus Monitor", price: 749.00, description: "4K UHD gaming monitor with high refresh rate", category: "Electronics", rating: 4.7, image: "AsusMonitor.jpg" },
        { id: "000013", name: "Kingston NVME SSD", price: 299.00, description: "High-speed NVMe solid state drive", category: "Electronics", rating: 4.5, image: "KingstonSSD.jpg" },
        { id: "000014", name: "PC Case", price: 49.00, description: "ATX mid-tower gaming computer case", category: "Electronics", rating: 4.0, image: "PcCase.jpg" }
    ];

    /**
     * Fetches all products from the backend API with a fallback mechanism.
     * @returns {Promise<Array<Object>>} A promise that resolves with the list of products (either from API or mock).
     */
    static async fetchProducts() {
        try {
            const response = await fetch(this.PRODUCTS_API_URL);

            if (!response.ok) {
                // Throw an error if the HTTP status is not 2xx
                const errorBody = await response.text();
                throw new Error(`HTTP error! Status: ${response.status} - ${errorBody.substring(0, 100)}...`);
            }

            // The line below is where the SyntaxError is currently happening due to bad JSON from the server.
            const products = await response.json();

            if (!Array.isArray(products)) {
                throw new Error("API response was successful but did not return a valid list of products.");
            }

            console.log(`API loaded ${products.length} products via HTTP fetch.`);

            return products;

        } catch (error) {
            // --- FALLBACK IMPLEMENTATION ---

            // 1. Log the original error for debugging the backend.
            console.error("Error fetching products from backend:", error);

            // 2. Log a warning about the fallback.
            console.warn(`API fetch failed. Falling back to internal mock data. Loading ${this.#FALLBACK_PRODUCTS.length} products.`);

            // 3. Return the hardcoded data to ensure the application loads.
            return this.#FALLBACK_PRODUCTS;
        }
    }
}

// Initialize global instance and set up event listeners

// Handle online/offline events
window.addEventListener('online', () => {
    // Process any queued requests when coming back online
    ApiService['#processQueue']();

    // Show online status
    const status = document.createElement('div');
    status.className = 'online-status online';
    status.textContent = 'Back online';
    document.body.appendChild(status);

    setTimeout(() => {
        status.classList.add('hide');
        setTimeout(() => status.remove(), 500);
    }, 3000);
});

window.addEventListener('offline', () => {
    // Show offline status
    const status = document.createElement('div');
    status.className = 'online-status offline';
    status.textContent = 'Offline - working in offline mode';
    document.body.appendChild(status);

    setTimeout(() => {
        status.classList.add('hide');
        setTimeout(() => status.remove(), 500);
    }, 5000);
});

// Service worker registration is disabled for this implementation
// if ('serviceWorker' in navigator) {
//     window.addEventListener('load', () => {
//         ApiService.registerServiceWorker();
//     });
// }

// Handle service worker updates (disabled)
// let refreshing = false;
// navigator.serviceWorker?.addEventListener('controllerchange', () => {
//     if (!refreshing) {
//         window.location.reload();
//         refreshing = true;
//     }
// });

// Listen for update available event (disabled)
// window.addEventListener('swUpdateAvailable', () => {
//     if (confirm('A new version is available. Update now?')) {
//         ApiService.updateServiceWorker();
//     }
// });
