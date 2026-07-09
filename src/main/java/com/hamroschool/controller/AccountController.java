package com.hamroschool.controller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.auth.UserRole;
import com.hamroschool.service.AuthService;
import com.hamroschool.service.impl.MongoAuthService;
import com.hamroschool.util.SceneSwitcher;
import com.hamroschool.util.SessionContext;
import com.hamroschool.util.Utils;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class AccountController {

    private static final int PAGE_SIZE = 7;

    private final AuthService authService = MongoAuthService.getInstance();

    private final ObservableList<UserAccount> allAccounts = FXCollections.observableArrayList();

    private List<UserAccount> filteredAccounts = List.of();

    private int currentPage = 0;


    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;
    @FXML private Label summaryLabel;

    @FXML private TextField searchField;

    @FXML private TableView<UserAccount>                 accountTable;
    @FXML private TableColumn<UserAccount, UserAccount>  userColumn;
    @FXML private TableColumn<UserAccount, String>       usernameColumn;
    @FXML private TableColumn<UserAccount, String>       roleColumn;
    @FXML private TableColumn<UserAccount, UserAccount>  actionsColumn;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button logoutButton;


    @FXML
    public void initialize() {
        setupTable();
        refreshCurrentUser();
        loadAccounts();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 0;
            applyFilterAndPage(newVal);
        });
    }


    private void setupTable() {
        userColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        userColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) { setGraphic(null); return; }

                Label initialsLabel = new Label(Utils.initials(account.getUsername()));
                initialsLabel.setStyle(
                    "-fx-background-color: #111111; -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; " +
                    "-fx-background-radius: 999; " +
                    "-fx-min-width: 30; -fx-min-height: 30; " +
                    "-fx-pref-width: 30; -fx-pref-height: 30; " +
                    "-fx-alignment: center;"
                );

                if (account.getRole() == UserRole.ADMIN) {
                    initialsLabel.setStyle(initialsLabel.getStyle());
                } else {
                    initialsLabel.setStyle(
                        "-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                        "-fx-font-size: 11px; -fx-font-weight: 800; " +
                        "-fx-background-radius: 999; " +
                        "-fx-min-width: 30; -fx-min-height: 30; " +
                        "-fx-pref-width: 30; -fx-pref-height: 30; " +
                        "-fx-alignment: center;"
                    );
                }

                Label nameLabel = new Label(Utils.formatName(account.getUsername()));
                nameLabel.setStyle("-fx-text-fill: #222222; -fx-font-size: 13px; -fx-font-weight: 700;");

                HBox container = new HBox(10, initialsLabel, nameLabel);
                container.setStyle("-fx-alignment: center-left;");
                setGraphic(container);
            }
        });

        usernameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUsername()));
        usernameColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setText(null); return; }
                setText(username);
                setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
            }
        });

        roleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRole().getDisplayName()));
        roleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); setText(null); return; }

                Label badge = new Label(role);
                boolean isAdmin = "Admin".equals(role);
                badge.setStyle(
                    "-fx-background-color: " + (isAdmin ? "#111111" : "#f0f0ef") + "; " +
                    "-fx-text-fill: "        + (isAdmin ? "white"   : "#333333") + "; " +
                    "-fx-padding: 4 12 4 12; -fx-background-radius: 999; " +
                    "-fx-font-size: 12px; -fx-font-weight: 700;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        actionsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) { setGraphic(null); return; }

                ImageView dotIcon = new ImageView(new Image(
                    Objects.requireNonNull(
                        getClass().getResourceAsStream(
                            "/com/hamroschool/assets/admin/account/three-dot.png"))));
                dotIcon.setFitHeight(16);
                dotIcon.setFitWidth(16);
                dotIcon.setPreserveRatio(true);

                Button btn = new Button();
                btn.setGraphic(dotIcon);
                btn.setStyle(
                    "-fx-background-color: transparent; -fx-cursor: hand; " +
                    "-fx-padding: 6 8 6 8; -fx-background-radius: 999;"
                );
                setGraphic(btn);
            }
        });

        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountTable.setPlaceholder(new Label("No accounts found"));

        accountTable.setStyle("-fx-background-color: transparent;");
    }


    private void loadAccounts() {
        allAccounts.setAll(authService.getAccounts());
        currentPage = 0;
        applyFilterAndPage(searchField.getText());
    }

    private void applyFilterAndPage(String query) {
        String q = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT));

        List<UserAccount> filtered = allAccounts.stream()
            .filter(a -> q.isEmpty()
                || a.getUsername().toLowerCase(Locale.ROOT).contains(q)
                || a.getRole().getDisplayName().toLowerCase(Locale.ROOT).contains(q))
            .toList();

        filteredAccounts = filtered;

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        accountTable.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));

        updateSummary(filtered.size());
        updatePaginationButtons(totalPages);
    }

    private void updateSummary(int filteredTotal) {
        int total = allAccounts.size();
        summaryLabel.setText("Showing " + filteredTotal + " of " + total + " accounts");
    }

    private void updatePaginationButtons(int totalPages) {
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
    }


    private void refreshCurrentUser() {
        SessionContext.getInstance().getCurrentUser().ifPresent(user -> {
            userInitialsLabel.setText(Utils.initials(user.getUsername()));
            userNameLabel.setText(user.getUsername());
        });
    }


    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            applyFilterAndPage(searchField.getText());
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredAccounts.size() / PAGE_SIZE));
        if (currentPage < totalPages - 1) {
            currentPage++;
            applyFilterAndPage(searchField.getText());
        }
    }

    @FXML
    private void handleNavDashboard() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/admin-view.fxml", "Admin Dashboard", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavClasses() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/class-view.fxml", "Classes", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavTeachers() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/teacher-view.fxml", "Teachers", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavStudents() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/student-view.fxml", "Students", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavSettings() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/settings-view.fxml", "Settings", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", SceneSwitcher.LOGIN_WIDTH, SceneSwitcher.LOGIN_HEIGHT);
    }
}
