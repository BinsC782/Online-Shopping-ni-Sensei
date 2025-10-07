# Online Shopping App

A native desktop shopping application built with JavaFX and Java backend, transformed from the original web-based version while preserving all business logic and design elements.

## Features
- User registration and authentication
- Product browsing and search with high-quality product images
- Shopping cart functionality with persistent storage
- Order management with CSV-based data persistence
- Native desktop interface with preserved original design
- Responsive layout optimized for desktop use

## Prerequisites
- Java 17 or later (JavaFX-compatible version)
- JavaFX SDK 17.0.2 or compatible version

## Architecture Transformation

### From Web Application to Desktop Application

**Original Web Architecture:**
```
HTML Pages → Browser Navigation → JavaScript → HTTP API → Java Backend
├── index.html (Login/Registration)
├── homepage.html (Product Listing)
├── Viewing cart.html (Shopping Cart)
└── checkout.html (Checkout Process)
```

**New Desktop Architecture:**
```
JavaFX Screens → Event-Driven UI → Direct Java Calls → Same Java Backend
├── Single Main Window (ShoppingApplication.java)
├── Main Shopping View (MainView.fxml + MainController.java)
├── Modal Dialogs (Product Details, Checkout)
└── Sidebar Components (Cart Display, Categories)
```

### Key Transformation Benefits

| **Aspect** | **Web Version** | **Desktop Version** |
|------------|-----------------|-------------------|
| **Navigation** | Page reloads, URL changes | Instant view switching, single window |
| **Performance** | HTTP requests for data | Direct method calls, no network latency |
| **User Experience** | Browser-based, multi-tab | Native desktop feel, integrated workflow |
| **Images** | Web-hosted, HTTP loaded | Local resources, instant loading |
| **Data Persistence** | Same CSV files, same FileHandler | Same CSV files, same FileHandler |

## Getting Started

### Prerequisites Installation

1. **Install Java JDK 17+**
   ```bash
   # Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
   # Verify installation:
   java -version
   javac -version
   ```

2. **Install JavaFX SDK**
   ```bash
   # Download JavaFX 17.0.2 from: https://gluonhq.com/products/javafx/
   # Extract to: C:\Program Files\Java\javafx-sdk-17.0.2\
   ```

### Building the Application

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/online-shopping-app.git
   cd online-shopping-app
   ```

2. **Build the application**
   ```bash
   compile.bat
   ```
   This compiles all Java source files with JavaFX module support and places compiled classes in the `target\classes` directory.

### Running the Application

1. **Start the desktop application**
   ```bash
   run.bat
   ```
   The JavaFX application window will open automatically.

2. **Application Window**
   - Main shopping interface with product grid
   - Search functionality and category filtering
   - Shopping cart sidebar (always accessible)
   - Modal dialogs for product details and checkout

### Development Workflow

1. **Make code changes** in your IDE (VS Code recommended)
2. **Compile**: `compile.bat`
3. **Run**: `run.bat`
4. **Test** the desktop application functionality

### VS Code Development Setup

1. **Open project in VS Code**
2. **Install recommended extensions**:
   - Extension Pack for Java
   - JavaFX Support

3. **Configure JavaFX paths** in `.vscode/settings.json`:
   ```json
   {
     "java.project.sourcePaths": ["src/main/java"],
     "java.project.referencedLibraries": ["lib/**/*.jar"]
   }
   ```

## Project Structure

```
Online Shopping Desktop Application/
├── src/main/
│   ├── java/com/shopping/
│   │   ├── ShoppingApplication.java    # Main JavaFX application class
│   │   ├── MainController.java         # UI controller for main view
│   │   ├── model/                      # Data models (Product, User, Order, Cart)
│   │   │   ├── Product.java
│   │   │   ├── User.java
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   └── Cart.java
│   │   ├── service/                    # Business logic layer
│   │   │   └── ShoppingService.java
│   │   └── data/                       # Data access layer
│   │       └── FileHandler.java
│   └── resources/
│       ├── fxml/
│       │   └── MainView.fxml           # Main application layout
│       ├── css/
│       │   └── application.css         # JavaFX styling (preserved original design)
│       └── images/                     # Product images (moved from web/Photos/)
├── lib/                                # JavaFX SDK libraries
├── target/classes/                     # Compiled Java classes
├── *.txt                               # CSV data files (products.txt, users.txt, orders.txt)
├── compile.bat                         # Build script with JavaFX support
├── run.bat                             # Run script for JavaFX application
└── README.md                           # This documentation
```

## Key Components

### Desktop Interface (JavaFX)
- **ShoppingApplication.java**: Main JavaFX application entry point
- **MainView.fxml**: FXML layout definition for the main window
- **MainController.java**: Java controller handling UI events and business logic integration
- **application.css**: JavaFX CSS styling preserving original web design colors and layout

### Business Logic (Preserved from Web Version)
- **ShoppingService.java**: Core business logic for product management, cart operations, and order processing
- **FileHandler.java**: CSV file I/O operations for data persistence
- **Data Models**: Product, User, Order, OrderItem, Cart classes with identical functionality

### Data Persistence (Unchanged)
- **CSV-based storage** using the same text files (products.txt, users.txt, orders.txt)
- **Same FileHandler** class for reading/writing CSV data
- **Identical data format** - no changes to data structure or storage logic

## Design Preservation

### Visual Design Transformation

**Original Web Design Elements → JavaFX CSS:**
- ✅ **Color Scheme**: `#f5f5f5` background, `#2c3e50` branding colors preserved
- ✅ **Layout Structure**: Product grid, navigation bar, search functionality maintained
- ✅ **Typography**: Segoe UI font family, same sizing and hierarchy
- ✅ **Interactive Effects**: Hover animations, button styling, shadows replicated
- ✅ **Product Images**: All 15 product images properly mapped and displayed

### HTML Pages → JavaFX Screens

| **Original HTML Pages** | **JavaFX Implementation** | **Navigation Pattern** |
|------------------------|---------------------------|------------------------|
| `index.html` (Login) | Login screen (if needed) or direct main view | Single window, view switching |
| `homepage.html` (Products) | Main shopping view with product grid | Same window, content sections |
| `Viewing cart.html` | Cart sidebar (always visible) | Sidebar component, instant access |
| `checkout.html` | Modal checkout dialog | Popup dialog, focused workflow |

## Development Notes

### Preserved Elements (Critical - Do Not Modify)
- **Business Logic**: `ShoppingService.java` and data models unchanged
- **Data Persistence**: CSV file format and `FileHandler.java` identical
- **Core Functionality**: Product search, cart operations, order processing preserved

### Modified Elements (Desktop Adaptation)
- **UI Framework**: HTML/CSS/JS → JavaFX/FXML/CSS
- **Navigation**: Page-based → Component-based in single window
- **Event Handling**: DOM events → JavaFX event handlers
- **Image Loading**: HTTP image URLs → Local resource loading

### Testing Accounts (Unchanged)
- Admin: admin/admin123
- User: test/test123

## Troubleshooting

### JavaFX Setup Issues
1. **Verify JavaFX SDK path** in build scripts matches installation location
2. **Check module path** includes JavaFX libraries in `compile.bat` and `run.bat`
3. **Update JDK version** if using different Java version than 17

### Application Issues
1. **Data file location**: Ensure CSV files are in project root
2. **Image resources**: Product images should be in `src/main/resources/images/`
3. **Build errors**: Run `compile.bat` before `run.bat`

## Contributing

The desktop version maintains the same core functionality as the original web application while providing a native desktop experience. When making changes:

1. **Preserve business logic** in service and model classes
2. **Maintain data persistence** CSV format and FileHandler functionality
3. **Update JavaFX UI components** in FXML files and controllers
4. **Test desktop-specific features** like window management and native dialogs

## License

This project is licensed under the MIT License.