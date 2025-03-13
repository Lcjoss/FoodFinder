package FoodFinder.ui;

import FoodFinder.dao.UserProfileDAO;
import FoodFinder.domain.UserProfile;
import FoodFinder.session.UserSession;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SignUpPanel extends JPanel {
    private MainFrame parentFrame;
    // Define the common brand color.
    private final Color brandColor = new Color(0xFF6666);

    public SignUpPanel(MainFrame frame) {
        this.parentFrame = frame;
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        // Top title: "FoodFinder" in the brand color.
        JLabel titleLabel = new JLabel("FoodFinder", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setForeground(brandColor);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Center panel with a vertical BoxLayout.
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

        // Row 0: "Sign Up" label spanning two columns.
        JLabel signUpHeader = new JLabel("Sign Up");
        signUpHeader.setFont(new Font("Segoe UI", Font.BOLD, 20));
        signUpHeader.setForeground(brandColor);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(signUpHeader, gbc);
        gbc.gridwidth = 1;

        // Row 1: Username label.
        JLabel userLabel = new JLabel("Choose Username:");
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
        JLabel passLabel = new JLabel("Choose Password:");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        passLabel.setForeground(Color.BLACK);
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

        // Create Sign Up and Back buttons with the same fixed size.
        JButton signUpButton = new JButton("Sign Up");
        JButton backButton = new JButton("Back");
        Dimension buttonSize = new Dimension(150, 50);
        signUpButton.setPreferredSize(buttonSize);
        backButton.setPreferredSize(buttonSize);

        // Style the Sign Up button as primary (brandColor background, white text).
        signUpButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        signUpButton.setFocusPainted(false);
        signUpButton.setBackground(brandColor);
        signUpButton.setForeground(Color.WHITE);

        // Style the Back button with white background, brandColor text and border.
        backButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        backButton.setFocusPainted(false);
        backButton.setBackground(Color.WHITE);
        backButton.setForeground(brandColor);
        backButton.setBorder(BorderFactory.createLineBorder(brandColor));

        bottomPanel.add(signUpButton);
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Action: sign up new user.
        signUpButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(
                        SignUpPanel.this,
                        "Please enter both username and password.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            UserProfile user = UserProfileDAO.signUp(username, password);
            if (user != null) {
                // For sign-up, the new user will proceed through the normal selection process.
                UserSession.setCurrentUser(user);
                parentFrame.showPanel("search");
            } else {
                JOptionPane.showMessageDialog(
                        SignUpPanel.this,
                        "Sign up failed. Username may already exist.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // Action: go back to the user login panel.
        backButton.addActionListener(e -> parentFrame.showPanel("userLogin"));
    }
}
