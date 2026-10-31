import pymupdf

doc = pymupdf.open('Hestia_Micro_Project_Report.pdf')
print(f"=== MASTER REPORT AUDIT (Total Pages: {len(doc)}) ===\n")

# 1. Front Matter Check
print("--- 1. FRONT MATTER (Pages 1 to 13) ---")
front_ok = True
for pno in range(1, 14):
    p = doc[pno - 1]
    # Check bottom region
    footers = []
    for b in p.get_text('dict')['blocks']:
        if 'lines' in b:
            for l in b['lines']:
                if l['bbox'][1] > 780:
                    txt = ' '.join([s['text'] for s in l['spans']]).strip()
                    if txt:
                        footers.append((round(l['bbox'][1], 1), txt))
    status = "CLEAN" if not footers else f"UNEXPECTED: {footers}"
    if footers:
        front_ok = False
    print(f"Page {pno:02d}: {status}")

print(f"\nFront Matter Check Passed: {front_ok}\n")

# 2. Body Pages Check
print("--- 2. BODY PAGES (Pages 14 to 33) ---")
body_ok = True
for pno in range(14, len(doc) + 1):
    body_idx = pno - 13
    p = doc[pno - 1]
    
    text_lines = []
    footer_found = None
    footer_y = None
    
    for b in p.get_text('dict')['blocks']:
        if 'lines' in b:
            for l in b['lines']:
                txt = ' '.join([s['text'] for s in l['spans']]).strip()
                if not txt:
                    continue
                y0, y1 = l['bbox'][1], l['bbox'][3]
                if txt == str(body_idx) and y0 > 780:
                    footer_found = txt
                    footer_y = y0
                else:
                    text_lines.append((y0, y1, txt))
    
    max_text_y = max([tl[1] for tl in text_lines]) if text_lines else 0
    clearance = (footer_y - max_text_y) if (footer_y and text_lines) else None
    
    status = "OK"
    if footer_found != str(body_idx):
        status = f"WRONG FOOTER: {footer_found} != {body_idx}"
        body_ok = False
    elif clearance is not None and clearance < 10:
        status = "OVERLAP"
        body_ok = False
        
    clearance_str = f"{clearance:.1f} pt" if clearance is not None else "N/A"
    print(f"PDF P{pno:02d} (Body {body_idx:02d}): Footer '{footer_found}' at y={footer_y:.1f} | Max text y={max_text_y:.1f} | Clearance: {clearance_str} -> {status}")

print(f"\nBody Pages Check Passed: {body_ok}\n")

# 3. Chapter Starts
print("--- 3. CHAPTER START PAGES ---")
chapters = [
    ("Chapter 1", "CHAPTER-1"),
    ("Chapter 2", "CHAPTER-2"),
    ("Chapter 3", "CHAPTER-3"),
    ("Chapter 4", "CHAPTER-4"),
    ("Chapter 5", "CHAPTER-5"),
    ("Chapter 6", "CHAPTER-6"),
    ("Chapter 7", "CHAPTER-7"),
    ("References", "REFERENCES")
]

for ch_name, query in chapters:
    for pno in range(14, len(doc) + 1):
        txt = doc[pno - 1].get_text()
        if query in txt:
            # Check if query is near the top of the page
            first_line = [l.strip() for l in txt.splitlines() if l.strip()][0]
            print(f"{ch_name:12s} starts on PDF Page {pno:02d} (Body Page {pno-13:02d}) | Top: {first_line[:35]}")
            break

# 4. Color Pages
print("\n--- 4. COLOR PAGES AUDIT ---")
color_pages = [pno for pno in range(1, len(doc) + 1) if doc[pno - 1].get_images()]
print(f"Color pages count: {len(color_pages)}")
print(f"Pages: {color_pages}")

# 5. Fonts Check
print("\n--- 5. FONTS AUDIT ---")
fonts = set()
for p in doc:
    for f in p.get_fonts():
        fonts.add(f[3])
for f in sorted(fonts):
    print(f"- {f}")
