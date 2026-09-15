import zipfile
import io

# We will build an XLSX file directly with python zipfile
wb_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="التصنيفات والفرعيات" sheetId="1" r:id="rId1"/>
    <sheet name="التصنيفات الرئيسية" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

wb_rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

root_rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

content_types = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

styles_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><name val="Segoe UI"/><sz val="11"/></font>
    <font><b/><name val="Segoe UI"/><sz val="12"/><color rgb="FFFFFFFF"/></font>
    <font><b/><name val="Segoe UI"/><sz val="11"/><color rgb="FF0369A1"/></font>
    <font><b/><name val="Segoe UI"/><sz val="14"/><color rgb="FF0F172A"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0F172A"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF0F9FF"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/></border>
    <border>
      <left style="thin"><color rgb="FFE2E8F0"/></left>
      <right style="thin"><color rgb="FFE2E8F0"/></right>
      <top style="thin"><color rgb="FFE2E8F0"/></top>
      <bottom style="thin"><color rgb="FFE2E8F0"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="5">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="4" borderId="1" xfId="0" applyFill="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""

import xml.sax.saxutils as saxutils

def escape_xml(s):
    return saxutils.escape(str(s))

from generate_categories_excel import categories_data

# Sheet 1: Comprehensive
rows_s1 = []
# Title
rows_s1.append('<row r="1" ht="35" customHeight="1"><c r="A1" s="4" t="inlineStr"><is><t>دليل ميت غمر - جدول التصنيفات الرئيسية والتخصصات الفرعية الرسمية</t></is></c></row>')

# Header
headers_s1 = ["م", "التصنيف الرئيسي", "التخصص / التصنيف الفرعي", "الكلمات الدلالية ومفردات البحث", "وصف التصنيف الرئيسي"]
h_cells = []
cols = ["A", "B", "C", "D", "E"]
for col, h in zip(cols, headers_s1):
    h_cells.append(f'<c r="{col}2" s="1" t="inlineStr"><is><t>{escape_xml(h)}</t></is></c>')
rows_s1.append(f'<row r="2" ht="28" customHeight="1">{"".join(h_cells)}</row>')

row_idx = 3
counter = 1
for cat in categories_data:
    cat_name = cat["name"]
    cat_desc = cat["desc"]
    subs = cat["subs"]
    for i, (sub_name, keywords) in enumerate(subs):
        fill_style = "3" if counter % 2 == 0 else "0"
        c_a = f'<c r="A{row_idx}" s="{fill_style}"><v>{counter}</v></c>'
        c_b = f'<c r="B{row_idx}" s="2" t="inlineStr"><is><t>{escape_xml(cat_name)}</t></is></c>'
        c_c = f'<c r="C{row_idx}" s="{fill_style}" t="inlineStr"><is><t>{escape_xml(sub_name)}</t></is></c>'
        c_d = f'<c r="D{row_idx}" s="{fill_style}" t="inlineStr"><is><t>{escape_xml(keywords)}</t></is></c>'
        c_e = f'<c r="E{row_idx}" s="{fill_style}" t="inlineStr"><is><t>{escape_xml(cat_desc if i == 0 else "")}</t></is></c>'
        rows_s1.append(f'<row r="{row_idx}" ht="24" customHeight="1">{c_a}{c_b}{c_c}{c_d}{c_e}</row>')
        row_idx += 1
        counter += 1

sheet1_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0" rightToLeft="1">
      <pane ySplit="2" topLeftCell="A3" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="8" customWidth="1"/>
    <col min="2" max="2" width="22" customWidth="1"/>
    <col min="3" max="3" width="28" customWidth="1"/>
    <col min="4" max="4" width="40" customWidth="1"/>
    <col min="5" max="5" width="45" customWidth="1"/>
  </cols>
  <sheetData>
    {"".join(rows_s1)}
  </sheetData>
  <mergeCells count="1">
    <mergeCell ref="A1:E1"/>
  </mergeCells>
</worksheet>"""

# Sheet 2: Summary
rows_s2 = []
rows_s2.append('<row r="1" ht="35" customHeight="1"><c r="A1" s="4" t="inlineStr"><is><t>قائمة التصنيفات الرئيسية الـ 21 المعتمدة في دليل ميت غمر</t></is></c></row>')
headers_s2 = ["م", "اسم التصنيف الرئيسي", "عدد الفرعيات", "وصف التصنيف ونطاقه", "معرّف النظام (ID)"]
h_cells = []
for col, h in zip(cols, headers_s2):
    h_cells.append(f'<c r="{col}2" s="1" t="inlineStr"><is><t>{escape_xml(h)}</t></is></c>')
rows_s2.append(f'<row r="2" ht="28" customHeight="1">{"".join(h_cells)}</row>')

row_idx = 3
for idx, cat in enumerate(categories_data, 1):
    fill_style = "3" if idx % 2 == 0 else "0"
    c_a = f'<c r="A{row_idx}" s="{fill_style}"><v>{idx}</v></c>'
    c_b = f'<c r="B{row_idx}" s="2" t="inlineStr"><is><t>{escape_xml(cat["name"])}</t></is></c>'
    c_c = f'<c r="C{row_idx}" s="{fill_style}"><v>{len(cat["subs"])}</v></c>'
    c_d = f'<c r="D{row_idx}" s="{fill_style}" t="inlineStr"><is><t>{escape_xml(cat["desc"])}</t></is></c>'
    c_e = f'<c r="E{row_idx}" s="{fill_style}" t="inlineStr"><is><t>{escape_xml(cat["id"])}</t></is></c>'
    rows_s2.append(f'<row r="{row_idx}" ht="24" customHeight="1">{c_a}{c_b}{c_c}{c_d}{c_e}</row>')
    row_idx += 1

sheet2_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0" rightToLeft="1">
      <pane ySplit="2" topLeftCell="A3" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  <cols>
    <col min="1" max="1" width="8" customWidth="1"/>
    <col min="2" max="2" width="25" customWidth="1"/>
    <col min="3" max="3" width="16" customWidth="1"/>
    <col min="4" max="4" width="50" customWidth="1"/>
    <col min="5" max="5" width="22" customWidth="1"/>
  </cols>
  <sheetData>
    {"".join(rows_s2)}
  </sheetData>
  <mergeCells count="1">
    <mergeCell ref="A1:E1"/>
  </mergeCells>
</worksheet>"""

with zipfile.ZipFile("dalil_mit_ghamr_categories.xlsx", "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", content_types)
    z.writestr("_rels/.rels", root_rels)
    z.writestr("xl/_rels/workbook.xml.rels", wb_rels)
    z.writestr("xl/workbook.xml", wb_xml)
    z.writestr("xl/styles.xml", styles_xml)
    z.writestr("xl/worksheets/sheet1.xml", sheet1_xml)
    z.writestr("xl/worksheets/sheet2.xml", sheet2_xml)

print("Saved dalil_mit_ghamr_categories.xlsx successfully!")
