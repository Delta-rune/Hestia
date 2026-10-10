package com.hestia.vault.dto;

public class JobDTO {

    private String id;
    private String jobTitle;
    private String companyName;
    private String location;
    private String employmentType; // Full-Time, Part-Time, Internship
    private String descriptionSnippet;
    private String applyUrl;
    private String salaryPackage;   // e.g. "₹7.5 - ₹12.0 LPA", "₹25,000 / month"
    private String experienceLevel; // e.g. "Fresher / 0-2 Yrs", "Graduate Trainee", "Internship"
    private String portalSource;    // e.g. "Naukri.com", "LinkedIn India", "Internshala"

    public JobDTO() {
    }

    public JobDTO(String id, String jobTitle, String companyName, String location, String employmentType, String descriptionSnippet, String applyUrl) {
        this.id = id;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.location = location;
        this.employmentType = employmentType;
        this.descriptionSnippet = descriptionSnippet;
        this.applyUrl = applyUrl;
        this.salaryPackage = "Competitive Market Band (₹ LPA)";
        this.experienceLevel = "Fresher / 0-2 Years";
        this.portalSource = "Naukri India / LinkedIn";
    }

    public JobDTO(String id, String jobTitle, String companyName, String location, String employmentType, String descriptionSnippet, String applyUrl, String salaryPackage, String experienceLevel, String portalSource) {
        this.id = id;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.location = location;
        this.employmentType = employmentType;
        this.descriptionSnippet = descriptionSnippet;
        this.applyUrl = applyUrl;
        this.salaryPackage = salaryPackage;
        this.experienceLevel = experienceLevel;
        this.portalSource = portalSource;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getDescriptionSnippet() {
        return descriptionSnippet;
    }

    public void setDescriptionSnippet(String descriptionSnippet) {
        this.descriptionSnippet = descriptionSnippet;
    }

    public String getApplyUrl() {
        return applyUrl;
    }

    public void setApplyUrl(String applyUrl) {
        this.applyUrl = applyUrl;
    }

    public String getSalaryPackage() {
        return salaryPackage;
    }

    public void setSalaryPackage(String salaryPackage) {
        this.salaryPackage = salaryPackage;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String getPortalSource() {
        return portalSource;
    }

    public void setPortalSource(String portalSource) {
        this.portalSource = portalSource;
    }
}

