package com.example.myapplicationt1;

import java.util.Date;

public class Completion_class {
    private String originalDay;
    private Date originalDate;
    private Date completionDate;
    private String originalClassId;

    public Completion_class() {}

    public Completion_class(String originalDay, Date originalDate, Date completionDate, String originalClassId) {
        this.originalDay = originalDay;
        this.originalDate = originalDate;
        this.completionDate = completionDate;
        this.originalClassId = originalClassId;
    }

    // Getters ו-Setters
    public String getOriginalDay() {
        return originalDay;
    }

    public void setOriginalDay(String originalDay) {
        this.originalDay = originalDay;
    }

    public Date getOriginalDate() {
        return originalDate;
    }

    public void setOriginalDate(Date originalDate) {
        this.originalDate = originalDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public String getOriginalClassId() {
        return originalClassId;
    }

    public void setOriginalClassId(String originalClassId) {
        this.originalClassId = originalClassId;
    }


}
