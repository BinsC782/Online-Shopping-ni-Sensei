# Online Shopping Application - Technical Documentation

---

## **🚀 1. Project Setup and Execution**

### **Prerequisites**
- **Java JDK 11+** (Required for compilation and execution)
- **Apache Maven 3.6+** (For dependency management and build automation)
- **Git** (For version control, optional for basic execution)

### **Environment Setup**
1. Ensure Java JDK is installed and `JAVA_HOME` environment variable is set
2. Ensure Maven is installed and `MAVEN_HOME` environment variable is set
3. Verify installations:
   ```bash
   java -version
   mvn -version
   ```

### **Build & Run Commands**

#### **Clean Build (Skip Tests)**
```bash
mvn clean package -DskipTests
```
This command:
- Cleans previous build artifacts
- Compiles all Java source files
- Runs resource processing
- Creates executable JAR: `target/online-shopping-app-1.0.0.jar`

#### **Run the Application**
```bash
java -jar target/online-shopping-app-1.0.0.jar
```
**Expected Output:**
```
Initializing data files...
Creating sample products...
Starting server...
Server started on http://localhost:8080
Press Ctrl+C to stop the server...
```

### **Access & Port Behavior**
- **Default URL:** `http://localhost:8080`
- **Port Selection:** Server automatically finds first available port in range 8080-8090
- **Port File:** Actual port saved to `server_port.txt` for reference
- **Auto-Launch:** Browser should open automatically (Windows batch file feature)

---

## **🏗️ 2. Application Architecture Overview**

### **Server Core**
The application uses Java's built-in lightweight HTTP server (`com.sun.net.httpserver.HttpServer`) instead of heavier frameworks like Spring Boot. This design choice provides:

- **Fast Startup:** No complex framework initialization
- **Low Resource Usage:** Minimal memory footprint
- **Simple Deployment:** Single JAR file execution

**Key Configuration (`ServerMain.java`):**
```java
HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
```

### **Request Flow**
When a browser request arrives at `http://localhost:8080`:

1. **Port Reception:** HttpServer receives request on configured port (8080+)
2. **Route Matching:** Server matches URL path to registered handlers:
   - `/` → `StaticFileHandler` (serves HTML/CSS/JS)
   - `/api/products` → `ProductsHandler` (product CRUD operations)
   - `/api/auth/login` → `LoginHandler` (user authentication)
   - `/api/auth/register` → `RegisterHandler` (user registration)
   - `/api/cart` → `CartHandler` (shopping cart management)
   - `/api/orders` → `OrdersHandler` (order processing)

3. **Handler Processing:** Appropriate handler processes request and generates response
4. **Response Generation:** Handler sends HTTP response back to client

### **Data Handling**
**Jackson ObjectMapper** handles JSON conversion between HTTP requests/responses and Java objects:

```java
// Handler receives JSON request body
String requestBody = ServerMain.readBody(exchange);
// Deserialize JSON to Java object
Map<String, Object> requestData = ServerMain.parseJsonToMap(requestBody);

// Handler processes business logic
Product product = shoppingService.createProduct(productData);

// Handler returns JSON response
String jsonResponse = JsonUtils.toJson(product);
ServerMain.sendJsonResponse(exchange, 200, jsonResponse);
```

### **Static Assets**
Static files (HTML, CSS, JS) are served from the `/web` directory:

- **Root Path (`/`):** Redirects to `/index.html`
- **File Resolution:** Maps `/web/filename.ext` to HTTP requests
- **Content-Type Detection:** Automatic MIME type detection based on file extension
- **Security:** Path traversal protection prevents access outside `/web` directory

---

## **🧩 3. Key Components and Code Reference**

### **com.shopping.ServerMain.java**
**Sole Responsibility: Server Configuration**

This class handles only server bootstrapping and configuration:
- **Port Discovery:** Finds available port in range 8080-8090
- **Server Creation:** Initializes HttpServer with thread pool
- **Handler Registration:** Maps URL patterns to handler classes
- **Lifecycle Management:** Starts server and handles graceful shutdown

**Critical Code Pattern:**
```java
// Server setup and handler registration
HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
server.createContext("/", new StaticFileHandler());
server.createContext("/api/products", new ProductsHandler(shoppingService));
server.start();

// Keep-alive mechanism
Thread.currentThread().join(); // Prevents immediate exit
```

### **com.shopping.handlers.* Classes**
**Primary Pattern: HTTP Request Processing**

All handler classes follow the same pattern:

1. **Request Reception:** Extract data from HttpExchange
2. **Input Validation:** Validate request parameters and JSON data
3. **Business Logic:** Call appropriate service methods
4. **Response Generation:** Format and send HTTP response

**Handler Structure Example:**
```java
public class ProductsHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 1. Parse request
        String method = exchange.getRequestMethod();
        String requestBody = ServerMain.readBody(exchange);

        // 2. Process based on HTTP method
        switch (method) {
            case "GET":
                List<Product> products = shoppingService.getProducts();
                ServerMain.sendJsonResponse(exchange, 200, JsonUtils.toJson(products));
                break;
            case "POST":
                // Create new product logic
                break;
        }
    }
}
```

### **com.shopping.util.ValidationUtil.java**
**Security and Data Integrity**

This utility provides centralized input validation across the application:

- **Password Validation:** Ensures strong passwords with regex pattern
- **Email Validation:** Validates email format using regex
- **Input Sanitization:** Prevents injection attacks and malformed data
- **Type Safety:** Validates data types and ranges

**Security Benefits:**
- **Prevents SQL Injection:** Validates input before database operations
- **Data Consistency:** Ensures all inputs meet business requirements
- **User Experience:** Provides clear error messages for invalid input

---

## **💾 4. Database (or Mock) Integration and Data Flow**

### **Data Storage Architecture**
**Current Implementation:** File-based storage using text files
- **Users:** `users.txt` (username,password,email format)
- **Products:** `products.txt` (CSV format with product details)
- **Orders:** `orders.txt` (order data and status tracking)

**FileHandler.java** manages all data persistence:
```java
public class FileHandler {
    // Data access methods
    public List<Product> loadProducts() throws IOException
    public void saveProducts(List<Product> products) throws IOException
    public boolean registerUser(String username, String password, String email)
    public User authenticateUser(String username, String password)
}
```

### **CRUD Operations Flow**

#### **Example: "Add Product" Operation**

1. **Request Reception:**
   ```
   POST /api/products
   Content-Type: application/json
   {"name":"Laptop","price":999.99,"description":"Gaming laptop"}
   ```

2. **Handler Processing (`ProductsHandler.java`):**
   ```java
   // Deserialize JSON to Map
   Map<String, Object> productData = ServerMain.parseJsonToMap(requestBody);

   // Convert to Product object (Jackson deserialization)
   Product product = JsonUtils.fromJson(jsonString, Product.class);

   // Validate input
   ValidationUtil.validateStringField("name", product.getName(), 1, 100);
   ```

3. **Service Layer (`ShoppingService.java`):**
   ```java
   public Product createProduct(Product product) {
       // Business logic validation
       if (product.getPrice() <= 0) {
           throw new IllegalArgumentException("Price must be positive");
       }

       // Generate unique ID
       String productId = fileHandler.generateProductId();
       product.setId(productId);

       // Save to file
       List<Product> products = fileHandler.loadProducts();
       products.add(product);
       fileHandler.saveProducts(products);

       return product;
   }
   ```

4. **Response Generation:**
   ```java
   // Serialize Product to JSON
   String jsonResponse = JsonUtils.toJson(createdProduct);
   ServerMain.sendJsonResponse(exchange, 201, jsonResponse);
   ```

---

## **✅ 5. Testing and Maintenance Guide**

### **Unit Testing (JUnit/Mockito)**

#### **Running Tests**
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ValidationUtilTest

# Run with verbose output
mvn test -X
```

#### **Mockito Usage Pattern**
```java
@Test
void testProductCreation() {
    // Create mock dependencies
    FileHandler mockFileHandler = Mockito.mock(FileHandler.class);
    ShoppingService service = new ShoppingService(mockFileHandler);

    // Setup mock behavior
    when(mockFileHandler.loadProducts()).thenReturn(new ArrayList<>());

    // Test business logic in isolation
    Product product = service.createProduct(testProduct);

    // Verify interactions
    verify(mockFileHandler).saveProducts(anyList());
}
```

### **Integration Testing**

#### **ServerMainIntegrationTest Purpose**
- **Live Server Verification:** Tests actual HTTP server behavior
- **CORS Headers:** Validates cross-origin resource sharing setup
- **Routing:** Ensures URL paths map to correct handlers
- **Error Handling:** Tests 404 responses and malformed requests

#### **Test Structure:**
```java
@Test
void testCorsHeaders() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8081/"))
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

    // Verify CORS headers exist
    assertNotNull(response.headers().firstValue("Access-Control-Allow-Origin"));
}
```

### **Debugging Guide**

#### **Attach Debugger to Running Application**
1. **Set Breakpoint:** In IDE, set breakpoint in desired method (e.g., `ProductsHandler.handle()`)
2. **Start Application:** Run `java -jar target/online-shopping-app-1.0.0.jar`
3. **Attach Debugger:**
   - IntelliJ IDEA: Run → Attach to Process → Select Java process
   - VS Code: Use "Java Debug" extension with process attach
4. **Trigger Request:** Make HTTP request to hit breakpoint

#### **Common Debugging Scenarios**
- **Server Not Starting:** Check `server_port.txt` for actual port number
- **Handler Not Called:** Verify URL path matches registered context
- **JSON Parse Errors:** Check request Content-Type header is `application/json`
- **File I/O Issues:** Verify `data/` directory exists and has write permissions

#### **Log Analysis**
Server logs appear in console:
```
[2024-01-15 10:30:15] GET /api/products
[2024-01-15 10:30:15] POST /api/auth/login
```

---

## **🎯 Summary**

This documentation provides comprehensive guidance for understanding, maintaining, and extending the online shopping application. The lightweight Java HTTP server architecture combined with modern testing practices ensures reliable, maintainable code that can be easily deployed and scaled.

For questions or contributions, refer to the project structure and follow the established patterns for handlers, services, and data access layers.