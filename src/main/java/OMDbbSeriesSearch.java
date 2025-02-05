import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class OMDbbSeriesSearch {
    private static final String API_KEY = "7485379f"; // کلید API خود را جایگزین کنید

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n📺 **جستجوی سریال - منوی کاربری**");
            System.out.println("1️⃣ جستجو بر اساس نام سریال");
            System.out.println("2️⃣ جستجو بر اساس کارگردان");
            System.out.println("3️⃣ جستجو بر اساس بازیگر");
            System.out.println("4️⃣ جستجو بر اساس ژانر");
            System.out.println("5️⃣ جستجو بر اساس امتیاز IMDb");
            System.out.println("6️⃣ جستجوی ترکیبی (چند معیار همزمان)");
            System.out.println("0️⃣ خروج");

            System.out.print("🔹 گزینه موردنظر را انتخاب کنید: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // خواندن خط جدید برای جلوگیری از مشکلات ورودی

            String title = "", director = "", actor = "", genre = "", rating = "";

            switch (choice) {
                case 1:
                    System.out.print("🔍 نام سریال: ");
                    title = scanner.nextLine().trim();
                    break;
                case 2:
                    System.out.print("🎬 نام کارگردان: ");
                    director = scanner.nextLine().trim();
                    break;
                case 3:
                    System.out.print("🎭 نام بازیگر: ");
                    actor = scanner.nextLine().trim();
                    break;
                case 4:
                    System.out.print("📺 ژانر: ");
                    genre = scanner.nextLine().trim();
                    break;
                case 5:
                    System.out.print("⭐ حداقل امتیاز IMDb: ");
                    rating = scanner.nextLine().trim();
                    break;
                case 6:
                    System.out.print("🔍 نام سریال (اختیاری): ");
                    title = scanner.nextLine().trim();
                    System.out.print("🎬 نام کارگردان (اختیاری): ");
                    director = scanner.nextLine().trim();
                    System.out.print("🎭 نام بازیگر (اختیاری): ");
                    actor = scanner.nextLine().trim();
                    System.out.print("📺 ژانر (اختیاری): ");
                    genre = scanner.nextLine().trim();
                    System.out.print("⭐ حداقل امتیاز IMDb (اختیاری): ");
                    rating = scanner.nextLine().trim();
                    break;
                case 0:
                    System.out.println("👋 خروج از برنامه...");
                    scanner.close();
                    return;
                default:
                    System.out.println("❌ گزینه نامعتبر! لطفاً دوباره تلاش کنید.");
                    continue;
            }

            searchSeries(title, director, actor, genre, rating);
        }
    }

    public static void searchSeries(String title, String director, String actor, String genre, String rating) {
        try {
            // اگر هیچ ورودی‌ای داده نشده باشد، پیام هشدار نمایش داده شود
            if (title.isEmpty() && director.isEmpty() && actor.isEmpty() && genre.isEmpty() && rating.isEmpty()) {
                System.out.println("❌ لطفاً حداقل یک فیلتر جستجو را وارد کنید.");
                return;
            }

            // اگر عنوان سریال داده نشده، یک جستجوی عمومی انجام می‌شود
            String searchQuery = title.isEmpty() ? "Breaking Bad" : title; // به عنوان نمونه اگر عنوان خالی باشد، یک سریال محبوب جستجو می‌شود

            // ایجاد URL برای جستجو
            String urlString = String.format("https://www.omdbapi.com/?apikey=%s&type=series&s=%s", API_KEY, searchQuery.replace(" ", "%20"));
            URL url = new URL(urlString);

            // اتصال به API
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // دریافت پاسخ API
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // تبدیل پاسخ به JSON
            JSONObject jsonResponse = new JSONObject(response.toString());

            // بررسی وجود نتایج
            if (jsonResponse.has("Search")) {
                JSONArray results = jsonResponse.getJSONArray("Search");

                System.out.println("\n📺 **نتایج جستجو:**");

                for (int i = 0; i < results.length(); i++) {
                    JSONObject series = results.getJSONObject(i);
                    String seriesTitle = series.getString("Title");
                    String imdbID = series.getString("imdbID");

                    // دریافت اطلاعات جزئی هر سریال
                    JSONObject seriesDetails = getSeriesDetails(imdbID);
                    if (seriesDetails != null) {
                        String seriesDirector = seriesDetails.optString("Director", "نامشخص");
                        String seriesActors = seriesDetails.optString("Actors", "نامشخص");
                        String seriesGenre = seriesDetails.optString("Genre", "نامشخص");
                        String imdbRating = seriesDetails.optString("imdbRating", "N/A");

                        // اعمال فیلترهای کاربر
                        if ((director.isEmpty() || seriesDirector.contains(director)) &&
                                (actor.isEmpty() || seriesActors.contains(actor)) &&
                                (genre.isEmpty() || seriesGenre.contains(genre)) &&
                                (rating.isEmpty() || (!imdbRating.equals("N/A") && Double.parseDouble(imdbRating) >= Double.parseDouble(rating)))) {

                            System.out.println("------------------------------------------------");
                            System.out.println("🎬 نام سریال: " + seriesTitle);
                            System.out.println("🎭 بازیگران: " + seriesActors);
                            System.out.println("🎬 کارگردان: " + seriesDirector);
                            System.out.println("📺 ژانر: " + seriesGenre);
                            System.out.println("⭐ امتیاز IMDb: " + imdbRating);
                            System.out.println("🔗 لینک IMDb: https://www.imdb.com/title/" + imdbID);
                        }
                    }
                }
            } else {
                System.out.println("❌ نتیجه‌ای یافت نشد!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // دریافت جزئیات سریال بر اساس IMDB ID
    private static JSONObject getSeriesDetails(String imdbID) {
        try {
            String urlString = String.format("https://www.omdbapi.com/?apikey=%s&i=%s", API_KEY, imdbID);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return new JSONObject(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}