# Project Changes and Concepts

## Summary
- Migrated from a CLI-first Java app to a localhost-only HTTP server + service layer + static web frontend.
- Removed unused Swing UI, reorganized web assets, and added a robust file-backed API.
- Introduced an image field for products and improved CSV parsing.

## Key Changes

- **Service Layer (Business Logic)**
  - Added `src/com/shopping/ShoppingService.java` to centralize logic:
    - `getProducts()`, `searchProducts(term)`
    - `authenticateUser(u,p)`, `registerUser(u,p,e)`
    - `placeOrder(username, List<OrderItem>)` with atomic stock decrement and order persistence
  - Uses a lock to ensure order placement is thread-safe.

- **HTTP Server (Localhost only)**
  - `src/com/shopping/ServerMain.java` serves:
    - Static files from `../web/` (e.g., `index.html`, CSS, JS, Photos)
    - JSON APIs via `/api/*` routes using `ShoppingService`:
      - `GET /api/products`
      - `POST /api/auth/login`
      - `POST /api/users`
      - `POST /api/orders`
  - Port fallback 8080→8090 and auto-open browser via `run-server.bat`.

- **Data Parsing & Persistence**
  - `src/com/shopping/data/FileHandler.java`:
    - Replaced naive `split(',')` with a CSV-aware `parseCsvLine()` (supports quoted commas and escaped quotes).
    - Loads/saves optional product image at field index 10.
    - Continues to use text files under `src/` as the source of truth (`products.txt`, `users.txt`, `orders.txt`).

- **Model Updates**
  - `src/com/shopping/model/Product.java`:
    - Added `image` field with getters/setters.
    - Fixed `getReviews()` to return the field value.

- **Frontend (Static Web)**
  - Moved assets to `web/` with `CSS/`, `Photos/`, and pages.
  - `web/main.js`:
    - Step 1: Safely fetches `/api/products` to build a name→id map.
    - Step 2: On `checkout.html`, posts the cart to `/api/orders` using a compact items string.
    - Step 3: Renders the product grid from the API (uses `image` if present, otherwise name-based guess).
    - Keeps localStorage cart logic and modal interactions.

- **Scripts**
  - `run-server.bat`: compiles server and opens browser to the auto-selected port.
  - `compile.bat`: compiles CLI/core (CLI now deprecated; server is preferred).

- **Removed/Deprecated**
  - Deleted Swing UI (`src/com/shopping/ui/`), updated `compile.bat` accordingly.
  - CLI `OnlineShoppingApp.java` is no longer required for the web path (can be kept for reference or removed later).

## Concepts Covered

- **HTML/CSS**: Semantic structure, responsive layout, asset paths.
- **JavaScript**: DOM manipulation, events, localStorage, fetch API, graceful fallbacks, modular UI logic.
- **HTTP/REST**: Methods (GET/POST), status codes, JSON payloads, static vs dynamic routes.
- **Java Architecture**: Separation of concerns (Service vs Transport), DTOs, models, packaging.
- **File I/O & CSV Parsing**: Robust parsing (quoted fields), reading/writing text files as persistence.
- **Concurrency**: Multi-threaded server, synchronized order placement to avoid race conditions.
- **Dev Ergonomics**: Port fallback, auto-open browser, concise logging, scripts for run/compile.

## Data Formats

- **products.txt**
  - Required fields (first 6): `id,name,price,description,category,stock`
  - Optional fields: `sellerName,sellerLocation,rating,reviews,image`
  - Example:
    ```
    1,Attack Shark Keyboard,19.99,"Tactile keys",Tech & Gadgets,50,Acme,Metro,4.5,"Great entry board",Attack Shark Keyboard.jpg
    2,Garuda Wireless Mouse,9.99,"Lightweight",Tech & Gadgets,100,,,,,GarudaHawk.jpg
    ```

- **orders.txt**
  - Format: `orderId,user,productList,status`
  - Example productList: `1x2;3x1`

- **users.txt**
  - Format: `username,password,email`

## How to Run (current)

1. Double-click `run-server.bat` at the repository root.
2. The script compiles the server, selects a free port, and opens the browser.
3. Visit `/api/products` to verify JSON output, then open `/homepage.html`.
4. Use the UI to add items and check out. Orders append to `src/orders.txt` and stock decrements in `src/products.txt`.

## Next Steps (optional)

- Wire `web/index.html` forms to `POST /api/auth/login` and `POST /api/users`, store `localStorage.username`.
- Move DTOs into a `dto/` package for clarity.
- Add more validations (e.g., max qty per item) and better error messages.
- Replace text files with a database when ready (keeping `ShoppingService` API stable).
