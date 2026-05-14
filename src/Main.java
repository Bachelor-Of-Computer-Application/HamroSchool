import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    JTextField usernameField;
    JPasswordField passwordField;
    JLabel message;

    public Main() {

        setTitle("Java GUI Login");
        setSize(350, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(7, 1, 10, 10));

        // Title
        JLabel title = new JLabel("Login Form", SwingConstants.CENTER);

        // Username
        JLabel userLabel = new JLabel("Username");
        usernameField = new JTextField();

        // Password
        JLabel passLabel = new JLabel("Password");
        passwordField = new JPasswordField();

        // Button
        JButton loginBtn = new JButton("Login");

        // Message
        message = new JLabel("", SwingConstants.CENTER);

        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add Components
        panel.add(title);

        panel.add(userLabel);
        panel.add(usernameField);

        panel.add(passLabel);
        panel.add(passwordField);

        panel.add(loginBtn);
        panel.add(message);

        add(panel);

        // Button Action
        loginBtn.addActionListener(e -> login());
    }

    private void login() {

        String username = usernameField.getText();
        String password = String.valueOf(passwordField.getPassword());

        if (!username.equals(DB.USERNAME)) {
            message.setText("Invalid Username");
            return;
        }

        if (!password.equals(DB.PASSWORD)) {
            message.setText("Invalid Password");
            return;
        }

        if (!isValidPassword(password)) {
            message.setText("Password validation failed");
            return;
        }

        // Open New Screen
        openNewScreen();

        // Close Current Window
        dispose();
    }

    // Password Validation
    private boolean isValidPassword(String password) {

        int letterCount = 0;
        int numberCount = 0;
        int symbolCount = 0;

        for (char ch : password.toCharArray()) {

            if (Character.isLetter(ch)) {
                letterCount++;
            }
            else if (Character.isDigit(ch)) {
                numberCount++;
            }
            else {
                symbolCount++;
            }
        }

        return letterCount >= 4 &&
                numberCount > 3 &&
                symbolCount >= 1;
    }

    // New Screen
    private void openNewScreen() {

        JFrame newFrame = new JFrame("Welcome");

        JLabel helloLabel = new JLabel("Hello World", SwingConstants.CENTER);

        helloLabel.setFont(new Font("Arial", Font.BOLD, 24));

        newFrame.add(helloLabel);

        newFrame.setSize(400, 200);
        newFrame.setLocationRelativeTo(null);
        newFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        newFrame.setVisible(true);
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}