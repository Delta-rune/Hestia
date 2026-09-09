package com.hestia.vault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "academic_records")
public class AcademicRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "apaar_id")
    private String apaarId;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "degree_name")
    private String degreeName;

    @Column(name = "current_cgpa")
    private Double currentCgpa;

    @Column(name = "total_credits_earned")
    private Integer totalCreditsEarned;

    @Column(name = "student_status")
    private String studentStatus = "PRE_GRADUATE"; // PRE_GRADUATE, UNDERGRADUATE, POST_GRADUATE, GRADUATED

    @Column(name = "semester_data_json", columnDefinition = "TEXT")
    private String semesterDataJson;

    public AcademicRecord() {
    }

    public AcademicRecord(Long userId, String apaarId, String institutionName, String degreeName, Double currentCgpa, Integer totalCreditsEarned) {
        this.userId = userId;
        this.apaarId = apaarId;
        this.institutionName = institutionName;
        this.degreeName = degreeName;
        this.currentCgpa = currentCgpa;
        this.totalCreditsEarned = totalCreditsEarned;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getApaarId() {
        return apaarId;
    }

    public void setApaarId(String apaarId) {
        this.apaarId = apaarId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDegreeName() {
        return degreeName;
    }

    public void setDegreeName(String degreeName) {
        this.degreeName = degreeName;
    }

    public Double getCurrentCgpa() {
        return currentCgpa;
    }

    public void setCurrentCgpa(Double currentCgpa) {
        this.currentCgpa = currentCgpa;
    }

    public Integer getTotalCreditsEarned() {
        return totalCreditsEarned;
    }

    public void setTotalCreditsEarned(Integer totalCreditsEarned) {
        this.totalCreditsEarned = totalCreditsEarned;
    }

    public String getStudentStatus() {
        return studentStatus;
    }

    public void setStudentStatus(String studentStatus) {
        this.studentStatus = studentStatus;
    }

    public String getSemesterDataJson() {
        return semesterDataJson;
    }

    public void setSemesterDataJson(String semesterDataJson) {
        this.semesterDataJson = semesterDataJson;
    }
}
