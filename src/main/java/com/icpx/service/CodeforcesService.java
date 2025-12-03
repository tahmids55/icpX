package com.icpx.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
     * Check if a user has accepted a specific problem
     * @param userId Codeforces user ID (handle)
     * @param problemUrl URL of the problem
     * @return true if accepted, false otherwise
     */
    public boolean checkProblemAccepted(String userId, String problemUrl) throws IOException {
        ProblemInfo problemInfo = parseProblemUrl(problemUrl);
        if (problemInfo == null) {
            System.err.println("Could not parse problem URL: " + problemUrl);
            return false;
        }

        // Fetch user submissions (last 500 to be comprehensive)
        String apiUrl = String.format("%s/user.status?handle=%s&from=1&count=500", CODEFORCES_API_URL, userId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    System.err.println("Codeforces API returned status: " + statusCode);
                    return false;
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String jsonString = EntityUtils.toString(entity);
                    JsonObject jsonResponse = JsonParser.parseString(jsonString).getAsJsonObject();
                    
                    if (!"OK".equals(jsonResponse.get("status").getAsString())) {
                        System.err.println("Codeforces API error: " + jsonResponse.get("comment").getAsString());
                        return false;
                    }

                    JsonArray submissions = jsonResponse.getAsJsonArray("result");
                    for (JsonElement element : submissions) {
                        JsonObject submission = element.getAsJsonObject();
                        JsonObject problem = submission.getAsJsonObject("problem");
                        
                        // Check if this submission is for the target problem
                        if (problem.has("contestId") && problem.has("index")) {
                            String subContestId = String.valueOf(problem.get("contestId").getAsInt());
                            String subIndex = problem.get("index").getAsString();
                            
                            if (subContestId.equals(problemInfo.contestId) && subIndex.equals(problemInfo.index)) {
                                // Check verdict
                                if (submission.has("verdict") && "OK".equals(submission.get("verdict").getAsString())) {
                                    return true;
                                }
                            }
                        }
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
}
