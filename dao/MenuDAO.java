package FoodFinder.dao;

import FoodFinder.domain.Menu;
import FoodFinder.domain.MenuItem;
import java.sql.*;
import java.util.*;

public class MenuDAO {
    public static List<Menu> getMenusForRestaurant(int restaurantId) {
        List<Menu> menus = new ArrayList<>();
        String sql = "SELECT mID, type FROM Menu WHERE rid = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int menuId = rs.getInt("mID");
                    String type = rs.getString("type");
                    List<MenuItem> items = getMenuItems(menuId);
                    menus.add(new Menu(menuId, type, items));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting menus for restaurant: " + e.getMessage(), e);
        }
        return menus;
    }

    public static List<MenuItem> getMenuItems(int menuId) {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT iname FROM Item WHERE mID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String itemName = rs.getString("iname");
                    List<String> allergens = getItemAllergens(menuId, itemName);
                    String recipe = allergens.isEmpty() ? "No recipe details available."
                            : "Ingredients: " + String.join(", ", allergens);
                    items.add(new MenuItem(itemName, recipe, allergens));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting menu items: " + e.getMessage(), e);
        }
        return items;
    }

    public static List<String> getItemAllergens(int menuId, String itemName) {
        List<String> allergens = new ArrayList<>();
        String sql = "SELECT A.ingName FROM Recipe R JOIN Allergen A ON R.ingID = A.ingID WHERE R.mID = ? AND R.iname = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuId);
            pstmt.setString(2, itemName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    allergens.add(rs.getString("ingName"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting item allergens: " + e.getMessage(), e);
        }
        return allergens;
    }

    public static List<String> getFilteredFoodItems(List<String> selectedCuisines,
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
        try (Connection conn = DatabaseHelper.getConnection();
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
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    foodItems.add(rs.getString("iname"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error filtering food items: " + e.getMessage(), e);
        }
        return foodItems;
    }

    // -------------------- New Admin Methods --------------------

    public static void addMenu(int restaurantId, String menuType) {
        List<String> allowedTypes = Arrays.asList("Breakfast", "Cafe", "Lunch", "Appetizers", "Drinks", "Dinner", "Sweets", "Lunch/Dinner");
        if (!allowedTypes.contains(menuType)) {
            throw new RuntimeException("Error: '" + menuType + "' is not a valid menu type.");
        }
        String sql = "INSERT INTO Menu (type, rid) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, menuType);
            pstmt.setInt(2, restaurantId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error adding menu: " + e.getMessage(), e);
        }
    }

    public static void deleteMenu(int menuId) {
        String deleteItemsSql = "DELETE FROM Item WHERE mID = ?";
        try (Connection conn = DatabaseHelper.getConnection()) {
            try (PreparedStatement pstmtItems = conn.prepareStatement(deleteItemsSql)) {
                pstmtItems.setInt(1, menuId);
                pstmtItems.executeUpdate();
            }
            String deleteMenuSql = "DELETE FROM Menu WHERE mID = ?";
            try (PreparedStatement pstmtMenu = conn.prepareStatement(deleteMenuSql)) {
                pstmtMenu.setInt(1, menuId);
                pstmtMenu.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting menu (foreign key constraints may prevent deletion): " + e.getMessage(), e);
        }
    }

    public static void addItem(int menuId, String itemName, String recipe) {
        String sql = "INSERT INTO Item (mID, iname) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuId);
            pstmt.setString(2, itemName);
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error adding item: " + e.getMessage(), e);
        }
        if (recipe != null && !recipe.trim().isEmpty()) {
            String[] ingredients = recipe.split(",");
            for (String ing : ingredients) {
                String ingredient = ing.trim();
                if (!ingredient.isEmpty()) {
                    int ingID = getAllergenId(ingredient);
                    if (ingID != -1) {
                        addRecipeEntry(menuId, itemName, ingID);
                    } else {
                        System.err.println("Ingredient not found in Allergen table: " + ingredient);
                    }
                }
            }
        }
    }

    public static void deleteItem(int menuId, String itemName) {
        String deleteRecipeSql = "DELETE FROM Recipe WHERE mID = ? AND iname = ?";
        try (Connection conn = DatabaseHelper.getConnection()) {
            try (PreparedStatement pstmtRecipe = conn.prepareStatement(deleteRecipeSql)) {
                pstmtRecipe.setInt(1, menuId);
                pstmtRecipe.setString(2, itemName);
                pstmtRecipe.executeUpdate();
            }
            String deleteItemSql = "DELETE FROM Item WHERE mID = ? AND iname = ?";
            try (PreparedStatement pstmtItem = conn.prepareStatement(deleteItemSql)) {
                pstmtItem.setInt(1, menuId);
                pstmtItem.setString(2, itemName);
                pstmtItem.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting item (foreign key constraints may prevent deletion): " + e.getMessage(), e);
        }
    }

    public static void updateRecipe(int menuId, String itemName, String updatedRecipe) {
        deleteRecipe(menuId, itemName);
        if (updatedRecipe != null && !updatedRecipe.trim().isEmpty()) {
            String[] ingredients = updatedRecipe.split(",");
            for (String ing : ingredients) {
                String ingredient = ing.trim();
                if (!ingredient.isEmpty()) {
                    int ingID = getAllergenId(ingredient);
                    if (ingID != -1) {
                        addRecipeEntry(menuId, itemName, ingID);
                    } else {
                        System.err.println("Ingredient not found in Allergen table: " + ingredient);
                    }
                }
            }
        }
    }

    public static void deleteRecipe(int menuId, String itemName) {
        String sql = "DELETE FROM Recipe WHERE mID = ? AND iname = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuId);
            pstmt.setString(2, itemName);
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting recipe (foreign key constraints may prevent deletion): " + e.getMessage(), e);
        }
    }

    // -------------------- Helper Methods --------------------
    private static int getAllergenId(String ingredient) {
        int ingID = -1;
        String sql = "SELECT ingID FROM Allergen WHERE ingName = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ingredient);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ingID = rs.getInt("ingID");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting allergen ID: " + e.getMessage(), e);
        }
        return ingID;
    }

    private static void addRecipeEntry(int menuId, String itemName, int ingID) {
        String sql = "INSERT INTO Recipe (mID, iname, ingID) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuId);
            pstmt.setString(2, itemName);
            pstmt.setInt(3, ingID);
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error adding recipe entry: " + e.getMessage(), e);
        }
    }

    // -------------------- Newly Added Methods --------------------

    public static List<String> getMealTypesForCuisines(List<String> cuisines) {
        List<String> typeList = new ArrayList<>();
        if (cuisines.isEmpty()) {
            String sql = "SELECT DISTINCT type FROM Menu";
            try (Connection conn = DatabaseHelper.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    typeList.add(rs.getString("type"));
                }
            } catch (Exception e) {
                throw new RuntimeException("Error getting meal types: " + e.getMessage(), e);
            }
        } else {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT M.type FROM Menu M JOIN Restaurant R ON M.rid = R.rid WHERE R.cuisine IN (");
            sql.append(String.join(", ", Collections.nCopies(cuisines.size(), "?")));
            sql.append(")");
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                int index = 1;
                for (String c : cuisines) {
                    pstmt.setString(index++, c);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        typeList.add(rs.getString("type"));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error getting meal types: " + e.getMessage(), e);
            }
        }
        return typeList;
    }

    public static List<String> getAllergens() {
        List<String> allergens = new ArrayList<>();
        String sql = "SELECT DISTINCT ingName FROM Allergen";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                allergens.add(rs.getString("ingName"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting allergens: " + e.getMessage(), e);
        }
        return allergens;
    }
}
