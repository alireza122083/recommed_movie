import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TsvToCsvCast {
    public static void main(String[] args) {
        String namesFile = "name.basics.tsv";
        String rolesFile = "title.principals.tsv";
        String outputFile = "cast.csv";

        Map<String, String> nameMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(namesFile))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }

                String[] columns = line.split("\t", -1);
                if (columns.length < 2) continue;

                nameMap.put(columns[0], columns[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(rolesFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }

                String[] columns = line.split("\t", -1);
                if (columns.length < 4) continue;

                String movieId = columns[0];
                String personId = columns[2];
                String role = columns[3];

                if (role.equalsIgnoreCase("actor") || role.equalsIgnoreCase("actress") || role.equalsIgnoreCase("director")) {
                    String personName = nameMap.getOrDefault(personId, "Unknown");
                    bw.write(movieId + "," + personName + "," + role);
                    bw.newLine();
                }
            }

            System.out.println("✅ cast.csv ساخته شد!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}