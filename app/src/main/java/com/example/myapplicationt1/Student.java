package com.example.myapplicationt1;

import androidx.annotation.NonNull;

public class Student {
    private String fullName;
    private String organizationId;
    private String gender;
    private String dateOfBirth;
    private String classDay;
    private String grade;
    private String phoneNumber;
    private String joiningDate;
    private String address;
    private String parent1Name;
    private String parent2Name;
    private String parentPhoneNumber;
    private String photo;
    private String leavingDate;

    private String classId; // מזהה של ה-CLASS אליו הוא שייך

    public Student() {}

    public Student(String fullName, String organizationId, String gender, String dateOfBirth,
                   String classDay, String grade, String phoneNumber, String joiningDate,
                   String address, String parent1Name, String parent2Name,
                   String parentPhoneNumber, String photo, String leavingDate, String classId) {
        this.fullName = fullName;
        this.organizationId = organizationId;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.classDay = classDay;
        this.grade = grade;
        this.phoneNumber = phoneNumber;
        this.joiningDate = joiningDate;
        this.address = address;
        this.parent1Name = parent1Name;
        this.parent2Name = parent2Name;
        this.parentPhoneNumber = parentPhoneNumber;
        this.photo = photo;
        this.leavingDate = leavingDate;
        this.classId = classId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getClassDay() {
        return classDay;
    }

    public void setClassDay(String classDay) {
        this.classDay = classDay;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getParent1Name() {
        return parent1Name;
    }

    public void setParent1Name(String parent1Name) {
        this.parent1Name = parent1Name;
    }

    public String getParent2Name() {
        return parent2Name;
    }

    public void setParent2Name(String parent2Name) {
        this.parent2Name = parent2Name;
    }

    public String getParentPhoneNumber() {
        return parentPhoneNumber;
    }

    public void setParentPhoneNumber(String parentPhoneNumber) {
        this.parentPhoneNumber = parentPhoneNumber;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getLeavingDate() {
        return leavingDate;
    }

    public void setLeavingDate(String leavingDate) {
        this.leavingDate = leavingDate;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    // 🔹 toString
    @NonNull
    @Override
    public String toString() {
        return "Student{" +
                "fullName='" + fullName + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", gender='" + gender + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", classDay='" + classDay + '\'' +
                ", grade='" + grade + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", joiningDate='" + joiningDate + '\'' +
                ", address='" + address + '\'' +
                ", parent1Name='" + parent1Name + '\'' +
                ", parent2Name='" + parent2Name + '\'' +
                ", parentPhoneNumber='" + parentPhoneNumber + '\'' +
                ", photo='" + photo + '\'' +
                ", leavingDate='" + leavingDate + '\'' +
                ", classId='" + classId + '\'' +
                '}';
    }

}
