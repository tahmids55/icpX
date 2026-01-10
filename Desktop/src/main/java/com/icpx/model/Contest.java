package com.icpx.model;

/**
 * Contest model representing a Codeforces contest
 */
public class Contest {
    private int id;
    private String name;
    private String type;
    private String phase;
    private boolean frozen;
    private long durationSeconds;
    private long startTimeSeconds;
    private long relativeTimeSeconds;

    public Contest() {
    }

    public Contest(int id, String name, String type, String phase, 
                   long durationSeconds, long startTimeSeconds) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.phase = phase;
        this.durationSeconds = durationSeconds;
        this.startTimeSeconds = startTimeSeconds;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public long getStartTimeSeconds() {
        return startTimeSeconds;
    }

    public void setStartTimeSeconds(long startTimeSeconds) {
        this.startTimeSeconds = startTimeSeconds;
    }

    public long getRelativeTimeSeconds() {
        return relativeTimeSeconds;
    }

    public void setRelativeTimeSeconds(long relativeTimeSeconds) {
        this.relativeTimeSeconds = relativeTimeSeconds;
    }

    public boolean isUpcoming() {
        return "BEFORE".equals(phase);
    }

    public boolean isRunning() {
        return "CODING".equals(phase);
    }

    public boolean isFinished() {
        return "FINISHED".equals(phase);
    }

    public String getContestUrl() {
        return "https://codeforces.com/contest/" + id;
    }

    @Override
    public String toString() {
        return "Contest{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phase='" + phase + '\'' +
                '}';
    }
}
