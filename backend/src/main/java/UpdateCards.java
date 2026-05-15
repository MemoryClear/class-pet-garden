import java.sql.*;

public class UpdateCards {
    public static void main(String[] args) {
        try { Class.forName("org.sqlite.JDBC"); } catch(Exception e) { e.printStackTrace(); }
        String url = "jdbc:sqlite:D:/workspace/class-pet-garden/backend/classpet.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            String sql = "UPDATE students SET pet_change_cards = 3 WHERE pet_change_cards IS NULL OR pet_change_cards = 0";
            int updated = conn.createStatement().executeUpdate(sql);
            System.out.println("Updated " + updated + " students to have 3 pet change cards");

            // 验证
            var rs = conn.createStatement().executeQuery("SELECT id, name, pet_icon, pet_change_cards FROM students LIMIT 5");
            System.out.println("Sample after update:");
            while (rs.next()) {
                System.out.println("  id=" + rs.getString(1) + " name=" + rs.getString(2) + " petIcon=" + rs.getString(3) + " cards=" + rs.getObject(4));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}