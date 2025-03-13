package FoodFinder.ui;

import FoodFinder.dao.MenuDAO;
import FoodFinder.dao.RestaurantDAO;
import FoodFinder.dao.UserProfileDAO;
import FoodFinder.domain.Menu;
import FoodFinder.domain.MenuItem;
import FoodFinder.domain.Restaurant;
import FoodFinder.domain.UserProfile;
import FoodFinder.map.MapUtils;
import FoodFinder.map.RestaurantWaypoint;
import FoodFinder.map.RestaurantWaypointRenderer;
import FoodFinder.session.UserSession;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointPainter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FoodFinderFrame extends JFrame {
    // -------------------- UI Constants --------------------
    private final Color accentColor = Color.decode("#FF6666");
    private final Color white = Color.WHITE;
    private final Color darkGray = Color.DARK_GRAY;
    private final Font headerFont = new Font("Segoe UI", Font.BOLD, 54);
    private final Font optionFont = new Font("Segoe UI", Font.PLAIN, 24);

    // -------------------- Global State --------------------
    private List<Restaurant> currentValidRestaurants = new ArrayList<>();
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JLabel[] progressLabels;
    private int currentStep = 0;

    // User selections (stored as lists for the session)
    List<String> selectedCuisines = new ArrayList<>();
    List<String> selectedMealTypes = new ArrayList<>();
    List<String> selectedRestrictions = new ArrayList<>();
    List<String> selectedFoodItems = new ArrayList<>();

    // Selected objects for later steps
    private Restaurant selectedRestaurant;
    private Menu selectedMenu;
    private MenuItem selectedItem;

    // Data loaded from the database
    private List<Restaurant> allRestaurants;

    // Map viewer and related state
    private JList<Restaurant> resultsList;
    private Map<Restaurant, String> restaurantMatchingItems = new HashMap<>();

    // References to dynamically updated panels/buttons
    private PagedSearchPanel foodOptionsPanel;
    private JButton foodContinueButton;
    private PagedSearchPanel mealTypeOptionsPanel;
    private JPanel mealTypePanel;
    private JXMapViewer resultsMapViewer;

    /**
     * Constructor creates a placeholder UI.
     * Panels are not built until initPanels() is called.
     */
    public FoodFinderFrame() {
        setTitle("FoodFinder");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(white);

        // Load static data from the database.
        allRestaurants = RestaurantDAO.getAllRestaurants();

        // Header (always visible)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(white);
        header.setBorder(new EmptyBorder(20, 20, 10, 20));
        JLabel titleLabel = new JLabel("FoodFinder", SwingConstants.CENTER);
        titleLabel.setFont(headerFont);
        titleLabel.setForeground(accentColor);
        header.add(titleLabel, BorderLayout.NORTH);
        add(header, BorderLayout.NORTH);

        // Create an initially empty card panel with a placeholder.
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(white);
        cardPanel.add(new JLabel("Please log in to start.", SwingConstants.CENTER), "placeholder");
        add(cardPanel, BorderLayout.CENTER);

        // Progress panel (for navigating between steps)
        JPanel progressPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        progressPanel.setBackground(white);
        progressPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
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
                        saveUserPreferences();  // Save before switching panels
                        cardLayout.show(cardPanel, getCardName(currentStep));
                        updateProgressLabels();
                    }
                }
            });
            progressPanel.add(progressLabels[i]);
        }
        add(progressPanel, BorderLayout.SOUTH);

        // If a user is already logged in, immediately load the panels.
        if (UserSession.getCurrentUser() != null) {
            initPanels();
        }
    }

    /**
     * Call this method after the user has successfully logged in.
     * It initializes all the selection panels using the current user's stored preferences.
     */
    public void initPanels() {
        // Remove placeholder content.
        cardPanel.removeAll();

        cardPanel.add(createCuisinePanel(), "cuisine");
        mealTypePanel = createMealTypePanel();
        cardPanel.add(mealTypePanel, "meal");
        cardPanel.add(createRestrictionsPanel(), "restrictions");
        cardPanel.add(createFoodItemPanel(), "food");
        cardPanel.add(createResultsPanel(), "results");
        // Start at the first step.
        currentStep = 0;
        cardLayout.show(cardPanel, getCardName(currentStep));
        updateProgressLabels();
        revalidate();
        repaint();
    }

    private String getCardName(int step) {
        switch (step) {
            case 0: return "cuisine";
            case 1: return "meal";
            case 2: return "restrictions";
            case 3: return "food";
            default: return "results";
        }
    }

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

    /**
     * Saves the current selections into the user profile in the database.
     */
    private void saveUserPreferences() {
        if (UserSession.getCurrentUser() != null) {
            UserSession.getCurrentUser().setSelectedCuisines(String.join(",", selectedCuisines));
            UserSession.getCurrentUser().setSelectedMealTypes(String.join(",", selectedMealTypes));
            UserSession.getCurrentUser().setSelectedRestrictions(String.join(",", selectedRestrictions));
            UserSession.getCurrentUser().setSelectedFoodItems(String.join(",", selectedFoodItems));
            UserProfileDAO.updatePreferences(UserSession.getCurrentUser());
        }
    }

    // -------------------- Panel Creation Methods --------------------

    private JPanel createCuisinePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind(s) of cuisine do you want?", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> cuisineList = RestaurantDAO.getDistinctCuisines();
        if (cuisineList.isEmpty()) {
            cuisineList = Arrays.asList("Italian", "Japanese", "Mexican", "American");
        }
        String[] cuisines = cuisineList.toArray(new String[0]);
        PagedSearchPanel optionsPanel = new PagedSearchPanel(cuisines);

        // Pre-select saved cuisines if available.
        if (UserSession.getCurrentUser() != null) {
            String saved = UserSession.getCurrentUser().getSelectedCuisines();
            if (saved != null && !saved.trim().isEmpty()) {
                List<String> savedList = Arrays.asList(saved.split(","));
                optionsPanel.setSelectedOptions(savedList);
                selectedCuisines.clear();
                selectedCuisines.addAll(savedList);
            }
        }

        panel.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        backButton.setEnabled(false);
        JButton continueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(continueButton);
        backButton.addActionListener(e -> {
            // No action on back for the first panel.
        });
        continueButton.addActionListener(e -> {
            List<String> sel = optionsPanel.getSelectedOptions();
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one cuisine.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedCuisines.clear();
                selectedCuisines.addAll(sel);
                saveUserPreferences();  // Save after cuisine selection.
                updateMealTypePanel();
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

    private JPanel createMealTypePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind of meals are you looking for?", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> typeList = MenuDAO.getMealTypesForCuisines(selectedCuisines);
        String[] mealTypes = typeList.toArray(new String[0]);
        mealTypeOptionsPanel = new PagedSearchPanel(mealTypes);

        // Pre-select saved meal types if available.
        if (UserSession.getCurrentUser() != null) {
            String saved = UserSession.getCurrentUser().getSelectedMealTypes();
            if (saved != null && !saved.trim().isEmpty()) {
                List<String> savedList = Arrays.asList(saved.split(","));
                mealTypeOptionsPanel.setSelectedOptions(savedList);
                selectedMealTypes.clear();
                selectedMealTypes.addAll(savedList);
            }
        }

        panel.add(mealTypeOptionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        JButton continueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(continueButton);
        backButton.addActionListener(e -> {
            saveUserPreferences();
            currentStep--;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        });
        continueButton.addActionListener(e -> {
            List<String> sel = mealTypeOptionsPanel.getSelectedOptions();
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one meal type.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedMealTypes.clear();
                selectedMealTypes.addAll(sel);
                saveUserPreferences();
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

    private void updateMealTypePanel() {
        List<String> typeList = MenuDAO.getMealTypesForCuisines(selectedCuisines);
        String[] mealTypes = typeList.toArray(new String[0]);
        if (mealTypeOptionsPanel != null) {
            mealTypeOptionsPanel.updateOptions(mealTypes);
            if (UserSession.getCurrentUser() != null) {
                String saved = UserSession.getCurrentUser().getSelectedMealTypes();
                if (saved != null && !saved.trim().isEmpty()) {
                    List<String> savedList = Arrays.asList(saved.split(","));
                    mealTypeOptionsPanel.setSelectedOptions(savedList);
                    selectedMealTypes.clear();
                    selectedMealTypes.addAll(savedList);
                }
            }
        }
    }

    private JPanel createRestrictionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("Select allergens or dietary restrictions (optional)", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> restrictionList = MenuDAO.getAllergens();
        String[] restrictions = restrictionList.toArray(new String[0]);
        PagedSearchPanel optionsPanel = new PagedSearchPanel(restrictions);

        // Pre-select saved restrictions if available.
        if (UserSession.getCurrentUser() != null) {
            String saved = UserSession.getCurrentUser().getSelectedRestrictions();
            if (saved != null && !saved.trim().isEmpty()) {
                List<String> savedList = Arrays.asList(saved.split(","));
                optionsPanel.setSelectedOptions(savedList);
                selectedRestrictions.clear();
                selectedRestrictions.addAll(savedList);
            }
        }

        panel.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        JButton continueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(continueButton);
        backButton.addActionListener(e -> {
            saveUserPreferences();
            currentStep--;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        });
        continueButton.addActionListener(e -> {
            List<String> sel = optionsPanel.getSelectedOptions();
            selectedRestrictions.clear();
            selectedRestrictions.addAll(sel);
            saveUserPreferences();
            // Update food item options based on selections.
            List<String> foodList = MenuDAO.getFilteredFoodItems(selectedCuisines, selectedMealTypes, selectedRestrictions);
            if (foodOptionsPanel != null) {
                foodOptionsPanel.updateOptions(foodList.toArray(new String[0]));
            }
            foodContinueButton.setEnabled(!foodList.isEmpty());
            currentStep++;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        });
        buttonPanel.add(backButton);
        buttonPanel.add(continueButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFoodItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("Select a food item", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        // Create the PagedSearchPanel for food items.
        foodOptionsPanel = new PagedSearchPanel(new String[0]);
        // Pre-select stored food items if available.
        if (UserSession.getCurrentUser() != null) {
            String saved = UserSession.getCurrentUser().getSelectedFoodItems();
//            System.out.println("'" + saved);
            if (saved != null && !saved.trim().isEmpty()) {
                List<String> savedList = Arrays.asList(saved.split(","));
                foodOptionsPanel.setSelectedOptions(savedList);
                selectedFoodItems.clear();
                selectedFoodItems.addAll(savedList);
            }
        }
        panel.add(foodOptionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        foodContinueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(foodContinueButton);
        backButton.addActionListener(e -> {
            saveUserPreferences();
            currentStep--;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        });
        foodContinueButton.addActionListener(e -> {
            List<String> sel = foodOptionsPanel.getSelectedOptions();
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one food item.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedFoodItems.clear();
                selectedFoodItems.addAll(sel);
                saveUserPreferences();
                updateResultsPanel();
                currentStep++;
                cardLayout.show(cardPanel, "results");
            }
        });
        buttonPanel.add(backButton);
        buttonPanel.add(foodContinueButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField searchField = new JTextField();
        searchField.setFont(optionFont);
        panel.add(searchField, BorderLayout.NORTH);

        resultsList = new JList<>();
        resultsList.setCellRenderer(new ResultsListCellRenderer());
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Restaurant clicked = resultsList.getSelectedValue();
                if (clicked != null) {
                    if (selectedRestaurant != null && selectedRestaurant.equals(clicked)) {
                        updateMenusPanel(clicked);
                        cardLayout.show(cardPanel, "menus");
                    } else {
                        selectedRestaurant = clicked;
                        resultsMapViewer.setAddressLocation(new RestaurantWaypoint(clicked).getPosition());
                        resultsMapViewer.setZoom(3);
                    }
                }
            }
        });
        JScrollPane listScrollPane = new JScrollPane(resultsList);

        resultsMapViewer = MapUtils.createMapViewer(allRestaurants);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, resultsMapViewer);
        splitPane.setDividerLocation(300);
        panel.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton startOverButton = new JButton("Start Over");
        styleButton(startOverButton);
        startOverButton.addActionListener(e -> {
            saveUserPreferences();
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

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filterList() {
                String filter = searchField.getText().trim().toLowerCase();
                DefaultListModel<Restaurant> filteredModel = new DefaultListModel<>();
                for (Restaurant r : currentValidRestaurants) {
                    if (r.name.toLowerCase().contains(filter)) {
                        filteredModel.addElement(r);
                    }
                }
                resultsList.setModel(filteredModel);
            }
            public void insertUpdate(DocumentEvent e) { filterList(); }
            public void removeUpdate(DocumentEvent e) { filterList(); }
            public void changedUpdate(DocumentEvent e) { filterList(); }
        });
        return panel;
    }

    private void updateResultsPanel() {
        List<Restaurant> filteredRestaurants = RestaurantDAO.getFilteredRestaurants(
                selectedCuisines, selectedMealTypes, selectedFoodItems, selectedRestrictions);
        currentValidRestaurants = filteredRestaurants;
        DefaultListModel<Restaurant> listModel = new DefaultListModel<>();
        if (currentValidRestaurants.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No matching restaurants found.", "Results", JOptionPane.INFORMATION_MESSAGE);
        } else {
            for (Restaurant r : currentValidRestaurants) {
                List<String> matchingItems = RestaurantDAO.getMatchingItemsForRestaurant(
                        r.id, selectedMealTypes, selectedFoodItems, selectedRestrictions);
                restaurantMatchingItems.put(r, String.join(", ", matchingItems));
                listModel.addElement(r);
            }
        }
        resultsList.setModel(listModel);

        // Update the map overlay to display only the restaurants in the results list.
        Set<RestaurantWaypoint> restaurantWaypoints = new HashSet<>();
        for (Restaurant r : currentValidRestaurants) {
            restaurantWaypoints.add(new RestaurantWaypoint(r));
        }
        WaypointPainter<RestaurantWaypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(restaurantWaypoints);
        waypointPainter.setRenderer(new RestaurantWaypointRenderer());
        resultsMapViewer.setOverlayPainter(waypointPainter);
    }

    private void updateMenusPanel(Restaurant restaurant) {
        JPanel menusPanel = new JPanel(new BorderLayout(10, 10));
        menusPanel.setBackground(white);
        menusPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Menus at " + restaurant.name, SwingConstants.CENTER);
        titleLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(darkGray);
        menusPanel.add(titleLabel, BorderLayout.NORTH);

        List<Menu> menus = MenuDAO.getMenusForRestaurant(restaurant.id);
        JPanel menuListPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        for (Menu menu : menus) {
            JButton btn = new JButton(menu.type);
            btn.setFont(optionFont);
            btn.addActionListener(e -> {
                selectedMenu = menu;
                updateItemsPanel(menu);
                cardLayout.show(cardPanel, "items");
            });
            menuListPanel.add(btn);
        }
        JScrollPane scroll = new JScrollPane(menuListPanel);
        menusPanel.add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        styleButton(backButton);
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "results"));
        buttonPanel.add(backButton);
        menusPanel.add(buttonPanel, BorderLayout.SOUTH);

        cardPanel.add(menusPanel, "menus");
    }

    private void updateItemsPanel(Menu menu) {
        JPanel itemsPanel = new JPanel(new BorderLayout(10, 10));
        itemsPanel.setBackground(white);
        itemsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Menu Items: " + menu.type, SwingConstants.CENTER);
        titleLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(darkGray);
        itemsPanel.add(titleLabel, BorderLayout.NORTH);

        JTextField searchField = new JTextField();
        searchField.setFont(optionFont);
        itemsPanel.add(searchField, BorderLayout.NORTH);

        JPanel itemListPanel = new JPanel();
        itemListPanel.setLayout(new BoxLayout(itemListPanel, BoxLayout.Y_AXIS));

        Runnable refreshItems = () -> {
            itemListPanel.removeAll();
            String filter = searchField.getText().trim().toLowerCase();
            for (MenuItem mi : menu.items) {
                if (mi.name.toLowerCase().contains(filter)) {
                    JButton btn = new JButton(mi.name);
                    btn.setFont(optionFont);
                    btn.addActionListener(ev -> {
                        selectedItem = mi;
                        updateRecipePanel(mi);
                        cardLayout.show(cardPanel, "recipe");
                    });
                    itemListPanel.add(btn);
                }
            }
            itemListPanel.revalidate();
            itemListPanel.repaint();
        };

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshItems.run(); }
            public void removeUpdate(DocumentEvent e) { refreshItems.run(); }
            public void changedUpdate(DocumentEvent e) { refreshItems.run(); }
        });
        refreshItems.run();

        JScrollPane scroll = new JScrollPane(itemListPanel);
        itemsPanel.add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        styleButton(backButton);
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "menus"));
        buttonPanel.add(backButton);
        itemsPanel.add(buttonPanel, BorderLayout.SOUTH);

        cardPanel.add(itemsPanel, "items");
    }

    private void updateRecipePanel(MenuItem item) {
        JPanel recipePanel = new JPanel(new BorderLayout(10, 10));
        recipePanel.setBackground(white);
        recipePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Recipe for: " + item.name, SwingConstants.CENTER);
        titleLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(darkGray);
        recipePanel.add(titleLabel, BorderLayout.NORTH);

        String recipeText = item.recipe;
        for (String allergen : selectedRestrictions) {
            Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(allergen));
            Matcher matcher = pattern.matcher(recipeText);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, "<span style='color:red;'>" + matcher.group() + "</span>");
            }
            matcher.appendTail(sb);
            recipeText = sb.toString();
        }
        JTextPane recipePane = new JTextPane();
        recipePane.setContentType("text/html");
        recipePane.setText("<html><body style='font-family:Segoe UI; font-size:16px;'>" + recipeText + "</body></html>");
        recipePane.setEditable(false);
        recipePane.setBackground(white);
        JScrollPane scroll = new JScrollPane(recipePane);
        recipePanel.add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        styleButton(backButton);
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "items"));
        buttonPanel.add(backButton);
        recipePanel.add(buttonPanel, BorderLayout.SOUTH);

        cardPanel.add(recipePanel, "recipe");
    }

    private void styleButton(JButton button) {
        button.setFont(optionFont);
        button.setBackground(accentColor);
        button.setForeground(white);
        button.setFocusPainted(false);
    }

    private void navigateBack() {
        if (currentStep > 0) {
            saveUserPreferences();
            currentStep--;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        }
    }

    // -------------------- Inner Class: ResultsListCellRenderer --------------------
    private class ResultsListCellRenderer extends JPanel implements ListCellRenderer<Restaurant> {
        private JLabel nameLabel = new JLabel();
        private JLabel itemsLabel = new JLabel();

        public ResultsListCellRenderer() {
            setLayout(new BorderLayout());
            nameLabel.setFont(optionFont);
            itemsLabel.setFont(optionFont.deriveFont(Font.ITALIC, 14f));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Restaurant> list, Restaurant value, int index, boolean isSelected, boolean cellHasFocus) {
            removeAll();
            if (value == null) {
                nameLabel.setText("No matching restaurants found.");
                add(nameLabel, BorderLayout.CENTER);
            } else {
                nameLabel.setText(value.name);
                String matching = restaurantMatchingItems.getOrDefault(value, "");
                itemsLabel.setText("Matching Items: " + matching);
                add(nameLabel, BorderLayout.NORTH);
                add(itemsLabel, BorderLayout.SOUTH);
            }
            setBackground(isSelected ? accentColor : white);
            setForeground(isSelected ? white : darkGray);
            return this;
        }
    }
}
