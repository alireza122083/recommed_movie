import java.sql.*;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MovieRecommender {

    private static final String DB_URL = "jdbc:sqlite:movies.db";  // دیتابیس محلی فیلم‌ها
    private static final String API_KEY = "a0ca34a93b0593a56abb6b6c74749234";  // کلید API برای TMDb
    private static final String BASE_URL = "https://api.themoviedb.org/3/";  // URL پایه TMDb

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // پرسیدن از کاربر اینکه فیلم می‌خواهد یا سریال
        System.out.println("🎬 آیا به فیلم نیاز دارید یا سریال؟ (فیلم/سریال): ");
        String choice = scanner.nextLine().trim().toLowerCase();

        if (choice.equals("فیلم")) {
            // جستجو در دیتابیس فیلم
            handleMovieSearch(scanner);
        } else if (choice.equals("سریال")) {
            // جستجو از طریق TMDb API برای سریال‌ها
            handleTVShowSearch(scanner);
        } else {
            System.out.println("⛔ گزینه نامعتبر!");
        }


    }

    private static void handleMovieSearch(Scanner scanner) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            while (true) {
                System.out.println("\n🔍 جستجوی فیلم‌های برتر");
                System.out.println("1️⃣ بر اساس بازیگر");
                System.out.println("2️⃣ بر اساس کارگردان");
                System.out.println("3️⃣ بر اساس ژانر");
                System.out.println("4️⃣ بر اساس نام فیلم");
                System.out.println("5️⃣ تست روان‌شناختی برای پیشنهاد فیلم");
                System.out.println("0️⃣ خروج");

                System.out.print("👉 گزینه را انتخاب کن: ");
                int choice = scanner.nextInt();
                scanner.nextLine();  // خواندن خط جدید

                if (choice == 0) break;

                if (choice == 5) {
                    // درخواست از کاربر برای پاسخ به تست روان‌شناختی
                    userProfileTest(scanner, conn, "فیلم");
                    continue;
                }

                System.out.print("🔎 جستجو: ");
                String searchTerm = scanner.nextLine();

                switch (choice) {
                    case 1:
                        searchMoviesByActor(conn, searchTerm);
                        break;
                    case 2:
                        searchMoviesByDirector(conn, searchTerm);
                        break;
                    case 3:
                        searchMoviesByGenre(conn, searchTerm);
                        break;
                    case 4:
                        searchMoviesByTitle(conn, searchTerm);
                        break;
                    default:
                        System.out.println("⛔ گزینه نامعتبر!");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void handleTVShowSearch(Scanner scanner) {
        // جستجو از طریق TMDb API برای سریال‌ها
        while (true) {
            System.out.println("\n🔍 جستجوی سریال‌های برتر");
            System.out.println("1️⃣ بر اساس بازیگر");
            System.out.println("2️⃣ بر اساس کارگردان");
            System.out.println("3️⃣ بر اساس ژانر");
            System.out.println("4️⃣ بر اساس نام سریال");
            System.out.println("5️⃣ تست روان‌شناختی برای پیشنهاد سریال");
            System.out.println("0️⃣ خروج");

            System.out.print("👉 گزینه را انتخاب کن: ");
            int choice = scanner.nextInt();
            scanner.nextLine();  // خواندن خط جدید

            if (choice == 0) break;

            if (choice == 5) {
                // درخواست از کاربر برای پاسخ به تست روان‌شناختی
                userProfileTest(scanner, null, "سریال");
                continue;
            }

            System.out.print("🔎 جستجو: ");
            String searchTerm = scanner.nextLine();

            switch (choice) {
                case 1:
                    searchTVShowsByActorTMDb(searchTerm);
                    break;
                case 2:
                    searchTVShowsByDirectorTMDb(searchTerm);
                    break;
                case 3:
                    searchTVShowsByGenreTMDb(searchTerm);
                    break;
                case 4:
                    searchTVShowsByTitleTMDb(searchTerm);
                    break;
                default:
                    System.out.println("⛔ گزینه نامعتبر!");
            }
        }
    }

    private static void searchMoviesByActor(Connection conn, String actorName) throws SQLException {
        String sql = "SELECT movies.title, ratings.rating FROM movies " +
                "JOIN cast ON movies.id = cast.movie_id " +
                "JOIN ratings ON movies.id = ratings.movie_id " +
                "WHERE cast.person_name = ? " +
                "ORDER BY ratings.rating DESC LIMIT 10;";
        executeQuery(conn, sql, actorName);
    }

    private static void searchMoviesByDirector(Connection conn, String directorName) throws SQLException {
        String sql = "SELECT movies.title, ratings.rating FROM movies " +
                "JOIN cast ON movies.id = cast.movie_id " +
                "JOIN ratings ON movies.id = ratings.movie_id " +
                "WHERE cast.person_name = ? AND cast.role = 'director' " +
                "ORDER BY ratings.rating DESC LIMIT 10;";
        executeQuery(conn, sql, directorName);
    }

    private static void searchMoviesByGenre(Connection conn, String genre) throws SQLException {
        String sql = "SELECT movies.title, ratings.rating FROM movies " +
                "JOIN ratings ON movies.id = ratings.movie_id " +
                "WHERE movies.genre LIKE ? " +
                "ORDER BY ratings.rating DESC LIMIT 10;";
        executeQuery(conn, sql, "%" + genre + "%");
    }

    private static void searchMoviesByTitle(Connection conn, String title) throws SQLException {
        String sql = "SELECT movies.title, ratings.rating FROM movies " +
                "JOIN ratings ON movies.id = ratings.movie_id " +
                "WHERE movies.title LIKE ? " +
                "ORDER BY ratings.rating DESC LIMIT 10;";
        executeQuery(conn, sql, "%" + title + "%");
    }

    private static void executeQuery(Connection conn, String sql, String param) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, param);
            ResultSet rs = pstmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("🎬 " + rs.getString("title") + " ⭐ " + rs.getDouble("rating"));
            }

            if (!found) System.out.println("❌ نتیجه‌ای یافت نشد.");
        }
    }

    private static void searchTVShowsByActorTMDb(String actorName) {
        try {
            String urlString = BASE_URL + "search/person?api_key=" + API_KEY + "&query=" + actorName;
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            System.out.println("نتایج جستجو برای سریال‌ها بر اساس بازیگر:");
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void searchTVShowsByDirectorTMDb(String directorName) {
        try {
            String urlString = BASE_URL + "search/person?api_key=" + API_KEY + "&query=" + directorName;
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            System.out.println("نتایج جستجو برای سریال‌ها بر اساس کارگردان:");
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void searchTVShowsByGenreTMDb(String genre) {
        try {
            String urlString = BASE_URL + "discover/tv?api_key=" + API_KEY + "&with_genres=" + genre;
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            System.out.println("نتایج جستجو برای سریال‌ها بر اساس ژانر:");
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void searchTVShowsByTitleTMDb(String title) {
        try {
            String urlString = BASE_URL + "search/tv?api_key=" + API_KEY + "&query=" + title;
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            System.out.println("نتایج جستجو برای سریال‌ها بر اساس عنوان:");
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // تست روان‌شناختی برای فیلم‌ها و سریال‌ها
    private static void userProfileTest(Scanner scanner, Connection conn, String type) {
        System.out.println("📊 به چند سوال پاسخ دهید تا بهترین پیشنهادات را دریافت کنید!");

        // سوالات تست روان‌شناختی
        System.out.print("کدام ژانر فیلم یا سریال را بیشتر می‌پسندید؟ (کمدی، درام، علمی-تخیلی، ترسناک): ");
        String genre = scanner.nextLine();

        System.out.print("آخرین فیلم یا سریالی که دیدید و دوست داشتید چه بود؟ ");
        String lastWatched = scanner.nextLine();

        System.out.print("آیا ترجیح می‌دهید فیلم‌ها/سریال‌های کلاسیک را تماشا کنید؟ (بله/خیر): ");
        String classicPreference = scanner.nextLine();

        System.out.print("چند ساعت در هفته فیلم یا سریال تماشا می‌کنید؟ ");
        int hoursPerWeek = scanner.nextInt();
        scanner.nextLine(); // خواندن خط جدید

        // پیشنهادات بر اساس تست روان‌شناختی
        System.out.println("نتایج بر اساس ترجیحات شما:");
        System.out.println("ژانر: " + genre);
        System.out.println("آخرین فیلم یا سریال: " + lastWatched);
        System.out.println("کلاسیک‌ها: " + classicPreference);
        System.out.println("ساعات تماشا در هفته: " + hoursPerWeek);

        if (type.equals("فیلم")) {
            recommendMovies(conn, genre, lastWatched, hoursPerWeek);
        } else if (type.equals("سریال")) {
            recommendTVShows(genre, lastWatched, hoursPerWeek);
        }
    }

    private static void recommendMovies(Connection conn, String genre, String lastWatched, int hoursPerWeek) {
        try {
            // اینجا می‌توانید منطق پیشنهاد فیلم‌ها بر اساس ورودی‌های کاربر را اضافه کنید
            System.out.println("پیشنهاد فیلم‌ها بر اساس اطلاعات شما:");
            // فرض کنید یک جستجوی ساده انجام می‌دهیم:
            String sql = "SELECT movies.title, ratings.rating FROM movies WHERE movies.genre LIKE ? ORDER BY ratings.rating DESC LIMIT 5;";
            executeQuery(conn, sql, "%" + genre + "%");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void recommendTVShows(String genre, String lastWatched, int hoursPerWeek) {
        try {
            // اینجا می‌توانید منطق پیشنهاد سریال‌ها بر اساس ورودی‌های کاربر را اضافه کنید
            System.out.println("پیشنهاد سریال‌ها بر اساس اطلاعات شما:");
            String urlString = BASE_URL + "discover/tv?api_key=" + API_KEY + "&with_genres=" + genre;
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}