package FoodFinder.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jxmapviewer.viewer.GeoPosition;
import FoodFinder.dao.MenuDAO;
import FoodFinder.dao.RestaurantDAO;
import FoodFinder.domain.Menu;
import FoodFinder.domain.MenuItem;
import FoodFinder.domain.Restaurant;
import FoodFinder.map.MapUtils;
import FoodFinder.map.RestaurantWaypoint;
import FoodFinder.map.RestaurantWaypointRenderer;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.WaypointPainter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class AdminPagePanel extends JPanel {
    // -------------------- UI Constants --------------------
    private final Color accentColor = Color.decode("#FF6666");
    private final Color white = Color.WHITE;
    private final Color darkGray = Color.DARK_GRAY;
    private final Font headerFont = new Font("Segoe UI", Font.BOLD, 54);
    private final Font optionFont = new Font("Segoe UI", Font.PLAIN, 24);

    // -------------------- Global State --------------------
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private Restaurant selectedRestaurant;
    private Menu selectedMenu;
    private MenuItem selectedItem;
    private JXMapViewer resultsMapViewer;
    private JList<Restaurant> resultsList;
    private List<Restaurant> currentValidRestaurants = new ArrayList<>();
    private List<Restaurant> allRestaurants;

    public AdminPagePanel() {
        setLayout(new BorderLayout());
        setBackground(white);

        // Initialize internal card panel.
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(white);

        // Load restaurants from DB.
        allRestaurants = RestaurantDAO.getAllRestaurants();

        // Add the results panel (other panels are added dynamically).
        cardPanel.add(createResultsPanel(), "results");
        add(cardPanel, BorderLayout.CENTER);
    }

    // -------------------- Results Panel --------------------
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(white);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Search field.
        JTextField searchField = new JTextField();
        searchField.setFont(optionFont);
        panel.add(searchField, BorderLayout.NORTH);

        // JList for restaurants.
        resultsList = new JList<>();
        resultsList.setCellRenderer(new ResultsListCellRenderer());
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Restaurant clicked = resultsList.getSelectedValue();
                if (clicked != null) {
                    // If the same restaurant is clicked again (already selected), navigate to its menus.
                    if (selectedRestaurant != null && selectedRestaurant.equals(clicked)) {
                        updateMenusPanel(clicked);
                        cardLayout.show(cardPanel, "menus");
                    } else {
                        // First click: select the restaurant.
                        selectedRestaurant = clicked;
                    }
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(resultsList);

        // Map viewer.
        resultsMapViewer = MapUtils.createMapViewer(allRestaurants);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, resultsMapViewer);
        splitPane.setDividerLocation(300);
        panel.add(splitPane, BorderLayout.CENTER);

        // Buttons panel.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton addRestButton = new JButton("Add Restaurant");
        styleButton(addRestButton);
        addRestButton.addActionListener(e -> openAddRestaurantDialog());
        buttonPanel.add(addRestButton);

        // New Delete Restaurant button.
        JButton deleteRestButton = new JButton("Delete Restaurant");
        styleButton(deleteRestButton);
        deleteRestButton.addActionListener(e -> {
            Restaurant selected = resultsList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(AdminPagePanel.this,
                        "Please select a restaurant to delete.",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(AdminPagePanel.this,
                    "Delete restaurant: " + selected.name + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    RestaurantDAO.deleteRestaurant(selected.id);
                    JOptionPane.showMessageDialog(AdminPagePanel.this, "Restaurant deleted successfully!");
                    allRestaurants = RestaurantDAO.getAllRestaurants();
                    updateResultsPanel();
                    resultsMapViewer = MapUtils.createMapViewer(allRestaurants);
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(AdminPagePanel.this,
                            "Error deleting restaurant: " + ex.getMessage(),
                            "Delete Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        buttonPanel.add(deleteRestButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Filter logic.
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

        updateResultsPanel();
        return panel;
    }

    private void updateResultsPanel() {
        currentValidRestaurants = allRestaurants;
        DefaultListModel<Restaurant> listModel = new DefaultListModel<>();
        for (Restaurant r : currentValidRestaurants) {
            listModel.addElement(r);
        }
        resultsList.setModel(listModel);

        // Update map overlay.
        Set<RestaurantWaypoint> waypoints = new HashSet<>();
        for (Restaurant r : currentValidRestaurants) {
            waypoints.add(new RestaurantWaypoint(r));
        }
        WaypointPainter<RestaurantWaypoint> painter = new WaypointPainter<>();
        painter.setWaypoints(waypoints);
        painter.setRenderer(new RestaurantWaypointRenderer());
        resultsMapViewer.setOverlayPainter(painter);

        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // -------------------- Add Restaurant Dialog --------------------
    private void openAddRestaurantDialog() {
        // Create a new modal dialog.
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Restaurant", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        // Create form fields for restaurant info including an address field.
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JTextField nameField = new JTextField();
        JTextField cuisineField = new JTextField();
        JComboBox<String> priceCombo = new JComboBox<>(new String[]{"$", "$$", "$$$", "$$$$", "$$$$$"});
        JTextField ratingField = new JTextField();
        JTextField addressField = new JTextField();

        formPanel.add(new JLabel("Restaurant Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Cuisine:"));
        formPanel.add(cuisineField);
        formPanel.add(new JLabel("Price:"));
        formPanel.add(priceCombo);
        formPanel.add(new JLabel("Rating:"));
        formPanel.add(ratingField);
        formPanel.add(new JLabel("Address:"));
        formPanel.add(addressField);

        dialog.add(formPanel, BorderLayout.CENTER);

        // OK and Cancel buttons.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        styleButton(okButton);
        styleButton(cancelButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // OK action: convert address to coordinates and add restaurant.
        okButton.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String cuisine = cuisineField.getText().trim();
            String price = (String) priceCombo.getSelectedItem();
            String rating = ratingField.getText().trim();
            String address = addressField.getText().trim();
            if(name.isEmpty() || cuisine.isEmpty() || rating.isEmpty() || address.isEmpty()){
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Convert address to coordinates (stub implementation).
            GeoPosition pos = geocodeAddress(address);
            if(pos == null) {
                JOptionPane.showMessageDialog(dialog, "Unable to geocode address.", "Geocode Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            double lat = pos.getLatitude();
            double lon = pos.getLongitude();

            // Create new Restaurant using your class constructor.
            Restaurant newRest = new Restaurant(0, name, cuisine, price, rating, lat, lon);
            try {
                RestaurantDAO.addRestaurant(newRest);
                JOptionPane.showMessageDialog(dialog, "Restaurant added successfully!");
                // Refresh restaurant list.
                allRestaurants = RestaurantDAO.getAllRestaurants();
                updateResultsPanel();
                resultsMapViewer = MapUtils.createMapViewer(allRestaurants);
                dialog.dispose();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding restaurant: " + ex.getMessage(), "Add Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(ae -> dialog.dispose());
        dialog.setVisible(true);
    }

    private GeoPosition geocodeAddress(String address) {
        try {
            // Build the URL for the Nominatim API with URL-encoded address.
            String urlStr = "https://nominatim.openstreetmap.org/search?q="
                    + URLEncoder.encode(address, "UTF-8")
                    + "&format=json&limit=1";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // Nominatim requires a valid User-Agent.
            conn.setRequestProperty("User-Agent", "FoodFinderApp/1.0 (your-email@example.com)");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // Parse the JSON response.
            JSONArray results = new JSONArray(response.toString());
            if (results.length() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                double lat = firstResult.getDouble("lat");
                double lon = firstResult.getDouble("lon");
                return new GeoPosition(lat, lon);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error geocoding address: " + e.getMessage(), "Geocode Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    // -------------------- Menus Panel (with Add/Delete) --------------------
    private void updateMenusPanel(Restaurant restaurant) {
        List<Menu> menus = MenuDAO.getMenusForRestaurant(restaurant.id);

        JPanel menusPanel = new JPanel(new BorderLayout(10, 10));
        menusPanel.setBackground(white);
        menusPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Menus at " + restaurant.name, SwingConstants.CENTER);
        titleLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(darkGray);
        menusPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel menuListPanel = new JPanel();
        menuListPanel.setLayout(new BoxLayout(menuListPanel, BoxLayout.Y_AXIS));
        for (Menu menu : menus) {
            JPanel menuRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton menuButton = new JButton(menu.type);
            menuButton.setFont(optionFont);
            menuButton.addActionListener(e -> {
                selectedMenu = menu;
                updateItemsPanel(menu);
                cardLayout.show(cardPanel, "items");
            });
            // Delete menu button.
            JButton deleteMenuButton = new JButton("Delete");
            deleteMenuButton.setFont(optionFont);
            deleteMenuButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Delete menu: " + menu.type + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        MenuDAO.deleteMenu(menu.id);
                        updateMenusPanel(restaurant);
                        cardLayout.show(cardPanel, "menus");
                    } catch (RuntimeException ex) {
                        JOptionPane.showMessageDialog(this, "Error deleting menu: " + ex.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            menuRow.add(menuButton);
            menuRow.add(deleteMenuButton);
            menuListPanel.add(menuRow);
        }
        // Add new menu button.
        JButton addMenuButton = new JButton("Add Menu");
        addMenuButton.setFont(optionFont);
        addMenuButton.addActionListener(e -> {
            String newMenuType = JOptionPane.showInputDialog(this, "Enter new menu type (Breakfast, Cafe, Lunch, Appetizers, Drinks, Dinner, Sweets, Lunch/Dinner):");
            if (newMenuType != null && !newMenuType.trim().isEmpty()) {
                String[] allowedTypes = {"Breakfast", "Cafe", "Lunch", "Appetizers", "Drinks", "Dinner", "Sweets", "Lunch/Dinner"};
                boolean valid = Arrays.stream(allowedTypes).anyMatch(allowed -> allowed.equalsIgnoreCase(newMenuType.trim()));
                if (!valid) {
                    JOptionPane.showMessageDialog(this, "Invalid menu type. Please enter one of: " + String.join(", ", allowedTypes), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    String normalizedType = Arrays.stream(allowedTypes)
                            .filter(allowed -> allowed.equalsIgnoreCase(newMenuType.trim()))
                            .findFirst().orElse(newMenuType.trim());
                    try {
                        MenuDAO.addMenu(selectedRestaurant.id, normalizedType);
                        updateMenusPanel(selectedRestaurant);
                        cardLayout.show(cardPanel, "menus");
                    } catch (RuntimeException ex) {
                        JOptionPane.showMessageDialog(this, "Error adding menu: " + ex.getMessage(), "Add Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        menuListPanel.add(addMenuButton);

        JScrollPane scroll = new JScrollPane(menuListPanel);
        menusPanel.add(scroll, BorderLayout.CENTER);

        // Back button.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        styleButton(backButton);
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "results"));
        buttonPanel.add(backButton);
        menusPanel.add(buttonPanel, BorderLayout.SOUTH);

        cardPanel.add(menusPanel, "menus");
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // -------------------- Items Panel (with Add/Delete) --------------------
    private void updateItemsPanel(Menu menu) {
        List<MenuItem> items = MenuDAO.getMenuItems(menu.id);
        menu.items = items;

        JPanel itemsPanel = new JPanel(new BorderLayout(10, 10));
        itemsPanel.setBackground(white);
        itemsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Menu Items: " + menu.type, SwingConstants.CENTER);
        titleLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(darkGray);
        itemsPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel itemListPanel = new JPanel();
        itemListPanel.setLayout(new BoxLayout(itemListPanel, BoxLayout.Y_AXIS));
        for (MenuItem item : items) {
            JPanel itemRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton itemButton = new JButton(item.name);
            itemButton.setFont(optionFont);
            itemButton.addActionListener(e -> {
                selectedItem = item;
                updateRecipePanel(item);
                cardLayout.show(cardPanel, "recipe");
            });
            // Delete item button.
            JButton deleteItemButton = new JButton("Delete");
            deleteItemButton.setFont(optionFont);
            deleteItemButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Delete item: " + item.name + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        MenuDAO.deleteItem(menu.id, item.name);
                        updateItemsPanel(menu);
                        cardLayout.show(cardPanel, "items");
                    } catch (RuntimeException ex) {
                        JOptionPane.showMessageDialog(this, "Error deleting item: " + ex.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            itemRow.add(itemButton);
            itemRow.add(deleteItemButton);
            itemListPanel.add(itemRow);
        }
        // Add new item button.
        JButton addItemButton = new JButton("Add Item");
        addItemButton.setFont(optionFont);
        addItemButton.addActionListener(e -> {
            String newItemName = JOptionPane.showInputDialog(this, "Enter new item name:");
            if (newItemName != null && !newItemName.trim().isEmpty()) {
                String newRecipe = JOptionPane.showInputDialog(this, "Enter recipe for the new item (comma-separated ingredients):");
                try {
                    MenuDAO.addItem(menu.id, newItemName, newRecipe);
                    updateItemsPanel(menu);
                    cardLayout.show(cardPanel, "items");
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this, "Error adding item: " + ex.getMessage(), "Add Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        itemListPanel.add(addItemButton);

        JScrollPane scroll = new JScrollPane(itemListPanel);
        itemsPanel.add(scroll, BorderLayout.CENTER);

        // Back button.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);
        JButton backButton = new JButton("Back");
        styleButton(backButton);
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "menus"));
        buttonPanel.add(backButton);
        itemsPanel.add(buttonPanel, BorderLayout.SOUTH);

        cardPanel.add(itemsPanel, "items");
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // -------------------- Recipe Panel (with Update/Delete) --------------------
    private void updateRecipePanel(MenuItem item) {
        JPanel recipePanel = new JPanel(new BorderLayout(10, 10));
        recipePanel.setBackground(white);
        recipePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Recipe for: " + item.name, SwingConstants.CENTER);
        titleLabel.setFont(headerFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(darkGray);
        recipePanel.add(titleLabel, BorderLayout.NORTH);

        // Remove "Ingredients: " prefix if present.
        String recipeText = item.recipe;
        String prefix = "Ingredients: ";
        if (recipeText.startsWith(prefix)) {
            recipeText = recipeText.substring(prefix.length());
        }

        // Plain JTextArea for ingredients.
        JTextArea recipeArea = new JTextArea(recipeText);
        recipeArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        recipeArea.setLineWrap(true);
        recipeArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(recipeArea);
        recipePanel.add(scroll, BorderLayout.CENTER);

        // Buttons: Delete Recipe, Update Recipe, Back.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(white);

        JButton deleteRecipeButton = new JButton("Delete Recipe");
        deleteRecipeButton.setFont(optionFont);
        deleteRecipeButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete recipe for: " + item.name + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    MenuDAO.deleteRecipe(selectedMenu.id, item.name);
                    recipeArea.setText("No recipe details available.");
                    updateRecipePanel(item);
                    cardLayout.show(cardPanel, "recipe");
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting recipe: " + ex.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            cardPanel.revalidate();
            cardPanel.repaint();
        });

        JButton updateRecipeButton = new JButton("Update Recipe");
        updateRecipeButton.setFont(optionFont);
        updateRecipeButton.addActionListener(e -> {
            String updatedRecipe = recipeArea.getText();
            try {
                MenuDAO.updateRecipe(selectedMenu.id, item.name, updatedRecipe);
                // Refresh in-memory item data.
                List<MenuItem> updatedItems = MenuDAO.getMenuItems(selectedMenu.id);
                for (MenuItem mi : updatedItems) {
                    if (mi.name.equals(item.name)) {
                        item.recipe = mi.recipe;
                        item.allergens = mi.allergens;
                        break;
                    }
                }
                JOptionPane.showMessageDialog(this, "Recipe updated successfully.");
                updateRecipePanel(item);
                cardLayout.show(cardPanel, "recipe");
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Error updating recipe: " + ex.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton backButton = new JButton("Back");
        styleButton(backButton);
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "items"));

        buttonPanel.add(deleteRecipeButton);
        buttonPanel.add(updateRecipeButton);
        buttonPanel.add(backButton);
        recipePanel.add(buttonPanel, BorderLayout.SOUTH);

        cardPanel.add(recipePanel, "recipe");
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // -------------------- Utility Method --------------------
    private void styleButton(JButton button) {
        button.setFont(optionFont);
        button.setBackground(accentColor);
        button.setForeground(white);
        button.setFocusPainted(false);
    }

    // -------------------- Inner Class: ResultsListCellRenderer --------------------
    private class ResultsListCellRenderer extends JPanel implements ListCellRenderer<Restaurant> {
        private JLabel nameLabel = new JLabel();
        public ResultsListCellRenderer() {
            setLayout(new BorderLayout());
            nameLabel.setFont(optionFont);
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends Restaurant> list, Restaurant value, int index, boolean isSelected, boolean cellHasFocus) {
            removeAll();
            if (value == null) {
                nameLabel.setText("No restaurants found.");
                add(nameLabel, BorderLayout.CENTER);
            } else {
                nameLabel.setText(value.name);
                add(nameLabel, BorderLayout.CENTER);
            }
            setBackground(isSelected ? accentColor : white);
            return this;
        }
    }
}
