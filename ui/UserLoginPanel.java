package FoodFinder.ui;

import FoodFinder.dao.UserProfileDAO;
import FoodFinder.domain.UserProfile;
import FoodFinder.session.UserSession;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class UserLoginPanel extends JPanel {
    private MainFrame parentFrame;
    private final Color brandColor = new Color(0xFF6666);

    public UserLoginPanel(MainFrame frame) {
        this.parentFrame = frame;
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        // Top title now shows "FoodFinder" in the brand color.
        JLabel titleLabel = new JLabel("FoodFinder", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setForeground(brandColor);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Center panel using vertical BoxLayout.
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Form panel using GridBagLayout.
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 0: "User Login" label spanning both columns.
        JLabel loginLabel = new JLabel("User Login");
        loginLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        loginLabel.setForeground(brandColor);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(loginLabel, gbc);

        // Reset gridwidth for following components.
        gbc.gridwidth = 1;

        // Row 1: Username label.
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        userLabel.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(userLabel, gbc);

        // Username text field.
        JTextField userField = new JTextField();
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        // Set foreground to match the label.
        userField.setForeground(Color.BLACK);
        gbc.gridx = 1;
        gbc.gridy = 1;
        formPanel.add(userField, gbc);

        // Row 2: Password label.
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(passLabel, gbc);

        // Password field.
        JPasswordField passField = new JPasswordField();
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        gbc.gridx = 1;
        gbc.gridy = 2;
        formPanel.add(passField, gbc);

        centerPanel.add(formPanel);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel for action buttons and admin link.
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(Color.WHITE);

        // Create Login and Sign Up buttons with the same fixed size.
        JButton loginButton = new JButton("Login");
        JButton signUpButton = new JButton("Sign Up");
        Dimension buttonSize = new Dimension(150, 50);
        loginButton.setPreferredSize(buttonSize);
        signUpButton.setPreferredSize(buttonSize);

        // Style Login button (brandColor background, white text).
        loginButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        loginButton.setFocusPainted(false);
        loginButton.setBackground(brandColor);
        loginButton.setForeground(Color.WHITE);

        // Style Sign Up button (white background, brandColor text and border).
        signUpButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        signUpButton.setFocusPainted(false);
        signUpButton.setBackground(Color.WHITE);
        signUpButton.setForeground(brandColor);
        signUpButton.setBorder(BorderFactory.createLineBorder(brandColor));

        // Admin login label.
        JLabel adminLoginLabel = new JLabel("<HTML><U>Login as administrator</U></HTML>");
        adminLoginLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        adminLoginLabel.setForeground(Color.BLACK);
        adminLoginLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        bottomPanel.add(loginButton);
        bottomPanel.add(signUpButton);
        bottomPanel.add(adminLoginLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // Action: attempt to authenticate the user.
        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(
                        UserLoginPanel.this,
                        "Please enter both username and password.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            UserProfile user = UserProfileDAO.authenticate(username, password);
            if (user != null) {
                UserSession.setCurrentUser(user);
                parentFrame.loadSearchPanel();
            } else {
                JOptionPane.showMessageDialog(
                        UserLoginPanel.this,
                        "Invalid credentials.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // Action: go to sign-up panel.
        signUpButton.addActionListener(e -> parentFrame.showPanel("signUp"));

        // Action: switch to administrator login panel when clicking the underlined label.
        adminLoginLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                parentFrame.showPanel("adminLogin");
            }
        });
    }
}
