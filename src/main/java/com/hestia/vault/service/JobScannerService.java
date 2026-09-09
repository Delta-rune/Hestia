package com.hestia.vault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hestia.vault.dto.JobDTO;
import com.hestia.vault.model.AcademicRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobScannerService {

    private static final String REMOTIVE_API_URL = "https://remotive.com/api/remote-jobs?search=";
    private final AcademicRecordService academicRecordService;
    private final ObjectMapper objectMapper;

    @Autowired
    public JobScannerService(AcademicRecordService academicRecordService) {
        this.academicRecordService = academicRecordService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Converts a user's saved AcademicRecord (degree name and total credits)
     * into a relevant search keyword string for job scanning.
     */
    public String deriveKeywordsFromAcademicRecord(AcademicRecord record) {
        if (record == null || record.getDegreeName() == null || record.getDegreeName().isBlank()) {
            return "Software Developer";
        }

        String degree = record.getDegreeName().trim();
        String lowerDegree = degree.toLowerCase();
        StringBuilder keywords = new StringBuilder(degree);

        // Matching Logic based on degree discipline
        if (lowerDegree.contains("computer") || lowerDegree.contains("software") || lowerDegree.contains("cs") || lowerDegree.contains("it")) {
            keywords.append(" Software Developer Engineer");
        } else if (lowerDegree.contains("electrical") || lowerDegree.contains("electronics") || lowerDegree.contains("eee") || lowerDegree.contains("ece")) {
            keywords.append(" Electrical Engineer Systems Hardware");
        } else if (lowerDegree.contains("mechanical") || lowerDegree.contains("robotics") || lowerDegree.contains("auto")) {
            keywords.append(" Mechanical Engineer Design Robotics");
        } else if (lowerDegree.contains("civil") || lowerDegree.contains("structural")) {
            keywords.append(" Civil Engineer Project Inspector");
        } else if (lowerDegree.contains("data") || lowerDegree.contains("ai") || lowerDegree.contains("analytics") || lowerDegree.contains("math")) {
            keywords.append(" Data Scientist Machine Learning Engineer");
        } else if (lowerDegree.contains("business") || lowerDegree.contains("management") || lowerDegree.contains("finance")) {
            keywords.append(" Product Manager Business Analyst");
        } else {
            keywords.append(" Professional Specialist");
        }

        // Credit-based seniority hint
        if (record.getTotalCreditsEarned() != null) {
            if (record.getTotalCreditsEarned() >= 120) {
                keywords.append(" Graduate Engineer");
            } else {
                keywords.append(" Intern Junior");
            }
        }

        return keywords.toString();
    }

    /**
     * Scans opportunities based on location, jobType, and qualifications.
     * Uses saved AcademicRecord if userId is provided and qualifications are omitted.
     */
    public List<JobDTO> scanJobs(String location, String jobType, String qualifications, Long userId) {
        String finalQualifications = qualifications;
        String finalLocation = (location != null && !location.isBlank()) ? location.trim() : "Remote / Flexible";
        String finalJobType = (jobType != null && !jobType.isBlank()) ? jobType.trim() : "Full-Time";

        // Auto-derive keywords from saved AcademicRecord if qualifications omitted
        if ((finalQualifications == null || finalQualifications.isBlank()) && userId != null) {
            Optional<AcademicRecord> recordOpt = academicRecordService.getAcademicRecordByUserId(userId);
            if (recordOpt.isPresent()) {
                finalQualifications = deriveKeywordsFromAcademicRecord(recordOpt.get());
            }
        }

        if (finalQualifications == null || finalQualifications.isBlank()) {
            finalQualifications = "Software Engineer Developer";
        }

        return fetchAggregatedJobs(finalLocation, finalJobType, finalQualifications);
    }

    /**
     * Aggregator logic to query external Remotive REST API for live remote tech jobs
     * with structured fallback response on connection issues.
     */
    private List<JobDTO> fetchAggregatedJobs(String location, String jobType, String qualifications) {
        try {
            String primaryKeyword = qualifications.split(" ")[0];
            String encodedQuery = URLEncoder.encode(primaryKeyword, StandardCharsets.UTF_8);
            String targetUrl = REMOTIVE_API_URL + encodedQuery + "&limit=10";

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(3000);
            requestFactory.setReadTimeout(4000);

            RestTemplate restTemplate = new RestTemplate(requestFactory);
            String rawJson = restTemplate.getForObject(targetUrl, String.class);

            if (rawJson != null && !rawJson.isBlank()) {
                JsonNode rootNode = objectMapper.readTree(rawJson);
                List<JobDTO> liveJobs = new ArrayList<>();

                if (rootNode.has("jobs") && rootNode.get("jobs").isArray()) {
                    for (JsonNode node : rootNode.get("jobs")) {
                        String id = node.has("id") ? node.get("id").asText() : UUID.randomUUID().toString();
                        String title = node.has("title") ? node.get("title").asText() : "Software Developer";
                        String company = node.has("company_name") ? node.get("company_name").asText() : "Tech Company";
                        
                        String reqLocation = node.has("candidate_required_location") && !node.get("candidate_required_location").asText().isBlank()
                                ? node.get("candidate_required_location").asText()
                                : location;

                        String rawType = node.has("job_type") ? node.get("job_type").asText() : "full_time";
                        String empType = mapRemotiveJobType(rawType, jobType);

                        String rawDesc = node.has("description") ? node.get("description").asText() : "";
                        String cleanDesc = sanitizeHtmlDescription(rawDesc);
                        if (cleanDesc.length() > 250) {
                            cleanDesc = cleanDesc.substring(0, 247) + "...";
                        }
                        if (cleanDesc.isBlank()) {
                            cleanDesc = "Remote " + title + " position at " + company + ".";
                        }

                        String applyUrl = node.has("url") ? node.get("url").asText() : "https://remotive.com";

                        liveJobs.add(new JobDTO(id, title, company, reqLocation, empType, cleanDesc, applyUrl));
                    }
                }

                if (!liveJobs.isEmpty()) {
                    return liveJobs;
                }
            }
        } catch (Exception ignored) {
            // Graceful fallback to mock listings on offline environment or API timeout
        }

        return fetchFallbackJobs(location, jobType, qualifications);
    }

    private List<JobDTO> fetchFallbackJobs(String location, String jobType, String qualifications) {
        List<JobDTO> jobs = new ArrayList<>();
        String primaryKeyword = qualifications.split(" ")[0];

        // Job 1
        jobs.add(new JobDTO(
                UUID.randomUUID().toString(),
                primaryKeyword + " Engineer - Core Systems",
                "Nexus Technologies",
                location,
                formatEmploymentType(jobType, "Full-Time"),
                "Join our core engineering team to build scalable microservices and high-throughput data pipelines. Drive architecture decisions and deploy critical production features with modern cloud infrastructure.",
                "https://careers.google.com/jobs/results/"
        ));

        // Job 2
        jobs.add(new JobDTO(
                UUID.randomUUID().toString(),
                "Lead " + qualifications + " Specialist",
                "Aether Innovations Inc.",
                location.equalsIgnoreCase("Remote / Flexible") ? "San Francisco, CA (Hybrid)" : location,
                formatEmploymentType(jobType, "Full-Time"),
                "Architect state-of-the-art solutions for enterprise clients. Collaborate with cross-functional product and design teams to deliver high-impact features and robust security protocols.",
                "https://jobs.lever.co/"
        ));

        // Job 3
        jobs.add(new JobDTO(
                UUID.randomUUID().toString(),
                "Junior " + primaryKeyword + " Associate / Trainee",
                "Vanguard Digital Systems",
                location,
                formatEmploymentType(jobType, "Part-Time"),
                "Accelerate your career in a fast-paced technology environment. Gain hands-on experience with modern tech stacks, automated unit testing, and continuous integration workflows under senior mentorship.",
                "https://www.linkedin.com/jobs/"
        ));

        // Job 4
        jobs.add(new JobDTO(
                UUID.randomUUID().toString(),
                qualifications + " Research & Development Intern",
                "Horizon Robotics & AI Labs",
                location.equalsIgnoreCase("Remote / Flexible") ? "Boston, MA (Remote)" : location,
                formatEmploymentType(jobType, "Internship"),
                "Explore next-generation algorithms, rapid prototyping, and empirical modeling. Perfect opportunity for motivated students and graduates to work directly on cutting-edge industry applications.",
                "https://www.indeed.com/"
        ));

        // Job 5
        jobs.add(new JobDTO(
                UUID.randomUUID().toString(),
                "Senior " + primaryKeyword + " Solutions Architect",
                "Apex Cloud Networks",
                location,
                formatEmploymentType(jobType, "Full-Time"),
                "Lead complex technical deployments and optimize distributed cloud infrastructure. Responsible for technical guidance, code reviews, and maintaining peak system reliability across global clusters.",
                "https://www.glassdoor.com/Job/"
        ));

        return jobs;
    }

    private String sanitizeHtmlDescription(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private String mapRemotiveJobType(String rawType, String userSelectedType) {
        if (userSelectedType != null && !userSelectedType.isBlank() && !userSelectedType.equalsIgnoreCase("All") && !userSelectedType.equalsIgnoreCase("All Types")) {
            return userSelectedType;
        }
        if (rawType == null) return "Full-Time";
        return switch (rawType.toLowerCase()) {
            case "full_time", "full-time" -> "Full-Time";
            case "part_time", "part-time" -> "Part-Time";
            case "contract", "freelance" -> "Contract";
            case "internship" -> "Internship";
            default -> "Full-Time";
        };
    }

    private String formatEmploymentType(String selectedType, String defaultType) {
        if (selectedType != null && !selectedType.isBlank() && !selectedType.equalsIgnoreCase("All") && !selectedType.equalsIgnoreCase("All Types")) {
            return selectedType;
        }
        return defaultType;
    }
}

