package com.example.myapplicationt1;

import androidx.annotation.NonNull;

public class Manager {
    private String uid; // מזהה ייחודי של המשתמש
    private String fullName;
    private String activeNumber;
    private String email;
    private String gender;
    private String birthDate;
    private String dayOfWeek;
    private String phone;
    private String joinDate;
    private String address;
    private String parent1Name;
    private String parent2Name;
    private String parentPhone;
    private String profileImageBase64;
    private String leavingDate;
    private String userType;

    public Manager() {}


    public Manager(String uid, String fullName, String activeNumber, String email, String gender, String birthDate,
                   String dayOfWeek, String phone, String joinDate, String address,
                   String parent1Name, String parent2Name, String parentPhone,
                   String profileImageBase64, String leavingDate, String userType) {
        this.uid = uid;
        this.fullName = fullName;
        this.activeNumber = activeNumber;
        this.email = email;
        this.gender = gender;
        this.birthDate = birthDate;
        this.dayOfWeek = dayOfWeek;
        this.phone = phone;
        this.joinDate = joinDate;
        this.address = address;
        this.parent1Name = parent1Name;
        this.parent2Name = parent2Name;
        this.parentPhone = parentPhone;
        this.profileImageBase64 = profileImageBase64;
        this.leavingDate = leavingDate;
        this.userType = userType;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getActiveNumber() { return activeNumber; }
    public void setActiveNumber(String activeNumber) { this.activeNumber = activeNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getParent1Name() { return parent1Name; }
    public void setParent1Name(String parent1Name) { this.parent1Name = parent1Name; }
    public String getParent2Name() { return parent2Name; }
    public void setParent2Name(String parent2Name) { this.parent2Name = parent2Name; }
    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }
    public String getLeavingDate() { return leavingDate; }
    public void setLeavingDate(String leavingDate) { this.leavingDate = leavingDate; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    @NonNull
    @Override
    public String toString() {
        return "Manager{" +
                "fullName='" + fullName + '\'' +
                ", activeNumber='" + activeNumber + '\'' +
                ", email='" + email + '\'' +
                ", gender='" + gender + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", dayOfWeek='" + dayOfWeek + '\'' +
                ", phone='" + phone + '\'' +
                ", joinDate='" + joinDate + '\'' +
                ", address='" + address + '\'' +
                ", parent1Name='" + parent1Name + '\'' +
                ", parent2Name='" + parent2Name + '\'' +
                ", parentPhone='" + parentPhone + '\'' +
                ", profileImageBase64='" + profileImageBase64 + '\'' +
                ", leavingDate='" + leavingDate + '\'' +
                ", uid='" + uid + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }
}




