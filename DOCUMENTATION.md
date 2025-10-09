# PowerPuffGirls Shopping Application - Technical Documentation

## 1. Project Overview

This is a desktop online shopping cart application built using JavaFX and managed with Apache Maven.
* **Technologies:** Java, JavaFX 21, Maven
* **Primary Features:** Product viewing, Add-to-Cart functionality, Categorization, and file-based order logging.

## 2. Prerequisites

You must have the following software installed:
* **JDK:** Java Development Kit (version 17 or higher recommended, as JavaFX 21 is used).
* **Maven:** Apache Maven 3.x.x.

## 3. Setup and Launch

Follow these steps from the project root directory:

### A. Clean Build and Install
This step compiles the code and downloads all dependencies, including the required JavaFX modules.

```bash
mvn clean install
```

### B. Run the Application

This command uses the JavaFX Maven plugin to launch the application with the necessary JVM arguments.

```bash
mvn javafx:run
```

## 4. Critical Configuration and Fixes

The following non-standard configurations are necessary for successful launch, especially on certain operating systems (like Windows). These settings must be present in the pom.xml and module-info.java files.

### A. Graphics Fix (pom.xml)

The following argument must be present within the <configuration> block of the javafx-maven-plugin to resolve rendering issues:

```xml
<vmArgs>
    --add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED
    --add-opens=javafx.base/com.sun.javafx.binding=ALL-UNNAMED
    -Dprism.order=sw
</vmArgs>
```

### B. Module System Fix (module-info.java)

The main application package (com.shopping) and utility package (com.shopping.util) must be explicitly opened for reflective access by the FXML loader. The file should contain:

```java
module com.shopping {
    // ... requires clauses ...
    requires java.logging;
    
    // CRITICAL: FXML reflection access
    opens com.shopping to javafx.fxml;
    opens com.shopping.util to javafx.fxml;

    // ... exports clauses ...
}
```

## 5. Data & Output

Product Data: Products are currently hardcoded within the application (e.g., in MainController.java or an initialization method).
Order Output: When the Checkout button is pressed, order details are appended to the file named orders.txt in the project's root directory. The application then displays a confirmation dialog.
