package com.example.myapplicationt1;

import java.util.Date;

public class CompletionRequest {
    private String studentName;
    private String submittedBy;
    private String regularDay;
    private Date completionDate;
    private Date missingDate;
    private boolean requiresManagerApproval;
    private boolean approved;

    public CompletionRequest() {}

    // Getters
    public String getStudentName() { return studentName; }
    public String getSubmittedBy() { return submittedBy; }
    public String getRegularDay() { return regularDay; }
    public Date getCompletionDate() { return completionDate; }
    public Date getMissingDate() { return missingDate; }
    public boolean isRequiresManagerApproval() { return requiresManagerApproval; }
    public boolean isApproved() { return approved; }

    // Setters
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public void setRegularDay(String regularDay) { this.regularDay = regularDay; }
    public void setCompletionDate(Date completionDate) { this.completionDate = completionDate; }
    public void setMissingDate(Date missingDate) { this.missingDate = missingDate; }
    public void setRequiresManagerApproval(boolean requiresManagerApproval) { this.requiresManagerApproval = requiresManagerApproval; }
    public void setApproved(boolean approved) { this.approved = approved; }
}
