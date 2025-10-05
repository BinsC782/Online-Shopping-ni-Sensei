class ApiService {
    static baseUrl = 'http://localhost:8080/api';

    /**
     * Get authentication headers with token
     * @private
     */
    static getAuthHeaders() {
        const token = localStorage.getItem('token');
        return {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        };
    }

    /**
     * Handle API response and errors
     * @private
     */
    static async handleResponse(response) {
        if (!response.ok) {
            let errorMsg = `HTTP error! status: ${response.status}`;
            try {
                const errorData = await response.json();
                errorMsg = errorData.message || errorMsg;
            } catch (e) {
                // If we can't parse the error as JSON, use the status text
                errorMsg = response.statusText || errorMsg;
            }
            throw new Error(errorMsg);
        }
        return response.json();
    }

    // ===== PRODUCT ENDPOINTS =====

    /**
     * Fetch all products with optional search term
     * @param {string} searchTerm - Optional search term to filter products
     * @returns {Promise<Array>} List of products
     */
    static async getProducts(searchTerm = '') {
        try {
            const url = searchTerm 
                ? `${this.baseUrl}/products?search=${encodeURIComponent(searchTerm)}`
                : `${this.baseUrl}/products`;
                
            const response = await fetch(url);
            return await this.handleResponse(response);
        } catch (error) {
            console.error('Error fetching products:', error);
            throw error;
        }
    }

    /**
     * Fetch a single product by ID
     * @param {string} productId - The 6-digit product ID
     * @returns {Promise<Object>} Product details
     */
    static async getProductById(productId) {
        try {
            const response = await fetch(`${this.baseUrl}/products/${productId}`);
            return await this.handleResponse(response);
        } catch (error) {
            console.error(`Error fetching product ${productId}:`, error);
            throw error;
        }
    }

    // ===== CART ENDPOINTS =====

    /**
     * Get the current user's cart
     * @returns {Promise<Object>} Cart contents
     */
    static async getCart() {
        try {
            const response = await fetch(`${this.baseUrl}/cart`, {
                headers: this.getAuthHeaders()
            });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('Error fetching cart:', error);
            throw error;
        }
    }

    /**
     * Add a product to the cart
     * @param {string} productId - The 6-digit product ID
     * @param {number} quantity - Quantity to add (default: 1)
     * @returns {Promise<Object>} Updated cart
     */
    static async addToCart(productId, quantity = 1) {
        try {
            const response = await fetch(`${this.baseUrl}/cart`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ productId, quantity })
            });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('Error adding to cart:', error);
            throw error;
        }
    }

    /**
     * Update cart item quantity
     * @param {string} productId - The 6-digit product ID
     * @param {number} quantity - New quantity
     * @returns {Promise<Object>} Updated cart
     */
    static async updateCartItem(productId, quantity) {
        try {
            const response = await fetch(`${this.baseUrl}/cart/${productId}`, {
                method: 'PUT',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ quantity })
            });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('Error updating cart item:', error);
            throw error;
        }
    }

    /**
     * Remove item from cart
     * @param {string} productId - The 6-digit product ID to remove
     * @returns {Promise<Object>} Updated cart
     */
    static async removeFromCart(productId) {
        try {
            const response = await fetch(`${this.baseUrl}/cart/${productId}`, {
                method: 'DELETE',
                headers: this.getAuthHeaders()
            });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('Error removing from cart:', error);
            throw error;
        }
    }

    // ===== AUTH ENDPOINTS =====

    /**
     * User login
     * @param {string} username - Username
     * @param {string} password - Password
     * @returns {Promise<Object>} User data with token
     */
    static async login(username, password) {
        try {
            const response = await fetch(`${this.baseUrl}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            
            const data = await this.handleResponse(response);
            
            if (data.token) {
                localStorage.setItem('token', data.token);
                localStorage.setItem('username', username);
            }
            
            return data;
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    }

    /**
     * User registration
     * @param {string} username - Username
     * @param {string} password - Password
     * @param {string} email - Email address
     * @returns {Promise<Object>} Registration result
     */
    static async register(username, password, email) {
        try {
            const response = await fetch(`${this.baseUrl}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password, email })
            });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('Registration error:', error);
            throw error;
        }
    }

    /**
     * User logout
     */
    static logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('username');
    }

    /**
     * Check if user is authenticated
     * @returns {boolean} True if user is logged in
     */
    static isAuthenticated() {
        return !!localStorage.getItem('token');
    }

    /**
     * Get current user info
     * @returns {Promise<Object>} User information
     */
    static async getCurrentUser() {
        try {
            const response = await fetch(`${this.baseUrl}/me`, {
                headers: this.getAuthHeaders()
            });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('Error fetching user info:', error);
            throw error;
        }
    }
}

// Make it available globally
window.ApiService = ApiService;
