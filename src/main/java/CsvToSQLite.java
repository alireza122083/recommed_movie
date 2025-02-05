import java.io.*;
import java.sql.*;

public class CsvToSQLite {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:movies.db";

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            Statement stmt = conn.createStatement();

            // ایجاد جدول‌ها
            stmt.execute("CREATE TABLE IF NOT EXISTS movies (id TEXT PRIMARY KEY, title TEXT, year INTEGER, genre TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS cast (movie_id TEXT, person_name TEXT, role TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS ratings (movie_id TEXT PRIMARY KEY, rating REAL, votes INTEGER)");

            // وارد کردن داده‌ها از CSV
            insertCSVIntoDatabase(conn, "movies.csv", "movies", 4);
            insertCSVIntoDatabase(conn, "cast.csv", "cast", 3);
            insertCSVIntoDatabase(conn, "ratings.csv", "ratings", 3);

            System.out.println("✅ تمام داده‌ها به SQLite وارد شدند!");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
    private static void insertCSVIntoDatabase(Connection conn, String csvFile, String tableName, int columnCount) throws SQLException, IOException {
        String insertSQL = "INSERT OR REPLACE INTO " + tableName + " VALUES (" + "?,".repeat(columnCount - 1) + "?)";

        conn.setAutoCommit(false); // 🚀 خاموش کردن AutoCommit برای افزایش سرعت

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile));
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            String line;
            int batchSize = 0;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1);
                if (values.length < columnCount) continue;

                for (int i = 0; i < columnCount; i++) {
                    pstmt.setString(i + 1, values[i]);
                }
                pstmt.addBatch();
                batchSize++;

                if (batchSize % 500 == 0) {  // 🚀 هر ۵۰۰ رکورد را یکجا ذخیره کن
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();  // 🚀 اجرای `COMMIT` بعد از وارد کردن داده‌ها
        } catch (Exception e) {
            conn.rollback(); // اگر خطایی رخ داد، `ROLLBACK` کن
            e.printStackTrace();
        } finally {
            conn.setAutoCommit(true); // دوباره AutoCommit را فعال کن
        }
    }
}