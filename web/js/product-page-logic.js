/**
 * product-page-logic.js
 * Handles product page interactions, including adding items to cart.
 */

// Import ApiService for network requests
import ApiService from './api-service.js';

/**
 * Add a product to the cart via ApiService.
 * @param {string} productId - The ID of the product to add.
 * @param {number} quantity - Quantity to add (default 1).
 */
export async function addProductToCart(productId, quantity = 1) {
    try {
        // Use ApiService's addToCart, but adapt for /api/cart if needed
        // Since ApiService expects productId and quantity, but our backend expects product object
        // We'll need to fetch product details first or assume product data is available
        // For simplicity, assume productData is passed or fetched
        const productData = { id: productId, name: 'Sample Product', price: 10.0 }; // Replace with actual data
        const response = await fetch('http://localhost:8084/api/cart', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(productData)
        });

        if (response.ok) {
            console.log('Product added to cart successfully');
        } else {
            throw new Error('Failed to add to cart');
        }
    } catch (error) {
        console.error('Error adding product to cart:', error);
    }
}

// Example usage: Call this from product page buttons
// addProductToCart('123', 1);
