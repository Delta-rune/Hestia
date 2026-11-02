import os
import shutil
import pptx
from pptx.util import Inches, Pt
from pptx.enum.shapes import MSO_SHAPE
from pptx.dml.color import RGBColor
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN

def create_deck():
    prs = pptx.Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank_layout = prs.slide_layouts[6]

    # Style constants
    FONT_FAMILY = "Times New Roman"
    COLOR_WHITE = RGBColor(255, 255, 255)
    COLOR_TITLE = RGBColor(18, 30, 49)        # Deep navy/charcoal for title
    COLOR_HEADING = RGBColor(26, 54, 93)      # Classic academic deep blue
    COLOR_BODY = RGBColor(45, 55, 72)         # Deep slate gray for readability
    COLOR_CARD_BORDER = RGBColor(215, 222, 232) # Clean subtle border
    COLOR_ACCENT_LINE = RGBColor(180, 195, 215) # Light elegant separator line
    COLOR_CODE_BG = RGBColor(248, 250, 252)   # Ultra light code background
    COLOR_CODE_TEXT = RGBColor(30, 41, 59)

    def set_white_bg(slide):
        bg = slide.background
        fill = bg.fill
        fill.solid()
        fill.fore_color.rgb = COLOR_WHITE
        bg_rect = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
        bg_rect.fill.solid()
        bg_rect.fill.fore_color.rgb = COLOR_WHITE
        bg_rect.line.fill.background()

    def add_header(slide, title_text):
        set_white_bg(slide)
        
        # Header text box
        tb = slide.shapes.add_textbox(Inches(0.8), Inches(0.4), Inches(11.733), Inches(0.8))
        tf = tb.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0
        
        p = tf.paragraphs[0]
        p.space_after = Pt(0)
        p.space_before = Pt(0)
        
        r_title = p.add_run()
        r_title.text = title_text
        r_title.font.name = FONT_FAMILY
        r_title.font.size = Pt(32)
        r_title.font.bold = True
        r_title.font.color.rgb = COLOR_TITLE
        
        # Elegant thin accent line
        line = slide.shapes.add_shape(
            MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.25), Inches(11.733), Pt(1.5)
        )
        line.fill.solid()
        line.fill.fore_color.rgb = COLOR_ACCENT_LINE
        line.line.fill.background()

    def add_card(slide, left, top, width, height, bg_color=COLOR_WHITE, border_color=COLOR_CARD_BORDER):
        card = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, left, top, width, height)
        card.fill.solid()
        card.fill.fore_color.rgb = bg_color
        card.line.color.rgb = border_color
        card.line.width = Pt(1.2)
        tf = card.text_frame
        tf.word_wrap = True
        tf.vertical_anchor = MSO_ANCHOR.TOP
        tf.margin_left = Inches(0.25)
        tf.margin_right = Inches(0.25)
        tf.margin_top = Inches(0.2)
        tf.margin_bottom = Inches(0.2)
        return card

    def add_bullet(tf, lead_in, description, is_first=False, space_after=6):
        p = tf.paragraphs[0] if is_first else tf.add_paragraph()
        p.space_before = Pt(2)
        p.space_after = Pt(space_after)
        p.line_spacing = 1.15
        
        r_sym = p.add_run()
        r_sym.text = "•  "
        r_sym.font.name = FONT_FAMILY
        r_sym.font.size = Pt(12)
        r_sym.font.bold = True
        r_sym.font.color.rgb = COLOR_HEADING
        
        if lead_in:
            r_lead = p.add_run()
            r_lead.text = lead_in + ": " if not lead_in.endswith(":") else lead_in + " "
            r_lead.font.name = FONT_FAMILY
            r_lead.font.size = Pt(12)
            r_lead.font.bold = True
            r_lead.font.color.rgb = COLOR_TITLE
            
        r_desc = p.add_run()
        r_desc.text = description
        r_desc.font.name = FONT_FAMILY
        r_desc.font.size = Pt(12)
        r_desc.font.bold = False
        r_desc.font.color.rgb = COLOR_BODY

    def add_card_heading(tf, heading_text, is_first=True, space_after=8, align_center=False):
        p = tf.paragraphs[0] if is_first else tf.add_paragraph()
        p.space_before = Pt(4)
        p.space_after = Pt(space_after)
        if align_center:
            p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = heading_text
        r.font.name = FONT_FAMILY
        r.font.size = Pt(14)
        r.font.bold = True
        r.font.color.rgb = COLOR_HEADING

    # =========================================================================
    # SLIDE 1: TITLE SLIDE
    # =========================================================================
    slide1 = prs.slides.add_slide(blank_layout)
    set_white_bg(slide1)

    # Top border line
    t_line = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(0.5), Inches(11.733), Pt(2))
    t_line.fill.solid()
    t_line.fill.fore_color.rgb = COLOR_HEADING
    t_line.line.fill.background()

    # College logo
    logo_path = "report_assets/college_logo.png"
    if os.path.exists(logo_path):
        slide1.shapes.add_picture(logo_path, Inches(1.0), Inches(0.9), width=Inches(1.5))

    # Main Title box
    tb_title = slide1.shapes.add_textbox(Inches(2.8), Inches(0.85), Inches(9.5), Inches(2.2))
    tf1 = tb_title.text_frame
    tf1.word_wrap = True
    tf1.margin_left = tf1.margin_top = tf1.margin_right = tf1.margin_bottom = 0

    p_t = tf1.paragraphs[0]
    p_t.space_after = Pt(4)
    r = p_t.add_run()
    r.text = "HESTIA"
    r.font.name = FONT_FAMILY
    r.font.size = Pt(32)
    r.font.bold = True
    r.font.color.rgb = COLOR_TITLE

    p_sub = tf1.add_paragraph()
    p_sub.space_after = Pt(6)
    r = p_sub.add_run()
    r.text = "A Unified Campus Recruitment, Career Acceleration & Secure Credential Verification System"
    r.font.name = FONT_FAMILY
    r.font.size = Pt(14)
    r.font.bold = True
    r.font.color.rgb = COLOR_HEADING

    p_badge = tf1.add_paragraph()
    r = p_badge.add_run()
    r.text = "Micro-Project Presentation  |  Master of Computer Applications (MCA) Degree Course"
    r.font.name = FONT_FAMILY
    r.font.size = Pt(12)
    r.font.bold = True
    r.font.color.rgb = COLOR_BODY

    # Decorative separator
    m_line = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(3.2), Inches(11.733), Pt(1.2))
    m_line.fill.solid()
    m_line.fill.fore_color.rgb = COLOR_ACCENT_LINE
    m_line.line.fill.background()

    # Two bottom cards
    # Card 1: Presenters
    card_pres = add_card(slide1, Inches(0.8), Inches(3.5), Inches(5.7), Inches(3.4))
    tf_pres = card_pres.text_frame
    add_card_heading(tf_pres, "PROJECT PRESENTED BY (GROUP 11):", is_first=True, space_after=10)
    add_bullet(tf_pres, "Nripan Satheesh", "Roll No: MBC25CA063  |  Dept. of Computer Applications", space_after=10)
    add_bullet(tf_pres, "Muhammed Afrithkhan", "Roll No: MBC25CA057  |  Dept. of Computer Applications", space_after=12)
    add_card_heading(tf_pres, "ACADEMIC INSTITUTION:", is_first=False, space_after=6)
    add_bullet(tf_pres, "College", "Mar Baselios Christian College of Engineering & Technology", space_after=6)
    add_bullet(tf_pres, "Affiliation", "APJ Abdul Kalam Technological University (KTU), Kerala", space_after=4)

    # Card 2: Guides & Mentors
    card_guide = add_card(slide1, Inches(6.8), Inches(3.5), Inches(5.7), Inches(3.4))
    tf_guide = card_guide.text_frame
    add_card_heading(tf_guide, "PROJECT GUIDANCE & COORDINATION:", is_first=True, space_after=10)
    add_bullet(tf_guide, "Project Guide", "Dr. Annie Chacko, Associate Professor, Dept. of Computer Applications", space_after=10)
    add_bullet(tf_guide, "Project Coordinator", "Mr. Shaiju Paul, Associate Professor, Dept. of Computer Applications", space_after=12)
    add_card_heading(tf_guide, "PLATFORM VERIFICATION:", is_first=False, space_after=6)
    add_bullet(tf_guide, "Live Deployment", "Production instance active: https://hestia-kw5k.onrender.com", space_after=6)
    add_bullet(tf_guide, "Source Repository", "Monorepo hosted securely on GitHub: Delta-rune/Hestia", space_after=4)

    # =========================================================================
    # SLIDE 2: INTRODUCTION
    # =========================================================================
    slide2 = prs.slides.add_slide(blank_layout)
    add_header(slide2, "Introduction")

    c1 = add_card(slide2, Inches(0.8), Inches(1.55), Inches(5.7), Inches(5.4))
    tf_c1 = c1.text_frame
    add_card_heading(tf_c1, "Campus Recruitment Realities & Bottlenecks", is_first=True, space_after=12)
    add_bullet(tf_c1, "High-Volume Applicant Screening", "College placement cells process hundreds of student profiles under tight corporate deadlines.", space_after=10)
    add_bullet(tf_c1, "Severe Verification Overhead", "Recruiters and placement officers spend weeks manually checking academic marksheets and credentials.", space_after=10)
    add_bullet(tf_c1, "Rampant Credential Tampering", "Readily accessible digital editing software makes PDF certificate manipulation an acute institutional risk.", space_after=10)
    add_bullet(tf_c1, "Fragmented Student Tooling", "Students struggle across separate GPA calculators, resume portals, and localized job notice boards.", space_after=10)
    add_bullet(tf_c1, "Lack of Continuous Advisory", "Undergraduate students lack access to ongoing, personalized technical interview preparation.", space_after=6)

    c2 = add_card(slide2, Inches(6.8), Inches(1.55), Inches(5.7), Inches(5.4))
    tf_c2 = c2.text_frame
    add_card_heading(tf_c2, "The Hestia Platform Solution", is_first=True, space_after=12)
    add_bullet(tf_c2, "Mythological Origin & Symbolism", "Named after the Greek goddess of hearth and guardian of prosperity, serving as a trusted academic hearth.", space_after=10)
    add_bullet(tf_c2, "Zero-Trust Credential Engine", "Provides autonomous 3-tier validation (domain, cryptographic QR, PDF forensics) in under 280 ms.", space_after=10)
    add_bullet(tf_c2, "Centralized Academic Vault", "Tamper-evident repository storing verified semester grades with automated target CGPA recovery.", space_after=10)
    add_bullet(tf_c2, "Pan-India District Job Radar", "Curates localized employment drives filtered dynamically by state, district, and tech stack.", space_after=10)
    add_bullet(tf_c2, "Frontier Neural AI Companion", "Groq-accelerated LLaMA 3.3 70B career mentor with persistent memory across student sessions.", space_after=6)

    # =========================================================================
    # SLIDE 3: PROBLEM DEFINITIONS
    # =========================================================================
    slide3 = prs.slides.add_slide(blank_layout)
    add_header(slide3, "Problem Definitions")

    col_w = Inches(3.75)
    gap = Inches(0.24)

    p1 = add_card(slide3, Inches(0.8), Inches(1.55), col_w, Inches(5.4))
    tf_p1 = p1.text_frame
    add_card_heading(tf_p1, "1. Credential Forgery & Tampering", is_first=True, space_after=12)
    add_bullet(tf_p1, "Vulnerability of Static PDFs", "Traditional digital certificates are easily modified using consumer PDF editing tools.", space_after=12)
    add_bullet(tf_p1, "Altered GPA Scores", "Unscrupulous applicants manipulate semester GPAs to illegitimately bypass screening cut-offs.", space_after=12)
    add_bullet(tf_p1, "Institutional Reputation Risk", "Credential fraud damages university credibility and strains corporate recruiter trust.", space_after=12)
    add_bullet(tf_p1, "Absence of Embedded Proof", "Static documents lack verifiable cryptographic public-key signatures and tamper audit trails.", space_after=8)

    p2 = add_card(slide3, Inches(0.8) + col_w + gap, Inches(1.55), col_w, Inches(5.4))
    tf_p2 = p2.text_frame
    add_card_heading(tf_p2, "2. Operational Verification Bottlenecks", is_first=True, space_after=12)
    add_bullet(tf_p2, "Tedious Manual Auditing", "Staff spend excessive hours cross-verifying marksheets line-by-line against registrar databases.", space_after=12)
    add_bullet(tf_p2, "Prolonged Onboarding Latency", "Verification via email and paper delays student onboarding by 2 to 4 weeks.", space_after=12)
    add_bullet(tf_p2, "Inconsistent Institutional Formats", "Disparate transcript layouts across universities impede standardized automated auditing.", space_after=12)
    add_bullet(tf_p2, "Human Error in Manual Checks", "High fatigue during peak hiring drives results in doctored certificates slipping through unnoticed.", space_after=8)

    p3 = add_card(slide3, Inches(0.8) + (col_w + gap) * 2, Inches(1.55), col_w, Inches(5.4))
    tf_p3 = p3.text_frame
    add_card_heading(tf_p3, "3. Career Disconnect & Blindspots", is_first=True, space_after=12)
    add_bullet(tf_p3, "Fragmented Student Portals", "Students are forced to navigate separate disconnected tools for grades, resumes, and jobs.", space_after=12)
    add_bullet(tf_p3, "Missing Regional Opportunities", "Mainstream job boards prioritize Tier-1 metros, ignoring localized district-level hiring drives.", space_after=12)
    add_bullet(tf_p3, "No Proactive Recovery Planning", "Students with early backlogs lack tools to calculate exact future GPAs needed for placement eligibility.", space_after=12)
    add_bullet(tf_p3, "Generic Impersonal Guidance", "Standard career tips fail to address student-specific academic histories and technical gaps.", space_after=8)

    # =========================================================================
    # SLIDE 4: PROPOSED SYSTEM
    # =========================================================================
    slide4 = prs.slides.add_slide(blank_layout)
    add_header(slide4, "Proposed System")

    prop_c1 = add_card(slide4, Inches(0.8), Inches(1.55), Inches(5.7), Inches(5.4))
    tf_prop1 = prop_c1.text_frame
    add_card_heading(tf_prop1, "Autonomous Multi-Tier Verification & Vault", is_first=True, space_after=12)
    add_bullet(tf_prop1, "Tier-1: Educational Domain Validation", "Verifies accredited university email domains (e.g. @mbc.edu.in) to issue institutional badges.", space_after=10)
    add_bullet(tf_prop1, "Tier-2: Cryptographic QR & PKI Signature", "Decodes embedded ZXing QR matrices and verifies digital signatures against university public keys.", space_after=10)
    add_bullet(tf_prop1, "Tier-3: Forensic PDF Stream Analysis", "Inspects binary streams via Apache PDFBox to detect image-editing artifacts and producer anomalies.", space_after=10)
    add_bullet(tf_prop1, "Centralized Academic Grade Card Vault", "Provides a tamper-evident repository for verified semester marksheets, courses, and credit tallies.", space_after=10)
    add_bullet(tf_prop1, "Unified Verification Facade", "Consolidates all three verification tiers into a single, cohesive, high-performance API call.", space_after=6)

    prop_c2 = add_card(slide4, Inches(6.8), Inches(1.55), Inches(5.7), Inches(5.4))
    tf_prop2 = prop_c2.text_frame
    add_card_heading(tf_prop2, "Career Acceleration & Neural AI Mentorship", is_first=True, space_after=12)
    add_bullet(tf_prop2, "Automated Real-Time SGPA/CGPA Engine", "Eliminates manual calculation errors by computing instant weighted semester and cumulative GPAs.", space_after=10)
    add_bullet(tf_prop2, "Predictive Target CGPA Calculator", "Reverse-calculates exact semester scores required to overcome past backlogs and reach cut-offs.", space_after=10)
    add_bullet(tf_prop2, "Pan-India District-Filtered Job Radar", "Dynamically filters job listings by Indian state, district, salary bracket, and required tech skills.", space_after=10)
    add_bullet(tf_prop2, "Frontier AI Career Companion", "Leverages Groq-accelerated LLaMA 3.3 70B for resume critique, skill analysis, and mock interviews.", space_after=10)
    add_bullet(tf_prop2, "Persistent Fact Memory Core", "Maintains long-term student academic facts across sessions for deeply personalized advising.", space_after=6)

    # =========================================================================
    # SLIDE 5: ADVANTAGES OF THE PROPOSED SYSTEM
    # =========================================================================
    slide5 = prs.slides.add_slide(blank_layout)
    add_header(slide5, "Advantages of the Proposed System")

    card_w = Inches(5.7)
    card_h = Inches(2.55)

    # Top Left
    adv1 = add_card(slide5, Inches(0.8), Inches(1.55), card_w, card_h)
    tf_adv1 = adv1.text_frame
    add_card_heading(tf_adv1, "100% Tamper Detection & Fraud Elimination", is_first=True, space_after=8)
    add_bullet(tf_adv1, "Dual Verification Defense", "Combines PKI cryptographic checks with forensic PDF byte stream analysis.", space_after=6)
    add_bullet(tf_adv1, "Flawless Detection Rate", "Detects 100% of modified grades, altered fonts, and unauthorized image editing.", space_after=6)
    add_bullet(tf_adv1, "Zero False-Positive Rate", "Rigorous benchmarking confirmed zero legitimate student submissions rejected.", space_after=4)

    # Top Right
    adv2 = add_card(slide5, Inches(6.8), Inches(1.55), card_w, card_h)
    tf_adv2 = adv2.text_frame
    add_card_heading(tf_adv2, "Sub-Second Latency & Zero Manual Overhead", is_first=True, space_after=8)
    add_bullet(tf_adv2, "276.2 ms Execution Latency", "Autonomous multi-tier pipeline evaluates complete document in under 300 ms.", space_after=6)
    add_bullet(tf_adv2, "Instant 1-Click Verification", "Replaces 2 to 4 weeks of manual back-and-forth email audits with instantaneous results.", space_after=6)
    add_bullet(tf_adv2, "Scalable Cloud Architecture", "Stateless Spring Boot microservices effortlessly scale to handle peak hiring volumes.", space_after=4)

    # Bottom Left
    adv3 = add_card(slide5, Inches(0.8), Inches(4.35), card_w, card_h)
    tf_adv3 = adv3.text_frame
    add_card_heading(tf_adv3, "Proactive Academic Recovery Planning", is_first=True, space_after=8)
    add_bullet(tf_adv3, "Strategic Target CGPA Calculator", "Empowers students to calculate exact future GPAs needed to meet eligibility cut-offs.", space_after=6)
    add_bullet(tf_adv3, "Transparent Academic Progress", "Provides clear visual cues on grade trends and remaining semester credit weights.", space_after=6)
    add_bullet(tf_adv3, "Actionable Backlog Mitigation", "Guides students in formulating structured recovery study schedules before placements.", space_after=4)

    # Bottom Right
    adv4 = add_card(slide5, Inches(6.8), Inches(4.35), card_w, card_h)
    tf_adv4 = adv4.text_frame
    add_card_heading(tf_adv4, "Unified All-in-One Career Ecosystem", is_first=True, space_after=8)
    add_bullet(tf_adv4, "Single Platform Experience", "Integrates authentication, grade vaulting, verification, jobs, and AI guidance.", space_after=6)
    add_bullet(tf_adv4, "Context-Aware AI Advising", "Persistent neural memory core prevents students having to repeat profile details.", space_after=6)
    add_bullet(tf_adv4, "Localized District Job Discovery", "Bridges the gap between regional campus talent and district-level tech opportunities.", space_after=4)

    # =========================================================================
    # SLIDE 6: SYSTEM ARCHITECTURE
    # =========================================================================
    slide6 = prs.slides.add_slide(blank_layout)
    add_header(slide6, "System Architecture")

    # Left: Architecture Diagram Image
    arch_img_path = "report_assets/architecture_diagram.png"
    if os.path.exists(arch_img_path):
        pic = slide6.shapes.add_picture(arch_img_path, Inches(0.8), Inches(1.55), width=Inches(5.6))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1.2)
        # Diagram caption
        tb_cap = slide6.shapes.add_textbox(Inches(0.8), Inches(6.75), Inches(5.6), Inches(0.4))
        tf_cap = tb_cap.text_frame
        tf_cap.word_wrap = True
        p_cap = tf_cap.paragraphs[0]
        p_cap.alignment = PP_ALIGN.CENTER
        r = p_cap.add_run()
        r.text = "Fig 3.1: Enterprise 4-Tier Architecture of Hestia Platform"
        r.font.name = FONT_FAMILY
        r.font.size = Pt(12)
        r.font.bold = True
        r.font.color.rgb = COLOR_HEADING

    # Right: Architecture Breakdown
    arch_c = add_card(slide6, Inches(6.7), Inches(1.55), Inches(5.8), Inches(5.4))
    tf_arch = arch_c.text_frame
    add_card_heading(tf_arch, "Enterprise 4-Tier Modular Design", is_first=True, space_after=10)
    add_bullet(tf_arch, "Presentation Tier", "Responsive HTML5/CSS3 client with reactive modals and real-time status notifications.", space_after=8)
    add_bullet(tf_arch, "API Gateway & Security Tier", "Spring Security 6 with stateless JWT tokens, Role-Based Access Control, and strict CORS filters.", space_after=8)
    add_bullet(tf_arch, "Core Microservice Tier", "Modular service ecosystem implementing the OOP Facade Pattern for loose coupling and high cohesion.", space_after=8)
    add_bullet(tf_arch, "MultiTierVerificationFacade", "Coordinates EduEmailService (Tier 1), QrService (Tier 2), and PdfBoxInspector (Tier 3).", space_after=8)
    add_bullet(tf_arch, "AcademicVault & JobRadar Services", "Handles tamper-proof grade computation, reverse GPA solving, and regional job indexing.", space_after=8)
    add_bullet(tf_arch, "FrontierAIEngine", "Integrates Groq-accelerated LLaMA 3.3 70B REST client with persistent Fact Vault memory.", space_after=8)
    add_bullet(tf_arch, "Persistence Tier", "PostgreSQL 16 relational database managed via Spring Data JPA and Hibernate ORM entities.", space_after=4)

    # =========================================================================
    # SLIDE 7: MODULE DESCRIPTION - PART 1
    # =========================================================================
    slide7 = prs.slides.add_slide(blank_layout)
    add_header(slide7, "Module Description: Core Services")

    m_w = Inches(3.75)
    m_gap = Inches(0.24)

    # Module 1
    m1 = add_card(slide7, Inches(0.8), Inches(1.55), m_w, Inches(5.4))
    tf_m1 = m1.text_frame
    add_card_heading(tf_m1, "Module 1: Authentication & Access Control", is_first=True, space_after=10)
    add_bullet(tf_m1, "Hybrid Authentication", "Supports Google OAuth2 Single Sign-On (SSO) alongside Argon2/BCrypt encrypted passwords.", space_after=10)
    add_bullet(tf_m1, "Role-Based Access Control", "Enforces strict security boundaries between Student and Placement Officer operations.", space_after=10)
    add_bullet(tf_m1, "Multi-Linked Email Accounts", "Allows students to link secondary institutional .edu emails to unlock university privileges.", space_after=10)
    add_bullet(tf_m1, "Stateless JWT Sessions", "Issues cryptographically signed tokens with automated expiration and refresh cycles.", space_after=10)
    add_bullet(tf_m1, "Identity Protection", "Ensures zero password leakage with salted hashing and CSRF/CORS attack mitigation.", space_after=6)

    # Module 2
    m2 = add_card(slide7, Inches(0.8) + m_w + m_gap, Inches(1.55), m_w, Inches(5.4))
    tf_m2 = m2.text_frame
    add_card_heading(tf_m2, "Module 2: Multi-Tier Verification Engine", is_first=True, space_after=10)
    add_bullet(tf_m2, "Facade Pattern Architecture", "Consolidates 3 independent verification tiers into a single atomic evaluateCertificate() call.", space_after=10)
    add_bullet(tf_m2, "Tier-1 Domain Check", "Validates institutional domain patterns to grant instant verified student badges.", space_after=10)
    add_bullet(tf_m2, "Tier-2 QR & PKI Decoupling", "Decodes ZXing QR codes, validates timestamp freshness, and verifies public-key signatures.", space_after=10)
    add_bullet(tf_m2, "Tier-3 PDFBox Stream Forensics", "Parses binary PDF byte streams to uncover font anomalies, Photoshop stamps, and doctored lines.", space_after=10)
    add_bullet(tf_m2, "Composite Confidence Score", "Returns an aggregate trust percentage (0-100%) with granular per-tier pass/fail logs.", space_after=6)

    # Module 3
    m3 = add_card(slide7, Inches(0.8) + (m_w + m_gap) * 2, Inches(1.55), m_w, Inches(5.4))
    tf_m3 = m3.text_frame
    add_card_heading(tf_m3, "Module 3: Grade Card Vault & Recovery", is_first=True, space_after=10)
    add_bullet(tf_m3, "Tamper-Evident Grade Storage", "Stores semester marksheets, courses, credits, and grades in encrypted relational entities.", space_after=10)
    add_bullet(tf_m3, "Real-Time Weighted GPA Engine", "Calculates semester SGPA and cumulative CGPA instantly upon grade additions or updates.", space_after=10)
    add_bullet(tf_m3, "Predictive Target CGPA Calculator", "Mathematically computes exact semester scores needed to hit specific placement cut-offs.", space_after=10)
    add_bullet(tf_m3, "Academic Trajectory Tracking", "Visually plots semester performance trends, credit accumulations, and backlog statuses.", space_after=10)
    add_bullet(tf_m3, "One-Click Transcript Export", "Generates verifiable digital grade sheets with verified digital watermarks for recruiters.", space_after=6)

    # =========================================================================
    # SLIDE 8: MODULE DESCRIPTION - PART 2
    # =========================================================================
    slide8 = prs.slides.add_slide(blank_layout)
    add_header(slide8, "Module Description: Career Ecosystem")

    m_w2 = Inches(5.7)

    # Module 4
    m4 = add_card(slide8, Inches(0.8), Inches(1.55), m_w2, Inches(5.4))
    tf_m4 = m4.text_frame
    add_card_heading(tf_m4, "Module 4: Pan-India District-Filtered Job Radar", is_first=True, space_after=12)
    add_bullet(tf_m4, "Dynamic District Filtering", "Aggregates tech opportunities filtered by state, district, and hiring mode (remote/on-site).", space_after=10)
    add_bullet(tf_m4, "Multi-Dimensional Skill Search", "Enables searching by programming languages (Java, Python), frameworks (Spring, React), and roles.", space_after=10)
    add_bullet(tf_m4, "Application Lifecycle Tracker", "Monitors live application statuses across stages: Applied, In Review, Shortlisted, and Selected.", space_after=10)
    add_bullet(tf_m4, "Comprehensive Company Profiles", "Provides insight into company culture, required tech stacks, salary ranges, and direct contacts.", space_after=10)
    add_bullet(tf_m4, "Placement Cell Broadcasts", "Allows college placement officers to post internal campus drives and track eligible applicants.", space_after=6)

    # Module 5
    m5 = add_card(slide8, Inches(6.8), Inches(1.55), m_w2, Inches(5.4))
    tf_m5 = m5.text_frame
    add_card_heading(tf_m5, "Module 5: Frontier AI Career Companion & Fact Vault", is_first=True, space_after=12)
    add_bullet(tf_m5, "Ultra-Fast Neural Inference", "Powered by Groq-accelerated LLaMA 3.3 70B delivering conversational responses under 800 ms.", space_after=10)
    add_bullet(tf_m5, "Personalized Career Coaching", "Performs tailored resume critiquing, skill gap analysis, and interactive mock technical interviews.", space_after=10)
    add_bullet(tf_m5, "Persistent Fact Memory Core", "Maintains long-term memory of student skills, GPA milestones, and career ambitions across sessions.", space_after=10)
    add_bullet(tf_m5, "Interactive Memory Fact Vault", "Empowers students to review, edit, or purge memories logged by the AI for full privacy control.", space_after=10)
    add_bullet(tf_m5, "Context-Aware Interview Prep", "Dynamically generates interview questions tailored to the student's actual enrolled courses.", space_after=6)

    # =========================================================================
    # SLIDE 9: SCREENSHOTS OF OUTPUT - PART 1 (AUTH & PROFILE)
    # =========================================================================
    slide9 = prs.slides.add_slide(blank_layout)
    add_header(slide9, "Screenshots: Authentication & Profile")

    sc_w = Inches(5.7)

    # Fig 6.1
    c_s1 = add_card(slide9, Inches(0.8), Inches(1.5), sc_w, Inches(5.5))
    tf_s1 = c_s1.text_frame
    add_card_heading(tf_s1, "Fig 6.1: Hybrid Authentication Modal", is_first=True, space_after=6, align_center=True)
    img61_path = "report_assets/auth_modal.png"
    if os.path.exists(img61_path):
        pic = slide9.shapes.add_picture(img61_path, Inches(1.0), Inches(2.05), width=Inches(5.3))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1)
    
    tb_s1_b = slide9.shapes.add_textbox(Inches(1.0), Inches(5.6), Inches(5.3), Inches(1.3))
    tf_s1_b = tb_s1_b.text_frame
    tf_s1_b.word_wrap = True
    tf_s1_b.margin_left = tf_s1_b.margin_top = tf_s1_b.margin_right = tf_s1_b.margin_bottom = 0
    add_bullet(tf_s1_b, "Dual Sign-In Pathways", "Provides one-click Google OAuth2 SSO and standard email/password authentication.", is_first=True, space_after=4)
    add_bullet(tf_s1_b, "Session Security", "Issues JWT Bearer tokens and directs users to customized role-based dashboards.", space_after=2)

    # Fig 6.2
    c_s2 = add_card(slide9, Inches(6.8), Inches(1.5), sc_w, Inches(5.5))
    tf_s2 = c_s2.text_frame
    add_card_heading(tf_s2, "Fig 6.2: Student Identity & Settings Modal", is_first=True, space_after=6, align_center=True)
    img62_path = "report_assets/settings_modal.png"
    if os.path.exists(img62_path):
        pic = slide9.shapes.add_picture(img62_path, Inches(7.0), Inches(2.05), width=Inches(5.3))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1)

    tb_s2_b = slide9.shapes.add_textbox(Inches(7.0), Inches(5.6), Inches(5.3), Inches(1.3))
    tf_s2_b = tb_s2_b.text_frame
    tf_s2_b.word_wrap = True
    tf_s2_b.margin_left = tf_s2_b.margin_top = tf_s2_b.margin_right = tf_s2_b.margin_bottom = 0
    add_bullet(tf_s2_b, "Profile Configuration", "Maintains student register number, degree specialization, and graduation year.", is_first=True, space_after=4)
    add_bullet(tf_s2_b, "Multi-Linked Accounts", "Enables linking institutional .edu emails to unlock university verified badges.", space_after=2)

    # =========================================================================
    # SLIDE 10: SCREENSHOTS OF OUTPUT - PART 2 (ACADEMIC VAULT & VERIFIER)
    # =========================================================================
    slide10 = prs.slides.add_slide(blank_layout)
    add_header(slide10, "Screenshots: Academic Vault & Verifier")

    # Fig 6.4
    c_s3 = add_card(slide10, Inches(0.8), Inches(1.5), sc_w, Inches(5.5))
    tf_s3 = c_s3.text_frame
    add_card_heading(tf_s3, "Fig 6.4: Grade Card Vault & Target Calculator", is_first=True, space_after=6, align_center=True)
    img64_path = "report_assets/academic_vault.png"
    if os.path.exists(img64_path):
        pic = slide10.shapes.add_picture(img64_path, Inches(1.0), Inches(2.05), width=Inches(5.3))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1)

    tb_s3_b = slide10.shapes.add_textbox(Inches(1.0), Inches(5.6), Inches(5.3), Inches(1.3))
    tf_s3_b = tb_s3_b.text_frame
    tf_s3_b.word_wrap = True
    tf_s3_b.margin_left = tf_s3_b.margin_top = tf_s3_b.margin_right = tf_s3_b.margin_bottom = 0
    add_bullet(tf_s3_b, "Tamper-Proof Grades", "Displays semester-wise course breakdown, credit totals, and live CGPA calculations.", is_first=True, space_after=4)
    add_bullet(tf_s3_b, "Target CGPA Planner", "Calculates required future semester grades to clear placement eligibility thresholds.", space_after=2)

    # Fig 6.5
    c_s4 = add_card(slide10, Inches(6.8), Inches(1.5), sc_w, Inches(5.5))
    tf_s4 = c_s4.text_frame
    add_card_heading(tf_s4, "Fig 6.5: 1-Click PDF Certificate Authenticator", is_first=True, space_after=6, align_center=True)
    img65_path = "report_assets/certificate_verifier.png"
    if os.path.exists(img65_path):
        pic = slide10.shapes.add_picture(img65_path, Inches(7.0), Inches(2.05), width=Inches(5.3))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1)

    tb_s4_b = slide10.shapes.add_textbox(Inches(7.0), Inches(5.6), Inches(5.3), Inches(1.3))
    tf_s4_b = tb_s4_b.text_frame
    tf_s4_b.word_wrap = True
    tf_s4_b.margin_left = tf_s4_b.margin_top = tf_s4_b.margin_right = tf_s4_b.margin_bottom = 0
    add_bullet(tf_s4_b, "Autonomous 3-Tier Check", "Features drag-and-drop PDF evaluation with instant sub-280ms execution latency.", is_first=True, space_after=4)
    add_bullet(tf_s4_b, "Detailed Audit Report", "Outputs granular pass/fail indicators for Domain, QR signature, and PDFBox metadata.", space_after=2)

    # =========================================================================
    # SLIDE 11: SCREENSHOTS OF OUTPUT - PART 3 (LANDING & JOB RADAR)
    # =========================================================================
    slide11 = prs.slides.add_slide(blank_layout)
    add_header(slide11, "Screenshots: Career Radar & Portal")

    # Fig 6.3
    c_s5 = add_card(slide11, Inches(0.8), Inches(1.5), sc_w, Inches(5.5))
    tf_s5 = c_s5.text_frame
    add_card_heading(tf_s5, "Fig 6.3: Universal Academic Landing Interface", is_first=True, space_after=6, align_center=True)
    img63_path = "report_assets/landing.png"
    if os.path.exists(img63_path):
        pic = slide11.shapes.add_picture(img63_path, Inches(1.0), Inches(2.05), width=Inches(5.3))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1)

    tb_s5_b = slide11.shapes.add_textbox(Inches(1.0), Inches(5.6), Inches(5.3), Inches(1.3))
    tf_s5_b = tb_s5_b.text_frame
    tf_s5_b.word_wrap = True
    tf_s5_b.margin_left = tf_s5_b.margin_top = tf_s5_b.margin_right = tf_s5_b.margin_bottom = 0
    add_bullet(tf_s5_b, "Centralized Navigation", "Provides single-click access across Grade Vault, Verifier, Job Radar, and AI Companion.", is_first=True, space_after=4)
    add_bullet(tf_s5_b, "Modern Responsive UI", "Engineered with dark-mode aesthetic, high contrast, and smooth fluid transitions.", space_after=2)

    # Fig 6.6
    c_s6 = add_card(slide11, Inches(6.8), Inches(1.5), sc_w, Inches(5.5))
    tf_s6 = c_s6.text_frame
    add_card_heading(tf_s6, "Fig 6.6: Pan-India District Job Radar", is_first=True, space_after=6, align_center=True)
    img66_path = "report_assets/job_scanner.png"
    if os.path.exists(img66_path):
        pic = slide11.shapes.add_picture(img66_path, Inches(7.0), Inches(2.05), width=Inches(5.3))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1)

    tb_s6_b = slide11.shapes.add_textbox(Inches(7.0), Inches(5.6), Inches(5.3), Inches(1.3))
    tf_s6_b = tb_s6_b.text_frame
    tf_s6_b.word_wrap = True
    tf_s6_b.margin_left = tf_s6_b.margin_top = tf_s6_b.margin_right = tf_s6_b.margin_bottom = 0
    add_bullet(tf_s6_b, "Targeted Career Discovery", "Filters employment listings by state, district, role category, and programming stack.", is_first=True, space_after=4)
    add_bullet(tf_s6_b, "Direct Application Tracking", "Enables students to apply directly and track their hiring lifecycle in real time.", space_after=2)

    # =========================================================================
    # SLIDE 12: SCREENSHOTS OF OUTPUT - PART 4 (FRONTIER AI & MEMORY)
    # =========================================================================
    slide12 = prs.slides.add_slide(blank_layout)
    add_header(slide12, "Screenshots: Frontier AI Companion")

    # Fig 6.7
    c_s7 = add_card(slide12, Inches(0.8), Inches(1.5), sc_w, Inches(5.5))
    tf_s7 = c_s7.text_frame
    add_card_heading(tf_s7, "Fig 6.7: AI Career Companion Workspace", is_first=True, space_after=6, align_center=True)
    img67_path = "report_assets/ai_companion.png"
    if os.path.exists(img67_path):
        pic = slide12.shapes.add_picture(img67_path, Inches(1.0), Inches(2.05), width=Inches(5.3))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1)

    tb_s7_b = slide12.shapes.add_textbox(Inches(1.0), Inches(5.6), Inches(5.3), Inches(1.3))
    tf_s7_b = tb_s7_b.text_frame
    tf_s7_b.word_wrap = True
    tf_s7_b.margin_left = tf_s7_b.margin_top = tf_s7_b.margin_right = tf_s7_b.margin_bottom = 0
    add_bullet(tf_s7_b, "Conversational AI Mentor", "Provides resume feedback, technical interview drills, and academic advice.", is_first=True, space_after=4)
    add_bullet(tf_s7_b, "Lightning-Fast Execution", "Powered by Groq-accelerated LLaMA 3.3 70B responding in under 800 ms.", space_after=2)

    # Fig 6.8
    c_s8 = add_card(slide12, Inches(6.8), Inches(1.5), sc_w, Inches(5.5))
    tf_s8 = c_s8.text_frame
    add_card_heading(tf_s8, "Fig 6.8: Hestia Memory Core & Fact Vault", is_first=True, space_after=6, align_center=True)
    img68_path = "report_assets/memory_modal.png"
    if os.path.exists(img68_path):
        pic = slide12.shapes.add_picture(img68_path, Inches(7.0), Inches(2.05), width=Inches(5.3))
        pic.line.color.rgb = COLOR_CARD_BORDER
        pic.line.width = Pt(1)

    tb_s8_b = slide12.shapes.add_textbox(Inches(7.0), Inches(5.6), Inches(5.3), Inches(1.3))
    tf_s8_b = tb_s8_b.text_frame
    tf_s8_b.word_wrap = True
    tf_s8_b.margin_left = tf_s8_b.margin_top = tf_s8_b.margin_right = tf_s8_b.margin_bottom = 0
    add_bullet(tf_s8_b, "Persistent Student Memory", "Stores extracted academic goals, technical skills, and past advice across sessions.", is_first=True, space_after=4)
    add_bullet(tf_s8_b, "User Privacy & Governance", "Allows students to inspect, search, and purge logged facts for complete control.", space_after=2)

    # =========================================================================
    # SLIDE 13: SAMPLE SOURCE CODE
    # =========================================================================
    slide13 = prs.slides.add_slide(blank_layout)
    add_header(slide13, "Sample Source Code")

    # Left: Code Block Box
    code_card = add_card(slide13, Inches(0.8), Inches(1.55), Inches(6.6), Inches(5.4), bg_color=COLOR_CODE_BG)
    tf_code = code_card.text_frame
    add_card_heading(tf_code, "MultiTierVerificationFacade.java (OOP Facade Pattern)", is_first=True, space_after=6)

    sample_code_lines = [
        "@Component",
        "public class MultiTierVerificationFacade {",
        "    private final EduEmailVerifier tier1Verifier;",
        "    private final QRCodeSignatureVerifier tier2Verifier;",
        "    private final PdfForensicStreamInspector tier3Inspector;",
        "",
        "    public VerificationResult evaluateCertificate(",
        "            MultipartFile pdfFile, String studentEmail) {",
        "        VerificationResult result = new VerificationResult();",
        "        // Tier 1: Institutional Academic Domain Check",
        "        result.setTier1Valid(tier1Verifier.isValidDomain(studentEmail));",
        "        // Tier 2: ZXing QR Matrix & PKI Signature Verification",
        "        BufferedImage img = renderPdfPage(pdfFile);",
        "        result.setTier2Valid(tier2Verifier.verifySignature(img));",
        "        // Tier 3: Apache PDFBox Forensic Stream Inspection",
        "        result.setTier3Valid(tier3Inspector.inspectMetadata(pdfFile));",
        "        return result;",
        "    }",
        "}"
    ]

    p_c = tf_code.add_paragraph()
    p_c.space_before = Pt(2)
    p_c.space_after = Pt(2)
    p_c.line_spacing = 1.05
    for line in sample_code_lines:
        r_line = p_c.add_run()
        r_line.text = line + "\n"
        r_line.font.name = "Consolas"
        r_line.font.size = Pt(9.5)
        r_line.font.bold = False
        r_line.font.color.rgb = COLOR_CODE_TEXT

    # Right: Architectural Highlights
    code_exp = add_card(slide13, Inches(7.6), Inches(1.55), Inches(4.9), Inches(5.4))
    tf_exp = code_exp.text_frame
    add_card_heading(tf_exp, "OOP Architecture & Design Highlights", is_first=True, space_after=12)
    add_bullet(tf_exp, "Facade Design Pattern", "Encapsulates complex multi-stage verification workflows behind a simple, unified interface.", space_after=10)
    add_bullet(tf_exp, "Inversion of Control (IoC)", "Leverages Spring @Component and constructor-based dependency injection for high testability.", space_after=10)
    add_bullet(tf_exp, "Single Responsibility Principle", "Each tier verifier maintains strict separation of concerns, isolating domain, QR, and forensic logic.", space_after=10)
    add_bullet(tf_exp, "High-Throughput Execution", "Processes all three verification tiers in parallel pipelines, completing evaluation in 276.2 ms.", space_after=10)
    add_bullet(tf_exp, "Robust Error Boundary", "Isolates malformed or corrupted PDF byte streams gracefully without halting application threads.", space_after=6)

    # =========================================================================
    # SLIDE 14: CONCLUSION
    # =========================================================================
    slide14 = prs.slides.add_slide(blank_layout)
    add_header(slide14, "Conclusion")

    c_con1 = add_card(slide14, Inches(0.8), Inches(1.55), Inches(5.7), Inches(5.4))
    tf_con1 = c_con1.text_frame
    add_card_heading(tf_con1, "Project Milestones & Deliverables Achieved", is_first=True, space_after=12)
    add_bullet(tf_con1, "Fully Functional Unified Platform", "Delivered an enterprise-grade academic verification, grade management, and career system.", space_after=10)
    add_bullet(tf_con1, "Production Cloud Deployment", "Successfully deployed on Render Cloud (https://hestia-kw5k.onrender.com) with PostgreSQL 16.", space_after=10)
    add_bullet(tf_con1, "Five Integrated Microservices", "Engineered and verified AuthService, VerificationFacade, AcademicVault, JobRadar, and FrontierAI.", space_after=10)
    add_bullet(tf_con1, "Empirical Validation Complete", "Conducted extensive testing with real-world student transcripts, fraudulent PDFs, and recruiter mocks.", space_after=10)
    add_bullet(tf_con1, "Robust OOP Architecture", "Engineered modular Java 21 / Spring Boot code adhering strictly to SOLID design principles.", space_after=6)

    c_con2 = add_card(slide14, Inches(6.8), Inches(1.55), Inches(5.7), Inches(5.4))
    tf_con2 = c_con2.text_frame
    add_card_heading(tf_con2, "System Performance & Placement Impact", is_first=True, space_after=12)
    add_bullet(tf_con2, "100% Tamper Detection Rate", "Successfully intercepted 100% of forged and modified certificates with zero false passes.", space_after=10)
    add_bullet(tf_con2, "276.2 ms Verification Speed", "Eliminated weeks of manual verification lag, achieving instantaneous 1-click audit turnaround.", space_after=10)
    add_bullet(tf_con2, "Restored Institutional Trust", "Provides recruiters with tamper-evident cryptographic assurance of student academic merits.", space_after=10)
    add_bullet(tf_con2, "Proactive Student Empowerment", "Equipped students with target CGPA calculators and personalized neural AI career mentorship.", space_after=10)
    add_bullet(tf_con2, "Elimination of Tool Fragmentation", "Unified all campus hiring and academic management workflows into a single cohesive hearth.", space_after=6)

    # =========================================================================
    # SLIDE 15: FUTURE SCOPE
    # =========================================================================
    slide15 = prs.slides.add_slide(blank_layout)
    add_header(slide15, "Future Scope")

    fut1 = add_card(slide15, Inches(0.8), Inches(1.55), Inches(5.7), Inches(5.4))
    tf_fut1 = fut1.text_frame
    add_card_heading(tf_fut1, "Decentralized Security & Mobile Ecosystem", is_first=True, space_after=12)
    add_bullet(tf_fut1, "Decentralized Blockchain Ledger Anchoring", "Recording SHA-256 certificate hashes on Ethereum/Polygon smart contracts for immutable global verification.", space_after=10)
    add_bullet(tf_fut1, "Trustless Cross-Border Verification", "Allows international universities and overseas employers to verify credentials without contacting college.", space_after=10)
    add_bullet(tf_fut1, "Native Cross-Platform Mobile Application", "Building companion Flutter/Android apps for instant camera-based QR scanning and credential checks.", space_after=10)
    add_bullet(tf_fut1, "Instant Mobile Recruitment Alerts", "Delivers automated push notifications for upcoming campus drives and application status updates.", space_after=6)

    fut2 = add_card(slide15, Inches(6.8), Inches(1.55), Inches(5.7), Inches(5.4))
    tf_fut2 = fut2.text_frame
    add_card_heading(tf_fut2, "National Depository & AI Interview Extensions", is_first=True, space_after=12)
    add_bullet(tf_fut2, "National Academic Bank of Credits (ABC / APAAR)", "Direct API synchronization with Government of India DigiLocker and APAAR academic repositories.", space_after=10)
    add_bullet(tf_fut2, "Voice-Enabled AI Mock Interviews", "Integrating OpenAI Whisper speech-to-text and low-latency neural TTS for verbal technical simulations.", space_after=10)
    add_bullet(tf_fut2, "Automated Coding Sandbox Assessment", "Embedding isolated Docker containers for real-time algorithmic coding tests during placement rounds.", space_after=10)
    add_bullet(tf_fut2, "Predictive Placement Analytics", "Machine learning models forecasting student placement probabilities based on historical hiring trends.", space_after=6)

    # =========================================================================
    # SLIDE 16: REFERENCES
    # =========================================================================
    slide16 = prs.slides.add_slide(blank_layout)
    add_header(slide16, "References")

    ref_c = add_card(slide16, Inches(0.8), Inches(1.55), Inches(11.733), Inches(5.4))
    tf_ref = ref_c.text_frame
    add_card_heading(tf_ref, "Frameworks, Technical Standards & Academic Literature", is_first=True, space_after=12)
    add_bullet(tf_ref, "Spring Boot Framework", "VMware Tanzu. Spring Boot Reference Documentation (v3.x / 4.x). Official Portal: https://spring.io/projects/spring-boot", space_after=9)
    add_bullet(tf_ref, "Apache PDFBox Library", "The Apache Software Foundation. Apache PDFBox: An Open Source Java Tool for Working with PDF Documents. https://pdfbox.apache.org", space_after=9)
    add_bullet(tf_ref, "ZXing Image Processing", "ZXing Team. Multi-format 1D/2D Barcode Image Processing Library for Java. GitHub: https://github.com/zxing/zxing", space_after=9)
    add_bullet(tf_ref, "OOP Design Patterns", "Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). Design Patterns: Elements of Reusable Object-Oriented Software. Addison-Wesley.", space_after=9)
    add_bullet(tf_ref, "National Academic Depository (NAD)", "Ministry of Education & DigiLocker, Government of India. Academic Depository Framework Guidelines & Verification APIs.", space_after=9)
    add_bullet(tf_ref, "Groq AI Cloud Platform", "Groq Inc. LLaMA 3.3 70B Versatile Language Model Inference API & Low-Latency LLM Serving Architecture. https://groq.com", space_after=9)
    add_bullet(tf_ref, "PostgreSQL Enterprise Database", "The PostgreSQL Global Development Group. PostgreSQL 16 Relational Database System Documentation. https://www.postgresql.org/docs/16/", space_after=6)

    # =========================================================================
    # SLIDE 17: THANK YOU SLIDE
    # =========================================================================
    slide17 = prs.slides.add_slide(blank_layout)
    set_white_bg(slide17)

    # Top border line
    t_line17 = slide17.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(0.5), Inches(11.733), Pt(2))
    t_line17.fill.solid()
    t_line17.fill.fore_color.rgb = COLOR_HEADING
    t_line17.line.fill.background()

    # Center Logo
    if os.path.exists(logo_path):
        slide17.shapes.add_picture(logo_path, Inches(5.866), Inches(1.1), width=Inches(1.6))

    # Center Title
    tb_ty = slide17.shapes.add_textbox(Inches(0.8), Inches(2.9), Inches(11.733), Inches(1.2))
    tf_ty = tb_ty.text_frame
    tf_ty.word_wrap = True
    p_ty = tf_ty.paragraphs[0]
    p_ty.alignment = PP_ALIGN.CENTER
    r = p_ty.add_run()
    r.text = "Thank You!"
    r.font.name = FONT_FAMILY
    r.font.size = Pt(32)
    r.font.bold = True
    r.font.color.rgb = COLOR_TITLE

    p_subty = tf_ty.add_paragraph()
    p_subty.alignment = PP_ALIGN.CENTER
    p_subty.space_before = Pt(4)
    r = p_subty.add_run()
    r.text = "Questions & Technical Discussion"
    r.font.name = FONT_FAMILY
    r.font.size = Pt(14)
    r.font.bold = True
    r.font.color.rgb = COLOR_HEADING

    # Bottom Contact Card
    c_ty = add_card(slide17, Inches(2.8), Inches(4.3), Inches(7.733), Inches(2.5))
    tf_cty = c_ty.text_frame
    add_card_heading(tf_cty, "Project Access & Verification Links", is_first=True, space_after=8, align_center=True)
    add_bullet(tf_cty, "Live Platform Deployment", "https://hestia-kw5k.onrender.com  |  Active Production Instance", space_after=6)
    add_bullet(tf_cty, "Source Code Repository", "https://github.com/Delta-rune/Hestia  |  Full Java & Web Monorepo", space_after=6)
    add_bullet(tf_cty, "Project Team Inquiries", "hestia.paranoia@gmail.com  |  Group 11 (Nripan Satheesh & Muhammed Afrithkhan)", space_after=4)

    # Save presentation
    output_pptx = "Hestia_Presentation.pptx"
    prs.save(output_pptx)
    print(f"Presentation saved successfully as {output_pptx} with {len(prs.slides)} slides!")

    # Also update hes.pptx so both files have the new version
    prs.save("hes.pptx")
    print("Also updated hes.pptx!")

    # Sync to Downloads folder
    downloads_dir = r"C:\Users\zyber\Downloads"
    if os.path.exists(downloads_dir):
        shutil.copyfile(output_pptx, os.path.join(downloads_dir, output_pptx))
        shutil.copyfile("hes.pptx", os.path.join(downloads_dir, "hes.pptx"))
        print(f"Synced {output_pptx} and hes.pptx to {downloads_dir}!")

if __name__ == "__main__":
    create_deck()
