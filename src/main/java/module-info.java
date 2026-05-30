module com.hammroschool {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.h2database;

    opens com.hammroschool to javafx.fxml;
    opens com.hammroschool.controller to javafx.fxml;
    exports com.hammroschool;
}