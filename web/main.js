
let cart = [];


function getCart() {
    const storedCart = localStorage.getItem('shoppingCart');
    cart = storedCart ? JSON.parse(storedCart) : [];
    console.log('Loaded cart:', cart);
    return cart;
}


function saveCart() {
    localStorage.setItem('shoppingCart', JSON.stringify(cart));
    console.log('Saved cart:', cart);
}


function addToCart(product) {
    const existingItem = cart.find(item => item.id === product.id);
    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        cart.push({ ...product, quantity: 1, checked: true });
    }
    saveCart();
    console.log('Added to cart:', product);
    alert(`${product.name} added to cart!`);
}


function updateQuantity(id, quantity) {
    const item = cart.find(item => item.id === id);
    if (item) {
        item.quantity = quantity;
        if (item.quantity <= 0) {
            removeItem(id);
        } else {
            saveCart();
        }
    }
    console.log('Updated quantity for', id, 'to', quantity);
}


function removeItem(id) {
    cart = cart.filter(item => item.id !== id);
    saveCart();
    console.log('Removed item:', id);
}


function toggleChecked(id) {
    const item = cart.find(item => item.id === id);
    if (item) {
        item.checked = !item.checked;
        saveCart();
        console.log('Toggled checked for', id, 'to', item.checked);
    }
}


function getTotal() {
    return cart.reduce((total, item) => total + (item.price * item.quantity), 0).toFixed(2);
}


function getCartCount() {
    return cart.reduce((count, item) => count + item.quantity, 0);
}


function displayCart() {
    const cartContainer = document.querySelector('.cart-container');
    if (!cartContainer) return;
    cartContainer.innerHTML = '';
    let total = 0;
    let itemCount = 0;
    const isCheckout = document.querySelector('.cart-footer') !== null;
    cart.forEach(item => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'cart-item';
        const checkedAttr = item.checked ? 'checked' : '';
        const checkbox = isCheckout ? `<input type="checkbox" ${checkedAttr} onchange="toggleChecked('${item.id}'); displayCart();">` : '';
        itemDiv.innerHTML = `
            ${checkbox}
            <img src="${item.image}" alt="${item.name}">
            <span class="item-name">${item.name}</span>
            <div class="quantity">
                <button class="qty-minus" onclick="updateQuantity('${item.id}', ${item.quantity - 1}); displayCart();">-</button>
                <span class="qty-value">${item.quantity}</span>
                <button class="qty-plus" onclick="updateQuantity('${item.id}', ${item.quantity + 1}); displayCart();">+</button>
            </div>
            <span class="price">$${item.price}</span>
            <button class="delete-btn" onclick="removeItem('${item.id}'); displayCart();">🗑️</button>
        `;
        cartContainer.appendChild(itemDiv);
        if (item.checked) {
            total += item.price * item.quantity;
            itemCount += item.quantity;
        }
    });
    const totalElement = document.querySelector('.total-price');
    if (totalElement) totalElement.textContent = `Total: $${total.toFixed(2)}`;
    const totalPriceSpan = document.getElementById('total-price');
    if (totalPriceSpan) totalPriceSpan.textContent = total.toFixed(2);
    const itemCountSpan = document.getElementById('item-count');
    if (itemCountSpan) itemCountSpan.textContent = itemCount;
    console.log('Displayed cart');
}

document.addEventListener('DOMContentLoaded', function() {
    getCart();
    displayCart();
    console.log('Cart initialized');


    const viewCloseBtn = document.querySelector('.cart-header .close-btn');
    if (viewCloseBtn) {
        viewCloseBtn.onclick = function() {
            window.location.href = 'homepage.html';
        };
    }
    const viewCheckoutBtn = document.querySelector('.cart-footer .checkout-btn');
    if (viewCheckoutBtn) {
        viewCheckoutBtn.onclick = function() {
            window.location.href = 'checkout.html';
        };
    }


    const modal = document.getElementById('product-modal');
    const closeBtn = document.querySelector('.close-btn');
    const modalImage = document.getElementById('modal-image');
    const modalTitle = document.getElementById('modal-title');
    const modalPrice = document.getElementById('modal-price');
    const modalRating = document.getElementById('modal-rating');
    const modalAddToCart = document.getElementById('modal-add-to-cart');
    const modalBuyNow = document.getElementById('modal-buy-now');
    const qtyMinus = document.getElementById('qty-minus');
    const qtyPlus = document.getElementById('qty-plus');
    const qtyValue = document.getElementById('qty-value');

    let currentQty = 1;


    document.querySelectorAll('.product-card button').forEach(btn => {
        btn.addEventListener('click', function(event) {
            event.stopPropagation();
        });
    });


    qtyMinus.onclick = function() {
        if (currentQty > 1) {
            currentQty--;
            qtyValue.textContent = currentQty;
        }
    };

    qtyPlus.onclick = function() {
        currentQty++;
        qtyValue.textContent = currentQty;
    };


    document.querySelectorAll('.product-card').forEach(card => {
        card.addEventListener('click', function() {
            const img = this.querySelector('img');
            const title = this.querySelector('h3').textContent;
            const priceText = this.querySelector('p').textContent;
            const rating = this.querySelector('span').textContent;
            const price = parseFloat(priceText.replace('$', ''));

            const id = title.toLowerCase().replace(/\s+/g, '-');

            modalImage.src = img.src;
            modalTitle.textContent = title;
            modalPrice.textContent = priceText;
            modalRating.textContent = rating;

  
            currentQty = 1;
            qtyValue.textContent = currentQty;

     
            modalAddToCart.onclick = function() {
                for (let i = 0; i < currentQty; i++) {
                    addToCart({id: id, name: title, price: price, image: img.src});
                }
                modal.style.display = 'none';
            };

 
            modalBuyNow.onclick = function() {
                alert('Proceeding to checkout!');
                modal.style.display = 'none';
            };

            modal.style.display = 'block';
        });
    });


    closeBtn.onclick = function() {
        modal.style.display = 'none';
    };

    window.onclick = function(event) {
        if (event.target == modal) {
            modal.style.display = 'none';
        }
    };
});



function updateTotal() {
    console.log('Updating total');
    let total = 0;
    let itemCount = 0;
    document.querySelectorAll('.cart-item').forEach((item, index) => {
        const checkbox = item.querySelector('input[type="checkbox"]');
        if (checkbox && checkbox.checked && item.style.display !== 'none') {
            const qty = parseInt(item.querySelector('.qty-value').textContent);
            const price = parseFloat(item.querySelector('.price').textContent.replace('$', ''));
            total += price * qty;
            itemCount += qty;
            console.log('Item', index, 'contributes', price * qty, 'to total');
        }
    });
    document.getElementById('total-price').textContent = total.toFixed(2);
    document.getElementById('item-count').textContent = itemCount;
    console.log('Total:', total.toFixed(2), 'Items:', itemCount);
}