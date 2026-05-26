module com.hammroschool {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.hammroschool to javafx.fxml;
    opens com.hammroschool.controller to javafx.fxml;
    exports com.hammroschool;
}