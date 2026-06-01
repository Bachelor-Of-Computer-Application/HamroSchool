package com.hammroschool.controller;

import com.hammroschool.model.auth.UserAccount;
import com.hammroschool.model.auth.UserRole;
import com.hammroschool.service.AuthService;
import com.hammroschool.service.impl.InMemoryAuthService;
import com.hammroschool.util.SceneSwitcher;
import com.hammroschool.util.SessionContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AdminController {
    private final AuthService authService = InMemoryAuthService.getInstance();

    @FXML
    private Label welcomeLabel;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<UserRole> roleChoiceBox;

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<String> accountListView;

    @FXML
    public void initialize() {
        roleChoiceBox.setItems(FXCollections.observableArrayList(UserRole.values()));
        roleChoiceBox.getSelectionModel().select(UserRole.TEACHER);
        refreshView();
    }

    @FXML
    private void handleCreateAccount() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        UserRole role = roleChoiceBox.getValue();

        if (authService.createAccount(username, password, role)) {
            statusLabel.setText("Account created for " + username + " as " + role.getDisplayName() + ".");
            usernameField.clear();
            passwordField.clear();
            roleChoiceBox.getSelectionModel().select(UserRole.TEACHER);
            refreshView();
        } else {
            statusLabel.setText("Username is required, password is required, and the username must be unique.");
        }
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(welcomeLabel, "/com/hammroschool/hello-view.fxml", "Hammro School", 920, 720);
    }

    private void refreshView() {
        UserAccount currentUser = SessionContext.getInstance().getCurrentUser().orElseThrow();
        welcomeLabel.setText("Welcome, " + currentUser.getUsername());
        accountListView.setItems(FXCollections.observableArrayList(
                authService.getAccounts().stream()
                        .map(account -> account.getUsername() + "  |  " + account.getRole().getDisplayName())
                        .toList()
        ));
    }
}