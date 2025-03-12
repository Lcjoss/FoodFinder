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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                foodItems.add(rs.getString("iname"));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return foodItems;
    }

    // New helper method: get meal types for the selected cuisines.
    public static List<String> getMealTypesForCuisines(List<String> cuisines) {
        List<String> typeList = new ArrayList<>();
        if (cuisines.isEmpty()) {
            String sql = "SELECT DISTINCT type FROM Menu";
            try (Connection conn = DatabaseHelper.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    typeList.add(rs.getString(1));
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                        typeList.add(rs.getString(1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            e.printStackTrace();
        }
        return allergens;
    }
}
