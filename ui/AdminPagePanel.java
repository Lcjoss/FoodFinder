package FoodFinder.ui;

import javax.swing.*;
import java.awt.*;

public class AdminPagePanel extends JPanel {
    public AdminPagePanel() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Admin Page", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 32));
        add(label, BorderLayout.CENTER);
    }
}
