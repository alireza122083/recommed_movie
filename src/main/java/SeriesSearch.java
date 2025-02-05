import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Scanner;

public class SeriesSearch {
    private static final String API_KEY = "a0ca34a93b0593a56abb6b6c74749234";  // API Key خود را اینجا قرار دهید
    private static final String BASE_URL = "https://api.themoviedb.org/3";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("جستجو بر اساس: \n1. نام سریال \n2. نام بازیگر \n3. نام کارگردان \n4. ژانر");
        System.out.print("انتخاب کنید: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume the newline character

        String query = "";
        switch (choice) {
            case 1:
                System.out.print("نام سریال را وارد کنید: ");
                query = scanner.nextLine();
                searchSeriesByName(query);
                break;
            case 2:
                System.out.print("نام بازیگر را وارد کنید: ");
                query = scanner.nextLine();
                searchSeriesByActor(query);
                break;
            case 3:
                System.out.print("نام کارگردان را وارد کنید: ");
                query = scanner.nextLine();
                searchSeriesByDirector(query);
                break;
            case 4:
                System.out.print("ژانر مورد نظر را وارد کنید: ");
                query = scanner.nextLine();
                searchSeriesByGenre(query);
                break;
            default:
                System.out.println("انتخاب نامعتبر.");
        }
        scanner.close();
    }

    // جستجوی سریال بر اساس نام
    private static void searchSeriesByName(String name) {
        try {
            String urlString = BASE_URL + "/search/tv?api_key=" + API_KEY + "&query=" + name;
            String response = sendGetRequest(urlString);
            parseAndDisplayResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // جستجوی سریال‌ها بر اساس نام بازیگر
    private static void searchSeriesByActor(String actor) {
        try {
            String urlString = BASE_URL + "/search/person?api_key=" + API_KEY + "&query=" + actor;
            String response = sendGetRequest(urlString);
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray results = jsonResponse.getJSONArray("results");

            if (results.length() > 0) {
                JSONObject person = results.getJSONObject(0);
                int personId = person.getInt("id");
                String actorName = person.getString("name");

                // دریافت سریال‌های بازیگر
                urlString = BASE_URL + "/person/" + personId + "/tv_credits?api_key=" + API_KEY;
                response = sendGetRequest(urlString);
                parseAndDisplayResponse(response);
            } else {
                System.out.println("بازیگری با این نام پیدا نشد.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // جستجوی سریال‌ها بر اساس کارگردان
    private static void searchSeriesByDirector(String director) {
        try {
            String urlString = BASE_URL + "/search/person?api_key=" + API_KEY + "&query=" + director;
            String response = sendGetRequest(urlString);
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray results = jsonResponse.getJSONArray("results");

            if (results.length() > 0) {
                JSONObject person = results.getJSONObject(0);
                int personId = person.getInt("id");
                String directorName = person.getString("name");

                // دریافت سریال‌های کارگردان
                urlString = BASE_URL + "/person/" + personId + "/tv_credits?api_key=" + API_KEY;
                response = sendGetRequest(urlString);
                parseAndDisplayResponse(response);
            } else {
                System.out.println("کارگردانی با این نام پیدا نشد.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // جستجوی سریال‌ها بر اساس ژانر
    private static void searchSeriesByGenre(String genre) {
        try {
            String urlString = BASE_URL + "/discover/tv?api_key=" + API_KEY + "&with_genres=" + genre;
            String response = sendGetRequest(urlString);
            parseAndDisplayResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ارسال درخواست GET به API
    private static String sendGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    // تجزیه و نمایش اطلاعات پاسخ
    private static void parseAndDisplayResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray results = jsonResponse.getJSONArray("results");

            if (results.length() == 0) {
                System.out.println("نتیجه‌ای یافت نشد.");
                return;
            }

            for (int i = 0; i < results.length(); i++) {
                JSONObject series = results.getJSONObject(i);
                String title = series.getString("name");
                String overview = series.optString("overview", "خلاصه داستان موجود نیست.");
                double rating = series.optDouble("vote_average", 0.0);
                String firstAirDate = series.optString("first_air_date", "تاریخ انتشار موجود نیست.");

                System.out.println("\nسریال: " + title);
                System.out.println("خلاصه داستان: " + overview);
                System.out.println("امتیاز: " + rating);
                System.out.println("تاریخ انتشار: " + firstAirDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}