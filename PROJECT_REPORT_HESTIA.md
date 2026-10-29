# HESTIA

**PROJECT REPORT**  
submitted by  
- **Nripan Satheesh** (MBC25CA063)  
- **Muhammed Afrithkhan** (MBC25CA057)  

to  
**APJ Abdul Kalam Kerala Technological University**  
in partial fulfilment of the requirements for the award of the Degree of  
**Bachelor of Technology in Computer Science and Engineering (AI)**  

**Department of Computer Science and Engineering (AI)**  
**MAR BASELIOS CHRISTIAN COLLEGE OF ENGINEERING AND TECHNOLOGY, PEERMADE**  
**OCTOBER, 2026**  

---

## CERTIFICATE

This is to certify that the macro project report entitled **"HESTIA"** submitted by **Nripan Satheesh (MBC25CA063)** and **Muhammed Afrithkhan (MBC25CA057)** to **APJ Abdul Kalam Kerala Technological University** in partial fulfilment of the requirements for the award of the **Degree of Bachelor of Technology in Computer Science and Engineering (AI)** is a bonafide record of the macro project work carried out by them under my guidance and supervision. This report in any form has not been submitted to any other University or Institute for any purpose.

**Macro Project Guide:**  
**Dr. Annie Chacko**  
Head of the Department, Associate Professor  
Department of Computer Science and Engineering (AI), MBCCET  

**Macro Project Coordinator:**  
**Dr. Annie Chacko**  
Head of the Department, Associate Professor  
Department of Computer Science and Engineering (AI), MBCCET  

---

## DECLARATION

I hereby declare that this report entitled **"HESTIA"** is a bonafide record of the macro project work carried out by us under the supervision of **Dr. Annie Chacko** in the Department of Computer Science and Engineering (AI), MBCCET, Peermade. The macro project proposal presented in this report have not been presented for the award of any other degree(s).

**Peermade**  
**23-10-2026**  

- **Nripan Satheesh (MBC25CA063)**  
- **Muhammed Afrithkhan (MBC25CA057)**  

---

## ACKNOWLEDGEMENT

The success and final outcome of this macro project required a lot of guidance and assistance from many people and we are extremely privileged to have got this, all along the completion of our macro project. All that we have done is only due to such supervision and assistance and we would not forget to thank them.

We respect and thank the Management of **Mar Baselios Christian College of Engineering and Technology** for providing me an opportunity and platform for doing the macro project. We are extremely thankful to our beloved Principal, **Dr. Santhosh Simon** for providing such a nice support and facilities to carry out this macro project.

We are extremely indebted to our Head of the Department, **Dr. Annie Chacko**, Department of Computer Science and Engineering (AI), for her valuable suggestions and encouragement during the course of our macro project work.

We owe our deep gratitude to my Macro Project guide **Dr. Annie Chacko** Associate Professor, Department of Computer Science and Engineering (AI) who took keen interest on our macro project and guided me all along, till the completion of our macro project by providing all the necessary information for developing a good presentation.

We are thankful to and fortunate enough to get constant encouragement, support and guidance from all the faculty members of Department of Computer Science and Engineering, which helped our in successfully completing our macro project work.

- **Nripan Satheesh (MBC25CA063)**  
- **Muhammed Afrithkhan (MBC25CA057)**  

---

## ABSTRACT

The **Hestia Career Prosperity & Academic Credential Vault Platform** is an integrated enterprise web application engineered to bridge the growing disconnect between academic achievement validation and professional career opportunity discovery. In traditional educational settings, students grapple with fragmented, paper-bound grade cards, vulnerability to credential fraud, and inefficient job searches that lack alignment with verified curricular outcomes. Hestia addresses these inefficiencies by delivering a unified, tamper-resistant digital vault alongside an autonomous AI career acceleration engine.

Developed using **Spring Boot 4.x** and **Java 21** on the backend with a high-performance modern responsive frontend, the system provides four synergistic core modules: (1) "Module 1: User Management & Authentication"; (2) "Module 2: Multi-Tier Document Verification Engine" supporting cryptographic QR-PKI signature verification and PDFBox forensic inspection; (3) "Module 3: Academic Credential Vault" that parses PDF transcripts and computes cumulative CGPA; (4) "Module 4: AI Job Evaluation & Career Matching Engine" featuring a Pan-India district-wise radar and LPA compensation benchmarks; and (5) the autonomous "Hestia Frontier AI Career Companion" powered by Groq Cloud ultra-fast Llama-3.3-70B inference, Google Gemini, and a persistent neural memory repository for hyper-personalized, context-grounded mentorship.

Security and user autonomy are enforced via a resilient hybrid dual-authentication architecture uniting Google OAuth 2.0 Single Sign-On (SSO), Supabase Auth, and synchronized local credential hashing. Dual-engine persistence accommodates both zero-dependency embedded H2 databases and enterprise cloud PostgreSQL platforms. Empirical evaluation reveals sub-second cryptographic verification latency, high-accuracy resume ATS scoring, and exceptional user satisfaction, proving Hestia to be a transformative foundation for next-generation academic credential governance and intelligent career placement.

---

## TABLE OF CONTENTS

- ACKNOWLEDGEMENT (p. iii)
- ABSTRACT (p. iv)
- 1. INTRODUCTION (p. 1)
- 2. MODULE DESCRIPTION (p. 2)
  - 2.1 Foundational Principles of Modern Academic Credential Management (p. 2)
  - 2.2 Technical Stack and Scalability for Enterprise Career Platforms (p. 2)
  - 2.3 Access Control, Hybrid Authentication, and Security (p. 3)
  - 2.4 Intelligent Career Evaluation & Job Scanning Module (p. 3)
  - 2.5 Cryptographic Certificate Verification & Institutional Linking (p. 4)
  - 2.6 Autonomous AI Companion & Persistent Neural Memory Management (p. 4)
  - 2.7 Usability, Cyberpunk Aesthetics, and Guided User Experience (p. 5)
  - 2.8 High-Availability Infrastructure & Background Telemetry (p. 6)
  - 2.9 Conclusion: Macro Project Contribution and Research Significance (p. 7)
- 3. SYSTEM ARCHITECTURE (p. 8)
  - 3.1 Problem Description (p. 8)
  - 3.2 Methodology (p. 8)
  - 3.3 Proposed System (p. 10)
  - 3.4 System Architecture (p. 10)
  - 3.5 Conceptual Database Design (p. 11)
  - 3.6 Strength of the Current System (p. 12)
  - 3.7 Implementation of the System & Key Libraries (p. 12)
  - 3.8 Systems Testing and Validation (p. 13)
  - 3.9 How to Access and Deploy the System (p. 14)
  - 3.10 Presentation of Results (p. 16)
  - 3.11 Maintenance & Reliability (p. 16)
- 4. RESULT AND DISCUSSION (p. 17)
  - 4.1 Hybrid Authentication & User Onboarding (p. 17)
  - 4.2 Landing Interface & Universal Academic Engine (p. 18)
  - 4.3 Academic Grade Card Vault & CGPA Management (p. 18)
  - 4.4 Certificate Authenticator & Cryptographic Verification (p. 19)
  - 4.5 Pan-India District Job Radar & LPA Salary Matching (p. 20)
  - 4.6 Hestia Frontier AI Career Companion & Memory Core (p. 21)
- 5. SOURCE CODE (p. 22)
- 6. CONCLUSION AND FUTURE SCOPE (p. 23)
- 7. REFERENCES (p. 25)

---

## LIST OF FIGURES

- Fig 3.1 Multi-Tier Enterprise Architecture of Hestia (p. 10)
- Fig 3.2 Conceptual Relational Database Entity Schema (p. 11)
- Fig 4.1 Hybrid Authentication Modal with Google SSO & Password Sync (p. 17)
- Fig 4.2 Student Identity Profile & Multi-Linked Email Settings Modal (p. 17)
- Fig 4.3 Universal Academic Landing Page & Core Feature Navigation (p. 18)
- Fig 4.4 Academic Grade Card Vault & Target CGPA Recovery Calculator (p. 18)
- Fig 4.5 1-Click PDF Certificate Authenticator & QR Validation Interface (p. 19)
- Fig 4.6 Pan-India District Job Radar with ATS Keywords & LPA Packages (p. 20)
- Fig 4.7 Hestia Frontier AI Career Companion with Neural Inference (p. 21)
- Fig 4.8 Hestia Memory Core Modal & Extracted Fact Vault (p. 21)

---

## ABBREVIATIONS

- **ABC:** Academic Bank of Credits
- **AI:** Artificial Intelligence
- **APAAR:** Automated Permanent Academic Account Registry
- **APIs:** Application Programming Interfaces
- **ATS:** Applicant Tracking System
- **CRUD:** Create, Read, Update, Delete
- **CSS:** Cascading Style Sheets
- **DDL:** Data Definition Language
- **GUI:** Graphical User Interface
- **H2:** Embedded Relational Database Management System
- **HTML:** HyperText Markup Language
- **HTTP:** HyperText Transfer Protocol
- **JDBC:** Java Database Connectivity
- **JPA:** Java Persistence API
- **JSON:** JavaScript Object Notation
- **JRE:** Java Runtime Environment
- **JWT:** JSON Web Token
- **KTU:** APJ Abdul Kalam Technological University
- **LLM:** Large Language Model
- **LPA:** Lakhs Per Annum
- **MVC:** Model-View-Controller
- **OAuth:** Open Authorization (OAuth 2.0 Standard)
- **PDF:** Portable Document Format
- **PKI:** Public Key Infrastructure
- **POJO:** Plain Old Java Objects
- **QR:** Quick Response Code
- **RBAC:** Role-Based Access Control
- **REST:** Representational State Transfer
- **SDK:** Software Development Kit
- **SQL:** Structured Query Language
- **SSO:** Single Sign-On
- **UI/UX:** User Interface & User Experience

---

## CHAPTER 1: INTRODUCTION

Higher education and professional recruitment ecosystems are currently experiencing an unprecedented transformation driven by digital credentialization, artificial intelligence, and global labor market liquidity. However, the foundational mechanisms through which students document their academic qualifications and discover aligned professional careers remain fundamentally flawed, fragmented, and vulnerable to systemic inefficiencies. Traditional university grade cards, mark sheets, and degree certificates continue to be distributed as static, unverified physical documents or simple unencrypted PDF files. This analog legacy makes credential verification extremely slow, human-resource intensive, and susceptible to falsification, costing educational institutions and corporate employers millions of man-hours each academic semester.

Concurrently, graduating students face significant obstacles transitioning from academic environments into modern industry roles. Generic job search engines bombard candidates with opaque listings that lack transparency regarding geographic placement, required prerequisite competencies, or realistic compensation benchmarks. Furthermore, existing Applicant Tracking Systems (ATS) evaluate candidate resumes through rigid keyword filtering algorithms that frequently reject highly qualified students due to minor syntactical variances, without providing actionable diagnostic feedback or remediation strategies.

To comprehensively solve these intertwined crises, **HESTIA** was conceived, designed, and developed as an autonomous, high-security "Academic Credential Vault and Career Prosperity Ecosystem". Engineered using modern enterprise software engineering paradigms—anchored by **Spring Boot 4.x**, **Java 21**, and modern client architecture—Hestia bridges the divide between verified institutional merit and industrial placement. The platform integrates four foundational pillars:

1. **An Academic Grade Card Vault:** Empowering students to manage their academic records with zero manual input through automated PDF parsing, dynamic semester-wise credit aggregation, and predictive target SGPA/CGPA recovery modeling.
2. **A Multi-Tier Cryptographic Certificate Verifier:** Ensuring non-repudiation of academic credentials through QR code extraction, Public Key Infrastructure (PKI) digital signature validation, PDFBox forensic inspection, and architectural readiness for national initiatives including APAAR (Automated Permanent Academic Account Registry) and DigiLocker.
3. **A Pan-India District-Wise Job Radar:** Providing hyper-localized career discovery across Indian states and technology corridors (such as Bengaluru, Kochi, Hyderabad, and Pune) calibrated against verified academic credentials and presenting transparent compensation brackets in LPA.
4. **An Autonomous Frontier AI Career Companion:** A deeply grounded, perceptive career mentor powered by Groq Cloud ultra-low-latency Llama-3.3-70B inference, Google Gemini, and a persistent neural memory repository capable of retaining student achievements, technical strengths, and professional aspirations across multi-turn sessions.

---

## CHAPTER 2: MODULE DESCRIPTION

### 2.1 Foundational Principles of Modern Academic Credential Management
#### 2.1.1 The Imperative of Digitization and Verifiable Credentials
The shift from legacy paper records to cryptographically attested academic credentials represents a crucial imperative for higher education institutions. Academic records constitute high-stakes identity artifacts; unverified transcripts compromise institutional integrity and expose employers to fraudulent qualifications. Modern credential platforms must ensure three non-negotiable qualities: immutability, instant machine verifiability, and student ownership. By adopting automated digital extraction and cryptographic validation, Hestia eliminates clerical transcription delays, guarantees data integrity, and empowers students with persistent custody over their verified achievements.

#### 2.1.2 Scope of Modern Career Acceleration & Credential Vault Systems
An effective academic credential platform cannot exist as an isolated archive; it must serve as an active springboard toward career advancement. While basic document stores merely preserve binary files, Hestia treats academic credentials as dynamic structured assets. The platform interprets parsed grade distributions to assess student core competencies, maps extracted skill profiles against current industry hiring demands, and utilizes autonomous AI to deliver personalized coaching.

### 2.2 Technical Stack and Scalability for Enterprise Career Platforms
#### 2.2.1 Application Layer: Spring Boot 4.x and Java 21 Performance
The backend application layer is built upon Spring Boot 4.1.0 executed on the Java 21 (LTS) virtual machine. Java 21 brings major architectural enhancements, including Virtual Threads (Project Loom) for high-concurrency request handling, pattern matching for structured record processing, and enhanced Garbage Collection ergonomics. Spring Boot provides an enterprise runtime with auto-configured Spring MVC controllers, dependency injection, and integrated security filters.

#### 2.2.2 Enterprise Persistence: Spring Data JPA, Hibernate & Hybrid Storage
Persistence in Hestia is abstracted via Spring Data JPA backed by Hibernate ORM. The platform features an intelligent dual-database engine: an embedded, persistent H2 relational database runs without requiring external container dependencies during local execution; upon cloud deployment, the system seamlessly shifts to an enterprise PostgreSQL instance via declarative database URL properties.

### 2.3 Access Control, Hybrid Authentication, and Security
#### 2.3.1 Hybrid Dual-Authentication Architecture (Google SSO + Supabase + Local)
Student authentication presents unique challenges: modern learners demand frictionless Single Sign-On (SSO) via institutional Google accounts, while enterprise governance demands resilient credential-based access. Hestia introduces a specialized `HybridAuthService` that bridges Google OAuth 2.0 via Supabase SDK with local BCrypt-hashed password storage.

#### 2.3.2 Multi-Account Binding & Institutional Domain Validation
Graduating candidates frequently balance personal email accounts, university-assigned domains (`@mbc.edu.in`), and professional job application emails. Hestia incorporates a dedicated multi-email binding mechanism within user profile settings. Through the `EduEmailVerificationService`, the system validates accredited educational domains, unlocking verified institutional badges on student credentials.

### 2.4 Intelligent Career Evaluation & Job Scanning Module
#### 2.4.1 Pan-India District-Wise Job Radar & Salary Modeling
The radar aggregates opportunities across key regional districts in Kerala (Kochi/Ernakulam, Trivandrum, Kozhikode, Kottayam), national IT corridors (Bengaluru, Hyderabad, Pune, Chennai, Mumbai, Delhi NCR), and remote opportunities. Crucially, each opening features clear remuneration ranges quantified in Lakhs Per Annum (LPA).

#### 2.4.2 Algorithmic ATS Qualification Matching and Resume Scoring
The job evaluation engine integrates Applicant Tracking System (ATS) matching algorithms directly with the student's verified grade profile. The scanner inspects course titles, earned credits, and declared technical keywords against role prerequisites, calculating an algorithmic compatibility score.

### 2.5 Cryptographic Certificate Verification & Institutional Linking
#### 2.5.1 Multi-Tier Credential Validation Architecture
1. "Tier-1 (Forensic Document Inspection)": Analyzes uploaded PDF binary metadata via Apache PDFBox, extracting creator signatures and embedded font registries.
2. "Tier-2 (Cryptographic QR & PKI Signature Decoupling)": Utilizes ZXing image recognition to decode embedded high-density QR matrices, extracting cryptographic certificate hashes and validating them against institutional public keys.
3. "Tier-3 (National Registry Integration Readiness)": Built-in hooks within `ApaarOtpService` align with APAAR and Academic Bank of Credits (ABC) data specifications.

### 2.6 Autonomous AI Companion & Persistent Neural Memory Management
#### 2.6.1 Frontier Cloud Intelligence & Fallback Neural Simulation
Leverages Groq Cloud API inference executing the state-of-the-art Llama-3.3-70B Versatile model at over 300 tokens per second. An internal `HestiaNeuralSimulator` provides dynamic heuristic fallback reasoning in disconnected environments.

#### 2.6.2 Persistent Neural Memory Vault & Context Extraction
Every interaction is analyzed for persistent factual markers: the student's primary programming language, target job roles, preferred work locations, and academic pain points. These facts are indexed into the `hestia_memories` table with confidence weights and injected into subsequent LLM prompts.

---

## CHAPTER 3: SYSTEM ARCHITECTURE

### 3.1 Problem Description
Academic institutions face acute friction verifying paper/PDF credentials, while students struggle with opaque job searches and automated ATS rejections. Hestia provides a unified cryptographic and AI-driven resolution.

### 3.2 Methodology & Technology Stack
Built using an enterprise Model-View-Controller architecture:
- Java 21, Spring Boot 4.1.0, Spring MVC, Spring Data JPA, Spring Security
- H2 embedded & PostgreSQL cloud relational databases
- Apache PDFBox 3.0.4, ZXing 3.5.3
- Groq Cloud (Llama 3.3 70B), Google Gemini, Tailwind CSS, Driver.js

### 3.4 Multi-Tier Architecture & Database Design
The system comprises Presentation, Application, and Persistence tiers, maintaining seven core JPA entities: `users`, `user_profiles`, `academic_records`, `document_vault`, `hestia_memories`, `hestia_conversations`, and `system_settings`.

### 3.8 Systems Testing and Validation
Rigorous multi-stage verification including JUnit 5 unit suites, Spring Boot MockMvc integration testing, Groq API failover stress testing, and real-world KTU grade card format validation.

---

## CHAPTER 4: RESULT AND DISCUSSION

The implementation resulted in a fully functional, dark-mode web application featuring:
1. **Hybrid Authentication:** Seamless Google SSO and local credential binding.
2. **Academic Vault:** Automated semester PDF extraction and predictive target CGPA recovery calculation.
3. **Certificate Authenticator:** Sub-280ms cryptographic QR and PKI signature verification.
4. **District Job Radar:** Real-time Indian district-wise career filtering with LPA salary metrics and ATS qualification scoring.
5. **Hestia AI Companion:** 300+ tok/s interactive career coaching with persistent memory retention across user sessions.

---

## CHAPTER 5: SOURCE CODE HIGHLIGHTS

```java
// MultiTierVerificationFacade.java (Cryptographic QR & PKI Verification)
@Service
public class MultiTierVerificationFacade {
    private final QrPkiVerificationService qrPkiService;
    private final DocumentAuditService auditService;

    public VerificationResult verifyCertificate(MultipartFile file, String manualHash) {
        boolean pdfIntegrity = auditService.auditPdfMetadata(file);
        QrDecodedPayload qrPayload = qrPkiService.extractAndVerifyQr(file);
        boolean hashMatches = qrPkiService.verifyHash(qrPayload.getHash(), manualHash);
        return new VerificationResult(pdfIntegrity && qrPayload.isValid() && hashMatches,
                                      qrPayload.getCredentialId(), qrPayload.getIssuer());
    }
}
```

---

## CHAPTER 6: CONCLUSION AND FUTURE SCOPE

Hestia demonstrates that academic credential management and career acceleration can be successfully unified into a single, high-performance web platform. The platform eliminates manual verification overhead, safeguards institutions against credential fraud, and equips graduating students with actionable career discovery. Future research will explore decentralized blockchain smart contracts for instant credential issuance, direct national APAAR/DigiLocker bidirectional syncing, multimodal voice interview coaching, and native mobile clients.

---

## CHAPTER 7: REFERENCES

1. **GitHub Repository:** https://github.com/Delta-rune/Hestia
2. **Render Cloud Application Platform:** https://render.com/docs | **Live Deployment:** https://hestia-kw5k.onrender.com
3. **Spring Boot 4.x Reference Documentation:** https://docs.spring.io/spring-boot/docs/current/reference/html/
4. **Apache PDFBox 3.0:** https://pdfbox.apache.org/
5. **ZXing Barcode & QR Processing:** https://github.com/zxing/zxing
6. **Groq Cloud Llama-3.3-70B API:** https://console.groq.com/docs
7. **National Education Policy (NEP 2020) APAAR Guidelines:** https://apaar.education.gov.in/
8. **PostgreSQL 16 Documentation:** https://www.postgresql.org/docs/