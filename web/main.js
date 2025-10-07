// Cart state management
let cart = [];

// Initialize cart from localStorage
function getCart() {
    try {
        const storedCart = localStorage.getItem('shoppingCart');
        cart = storedCart ? JSON.parse(storedCart) : [];
        console.log('Retrieved cart:', cart);
        updateCartBadge();
        return cart;
    } catch (error) {
        console.error('Error loading cart:', error);
        cart = [];
        return [];
    }
}

// Save cart to localStorage
function saveCart() {
    try {
        localStorage.setItem('shoppingCart', JSON.stringify(cart));
        console.log('Cart saved:', cart);
        updateCartBadge();
        
        // Update cart display if on cart page
        if (window.location.pathname.includes('Viewing cart.html')) {
            displayCart();
        }
    } catch (error) {
        console.error('Error saving cart:', error);
    }
}

// Add item to cart or update quantity if exists
function addToCart(product) {
    try {
        if (!product || !product.id) {
            console.error('Invalid product:', product);
            return;
        }
        
        const existingItem = cart.find(item => item.id === product.id);
        if (existingItem) {
            existingItem.quantity += 1;
            showNotification(`${product.name} quantity updated to ${existingItem.quantity}`);
        } else {
            cart.push({ 
                ...product, 
                quantity: 1, 
                checked: true,
                addedAt: new Date().toISOString()
            });
            showNotification(`${product.name} added to cart`);
        }
        saveCart();
    } catch (error) {
        console.error('Error adding to cart:', error);
        showNotification('Failed to add item to cart', 'error');
    }
}

// Update item quantity
function updateQuantity(id, newQuantity) {
    try {
        const item = cart.find(item => item.id === id);
        if (!item) return;
        
        newQuantity = parseInt(newQuantity, 10);
        if (isNaN(newQuantity) || newQuantity < 0) return;
        
        if (newQuantity === 0) {
            removeItem(id);
        } else {
            item.quantity = newQuantity;
            saveCart();
        }
    } catch (error) {
        console.error('Error updating quantity:', error);
    }
}

// Remove item from cart
function removeItem(id) {
    try {
        const item = cart.find(item => item.id === id);
        if (!item) return;
        
        if (confirm(`Remove ${item.name} from cart?`)) {
            cart = cart.filter(item => item.id !== id);
            saveCart();
            showNotification(`${item.name} removed from cart`);
        }
    } catch (error) {
        console.error('Error removing item:', error);
    }
}

// Toggle item checked state
function toggleChecked(id) {
    const item = cart.find(item => item.id === id);
    if (item) {
        item.checked = !item.checked;
        saveCart();
    }
}

// Calculate cart total
function getTotal() {
    return cart.reduce((total, item) => total + (item.price * item.quantity), 0);
}

// Get total number of items in cart
function getCartCount() {
    return cart.reduce((count, item) => count + item.quantity, 0);
}

// Update cart badge in header
function updateCartBadge() {
    const badge = document.querySelector('.cart-badge');
    if (badge) {
        const count = getCartCount();
        badge.textContent = count > 0 ? count : '';
        badge.style.display = count > 0 ? 'flex' : 'none';
    }
}

// Display cart items
function displayCart() {
    const cartItems = document.getElementById('cart-items');
    const cartFooter = document.getElementById('cart-footer');
    const emptyCart = document.getElementById('empty-cart');
    
    if (!cartItems) return;

    // Clear existing items
    cartItems.innerHTML = '';

    if (cart.length === 0) {
        if (emptyCart) emptyCart.style.display = 'block';
        if (cartFooter) cartFooter.style.display = 'none';
        return;
    }

    // Hide empty cart message
    if (emptyCart) emptyCart.style.display = 'none';
    if (cartFooter) cartFooter.style.display = 'block';

    // Add each item to the cart
    cart.forEach(item => {
        const itemElement = document.createElement('div');
        itemElement.className = 'cart-item';
        itemElement.dataset.id = item.id;
        
        itemElement.innerHTML = `
            <div class="item-image">
                <img src="${item.imageUrl || 'Photos/default-product.png'}" alt="${item.name}" onerror="this.src='Photos/default-product.png'">
            </div>
            <div class="item-details">
                <h3 class="item-name">${item.name}</h3>
                <p class="item-price">$${item.price.toFixed(2)}</p>
                <div class="quantity-controls">
                    <button class="quantity-btn" onclick="updateQuantity('${item.id}', ${item.quantity - 1})">-</button>
                    <input type="number" min="1" value="${item.quantity}" 
                           onchange="updateQuantity('${item.id}', this.value)" 
                           class="quantity-input">
                    <button class="quantity-btn" onclick="updateQuantity('${item.id}', ${item.quantity + 1})">+</button>
                </div>
                <button class="remove-btn" onclick="removeItem('${item.id}')">
                    <i class="fas fa-trash"></i> Remove
                </button>
            </div>
        `;
        cartItems.appendChild(itemElement);
    });

    updateTotal();
}

// Show notification
function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    // Auto-remove after 3 seconds
    setTimeout(() => {
        notification.classList.add('fade-out');
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// DO NOT EVER REMOVE, ONLY MODIFY - Core modal initialization and event listeners
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM fully loaded, initializing...');
    
    // Initialize cart
    getCart();
    updateCartBadge();
    
    // Get all product cards
    const productCards = document.querySelectorAll('.product-card');
    console.log(`Found ${productCards.length} product cards`);
    
    // Get modal elements
    const modal = document.getElementById('product-modal');
    if (!modal) console.error('Modal element not found!');
    
    const closeBtn = document.querySelector('.product-modal .close-btn');
    const modalImage = document.getElementById('modal-image');
    const modalTitle = document.getElementById('modal-title');
    const modalPrice = document.getElementById('modal-price');
    const modalRating = document.getElementById('modal-rating');
    const modalAddToCart = document.getElementById('modal-add-to-cart');
    const modalBuyNow = document.getElementById('modal-buy-now');
    
    // Initialize quantity controls
    const qtyMinus = document.querySelector('.qty-minus');
    const qtyPlus = document.querySelector('.qty-plus');
    const qtyValue = document.querySelector('.qty-value');
    let currentQty = 1;
    let currentProduct = null;

    // Quantity control handlers
    if (qtyMinus) {
        qtyMinus.onclick = function() {
            if (currentQty > 1) {
                currentQty--;
                qtyValue.textContent = currentQty;
            }
        };
    }

    if (qtyPlus) {
        qtyPlus.onclick = function() {
            currentQty++;
            qtyValue.textContent = currentQty;
        };
    }

    // Handle product card clicks
    document.addEventListener('click', function(event) {
        // Find the closest product card element from the click target
        const card = event.target.closest('.product-card');
        if (!card) return;

        try {
            const img = card.querySelector('img');
            const titleEl = card.querySelector('h3');
            const priceEl = card.querySelector('p');
            const ratingEl = card.querySelector('span');
            
            if (!img || !titleEl || !priceEl || !ratingEl) {
                console.error('Missing required elements in product card');
                return;
            }

            const title = titleEl.textContent.trim();
            const priceText = priceEl.textContent.trim();
            const rating = ratingEl.textContent.trim();
            const price = parseFloat(priceText.replace(/[^0-9.-]+/g, ''));
            const id = title.toLowerCase().replace(/\s+/g, '-');

            console.log('Opening product:', { title, price, id });

            // Update modal content
            if (modalImage) modalImage.src = img.src || '';
            if (modalImage) modalImage.alt = title;
            if (modalTitle) modalTitle.textContent = title;
            if (modalPrice) modalPrice.textContent = priceText;
            if (modalRating) modalRating.textContent = rating;
      
            // Reset quantity
            currentQty = 1;
            if (qtyValue) qtyValue.textContent = currentQty;
            
            // Store current product info
            currentProduct = { 
                id, 
                name: title, 
                price, 
                image: img.src,
                rating: rating
            };

            // Show modal
            if (modal) {
                modal.style.display = 'block';
                console.log('Modal should be visible now');
            } else {
                console.error('Modal element not found');
            }
        } catch (error) {
            console.error('Error handling product card click:', error);
        }
    });

    // Add to cart handler
    if (modalAddToCart) {
        modalAddToCart.addEventListener('click', function() {
            if (!currentProduct) {
                console.error('No product selected');
                return;
            }
            
            console.log(`Adding ${currentQty} ${currentProduct.name} to cart`);
            
            for (let i = 0; i < currentQty; i++) {
                addToCart({...currentProduct});
            }
            
            if (modal) {
                modal.style.display = 'none';
            }
        });
    } else {
        console.error('Add to cart button not found');
    }

    // Buy now handler
    if (modalBuyNow) {
        modalBuyNow.addEventListener('click', function() {
            if (!currentProduct) {
                console.error('No product selected');
                return;
            }
            
            console.log(`Buying ${currentQty} ${currentProduct.name} now`);
            
            // Add to cart first
            for (let i = 0; i < currentQty; i++) {
                addToCart({...currentProduct});
            }
            
            // Then redirect to checkout
            window.location.href = 'Viewing cart.html';
        });
    } else {
        console.error('Buy now button not found');
    }

    // Close modal when clicking the close button
    if (closeBtn) {
        closeBtn.addEventListener('click', function() {
            if (modal) {
                modal.style.display = 'none';
                console.log('Modal closed');
            }
        });
    } else {
        console.error('Close button not found');
    }

    // Close modal when clicking outside
    if (modal) {
        modal.addEventListener('click', function(event) {
            if (event.target === modal) {
                modal.style.display = 'none';
                console.log('Modal closed (outside click)');
            }
        });
    }
});

// Function to save order to a text file
async function saveOrderToFile(orderData) {
    try {
        // Format the order data as a string
        const orderString = `Order ${new Date().toISOString()}:\n` +
            `Items: ${orderData.items.map(item => `${item.quantity}x ${item.name}`).join(', ')}\n` +
            `Total: $${orderData.total.toFixed(2)}\n\n`;

        // In a real app, you would send this to your backend to save to a file
        // For now, we'll just log it to the console
        console.log('Order details to be saved:', orderString);
        
        // Show success message
        const messageDiv = document.createElement('div');
        messageDiv.className = 'order-success-message';
        messageDiv.innerHTML = `
            <div class="message-content">
                <i class="fas fa-check-circle"></i>
                <h3>Order Placed Successfully!</h3>
                <p>You have bought ${orderData.items.reduce((sum, item) => sum + item.quantity, 0)} items.</p>
                <p>Total: $${orderData.total.toFixed(2)}</p>
                <button onclick="this.parentElement.parentElement.remove()">OK</button>
            </div>
        `;
        document.body.appendChild(messageDiv);
        
        // Clear the cart
        cart = [];
        saveCart();
        displayCart();
        updateTotal();
        
        return true;
    } catch (error) {
        console.error('Error saving order:', error);
        return false;
    }
}

async function handleApiRequest(url, options = {}) {
    try {
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`,
                ...options.headers
            }
        });

        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.error || 'Request failed');
        }

        return data;
    } catch (error) {
        console.error('API request failed:', error);
        showNotification(error.message || 'An error occurred', 'error');
        throw error;
    }
}

// DO NOT EVER REMOVE, ONLY MODIFY - Core cart total calculation and UI update
function updateTotal() {
    try {
        const total = getTotal();
        const totalElement = document.getElementById('total-price');
        const confirmationTotal = document.getElementById('confirmation-total');
        const checkoutBtn = document.getElementById('checkout-btn');
        const cartFooter = document.getElementById('cart-footer');
        const emptyCart = document.getElementById('empty-cart');
        
        // Update total price display
        if (totalElement) {
            totalElement.textContent = `$${total.toFixed(2)}`;
        }
        
        // Update confirmation total
        if (confirmationTotal) {
            confirmationTotal.textContent = `$${total.toFixed(2)}`;
        }
        
        // Update cart footer and empty cart message visibility
        const itemCount = getCartCount();
        if (emptyCart) {
            emptyCart.style.display = itemCount > 0 ? 'none' : 'flex';
        }
        
        if (cartFooter) {
            cartFooter.style.display = itemCount > 0 ? 'flex' : 'none';
        }
        
        // Update checkout button state
        if (checkoutBtn) {
            checkoutBtn.disabled = itemCount === 0;
            checkoutBtn.title = itemCount > 0 ? 'Complete your purchase' : 'Your cart is empty';
        }
        
        // Update cart badge in header
        updateCartBadge();
        
        return total;
    } catch (error) {
        console.error('Error updating cart total:', error);
        return 0;
    }
}

async function displayProducts() {
    const productsContainer = document.getElementById('products-container');
    if (!productsContainer) return;

    try {
        // Show loading state
        productsContainer.innerHTML = '<div class="loading">Loading products...</div>';
        
        const products = await ApiService.getProducts();
        
        if (products.length === 0) {
            productsContainer.innerHTML = '<div class="no-products">No products available</div>';
            return;
        }

        productsContainer.innerHTML = products.map(product => `
            <div class="product-card" 
                 data-id="${product.id}" 
                 data-category="${product.category}">
                <img src="${product.imageUrl || 'placeholder.jpg'}" alt="${product.name}">
                <div class="product-info">
                    <h3>${product.name}</h3>
                    <p>$${product.price.toFixed(2)}</p>
                    <span>${'★'.repeat(Math.round(product.rating))}${'☆'.repeat(5-Math.round(product.rating))}</span>
                    ${product.stock > 0 ? 
                        `<button class="add-to-cart" data-id="${product.id}">
                            Add to Cart
                        </button>` : 
                        '<button class="out-of-stock" disabled>Out of Stock</button>'}
                    ${product.stock < 10 && product.stock > 0 ? 
                        `<div class="low-stock">Only ${product.stock} left!</div>` : ''}
                </div>
            </div>
        `).join('');

        // Add event listeners
        document.querySelectorAll('.add-to-cart').forEach(button => {
            button.addEventListener('click', async (e) => {
                const productId = e.target.dataset.id;
                try {
                    await ApiService.addToCart(productId, 1);
                    showNotification('Added to cart!', 'success');
                    updateCartBadge();
                } catch (error) {
                    showNotification(error.message || 'Failed to add to cart', 'error');
                }
            });
        });

    } catch (error) {
        console.error('Error displaying products:', error);
        productsContainer.innerHTML = `
            <div class="error">
                Failed to load products. 
                <button onclick="location.reload()">Try Again</button>
            </div>`;
    }
}

// Generate order ID in the format: ORD + timestamp
function generateOrderId() {
    return 'ORD' + Date.now();
}

// Handle checkout process
function handleCheckout() {
    const total = getTotal();
    if (total <= 0) return;
    
    // Generate order ID
    const orderId = generateOrderId();
    
    // Show confirmation message with order details
    const confirmationMessage = document.getElementById('confirmation-message');
    const orderIdElement = document.getElementById('order-id');
    
    if (confirmationMessage && orderIdElement) {
        // Update the order ID in the UI
        orderIdElement.textContent = orderId;
        
        // Show the confirmation message
        confirmationMessage.classList.add('show');
        
        // Hide the cart items and footer
        const cartContainer = document.querySelector('.cart-container');
        const cartFooter = document.getElementById('cart-footer');
        if (cartContainer) cartContainer.style.display = 'none';
        if (cartFooter) cartFooter.style.display = 'none';
        
        // Save the order to the server
        saveOrderToServer(orderId, total);
    }
    
    // Clear the cart after showing the message
    cart = [];
    saveCart();
}

// Save order to server
async function saveOrderToServer(orderId, total) {
    // Get current user from localStorage or use 'guest' if not logged in
    const currentUser = localStorage.getItem('currentUser') || 'guest';
    
    try {
        // Format items as a semicolon-separated list of product names
        const itemsList = cart.map(item => item.name).join(';');
        
        // Format the order line to match the existing format in orders.txt
        // Format: ORD{timestamp},username,item1;item2;item3,status
        const orderLine = `${orderId},${currentUser},${itemsList},Pending`;
        
        console.log('Saving order:', orderLine);
        
        // Send the order to the server to be saved to orders.txt
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain',
            },
            body: orderLine
        });
        
        if (response.ok) {
            console.log('Order saved to server:', orderLine);
            return true;
        }
        
        // If we got here, the server returned an error
        const errorData = await response.text();
        throw new Error(`Server error: ${response.status} ${errorData}`);
        
    } catch (error) {
        console.error('Error saving order:', error);
        // Show error message to user
        const orderIdElement = document.getElementById('order-id');
        if (orderIdElement) {
            orderIdElement.textContent = `${orderId} (Save Failed - Check Console)`;
        }
        
        // Fallback to localStorage if server save fails
        try {
            const pendingOrders = JSON.parse(localStorage.getItem('pendingOrders') || '[]');
            pendingOrders.push({
                orderId: orderId,
                userId: currentUser,
                items: cart.map(item => item.name).join(';'),
                status: 'Pending',
                total: total,
                timestamp: new Date().toISOString(),
                rawLine: `${orderId},${currentUser},${cart.map(item => item.name).join(';')},Pending`
            });
            
            localStorage.setItem('pendingOrders', JSON.stringify(pendingOrders));
            console.log('Order saved to localStorage as fallback');
            
            if (orderIdElement) {
                orderIdElement.textContent = `${orderId} (Saved Locally - Not on Server)`;
            }
        } catch (e) {
            console.error('Failed to save order to localStorage:', e);
            if (orderIdElement) {
                orderIdElement.textContent = `${orderId} (Save Failed Completely)`;
            }
        }
        
        return false;
    }
}
// Close cart and redirect to home
function closeCart() {
    window.location.href = 'index.html';
}

// Initialize event listeners for cart page
document.addEventListener('DOMContentLoaded', function() {
    // Initialize cart
    getCart();
    updateTotal();
    
    // Handle checkout button click
    const checkoutBtn = document.getElementById('checkout-btn');
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', handleCheckout);
    }
    
    // Handle continue shopping button in confirmation
    const continueShoppingBtn = document.getElementById('continue-shopping');
    if (continueShoppingBtn) {
        continueShoppingBtn.addEventListener('click', closeCart);
    }
    
    // Handle close button
    const closeBtn = document.getElementById('close-cart');
    if (closeBtn) {
        closeBtn.addEventListener('click', closeCart);
    }
});