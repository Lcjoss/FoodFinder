package FoodFinder;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.WaypointPainter;
import org.jxmapviewer.viewer.WaypointRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrontEnd extends JFrame {

    // ===================== Fields and Global State =====================
    private final Color accentColor = Color.decode("#FF6666");
    private final Color white = Color.WHITE;
    private final Color darkGray = Color.DARK_GRAY;
    private final Font headerFont = new Font("Segoe UI", Font.BOLD, 54);
    private final Font optionFont = new Font("Segoe UI", Font.PLAIN, 24);

    private List<Restaurant> currentValidRestaurants = new ArrayList<>();
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JLabel[] progressLabels;
    private int currentStep = 0;

    // User selections from earlier steps
    private List<String> selectedCuisines = new ArrayList<>();
    private List<String> selectedMealTypes = new ArrayList<>();
    private List<String> selectedRestrictions = new ArrayList<>();
    private List<String> selectedFoodItems = new ArrayList<>();

    // Selected objects from later steps.
    private Restaurant selectedRestaurant;
    private Menu selectedMenu;
    private MenuItem selectedItem;

    // List of restaurants loaded from the database.
    private List<Restaurant> allRestaurants;

    // Map viewer and related waypoint state.
    private JXMapViewer resultsMapViewer;
    private Set<RestaurantWaypoint> restaurantWaypoints;

    // JList for clickable results.
    private JList<Restaurant> resultsList;
    private Map<Restaurant, String> restaurantMatchingItems = new HashMap<>();

    // References to dynamically updated panels/buttons
    private PagedSearchPanel foodOptionsPanel;
    private JButton foodContinueButton;
    private PagedSearchPanel mealTypeOptionsPanel;
    private JPanel mealTypePanel;

    // ----------------- Domain Object Classes -----------------
    static class Restaurant {
        int id;
        String name, cuisine, price, rating;
        double lat, lon;

        public Restaurant(int id, String name, String cuisine, String price, String rating, double lat, double lon) {
            this.id = id;
            this.name = name;
            this.cuisine = cuisine;
            this.price = price;
            this.rating = rating;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class Menu {
        int id;
        String type;
        List<MenuItem> items;

        public Menu(int id, String type, List<MenuItem> items) {
            this.id = id;
            this.type = type;
            this.items = items;
        }
    }

    static class MenuItem {
        String name, recipe;
        List<String> allergens;

        public MenuItem(String name, String recipe, List<String> allergens) {
            this.name = name;
            this.recipe = recipe;
            this.allergens = allergens;
        }
    }

    // ----------------- Custom Waypoint and Renderer -----------------
    static class RestaurantWaypoint extends DefaultWaypoint {
        Restaurant restaurant;

        public RestaurantWaypoint(Restaurant restaurant) {
            super(new GeoPosition(restaurant.lat, restaurant.lon));
            this.restaurant = restaurant;
        }
    }

    static class RestaurantWaypointRenderer implements WaypointRenderer<RestaurantWaypoint> {
        @Override
        public void paintWaypoint(Graphics2D g, JXMapViewer map, RestaurantWaypoint wp) {
            Point2D pt = map.getTileFactory().geoToPixel(wp.getPosition(), map.getZoom());
            int x = (int) pt.getX();
            int y = (int) pt.getY();
            int radius = 10;
            g.setColor(new Color(0, 0, 139)); // dark blue
            g.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
            g.setColor(Color.WHITE);
            Font font = g.getFont().deriveFont(Font.BOLD, 16f);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            String text = "$";
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();
            g.drawString(text, x - textWidth / 2, y + textHeight / 2 - 2);
        }
    }

    // -------------------- Database Helper Methods --------------------
    private Connection getConnection() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password");
    }

    private List<Restaurant> loadRestaurants() {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT rid, rname, cuisine, price, coordinates, rating FROM Restaurant";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("rid");
                String name = rs.getString("rname");
                String cuisine = rs.getString("cuisine");
                String price = rs.getString("price");
                String rating = rs.getString("rating");
                String coords = rs.getString("coordinates");
                double lat = 0.0, lon = 0.0;
                if (coords != null && coords.contains(",")) {
                    String[] parts = coords.split(",");
                    lat = Double.parseDouble(parts[0].trim());
                    lon = Double.parseDouble(parts[1].trim());
                }
                list.add(new Restaurant(id, name, cuisine, price, rating, lat, lon));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<Menu> loadMenus(int restaurantId) {
        List<Menu> menus = new ArrayList<>();
        String sql = "SELECT mID, type FROM Menu WHERE rid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int menuId = rs.getInt("mID");
                    String type = rs.getString("type");
                    List<MenuItem> items = loadMenuItems(menuId);
                    menus.add(new Menu(menuId, type, items));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return menus;
    }

    private List<MenuItem> loadMenuItems(int menuId) {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT iname FROM Item WHERE mID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String itemName = rs.getString("iname");
                    List<String> allergens = loadItemAllergens(menuId, itemName);
                    String recipe = allergens.isEmpty() ? "No recipe details available."
                            : "Ingredients: " + String.join(", ", allergens);
                    items.add(new MenuItem(itemName, recipe, allergens));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    private List<String> loadItemAllergens(int menuId, String itemName) {
        List<String> allergens = new ArrayList<>();
        String sql = "SELECT A.ingName FROM Recipe R JOIN Allergen A ON R.ingID = A.ingID WHERE R.mID = ? AND R.iname = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuId);
            pstmt.setString(2, itemName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    allergens.add(rs.getString("ingName"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allergens;
    }

    private List<String> loadAllergens() {
        List<String> allergens = new ArrayList<>();
        String sql = "SELECT DISTINCT ingName FROM Allergen";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                allergens.add(rs.getString("ingName"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allergens;
    }

    private List<String> loadFoodItems() {
        List<String> foodItems = new ArrayList<>();
        String sql = "SELECT DISTINCT iname FROM Item";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                foodItems.add(rs.getString("iname"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return foodItems;
    }

    // New method: Load food items filtered by cuisine, meal type, and restrictions (allergens)
    private List<String> loadFilteredFoodItems(List<String> selectedCuisines,
                                               List<String> selectedMealTypes,
                                               List<String> selectedRestrictions) {
        List<String> foodItems = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT I.iname ")
                .append("FROM Restaurant R ")
                .append("JOIN Menu M ON R.rid = M.rid ")
                .append("JOIN Item I ON M.mID = I.mID ")
                .append("WHERE 1=1 ");
        if (!selectedCuisines.isEmpty()) {
            sql.append("AND R.cuisine IN (")
                    .append(String.join(", ", Collections.nCopies(selectedCuisines.size(), "?")))
                    .append(") ");
        }
        if (!selectedMealTypes.isEmpty()) {
            sql.append("AND M.type IN (")
                    .append(String.join(", ", Collections.nCopies(selectedMealTypes.size(), "?")))
                    .append(") ");
        }
        if (!selectedRestrictions.isEmpty()) {
            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM Recipe RC JOIN Allergen A ON RC.ingID = A.ingID ")
                    .append("WHERE RC.mID = I.mID AND RC.iname = I.iname AND A.ingName IN (")
                    .append(String.join(", ", Collections.nCopies(selectedRestrictions.size(), "?")))
                    .append(")) ");
        }
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            if (!selectedCuisines.isEmpty()) {
                for (String c : selectedCuisines) {
                    pstmt.setString(index++, c);
                }
            }
            if (!selectedMealTypes.isEmpty()) {
                for (String m : selectedMealTypes) {
                    pstmt.setString(index++, m);
                }
            }
            if (!selectedRestrictions.isEmpty()) {
                for (String r : selectedRestrictions) {
                    pstmt.setString(index++, r);
                }
            }
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                foodItems.add(rs.getString("iname"));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return foodItems;
    }

    // -------------------- SQL Filtering Methods --------------------
    private List<Restaurant> loadFilteredRestaurants(List<String> selectedCuisines,
                                                     List<String> selectedMealTypes,
                                                     List<String> selectedFoodItems,
                                                     List<String> selectedRestrictions) {
        List<Restaurant> list = new ArrayList<>();
        if (selectedFoodItems.isEmpty()) return list; // Food item filter is required.
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT R.rid, R.rname, R.cuisine, R.price, R.coordinates, R.rating ")
                .append("FROM Restaurant R ")
                .append("JOIN Menu M ON R.rid = M.rid ")
                .append("JOIN Item I ON M.mID = I.mID ")
                .append("WHERE 1=1 ");
        if (!selectedCuisines.isEmpty()) {
            sql.append("AND R.cuisine IN (")
                    .append(String.join(", ", Collections.nCopies(selectedCuisines.size(), "?")))
                    .append(") ");
        }
        if (!selectedMealTypes.isEmpty()) {
            sql.append("AND M.type IN (")
                    .append(String.join(", ", Collections.nCopies(selectedMealTypes.size(), "?")))
                    .append(") ");
        }
        sql.append("AND (");
        for (int i = 0; i < selectedFoodItems.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("LOWER(I.iname) LIKE ?");
        }
        sql.append(") ");
        if (!selectedRestrictions.isEmpty()) {
            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM Recipe RC JOIN Allergen A ON RC.ingID = A.ingID ")
                    .append("WHERE RC.mID = I.mID AND RC.iname = I.iname AND A.ingName IN (")
                    .append(String.join(", ", Collections.nCopies(selectedRestrictions.size(), "?")))
                    .append(")) ");
        }
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            if (!selectedCuisines.isEmpty()) {
                for (String c : selectedCuisines) {
                    pstmt.setString(index++, c);
                }
            }
            if (!selectedMealTypes.isEmpty()) {
                for (String m : selectedMealTypes) {
                    pstmt.setString(index++, m);
                }
            }
            for (String f : selectedFoodItems) {
                pstmt.setString(index++, "%" + f.toLowerCase() + "%");
            }
            if (!selectedRestrictions.isEmpty()) {
                for (String r : selectedRestrictions) {
                    pstmt.setString(index++, r);
                }
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("rid");
                String name = rs.getString("rname");
                String cuisine = rs.getString("cuisine");
                String price = rs.getString("price");
                String rating = rs.getString("rating");
                String coords = rs.getString("coordinates");
                double lat = 0.0, lon = 0.0;
                if (coords != null && coords.contains(",")) {
                    String[] parts = coords.split(",");
                    lat = Double.parseDouble(parts[0].trim());
                    lon = Double.parseDouble(parts[1].trim());
                }
                list.add(new Restaurant(id, name, cuisine, price, rating, lat, lon));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<String> loadMatchingItemsForRestaurant(int restaurantId,
                                                        List<String> selectedMealTypes,
                                                        List<String> selectedFoodItems,
                                                        List<String> selectedRestrictions) {
        List<String> items = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT I.iname ")
                .append("FROM Menu M JOIN Item I ON M.mID = I.mID ")
                .append("WHERE M.rid = ? ");
        if (!selectedMealTypes.isEmpty()) {
            sql.append("AND M.type IN (")
                    .append(String.join(", ", Collections.nCopies(selectedMealTypes.size(), "?")))
                    .append(") ");
        }
        sql.append("AND (");
        for (int i = 0; i < selectedFoodItems.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("LOWER(I.iname) LIKE ?");
        }
        sql.append(") ");
        if (!selectedRestrictions.isEmpty()) {
            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM Recipe RC JOIN Allergen A ON RC.ingID = A.ingID ")
                    .append("WHERE RC.mID = I.mID AND RC.iname = I.iname AND A.ingName IN (")
                    .append(String.join(", ", Collections.nCopies(selectedRestrictions.size(), "?")))
                    .append(")) ");
        }
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            pstmt.setInt(index++, restaurantId);
            if (!selectedMealTypes.isEmpty()) {
                for (String m : selectedMealTypes) {
                    pstmt.setString(index++, m);
                }
            }
            for (String f : selectedFoodItems) {
                pstmt.setString(index++, "%" + f.toLowerCase() + "%");
            }
            if (!selectedRestrictions.isEmpty()) {
                for (String r : selectedRestrictions) {
                    pstmt.setString(index++, r);
                }
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.add(rs.getString("iname"));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    // -------------------- Constructor --------------------
    public FrontEnd() {
        setTitle("FoodFinder");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(white);

        allRestaurants = loadRestaurants();

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(white);
        header.setBorder(new EmptyBorder(20, 20, 10, 20));
        JLabel titleLabel = new JLabel("FoodFinder", SwingConstants.CENTER);
        titleLabel.setFont(headerFont);
        titleLabel.setForeground(accentColor);
        header.add(titleLabel, BorderLayout.NORTH);
        add(header, BorderLayout.NORTH);

        // Card panel
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(white);
        cardPanel.add(createCuisinePanel(), "cuisine");
        // Create and store the meal type panel for later updating
        mealTypePanel = createMealTypePanel();
        cardPanel.add(mealTypePanel, "meal");
        cardPanel.add(createRestrictionsPanel(), "restrictions");
        cardPanel.add(createFoodItemPanel(), "food");
        cardPanel.add(createResultsPanel(), "results");
        add(cardPanel, BorderLayout.CENTER);

        // Progress panel
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
                        cardLayout.show(cardPanel, getCardName(currentStep));
                        updateProgressLabels();
                    }
                }
            });
            progressPanel.add(progressLabels[i]);
        }
        add(progressPanel, BorderLayout.SOUTH);
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

    // -------------------- Panel Creation Methods --------------------
    private JPanel createCuisinePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind(s) of cuisine do you want?", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> cuisineList = new ArrayList<>();
        String sql = "SELECT DISTINCT cuisine FROM Restaurant";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cuisineList.add(rs.getString(1));
            }
        } catch (Exception e) {
            cuisineList = Arrays.asList("Italian", "Japanese", "Mexican", "American");
        }
        String[] cuisines = cuisineList.toArray(new String[0]);
        PagedSearchPanel optionsPanel = new PagedSearchPanel(cuisines);
        panel.add(optionsPanel, BorderLayout.CENTER);

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
                JOptionPane.showMessageDialog(this, "Please select at least one cuisine.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedCuisines.addAll(sel);
                // Update the meal type panel based on the selected cuisines.
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

    // This method rebuilds the meal type options based on the selected cuisines.
    private void updateMealTypePanel() {
        List<String> typeList = new ArrayList<>();
        if (!selectedCuisines.isEmpty()){
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT M.type FROM Menu M JOIN Restaurant R ON M.rid = R.rid WHERE R.cuisine IN (");
            sql.append(String.join(", ", Collections.nCopies(selectedCuisines.size(), "?")));
            sql.append(")");
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                int index = 1;
                for (String c : selectedCuisines) {
                    pstmt.setString(index++, c);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        typeList.add(rs.getString(1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String[] mealTypes = typeList.toArray(new String[0]);
        if (mealTypeOptionsPanel != null) {
            mealTypeOptionsPanel.updateOptions(mealTypes);
        }
    }

    private JPanel createMealTypePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind of meals are you looking for?", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        // At this point, selectedCuisines has been set by the previous step.
        // Query the meal type options for the chosen cuisines.
        List<String> typeList = new ArrayList<>();
        if (!selectedCuisines.isEmpty()){
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT M.type FROM Menu M JOIN Restaurant R ON M.rid = R.rid WHERE R.cuisine IN (");
            sql.append(String.join(", ", Collections.nCopies(selectedCuisines.size(), "?")));
            sql.append(")");
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                int index = 1;
                for(String c: selectedCuisines){
                    pstmt.setString(index++, c);
                }
                try (ResultSet rs = pstmt.executeQuery()){
                    while(rs.next()){
                        typeList.add(rs.getString(1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Fallback if no cuisines are selected (should not happen)
            String sql = "SELECT DISTINCT type FROM Menu";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()){
                    typeList.add(rs.getString(1));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String[] mealTypes = typeList.toArray(new String[0]);
        mealTypeOptionsPanel = new PagedSearchPanel(mealTypes);
        panel.add(mealTypeOptionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        JButton continueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(continueButton);
        backButton.addActionListener(e -> navigateBack());
        continueButton.addActionListener(e -> {
            selectedMealTypes.clear();
            List<String> sel = mealTypeOptionsPanel.getSelectedOptions();
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one meal type.", "Selection Required", JOptionPane.WARNING_MESSAGE);
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

    private JPanel createRestrictionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("Select allergens or dietary restrictions (optional)", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        List<String> restrictionList = loadAllergens();
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
            selectedRestrictions.addAll(sel);
            // Update the food item panel with filtered options based on selected cuisines, meal types, and restrictions:
            List<String> foodList = loadFilteredFoodItems(selectedCuisines, selectedMealTypes, selectedRestrictions);
            if (foodOptionsPanel != null) {
                foodOptionsPanel.updateOptions(foodList.toArray(new String[0]));
            }
            // Gray out (disable) the Continue button on the Food panel if no food items were found.
            if (foodList.isEmpty()) {
                foodContinueButton.setEnabled(false);
            } else {
                foodContinueButton.setEnabled(true);
            }
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

        // Initialize with an empty array; it will be updated when leaving the restrictions panel.
        foodOptionsPanel = new PagedSearchPanel(new String[0]);
        panel.add(foodOptionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        // Use the class-level continue button reference.
        foodContinueButton = new JButton("Continue");
        styleButton(backButton);
        styleButton(foodContinueButton);
        backButton.addActionListener(e -> navigateBack());
        foodContinueButton.addActionListener(e -> {
            selectedFoodItems.clear();
            List<String> sel = foodOptionsPanel.getSelectedOptions();
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one food item.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedFoodItems.addAll(sel);
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
                        GeoPosition pos = new GeoPosition(clicked.lat, clicked.lon);
                        resultsMapViewer.setAddressLocation(pos);
                        resultsMapViewer.setZoom(3);
                    }
                }
            }
        });
        JScrollPane listScrollPane = new JScrollPane(resultsList);

        resultsMapViewer = createMapViewer(allRestaurants);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, resultsMapViewer);
        splitPane.setDividerLocation(300);
        panel.add(splitPane, BorderLayout.CENTER);

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
        List<Restaurant> filteredRestaurants = loadFilteredRestaurants(selectedCuisines, selectedMealTypes, selectedFoodItems, selectedRestrictions);
        currentValidRestaurants = filteredRestaurants;
        DefaultListModel<Restaurant> listModel = new DefaultListModel<>();
        if (currentValidRestaurants.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No matching restaurants found.", "Results", JOptionPane.INFORMATION_MESSAGE);
        } else {
            for (Restaurant r : currentValidRestaurants) {
                List<String> matchingItems = loadMatchingItemsForRestaurant(r.id, selectedMealTypes, selectedFoodItems, selectedRestrictions);
                restaurantMatchingItems.put(r, String.join(", ", matchingItems));
                listModel.addElement(r);
            }
        }
        resultsList.setModel(listModel);

        restaurantWaypoints = new HashSet<>();
        for (Restaurant r : currentValidRestaurants) {
            restaurantWaypoints.add(new RestaurantWaypoint(r));
        }
        WaypointPainter<RestaurantWaypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(restaurantWaypoints);
        waypointPainter.setRenderer(new RestaurantWaypointRenderer());
        resultsMapViewer.setOverlayPainter(waypointPainter);
    }

    private JXMapViewer createMapViewer(List<Restaurant> restaurants) {
        JXMapViewer mapViewer = new JXMapViewer();
        mapViewer.setTileFactory(new DefaultTileFactory(new OSMTileFactoryInfo()));
        GeoPosition center = new GeoPosition(35.2704, -120.6631);
        mapViewer.setZoom(5);
        mapViewer.setAddressLocation(center);

        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().registerComponent(mapViewer);

        restaurantWaypoints = new HashSet<>();
        for (Restaurant res : restaurants) {
            restaurantWaypoints.add(new RestaurantWaypoint(res));
        }
        WaypointPainter<RestaurantWaypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(restaurantWaypoints);
        waypointPainter.setRenderer(new RestaurantWaypointRenderer());
        mapViewer.setOverlayPainter(waypointPainter);

        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        return mapViewer;
    }

    private void updateMenusPanel(Restaurant restaurant) {
        JPanel menusPanel = new JPanel(new BorderLayout(10, 10));
        menusPanel.setBackground(white);
        menusPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Menus at " + restaurant.name, SwingConstants.CENTER);
        titleLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(darkGray);
        menusPanel.add(titleLabel, BorderLayout.NORTH);

        List<Menu> menus = loadMenus(restaurant.id);
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
            currentStep--;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        }
    }

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

    private class PagedSearchPanel extends JPanel {
        private List<String> originalOptions;
        private List<String> filteredOptions;
        private Map<String, Boolean> selectionMap;
        private int currentPage = 0;
        private final int itemsPerPage = 12;

        private JTextField searchField;
        private JPanel gridPanel;
        private JButton leftButton, rightButton;

        public PagedSearchPanel(String[] options) {
            originalOptions = new ArrayList<>(Arrays.asList(options));
            filteredOptions = new ArrayList<>(originalOptions);
            selectionMap = new HashMap<>();
            for (String opt : originalOptions) {
                selectionMap.put(opt, false);
            }
            setLayout(new BorderLayout(10, 10));

            searchField = new JTextField();
            searchField.setFont(optionFont);
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updateFilter(); }
                public void removeUpdate(DocumentEvent e) { updateFilter(); }
                public void changedUpdate(DocumentEvent e) { updateFilter(); }
            });
            add(searchField, BorderLayout.NORTH);

            gridPanel = new JPanel(new GridLayout(3, 4, 15, 15));
            add(gridPanel, BorderLayout.CENTER);

            JPanel arrowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            leftButton = new JButton("<");
            rightButton = new JButton(">");
            leftButton.setFont(optionFont);
            rightButton.setFont(optionFont);
            leftButton.addActionListener(e -> {
                if (currentPage > 0) { currentPage--; refreshGrid(); }
            });
            rightButton.addActionListener(e -> {
                if ((currentPage + 1) * itemsPerPage < filteredOptions.size()) { currentPage++; refreshGrid(); }
            });
            arrowPanel.add(leftButton);
            arrowPanel.add(rightButton);
            add(arrowPanel, BorderLayout.SOUTH);

            refreshGrid();
        }

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

        private void refreshGrid() {
            gridPanel.removeAll();
            int start = currentPage * itemsPerPage;
            int end = Math.min(start + itemsPerPage, filteredOptions.size());
            if (filteredOptions.isEmpty()) {
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
                    btn.setBackground(selected ? accentColor : white);
                    btn.setForeground(selected ? white : darkGray);
                    btn.addItemListener(e -> {
                        boolean isSelected = btn.isSelected();
                        selectionMap.put(option, isSelected);
                        btn.setBackground(isSelected ? accentColor : white);
                        btn.setForeground(isSelected ? white : darkGray);
                    });
                    gridPanel.add(btn);
                }
                int itemsAdded = end - start;
                for (int i = itemsAdded; i < itemsPerPage; i++) {
                    gridPanel.add(new JPanel());
                }
            }
            leftButton.setEnabled(currentPage > 0);
            rightButton.setEnabled((currentPage + 1) * itemsPerPage < filteredOptions.size());
            gridPanel.revalidate();
            gridPanel.repaint();
        }

        // New method to update the options dynamically
        public void updateOptions(String[] options) {
            originalOptions = new ArrayList<>(Arrays.asList(options));
            filteredOptions = new ArrayList<>(originalOptions);
            selectionMap.clear();
            for (String opt : originalOptions) {
                selectionMap.put(opt, false);
            }
            currentPage = 0;
            refreshGrid();
        }

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
