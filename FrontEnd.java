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
    // Add this new field near your other fields:
    private List<Restaurant> currentValidRestaurants = new ArrayList<>();

    private final Color white = Color.WHITE;
    private final Color darkGray = Color.DARK_GRAY;
    private final Font headerFont = new Font("Segoe UI", Font.BOLD, 54);
    private final Font optionFont = new Font("Segoe UI", Font.PLAIN, 24);

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JLabel[] progressLabels;
    private int currentStep = 0;

    // User selections from earlier steps
    private List<String> selectedCuisines = new ArrayList<>();
    private List<String> selectedMealTypes = new ArrayList<>();
    private List<String> selectedRestrictions = new ArrayList<>();
    private List<String> selectedFoodItems = new ArrayList<>();

    // The following fields hold the restaurant, menu, and item the user has chosen later on.
    private Restaurant selectedRestaurant;
    private Menu selectedMenu;
    private MenuItem selectedItem;

    // Database connection (if used)
    static Connection connect;

    // Sample restaurants used for the map and results
    private List<Restaurant> allRestaurants = Arrays.asList(
            new Restaurant("Cool Cat Cafe", "$$", "A", 35.26148263879988, -120.65074302104684),
            new Restaurant("Copper Cafe (Madonna Inn)", "$$$", "A", 35.26754456858061, -120.67471354803263),
            new Restaurant("Cowboy Cookies & Ice Cream", "$", "B+", 35.28003629861211, -120.66350028851153),
            new Restaurant("Doc Burnstein's", "$$", "A", 35.28041943934528, -120.66252590200459),
            new Restaurant("Domino's", "$", "B+", 35.294668938163205, -120.67057371919572),
            new Restaurant("El Pollo Loco", "$$", "B", 35.252762644552206, -120.68540577501963)
    );

    // Demo mapping of restaurants to menus
    private Map<String, List<Menu>> restaurantMenus = new HashMap<>();

    // Field to store the map viewer so we can update its waypoints.
    private JXMapViewer resultsMapViewer;

    // Hold the set of waypoints (updated when filtering valid restaurants).
    private Set<RestaurantWaypoint> restaurantWaypoints;

    // Instead of a JTextArea, we now use a JList for clickable results.
    private JList<Restaurant> resultsList;
    // This map holds the extra info (matching menu items) per restaurant.
    private Map<Restaurant, String> restaurantMatchingItems = new HashMap<>();

    // ----------------- Inner classes for domain objects -----------------

    // Restaurant class
    static class Restaurant {
        String name, price, rating;
        double lat, lon;

        public Restaurant(String name, String price, String rating, double lat, double lon) {
            this.name = name;
            this.price = price;
            this.rating = rating;
            this.lat = lat;
            this.lon = lon;
        }
    }

    // Menu class
    static class Menu {
        String name;
        List<MenuItem> items;

        public Menu(String name, List<MenuItem> items) {
            this.name = name;
            this.items = items;
        }
    }

    // MenuItem class
    static class MenuItem {
        String name;
        String recipe;
        List<String> allergens;

        public MenuItem(String name, String recipe, List<String> allergens) {
            this.name = name;
            this.recipe = recipe;
            this.allergens = allergens;
        }
    }

    // ----------------- Custom Waypoint and Renderer -----------------

    // Custom Waypoint that holds a Restaurant reference
    static class RestaurantWaypoint extends DefaultWaypoint {
        Restaurant restaurant;

        public RestaurantWaypoint(Restaurant restaurant) {
            super(new GeoPosition(restaurant.lat, restaurant.lon));
            this.restaurant = restaurant;
        }
    }

    // Custom renderer that draws a larger, darker blue circle with a white dollar sign inside.
    static class RestaurantWaypointRenderer implements WaypointRenderer<RestaurantWaypoint> {
        @Override
        public void paintWaypoint(Graphics2D g, JXMapViewer map, RestaurantWaypoint wp) {
            Point2D pt = map.getTileFactory().geoToPixel(wp.getPosition(), map.getZoom());
            int x = (int) pt.getX();
            int y = (int) pt.getY();
            int radius = 10; // increased size
            // Draw a darker blue circle
            g.setColor(new Color(0, 0, 139)); // dark blue
            g.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
            // Draw a white dollar sign in the center.
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

    // -------------------- Constructor --------------------
    public FrontEnd() {
        setTitle("FoodFinder");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(white);

        // Initialize demo restaurant menus (demo data)
        initDemoMenus();

        // Header panel
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(white);
        header.setBorder(new EmptyBorder(20, 20, 10, 20));
        JLabel titleLabel = new JLabel("FoodFinder", SwingConstants.CENTER);
        titleLabel.setFont(headerFont);
        titleLabel.setForeground(accentColor);
        header.add(titleLabel, BorderLayout.NORTH);
        add(header, BorderLayout.NORTH);

        // Card panel with one card per step/view.
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(white);
        cardPanel.add(createCuisinePanel(), "cuisine");
        cardPanel.add(createMealTypePanel(), "meal");
        cardPanel.add(createRestrictionsPanel(), "restrictions");
        cardPanel.add(createFoodItemPanel(), "food");
        cardPanel.add(createResultsPanel(), "results");
        // Menus, Items, and Recipe panels will be added dynamically.
        add(cardPanel, BorderLayout.CENTER);

        // Progress panel (for the first four steps)
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

    // -------------------- Demo Data Initialization --------------------
    private void initDemoMenus() {
        // For demonstration, assign each restaurant a list of menus and items.
        restaurantMenus.put("Cool Cat Cafe", Arrays.asList(
                new Menu("Breakfast", Arrays.asList(
                        new MenuItem("Pancakes", "Fluffy pancakes with syrup. Contains dairy.", Arrays.asList("Dairy")),
                        new MenuItem("Omelette", "Egg omelette with veggies. Contains eggs.", Arrays.asList("Eggs"))
                )),
                new Menu("Lunch", Arrays.asList(
                        new MenuItem("Grilled Cheese", "Cheesy goodness on toasted bread. Contains dairy, gluten.", Arrays.asList("Dairy", "Gluten")),
                        new MenuItem("Salad", "Mixed greens with vinaigrette.", new ArrayList<>())
                ))
        ));
        restaurantMenus.put("Copper Cafe (Madonna Inn)", Arrays.asList(
                new Menu("Lunch", Arrays.asList(
                        new MenuItem("Pasta Primavera", "Pasta with fresh vegetables. Contains gluten.", Arrays.asList("Gluten")),
                        new MenuItem("Soup of the Day", "Seasonal soup.", new ArrayList<>())
                )),
                new Menu("Dinner", Arrays.asList(
                        new MenuItem("Steak", "Grilled steak with mashed potatoes. Contains dairy.", Arrays.asList("Dairy")),
                        new MenuItem("Wine Pairing", "A selection of red wines.", new ArrayList<>())
                ))
        ));
        restaurantMenus.put("Cowboy Cookies & Ice Cream", Arrays.asList(
                new Menu("Dessert", Arrays.asList(
                        new MenuItem("Chocolate Chip Cookie", "Crispy cookie with chocolate chips. Contains gluten.", Arrays.asList("Gluten")),
                        new MenuItem("Vanilla Ice Cream", "Creamy ice cream. Contains dairy.", Arrays.asList("Dairy"))
                ))
        ));
        restaurantMenus.put("Doc Burnstein's", Arrays.asList(
                new Menu("Brunch", Arrays.asList(
                        new MenuItem("Eggs Benedict", "Poached eggs on English muffins. Contains eggs and gluten.", Arrays.asList("Eggs", "Gluten")),
                        new MenuItem("Fruit Bowl", "Seasonal fruits.", new ArrayList<>())
                )),
                new Menu("Dinner", Arrays.asList(
                        new MenuItem("Salmon", "Grilled salmon with lemon butter. Contains dairy.", Arrays.asList("Dairy")),
                        new MenuItem("Risotto", "Creamy risotto with mushrooms. Contains dairy.", Arrays.asList("Dairy"))
                ))
        ));
        restaurantMenus.put("Domino's", Arrays.asList(
                new Menu("Pizza Specials", Arrays.asList(
                        new MenuItem("Pepperoni Pizza", "Classic pepperoni pizza. Contains gluten, dairy.", Arrays.asList("Gluten", "Dairy")),
                        new MenuItem("Veggie Pizza", "Pizza loaded with vegetables. Contains gluten, dairy.", Arrays.asList("Gluten", "Dairy"))
                ))
        ));
        restaurantMenus.put("El Pollo Loco", Arrays.asList(
                new Menu("Grill", Arrays.asList(
                        new MenuItem("Grilled Chicken", "Spicy grilled chicken. Contains none.", new ArrayList<>()),
                        new MenuItem("Chicken Tacos", "Tacos with grilled chicken. Contains gluten.", Arrays.asList("Gluten"))
                )),
                new Menu("Salads", Arrays.asList(
                        new MenuItem("Caesar Salad", "Romaine with Caesar dressing. Contains dairy, gluten.", Arrays.asList("Dairy", "Gluten")),
                        new MenuItem("House Salad", "Fresh mixed greens.", new ArrayList<>())
                ))
        ));
    }

    // -------------------- Panel Creation Methods --------------------

    // Returns card name based on current step index.
    private String getCardName(int step) {
        switch (step) {
            case 0:
                return "cuisine";
            case 1:
                return "meal";
            case 2:
                return "restrictions";
            case 3:
                return "food";
            default:
                return "results";
        }
    }

    // Update progress label colors.
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

    private JPanel createCuisinePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel questionLabel = new JLabel("What kind(s) of cuisine do you want?", SwingConstants.CENTER);
        questionLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        questionLabel.setForeground(darkGray);
        panel.add(questionLabel, BorderLayout.NORTH);

        // Retrieve cuisines from database or fallback.
        List<String> cuisineList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password");
            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT distinct cuisine FROM Restaurant;");
            while (rs.next()) {
                cuisineList.add(rs.getString(1));
            }
        } catch (Exception e) {
            cuisineList = Arrays.asList("Italian", "Japanese", "Mexican", "American");
        }
        String[] cuisines = cuisineList.toArray(new String[0]);
        PagedSearchPanel optionsPanel = new PagedSearchPanel(cuisines);
        panel.add(optionsPanel, BorderLayout.CENTER);

        // Navigation buttons.
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

        List<String> typeList = new ArrayList<>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password");
            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT distinct type FROM Menu;");
            while (rs.next()) {
                typeList.add(rs.getString(1));
            }
        } catch (Exception e) {
            typeList = Arrays.asList("Breakfast", "Lunch", "Dinner", "Dessert", "Brunch", "Grill", "Salads");
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

        List<String> restrictionList = Arrays.asList("Peanuts", "Shellfish", "Gluten", "Dairy");
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

        List<String> foodList = Arrays.asList("Pizza", "Burger", "Sushi", "Pasta", "Salad");
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
                JOptionPane.showMessageDialog(this, "Please select at least one food item.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            } else {
                selectedFoodItems.addAll(sel);
                updateResultsPanel();  // update list and map based on selection
                currentStep++;
                cardLayout.show(cardPanel, "results");
            }
        });
        buttonPanel.add(backButton);
        buttonPanel.add(continueButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------- Update Results Panel --------------------
    private void updateResultsPanel() {
        // Build the list of valid restaurants and compute matching items.
        Set<Restaurant> validRestaurantsSet = new HashSet<>();
        restaurantMatchingItems.clear();
        for (Restaurant r : allRestaurants) {
            List<Menu> menus = restaurantMenus.get(r.name);
            if (menus != null) {
                outer:
                for (Menu menu : menus) {
                    for (MenuItem mi : menu.items) {
                        for (String food : selectedFoodItems) {
                            if (mi.name.toLowerCase().contains(food.toLowerCase())) {
                                validRestaurantsSet.add(r);
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        // Save the complete list for filtering
        currentValidRestaurants = new ArrayList<>(validRestaurantsSet);

        DefaultListModel<Restaurant> listModel = new DefaultListModel<>();
        if (currentValidRestaurants.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No matching restaurants found.", "Results", JOptionPane.INFORMATION_MESSAGE);
        } else {
            for (Restaurant r : currentValidRestaurants) {
                // Compute matching items for each restaurant.
                List<String> matchingItems = new ArrayList<>();
                List<Menu> menus = restaurantMenus.get(r.name);
                if (menus != null) {
                    for (Menu menu : menus) {
                        for (MenuItem mi : menu.items) {
                            for (String food : selectedFoodItems) {
                                if (mi.name.toLowerCase().contains(food.toLowerCase())) {
                                    matchingItems.add(mi.name);
                                    break;
                                }
                            }
                        }
                    }
                }
                restaurantMatchingItems.put(r, String.join(", ", matchingItems));
                listModel.addElement(r);
            }
        }
        resultsList.setModel(listModel);

        // (Keep your existing map update code below…)
        Set<RestaurantWaypoint> validWaypoints = new HashSet<>();
        for (Restaurant r : currentValidRestaurants) {
            validWaypoints.add(new RestaurantWaypoint(r));
        }
        restaurantWaypoints = validWaypoints;
        WaypointPainter<RestaurantWaypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(restaurantWaypoints);
        waypointPainter.setRenderer(new RestaurantWaypointRenderer());
        resultsMapViewer.setOverlayPainter(waypointPainter);
    }


    // -------------------- Map Viewer with Custom Markers --------------------
    private JXMapViewer createMapViewer(List<Restaurant> restaurants) {
        JXMapViewer mapViewer = new JXMapViewer() {
            @Override
            public String getToolTipText(MouseEvent e) {
                for (RestaurantWaypoint wp : restaurantWaypoints) {
                    Point2D pt = getTileFactory().geoToPixel(wp.getPosition(), getZoom());
                    int dx = (int) pt.getX() - e.getX();
                    int dy = (int) pt.getY() - e.getY();
                    if (Math.hypot(dx, dy) < 15) { // increased threshold
                        String rating = wp.restaurant.rating;
                        String color;
                        if (rating.toUpperCase().contains("A")) {
                            color = "green";
                        } else if (rating.toUpperCase().contains("B")) {
                            color = "lightgreen";
                        } else {
                            color = "yellow";
                        }
                        // Debug print to confirm tooltip detection.
                        System.out.println("Hover detected near " + wp.restaurant.name);
                        return "<html><b>" + wp.restaurant.name + "</b> - <span style='color:" + color + ";'>" + rating + "</span></html>";
                    }
                }
                return null;
            }
        };
        mapViewer.setTileFactory(new DefaultTileFactory(new OSMTileFactoryInfo()));
        GeoPosition center = new GeoPosition(35.2704, -120.6631);
        mapViewer.setZoom(5);
        mapViewer.setAddressLocation(center);

        // Activate tooltips immediately.
        mapViewer.setToolTipText("");
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

        // Existing map click listener (if needed) can remain here.
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (RestaurantWaypoint wp : restaurantWaypoints) {
                    Point2D pt = mapViewer.getTileFactory().geoToPixel(wp.getPosition(), mapViewer.getZoom());
                    int dx = (int) pt.getX() - e.getX();
                    int dy = (int) pt.getY() - e.getY();
                    if (Math.hypot(dx, dy) < 15) {
                        System.out.println("Clicked on " + wp.restaurant.name);
                        // This click handling on the map remains separate from the results list.
                        // (You could merge the behavior if desired.)
                        selectedRestaurant = wp.restaurant;
                        updateMenusPanel(selectedRestaurant);
                        cardLayout.show(cardPanel, "menus");
                        break;
                    }
                }
            }
        });

        return mapViewer;
    }

    // -------------------- Results Panel --------------------
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Search field for filtering restaurants.
        JTextField searchField = new JTextField();
        searchField.setFont(optionFont);
        panel.add(searchField, BorderLayout.NORTH);

        // Create the JList to display restaurants.
        resultsList = new JList<>();
        resultsList.setCellRenderer(new ResultsListCellRenderer());
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Restaurant clicked = resultsList.getSelectedValue();
                if (clicked != null) {
                    // If already selected, open the menus panel.
                    if (selectedRestaurant != null && selectedRestaurant.equals(clicked)) {
                        updateMenusPanel(clicked);
                        cardLayout.show(cardPanel, "menus");
                    } else {
                        // Otherwise, set it as the selected restaurant and zoom the map.
                        selectedRestaurant = clicked;
                        GeoPosition pos = new GeoPosition(clicked.lat, clicked.lon);
                        resultsMapViewer.setAddressLocation(pos);
                        resultsMapViewer.setZoom(3); // Zoom in a lot more.
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

        // Add a DocumentListener to the search field to filter the list.
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


    // -------------------- Custom ListCellRenderer for Results List --------------------
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

    // -------------------- Menus Panel --------------------
    private void updateMenusPanel(Restaurant restaurant) {
        JPanel menusPanel = new JPanel(new BorderLayout(10, 10));
        menusPanel.setBackground(white);
        menusPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Menus at " + restaurant.name, SwingConstants.CENTER);
        titleLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(darkGray);
        menusPanel.add(titleLabel, BorderLayout.NORTH);

        List<Menu> menus = restaurantMenus.get(restaurant.name);
        if (menus == null) {
            menus = new ArrayList<>();
        }
        JPanel menuListPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        for (Menu menu : menus) {
            JButton btn = new JButton(menu.name);
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

    // -------------------- Items Panel --------------------
    private void updateItemsPanel(Menu menu) {
        JPanel itemsPanel = new JPanel(new BorderLayout(10, 10));
        itemsPanel.setBackground(white);
        itemsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Menu Items: " + menu.name, SwingConstants.CENTER);
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
                if (selectedFoodItems.contains(mi.name) && mi.name.toLowerCase().contains(filter)) {
                    JButton btn = new JButton(mi.name);
                    btn.setFont(optionFont);
                    btn.setBackground(white); // keep same background
                    // Removed gold border
                    btn.addActionListener(ev -> {
                        selectedItem = mi;
                        updateRecipePanel(mi);
                        cardLayout.show(cardPanel, "recipe");
                    });
                    itemListPanel.add(btn);
                }
            }
            for (MenuItem mi : menu.items) {
                if (!selectedFoodItems.contains(mi.name) && mi.name.toLowerCase().contains(filter)) {
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

    // -------------------- Recipe Panel --------------------
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

    // -------------------- Helper: Button Styling --------------------
    private void styleButton(JButton button) {
        button.setFont(optionFont);
        button.setBackground(accentColor);
        button.setForeground(white);
        button.setFocusPainted(false);
    }

    // -------------------- Helper: Navigation --------------------
    private void navigateBack() {
        if (currentStep > 0) {
            currentStep--;
            cardLayout.show(cardPanel, getCardName(currentStep));
            updateProgressLabels();
        }
    }

    // -------------------- Inner Class: PagedSearchPanel --------------------
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

    // -------------------- Main --------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FrontEnd app = new FrontEnd();
            app.setVisible(true);
        });
    }
}
