import java.io.*;
import java.sql.*;

public class IMDbToSQLite {
    private static final String DATABASE_NAME = "imdb_series.db";

    private static final String TSV_SERIES = "title.basics.tsv";
    private static final String TSV_RATINGS = "title.ratings.tsv";
    private static final String CSV_PRINCIPALS = "principals.csv";
    private static final String CSV_CREW = "crew.csv";

    public static void main(String[] args) {
        try (Connection connection = connectToDatabase()) {
            createTables(connection);

            connection.setAutoCommit(false); // افزایش سرعت با غیرفعال کردن Auto-Commit

            insertSeriesData(connection, TSV_SERIES);
            insertRatingsData(connection, TSV_RATINGS);
            insertPrincipalsData(connection, CSV_PRINCIPALS);
            insertCrewData(connection, CSV_CREW);

            connection.commit(); // انجام همه تراکنش‌ها یکجا
            System.out.println("✅ تمام داده‌ها با موفقیت در دیتابیس ذخیره شدند.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Connection connectToDatabase() throws SQLException {
        String url = "jdbc:sqlite:" + DATABASE_NAME;
        Connection conn = DriverManager.getConnection(url);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA synchronous = OFF;");
            stmt.execute("PRAGMA journal_mode = MEMORY;");
        }

        return conn;
    }
    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS series (id TEXT PRIMARY KEY, title TEXT, year INTEGER, genres TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS ratings (series_id TEXT PRIMARY KEY, imdb_rating REAL, votes INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS actors (series_id TEXT, actor_name TEXT, UNIQUE (series_id, actor_name))");
            stmt.execute("CREATE TABLE IF NOT EXISTS crew (series_id TEXT, director_name TEXT, UNIQUE (series_id, director_name))");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_series ON series(id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ratings ON ratings(series_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_actors ON actors(series_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_crew ON crew(series_id)");
        }
    }

    private static void insertSeriesData(Connection conn, String filePath) {
        String sql = "INSERT INTO series (id, title, year, genres) VALUES (?, ?, ?, ?)";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            br.readLine();
            int count = 0;

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                if (values.length < 9 || !values[1].equals("tvSeries")) continue;

                pstmt.setString(1, values[0]);
                pstmt.setString(2, values[2]);
                pstmt.setInt(3, values[5].equals("\\N") ? 0 : Integer.parseInt(values[5]));
                pstmt.setString(4, values[8]);

                pstmt.addBatch();
                count++;

                if (count % 10000 == 0) pstmt.executeBatch();
            }
            pstmt.executeBatch();
            System.out.println("✅ سریال‌ها وارد دیتابیس شدند.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertRatingsData(Connection conn, String filePath) {
        String sql = "INSERT INTO ratings (series_id, imdb_rating, votes) VALUES (?, ?, ?)";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            br.readLine();
            int count = 0;

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                if (values.length < 3) continue;

                pstmt.setString(1, values[0]);
                pstmt.setDouble(2, values[1].equals("\\N") ? 0.0 : Double.parseDouble(values[1]));
                pstmt.setInt(3, values[2].equals("\\N") ? 0 : Integer.parseInt(values[2]));

                pstmt.addBatch();
                count++;

                if (count % 10000 == 0) pstmt.executeBatch();
            }
            pstmt.executeBatch();
            System.out.println("✅ امتیازات وارد دیتابیس شدند.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertPrincipalsData(Connection conn, String filePath) {
        String sql = "INSERT OR IGNORE INTO actors (series_id, actor_name) VALUES (?, ?)"; // 🚀 جلوگیری از خطای UNIQUE
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            br.readLine(); // رد کردن هدر
            int count = 0;

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 2) continue;

                pstmt.setString(1, values[0]);
                pstmt.setString(2, values[1]);

                pstmt.addBatch();
                count++;

                if (count % 10000 == 0) pstmt.executeBatch();
            }
            pstmt.executeBatch();
            System.out.println("✅ بازیگران وارد دیتابیس شدند.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertCrewData(Connection conn, String filePath) {
        String sql = "INSERT OR IGNORE INTO crew (series_id, director_name) VALUES (?, ?)"; // 🚀 جلوگیری از خطای UNIQUE
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            br.readLine(); // رد کردن هدر
            int count = 0;

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 2) continue;

                pstmt.setString(1, values[0]);
                pstmt.setString(2, values[1]);

                pstmt.addBatch();
                count++;

                if (count % 10000 == 0) pstmt.executeBatch();
            }
            pstmt.executeBatch();
            System.out.println("✅ کارگردانان وارد دیتابیس شدند.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}