import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import java.io.OutputStream;

public class LlamaAPIIntegration {
    private static final String LLAMA_API_URL = "http://localhost:11434/api/generate";  // آدرس API لاما
    private static final String LLAMA_API_KEY = "YOUR_LLAM_API_KEY";// کلید API لاما
    private static final String MODEL_NAME="llama3.2:1b";
    public static void main(String[] args) {
        // دریافت ورودی از کاربر برای جستجوی سریال
        String prompt = "Please provide a summary, actors, ratings, and reviews for the TV show 'Stranger Things'.";
        String jsonInputString = "{ \"model\": \"" + MODEL_NAME + "\", \"prompt\": \"" + prompt + "\" }";
        // ارسال پرامپت به Llama و دریافت پاسخ
        sendPromptToLlama(jsonInputString);
    }

    // ارسال پرامپت به Llama و دریافت پاسخ
    private static void sendPromptToLlama(String prompt) {
        try {
            // ایجاد URL برای API لاما
            URL url = new URL(LLAMA_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer ");
            connection.setDoOutput(true);

            // ایجاد JSON برای ارسال به Llama
            JSONObject json = new JSONObject();
            json.put("prompt", prompt);
            json.put("max_tokens", 200);  // حداکثر تعداد کلمات در پاسخ

            // ارسال درخواست
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // خواندن پاسخ از Llama
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // نمایش پاسخ از Llama
            System.out.println("پاسخ از Llama:");
            JSONObject jsonResponse = new JSONObject(response.toString());
            System.out.println(jsonResponse.getString("text"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
