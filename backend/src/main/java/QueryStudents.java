import java.sql.*;

public class QueryStudents {
    public static void main(String[] args) {
        try { Class.forName("org.sqlite.JDBC"); } catch(Exception e) { e.printStackTrace(); }
        String url = "jdbc:sqlite:D:/workspace/class-pet-garden/backend/classpet.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            var rs = conn.createStatement().executeQuery("SELECT id, name, pet_icon, represent_pokemon_id FROM students");
            System.out.println("Students:");
            while (rs.next()) {
                System.out.println("  name=" + rs.getString(2) + " pet_icon=" + rs.getString(3) + " represent_pokemon_id=" + rs.getString(4));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}