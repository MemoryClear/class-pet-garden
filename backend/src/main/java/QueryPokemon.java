import java.sql.*;

public class QueryPokemon {
    public static void main(String[] args) {
        try { Class.forName("org.sqlite.JDBC"); } catch(Exception e) { e.printStackTrace(); }
        String url = "jdbc:sqlite:D:/workspace/class-pet-garden/backend/classpet.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            var rs = conn.createStatement().executeQuery("SELECT id, student_id, pokedex_id, name, image FROM student_pokemon");
            System.out.println("StudentPokemon:");
            while (rs.next()) {
                System.out.println("  pokedex_id=" + rs.getInt(3) + " name=" + rs.getString(4) + " image=" + rs.getString(5));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}