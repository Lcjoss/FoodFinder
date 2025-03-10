package FoodFinder;
import java.sql.*;
import java.util.Collections;

public class Main {
    static   Connection connect;
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password"); // Replace with database name (username), username, and password


            Statement statement = connect.createStatement();
            String sql= "Select * from Allergen;";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                System.out.println(rs.getString(2)); // name is first field
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }}
