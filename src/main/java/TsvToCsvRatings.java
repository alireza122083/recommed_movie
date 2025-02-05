import java.io.*;

public class TsvToCsvRatings {
    public static void main(String[] args) {
        String inputFile = "title.ratings.tsv";
        String outputFile = "ratings.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }

                String[] columns = line.split("\t", -1);
                if (columns.length < 3) continue;

                bw.write(columns[0] + "," + columns[1] + "," + columns[2]);
                bw.newLine();
            }

            System.out.println("✅ ratings.csv ساخته شد!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}