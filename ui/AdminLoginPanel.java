package FoodFinder.ui;

import FoodFinder.dao.AdminProfileDAO;
import FoodFinder.domain.AdminProfile;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AdminLoginPanel extends JPanel {
    private MainFrame parentFrame;
    private final Color brandColor = new Color(0xFF6666);

    public AdminLoginPanel(MainFrame frame) {
        this.parentFrame = frame;
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        // Top title: "FoodFinder" in brand color.
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

        // Row 0: "Administrator Login" label spanning two columns.
        JLabel loginLabel = new JLabel("Administrator Login");
        loginLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        loginLabel.setForeground(brandColor);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(loginLabel, gbc);
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

        // Bottom panel for action buttons.
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(Color.WHITE);

        // Create Login and Back buttons with the same fixed size.
        JButton loginButton = new JButton("Login");
        JButton backButton = new JButton("Back");
        Dimension buttonSize = new Dimension(150, 50);
        loginButton.setPreferredSize(buttonSize);
        backButton.setPreferredSize(buttonSize);

        // Style Login button: brandColor background with white text.
        loginButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        loginButton.setFocusPainted(false);
        loginButton.setBackground(brandColor);
        loginButton.setForeground(Color.WHITE);

        // Style Back button: white background, brandColor text and border.
        backButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        backButton.setFocusPainted(false);
        backButton.setBackground(Color.WHITE);
        backButton.setForeground(brandColor);
        backButton.setBorder(BorderFactory.createLineBorder(brandColor));

        bottomPanel.add(loginButton);
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Action: Authenticate admin.
        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(
                        AdminLoginPanel.this,
                        "Please enter both username and password.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            AdminProfile admin = AdminProfileDAO.authenticate(username, password);
            if (admin != null) {
                parentFrame.showPanel("adminPage");
            } else {
                JOptionPane.showMessageDialog(
                        AdminLoginPanel.this,
                        "Invalid administrator credentials.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // Action: Go back to the user login panel.
        backButton.addActionListener(e -> parentFrame.showPanel("userLogin"));
    }
}
