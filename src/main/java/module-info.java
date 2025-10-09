module com.shopping {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires java.logging;
    requires java.base;

    opens com.shopping to javafx.fxml, javafx.graphics;

    exports com.shopping;
}