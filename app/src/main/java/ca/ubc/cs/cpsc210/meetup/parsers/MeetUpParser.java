package ca.ubc.cs.cpsc210.meetup.parsers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ca.ubc.cs.cpsc210.meetup.model.Course;
import ca.ubc.cs.cpsc210.meetup.model.CourseFactory;
import ca.ubc.cs.cpsc210.meetup.model.Schedule;
import ca.ubc.cs.cpsc210.meetup.model.Section;
import ca.ubc.cs.cpsc210.meetup.model.Student;

/**
 * Created by Ryan on 15-03-22.
 */
public class MeetUpParser {

    public Student parseStudent(String rawInput) {

        JSONTokener jtInput = new JSONTokener(rawInput);

        try {
            JSONObject input = new JSONObject(jtInput);
            String firstName = input.getString("FirstName");
            String lastName = input.getString("LastName");
            int id = input.getInt("Id");

            Student student = new Student(firstName, lastName, id);
            return student;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public SortedSet<Section> parseSections(String rawInput) {


        JSONTokener jtInput = new JSONTokener(rawInput);
        SortedSet<Section> allSections = new TreeSet<Section>();
        // Course Factory will allow us to make comparisons from within the for loop below.
        CourseFactory courseFactory = CourseFactory.getInstance();

        try {
            JSONObject input = new JSONObject(jtInput);
            JSONArray sections = input.getJSONArray("Sections");

            for (int i = 0; i < sections.length(); i++) {

                JSONObject jsSection = sections.getJSONObject(i);
                String courseName = jsSection.getString("CourseName");
                int courseNumber = jsSection.getInt("CourseNumber");
                String sectionNumber = jsSection.getString("SectionName");

                Course currentCourse = courseFactory.getCourse(courseName, courseNumber);
                Section currentSection = currentCourse.getSection(sectionNumber);
                allSections.add(currentSection);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return allSections;
    }
}
