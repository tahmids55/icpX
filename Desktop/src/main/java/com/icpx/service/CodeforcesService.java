package com.icpx.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.icpx.model.Contest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for interacting with Codeforces API
 */
public class CodeforcesService {

    private static final String CODEFORCES_API_URL = "https://codeforces.com/api";

    /**
     * Fetch list of contests from Codeforces API
     * @param gym if true, fetch gym contests; if false, fetch regular contests
     * @return List of contests
     */
    public List<Contest> fetchContests(boolean gym) throws IOException {
        String apiUrl = CODEFORCES_API_URL + "/contest.list?gym=" + gym;
        List<Contest> contests = new ArrayList<>();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    System.err.println("Codeforces API returned status: " + statusCode);
                    throw new IOException("Failed to fetch contests: status " + statusCode);
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String jsonString = EntityUtils.toString(entity);
                    JsonObject jsonResponse = JsonParser.parseString(jsonString).getAsJsonObject();
                    
                    if (!"OK".equals(jsonResponse.get("status").getAsString())) {
                        String errorMsg = jsonResponse.has("comment") ? jsonResponse.get("comment").getAsString() : "Unknown error";
                        throw new IOException("Codeforces API error: " + errorMsg);
                    }

                    JsonArray contestsArray = jsonResponse.getAsJsonArray("result");
                    
                    for (JsonElement element : contestsArray) {
                        JsonObject contestJson = element.getAsJsonObject();
                        Contest contest = new Contest();
                        
                        contest.setId(contestJson.get("id").getAsInt());
                        contest.setName(contestJson.get("name").getAsString());
                        contest.setType(contestJson.has("type") ? contestJson.get("type").getAsString() : "");
                        contest.setPhase(contestJson.get("phase").getAsString());
                        contest.setFrozen(contestJson.has("frozen") && contestJson.get("frozen").getAsBoolean());
                        contest.setDurationSeconds(contestJson.get("durationSeconds").getAsLong());
                        
                        if (contestJson.has("startTimeSeconds")) {
                            contest.setStartTimeSeconds(contestJson.get("startTimeSeconds").getAsLong());
                        }
                        
                        if (contestJson.has("relativeTimeSeconds")) {
                            contest.setRelativeTimeSeconds(contestJson.get("relativeTimeSeconds").getAsLong());
                        }
                        
                        contests.add(contest);
                    }
                }
            }
        }
        
        return contests;
    }


    /**
     * Check if a user has accepted a specific problem
     * @param userId Codeforces user ID (handle)
     * @param problemUrl URL of the problem
     * @return true if accepted, false otherwise
     */
    public boolean checkProblemAccepted(String userId, String problemUrl) throws IOException {
        ProblemInfo problemInfo = parseProblemUrl(problemUrl);
        if (problemInfo == null) {
            System.err.println("Could not parse problem URL: " + problemUrl);
            throw new IOException("Invalid problem URL format");
        }

        System.out.println("Checking problem: Contest=" + problemInfo.contestId + ", Index=" + problemInfo.index);
        System.out.println("For user handle: " + userId);

        // URL encode the handle to handle special characters
        String encodedHandle = java.net.URLEncoder.encode(userId.trim(), "UTF-8");
        
        // Fetch user submissions (500 is a safe limit for Codeforces API)
        String apiUrl = String.format("%s/user.status?handle=%s&from=1&count=500", CODEFORCES_API_URL, encodedHandle);
        System.out.println("API URL: " + apiUrl);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    // Try to get error details from response
                    HttpEntity errorEntity = response.getEntity();
                    String errorBody = errorEntity != null ? EntityUtils.toString(errorEntity) : "No error details";
                    System.err.println("Codeforces API returned status: " + statusCode);
                    System.err.println("Response body: " + errorBody);
                    throw new IOException("Codeforces API error (status " + statusCode + "): Check if handle '" + userId + "' is valid");
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String jsonString = EntityUtils.toString(entity);
                    JsonObject jsonResponse = JsonParser.parseString(jsonString).getAsJsonObject();
                    
                    if (!"OK".equals(jsonResponse.get("status").getAsString())) {
                        String errorMsg = jsonResponse.has("comment") ? jsonResponse.get("comment").getAsString() : "Unknown error";
                        System.err.println("Codeforces API error: " + errorMsg);
                        throw new IOException("Codeforces API error: " + errorMsg);
                    }

                    JsonArray submissions = jsonResponse.getAsJsonArray("result");
                    System.out.println("Found " + submissions.size() + " submissions for user");
                    
                    boolean foundProblem = false;
                    for (JsonElement element : submissions) {
                        JsonObject submission = element.getAsJsonObject();
                        JsonObject problem = submission.getAsJsonObject("problem");
                        
                        // Check if this submission is for the target problem
                        if (problem.has("contestId") && problem.has("index")) {
                            String subContestId = String.valueOf(problem.get("contestId").getAsInt());
                            String subIndex = problem.get("index").getAsString();
                            
                            if (subContestId.equals(problemInfo.contestId) && subIndex.equals(problemInfo.index)) {
                                foundProblem = true;
                                String verdict = submission.has("verdict") ? submission.get("verdict").getAsString() : "UNKNOWN";
                                System.out.println("Found submission for problem with verdict: " + verdict);
                                
                                // Check verdict
                                if ("OK".equals(verdict)) {
                                    System.out.println("Problem is ACCEPTED!");
                                    return true;
                                }
                            }
                        }
                    }
                    
                    if (!foundProblem) {
                        System.out.println("No submissions found for this problem");
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Parse Codeforces problem URL to extract contest ID and problem index
     */
    public ProblemInfo parseProblemUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // Pattern for contest URL: .../contest/{contestId}/problem/{index}
        Pattern contestPattern = Pattern.compile("contest/(\\d+)/problem/([A-Z0-9]+)");
        Matcher contestMatcher = contestPattern.matcher(url);
        if (contestMatcher.find()) {
            return new ProblemInfo(contestMatcher.group(1), contestMatcher.group(2));
        }

        // Pattern for problemset URL: .../problemset/problem/{contestId}/{index}
        Pattern problemsetPattern = Pattern.compile("problemset/problem/(\\d+)/([A-Z0-9]+)");
        Matcher problemsetMatcher = problemsetPattern.matcher(url);
        if (problemsetMatcher.find()) {
            return new ProblemInfo(problemsetMatcher.group(1), problemsetMatcher.group(2));
        }
        
        // Pattern for gym URL: .../gym/{contestId}/problem/{index}
        Pattern gymPattern = Pattern.compile("gym/(\\d+)/problem/([A-Z0-9]+)");
        Matcher gymMatcher = gymPattern.matcher(url);
        if (gymMatcher.find()) {
            return new ProblemInfo(gymMatcher.group(1), gymMatcher.group(2));
        }

        return null;
    }

    /**
     * Fetch all problems for a specific contest
     */
    public List<ContestProblem> fetchContestProblems(String contestId) throws IOException {
        String apiUrl = String.format("%s/contest.standings?contestId=%s&from=1&count=1", CODEFORCES_API_URL, contestId);
        List<ContestProblem> problems = new ArrayList<>();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    System.err.println("Codeforces API returned status: " + statusCode);
                    return problems;
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String jsonString = EntityUtils.toString(entity);
                    JsonObject jsonResponse = JsonParser.parseString(jsonString).getAsJsonObject();
                    
                    if (!"OK".equals(jsonResponse.get("status").getAsString())) {
                        System.err.println("Codeforces API error: " + jsonResponse.get("comment").getAsString());
                        return problems;
                    }

                    JsonObject result = jsonResponse.getAsJsonObject("result");
                    JsonArray problemsArray = result.getAsJsonArray("problems");
                    
                    for (JsonElement element : problemsArray) {
                        JsonObject problemJson = element.getAsJsonObject();
                        if (problemJson.has("contestId") && problemJson.has("index") && problemJson.has("name")) {
                            String cId = String.valueOf(problemJson.get("contestId").getAsInt());
                            String index = problemJson.get("index").getAsString();
                            String name = problemJson.get("name").getAsString();
                            problems.add(new ContestProblem(cId, index, name));
                        }
                    }
                }
            }
        }
        return problems;
    }

    /**
     * Fetch problem details (name and rating) from a problem URL
     */
    public ProblemDetails fetchProblemDetails(String problemUrl) throws IOException {
        ProblemInfo problemInfo = parseProblemUrl(problemUrl);
        if (problemInfo == null) {
            throw new IOException("Invalid problem URL format");
        }

        String apiUrl = String.format("%s/problemset.problems", CODEFORCES_API_URL);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("Codeforces API returned status: " + statusCode);
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String jsonString = EntityUtils.toString(entity);
                    JsonObject jsonResponse = JsonParser.parseString(jsonString).getAsJsonObject();
                    
                    if (!"OK".equals(jsonResponse.get("status").getAsString())) {
                        throw new IOException("Codeforces API error: " + jsonResponse.get("comment").getAsString());
                    }

                    JsonObject result = jsonResponse.getAsJsonObject("result");
                    JsonArray problemsArray = result.getAsJsonArray("problems");
                    
                    for (JsonElement element : problemsArray) {
                        JsonObject problemJson = element.getAsJsonObject();
                        if (problemJson.has("contestId") && problemJson.has("index")) {
                            String cId = String.valueOf(problemJson.get("contestId").getAsInt());
                            String index = problemJson.get("index").getAsString();
                            
                            if (cId.equals(problemInfo.contestId) && index.equals(problemInfo.index)) {
                                String name = problemJson.has("name") ? problemJson.get("name").getAsString() : "Unknown Problem";
                                Integer rating = problemJson.has("rating") ? problemJson.get("rating").getAsInt() : null;
                                return new ProblemDetails(name, rating);
                            }
                        }
                    }
                }
            }
        }
        
        throw new IOException("Problem not found in Codeforces problemset");
    }

    /**
     * Inner class to hold problem information
     */
    public static class ProblemInfo {
        public final String contestId;
        public final String index;

        public ProblemInfo(String contestId, String index) {
            this.contestId = contestId;
            this.index = index;
        }
    }

    /**
     * Inner class to hold contest problem details
     */
    public static class ContestProblem {
        public final String contestId;
        public final String index;
        public final String name;

        public ContestProblem(String contestId, String index, String name) {
            this.contestId = contestId;
            this.index = index;
            this.name = name;
        }
        
        public String getUrl() {
            return String.format("https://codeforces.com/contest/%s/problem/%s", contestId, index);
        }
    }

    /**
     * Inner class to hold problem details (name and rating)
     */
    public static class ProblemDetails {
        public final String name;
        public final Integer rating;

        public ProblemDetails(String name, Integer rating) {
            this.name = name;
            this.rating = rating;
        }
    }

    /**
     * Fetch rating for a specific Codeforces problem
     * @param contestId Contest ID
     * @param problemIndex Problem index (A, B, C, etc.)
     * @return Problem rating or null if not found
     */
    public Integer fetchProblemRating(String contestId, String problemIndex) {
        try {
            String apiUrl = CODEFORCES_API_URL + "/problemset.problems";
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(apiUrl);
                
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (response.getStatusLine().getStatusCode() != 200) {
                        return null;
                    }
                    
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String jsonString = EntityUtils.toString(entity);
                        JsonObject jsonResponse = JsonParser.parseString(jsonString).getAsJsonObject();
                        
                        if ("OK".equals(jsonResponse.get("status").getAsString())) {
                            JsonArray problems = jsonResponse.getAsJsonObject("result").getAsJsonArray("problems");
                            
                            for (JsonElement element : problems) {
                                JsonObject problem = element.getAsJsonObject();
                                if (problem.has("contestId") && problem.has("index")) {
                                    int pContestId = problem.get("contestId").getAsInt();
                                    String pIndex = problem.get("index").getAsString();
                                    
                                    if (String.valueOf(pContestId).equals(contestId) && pIndex.equals(problemIndex)) {
                                        if (problem.has("rating")) {
                                            return problem.get("rating").getAsInt();
                                        }
                                        return null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching problem rating: " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetch rating from Codeforces problem URL
     * @param problemUrl Full URL like https://codeforces.com/contest/1234/problem/A
     * @return Problem rating or null if not found/not a valid CF URL
     */
    public Integer fetchProblemRatingFromUrl(String problemUrl) {
        if (problemUrl == null || !problemUrl.contains("codeforces.com")) {
            return null;
        }
        
        // Parse URL to extract contest ID and problem index
        Pattern pattern = Pattern.compile("codeforces\\.com/(?:contest|problemset/problem)/(\\d+)/(?:problem/)?(\\w+)");
        Matcher matcher = pattern.matcher(problemUrl);
        
        if (matcher.find()) {
            String contestId = matcher.group(1);
            String problemIndex = matcher.group(2);
            return fetchProblemRating(contestId, problemIndex);
        }
        
        return null;
    }

    /**
     * Fetch user's submission activity for heatmap
     * Returns a map of date -> number of accepted submissions
     */
    public java.util.Map<java.time.LocalDate, Integer> fetchSubmissionActivity(String handle) throws IOException {
        java.util.Map<java.time.LocalDate, Integer> activityMap = new java.util.HashMap<>();
        
        try {
            String encodedHandle = java.net.URLEncoder.encode(handle.trim(), "UTF-8");
            String apiUrl = String.format("%s/user.status?handle=%s&from=1&count=10000", CODEFORCES_API_URL, encodedHandle);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(apiUrl);
                
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                        throw new IOException("Failed to fetch submissions: status " + statusCode);
                    }

                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String jsonString = EntityUtils.toString(entity);
                        JsonObject jsonResponse = JsonParser.parseString(jsonString).getAsJsonObject();
                        
                        if (!"OK".equals(jsonResponse.get("status").getAsString())) {
                            throw new IOException("API error: " + jsonResponse.get("comment").getAsString());
                        }

                        JsonArray submissions = jsonResponse.getAsJsonArray("result");
                        
                        // Track unique problems solved per day (not duplicate submissions)
                        java.util.Map<java.time.LocalDate, java.util.Set<String>> problemsPerDay = new java.util.HashMap<>();
                        
                        for (JsonElement element : submissions) {
                            JsonObject submission = element.getAsJsonObject();
                            
                            // Only count accepted submissions
                            if (!submission.has("verdict") || !"OK".equals(submission.get("verdict").getAsString())) {
                                continue;
                            }
                            
                            // Get submission timestamp
                            long timestamp = submission.get("creationTimeSeconds").getAsLong();
                            java.time.LocalDate date = java.time.Instant.ofEpochSecond(timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                            
                            // Get problem identifier
                            JsonObject problem = submission.getAsJsonObject("problem");
                            String problemId = "";
                            if (problem.has("contestId")) {
                                problemId = problem.get("contestId").getAsInt() + "-";
                            }
                            if (problem.has("index")) {
                                problemId += problem.get("index").getAsString();
                            }
                            
                            // Add to set of problems solved that day
                            problemsPerDay.computeIfAbsent(date, k -> new java.util.HashSet<>()).add(problemId);
                        }
                        
                        // Convert to count map
                        for (java.util.Map.Entry<java.time.LocalDate, java.util.Set<String>> entry : problemsPerDay.entrySet()) {
                            activityMap.put(entry.getKey(), entry.getValue().size());
                        }
                    }
                }
            }
        } catch (java.net.SocketException e) {
            // Re-throw socket exceptions to be handled specially
            throw e;
        } catch (java.net.UnknownHostException e) {
            throw new IOException("Cannot reach Codeforces. Check your internet connection.");
        } catch (Exception e) {
            throw new IOException("Error fetching activity: " + e.getMessage(), e);
        }
        
        return activityMap;
    }
}
