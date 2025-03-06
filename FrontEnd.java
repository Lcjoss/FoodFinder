package FoodFinder;
import java.sql.*;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FrontEnd extends JFrame {
    // UI components
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JPanel progressPanel;
    private JLabel[] progressLabels;
    private int currentStep = 0;

    // Selection storage
    private List<String> selectedCuisines = new ArrayList<>();
    private List<String> selectedMealTypes = new ArrayList<>();
    private List<String> selectedRestrictions = new ArrayList<>();
    private List<String> selectedFoodItems = new ArrayList<>();

    // Colors for styling
    private final Color pureWhite = Color.WHITE;
    private final Color lightRed = new Color(255, 102, 102);
    private final Color lightGray = new Color(192, 192, 192);
    private final Color black = Color.BLACK;

    // Fonts using "Product Sans" (or fallback) – sizes increased
    private final Font baseFont = new Font("Product Sans", Font.PLAIN, 32);
    private final Font headerFont = new Font("Product Sans", Font.BOLD, 48);
    static Connection connect;


    public FrontEnd() {
        setTitle("Food Selection App");
        // Set initial window size to 1/2 of screen width and height
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width / 2, screenSize.height / 2);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(pureWhite);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(pureWhite);
        add(cardPanel, BorderLayout.CENTER);

        // Progress panel (no extra border)
        progressPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        progressPanel.setBackground(pureWhite);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        progressLabels = new JLabel[4];
        String[] steps = {"Cuisine", "Meal Type", "Dietary Restrictions", "Food Item"};
        for (int i = 0; i < 4; i++) {
            progressLabels[i] = new JLabel(steps[i], SwingConstants.CENTER);
            progressLabels[i].setOpaque(true);
            progressLabels[i].setBackground(pureWhite);
            progressLabels[i].setFont(baseFont);
            progressLabels[i].addMouseListener(new ProgressLabelListener(i));
            progressPanel.add(progressLabels[i]);
        }
        add(progressPanel, BorderLayout.SOUTH);

        cardPanel.add(createCuisinePanel(), "cuisine");
        cardPanel.add(createMealTypePanel(), "meal");
        cardPanel.add(createRestrictionsPanel(), "restrictions");
        cardPanel.add(createFoodItemPanel(), "food");
        cardPanel.add(createResultsPanel(), "results");

        updateProgressLabels();
    }

    /**
     * NEW: A lazy-loading options panel.
     * It loads a fixed number of rows (each row has 4 items) initially,
     * and supports loading additional rows.
     */
    private class LazyOptionsPanel extends JPanel {
        private String[] options;
        private int itemsLoaded = 0;
        private final int columns = 4;
        private final int gap = 15; // gap between cells/rows

        public LazyOptionsPanel(String[] options) {
            this.options = options;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(pureWhite);
            // Load initial batch: 4 rows (i.e. up to 16 items)
            loadMoreRows(4);
        }

        /**
         * Loads the next numberOfRows rows (each row = 4 items).
         * If there are fewer items remaining, only those are added.
         */
        public void loadMoreRows(int numberOfRows) {
            for (int i = 0; i < numberOfRows; i++) {
                if (itemsLoaded >= options.length) {
                    break;
                }
                JPanel row = new JPanel(new GridLayout(1, columns, gap, gap));
                row.setBackground(pureWhite);
                for (int j = 0; j < columns; j++) {
                    if (itemsLoaded < options.length) {
                        row.add(createOptionCell(options[itemsLoaded]));
                        itemsLoaded++;
                    } else {
                        // Add filler panel if no more items
                        JPanel filler = new JPanel();
                        filler.setBackground(pureWhite);
                        filler.setBorder(null);
                        row.add(filler);
                    }
                }
                row.setPreferredSize(new Dimension(800, 130));
                add(row);
                // Add vertical spacing between rows except after the last loaded row
                if (itemsLoaded < options.length) {
                    add(Box.createVerticalStrut(gap));
                }
            }
            revalidate();
            repaint();
        }
    }

    /**
     * NEW: A helper method that wraps a LazyOptionsPanel in a JScrollPane.
     * It attaches a scroll listener to load additional rows when the scrollbar reaches the bottom.
     */
    private JScrollPane createOptionsScrollPane(String[] options) {
        LazyOptionsPanel optionsPanel = new LazyOptionsPanel(options);
        JScrollPane scrollPane = new JScrollPane(optionsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(pureWhite);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.setPreferredSize(new Dimension(800, 260));

        // Attach scroll listener to load an extra row when reaching the bottom
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                JScrollBar sb = (JScrollBar) e.getSource();
                if (!e.getValueIsAdjusting() && sb.getValue() + sb.getVisibleAmount() >= sb.getMaximum()) {
                    optionsPanel.loadMoreRows(1);
                }
            }
        });
        return scrollPane;
    }
    private SimpleToggleButton createOptionCell(String text) {
        SimpleToggleButton toggle = new SimpleToggleButton(text);
        int borderThickness = 6;
        toggle.setBorder(BorderFactory.createLineBorder(lightGray, borderThickness));
        toggle.setFont(baseFont);
        toggle.setBackground(pureWhite);
        toggle.setForeground(black);
        // Fixed preferred size for consistency
        toggle.setPreferredSize(new Dimension(150, 120));
        toggle.addItemListener(e -> {
            if (toggle.isSelected()) {
                toggle.setBorder(BorderFactory.createLineBorder(lightRed, borderThickness));
            } else {
                toggle.setBorder(BorderFactory.createLineBorder(lightGray, borderThickness));
            }
        });
        return toggle;
    }
    // Cuisine Selection Panel
    private JPanel createCuisinePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(pureWhite);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind(s) of cuisine do you want?");
        questionLabel.setForeground(black);
        questionLabel.setFont(headerFont);
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> cuisineList = new ArrayList<>();


        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password"); // Replace with database name (username), username, and password


            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT distinct cuisine FROM Restaurant;");
            while (rs.next()) {
                String cuisineName = rs.getString(1); // name is first field
                cuisineList.add(cuisineName);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        String[] cuisines = cuisineList.toArray(new String[cuisineList.size()]);

        // Use lazy loading scroll pane
        JScrollPane scrollPane = createOptionsScrollPane(cuisines);
        // We need to retrieve the panel later for processing selections
        LazyOptionsPanel optionsPanel = (LazyOptionsPanel) scrollPane.getViewport().getView();
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(pureWhite);
        JButton backButton = new JButton("Back");
        backButton.setEnabled(false);
        JButton confirmButton = new JButton("Confirm");
        styleButton(backButton);
        styleButton(confirmButton);

        backButton.addActionListener(e -> navigateBack());
        confirmButton.addActionListener(e -> {
            selectedCuisines.clear();
            boolean selected = false;
            for (Component comp : optionsPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    for (Component inner : ((JPanel) comp).getComponents()) {
                        if (inner instanceof SimpleToggleButton) {
                            SimpleToggleButton tb = (SimpleToggleButton) inner;
                            if (tb.isSelected()) {
                                selectedCuisines.add(tb.getText());
                                selected = true;
                            }
                        }
                    }
                }
            }
            if (!selected) {
                JOptionPane.showMessageDialog(this, "Please select at least one cuisine.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                currentStep++;
                cardLayout.show(cardPanel, getCardName(currentStep));
                updateProgressLabels();
            }
        });
        buttonPanel.add(backButton);
        buttonPanel.add(confirmButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Meal Type Selection Panel
    private JPanel createMealTypePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(pureWhite);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind of meals are you looking for?");
        questionLabel.setForeground(black);
        questionLabel.setFont(headerFont);
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> typeList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password"); // Replace with database name (username), username, and password


            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT distinct type FROM Menu;");
            while (rs.next()) {
                String typeName = rs.getString(1); // name is first field
                typeList.add(typeName);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        String[] mealTypes = typeList.toArray(new String[typeList.size()]);
        JScrollPane scrollPane = createOptionsScrollPane(mealTypes);
        LazyOptionsPanel optionsPanel = (LazyOptionsPanel) scrollPane.getViewport().getView();
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(pureWhite);
        JButton backButton = new JButton("Back");
        JButton confirmButton = new JButton("Confirm");
        styleButton(backButton);
        styleButton(confirmButton);

        backButton.addActionListener(e -> navigateBack());
        confirmButton.addActionListener(e -> {
            selectedMealTypes.clear();
            boolean selected = false;
            for (Component comp : optionsPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    for (Component inner : ((JPanel) comp).getComponents()) {
                        if (inner instanceof SimpleToggleButton) {
                            SimpleToggleButton tb = (SimpleToggleButton) inner;
                            if (tb.isSelected()) {
                                selectedMealTypes.add(tb.getText());
                                selected = true;
                            }
                        }
                    }
                }
            }
            if (!selected) {
                JOptionPane.showMessageDialog(this, "Please select at least one meal type.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                currentStep++;
                cardLayout.show(cardPanel, getCardName(currentStep));
                updateProgressLabels();
            }
        });
        buttonPanel.add(backButton);
        buttonPanel.add(confirmButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Dietary Restrictions Selection Panel (selection optional)
    private JPanel createRestrictionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(pureWhite);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("Select allergens or dietary restrictions?");
        questionLabel.setForeground(black);
        questionLabel.setFont(headerFont);
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> restrictionList = new ArrayList<>();


        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password"); // Replace with database name (username), username, and password


            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT distinct ingName FROM Allergen;");
            while (rs.next()) {
                String restriction = rs.getString(1); // name is first field
                restrictionList.add(restriction);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        String[] restrictions = restrictionList.toArray(new String[restrictionList.size()]);

        JScrollPane scrollPane = createOptionsScrollPane(restrictions);
        LazyOptionsPanel optionsPanel = (LazyOptionsPanel) scrollPane.getViewport().getView();
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(pureWhite);
        JButton backButton = new JButton("Back");
        JButton confirmButton = new JButton("Confirm");
        styleButton(backButton);
        styleButton(confirmButton);

        backButton.addActionListener(e -> navigateBack());
        confirmButton.addActionListener(e -> {
            selectedRestrictions.clear();
            for (Component comp : optionsPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    for (Component inner : ((JPanel) comp).getComponents()) {
                        if (inner instanceof SimpleToggleButton) {
                            SimpleToggleButton tb = (SimpleToggleButton) inner;
                            if (tb.isSelected()) {
                                selectedRestrictions.add(tb.getText());
                            }
                        }
                    }
                }
            }
            currentStep++;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        });
        buttonPanel.add(backButton);
        buttonPanel.add(confirmButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Food Item Selection Panel styled like the others (using grid toggle buttons)
    private JPanel createFoodItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(pureWhite);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("Select a food item");
        questionLabel.setForeground(black);
        questionLabel.setFont(headerFont);
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> foodList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password"); // Replace with database name (username), username, and password


            Statement statement = connect.createStatement();
            String restaurants = "SELECT rid FROM Restaurant WHERE cuisine IN (" +
                    String.join(",", Collections.nCopies(selectedCuisines.size(), "?")) + ")))";
            String menus= "Select mid from Menu where type in (" +
                    String.join(",", Collections.nCopies(selectedMealTypes.size(), "?")) + ")" + "and rid=(";
            String restrictions="Select I.iname from Item I join Recipe R join Allergen A where ingName not in " +
                    String.join(",", Collections.nCopies(selectedRestrictions.size(), "?")) + ") and mid=(";
            String sql= restrictions+menus+restaurants;
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                String itemName = rs.getString(1); // name is first field
                foodList.add(itemName);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        String[] foodItems = foodList.toArray(new String[foodList.size()]);
        JScrollPane scrollPane = createOptionsScrollPane(foodItems);
        LazyOptionsPanel optionsPanel = (LazyOptionsPanel) scrollPane.getViewport().getView();
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(pureWhite);
        JButton backButton = new JButton("Back");
        JButton confirmButton = new JButton("Confirm");
        styleButton(backButton);
        styleButton(confirmButton);

        backButton.addActionListener(e -> navigateBack());
        confirmButton.addActionListener(e -> {
            selectedFoodItems.clear();
            boolean selected = false;
            for (Component comp : optionsPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    for (Component inner : ((JPanel) comp).getComponents()) {
                        if (inner instanceof SimpleToggleButton) {
                            SimpleToggleButton tb = (SimpleToggleButton) inner;
                            if (tb.isSelected()) {
                                selectedFoodItems.add(tb.getText());
                                selected = true;
                            }
                        }
                    }
                }
            }
            if (!selected) {
                JOptionPane.showMessageDialog(this, "Please select at least one food item.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                currentStep++;
                cardLayout.show(cardPanel, getCardName(currentStep));
                updateProgressLabels();
            }
        });
        buttonPanel.add(backButton);
        buttonPanel.add(confirmButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Results Panel
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(pureWhite);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea resultsArea = new JTextArea(10, 40);
        resultsArea.setEditable(false);
        resultsArea.setBackground(pureWhite);
        resultsArea.setForeground(black);
        resultsArea.setFont(baseFont);
        StringBuilder results = new StringBuilder("Matching Restaurants:\n\n");
        if (selectedFoodItems != null) {
            if (selectedFoodItems.contains("Pizza") && selectedCuisines.contains("Italian")) {
                results.append("Restaurant: Bella Napoli\n")
                        .append("Item: ").append(selectedFoodItems).append("\n")
                        .append("Ingredients: Tomato, Mozzarella, Basil\n\n");
            }
            if (selectedFoodItems.contains("Burger")) {
                results.append("Restaurant: Burger Bonanza\n")
                        .append("Item: ").append(selectedFoodItems).append("\n")
                        .append("Ingredients: Beef, Lettuce, Tomato, Bun\n\n");
            }
            if (selectedFoodItems.contains("Sushi") && selectedCuisines.contains("Japanese")) {
                results.append("Restaurant: Sushi Samurai\n")
                        .append("Item: ").append(selectedFoodItems).append("\n")
                        .append("Ingredients: Rice, Fish, Seaweed\n\n");
            }
        }
        if (results.length() == "Matching Restaurants:\n\n".length()) {
            results.append("No matching restaurants found.");
        }
        resultsArea.setText(results.toString());
        panel.add(new JScrollPane(resultsArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(pureWhite);
        JButton startOverButton = new JButton("Start Over");
        styleButton(startOverButton);
        startOverButton.addActionListener(e -> {
            currentStep = 0;
            selectedCuisines.clear();
            selectedMealTypes.clear();
            selectedRestrictions.clear();
            selectedFoodItems.clear();
            cardLayout.show(cardPanel, "cuisine");
            updateProgressLabels();
        });
        buttonPanel.add(startOverButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Helper: Style buttons using Product Sans and baseFont
    private void styleButton(JButton button) {
        button.setBackground(lightRed);
        button.setForeground(black);
        button.setFocusPainted(false);
        button.setFont(baseFont);
    }

    // Helper: Navigate back
    private void navigateBack() {
        if (currentStep > 0) {
            currentStep--;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        }
    }

    // Helper: Update progress labels based on current step
    private void updateProgressLabels() {
        for (int i = 0; i < 4; i++) {
            if (i < currentStep) {
                progressLabels[i].setForeground(lightRed);
            } else if (i == currentStep) {
                progressLabels[i].setForeground(black);
            } else {
                progressLabels[i].setForeground(lightGray);
            }
        }
    }

    // Helper: Get card name based on step
    private String getCardName(int step) {
        switch (step) {
            case 0: return "cuisine";
            case 1: return "meal";
            case 2: return "restrictions";
            case 3: return "food";
            default: return "results";
        }
    }

    // Listener for clickable progress labels
    private class ProgressLabelListener extends MouseAdapter {
        private final int step;
        public ProgressLabelListener(int step) { this.step = step; }
        @Override
        public void mouseClicked(MouseEvent e) {
            if (step < currentStep) {
                currentStep = step;
                cardLayout.show(cardPanel, getCardName(step));
                updateProgressLabels();
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            if (step < currentStep) {
                progressLabels[step].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
        @Override
        public void mouseExited(MouseEvent e) {
            progressLabels[step].setCursor(Cursor.getDefaultCursor());
        }
    }

    // SimpleToggleButton: a custom toggle button with centered text and custom painting.
    private static class SimpleToggleButton extends JToggleButton {
        public SimpleToggleButton(String text) {
            super(text);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(new Font("Product Sans", Font.PLAIN, 32));
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(false);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, w, h);
            Border border = getBorder();
            if (border != null) {
                border.paintBorder(this, g2, 0, 0, w, h);
            }
            String text = getText();
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();
            int textX = (w - textWidth) / 2;
            int textY = (h + textHeight - fm.getDescent()) / 2;
            g2.setColor(getForeground());
            g2.drawString(text, textX, textY);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FrontEnd app = new FrontEnd();
            app.setVisible(true);
        });
    }
}
