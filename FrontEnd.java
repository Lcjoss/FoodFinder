import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
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
    private String selectedFoodItem;

    // Colors for styling
    private final Color pureWhite = Color.WHITE;
    private final Color lightRed = new Color(255, 102, 102);
    private final Color lightGray = new Color(192, 192, 192);
    private final Color black = Color.BLACK;

    // Fonts using "Product Sans" (or fallback) – sizes increased
    private final Font baseFont = new Font("Product Sans", Font.PLAIN, 32);
    private final Font headerFont = new Font("Product Sans", Font.BOLD, 48);

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

    // Helper: Build a container that groups options into rows of 4 with extra spacing.
    private JPanel createOptionsPanel(String[] options) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(pureWhite);
        int columns = 4;
        int gap = 15; // extra gap between grid cells and rows
        for (int i = 0; i < options.length; i += columns) {
            JPanel row = new JPanel(new GridLayout(1, columns, gap, gap));
            row.setBackground(pureWhite);
            for (int j = 0; j < columns; j++) {
                int index = i + j;
                if (index < options.length) {
                    row.add(createOptionCell(options[index]));
                } else {
                    // Filler panel with pure white background and no border
                    JPanel filler = new JPanel();
                    filler.setBackground(pureWhite);
                    filler.setBorder(null);
                    row.add(filler);
                }
            }
            // Fix row height (e.g., 130 pixels)
            row.setPreferredSize(new Dimension(800, 130));
            container.add(row);
            // Add vertical spacing between rows (except after the last row)
            if (i + columns < options.length) {
                container.add(Box.createVerticalStrut(gap));
            }
        }
        return container;
    }

    // Helper: Create a simple toggle button with centered text and a thick border.
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

        String[] cuisines = {"Italian", "Chinese", "Mexican", "Indian", "Japanese", "Thai", "French", "Greek", "Lebanese", "a", "b", "c", "d"};
        JPanel optionsPanel = createOptionsPanel(cuisines);
        JScrollPane scrollPane = new JScrollPane(optionsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(pureWhite);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        // Fixed preferred size to show 2 rows (≈2 x 130 = 260 pixels)
        scrollPane.setPreferredSize(new Dimension(800, 260));
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

        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack", "Brunch", "Midnight"};
        JPanel optionsPanel = createOptionsPanel(mealTypes);
        JScrollPane scrollPane = new JScrollPane(optionsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(pureWhite);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.setPreferredSize(new Dimension(800, 260));
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

        JLabel questionLabel = new JLabel("Do you have any allergens or dietary restrictions?");
        questionLabel.setForeground(black);
        questionLabel.setFont(headerFont);
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(questionLabel, BorderLayout.NORTH);

        String[] restrictions = {"Gluten-free", "Vegetarian", "Vegan", "Nut-free", "Dairy-free", "Halal", "Kosher"};
        JPanel optionsPanel = createOptionsPanel(restrictions);
        JScrollPane scrollPane = new JScrollPane(optionsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(pureWhite);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.setPreferredSize(new Dimension(800, 260));
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

        // Food items as toggle buttons in the same style
        String[] foodItems = {"Pizza", "Burger", "Sushi", "Pasta", "Salad"};
        JPanel optionsPanel = createOptionsPanel(foodItems);
        JScrollPane scrollPane = new JScrollPane(optionsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(pureWhite);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.setPreferredSize(new Dimension(800, 260));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Enforce single selection using a ButtonGroup
        ButtonGroup foodGroup = new ButtonGroup();
        for (Component comp : optionsPanel.getComponents()) {
            if (comp instanceof JPanel) {
                for (Component inner : ((JPanel) comp).getComponents()) {
                    if (inner instanceof SimpleToggleButton) {
                        foodGroup.add((SimpleToggleButton) inner);
                    }
                }
            }
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(pureWhite);
        JButton backButton = new JButton("Back");
        JButton confirmButton = new JButton("Confirm");
        styleButton(backButton);
        styleButton(confirmButton);

        backButton.addActionListener(e -> navigateBack());
        confirmButton.addActionListener(e -> {
            String selected = null;
            for (Component comp : optionsPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    for (Component inner : ((JPanel) comp).getComponents()) {
                        if (inner instanceof SimpleToggleButton) {
                            SimpleToggleButton tb = (SimpleToggleButton) inner;
                            if (tb.isSelected()) {
                                selected = tb.getText();
                                break;
                            }
                        }
                    }
                }
                if (selected != null) break;
            }
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select a food item.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedFoodItem = selected;
                cardLayout.show(cardPanel, "results");
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
        if (selectedFoodItem != null) {
            if (selectedFoodItem.equals("Pizza") && selectedCuisines.contains("Italian")) {
                results.append("Restaurant: Bella Napoli\n")
                        .append("Item: ").append(selectedFoodItem).append("\n")
                        .append("Ingredients: Tomato, Mozzarella, Basil\n\n");
            }
            if (selectedFoodItem.equals("Burger")) {
                results.append("Restaurant: Burger Bonanza\n")
                        .append("Item: ").append(selectedFoodItem).append("\n")
                        .append("Ingredients: Beef, Lettuce, Tomato, Bun\n\n");
            }
            if (selectedFoodItem.equals("Sushi") && selectedCuisines.contains("Japanese")) {
                results.append("Restaurant: Sushi Samurai\n")
                        .append("Item: ").append(selectedFoodItem).append("\n")
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
            selectedFoodItem = null;
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
