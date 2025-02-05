import java.io.*;

public class TsvToCsvMovies {
    public static void main(String[] args) {
        String inputFile = "title.basics.tsv";
        String outputFile = "movies.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }

                String[] columns = line.split("\t", -1);
                if (columns.length < 9) continue;

                String id = columns[0];
                String title = columns[2];
                String year = columns[5].isEmpty() ? "0" : columns[5];
                String genre = columns[8];

                bw.write(id + "," + title + "," + year + "," + genre);
                bw.newLine();
            }

            System.out.println("✅ movies.csv ساخته شد!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}