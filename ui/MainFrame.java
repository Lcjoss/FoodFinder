package FoodFinder.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardPanel;

    public MainFrame() {
        setTitle("FoodFinder");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Create the login/sign-up and admin panels.
        UserLoginPanel userLoginPanel = new UserLoginPanel(this);
        AdminLoginPanel adminLoginPanel = new AdminLoginPanel(this);
        SignUpPanel signUpPanel = new SignUpPanel(this);
        AdminPagePanel adminPagePanel = new AdminPagePanel();

        // Create a placeholder panel for the search screen.
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.add(new JLabel("Please log in to start.", SwingConstants.CENTER), BorderLayout.CENTER);

        // Add panels to the card panel.
        cardPanel.add(userLoginPanel, "userLogin");
        cardPanel.add(adminLoginPanel, "adminLogin");
        cardPanel.add(signUpPanel, "signUp");
        cardPanel.add(placeholder, "search");  // initial placeholder
        cardPanel.add(adminPagePanel, "adminPage");

        add(cardPanel);
    }

    // Method to switch to a particular card.
    public void showPanel(String panelName) {
        cardLayout.show(cardPanel, panelName);
    }

    /**
     * After a successful login, call this method to load the search panel.
     * It creates a new FoodFinderFrame, calls its initPanels() method (which builds
     * the selection panels using the current user's stored preferences), and then
     * adds its content to the card panel under "search" and shows it.
     */
    public void loadSearchPanel() {
        FoodFinderFrame foodFinderFrame = new FoodFinderFrame();
        foodFinderFrame.initPanels();  // Build the panels using UserSession data.
        // Replace the placeholder (or any previous search panel) with the new search panel.
        cardPanel.add(foodFinderFrame.getContentPane(), "search");
        showPanel("search");
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
