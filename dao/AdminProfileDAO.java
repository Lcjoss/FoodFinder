package FoodFinder.dao;

import FoodFinder.domain.AdminProfile;
import java.sql.*;

public class AdminProfileDAO {

    /**
     * Authenticates an administrator using the AdminProfile table.
     *
     * @param username the administrator’s username
     * @param password the administrator’s password (plain text comparison for this demo)
     * @return an AdminProfile object if authentication is successful, or null otherwise.
     */
    public static AdminProfile authenticate(String username, String password) {
        String sql = "SELECT adminId, username, password FROM AdminProfile WHERE username = ?";
        try (Connection con = DatabaseHelper.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    // In production, use secure password hashing.
                    if (dbPassword.equals(password)) {
                        AdminProfile admin = new AdminProfile(rs.getString("username"), dbPassword);
                        admin.setAdminId(rs.getInt("adminId"));
                        return admin;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
