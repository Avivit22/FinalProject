package com.example.myapplicationt1;

import androidx.annotation.NonNull;

public class Student {
    private String fullName;
    private String activeNumber;
    private String gender;
    private String birthDate;
    private String dayOfWeek;
    private String grade;
    private String phone;
    private String joinDate;
    private String address;
    private String parent1Name;
    private String parent2Name;
    private String parentPhone;
    private String profileImageBase64;
    private String leavingDate;

    private String classId; // מזהה של ה-CLASS אליו הוא שייך

    public Student() {}

    public Student(String fullName, String activeNumber, String gender, String birthDate,
                   String dayOfWeek, String grade, String phone, String joinDate,
                   String address, String parent1Name, String parent2Name,
                   String parentPhone, String profileImageBase64, String leavingDate, String classId) {
        this.fullName = fullName;
        this.activeNumber = activeNumber;
        this.gender = gender;
        this.birthDate = birthDate;
        this.dayOfWeek = dayOfWeek;
        this.grade = grade;
        this.phone = phone;
        this.joinDate = joinDate;
        this.address = address;
        this.parent1Name = parent1Name;
        this.parent2Name = parent2Name;
        this.parentPhone = parentPhone;
        this.profileImageBase64 = profileImageBase64;
        this.leavingDate = leavingDate;
        this.classId = classId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getActiveNumber() {
        return activeNumber;
    }

    public void setActiveNumber(String activeNumber) {
        this.activeNumber = activeNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
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

    public String getParentPhone() {
        return parentPhone;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
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
                ", organizationId='" + activeNumber + '\'' +
                ", gender='" + gender + '\'' +
                ", dateOfBirth='" + birthDate + '\'' +
                ", classDay='" + dayOfWeek + '\'' +
                ", grade='" + grade + '\'' +
                ", phoneNumber='" + phone + '\'' +
                ", joiningDate='" + joinDate + '\'' +
                ", address='" + address + '\'' +
                ", parent1Name='" + parent1Name + '\'' +
                ", parent2Name='" + parent2Name + '\'' +
                ", parentPhoneNumber='" + parentPhone + '\'' +
                ", photo='" + profileImageBase64 + '\'' +
                ", leavingDate='" + leavingDate + '\'' +
                ", classId='" + classId + '\'' +
                '}';
    }

}
