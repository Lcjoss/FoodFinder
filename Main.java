package FoodFinder;
import java.sql.*;
import java.util.Collections;

public class Main {
    static   Connection connect;
    public static void main(String[] args) {
//        try {
//            Class.forName("com.mysql.jdbc.Driver");
//            connect = DriverManager.getConnection(
//                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password"); // Replace with database name (username), username, and password
//
//
//            Statement statement = connect.createStatement();
//            String restaurants = "SELECT rid FROM Restaurant WHERE cuisine IN (" +
//                    String.join(",", Collections.nCopies(selectedCuisines.size(), "?")) + ")))";
//            String menus= "Select mid from Menu where type in (" +
//                    String.join(",", Collections.nCopies(selectedMealTypes.size(), "?")) + ")" + "and rid=(";
//            String restrictions="Select I.iname from Item I join Recipe R join Allergen A where ingName not in " +
//                    String.join(",", Collections.nCopies(selectedRestrictions.size(), "?")) + ") and mid=(";
//            String sql= restrictions+menus+restaurants;
//            ResultSet rs = statement.executeQuery(sql);
//            while (rs.next()) {
//                String itemName = rs.getString(1); // name is first field
//            }
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
    }}
