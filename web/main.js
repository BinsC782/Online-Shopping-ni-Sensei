let cart = [];

function getCart() {
    const storedCart = localStorage.getItem('shoppingCart');
    cart = storedCart ? JSON.parse(storedCart) : [];
    console.log('Retrieved cart:', cart);
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
}

function removeItem(id) {
    cart = cart.filter(item => item.id !== id);
    saveCart();
}

function toggleChecked(id) {
    const item = cart.find(item => item.id === id);
    if (item) {
        item.checked = !item.checked;
        saveCart();
    }
}

function getTotal() {
    return cart.reduce((total, item) => total + (item.price * item.quantity), 0);
}

function getCartCount() {
    return cart.reduce((count, item) => count + item.quantity, 0);
}

function displayCart() {
    const cartItems = document.getElementById('cart-items');
    if (!cartItems) return;

    cartItems.innerHTML = '';

    if (cart.length === 0) {
        cartItems.innerHTML = '<p>Your cart is empty</p>';
        return;
    }

    cart.forEach(item => {
        const itemElement = document.createElement('div');
        itemElement.className = 'cart-item';
        itemElement.innerHTML = `
            <img src="${item.image}" alt="${item.name}">
            <div class="item-details">
                <h3>${item.name}</h3>
                <p>$${item.price.toFixed(2)}</p>
                <div class="quantity">
                    <button onclick="updateQuantity('${item.id}', ${item.quantity - 1})">-</button>
                    <span>${item.quantity}</span>
                    <button onclick="updateQuantity('${item.id}', ${item.quantity + 1})">+</button>
                </div>
                <button onclick="removeItem('${item.id}')" class="remove-btn">Remove</button>
            </div>
        `;
        cartItems.appendChild(itemElement);
    });

    updateTotal();
    console.log('Displayed cart');
}

document.addEventListener('DOMContentLoaded', function() {
    getCart();
    displayCart();
    console.log('Cart initialized');

    const viewCloseBtn = document.querySelector('.cart-header .close-btn');
    if (viewCloseBtn) {
        viewCloseBtn.onclick = function() {
            window.history.back();
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
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    };
});

function updateTotal() {
    console.log('Updating total');
    let total = 0;
    let itemCount = 0;

    cart.forEach(item => {
        total += item.price * item.quantity;
        itemCount += item.quantity;
    });

    console.log('Total:', total);
    console.log('Item count:', itemCount);

    const totalElement = document.getElementById('total-price');
    const countElement = document.getElementById('item-count');

    if (totalElement) totalElement.textContent = total.toFixed(2);
    if (countElement) countElement.textContent = itemCount;
}