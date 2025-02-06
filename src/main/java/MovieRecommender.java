import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.json.*;

public class MovieRecommender {
    private static final String DB_URL = "jdbc:sqlite:movies.db";
    private static final String API_KEY = "a0ca34a93b0593a56abb6b6c74749234";
    private static final String BASE_URL = "https://api.themoviedb.org/3/";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("🎬 Do you want a movie or a series?");
        String choice = scanner.nextLine().trim().toLowerCase();

        if (choice.equals("movie")) {
            handleMovieSearch(scanner);
        } else if (choice.equals("series")) {
            handleTVShowSearch(scanner);
        } else {
            System.out.println("⛔ Invalid option!");
        }
    }

    private static void handleMovieSearch(Scanner scanner) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            while (true) {
                System.out.println("\n🔍 Search for top movies");
                System.out.println("1️⃣ By actor");
                System.out.println("2️⃣ By director");
                System.out.println("3️⃣ By genre");
                System.out.println("4️⃣ By movie title");
                System.out.println("5️⃣ Movies introduction test based on interst");
                System.out.println("0️⃣ Exit");

                System.out.print("👉 Choose an option: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                if (choice == 0) break;

                if (choice == 5) {
                    userProfileTestMovie(scanner);
                    continue;
                }

                System.out.print("🔎 Search: ");
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
                        System.out.println("⛔ Invalid option!");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void handleTVShowSearch(Scanner scanner) {
        while (true) {
            System.out.println("\n🔍 Search for top series");
            System.out.println("1️⃣ By actor");
            System.out.println("2️⃣ By director");
            System.out.println("3️⃣ By genre");
            System.out.println("4️⃣ By series title");
            System.out.println("5️⃣ Series introduction test based on interst");
            System.out.println("0️⃣ Exit");

            System.out.print("👉 Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) break;

            if (choice == 5) {

                userProfileTestSeries(scanner);
                continue;
            }

            System.out.print("🔎 Search: ");
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
                    System.out.println("⛔ Invalid option!");
            }
        }
    }

    private static void searchMoviesByActor(Connection conn, String actorName) throws SQLException {
        String sql = "SELECT movies.title, ratings.rating FROM movies " +
                "JOIN \"cast\" ON movies.id = \"cast\".movie_id " +
                "JOIN ratings ON movies.id = ratings.movie_id " +
                "WHERE LOWER(\"cast\".person_name) LIKE LOWER(?) AND ratings.rating < 9.5 " +
                "ORDER BY ratings.rating DESC LIMIT 10;";
        executeQuery(conn, sql, "%" + actorName + "%");
    }

    private static void searchMoviesByDirector(Connection conn, String directorName) throws SQLException {
        String sql = "SELECT movies.title, ratings.rating FROM movies " +
                "JOIN \"cast\" ON movies.id = \"cast\".movie_id " +
                "JOIN ratings ON movies.id = ratings.movie_id " +
                "WHERE LOWER(\"cast\".person_name) LIKE LOWER(?) AND \"cast\".role = 'director' AND ratings.rating < 9.5 " +
                "ORDER BY ratings.rating DESC LIMIT 10;";
        executeQuery(conn, sql, "%" + directorName + "%");
    }

    private static void searchMoviesByGenre(Connection conn, String genre) throws SQLException {
        String sql = "SELECT movies.title, ratings.rating FROM movies " +
                "JOIN ratings ON movies.id = ratings.movie_id " +
                "WHERE LOWER(movies.genre) LIKE LOWER(?) AND ratings.rating < 9.5 " +
                "ORDER BY ratings.rating DESC LIMIT 10;";
        executeQuery(conn, sql, "%" + genre + "%");
    }

    private static void searchMoviesByTitle(Connection conn, String title) throws SQLException {
        String sql = "SELECT movies.title, ratings.rating FROM movies " +
                "JOIN ratings ON movies.id = ratings.movie_id " +
                "WHERE LOWER(movies.title) LIKE LOWER(?) AND ratings.rating < 9.5 " +
                "ORDER BY ratings.rating DESC LIMIT 10;";
        executeQuery(conn, sql, "%" + title + "%");
    }

    private static void executeQuery(Connection conn, String sql, String param) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, param);
            ResultSet rs = pstmt.executeQuery();
            List<String> movies = new ArrayList<>();
            boolean found = false;
            while (rs.next()) {
                found = true;
                String movieTitle = rs.getString("title");
                if (movies.contains(movieTitle)) {
                    continue;
                } else {
                    movies.add(movieTitle);
                }
                fetchMovieDetailsFromOMDb(movieTitle);
            }

            if (!found) System.out.println("❌ No results found.");
        }
    }

    private static void fetchMovieDetailsFromOMDb(String movieTitle) {
        String movieName = movieTitle.replace(" ", "%20");
        try {

            String searchUrl = "https://api.themoviedb.org/3/search/movie?api_key=" + API_KEY + "&query=" + movieName;
            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder searchResponse = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                searchResponse.append(inputLine);
            }
            in.close();
            conn.disconnect();


            JSONObject searchJson = new JSONObject(searchResponse.toString());
            JSONArray results = searchJson.getJSONArray("results");
            if (results.length() > 0) {
                JSONObject movie = results.getJSONObject(0);
                int movieId = movie.getInt("id");


                String detailsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + API_KEY;
                url = new URL(detailsUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder detailsResponse = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    detailsResponse.append(inputLine);
                }
                in.close();
                conn.disconnect();

                JSONObject detailsJson = new JSONObject(detailsResponse.toString());

                System.out.println("Title: " + detailsJson.getString("title"));
                System.out.println("Release Date: " + detailsJson.getString("release_date"));
                System.out.println("Rating: " + detailsJson.getDouble("vote_average"));
                System.out.println("Overview: " + detailsJson.getString("overview"));
                System.out.println("--------------------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void searchTVShowsByActorTMDb(String actorName) {
        String encodedActorName = actorName.replace(" ", "%20");
        String searchUrl = BASE_URL + "search/person?api_key=" + API_KEY + "&query=" + encodedActorName;
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest searchRequest = HttpRequest.newBuilder(URI.create(searchUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());

            if (searchResponse.statusCode() == 200) {
                JSONObject searchJson = new JSONObject(searchResponse.body());
                JSONArray results = searchJson.getJSONArray("results");

                if (results.length() > 0) {
                    JSONObject actor = results.getJSONObject(0);
                    int actorId = actor.getInt("id");
                    String tvCreditsUrl = BASE_URL + "person/" + actorId + "/tv_credits?api_key=" + API_KEY;
                    HttpRequest tvRequest = HttpRequest.newBuilder(URI.create(tvCreditsUrl))
                            .header("Accept", "application/json")
                            .build();
                    HttpResponse<String> tvResponse = client.send(tvRequest, HttpResponse.BodyHandlers.ofString());

                    if (tvResponse.statusCode() == 200) {
                        JSONObject tvJson = new JSONObject(tvResponse.body());
                        JSONArray tvCredits = tvJson.getJSONArray("cast");

                        // Convert JSONArray to List for sorting
                        List<JSONObject> seriesList = new ArrayList<>();
                        for (int i = 0; i < tvCredits.length(); i++) {
                            JSONObject tvShow = tvCredits.getJSONObject(i);
                            seriesList.add(tvShow);
                        }
                        seriesList.sort(new Comparator<JSONObject>() {
                            @Override
                            public int compare(JSONObject o1, JSONObject o2) {
                                double v1 = o1.optDouble("vote_average", 0);
                                double v2 = o2.optDouble("vote_average", 0);
                                return Double.compare(v2, v1);
                            }
                        });
                        int count = Math.min(3, seriesList.size());
                        System.out.println("Top TV shows of " + actorName + " based on rating:");
                        for (int i = 0; i < count; i++) {
                            JSONObject series = seriesList.get(i);
                            int tvId = series.getInt("id");


                            String detailsUrl = "https://api.themoviedb.org/3/tv/" + tvId + "?api_key=" + API_KEY;
                            HttpRequest detailsRequest = HttpRequest.newBuilder(URI.create(detailsUrl))
                                    .header("Accept", "application/json")
                                    .build();
                            HttpResponse<String> detailsResponse = client.send(detailsRequest, HttpResponse.BodyHandlers.ofString());

                            if (detailsResponse.statusCode() == 200) {
                                JSONObject detailsJson = new JSONObject(detailsResponse.body());
                                String seriesName = detailsJson.optString("name", "Unknown");
                                String overview = detailsJson.optString("overview", "No overview available");
                                double voteAverage = detailsJson.optDouble("vote_average", 0);


                                String creditsUrl = "https://api.themoviedb.org/3/tv/" + tvId + "/credits?api_key=" + API_KEY;
                                HttpRequest creditsRequest = HttpRequest.newBuilder(URI.create(creditsUrl))
                                        .header("Accept", "application/json")
                                        .build();
                                HttpResponse<String> creditsResponse = client.send(creditsRequest, HttpResponse.BodyHandlers.ofString());

                                String castNames = "";
                                String directorName = "";
                                if (creditsResponse.statusCode() == 200) {
                                    JSONObject creditsJson = new JSONObject(creditsResponse.body());
                                    JSONArray castArray = creditsJson.getJSONArray("cast");
                                    // Extract top 3 cast members
                                    List<String> castList = new ArrayList<>();
                                    for (int j = 0; j < Math.min(3, castArray.length()); j++) {
                                        JSONObject castMember = castArray.getJSONObject(j);
                                        castList.add(castMember.optString("name", "Unknown"));
                                    }
                                    castNames = String.join(", ", castList);
                                }

                                System.out.println("-----");
                                System.out.println("Series Name: " + seriesName);
                                System.out.println("Rating: " + voteAverage);
                                System.out.println("Overview: " + overview);
                                System.out.println("Cast: " + (castNames.isEmpty() ? "Unknown" : castNames));
                            } else {
                                System.out.println("Error retrieving series details with ID " + tvId);
                            }
                        }
                    } else {
                        System.out.println("Error retrieving TV shows: " + tvResponse.statusCode());
                    }
                } else {
                    System.out.println("Actor not found.");
                }
            } else {
                System.out.println("Error searching for actor: " + searchResponse.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void searchTVShowsByDirectorTMDb(String directorName) {
        String encodedDirectorName = directorName.replace(" ", "%20");
        String searchUrl = BASE_URL + "search/person?api_key=" + API_KEY + "&query=" + encodedDirectorName;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest searchRequest = HttpRequest.newBuilder(URI.create(searchUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());

            if (searchResponse.statusCode() == 200) {
                JSONObject searchJson = new JSONObject(searchResponse.body());
                JSONArray results = searchJson.getJSONArray("results");

                if (results.length() > 0) {
                    JSONObject person = results.getJSONObject(0);
                    int personId = person.getInt("id");

                    String tvCreditsUrl = "https://api.themoviedb.org/3/person/" + personId + "/tv_credits?api_key=" + API_KEY;
                    HttpRequest tvCreditsRequest = HttpRequest.newBuilder(URI.create(tvCreditsUrl))
                            .header("Accept", "application/json")
                            .build();
                    HttpResponse<String> tvCreditsResponse = client.send(tvCreditsRequest, HttpResponse.BodyHandlers.ofString());

                    if (tvCreditsResponse.statusCode() == 200) {
                        JSONObject creditsJson = new JSONObject(tvCreditsResponse.body());
                        JSONArray crewArray = creditsJson.getJSONArray("crew");

                        List<JSONObject> directorSeries = new ArrayList<>();
                        for (int i = 0; i < crewArray.length(); i++) {
                            JSONObject crewMember = crewArray.getJSONObject(i);
                            if ("Director".equalsIgnoreCase(crewMember.optString("job"))) {
                                directorSeries.add(crewMember);
                            }
                        }

                        if (directorSeries.isEmpty()) {
                            System.out.println("No TV series directed found.");
                            return;
                        }

                        directorSeries.sort(new Comparator<JSONObject>() {
                            @Override
                            public int compare(JSONObject o1, JSONObject o2) {
                                double v1 = o1.optDouble("vote_average", 0);
                                double v2 = o2.optDouble("vote_average", 0);
                                return Double.compare(v2, v1);
                            }
                        });


                        int count = Math.min(5, directorSeries.size());
                        System.out.println("Top TV shows directed by " + directorName + " based on rating:");
                        for (int i = 0; i < count; i++) {
                            JSONObject series = directorSeries.get(i);
                            int tvId = series.getInt("id");

                            // Get series details (/tv/{tv_id})
                            String detailsUrl = "https://api.themoviedb.org/3/tv/" + tvId + "?api_key=" + API_KEY;
                            HttpRequest detailsRequest = HttpRequest.newBuilder(URI.create(detailsUrl))
                                    .header("Accept", "application/json")
                                    .build();
                            HttpResponse<String> detailsResponse = client.send(detailsRequest, HttpResponse.BodyHandlers.ofString());

                            if (detailsResponse.statusCode() == 200) {
                                JSONObject detailsJson = new JSONObject(detailsResponse.body());
                                String seriesName = detailsJson.optString("name", "Unknown");
                                String overview = detailsJson.optString("overview", "No overview available");
                                double voteAverage = detailsJson.optDouble("vote_average", 0);


                                String creditsUrl = "https://api.themoviedb.org/3/tv/" + tvId + "/credits?api_key=" + API_KEY;
                                HttpRequest creditsRequest = HttpRequest.newBuilder(URI.create(creditsUrl))
                                        .header("Accept", "application/json")
                                        .build();
                                HttpResponse<String> castResponse = client.send(creditsRequest, HttpResponse.BodyHandlers.ofString());

                                String castNames = "";
                                if (castResponse.statusCode() == 200) {
                                    JSONObject castJson = new JSONObject(castResponse.body());
                                    JSONArray castArray = castJson.getJSONArray("cast");

                                    List<String> castList = new ArrayList<>();
                                    for (int j = 0; j < Math.min(3, castArray.length()); j++) {
                                        JSONObject castMember = castArray.getJSONObject(j);
                                        castList.add(castMember.optString("name", "Unknown"));
                                    }
                                    castNames = String.join(", ", castList);
                                }

                                System.out.println("-----");
                                System.out.println("Series Name: " + seriesName);
                                System.out.println("Rating: " + voteAverage);
                                System.out.println("Overview: " + overview);
                                System.out.println("Cast: " + (castNames.isEmpty() ? "Unknown" : castNames));
                            } else {
                                System.out.println("Error retrieving series details with ID " + tvId);
                            }
                        }
                    } else {
                        System.out.println("Error retrieving tv_credits: " + tvCreditsResponse.statusCode());
                    }
                } else {
                    System.out.println("Director not found.");
                }
            } else {
                System.out.println("Error searching for director: " + searchResponse.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void searchTVShowsByGenreTMDb(String genreInput) {
        String genreListUrl = BASE_URL + "genre/tv/list?api_key=" + API_KEY + "&language=en-US";
        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest genreRequest = HttpRequest.newBuilder(URI.create(genreListUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> genreResponse = client.send(genreRequest, HttpResponse.BodyHandlers.ofString());

            if (genreResponse.statusCode() == 200) {
                JSONObject genreJson = new JSONObject(genreResponse.body());
                JSONArray genres = genreJson.getJSONArray("genres");
                String genreId = null;

                for (int i = 0; i < genres.length(); i++) {
                    JSONObject genreObj = genres.getJSONObject(i);
                    String genreName = genreObj.optString("name", "");
                    if (genreName.equalsIgnoreCase(genreInput)) {
                        genreId = String.valueOf(genreObj.getInt("id"));
                        break;
                    }
                }

                if (genreId == null) {
                    System.out.println("The entered genre was not found.");
                    return;
                }


                String discoverUrl = BASE_URL + "discover/tv?api_key=" + API_KEY +
                        "&with_genres=" + genreId + "&sort_by=vote_average.desc";
                HttpRequest discoverRequest = HttpRequest.newBuilder(URI.create(discoverUrl))
                        .header("Accept", "application/json")
                        .build();
                HttpResponse<String> discoverResponse = client.send(discoverRequest, HttpResponse.BodyHandlers.ofString());

                if (discoverResponse.statusCode() == 200) {
                    JSONObject discoverJson = new JSONObject(discoverResponse.body());
                    JSONArray results = discoverJson.getJSONArray("results");

                    if (results.length() > 0) {
                        System.out.println("Top TV shows in the " + genreInput + " genre based on rating:");

                        for (int i = 0; i < Math.min(3, results.length()); i++) {
                            JSONObject series = results.getJSONObject(i);
                            String seriesName = series.optString("name", "Unknown");
                            String overview = series.optString("overview", "No overview available");
                            double voteAverage = series.optDouble("vote_average", 0);

                            System.out.println("-----");
                            System.out.println("Series Name: " + seriesName);
                            System.out.println("Rating: " + voteAverage);
                            System.out.println("Overview: " + overview);
                        }
                    } else {
                        System.out.println("No TV shows found for the " + genreInput + " genre.");
                    }
                } else {
                    System.out.println("Error fetching TV shows: " + discoverResponse.statusCode());
                }
            } else {
                System.out.println("Error fetching genre list: " + genreResponse.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void searchTVShowsByTitleTMDb(String seriesName) {

        String encodedSeriesName = seriesName.replace(" ", "%20");

        String searchUrl = "https://api.themoviedb.org/3/search/tv?api_key=" + API_KEY + "&query=" + encodedSeriesName;
        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest searchRequest = HttpRequest.newBuilder(URI.create(searchUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());

            if (searchResponse.statusCode() == 200) {
                JSONObject searchJson = new JSONObject(searchResponse.body());
                JSONArray results = searchJson.getJSONArray("results");

                if (results.length() > 0) {

                    JSONObject series = results.getJSONObject(0);
                    int tvId = series.getInt("id");

                    String detailsUrl = "https://api.themoviedb.org/3/tv/" + tvId + "?api_key=" + API_KEY;
                    HttpRequest detailsRequest = HttpRequest.newBuilder(URI.create(detailsUrl))
                            .header("Accept", "application/json")
                            .build();
                    HttpResponse<String> detailsResponse = client.send(detailsRequest, HttpResponse.BodyHandlers.ofString());

                    if (detailsResponse.statusCode() == 200) {
                        JSONObject detailsJson = new JSONObject(detailsResponse.body());
                        String name = detailsJson.optString("name", "Unknown");
                        String overview = detailsJson.optString("overview", "No overview available");
                        double voteAverage = detailsJson.optDouble("vote_average", 0);

                        System.out.println("----- TV Show Details -----");
                        System.out.println("Name: " + name);
                        System.out.println("Rating: " + voteAverage);
                        System.out.println("Overview: " + overview);
                        System.out.println("--------------------------");
                    } else {
                        System.out.println("Error fetching TV show details: " + detailsResponse.statusCode());
                    }
                } else {
                    System.out.println("TV show not found.");
                }
            } else {
                System.out.println("Error searching for TV show: " + searchResponse.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void userProfileTestMovie(Scanner scanner) {
        System.out.print("Do you prefer long (L) or short (S) movies? ");
        String lengthPref = scanner.nextLine().trim();

        System.out.print("What is the last movie you liked? ");
        String lastLikedMovie = scanner.nextLine().trim();

        System.out.print("What is your favorite movie genre? (e.g., Action) ");
        String genreInput = scanner.nextLine().trim();

        System.out.print("Who is your favorite actor for movies? ");
        String actorInput = scanner.nextLine().trim();
        recommendMovies(lengthPref, genreInput, lastLikedMovie, actorInput);
    }

    private static void userProfileTestSeries(Scanner scanner) {
        System.out.println("📊 Answer a few questions to get the best recommendations!");
        System.out.print("Do you prefer long (B) or short (K) TV series? ");
        String lengthPref = scanner.nextLine().trim();

        System.out.print("What is the last TV series you liked? ");
        String lastLikedSeries = scanner.nextLine().trim();

        System.out.print("What is your favorite genre? (e.g., Drama) ");
        String genreInput = scanner.nextLine().trim();

        System.out.print("Who is your favorite actor? ");
        String actorInput = scanner.nextLine().trim();
        scanner.nextLine();
        recommendTVShows(lengthPref, genreInput, lastLikedSeries, actorInput);
    }

    private static void recommendMovies(String lengthPref, String genreInput, String lastLikedMovie, String actorInput) {
        System.out.println("\nEntered details:");
        System.out.println("Movie length preference: " + (lengthPref.equalsIgnoreCase("L") ? "Long" : "Short"));
        System.out.println("Last liked movie: " + lastLikedMovie);
        System.out.println("Genre: " + genreInput);
        System.out.println("Actor: " + actorInput);
        System.out.println("Fetching movie recommendations...\n");

        HttpClient client = HttpClient.newHttpClient();

        try {
            // 1. Get the list of movie genres
            String genreListUrl = "https://api.themoviedb.org/3/genre/movie/list?api_key=" + API_KEY + "&language=en-US";
            HttpRequest genreRequest = HttpRequest.newBuilder(URI.create(genreListUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> genreResponse = client.send(genreRequest, HttpResponse.BodyHandlers.ofString());
            String genreId = "";
            if (genreResponse.statusCode() == 200) {
                JSONObject genreJson = new JSONObject(genreResponse.body());
                JSONArray genres = genreJson.getJSONArray("genres");
                // Convert the entered genre name to its corresponding ID
                for (int i = 0; i < genres.length(); i++) {
                    JSONObject genreObj = genres.getJSONObject(i);
                    String name = genreObj.optString("name", "");
                    if (name.equalsIgnoreCase(genreInput)) {
                        genreId = String.valueOf(genreObj.getInt("id"));
                        break;
                    }
                }
            } else {
                System.out.println("Error fetching genre list: " + genreResponse.statusCode());
                return;
            }
            if (genreId.isEmpty()) {
                System.out.println("Entered genre not found.");
                return;
            }

            // 2. Search for actor to get their ID
            String encodedActor = actorInput.replace(" ", "%20");
            String actorSearchUrl = "https://api.themoviedb.org/3/search/person?api_key=" + API_KEY + "&query=" + encodedActor;
            HttpRequest actorRequest = HttpRequest.newBuilder(URI.create(actorSearchUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> actorResponse = client.send(actorRequest, HttpResponse.BodyHandlers.ofString());
            String actorId = "";
            if (actorResponse.statusCode() == 200) {
                JSONObject actorJson = new JSONObject(actorResponse.body());
                JSONArray actorResults = actorJson.getJSONArray("results");
                if (actorResults.length() > 0) {
                    actorId = String.valueOf(actorResults.getJSONObject(0).getInt("id"));
                } else {
                    System.out.println("Entered actor not found.");
                    return;
                }
            } else {
                System.out.println("Error searching actor: " + actorResponse.statusCode());
                return;
            }

            // 3. Set up the movie length filters
            String runtimeParam = "";
            if (lengthPref.equalsIgnoreCase("L")) {
                runtimeParam = "&with_runtime.gte=120";
            } else if (lengthPref.equalsIgnoreCase("S")) {
                runtimeParam = "&with_runtime.lte=90";
            }

            // 4. Construct the final URL for Discover movies
            String discoverUrl = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY +
                    "&with_genres=" + genreId +
                    "&with_cast=" + actorId +
                    runtimeParam +
                    "&sort_by=vote_average.desc";
            HttpRequest discoverRequest = HttpRequest.newBuilder(URI.create(discoverUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> discoverResponse = client.send(discoverRequest, HttpResponse.BodyHandlers.ofString());

            if (discoverResponse.statusCode() == 200) {
                JSONObject discoverJson = new JSONObject(discoverResponse.body());
                JSONArray results = discoverJson.getJSONArray("results");
                if (results.length() > 0) {
                    System.out.println("Recommended movies based on your input:");
                    // Display 3 recommended movies
                    for (int i = 0; i < Math.min(3, results.length()); i++) {
                        JSONObject movie = results.getJSONObject(i);
                        String title = movie.optString("title", "Unknown");
                        String overview = movie.optString("overview", "No overview available");
                        double voteAverage = movie.optDouble("vote_average", 0);
                        int movieId = movie.optInt("id", -1);

                        System.out.println("-----");
                        System.out.println("Title: " + title);
                        System.out.println("Rating: " + voteAverage);
                        System.out.println("Overview: " + overview);
                        if (movieId != -1) {
                            String creditsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + API_KEY;
                            HttpRequest creditsRequest = HttpRequest.newBuilder(URI.create(creditsUrl))
                                    .header("Accept", "application/json")
                                    .build();
                            HttpResponse<String> creditsResponse = client.send(creditsRequest, HttpResponse.BodyHandlers.ofString());
                            if (creditsResponse.statusCode() == 200) {
                                JSONObject creditsJson = new JSONObject(creditsResponse.body());
                                JSONArray castArray = creditsJson.getJSONArray("cast");
                                System.out.println("Cast:");
                                for (int j = 0; j < Math.min(5, castArray.length()); j++) {
                                    JSONObject castMember = castArray.getJSONObject(j);
                                    String actorName = castMember.optString("name", "Unknown");
                                    String character = castMember.optString("character", "Unknown");
                                    System.out.println(actorName + " as " + character);
                                }
                                System.out.println("-------------------------");
                            } else {
                                System.out.println("Error fetching cast details: " + creditsResponse.statusCode());
                            }
                        }
                    }
                } else {
                    System.out.println("No movies found based on your input.");
                }
            } else {
                System.out.println("Error fetching movies: " + discoverResponse.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recommendTVShows(String lengthPref, String genreInput, String lastLikedSeries, String actorInput) {
        System.out.println("\nYou entered the following details:");
        System.out.println("TV show length preference: " + (lengthPref.equalsIgnoreCase("B") ? "Long" : "Short"));
        System.out.println("Last liked series: " + lastLikedSeries);
        System.out.println("Genre: " + genreInput);
        System.out.println("Actor: " + actorInput);
        System.out.println("Fetching TV show recommendations...\n");
        HttpClient client = HttpClient.newHttpClient();

        try {
            String genreListUrl = "https://api.themoviedb.org/3/genre/tv/list?api_key=" + API_KEY + "&language=en-US";
            HttpRequest genreRequest = HttpRequest.newBuilder(URI.create(genreListUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> genreResponse = client.send(genreRequest, HttpResponse.BodyHandlers.ofString());
            String genreId = "";
            if (genreResponse.statusCode() == 200) {
                JSONObject genreJson = new JSONObject(genreResponse.body());
                JSONArray genres = genreJson.getJSONArray("genres");
                for (int i = 0; i < genres.length(); i++) {
                    JSONObject genreObj = genres.getJSONObject(i);
                    String name = genreObj.optString("name", "");
                    if (name.equalsIgnoreCase(genreInput)) {
                        genreId = String.valueOf(genreObj.getInt("id"));
                        break;
                    }
                }
            } else {
                System.out.println("Error fetching genre list: " + genreResponse.statusCode());
                return;
            }
            if (genreId.isEmpty()) {
                System.out.println("Entered genre not found.");
                return;
            }
            String encodedActor = actorInput.replace(" ", "%20");
            String actorSearchUrl = "https://api.themoviedb.org/3/search/person?api_key=" + API_KEY + "&query=" + encodedActor;
            HttpRequest actorRequest = HttpRequest.newBuilder(URI.create(actorSearchUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> actorResponse = client.send(actorRequest, HttpResponse.BodyHandlers.ofString());
            String actorId = "";
            if (actorResponse.statusCode() == 200) {
                JSONObject actorJson = new JSONObject(actorResponse.body());
                JSONArray actorResults = actorJson.getJSONArray("results");
                if (actorResults.length() > 0) {
                    actorId = String.valueOf(actorResults.getJSONObject(0).getInt("id"));
                } else {
                    System.out.println("Entered actor not found.");
                    return;
                }
            } else {
                System.out.println("Error searching actor: " + actorResponse.statusCode());
                return;
            }

            String runtimeParam = "";
            if (lengthPref.equalsIgnoreCase("B")) {
                runtimeParam = "&with_runtime.gte=50";
            } else if (lengthPref.equalsIgnoreCase("K")) {
                runtimeParam = "&with_runtime.lte=30";
            }

            String discoverUrl = "https://api.themoviedb.org/3/discover/tv?api_key=" + API_KEY +
                    "&with_genres=" + genreId +
                    "&with_cast=" + actorId +
                    runtimeParam +
                    "&sort_by=popularity.desc";

            HttpRequest discoverRequest = HttpRequest.newBuilder(URI.create(discoverUrl))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> discoverResponse = client.send(discoverRequest, HttpResponse.BodyHandlers.ofString());

            if (discoverResponse.statusCode() == 200) {
                JSONObject discoverJson = new JSONObject(discoverResponse.body());
                JSONArray results = discoverJson.getJSONArray("results");
                if (results.length() > 0) {
                    System.out.println("Recommended TV shows for you:");
                    // Display 3 recommended TV shows (if available)
                    for (int i = 0; i < Math.min(3, results.length()); i++) {
                        JSONObject series = results.getJSONObject(i);
                        String name = series.optString("name", "Unknown");
                        String overview = series.optString("overview", "No overview available");
                        double voteAverage = series.optDouble("vote_average", 0);

                        System.out.println("-----");
                        System.out.println("TV show name: " + name);
                        System.out.println("Rating: " + voteAverage);
                        System.out.println("Overview: " + overview);
                        System.out.println("----------------------");
                    }
                } else {
                    System.out.println("No TV shows found based on your input.");
                }
            } else {
                System.out.println("Error fetching TV shows: " + discoverResponse.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




