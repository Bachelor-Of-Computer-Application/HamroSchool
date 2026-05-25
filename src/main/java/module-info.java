module hammroSchool {
    requires javafx.controls;
    requires javafx.fxml;

    opens hammroSchool to javafx.fxml;
    exports hammroSchool;
}