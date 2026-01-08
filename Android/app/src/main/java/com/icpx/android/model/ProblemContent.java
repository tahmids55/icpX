package com.icpx.android.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class to hold parsed problem content
 */
public class ProblemContent {
    private String problemName;
    private String timeLimit;
    private String memoryLimit;
    private String problemStatement;
    private String inputFormat;
    private String outputFormat;
    private List<String> imageUrls;
    private List<TestCase> examples;
    private String notes;

    public ProblemContent() {
        this.imageUrls = new ArrayList<>();
        this.examples = new ArrayList<>();
    }

    public static class TestCase {
        private String input;
        private String output;

        public TestCase(String input, String output) {
            this.input = input;
            this.output = output;
        }

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
    }

    // Getters and Setters
    public String getProblemName() { return problemName; }
    public void setProblemName(String problemName) { this.problemName = problemName; }

    public String getTimeLimit() { return timeLimit; }
    public void setTimeLimit(String timeLimit) { this.timeLimit = timeLimit; }

    public String getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(String memoryLimit) { this.memoryLimit = memoryLimit; }

    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }

    public String getInputFormat() { return inputFormat; }
    public void setInputFormat(String inputFormat) { this.inputFormat = inputFormat; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void addImageUrl(String url) { this.imageUrls.add(url); }

    public List<TestCase> getExamples() { return examples; }
    public void setExamples(List<TestCase> examples) { this.examples = examples; }
    public void addExample(String input, String output) { 
        this.examples.add(new TestCase(input, output)); 
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
