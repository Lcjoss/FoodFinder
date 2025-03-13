package FoodFinder.dao;

import FoodFinder.domain.Restaurant;
import java.sql.*;
import java.util.*;

public class RestaurantDAO {
    public static void deleteRestaurant(int restaurantId) {
        String sql = "DELETE FROM Restaurant WHERE rid = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, restaurantId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No restaurant found with id: " + restaurantId);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error deleting restaurant", ex);
        }
    }
    public static void addRestaurant(Restaurant restaurant) {
        // Combine latitude and longitude into a coordinate string.
        String coordString = restaurant.lat + "," + restaurant.lon;
        String sql = "INSERT INTO Restaurant (rname, cuisine, price, coordinates, rating) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, restaurant.name);
            pstmt.setString(2, restaurant.cuisine);
            pstmt.setString(3, restaurant.price);
            pstmt.setString(4, coordString);
            pstmt.setString(5, restaurant.rating);
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error adding restaurant: " + e.getMessage(), e);
        }
    }

    public static List<Restaurant> getAllRestaurants() {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT rid, rname, cuisine, price, coordinates, rating FROM Restaurant";
        try (Connection conn = DatabaseHelper.getConnection();
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

    public static List<String> getDistinctCuisines() {
        List<String> cuisines = new ArrayList<>();
        String sql = "SELECT DISTINCT cuisine FROM Restaurant";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cuisines.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cuisines;
    }

    public static List<Restaurant> getFilteredRestaurants(List<String> selectedCuisines,
                                                          List<String> selectedMealTypes,
                                                          List<String> selectedFoodItems,
                                                          List<String> selectedRestrictions) {
        List<Restaurant> list = new ArrayList<>();
        if (selectedFoodItems.isEmpty()) return list;
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

    public static List<String> getMatchingItemsForRestaurant(int restaurantId,
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
        try (Connection conn = DatabaseHelper.getConnection();
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
}
