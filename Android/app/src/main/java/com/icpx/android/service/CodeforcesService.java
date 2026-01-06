package com.icpx.android.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for interacting with Codeforces API
 */
public class CodeforcesService {

    private static final String API_BASE_URL = "https://codeforces.com/api/";
    private static final String PROBLEMSET_URL = "https://codeforces.com/problemset/problem/";

    public static class ProblemDetails {
        public String name;
        public int rating;
        public String contestId;
        public String index;

        public ProblemDetails(String name, int rating, String contestId, String index) {
            this.name = name;
            this.rating = rating;
            this.contestId = contestId;
            this.index = index;
        }
    }

    /**
     * Fetch problem details from a Codeforces problem URL
     */
    public ProblemDetails fetchProblemDetails(String problemUrl) {
        try {
            // Extract contest ID and problem index from URL
            // Pattern updated to handle indices like A, B1, C2, etc.
            Pattern pattern = Pattern.compile("codeforces\\.com/(?:problemset/problem|contest)/(\\d+)/problem/([A-Z]\\d*)");
            Matcher matcher = pattern.matcher(problemUrl);

            if (!matcher.find()) {
                // Try alternative pattern without /problem/
                pattern = Pattern.compile("codeforces\\.com/(?:problemset/problem|contest)/(\\d+)/([A-Z]\\d*)");
                matcher = pattern.matcher(problemUrl);
                if (!matcher.find()) {
                    System.err.println("Failed to match URL pattern: " + problemUrl);
                    return null;
                }
            }

            String contestId = matcher.group(1);
            String problemIndex = matcher.group(2);

            System.out.println("Fetching problem: Contest=" + contestId + ", Index=" + problemIndex);

            // Fetch problem details from API
            String apiUrl = API_BASE_URL + "problemset.problems";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("API returned error code: " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());
            if (!jsonResponse.getString("status").equals("OK")) {
                System.err.println("API status not OK");
                return null;
            }

            JSONArray problems = jsonResponse.getJSONObject("result")
                    .getJSONArray("problems");

            // Find matching problem
            for (int i = 0; i < problems.length(); i++) {
                JSONObject problem = problems.getJSONObject(i);
                String cId = String.valueOf(problem.getInt("contestId"));
                String idx = problem.getString("index");

                if (cId.equals(contestId) && idx.equalsIgnoreCase(problemIndex)) {
                    String name = problem.getString("name");
                    int rating = problem.optInt("rating", 0);
                    
                    System.out.println("Found problem: " + name + " (Rating: " + rating + ")");
                    return new ProblemDetails(name, rating, contestId, problemIndex);
                }
            }

            System.err.println("Problem not found in API response");

        } catch (Exception e) {
            System.err.println("Exception in fetchProblemDetails: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Fetch user info from Codeforces
     */
    public JSONObject fetchUserInfo(String handle) {
        try {
            String apiUrl = API_BASE_URL + "user.info?handles=" + handle;
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            if (jsonResponse.getString("status").equals("OK")) {
                return jsonResponse.getJSONArray("result").getJSONObject(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Check if a user has solved a specific problem
     * @param handle Codeforces handle
     * @param contestId Contest ID
     * @param problemIndex Problem index (A, B, C, etc.)
     * @return true if solved, false otherwise
     */
    public boolean isProblemSolved(String handle, String contestId, String problemIndex) {
        try {
            String apiUrl = API_BASE_URL + "user.status?handle=" + handle + "&from=1&count=10000";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("API returned error code: " + responseCode);
                return false;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            if (!jsonResponse.getString("status").equals("OK")) {
                return false;
            }

            JSONArray submissions = jsonResponse.getJSONArray("result");

            // Check if any submission is accepted for this problem
            for (int i = 0; i < submissions.length(); i++) {
                JSONObject submission = submissions.getJSONObject(i);
                JSONObject problem = submission.getJSONObject("problem");
                
                String subContestId = String.valueOf(problem.getInt("contestId"));
                String subIndex = problem.getString("index");
                String verdict = submission.optString("verdict", "");

                if (subContestId.equals(contestId) && 
                    subIndex.equalsIgnoreCase(problemIndex) && 
                    verdict.equals("OK")) {
                    System.out.println("Problem solved: " + contestId + problemIndex);
                    return true;
                }
            }

        } catch (Exception e) {
            System.err.println("Exception in isProblemSolved: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}
