// Homepage specific functionality
document.addEventListener('DOMContentLoaded', () => {
    // Set default user session if not exists
    if (!localStorage.getItem('token')) {
        localStorage.setItem('token', 'temporary-session-token');
        localStorage.setItem('username', 'demo-user');
    }

    // Initialize the page
    function initializePage() {
        try {
            setupEventListeners();
            loadProducts().catch(error => {
                console.error('Error loading products:', error);
                // Don't show error if we're not on the products page
                if (document.getElementById('products-container')) {
                    showError('Failed to load products. Using demo data.');
                    loadDemoProducts();
                }
            });
        } catch (error) {
            console.error('Error initializing homepage:', error);
            // Ensure basic functionality still works
            setupEventListeners();
        }
    }
    
    // Start the application
    initializePage();
});

// Load products from the backend
async function loadProducts() {
    const productsContainer = document.getElementById('products-container');

    try {
        // Show loading state
        productsContainer.innerHTML = '<div class="loading">Loading products...</div>';

        // Fetch products from the backend using the API service
        const products = await ApiService.getProducts();

        if (products.length === 0) {
            productsContainer.innerHTML = '<div class="no-products">No products available</div>';
            return;
        }

        // Render products dynamically
        productsContainer.innerHTML = products.map(product => `
            <div class="product-card" data-id="${product.id}">
                <img src="Photos/${product.image}" alt="${product.name}">
                <div class="product-info">
                    <h3>${product.name}</h3>
                    <p>$${product.price.toFixed(2)}</p>
                    <div class="product-actions">
                        <button class="add-to-cart" data-id="${product.id}">
                            <img src="https://img.icons8.com/ios-filled/24/1e90ff/shopping-cart.png" alt="Add to cart">
                        </button>
                        <button class="view-details" data-id="${product.id}">View Details</button>
                    </div>
                </div>
            </div>
        `).join('');

    } catch (error) {
        console.error('Error loading products:', error);
        productsContainer.innerHTML = `
            <div class="error">
                Failed to load products.
                <button onclick="location.reload()">Try Again</button>
            </div>`;
    }
}

// Set up event listeners for the homepage
function setupEventListeners() {
    // Category filter buttons
    const categoryButtons = document.querySelectorAll('.categories button');
    categoryButtons.forEach(button => {
        button.addEventListener('click', () => {
            // Remove active class from all buttons
            categoryButtons.forEach(btn => btn.classList.remove('active'));
            // Add active class to clicked button
            button.classList.add('active');
            
            // TODO: Implement category filtering
            const category = button.textContent.trim();
            if (category === 'All Categories') {
                loadProducts();
            } else {
                // Filter products by category
                // This would be implemented once the backend supports category filtering
                console.log('Filtering by category:', category);
            }
        });
    });

    // Search functionality
    const searchInput = document.querySelector('.search-bar input');
    const searchButton = document.querySelector('.search-bar button');
    
    const performSearch = () => {
        const searchTerm = searchInput.value.trim();
        if (searchTerm) {
            // TODO: Implement search functionality
            console.log('Searching for:', searchTerm);
        }
    };
    
    searchButton.addEventListener('click', performSearch);
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') performSearch();
    });

    // Set up product card event listeners
    setupProductCardListeners();
}

// Add event listeners for product cards
function setupProductCardListeners() {
    // View details button click
    document.addEventListener('click', (e) => {
        const viewDetailsBtn = e.target.closest('.view-details');
        if (viewDetailsBtn) {
            const productId = viewDetailsBtn.dataset.id;
            showProductDetails(productId);
            return;
        }

        // Product card click (except on buttons)
        const productCard = e.target.closest('.product-card');
        if (productCard && !e.target.closest('button')) {
            const productId = productCard.dataset.id;
            showProductDetails(productId);
        }
    });

    // Close modal button
    const closeBtn = document.querySelector('.close-btn');
    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            document.querySelector('.modal').style.display = 'none';
        });
    }

    // Close modal when clicking outside
    window.addEventListener('click', (e) => {
        const modal = document.querySelector('.modal');
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });

    // Initialize modal content
    const modal = document.querySelector('.modal');
    if (modal && !modal.querySelector('.modal-content')) {
        try {
            const modalContent = document.createElement('div');
            modalContent.className = 'modal-content';
            modalContent.innerHTML = `
                <button class="close-btn">&times;</button>
                <div class="product-details">
                    <div class="product-image">
                        <img id="modal-product-image" src="" alt="Product Image">
                    </div>
                    <div class="product-info">
                        <h2 id="modal-product-title"></h2>
                        <p id="modal-product-price"></p>
                        <p id="modal-product-description"></p>
                        <div class="quantity">
                            <button id="qty-minus">-</button>
                            <span id="qty-value">1</span>
                            <button id="qty-plus">+</button>
                        </div>
                        <div class="actions">
                            <button class="add-cart" id="modal-add-to-cart">Add To Cart</button>
                            <button class="buy-now" id="modal-buy-now">Buy Now</button>
                        </div>
                    </div>
                </div>`;
            
            modal.appendChild(modalContent);

            // Add event listeners for quantity controls
            const qtyMinus = document.getElementById('qty-minus');
            const qtyPlus = document.getElementById('qty-plus');
            const qtyValue = document.getElementById('qty-value');
            
            if (qtyMinus && qtyPlus && qtyValue) {
                qtyMinus.addEventListener('click', () => {
                    let qty = parseInt(qtyValue.textContent);
                    if (qty > 1) qty--;
                    qtyValue.textContent = qty;
                });
                
                qtyPlus.addEventListener('click', () => {
                    let qty = parseInt(qtyValue.textContent);
                    qty++;
                    qtyValue.textContent = qty;
                });
            }
            
            // Add to cart from modal
            const addToCartBtn = document.getElementById('modal-add-to-cart');
            if (addToCartBtn) {
                addToCartBtn.addEventListener('click', () => {
                    const currentProductId = modal.dataset.currentProductId;
                    const quantity = parseInt(qtyValue ? qtyValue.textContent : '1');
                    if (currentProductId) {
                        addToCart(currentProductId, quantity);
                    } else {
                        console.error('No product ID found for adding to cart');
                    }
                });
            }
            
        } catch (error) {
            console.error('Error initializing modal:', error);
            if (modal) {
                modal.innerHTML = `
                    <div class="error">
                        <p>Failed to load product details.</p>
                        <button class="close-btn">&times;</button>
                    </div>`;
            }
        }
    }
}

// Add product to cart
function addToCart(productId, quantity = 1) {
    if (!productId) {
        console.error('Product ID is required');
        return;
    }
    
    try {
        // TODO: Implement add to cart functionality
        console.log(`Adding ${quantity}x product ${productId} to cart`);
        // Show success message or update cart count
    } catch (error) {
        console.error('Error adding to cart:', error);
        showError('Failed to add item to cart. Please try again.');
    }
}

// Show error message
function showError(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    
    // Add to the top of the page
    const header = document.querySelector('header');
    if (header && header.nextSibling) {
        header.parentNode.insertBefore(errorDiv, header.nextSibling);
    } else {
        document.body.prepend(errorDiv);
    }
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        errorDiv.remove();
    }, 5000);
}

// Show product details in modal
async function showProductDetails(productId) {
    const modal = document.querySelector('.modal');
    if (!modal) return;

    try {
        // Set loading state
        modal.innerHTML = '<div class="loading">Loading product details...</div>';
        modal.style.display = 'block';
        
        // Store the current product ID on the modal
        modal.dataset.currentProductId = productId;
        
        // In a real app, you would fetch the product details from your API
        // For now, we'll use the product ID to find it in the DOM
        const productCard = document.querySelector(`.product-card[data-id="${productId}"]`);
        if (!productCard) throw new Error('Product not found');
        
        // Get product details from the card
        const title = productCard.querySelector('.product-title')?.textContent || 'Product';
        const price = productCard.querySelector('.product-price')?.textContent || 'N/A';
        const imageSrc = productCard.querySelector('.product-image img')?.src || '';
        
        // Update modal content
        const modalContent = `
            <div class="modal-content">
                <button class="close-btn">&times;</button>
                <div class="product-details">
                    <div class="product-image">
                        <img id="modal-product-image" src="${imageSrc}" alt="${title}">
                    </div>
                    <div class="product-info">
                        <h2 id="modal-product-title">${title}</h2>
                        <p id="modal-product-price">${price}</p>
                        <p id="modal-product-description">Product description goes here.</p>
                        <div class="quantity">
                            <button id="qty-minus">-</button>
                            <span id="qty-value">1</span>
                            <button id="qty-plus">+</button>
                        </div>
                        <div class="actions">
                            <button class="add-cart" id="modal-add-to-cart">Add To Cart</button>
                            <button class="buy-now" id="modal-buy-now">Buy Now</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        modal.innerHTML = modalContent;
        
        // Re-attach event listeners
        setupModalEventListeners();
        
    } catch (error) {
        console.error('Error showing product details:', error);
        modal.innerHTML = `
            <div class="error">
                <p>Failed to load product details.</p>
                <button class="close-btn">&times;</button>
            </div>`;
    }
}

// Set up event listeners for modal elements
function setupModalEventListeners() {
    // Close modal when clicking the close button
    const closeBtn = document.querySelector('.modal .close-btn');
    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            const modal = document.querySelector('.modal');
            if (modal) modal.style.display = 'none';
        });
    }
    
    // Quantity controls
    const qtyMinus = document.getElementById('qty-minus');
    const qtyPlus = document.getElementById('qty-plus');
    const qtyValue = document.getElementById('qty-value');
    
    if (qtyMinus && qtyPlus && qtyValue) {
        qtyMinus.addEventListener('click', () => {
            let qty = parseInt(qtyValue.textContent);
            if (qty > 1) qty--;
            qtyValue.textContent = qty;
        });
        
        qtyPlus.addEventListener('click', () => {
            let qty = parseInt(qtyValue.textContent);
            qty++;
            qtyValue.textContent = qty;
        });
    }
    
    // Add to cart button
    const addToCartBtn = document.getElementById('modal-add-to-cart');
    if (addToCartBtn) {
        addToCartBtn.addEventListener('click', () => {
            const modal = document.querySelector('.modal');
            if (!modal) return;
            
            const productId = modal.dataset.currentProductId;
            const quantity = parseInt(document.getElementById('qty-value')?.textContent || '1');
            
            if (productId) {
                addToCart(productId, quantity);
                // Close the modal after adding to cart
                modal.style.display = 'none';
            } else {
                console.error('No product ID found for adding to cart');
            }
        });
    }
    
    // Buy now button
    const buyNowBtn = document.getElementById('modal-buy-now');
    if (buyNowBtn) {
        buyNowBtn.addEventListener('click', () => {
            // Implement buy now functionality
            alert('Buy now functionality will be implemented here');
        });
    }
}

// Make functions available globally
window.loadProducts = loadProducts;
window.showError = showError;
window.showProductDetails = showProductDetails;
