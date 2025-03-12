package FoodFinder.dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseHelper {
    public static Connection getConnection() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password");
    }
}
