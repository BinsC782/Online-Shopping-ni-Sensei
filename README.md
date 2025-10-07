# Online Shopping App

A simple web-based shopping application with Java backend and HTML/CSS/JavaScript frontend.

## Features
- User registration and authentication
- Product browsing and search
- Shopping cart functionality
- Order management
- Responsive web interface

## Prerequisites
- Java 11 or later
- Web browser (Chrome, Firefox, Edge, etc.)

## Getting Started

### Building the Application

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/online-shopping-app.git
   cd online-shopping-app
   ```

2. **Build the application**
   ```bash
   build.bat
   ```
   This will compile all Java source files and place the compiled classes in the `bin` directory.

### Running the Application

1. **Start the server**
   ```bash
   run.bat
   ```
   The server will start on `http://localhost:8080` by default.

2. **Access the application**
   Open your web browser and navigate to:
   ```
   http://localhost:8080
   ```

### Running Tests

To run the test suite:
```bash
test.bat
```

### Cleaning Build Artifacts

To clean up compiled files and temporary directories:
```bash
clean.bat
```

## Project Structure

```
/
├── bin/                    # Compiled Java classes
├── bin-test/               # Compiled test classes
├── src/
│   ├── main/
│   │   ├── java/          # Java source files
│   │   │   └── com/shopping/
│   │   │       ├── config/ # Configuration classes
│   │   │       ├── data/   # Data access layer
│   │   │       ├── model/  # Data models
│   │   │       ├── util/   # Utility classes
│   │   │       └── ServerMain.java  # Main server class
│   │   └── resources/     # Resource files
│   └── test/              # Test source files
├── web/                   # Frontend files
│   ├── css/               # Stylesheets
│   ├── js/                # JavaScript files
│   └── *.html             # HTML pages
├── build.bat              # Build script
├── run.bat                # Run script
├── test.bat               # Test script
└── clean.bat              # Clean script
```

## Configuration

Server configuration can be modified by setting environment variables or creating an `app.properties` file in the root directory.

Example `app.properties`:
```properties
# Server configuration
server.port=8080
server.host=127.0.0.1

# File paths
users.file=users.txt
products.file=products.txt
orders.file=orders.txt

# Security
jwt.secret=your-secret-key
jwt.expiration.ms=86400000  # 24 hours

# CORS (comma-separated list of allowed origins)
cors.allowed.origins=http://localhost:8080,http://127.0.0.1:8080
```

## Development

### Adding New Features
1. Create a new branch for your feature
2. Make your changes
3. Add tests for your changes
4. Run tests to ensure everything works
5. Submit a pull request

### Debugging
- Server logs are output to the console
- Check the browser's developer console for frontend errors
- Use the browser's network tab to inspect API requests/responses

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Features
- User authentication and management
- Product browsing with detailed descriptions
- Category filtering and text search (in the GUI components)
- Shopping cart and checkout (GUI) with stock updates and order persistence
- File-based order management; CLI order history view is coming soon
- Data persistence via CSV-like text files

## Project Structure
```
OnlineShoppingApp/
├── README.md
├── compile.bat
├── run.bat
├── products.txt
├── users.txt
├── orders.txt
└── src/
    └── com/shopping/
        ├── OnlineShoppingApp.java            # CLI entrypoint (main)
        ├── data/
        │   └── FileHandler.java              # File I/O and parsing
        ├── model/
        │   ├── Product.java
        │   ├── User.java
        │   └── Order.java
        └── ui/
            ├── LoginUi/                      # Login/register dialog
            ├── MainProgram/                  # MainFrame container
            ├── ProductUi/                    # Product listing/search
            ├── CartUi/                       # Cart sidebar and view
            └── CheckOutUi/                   # Checkout dialog
```

Note: The project contains duplicate data files both at the repository root and under `src/`. Which set is used depends on the working directory when you run the app (see below).

## Setup Instructions
1. Ensure you have the Java Development Kit (JDK) installed.
2. Option A — Using the provided scripts (Windows):
   - Double-click `compile.bat` to compile.
   - Double-click `run.bat` to launch the CLI app.
   - These scripts change the working directory to `src`, so the app will read/write `products.txt`, `users.txt`, and `orders.txt` located under `src/`.
3. Option B — Manual compile/run:
   - From the project root:
     ```
     javac -Xlint:none src/com/shopping/OnlineShoppingApp.java src/com/shopping/data/FileHandler.java src/com/shopping/model/*.java src/com/shopping/ui/CartUi/*.java src/com/shopping/ui/CheckOutUi/*.java src/com/shopping/ui/LoginUi/*.java src/com/shopping/ui/ProductUi/*.java src/com/shopping/ui/MainProgram/*.java
     ```
   - To run the CLI app with `src` as the working directory:
     ```
     cd src
     java com.shopping.OnlineShoppingApp
     ```
     Running from `src` ensures the app uses the data files inside `src/`.

## Usage Guidelines
- CLI application (default):
  - Register or log in via the console prompts.
  - View and search products.
  - You can simulate placing an order by selecting a product and quantity; CLI order history display is planned.
- GUI components (compiled, not launched by default):
  - The Swing UI includes `LoginDialog`, `MainFrame`, `ProductPanel`, `CartPanel`, and `CheckoutDialog`.
  - Checkout in the GUI persists orders to `orders.txt` and updates product stock in `products.txt` via `FileHandler`.

## Contributing
Contributions are welcome! Please feel free to submit a pull request or open an issue for any suggestions or improvements.

## License
This project is licensed under the MIT License.