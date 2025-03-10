package FoodFinder;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class FrontEnd extends JFrame {

    // Colors and Fonts (using a modern "Segoe UI" style)
    private final Color accentColor = Color.decode("#FF6666");
    private final Color white = Color.WHITE;
    private final Color darkGray = Color.DARK_GRAY;
    private final Font headerFont = new Font("Segoe UI", Font.BOLD, 54);
    private final Font optionFont = new Font("Segoe UI", Font.PLAIN, 24);

    // Card layout and progress components
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JLabel[] progressLabels;
    private int currentStep = 0;

    // Storage for user selections
    private List<String> selectedCuisines = new ArrayList<>();
    private List<String> selectedMealTypes = new ArrayList<>();
    private List<String> selectedRestrictions = new ArrayList<>();
    private List<String> selectedFoodItems = new ArrayList<>();

    // Database connection
    static Connection connect;

    public FrontEnd() {
        setTitle("FoodFinder");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(white);

        // Header panel
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(white);
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        JLabel titleLabel = new JLabel("FoodFinder", SwingConstants.CENTER);
        titleLabel.setFont(headerFont);
        titleLabel.setForeground(accentColor);
        header.add(titleLabel, BorderLayout.NORTH);
        add(header, BorderLayout.NORTH);

        // Card panel with one card per step
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(white);
        cardPanel.add(createCuisinePanel(), "cuisine");
        cardPanel.add(createMealTypePanel(), "meal");
        cardPanel.add(createRestrictionsPanel(), "restrictions");
        cardPanel.add(createFoodItemPanel(), "food");
        cardPanel.add(createResultsPanel(), "results");
        add(cardPanel, BorderLayout.CENTER);

        // Progress panel with clickable labels (for previous steps)
        JPanel progressPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        progressPanel.setBackground(white);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String[] steps = {"Cuisine", "Meal Type", "Dietary Restrictions", "Food Item"};
        progressLabels = new JLabel[steps.length];
        for (int i = 0; i < steps.length; i++) {
            progressLabels[i] = new JLabel(steps[i], SwingConstants.CENTER);
            progressLabels[i].setFont(optionFont);
            progressLabels[i].setOpaque(true);
            progressLabels[i].setBackground(white);
            progressLabels[i].setForeground(i == 0 ? darkGray : Color.LIGHT_GRAY);
            final int step = i;
            progressLabels[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (step < currentStep) {
                        currentStep = step;
                        cardLayout.show(cardPanel, getCardName(currentStep));
                        updateProgressLabels();
                    }
                }
            });
            progressPanel.add(progressLabels[i]);
        }
        add(progressPanel, BorderLayout.SOUTH);
    }

    // Update progress label colors based on the current step
    private void updateProgressLabels() {
        for (int i = 0; i < progressLabels.length; i++) {
            if (i < currentStep) {
                progressLabels[i].setForeground(accentColor);
            } else if (i == currentStep) {
                progressLabels[i].setForeground(darkGray);
            } else {
                progressLabels[i].setForeground(Color.LIGHT_GRAY);
            }
        }
    }

    // Returns the card name for a given step index
    private String getCardName(int step) {
        switch (step) {
            case 0: return "cuisine";
            case 1: return "meal";
            case 2: return "restrictions";
            case 3: return "food";
            default: return "results";
        }
    }

    // Panel for cuisine selection
    private JPanel createCuisinePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind(s) of cuisine do you want?", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        // Retrieve cuisine options from the database
        List<String> cuisineList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password");
            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT distinct cuisine FROM Restaurant;");
            while (rs.next()) {
                cuisineList.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] cuisines = cuisineList.toArray(new String[0]);
        PagedSearchPanel optionsPanel = new PagedSearchPanel(cuisines);
        panel.add(optionsPanel, BorderLayout.CENTER);

        // Navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        backButton.setEnabled(false);
        JButton continueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(continueButton);

        backButton.addActionListener(e -> navigateBack());
        continueButton.addActionListener(e -> {
            selectedCuisines.clear();
            List<String> sel = optionsPanel.getSelectedOptions();
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one cuisine.",
                        "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedCuisines.addAll(sel);
                currentStep++;
                cardLayout.show(cardPanel, getCardName(currentStep));
                updateProgressLabels();
            }
        });
        buttonPanel.add(backButton);
        buttonPanel.add(continueButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Panel for meal type selection
    private JPanel createMealTypePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind of meals are you looking for?", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> typeList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password");
            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT distinct type FROM Menu;");
            while (rs.next()) {
                typeList.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] mealTypes = typeList.toArray(new String[0]);
        PagedSearchPanel optionsPanel = new PagedSearchPanel(mealTypes);
        panel.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        JButton continueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(continueButton);

        backButton.addActionListener(e -> navigateBack());
        continueButton.addActionListener(e -> {
            selectedMealTypes.clear();
            List<String> sel = optionsPanel.getSelectedOptions();
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one meal type.",
                        "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedMealTypes.addAll(sel);
                currentStep++;
                cardLayout.show(cardPanel, getCardName(currentStep));
                updateProgressLabels();
            }
        });
        buttonPanel.add(backButton);
        buttonPanel.add(continueButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Panel for dietary restrictions selection (optional)
    private JPanel createRestrictionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("Select allergens or dietary restrictions (optional)", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> restrictionList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password");
            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT distinct ingName FROM Allergen;");
            while (rs.next()) {
                restrictionList.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] restrictions = restrictionList.toArray(new String[0]);
        PagedSearchPanel optionsPanel = new PagedSearchPanel(restrictions);
        panel.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        JButton continueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(continueButton);

        backButton.addActionListener(e -> navigateBack());
        continueButton.addActionListener(e -> {
            selectedRestrictions.clear();
            List<String> sel = optionsPanel.getSelectedOptions();
            // Note: No warning here since restrictions are optional
            selectedRestrictions.addAll(sel);
            currentStep++;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        });
        buttonPanel.add(backButton);
        buttonPanel.add(continueButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Panel for food item selection
    private JPanel createFoodItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("Select a food item", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> foodList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password");
            Statement statement = connect.createStatement();
            // Example SQL query – adjust as needed
            ResultSet rs = statement.executeQuery("SELECT distinct itemName FROM Item WHERE 1=1");
            while (rs.next()) {
                foodList.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] foodItems = foodList.toArray(new String[0]);
        PagedSearchPanel optionsPanel = new PagedSearchPanel(foodItems);
        panel.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        JButton continueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(continueButton);

        backButton.addActionListener(e -> navigateBack());
        continueButton.addActionListener(e -> {
            selectedFoodItems.clear();
            List<String> sel = optionsPanel.getSelectedOptions();
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one food item.",
                        "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedFoodItems.addAll(sel);
                currentStep++;
                cardLayout.show(cardPanel, getCardName(currentStep));
                updateProgressLabels();
            }
        });
        buttonPanel.add(backButton);
        buttonPanel.add(continueButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Panel to display results
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(optionFont);
        resultsArea.setBackground(white);
        resultsArea.setForeground(darkGray);
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Example logic for matching restaurants (adjust your business logic accordingly)
        StringBuilder results = new StringBuilder("Matching Restaurants:\n\n");
        if (selectedFoodItems != null) {
            if (selectedFoodItems.contains("Pizza") && selectedCuisines.contains("Italian")) {
                results.append("Restaurant: Bella Napoli\n")
                        .append("Item: Pizza\n")
                        .append("Ingredients: Tomato, Mozzarella, Basil\n\n");
            }
            if (selectedFoodItems.contains("Burger")) {
                results.append("Restaurant: Burger Bonanza\n")
                        .append("Item: Burger\n")
                        .append("Ingredients: Beef, Lettuce, Tomato, Bun\n\n");
            }
            if (selectedFoodItems.contains("Sushi") && selectedCuisines.contains("Japanese")) {
                results.append("Restaurant: Sushi Samurai\n")
                        .append("Item: Sushi\n")
                        .append("Ingredients: Rice, Fish, Seaweed\n\n");
            }
        }
        if (results.toString().equals("Matching Restaurants:\n\n")) {
            results.append("No matching restaurants found.");
        }
        resultsArea.setText(results.toString());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
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

    // Helper: Style buttons consistently
    private void styleButton(JButton button) {
        button.setFont(optionFont);
        button.setBackground(accentColor);
        button.setForeground(white);
        button.setFocusPainted(false);
    }

    // Navigate back one step
    private void navigateBack() {
        if (currentStep > 0) {
            currentStep--;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        }
    }

    // *********************************************************
    // Inner Class: PagedSearchPanel
    // A custom panel that displays up to 12 options (3 rows x 4 columns)
    // with a search bar that filters options dynamically and
    // left/right arrow buttons for paging through items.
    // *********************************************************
    private class PagedSearchPanel extends JPanel {
        private List<String> originalOptions;
        private List<String> filteredOptions;
        private Map<String, Boolean> selectionMap;
        private int currentPage = 0;
        private final int itemsPerPage = 12; // 3 x 4 grid

        private JTextField searchField;
        private JPanel gridPanel;
        private JButton leftButton, rightButton;

        public PagedSearchPanel(String[] options) {
            originalOptions = new ArrayList<>();
            for (String opt : options) {
                originalOptions.add(opt);
            }
            filteredOptions = new ArrayList<>(originalOptions);
            selectionMap = new HashMap<>();
            for (String opt : originalOptions) {
                selectionMap.put(opt, false);
            }
            setLayout(new BorderLayout(10, 10));

            // Search bar at the top
            searchField = new JTextField();
            searchField.setFont(optionFont);
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateFilter();
                }
                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateFilter();
                }
                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateFilter();
                }
            });
            add(searchField, BorderLayout.NORTH);

            // Grid panel for options
            gridPanel = new JPanel(new GridLayout(3, 4, 15, 15));
            add(gridPanel, BorderLayout.CENTER);

            // Arrow panel for pagination
            JPanel arrowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            leftButton = new JButton("<");
            rightButton = new JButton(">");
            leftButton.setFont(optionFont);
            rightButton.setFont(optionFont);
            leftButton.addActionListener(e -> {
                if (currentPage > 0) {
                    currentPage--;
                    refreshGrid();
                }
            });
            rightButton.addActionListener(e -> {
                if ((currentPage + 1) * itemsPerPage < filteredOptions.size()) {
                    currentPage++;
                    refreshGrid();
                }
            });
            arrowPanel.add(leftButton);
            arrowPanel.add(rightButton);
            add(arrowPanel, BorderLayout.SOUTH);

            refreshGrid();
        }

        // Updates the filtered options based on the search field text
        private void updateFilter() {
            String text = searchField.getText().trim().toLowerCase();
            filteredOptions.clear();
            for (String opt : originalOptions) {
                if (opt.toLowerCase().contains(text)) {
                    filteredOptions.add(opt);
                }
            }
            currentPage = 0;
            refreshGrid();
        }

        // Rebuilds the grid based on the current page and filtered options
        private void refreshGrid() {
            gridPanel.removeAll();
            int start = currentPage * itemsPerPage;
            int end = Math.min(start + itemsPerPage, filteredOptions.size());
            if (filteredOptions.size() == 0) {
                gridPanel.setLayout(new BorderLayout());
                gridPanel.add(new JLabel("No results found", SwingConstants.CENTER), BorderLayout.CENTER);
            } else {
                gridPanel.setLayout(new GridLayout(3, 4, 15, 15));
                for (int i = start; i < end; i++) {
                    String option = filteredOptions.get(i);
                    JToggleButton btn = new JToggleButton(option);
                    btn.setFont(optionFont);
                    btn.setFocusPainted(false);
                    btn.setBackground(white);
                    btn.setForeground(darkGray);
                    boolean selected = selectionMap.getOrDefault(option, false);
                    btn.setSelected(selected);
                    if (selected) {
                        btn.setBackground(accentColor);
                        btn.setForeground(white);
                    }
                    btn.addItemListener(e -> {
                        boolean isSelected = btn.isSelected();
                        selectionMap.put(option, isSelected);
                        if (isSelected) {
                            btn.setBackground(accentColor);
                            btn.setForeground(white);
                        } else {
                            btn.setBackground(white);
                            btn.setForeground(darkGray);
                        }
                    });
                    gridPanel.add(btn);
                }
                int itemsAdded = end - start;
                // Fill any remaining grid cells with empty panels for consistent layout
                for (int i = itemsAdded; i < itemsPerPage; i++) {
                    gridPanel.add(new JPanel());
                }
            }
            // Update arrow buttons based on the current page and total results
            leftButton.setEnabled(currentPage > 0);
            rightButton.setEnabled((currentPage + 1) * itemsPerPage < filteredOptions.size());
            gridPanel.revalidate();
            gridPanel.repaint();
        }

        // Returns a list of all selected options from the full (unfiltered) set
        public List<String> getSelectedOptions() {
            List<String> selected = new ArrayList<>();
            for (Map.Entry<String, Boolean> entry : selectionMap.entrySet()) {
                if (entry.getValue()) {
                    selected.add(entry.getKey());
                }
            }
            return selected;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FrontEnd app = new FrontEnd();
            app.setVisible(true);
        });
    }
}
