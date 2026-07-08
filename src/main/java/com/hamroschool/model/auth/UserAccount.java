package com.hamroschool.model.auth;

public class UserAccount {

    private final String   username;
    private final String   password;
    private final UserRole role;

    private final String fullName;
    private final String gender;
    private final String dateOfBirth;
    private final String phone;
    private final String email;
    private final String address;
    private final String userId;          // STU-0001 or TCH-0001

    private final String assignedClass;
    private final String rollNumber;
    private final String academicSession;
    private final String guardianName;
    private final String guardianRelation;
    private final String guardianPhone;
    private final String guardianEmail;

    private final String subject;
    private final String qualification;
    private final String employmentStatus;

    public UserAccount(String username, String password, UserRole role) {
        this(username, password, role, "", "", "", "", "", "", "",
             "", "", "", "", "", "", "",
             "", "", "");
    }

    public UserAccount(String username, String password, UserRole role,
                       String fullName, String gender, String dateOfBirth,
                       String phone, String email, String address, String userId,
                       String assignedClass, String rollNumber, String academicSession,
                       String guardianName, String guardianRelation,
                       String guardianPhone, String guardianEmail,
                       String subject, String qualification, String employmentStatus) {
        this.username         = username;
        this.password         = password;
        this.role             = role;
        this.fullName         = safe(fullName);
        this.gender           = safe(gender);
        this.dateOfBirth      = safe(dateOfBirth);
        this.phone            = safe(phone);
        this.email            = safe(email);
        this.address          = safe(address);
        this.userId           = safe(userId);
        this.assignedClass    = safe(assignedClass);
        this.rollNumber       = safe(rollNumber);
        this.academicSession  = safe(academicSession);
        this.guardianName     = safe(guardianName);
        this.guardianRelation = safe(guardianRelation);
        this.guardianPhone    = safe(guardianPhone);
        this.guardianEmail    = safe(guardianEmail);
        this.subject          = safe(subject);
        this.qualification    = safe(qualification);
        this.employmentStatus = safe(employmentStatus);
    }

    private static String safe(String v) { return v == null ? "" : v.trim(); }

    public String   getUsername()         { return username; }
    public String   getPassword()         { return password; }
    public UserRole getRole()             { return role; }
    public String   getFullName()         { return fullName; }
    public String   getGender()           { return gender; }
    public String   getDateOfBirth()      { return dateOfBirth; }
    public String   getPhone()            { return phone; }
    public String   getEmail()            { return email; }
    public String   getAddress()          { return address; }
    public String   getUserId()           { return userId; }
    public String   getAssignedClass()    { return assignedClass; }
    public String   getRollNumber()       { return rollNumber; }
    public String   getAcademicSession()  { return academicSession; }
    public String   getGuardianName()     { return guardianName; }
    public String   getGuardianRelation() { return guardianRelation; }
    public String   getGuardianPhone()    { return guardianPhone; }
    public String   getGuardianEmail()    { return guardianEmail; }
    public String   getSubject()          { return subject; }
    public String   getQualification()    { return qualification; }
    public String   getEmploymentStatus() { return employmentStatus; }

    public String getDisplayName() {
        return fullName.isEmpty() ? username : fullName;
    }
}
