package ca.ubc.cs.cpsc210.meetup.model;

/**
 * Created by Ryan on 15-04-02.
 */

/**
 * Class to represent the cumulative break times of a student.
 */
public class BreakBlock {

    String startTime;
    String endTime;

    public BreakBlock(String startTime, String endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BreakBlock)) return false;

        BreakBlock that = (BreakBlock) o;

        if (!endTime.equals(that.endTime)) return false;
        if (!startTime.equals(that.startTime)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startTime.hashCode();
        result = 31 * result + endTime.hashCode();
        return result;
    }
}
