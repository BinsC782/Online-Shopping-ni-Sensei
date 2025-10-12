/**
 * cart-view-logic.js
 * Loads cart contents and renders them dynamically to the viewing-cart.html page.
 * ASSUMPTION: ApiService class is available globally because it is loaded 
 * via a separate <script> tag in viewing-cart.html, meaning no explicit import is necessary here.
 */

/**
 * Global function definition to handle the 'close cart' event, 
 * suppressing the ReferenceError if it's called from HTML.
 */
window.closeCart = function() {
    console.log("Cart closing function called. (If this was a modal, it would now hide.)");
};

/**
 * Simple sanitization helper function for dynamic content
 * @param {string} text - Text to sanitize
 * @returns {string} Sanitized text
 */
function sanitize(text) {
    return text ? String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;') : '';
}

/**
 * Renders the cart items fetched from the API into the DOM.
 * @param {Array<Object>} items - The list of cart items returned by the backend.
 */
function renderCartItems(items) {
    const listContainer = document.getElementById('cart-items-list');
    if (!listContainer) {
        console.error("Cart container #cart-items-list not found in HTML.");
        return;
    }

    listContainer.innerHTML = ''; // Clear existing list content

    if (items.length === 0) {
        listContainer.innerHTML = `<li class="p-4 text-gray-500 italic">Your cart is currently empty. Add some products to see them here!</li>`;
        return;
    }

    let cartTotal = 0;

    items.forEach(item => {
        // Assume the item structure returned by the Java Servlet is { id, name, price, quantity (optional) }
        const name = item.name || 'Unnamed Product';
        const price = item.price || 0.00;
        const quantity = item.quantity || 1; // Assuming quantity is 1 if not specified

        const listItem = document.createElement('li');
        listItem.className = 'flex justify-between items-center py-3 border-b border-gray-100 last:border-b-0';

        const itemTotal = price * quantity;
        cartTotal += itemTotal;

        listItem.innerHTML = `
            <div class="flex-grow">
                <p class="text-lg font-semibold text-gray-800">${sanitize(name)}</p>
                <p class="text-sm text-gray-500">Qty: ${quantity} &times; $${price.toFixed(2)}</p>
            </div>
            <div class="text-lg font-bold text-indigo-600">$${itemTotal.toFixed(2)}</div>
        `;

        listContainer.appendChild(listItem);
    });

    // Update the total summary
    const totalElement = document.getElementById('total-price');
    if (totalElement) {
        totalElement.textContent = `$${cartTotal.toFixed(2)}`;
    }
}

/**
 * Loads the cart data using ApiService and initiates rendering.
 */
async function loadAndRenderCart() {
    console.log('Fetching and rendering cart contents...');

    // Set loading state
    const listContainer = document.getElementById('cart-items-list');
    if (listContainer) {
        listContainer.innerHTML = `<li class="p-4 text-indigo-500 font-medium">Loading cart items...</li>`;
    }

    try {
        // Use direct fetch since ApiService is global
        const response = await fetch(`http://localhost:8084/api/cart`);
        if (!response.ok) {
            throw new Error(`Failed to load cart: ${response.status}`);
        }
        const data = await response.json();
        const items = data || []; // Assuming array or object with items

        renderCartItems(items);
        console.log(`✅ Cart loaded successfully with ${items.length} items.`);

    } catch (error) {
        console.error('❌ Failed to load cart:', error.message || error);

        // Enhanced error reporting for debugging
        if (error.message && error.message.includes('401')) {
            console.error('🚨 Authorization Error: The server requires authentication for /api/cart. Check if your Java servlet allows unauthenticated access or if ApiService is sending the correct auth headers.');
        }

        // Display error message to the user
        if (listContainer) {
            listContainer.innerHTML = `<li class="p-4 text-red-500">Error loading cart: ${error.message || 'Network error.'} Please check the server and console.</li>`;
        }
        // Ensure total is reset on failure
        const totalElement = document.getElementById('total-price');
        if (totalElement) totalElement.textContent = '$0.00';
    }
}

// Start loading the cart once the entire page structure is ready
document.addEventListener('DOMContentLoaded', loadAndRenderCart);
