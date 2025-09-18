# Online Shopping App


## [09/18/2025] - OnlineShopping Beta V0.1 - ChewieYodayahh commit 
- Simple CLI application that allows users to register, login, and browse products.
- Disabled Swing GUI features until further consideration to develop into a Web Application (integrating both HTML, CSS, and JavaScript + Java as backend). 
- Refer always to the Gantt Chart for new tasks religiously, and coordinate with the Group Chat
hfh



### Folder Structure - 
This is only the introduction so you may understand the rest. After reading this, please also hover to Project Structure to see the actual folder structure.
- Data folder (package com.shoppinig.data) is used to store data to .txt files, manage data persistence, and update. CRUD
- Model folder (package com.shopping.model) is most important. It is the core business logic contains objects such as Products, Users, Orders, etc.
- UI folder (package com.shopping.ui) is used to create the Swing GUI user interface. It is not yet launched by default. There are also additional business logic such as CartUI, CheckOutUI, ProductUI, LoginUI for user-authentication, and MainProgramUI for the main frame container. 


## Overview
The Online Shopping App is a Java-based application that simulates an online shopping experience. It includes a console (CLI) application as the primary entrypoint, as well as a set of Swing UI components (login, product browsing, cart, and checkout) that are compiled but not yet launched by default.

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