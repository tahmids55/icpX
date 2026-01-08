package com.icpx.android.utils;

import android.util.Log;

import com.icpx.android.model.ProblemContent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * Utility class to parse Codeforces problem HTML
 */
public class ProblemParser {
    
    private static final String TAG = "ProblemParser";

    /**
     * Parse HTML content and extract problem details
     */
    public static ProblemContent parseProblemHtml(String html, String baseUrl) {
        ProblemContent content = new ProblemContent();
        
        try {
            Document doc = Jsoup.parse(html, baseUrl);
            
            // Extract problem name from .title
            Element titleElement = doc.selectFirst(".problem-statement .title");
            if (titleElement != null) {
                content.setProblemName(cleanText(titleElement.text()));
            }
            
            // Extract time limit
            Element timeLimitElement = doc.selectFirst(".time-limit");
            if (timeLimitElement != null) {
                String timeText = timeLimitElement.text();
                timeText = timeText.replace("time limit per test", "").trim();
                content.setTimeLimit(cleanText(timeText));
            }
            
            // Extract memory limit
            Element memoryLimitElement = doc.selectFirst(".memory-limit");
            if (memoryLimitElement != null) {
                String memText = memoryLimitElement.text();
                memText = memText.replace("memory limit per test", "").trim();
                content.setMemoryLimit(cleanText(memText));
            }
            
            // Extract problem statement - get the div after header
            Elements statementParagraphs = doc.select(".problem-statement > div > p");
            if (statementParagraphs.isEmpty()) {
                // Try alternative selector
                Element problemDiv = doc.selectFirst(".problem-statement");
                if (problemDiv != null) {
                    // Remove unwanted sections
                    problemDiv.select(".header").remove();
                    problemDiv.select(".input-specification").remove();
                    problemDiv.select(".output-specification").remove();
                    problemDiv.select(".sample-tests").remove();
                    problemDiv.select(".note").remove();
                    
                    String statement = getFormattedText(problemDiv);
                    content.setProblemStatement(cleanText(statement));
                }
            } else {
                StringBuilder statement = new StringBuilder();
                for (Element p : statementParagraphs) {
                    statement.append(p.text()).append("\n\n");
                }
                content.setProblemStatement(cleanText(statement.toString()));
            }
            
            // Extract input specification
            Element inputSpec = doc.selectFirst(".input-specification");
            if (inputSpec != null) {
                inputSpec.select(".section-title").remove();
                String inputHtml = inputSpec.html();
                inputHtml = inputHtml.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n");
                Document inputDoc = Jsoup.parse(inputHtml);
                String inputText = inputDoc.body().wholeText();
                content.setInputFormat(cleanText(inputText));
            }
            
            // Extract output specification
            Element outputSpec = doc.selectFirst(".output-specification");
            if (outputSpec != null) {
                outputSpec.select(".section-title").remove();
                String outputHtml = outputSpec.html();
                outputHtml = outputHtml.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n");
                Document outputDoc = Jsoup.parse(outputHtml);
                String outputText = outputDoc.body().wholeText();
                content.setOutputFormat(cleanText(outputText));
            }
            
            // Extract sample tests
            Element sampleTests = doc.selectFirst(".sample-tests");
            if (sampleTests != null) {
                Elements inputDivs = sampleTests.select(".input");
                Elements outputDivs = sampleTests.select(".output");
                
                int count = Math.min(inputDivs.size(), outputDivs.size());
                for (int i = 0; i < count; i++) {
                    Element inputPre = inputDivs.get(i).selectFirst("pre");
                    Element outputPre = outputDivs.get(i).selectFirst("pre");
                    
                    if (inputPre != null && outputPre != null) {
                        // Get text preserving all whitespace and newlines
                        String input = extractPreFormattedText(inputPre);
                        String output = extractPreFormattedText(outputPre);
                        
                        content.addExample(input, output);
                    }
                }
            }
            
            // Extract note
            Element noteElement = doc.selectFirst(".note");
            if (noteElement != null) {
                noteElement.select(".section-title").remove();
                String noteText = getFormattedText(noteElement);
                content.setNotes(cleanText(noteText));
            }
            
            // Extract images
            Elements images = doc.select("img");
            for (Element img : images) {
                String src = img.absUrl("src");
                if (src != null && !src.isEmpty() && 
                    (src.endsWith(".png") || src.endsWith(".jpg") || 
                     src.endsWith(".jpeg") || src.endsWith(".gif"))) {
                    content.addImageUrl(src);
                    Log.d(TAG, "Found image: " + src);
                }
            }
            
            Log.d(TAG, "✅ Parsed problem: " + content.getProblemName());
            Log.d(TAG, "Examples: " + content.getExamples().size());
            Log.d(TAG, "Images: " + content.getImageUrls().size());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing problem HTML: " + e.getMessage(), e);
        }
        
        return content;
    }
    
    /**
     * Get formatted text preserving paragraphs and line breaks
     */
    private static String getFormattedText(Element element) {
        if (element == null) return "";
        
        StringBuilder sb = new StringBuilder();
        
        // Process each child node
        for (Element child : element.children()) {
            String tag = child.tagName();
            
            if (tag.equals("p") || tag.equals("div")) {
                sb.append(child.text()).append("\n\n");
            } else if (tag.equals("br")) {
                sb.append("\n");
            } else if (tag.equals("ul") || tag.equals("ol")) {
                for (Element li : child.select("li")) {
                    sb.append("• ").append(li.text()).append("\n");
                }
                sb.append("\n");
            } else {
                sb.append(child.text()).append(" ");
            }
        }
        
        // If no children, get direct text
        if (sb.length() == 0) {
            sb.append(element.text());
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Clean text - remove extra spaces and special characters that cause issues
     */
    private static String cleanText(String text) {
        if (text == null) return "";
        
        // Handle LaTeX formulas
        text = handleLatexFormulas(text);
        
        return text
            .replaceAll("\\s+", " ")  // Multiple spaces to single space
            .replaceAll("\\u00A0", " ")  // Non-breaking space to regular space
            .replaceAll("\\$+", "")  // Remove remaining dollar signs
            .trim();
    }
    
    /**
     * Handle LaTeX formulas - convert common patterns to readable text
     */
    private static String handleLatexFormulas(String text) {
        if (text == null) return "";
        
        // Replace common LaTeX patterns
        text = text.replaceAll("\\$\\$([^\\$]+)\\$\\$", "[$1]");  // Display formulas
        text = text.replaceAll("\\$([^\\$]+)\\$", "$1");  // Inline formulas
        
        // Common LaTeX commands
        text = text.replaceAll("\\\\leq", "<=");
        text = text.replaceAll("\\\\geq", ">=");
        text = text.replaceAll("\\\\neq", "!=");
        text = text.replaceAll("\\\\times", "×");
        text = text.replaceAll("\\\\cdot", "·");
        text = text.replaceAll("\\\\ldots", "...");
        text = text.replaceAll("\\\\sum", "Σ");
        text = text.replaceAll("\\\\prod", "Π");
        text = text.replaceAll("\\\\sqrt\\{([^}]+)\\}", "√($1)");
        text = text.replaceAll("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}", "($1/$2)");
        text = text.replaceAll("_\\{([^}]+)\\}", "_$1");
        text = text.replaceAll("\\^\\{([^}]+)\\}", "^$1");
        
        // Remove other LaTeX commands
        text = text.replaceAll("\\\\[a-zA-Z]+\\{([^}]+)\\}", "$1");
        text = text.replaceAll("\\\\[a-zA-Z]+", "");
        
        return text;
    }
    
    /**
     * Extract preformatted text from <pre> tag preserving exact whitespace and newlines
     */
    private static String extractPreFormattedText(Element preElement) {
        if (preElement == null) return "";
        
        StringBuilder result = new StringBuilder();
        extractTextRecursive(preElement, result);
        
        // Trim only trailing whitespace at the very end
        String text = result.toString();
        return text.replaceAll("\\s+$", "");
    }
    
    /**
     * Recursively extract text from element preserving all whitespace
     */
    private static void extractTextRecursive(org.jsoup.nodes.Node node, StringBuilder sb) {
        if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            // Get the text exactly as it appears, preserving spaces and newlines
            sb.append(textNode.getWholeText());
        } else if (node instanceof Element) {
            Element element = (Element) node;
            String tagName = element.tagName();
            
            // Handle <br> tags as newlines
            if (tagName.equals("br")) {
                sb.append("\n");
            } else {
                // Process all child nodes
                for (org.jsoup.nodes.Node child : element.childNodes()) {
                    extractTextRecursive(child, sb);
                }
            }
        }
    }
}
