package FoodFinder.dao;

import FoodFinder.domain.UserProfile;
import java.sql.*;

public class UserProfileDAO {

    /**
     * Authenticates the user against the UserProfile table.
     *
     * @param username the username entered by the user
     * @param password the password entered by the user (plain text comparison for this demo)
     * @return a UserProfile object if authentication is successful, or null otherwise.
     */
    public static UserProfile authenticate(String username, String password) {
        String sql = "SELECT uid, username, password, selectedCuisines, selectedMealTypes, selectedRestrictions, selectedFoodItems " +
                "FROM UserProfile WHERE username = ?";
        try (Connection con = DatabaseHelper.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    // In production, use secure password hashing.
                    if (dbPassword.equals(password)) {
                        UserProfile user = new UserProfile(rs.getString("username"), dbPassword);
                        user.setUid(rs.getInt("uid"));
                        user.setSelectedCuisines(rs.getString("selectedCuisines"));
                        user.setSelectedMealTypes(rs.getString("selectedMealTypes"));
                        user.setSelectedRestrictions(rs.getString("selectedRestrictions"));
                        user.setSelectedFoodItems(rs.getString("selectedFoodItems"));
                        return user;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Signs up a new user by inserting into the UserProfile table.
     *
     * @param username the username the new user wishes to use
     * @param password the new user’s password
     * @return a UserProfile object if sign-up is successful, or null otherwise.
     */
    public static UserProfile signUp(String username, String password) {
        String sql = "INSERT INTO UserProfile(username, password) VALUES (?, ?)";
        try (Connection con = DatabaseHelper.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, password);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                return null;
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int uid = generatedKeys.getInt(1);
                    UserProfile user = new UserProfile(username, password);
                    user.setUid(uid);
                    // New users start with no stored preferences.
                    return user;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Updates the user’s stored preferences in the UserProfile table.
     *
     * @param user the user whose preferences have changed
     * @return true if the update was successful, false otherwise.
     */
    public static boolean updatePreferences(UserProfile user) {
        String sql = "UPDATE UserProfile SET selectedCuisines = ?, selectedMealTypes = ?, selectedRestrictions = ?, selectedFoodItems = ? WHERE uid = ?";
        try (Connection con = DatabaseHelper.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getSelectedCuisines());
            ps.setString(2, user.getSelectedMealTypes());
            ps.setString(3, user.getSelectedRestrictions());
            ps.setString(4, user.getSelectedFoodItems());
            ps.setInt(5, user.getUid());

            return ps.executeUpdate() > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
