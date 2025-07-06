package com.example.myapplicationt1;

import java.util.HashMap;
import java.util.Map;

public class StudentStatus {

    private String studentName;
    private String status;     // "נוכח", "חיסור", "השלמה"
    private boolean isToran;
    private String notes;
    private String date;       // למשל: "13/05/2025"
    private String activeNumber;

    public StudentStatus() {
        // נדרש ע"י Firestore
    }

    public StudentStatus(String studentName, String status, boolean isToran, String notes, String date, String activeNumber) {
        this.studentName = studentName;
        this.status = status;
        this.isToran = isToran;
        this.notes = notes;
        this.date = date;
        this.activeNumber = activeNumber;
    }

    // Getters & Setters

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isToran() {
        return isToran;
    }

    public void setToran(boolean toran) {
        isToran = toran;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    // פונקציה להפוך את המידע ל־Map לצורך שמירה בפיירבייס
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("studentName", studentName);
        map.put("status", status);
        map.put("toran", isToran);
        map.put("notes", notes);
        map.put("date", date);
        map.put("activeNumber", activeNumber);
        return map;
    }
}
