package FoodFinder;
import java.sql.*;

public class Main {
    static   Connection connect;
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(
                    "jdbc:mysql://ambari-node5.csc.calpoly.edu/foodfinder", "foodfinder", "password"); // Replace with database name (username), username, and password

            Statement statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM Ingredient;");
            while (rs.next()) {
                String ingredientName = rs.getString(1); // name is first field
                System.out.println("Ingredient name = " +
                        studentName);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }}
