package com.example.myapplicationt1;

import java.util.List;

public class Class {
    private String classDate;
    private String classDay;
    private List<String> arr_students; // רשימת מזהי חניכים (Student IDs)
    private List<String> arr_guides;   // רשימת מזהי מדריכים (Guide IDs)
    private List<String> arr_cleaning; // חניכים שהיו תורני ניקיון
    private String attendanceStatus;

    public Class() {} // נדרש עבור Firebase

    public Class(String classDate, String classDay, List<String> arr_students,
                 List<String> arr_guides, List<String> arr_cleaning, String attendanceStatus) {
        this.classDate = classDate;
        this.classDay = classDay;
        this.arr_students = arr_students;
        this.arr_guides = arr_guides;
        this.arr_cleaning = arr_cleaning;
        this.attendanceStatus = attendanceStatus;
    }

    // 🔹 Getters ו־Setters

    public String getClassDate() {
        return classDate;
    }

    public void setClassDate(String classDate) {
        this.classDate = classDate;
    }

    public String getClassDay() {
        return classDay;
    }

    public void setClassDay(String classDay) {
        this.classDay = classDay;
    }

    public List<String> getArr_students() {
        return arr_students;
    }

    public void setArr_students(List<String> arr_students) {
        this.arr_students = arr_students;
    }

    public List<String> getArr_guides() {
        return arr_guides;
    }

    public void setArr_guides(List<String> arr_guides) {
        this.arr_guides = arr_guides;
    }

    public List<String> getArr_cleaning() {
        return arr_cleaning;
    }

    public void setArr_cleaning(List<String> arr_cleaning) {
        this.arr_cleaning = arr_cleaning;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    // 🔹 toString
    @Override
    public String toString() {
        return "Class{" +
                "classDate='" + classDate + '\'' +
                ", classDay='" + classDay + '\'' +
                ", arr_students=" + arr_students +
                ", arr_guides=" + arr_guides +
                ", arr_cleaning=" + arr_cleaning +
                ", attendanceStatus='" + attendanceStatus + '\'' +
                '}';
    }


}
