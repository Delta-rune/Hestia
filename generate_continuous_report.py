import subprocess
import pymupdf
import re
import os
import shutil

print("Step 1: Extracting clean base assets...")
res = subprocess.run(["git", "show", "6959197:project_report_hestia.html"], capture_output=True, text=True, encoding='utf-8')
orig_html = res.stdout
imgs = re.findall(r'<img[^>]+src="(data:image/[^">]+)"', orig_html)
college_logo = imgs[0]
fig31_img = imgs[2]    # Architecture diagram
fig32_img = imgs[3]    # DB Schema diagram
fig61_img = imgs[4]    # Screenshot 6.1
fig62_img = imgs[5]    # Screenshot 6.2
fig63_img = imgs[6]    # Screenshot 6.3
fig64_img = imgs[7]    # Screenshot 6.4
fig65_img = imgs[8]    # Screenshot 6.5
fig66_img = imgs[9]    # Screenshot 6.6
fig67_img = imgs[10]   # Screenshot 6.7
fig68_img = imgs[11]   # Screenshot 6.8

# Extract front matter
pages_orig = re.findall(r'<!-- =+\s*PAGE (\d+):?\s*([^=]*?)\s*=+ -->(.*?)(?=<!-- =+\s*PAGE|\Z)', orig_html, re.DOTALL)
p01_cover = pages_orig[0][2].strip().replace("MICRO PROJECT REPORT", "MICRO-PROJECT REPORT")
p02_cert = pages_orig[1][2].strip().replace("Micro Project Guide:", "Project Guide:").replace("Micro Project Coordinator:", "Project Coordinator:").replace("macro project", "micro project")
p03_decl = pages_orig[2][2].strip().replace("this report", "this micro project report")
p04_ack = pages_orig[3][2].strip()
p05_vis = pages_orig[4][2].strip()
p06_pso = pages_orig[5][2].strip()
p07_po = pages_orig[6][2].strip().replace('font-size: 10pt;', 'font-size: 9.5pt; line-height: 1.32;').replace('margin-bottom: 1.2mm;', 'margin-bottom: 0.8mm;')
p08_abs = pages_orig[7][2].strip()
p13_abbr = pages_orig[12][2].strip()

def clean_page(p_html):
    p_html = re.sub(r'^\s*<div class="page[^"]*">\s*', '', p_html)
    p_html = re.sub(r'\s*</div>\s*$', '', p_html)
    return p_html

# Read body pages from generate_final_report.py
with open('generate_final_report.py', 'r', encoding='utf-8') as f:
    gen_code = f.read()

bp_dict = {}
for i in range(1, 24):
    pat = rf'body_p{i}\s*=\s*(?:f?"""|""")\s*<div class="page[^"]*">\s*(.*?)\s*<div class="page-footer">{i}</div>\s*</div>\s*"""'
    m = re.search(pat, gen_code, re.DOTALL)
    if m:
        bp_dict[i] = m.group(1).strip()

# Interpolate images
for k in bp_dict:
    bp_dict[k] = bp_dict[k].replace('{fig31_img}', fig31_img).replace('{fig32_img}', fig32_img)
    bp_dict[k] = bp_dict[k].replace('{fig61_img}', fig61_img).replace('{fig62_img}', fig62_img)
    bp_dict[k] = bp_dict[k].replace('{fig63_img}', fig63_img).replace('{fig64_img}', fig64_img)
    bp_dict[k] = bp_dict[k].replace('{fig65_img}', fig65_img).replace('{fig66_img}', fig66_img)
    bp_dict[k] = bp_dict[k].replace('{fig67_img}', fig67_img).replace('{fig68_img}', fig68_img)

# Combine chapters into continuous strips!
ch1 = f"""<div class="chapter-flow" id="ch1">
{bp_dict[1]}

{bp_dict[2]}
</div>"""

ch2 = f"""<div class="chapter-flow" id="ch2">
{bp_dict[3]}

{bp_dict[4]}

{bp_dict[5]}
</div>"""

ch3 = f"""<div class="chapter-flow" id="ch3">
{bp_dict[6]}

{bp_dict[7]}

{bp_dict[8]}

{bp_dict[9]}

{bp_dict[10]}
</div>"""

ch4 = f"""<div class="chapter-flow" id="ch4">
{bp_dict[11]}

{bp_dict[12]}

{bp_dict[13]}
</div>"""

ch5 = f"""<div class="chapter-flow" id="ch5">
{bp_dict[14]}
</div>"""

ch6 = f"""<div class="chapter-flow" id="ch6">
{bp_dict[15]}
<div style="page-break-before: always; break-before: page;"></div>
{bp_dict[16]}
<div style="page-break-before: always; break-before: page;"></div>
{bp_dict[17]}
<div style="page-break-before: always; break-before: page;"></div>
{bp_dict[18]}
</div>"""

ch7 = f"""<div class="chapter-flow" id="ch7">
{bp_dict[19]}

{bp_dict[20]}

{bp_dict[21]}

{bp_dict[22]}
</div>"""

ch_ref = f"""<div class="chapter-flow" id="ch_ref">
{bp_dict[23]}
</div>"""

# Build Table of Contents with Exact Detected Continuous Page Numbers
p09_toc1 = """<div class="center-title" style="margin-top: 4mm; margin-bottom: 5mm; font-size: 14pt; letter-spacing: 1.2px;">
  LIST OF CONTENTS
</div>

<table class="toc-table">
  <thead>
    <tr style="font-weight: bold; font-size: 12pt;">
      <td style="width: 15%;">SL.NO</td>
      <td style="width: 73%;">CONTENTS</td>
      <td style="width: 12%; text-align: right;">PAGE.NO</td>
    </tr>
    <tr><td colspan="3" style="height: 3mm;"></td></tr>
  </thead>
  <tbody style="font-size: 11pt;">
    <tr><td></td><td style="font-weight: bold;">ABSTRACT</td><td style="text-align: right;"></td></tr>
    <tr><td></td><td style="font-weight: bold;">LIST OF FIGURES</td><td style="text-align: right;"></td></tr>
    <tr><td></td><td style="font-weight: bold;">LIST OF TABLES</td><td style="text-align: right;"></td></tr>
    <tr><td></td><td style="font-weight: bold;">LIST OF ABBREVIATIONS</td><td style="text-align: right;"></td></tr>
    <tr><td colspan="3" style="height: 2mm;"></td></tr>

    <tr><td style="font-weight: bold;">1</td><td style="font-weight: bold;">INTRODUCTION</td><td style="text-align: right; font-weight: bold;">1</td></tr>
    <tr><td>1.1</td><td style="padding-left: 4mm;">Overview</td><td style="text-align: right;">1</td></tr>
    <tr><td>1.2</td><td style="padding-left: 4mm;">Motivation</td><td style="text-align: right;">1</td></tr>
    <tr><td>1.3</td><td style="padding-left: 4mm;">Objectives</td><td style="text-align: right;">1</td></tr>
    <tr><td colspan="3" style="height: 2mm;"></td></tr>

    <tr><td style="font-weight: bold;">2</td><td style="font-weight: bold;">LITERATURE SURVEY</td><td style="text-align: right; font-weight: bold;">3</td></tr>
    <tr><td>2.1</td><td style="padding-left: 4mm;">Evolution of Placement &amp; Academic Career Platforms</td><td style="text-align: right;">3</td></tr>
    <tr><td>2.2</td><td style="padding-left: 4mm;">Modern Desktop vs Web-Based Educational Portals</td><td style="text-align: right;">3</td></tr>
    <tr><td>2.3</td><td style="padding-left: 4mm;">Automated Academic Verification &amp; Multi-Tier Validation</td><td style="text-align: right;">3</td></tr>
    <tr><td>2.4</td><td style="padding-left: 4mm;">LLM-Driven Autonomous Mentorship &amp; Memory Persistence</td><td style="text-align: right;">4</td></tr>
    <tr><td>2.5</td><td style="padding-left: 4mm;">Gaps &amp; Vulnerabilities in Current Institutional Systems</td><td style="text-align: right;">4</td></tr>
    <tr><td>2.6</td><td style="padding-left: 4mm;">Summary &amp; Positioning of Proposed System</td><td style="text-align: right;">5</td></tr>
    <tr><td colspan="3" style="height: 2mm;"></td></tr>

    <tr><td style="font-weight: bold;">3</td><td style="font-weight: bold;">SYSTEM ARCHITECTURE &amp; METHODOLOGY</td><td style="text-align: right; font-weight: bold;">6</td></tr>
    <tr><td>3.1</td><td style="padding-left: 4mm;">Problem Description</td><td style="text-align: right;">6</td></tr>
    <tr><td>3.2</td><td style="padding-left: 4mm;">Methodology &amp; Architectural Principles</td><td style="text-align: right;">6</td></tr>
    <tr><td>3.3</td><td style="padding-left: 4mm;">Proposed System Architecture</td><td style="text-align: right;">6</td></tr>
    <tr><td>3.4</td><td style="padding-left: 4mm;">System Architecture Diagram</td><td style="text-align: right;">7</td></tr>
    <tr><td>3.5</td><td style="padding-left: 4mm;">Conceptual Relational Database Design</td><td style="text-align: right;">7</td></tr>
    <tr><td>3.6</td><td style="padding-left: 4mm;">Strength of the Proposed System</td><td style="text-align: right;">8</td></tr>
    <tr><td>3.7</td><td style="padding-left: 4mm;">Implementation of System Modules</td><td style="text-align: right;">8</td></tr>
    <tr><td>3.8</td><td style="padding-left: 4mm;">Systems Testing &amp; Validation</td><td style="text-align: right;">8</td></tr>
  </tbody>
</table>"""

p10_toc2 = """<table class="toc-table" style="margin-top: 4mm;">
  <thead>
    <tr style="font-weight: bold; font-size: 12pt;">
      <td style="width: 15%;">SL.NO</td>
      <td style="width: 73%;">CONTENTS</td>
      <td style="width: 12%; text-align: right;">PAGE.NO</td>
    </tr>
    <tr><td colspan="3" style="height: 3mm;"></td></tr>
  </thead>
  <tbody style="font-size: 11pt;">
    <tr><td>3.9</td><td style="padding-left: 4mm;">Software &amp; Hardware Requirements</td><td style="text-align: right;">9</td></tr>
    <tr><td>3.10</td><td style="padding-left: 4mm;">System Deployment &amp; Access Protocol</td><td style="text-align: right;">9</td></tr>
    <tr><td>3.11</td><td style="padding-left: 4mm;">Maintenance &amp; Fault Tolerance</td><td style="text-align: right;">9</td></tr>
    <tr><td colspan="3" style="height: 2mm;"></td></tr>

    <tr><td style="font-weight: bold;">4</td><td style="font-weight: bold;">RESULT AND DISCUSSION</td><td style="text-align: right; font-weight: bold;">10</td></tr>
    <tr><td>4.1</td><td style="padding-left: 4mm;">Executive Placement Cell Workspace (Admin View)</td><td style="text-align: right;">10</td></tr>
    <tr><td>4.2</td><td style="padding-left: 4mm;">Candidate Career Workspace (Student View)</td><td style="text-align: right;">10</td></tr>
    <tr><td>4.3</td><td style="padding-left: 4mm;">System Testing Results &amp; Performance Discussion</td><td style="text-align: right;">10</td></tr>
    <tr><td>4.4</td><td style="padding-left: 4mm;">Comparative Analysis with Traditional Portals</td><td style="text-align: right;">11</td></tr>
    <tr><td colspan="3" style="height: 2mm;"></td></tr>

    <tr><td style="font-weight: bold;">5</td><td style="font-weight: bold;">CONCLUSION AND FUTURE SCOPE</td><td style="text-align: right; font-weight: bold;">12</td></tr>
    <tr><td>5.1</td><td style="padding-left: 4mm;">Conclusion</td><td style="text-align: right;">12</td></tr>
    <tr><td>5.2</td><td style="padding-left: 4mm;">Scope of Future Works</td><td style="text-align: right;">12</td></tr>
    <tr><td colspan="3" style="height: 2mm;"></td></tr>

    <tr><td style="font-weight: bold;">6</td><td style="font-weight: bold;">SCREENSHOTS</td><td style="text-align: right; font-weight: bold;">13</td></tr>
    <tr><td colspan="3" style="height: 2mm;"></td></tr>

    <tr><td style="font-weight: bold;">7</td><td style="font-weight: bold;">SOURCE CODE</td><td style="text-align: right; font-weight: bold;">17</td></tr>
    <tr><td colspan="3" style="height: 2mm;"></td></tr>

    <tr><td></td><td style="font-weight: bold;">REFERENCES</td><td style="text-align: right; font-weight: bold;">20</td></tr>
  </tbody>
</table>"""

p11_lof = """<div class="center-title" style="margin-top: 4mm; margin-bottom: 5mm; font-size: 14pt; letter-spacing: 1.2px;">
  LIST OF FIGURES
</div>

<table class="toc-table">
  <thead>
    <tr style="font-weight: bold; font-size: 12pt;">
      <td style="width: 15%;">Fig No.</td>
      <td style="width: 73%;">Title</td>
      <td style="width: 12%; text-align: right;">Page No.</td>
    </tr>
    <tr><td colspan="3" style="height: 3mm;"></td></tr>
  </thead>
  <tbody style="font-size: 11pt;">
    <tr><td>Fig 3.1</td><td>Enterprise Architecture of Hestia Platform</td><td style="text-align: right;">7</td></tr>
    <tr><td>Fig 3.2</td><td>Relational Database Schema &amp; Entity Relationships</td><td style="text-align: right;">8</td></tr>
    <tr><td>Fig 6.1</td><td>Hybrid Authentication Modal (Google SSO &amp; Password Sync)</td><td style="text-align: right;">13</td></tr>
    <tr><td>Fig 6.2</td><td>Student Identity Profile &amp; Multi-Linked Email Settings Modal</td><td style="text-align: right;">13</td></tr>
    <tr><td>Fig 6.3</td><td>Universal Academic Landing Interface &amp; Feature Navigation</td><td style="text-align: right;">14</td></tr>
    <tr><td>Fig 6.4</td><td>Academic Grade Card Vault &amp; Target CGPA Calculator</td><td style="text-align: right;">14</td></tr>
    <tr><td>Fig 6.5</td><td>1-Click PDF Certificate Authenticator &amp; QR Validation Interface</td><td style="text-align: right;">15</td></tr>
    <tr><td>Fig 6.6</td><td>Pan-India District Job Radar &amp; LPA Salary Matching</td><td style="text-align: right;">15</td></tr>
    <tr><td>Fig 6.7</td><td>Hestia Frontier AI Career Companion &amp; Neural Chatbot</td><td style="text-align: right;">16</td></tr>
    <tr><td>Fig 6.8</td><td>Hestia Memory Core Modal &amp; Extracted Fact Vault</td><td style="text-align: right;">16</td></tr>
  </tbody>
</table>"""

p12_lot = """<div class="center-title" style="margin-top: 4mm; margin-bottom: 5mm; font-size: 14pt; letter-spacing: 1.2px;">
  LIST OF TABLES
</div>

<table class="toc-table">
  <thead>
    <tr style="font-weight: bold; font-size: 12pt;">
      <td style="width: 18%;">Table No.</td>
      <td style="width: 70%;">Title</td>
      <td style="width: 12%; text-align: right;">Page No.</td>
    </tr>
    <tr><td colspan="3" style="height: 3mm;"></td></tr>
  </thead>
  <tbody style="font-size: 11pt;">
    <tr><td>Table 3.1</td><td>Hardware and Software Requirements Specification</td><td style="text-align: right;">9</td></tr>
    <tr><td>Table 4.1</td><td>Multi-Tier Verification &amp; System Performance Metrics</td><td style="text-align: right;">10</td></tr>
  </tbody>
</table>"""

front_pages_html = f"""
<div class="fm-section">{clean_page(p01_cover)}</div>
<div class="fm-section">{clean_page(p02_cert)}</div>
<div class="fm-section">{clean_page(p03_decl)}</div>
<div class="fm-section">{clean_page(p04_ack)}</div>
<div class="fm-section">{clean_page(p05_vis)}</div>
<div class="fm-section">{clean_page(p06_pso)}</div>
<div class="fm-section">{clean_page(p07_po)}</div>
<div class="fm-section">{clean_page(p08_abs)}</div>
<div class="fm-section">{p09_toc1}</div>
<div class="fm-section">{p10_toc2}</div>
<div class="fm-section">{p11_lof}</div>
<div class="fm-section">{p12_lot}</div>
<div class="fm-section">{clean_page(p13_abbr)}</div>
"""

full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>HESTIA - MICRO-PROJECT REPORT</title>
<style>
  @page {{
    size: A4 portrait;
    margin: 20mm 20mm 20mm 25mm;
  }}

  * {{
    box-sizing: border-box;
    margin: 0;
    padding: 0;
    font-family: 'Times New Roman', Times, serif;
  }}

  body {{
    font-size: 12pt;
    line-height: 1.5;
    text-align: justify;
    color: #000000;
  }}

  .fm-section {{
    page-break-before: always;
    break-before: page;
  }}
  .fm-section:first-child {{
    page-break-before: avoid;
    break-before: avoid;
  }}

  .chapter-flow {{
    page-break-before: always;
    break-before: page;
  }}

  h1.chapter-num {{
    text-align: center;
    font-size: 14pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-top: 4mm;
    margin-bottom: 2mm;
    letter-spacing: 0.5px;
    page-break-after: avoid;
    break-after: avoid;
  }}

  h1.chapter-title {{
    text-align: center;
    font-size: 14pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-bottom: 4mm;
    letter-spacing: 0.5px;
    page-break-after: avoid;
    break-after: avoid;
  }}

  .center-title {{
    text-align: center;
    font-size: 14pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-top: 3mm;
    margin-bottom: 4mm;
    letter-spacing: 0.8px;
    page-break-after: avoid;
    break-after: avoid;
  }}

  h2.section-title {{
    font-size: 12pt;
    font-weight: bold;
    margin-top: 3.5mm;
    margin-bottom: 1.8mm;
    page-break-after: avoid;
    break-after: avoid;
  }}

  h3.subsection-title {{
    font-size: 12pt;
    font-weight: bold;
    margin-top: 3mm;
    margin-bottom: 1.5mm;
    page-break-after: avoid;
    break-after: avoid;
  }}

  p {{
    font-size: 12pt;
    line-height: 1.5;
    text-indent: 6mm;
    margin-bottom: 2mm;
    text-align: justify;
  }}

  p.no-indent {{
    text-indent: 0;
  }}

  /* Title / Cover Page */
  .title-page-border {{
    border: none;
    padding: 0;
    text-align: center;
    height: 250mm;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
  }}

  .project-title {{
    font-size: 26pt;
    font-weight: bold;
    text-transform: uppercase;
    letter-spacing: 2px;
    margin-bottom: 3.5mm;
  }}

  .student-list {{
    font-size: 13.5pt;
    font-weight: bold;
    line-height: 1.6;
  }}

  /* Tables */
  table.report-table {{
    width: 100%;
    border-collapse: collapse;
    margin: 2.5mm 0;
    font-size: 10pt;
    page-break-inside: avoid;
    break-inside: avoid;
  }}

  table.report-table th, table.report-table td {{
    border: 1px solid #333333;
    padding: 1.8mm 2.5mm;
    text-align: left;
    vertical-align: middle;
  }}

  table.report-table th {{
    background-color: #f2f2f2;
    font-weight: bold;
    text-align: center;
  }}

  /* Table of Contents Table */
  table.toc-table {{
    width: 100%;
    border-collapse: collapse;
    font-size: 11pt;
    line-height: 1.5;
  }}

  table.toc-table td {{
    padding: 1.2mm 0;
    vertical-align: bottom;
  }}

  /* Code Block Styling */
  pre.code-block {{
    background: #f8f9fa;
    border: 1px solid #dcdcdc;
    border-radius: 4px;
    padding: 1.8mm 3mm;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 8.0pt;
    line-height: 1.24;
    white-space: pre-wrap;
    word-break: break-word;
    margin: 1.5mm 0;
    page-break-inside: auto;
    break-inside: auto;
  }}

  /* Figure Styling */
  .figure-container {{
    text-align: center;
    margin: 2.5mm 0;
    page-break-inside: avoid;
    break-inside: avoid;
  }}

  .figure-container img {{
    width: 88%;
    max-width: 140mm;
    height: auto;
    border: 1px solid #333333;
    border-radius: 2px;
    display: block;
    margin: 0 auto;
  }}

  .figure-caption {{
    font-size: 11pt;
    font-weight: bold;
    margin-top: 1.5mm;
    text-align: center;
  }}

  ol, ul {{
    margin-left: 7mm;
    margin-bottom: 2mm;
  }}

  li {{
    font-size: 12pt;
    margin-bottom: 1.2mm;
    line-height: 1.5;
  }}

  .ref-container p {{
    font-size: 10pt !important;
    line-height: 1.32 !important;
    margin-bottom: 1.2mm !important;
  }}
</style>
</head>
<body>

{front_pages_html}

{ch1}
{ch2}
{ch3}
{ch4}
{ch5}
{ch6}
{ch7}
{ch_ref}

</body>
</html>
"""

# Clean quotes safely
full_html = full_html.replace('\u201c', '"').replace('\u201d', '"').replace('\u2018', "'").replace('\u2019', "'").replace('\ufffd', '"')

# Save HTML files
with open('project_report_36pages.html', 'w', encoding='utf-8') as f:
    f.write(full_html)

with open('project_report_hestia.html', 'w', encoding='utf-8') as f:
    f.write(full_html)

print("Saved project_report_36pages.html and project_report_hestia.html successfully!")

# Step 2: Render with Edge headless
temp_pdf = r"e:\Hestia\temp_master.pdf"
if os.path.exists(temp_pdf):
    try:
        os.remove(temp_pdf)
    except:
        pass

edge_exe = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
cmd = [
    edge_exe,
    "--headless=new",
    "--disable-gpu",
    "--no-pdf-header-footer",
    f"--print-to-pdf={temp_pdf}",
    "file:///e:/Hestia/project_report_36pages.html"
]

print("Rendering with Edge headless...")
res = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
print("Edge returncode:", res.returncode)

doc = pymupdf.open(temp_pdf)
total_pages = len(doc)
print(f"Total pages rendered: {total_pages}")

# Step 3: Stamp page numbers strictly starting from Chapter 1 (PDF Page 14) with '1'
# Left margin: 25mm (70.9 pt), right margin: 20mm (56.7 pt), width: 595.3 pt
for pno in range(14, total_pages + 1):
    body_page = pno - 13
    page = doc[pno - 1]
    rect = pymupdf.Rect(70.9, 796, 595.3 - 56.7, 818)
    page.insert_textbox(
        rect,
        str(body_page),
        fontsize=11,
        fontname="times-bold",
        align=pymupdf.TEXT_ALIGN_CENTER,
        color=(0, 0, 0)
    )

stamped_pdf = r"e:\Hestia\Hestia_Micro_Project_Report.pdf"
doc.save(stamped_pdf)
shutil.copyfile(stamped_pdf, r"e:\Hestia\Hestia_Project_Report.pdf")
print("Saved stamped Master PDFs successfully!")

# Step 4: Slicing coloured and blackandwhited PDFs
doc_final = pymupdf.open(stamped_pdf)
color_indices = []
for pno in range(1, len(doc_final) + 1):
    if doc_final[pno - 1].get_images():
        color_indices.append(pno - 1)

print(f"Detected {len(color_indices)} color pages: {[i+1 for i in color_indices]}")

doc_color = pymupdf.open()
for idx in color_indices:
    doc_color.insert_pdf(doc_final, from_page=idx, to_page=idx)
doc_color.save(r"e:\Hestia\coloured.pdf")
doc_color.close()
print(f"Saved coloured.pdf with {len(color_indices)} pages.")

bw_indices = [i for i in range(len(doc_final)) if i not in color_indices]
doc_bw = pymupdf.open()
for idx in bw_indices:
    doc_bw.insert_pdf(doc_final, from_page=idx, to_page=idx)
doc_bw.save(r"e:\Hestia\blackandwhited.pdf")
doc_bw.close()
print(f"Saved blackandwhited.pdf with {len(bw_indices)} pages.")

# Step 5: Sync to Downloads
downloads_dir = r"C:\Users\zyber\Downloads"
for fname in ["Hestia_Micro_Project_Report.pdf", "Hestia_Project_Report.pdf", "coloured.pdf", "blackandwhited.pdf"]:
    src = os.path.join(r"e:\Hestia", fname)
    dst = os.path.join(downloads_dir, fname)
    shutil.copyfile(src, dst)
    print(f"Synced -> {dst} ({os.path.getsize(dst):,} bytes)")

print("\nAll tasks completed successfully!")
