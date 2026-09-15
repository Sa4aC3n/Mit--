package com.example.util

import com.example.data.model.seed.InitialDataSeed
import com.example.data.engine.MetGhamrGeoHierarchy

/**
 * Generates rich, fully styled Excel Workbooks (SpreadsheetML / .xls) and CSV templates
 * with standardized categories, subcategory dropdown references, and Met Ghamr areas.
 */
object ExcelTemplateGenerator {

    /**
     * Generates a multi-sheet Microsoft Excel XML Workbook (.xls)
     * containing:
     * 1. Data Entry Sheet with standardized columns and sample data.
     * 2. Canonical Category & Subcategory Catalog.
     * 3. Standard Met Ghamr Areas & Villages Catalog.
     * 4. Rules & Standardization Guidelines to prevent duplicate entries and spelling typos.
     */
    fun generateExcelWorkbookXml(): String {
        val categories = InitialDataSeed.categories
        val areas: List<String> = MetGhamrGeoHierarchy.villagesAndUnits.map { it.nameAr }

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
  <Title>قالب استيراد أنشطة دليل ميت غمر الشامل</Title>
  <Subject>قالب إكسل قياسي مع القوائم المنسدلة والتصنيفات المعتمدة</Subject>
  <Author>إدارة دليل ميت غمر الذكي</Author>
  <Created>2026-08-29T08:00:00Z</Created>
  <Company>دليل ميت غمر التجاري والطبي</Company>
 </DocumentProperties>
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Borders/>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="11" ss:Color="#0F172A"/>
   <Interior/>
   <NumberFormat/>
   <Protection/>
  </Style>
  <Style ss:ID="HeaderNavy">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0F172A"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0F172A"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/>
   <Interior ss:Color="#0F172A" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="HeaderTeal">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0D9488"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0D9488"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/>
   <Interior ss:Color="#0D9488" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="HeaderGold">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#B45309"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#B45309"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/>
   <Interior ss:Color="#D97706" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="RowEven">
   <Alignment ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="10" ss:Color="#1E293B"/>
   <Interior ss:Color="#F8FAFC" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="RowOdd">
   <Alignment ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="10" ss:Color="#1E293B"/>
   <Interior ss:Color="#FFFFFF" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="SampleHighlight">
   <Alignment ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="10" ss:Color="#0F172A" ss:Bold="1"/>
   <Interior ss:Color="#EFF6FF" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="PhoneCell">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Consolas" ss:Size="10" ss:Color="#0369A1" ss:Bold="1"/>
   <NumberFormat ss:Format="@"/>
  </Style>
  <Style ss:ID="TitleStyle">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="16" ss:Color="#0F172A" ss:Bold="1"/>
   <Interior ss:Color="#E2E8F0" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="InstructionTitle">
   <Alignment ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="12" ss:Color="#0F172A" ss:Bold="1"/>
   <Interior ss:Color="#FEF3C7" ss:Pattern="Solid"/>
  </Style>
 </Styles>
""")

        // ==========================================
        // SHEET 1: Data Entry Sheet
        // ==========================================
        sb.append("""
 <Worksheet ss:Name="نموذج_إدخال_الأنشطة">
  <Table ss:DefaultColumnWidth="120" ss:DefaultRowHeight="24">
   <Column ss:Width="170"/> <!-- اسم النشاط -->
   <Column ss:Width="140"/> <!-- التصنيف الرئيسي -->
   <Column ss:Width="160"/> <!-- التخصص / القسم الفرعي -->
   <Column ss:Width="110"/> <!-- رقم الهاتف الأساسي -->
   <Column ss:Width="110"/> <!-- رقم هاتف إضافي -->
   <Column ss:Width="110"/> <!-- واتساب -->
   <Column ss:Width="180"/> <!-- العنوان بالتفصيل -->
   <Column ss:Width="130"/> <!-- المنطقة أو القرية -->
   <Column ss:Width="100"/> <!-- المدينة -->
   <Column ss:Width="120"/> <!-- أيام العمل -->
   <Column ss:Width="130"/> <!-- مواعيد العمل -->
   <Column ss:Width="200"/> <!-- وصف النشاط والخدمات -->
   <Column ss:Width="160"/> <!-- رابط خرائط جوجل -->
   <Column ss:Width="140"/> <!-- رابط الفيسبوك -->
   <Column ss:Width="140"/> <!-- الموقع الإلكتروني -->

   <!-- Header Row -->
   <Row ss:Height="32">
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">اسم النشاط أو المنشأة *</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">التصنيف الرئيسي * (اختر من الدليل)</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">التخصص / القسم الفرعي *</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">رقم الهاتف الأساسي *</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">رقم هاتف إضافي</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">رقم الواتساب</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">العنوان بالتفصيل *</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">المنطقة أو القرية *</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">المدينة / المركز</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">أيام العمل</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">مواعيد العمل (من - إلى)</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">وصف النشاط والخدمات</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">رابط خرائط جوجل (Google Maps)</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">صفحة الفيسبوك</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">الموقع الإلكتروني</Data></Cell>
   </Row>
""")

        // Sample Pre-Filled Exemplar Rows representing diverse requested categories (Doctors, Clubs, Schools, Universities, Gov, Restaurants, etc.)
        val sampleEntries = listOf(
            SampleRow(
                name = "عيادة د. محمد النجار",
                category = "عيادات وأطباء",
                specialty = "باطنة وقلب",
                phone = "01012345678",
                phone2 = "0506901234",
                whatsapp = "201012345678",
                address = "شارع الحرية - برج الأطباء - الدور الثالث",
                area = "شارع الحرية",
                city = "مدينة ميت غمر",
                workingDays = "السبت إلى الخميس",
                workingHours = "05:00 م - 10:00 م",
                description = "استشاري أمراض الباطنة والقلب والسكر، رسم قلب وإيكو",
                mapsUrl = "https://maps.google.com/?q=30.7180,31.2610",
                fbUrl = "https://facebook.com/dr.elnaggar.clinic",
                webUrl = ""
            ),
            SampleRow(
                name = "مركز د. سارة للأسنان وجراحة الفم",
                category = "عيادات وأطباء",
                specialty = "أسنان وتجميل",
                phone = "01123456789",
                phone2 = "",
                whatsapp = "201123456789",
                address = "شارع بورسعيد - أعلى صيدلية الأمل",
                area = "شارع بورسعيد",
                city = "مدينة ميت غمر",
                workingDays = "يومياً عدا الجمعة",
                workingHours = "03:00 م - 09:00 م",
                description = "زراعة وتقويم الأسنان، حشو عصب ميكروسكوبي وتبييض ليزر",
                mapsUrl = "https://maps.google.com/?q=30.7192,31.2585",
                fbUrl = "https://facebook.com/dr.sara.dental",
                webUrl = ""
            ),
            SampleRow(
                name = "دكتور سمير عبد الرازق",
                category = "عيادات وأطباء",
                specialty = "جراحة عامة ومناظير",
                phone = "01234567890",
                phone2 = "0506912345",
                whatsapp = "201234567890",
                address = "شارع أحمد عرابي - مجمع العيادات التخصصي",
                area = "وسط البلد",
                city = "مدينة ميت غمر",
                workingDays = "السبت والاثنين والأربعاء",
                workingHours = "06:00 م - 10:00 م",
                description = "استشاري الجراحة العامة وجراحة المناظير والأورام",
                mapsUrl = "https://maps.google.com/?q=30.7175,31.2600",
                fbUrl = "",
                webUrl = ""
            ),
            SampleRow(
                name = "نادي ميت غمر الرياضي والاجتماعي",
                category = "نوادي ومراكز رياضية",
                specialty = "نوادي اجتماعية ورياضية",
                phone = "0506905544",
                phone2 = "01009988776",
                whatsapp = "201009988776",
                address = "شارع البحر - كورنيش النيل ميت غمر",
                area = "شارع البحر",
                city = "مدينة ميت غمر",
                workingDays = "يومياً طوال الأسبوع",
                workingHours = "08:00 ص - 11:30 م",
                description = "ملاعب خماسية وتنس، حمامات سباحة، صالة جيم ولياقة بدنية وحديقة عائلات",
                mapsUrl = "https://maps.google.com/?q=30.7160,31.2550",
                fbUrl = "https://facebook.com/metghamr.sport.club",
                webUrl = "https://metghamrclub.org"
            ),
            SampleRow(
                name = "مدرسة ميت غمر الرسمية المتميزة للغات",
                category = "مدارس",
                specialty = "مدارس رسمية لغات",
                phone = "0506903322",
                phone2 = "",
                whatsapp = "",
                address = "حي المعلمين - بجوار الإدارة التعليمية",
                area = "حي المعلمين",
                city = "مدينة ميت غمر",
                workingDays = "الأحد إلى الخميس",
                workingHours = "07:30 ص - 02:30 م",
                description = "مراحل رياض الأطفال والابتدائي والإعدادي والثانوي لغات تجريبي",
                mapsUrl = "https://maps.google.com/?q=30.7230,31.2650",
                fbUrl = "https://facebook.com/metghamr.language.school",
                webUrl = ""
            ),
            SampleRow(
                name = "كلية الشريعة والقانون - جامعة الأزهر بتفهنا الأشراف",
                category = "جامعات",
                specialty = "جامعة الأزهر تفهنا الأشراف",
                phone = "0506820011",
                phone2 = "0506820012",
                whatsapp = "",
                address = "قرية تفهنا الأشراف - مركز ميت غمر",
                area = "تفهنا الأشراف",
                city = "مركز ميت غمر",
                workingDays = "السبت إلى الخميس",
                workingHours = "08:00 ص - 03:00 م",
                description = "فرع جامعة الأزهر بتفهنا الأشراف، كليات الشريعة والقانون والتجارة والتربية",
                mapsUrl = "https://maps.google.com/?q=30.7450,31.3100",
                fbUrl = "https://facebook.com/sharia.law.tfahna",
                webUrl = "http://azhar.edu.eg"
            ),
            SampleRow(
                name = "مجمع المصالح الحكومية والسجل المدني",
                category = "مصالح حكومية",
                specialty = "سجل مدني وشهر عقاري",
                phone = "0506902211",
                phone2 = "",
                whatsapp = "",
                address = "شارع 26 يوليو - بجوار محكمة ميت غمر",
                area = "شارع 26 يوليو",
                city = "مدينة ميت غمر",
                workingDays = "السبت إلى الخميس",
                workingHours = "08:00 ص - 03:00 م",
                description = "استخراج بطاقات الرقم القومي، شهادات الميلاد، والتوثيق والشهر العقاري",
                mapsUrl = "https://maps.google.com/?q=30.7188,31.2595",
                fbUrl = "",
                webUrl = "https://digital.gov.eg"
            ),
            SampleRow(
                name = "مطعم ومشويات ابن البلد",
                category = "مطاعم",
                specialty = "مأكولات شرقية ومشويات",
                phone = "01099887766",
                phone2 = "0506915555",
                whatsapp = "201099887766",
                address = "شارع بورسعيد الرئيسي - أمام حديقة الطفل",
                area = "شارع بورسعيد",
                city = "مدينة ميت غمر",
                workingDays = "يومياً طوال الأسبوع",
                workingHours = "10:00 ص - 02:00 ص",
                description = "كباب وكفتة وطواجن فرن بلدي، صالة مكيفة وخدمة توصيل سريعة",
                mapsUrl = "https://maps.google.com/?q=30.7195,31.2580",
                fbUrl = "https://facebook.com/ibnelbalad.restaurant",
                webUrl = ""
            ),
            SampleRow(
                name = "صيدلية د. مصطفى الشناوي",
                category = "صيدليات",
                specialty = "صيدليات 24 ساعة وطوارئ",
                phone = "01005544332",
                phone2 = "0506908877",
                whatsapp = "201005544332",
                address = "ميدان المحطة - بجوار محطة القطار",
                area = "ميدان المحطة",
                city = "مدينة ميت غمر",
                workingDays = "طوال الأسبوع (24 ساعة)",
                workingHours = "00:00 ص - 11:59 م (24 ساعة)",
                description = "خدمة دوائية على مدار 24 ساعة، قياس ضغط وسكر، وتوصيل مجاني للمنازل",
                mapsUrl = "https://maps.google.com/?q=30.7170,31.2625",
                fbUrl = "https://facebook.com/dr.shennawy.pharmacy",
                webUrl = ""
            ),
            SampleRow(
                name = "البنك الأهلي المصري - فرع ميت غمر",
                category = "بنوك",
                specialty = "فروع البنوك الرئيسية",
                phone = "19623",
                phone2 = "0506904411",
                whatsapp = "",
                address = "شارع البحر - كورنيش النيل",
                area = "شارع البحر",
                city = "مدينة ميت غمر",
                workingDays = "الأحد إلى الخميس",
                workingHours = "08:30 ص - 03:00 م",
                description = "خدمات مصرفية للأفراد والشركات، تمويل وقروض وماكينات صراف آلي ATM",
                mapsUrl = "https://maps.google.com/?q=30.7155,31.2560",
                fbUrl = "https://facebook.com/NBEgypt",
                webUrl = "https://nbe.com.eg"
            )
        )

        sampleEntries.forEachIndexed { index, row ->
            val styleId = if (index % 2 == 0) "RowEven" else "RowOdd"
            sb.append("""
   <Row ss:Height="22">
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.name)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.category)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.specialty)}</Data></Cell>
    <Cell ss:StyleID="PhoneCell"><Data ss:Type="String">${escapeXml(row.phone)}</Data></Cell>
    <Cell ss:StyleID="PhoneCell"><Data ss:Type="String">${escapeXml(row.phone2)}</Data></Cell>
    <Cell ss:StyleID="PhoneCell"><Data ss:Type="String">${escapeXml(row.whatsapp)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.address)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.area)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.city)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.workingDays)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.workingHours)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.description)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.mapsUrl)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.fbUrl)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(row.webUrl)}</Data></Cell>
   </Row>
""")
        }

        // Add 25 blank formatted rows ready for the user to type in
        for (i in 1..25) {
            val styleId = if (i % 2 == 0) "RowEven" else "RowOdd"
            sb.append("""
   <Row ss:Height="22">
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="PhoneCell"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="PhoneCell"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="PhoneCell"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">مدينة ميت غمر</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">يومياً</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">09:00 ص - 10:00 م</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String"></Data></Cell>
   </Row>
""")
        }

        sb.append("""
  </Table>
  <WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
   <Selected/>
   <FreezePanes/>
   <FrozenNoSplit/>
   <SplitHorizontal>1</SplitHorizontal>
   <TopRowBottomPane>1</TopRowBottomPane>
   <ActivePane>2</ActivePane>
   <Panes>
    <Pane>
     <Number>3</Number>
    </Pane>
    <Pane>
     <Number>2</Number>
    </Pane>
   </Panes>
   <ProtectObjects>False</ProtectObjects>
   <ProtectScenarios>False</ProtectScenarios>
  </WorksheetOptions>
 </Worksheet>
""")

        // ==========================================
        // SHEET 2: Categories & Specialties Catalog
        // ==========================================
        sb.append("""
 <Worksheet ss:Name="دليل_التصنيفات_والتخصصات">
  <Table ss:DefaultColumnWidth="140" ss:DefaultRowHeight="22">
   <Column ss:Width="160"/> <!-- التصنيف الرئيسي -->
   <Column ss:Width="200"/> <!-- التخصص المعتمد -->
   <Column ss:Width="240"/> <!-- الكلمات الدلالية والأمثلة -->
   <Column ss:Width="100"/> <!-- كود التصنيف -->

   <Row ss:Height="30">
    <Cell ss:StyleID="HeaderTeal"><Data ss:Type="String">التصنيف الرئيسي المعتمد (اختر منه للعمود B)</Data></Cell>
    <Cell ss:StyleID="HeaderTeal"><Data ss:Type="String">التخصص / القسم الفرعي المعتمد (للعمود C)</Data></Cell>
    <Cell ss:StyleID="HeaderTeal"><Data ss:Type="String">أمثلة وخدمات التخصص</Data></Cell>
    <Cell ss:StyleID="HeaderTeal"><Data ss:Type="String">كود النظام الداخلي</Data></Cell>
   </Row>
""")

        var subRowCounter = 0
        categories.forEach { cat ->
            if (cat.subcategories.isEmpty()) {
                val styleId = if (subRowCounter % 2 == 0) "RowEven" else "RowOdd"
                subRowCounter++
                sb.append("""
   <Row ss:Height="22">
    <Cell ss:StyleID="SampleHighlight"><Data ss:Type="String">${escapeXml(cat.nameAr)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(cat.nameAr)} عام</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(cat.description)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(cat.id)}</Data></Cell>
   </Row>
""")
            } else {
                cat.subcategories.forEach { sub ->
                    val styleId = if (subRowCounter % 2 == 0) "RowEven" else "RowOdd"
                    subRowCounter++
                    sb.append("""
   <Row ss:Height="22">
    <Cell ss:StyleID="SampleHighlight"><Data ss:Type="String">${escapeXml(cat.nameAr)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(sub.nameAr)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(sub.keywords.joinToString("، "))}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(sub.id)}</Data></Cell>
   </Row>
""")
                }
            }
        }

        sb.append("""
  </Table>
  <WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
   <FreezePanes/>
   <FrozenNoSplit/>
   <SplitHorizontal>1</SplitHorizontal>
   <TopRowBottomPane>1</TopRowBottomPane>
   <ProtectObjects>False</ProtectObjects>
   <ProtectScenarios>False</ProtectScenarios>
  </WorksheetOptions>
 </Worksheet>
""")

        // ==========================================
        // SHEET 3: Approved Areas & Villages
        // ==========================================
        sb.append("""
 <Worksheet ss:Name="المناطق_والقرى_المعتمدة">
  <Table ss:DefaultColumnWidth="150" ss:DefaultRowHeight="22">
   <Column ss:Width="180"/> <!-- المنطقة أو القرية -->
   <Column ss:Width="140"/> <!-- النوع (شارع رئيسي / قرية / مجمع) -->
   <Column ss:Width="130"/> <!-- التبعية الإدارية -->
   <Column ss:Width="180"/> <!-- معالم رئيسية -->

   <Row ss:Height="30">
    <Cell ss:StyleID="HeaderGold"><Data ss:Type="String">المنطقة أو القرية المعتمدة (للعمود H)</Data></Cell>
    <Cell ss:StyleID="HeaderGold"><Data ss:Type="String">النوع والتصنيف الجغرافي</Data></Cell>
    <Cell ss:StyleID="HeaderGold"><Data ss:Type="String">المدينة / المركز (للعمود I)</Data></Cell>
    <Cell ss:StyleID="HeaderGold"><Data ss:Type="String">أبرز المعالم</Data></Cell>
   </Row>
""")

        val keyCityStreets = listOf(
            Triple("شارع بورسعيد", "شارع تجاري رئيسي", "مدينة ميت غمر"),
            Triple("شارع البحر (الكورنيش)", "كورنيش النيل والبنوك", "مدينة ميت غمر"),
            Triple("شارع الحرية", "منطقة عيادات وتجارة", "مدينة ميت غمر"),
            Triple("شارع 26 يوليو", "منطقة خدمات ومحكمة", "مدينة ميت غمر"),
            Triple("شارع أحمد عرابي", "شارع تجاري وحيوي", "مدينة ميت غمر"),
            Triple("ميدان المحطة", "ميدان رئيسي ومواصلات", "مدينة ميت غمر"),
            Triple("حي المعلمين", "حي سكني ومدارس", "مدينة ميت غمر"),
            Triple("حي الأشراف", "حي سكني وتجاري", "مدينة ميت غمر"),
            Triple("وسط البلد", "قلب المدينة التجاري", "مدينة ميت غمر"),
            Triple("المنطقة الصناعية (الألومنيوم)", "مجمع مصانع وورش", "مدينة ميت غمر")
        )

        var areaIdx = 0
        keyCityStreets.forEach { (name, type, parent) ->
            val styleId = if (areaIdx % 2 == 0) "RowEven" else "RowOdd"
            areaIdx++
            sb.append("""
   <Row ss:Height="22">
    <Cell ss:StyleID="SampleHighlight"><Data ss:Type="String">${escapeXml(name)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(type)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(parent)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">ميت غمر الحضرية</Data></Cell>
   </Row>
""")
        }

        areas.forEach { village: String ->
            val styleId = if (areaIdx % 2 == 0) "RowEven" else "RowOdd"
            areaIdx++
            sb.append("""
   <Row ss:Height="22">
    <Cell ss:StyleID="SampleHighlight"><Data ss:Type="String">${escapeXml(village)}</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">قرية تابعة للمركز</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">مركز ميت غمر</Data></Cell>
    <Cell ss:StyleID="$styleId"><Data ss:Type="String">محافظة الدقهلية</Data></Cell>
   </Row>
""")
        }

        sb.append("""
  </Table>
  <WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
   <FreezePanes/>
   <FrozenNoSplit/>
   <SplitHorizontal>1</SplitHorizontal>
   <TopRowBottomPane>1</TopRowBottomPane>
   <ProtectObjects>False</ProtectObjects>
   <ProtectScenarios>False</ProtectScenarios>
  </WorksheetOptions>
 </Worksheet>
""")

        // ==========================================
        // SHEET 4: Rules & Guidelines
        // ==========================================
        sb.append("""
 <Worksheet ss:Name="تعليمات_وقواعد_الإدخال">
  <Table ss:DefaultColumnWidth="200" ss:DefaultRowHeight="24">
   <Column ss:Width="250"/>
   <Column ss:Width="450"/>

   <Row ss:Height="34">
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">القاعدة / العنصر</Data></Cell>
    <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">البيان والشرح القياسي لمنع التكرار والأخطاء</Data></Cell>
   </Row>

   <Row ss:Height="28">
    <Cell ss:StyleID="InstructionTitle"><Data ss:Type="String">1. توحيد التصنيف والتخصص</Data></Cell>
    <Cell ss:StyleID="RowEven"><Data ss:Type="String">انسخ اسم التصنيف من صفحة (دليل_التصنيفات_والتخصصات) كما هو لتجنب الأخطاء الإملائية مثل (اطباء بدلاً من عيادات وأطباء) أو (نوادى بدلاً من نوادي ومراكز رياضية).</Data></Cell>
   </Row>

   <Row ss:Height="28">
    <Cell ss:StyleID="InstructionTitle"><Data ss:Type="String">2. تصنيف الأطباء والتخصصات</Data></Cell>
    <Cell ss:StyleID="RowOdd"><Data ss:Type="String">في خانة التصنيف اختر: (عيادات وأطباء)، وفي خانة التخصص اختر: (باطنة وقلب، جراحة عامة ومناظير، عظام ومفاصل، أسنان وتجميل، أطفال وحديثي الولادة، نساء وتوليد، جلدية، عيون...).</Data></Cell>
   </Row>

   <Row ss:Height="28">
    <Cell ss:StyleID="InstructionTitle"><Data ss:Type="String">3. تصنيف الأنشطة التعليمية والحكومية والرياضية</Data></Cell>
    <Cell ss:StyleID="RowEven"><Data ss:Type="String">للأنشطة الرياضية: (نوادي ومراكز رياضية)، للمدارس: (مدارس)، للجامعات والمعاهد: (جامعات)، للمصالح الرسمية: (مصالح حكومية).</Data></Cell>
   </Row>

   <Row ss:Height="28">
    <Cell ss:StyleID="InstructionTitle"><Data ss:Type="String">4. تنسيق أرقام الهواتف</Data></Cell>
    <Cell ss:StyleID="RowOdd"><Data ss:Type="String">أرقام المحمول تكتب بالصيغة القياسية 01xxxxxxxxx (11 رقماً)، أو الأرضي بكود ميت غمر 050xxxxxxx.</Data></Cell>
   </Row>

   <Row ss:Height="28">
    <Cell ss:StyleID="InstructionTitle"><Data ss:Type="String">5. توحيد أسماء القرى والشوارع</Data></Cell>
    <Cell ss:StyleID="RowEven"><Data ss:Type="String">استخدم الأسماء القياسية من صفحة (المناطق_والقرى_المعتمدة) مثل (صهرجت الكبرى، تفهنا الأشراف، سنفا، بشلا، شارع بورسعيد، شارع البحر).</Data></Cell>
   </Row>

   <Row ss:Height="28">
    <Cell ss:StyleID="InstructionTitle"><Data ss:Type="String">6. كيفية الاستيراد للتطبيق</Data></Cell>
    <Cell ss:StyleID="RowOdd"><Data ss:Type="String">بعد تعبئة البيانات في صفحة (نموذج_إدخال_الأنشطة)، احفظ الملف وافتحه داخل تطبيق دليل ميت غمر -> شاشة إدارة البيانات -> استيراد جماعي Excel/CSV -> تنفيذ الاستيراد الفعلي.</Data></Cell>
   </Row>
  </Table>
 </Worksheet>
</Workbook>
""")

        return sb.toString()
    }

    /**
     * Generates a lightweight CSV template with UTF-8 BOM.
     */
    fun generateStandardCsvTemplate(): String {
        return "اسم النشاط,التصنيف الرئيسي,التخصص الفرعي,رقم الهاتف,رقم هاتف إضافي,واتساب,العنوان,المنطقة أو القرية,المدينة,أيام العمل,مواعيد العمل,الوصف,رابط الخريطة,الفيسبوك,الموقع\n" +
                "عيادة د. محمد النجار,عيادات وأطباء,باطنة وقلب,01012345678,0506901234,201012345678,شارع الحرية - برج الأطباء,شارع الحرية,مدينة ميت غمر,السبت إلى الخميس,05:00 م - 10:00 م,استشاري أمراض الباطنة والقلب,https://maps.google.com/?q=30.7180,31.2610,https://facebook.com/clinic,\n" +
                "مركز د. سارة للأسنان,عيادات وأطباء,أسنان وتجميل,01123456789,,201123456789,شارع بورسعيد,شارع بورسعيد,مدينة ميت غمر,يومياً عدا الجمعة,03:00 م - 09:00 م,زراعة وتقويم وتبييض أسنان,https://maps.google.com/?q=30.7192,31.2585,,\n" +
                "نادي ميت غمر الرياضي,نوادي ومراكز رياضية,صالات جيم كمال أجسام,0506905544,01009988776,201009988776,شارع البحر - الكورنيش,شارع البحر,مدينة ميت غمر,يومياً,08:00 ص - 11:30 م,صالات جيم وملاعب خماسية وحمامات سباحة,https://maps.google.com/?q=30.7160,31.2550,https://facebook.com/club,https://metghamrclub.org\n" +
                "مدرسة ميت غمر الرسمية للغات,مدارس,مدارس رسمية لغات ورسمية,0506903322,,,حي المعلمين,حي المعلمين,مدينة ميت غمر,الأحد إلى الخميس,07:30 ص - 02:30 م,مراحل رياض أطفال وابتدائي وإعدادي وثانوي تجريبي,https://maps.google.com/?q=30.7230,31.2650,,\n" +
                "كلية الشريعة والقانون - الأزهر,جامعات,جامعة الأزهر تفهنا الأشراف,0506820011,0506820012,,تفهنا الأشراف,تفهنا الأشراف,مركز ميت غمر,السبت إلى الخميس,08:00 ص - 03:00 م,فرع جامعة الأزهر بتفهنا الأشراف,https://maps.google.com/?q=30.7450,31.3100,,http://azhar.edu.eg\n" +
                "السجل المدني ومجلس المدينة,مصالح حكومية,سجل مدني وشهر عقاري,0506902211,,,شارع 26 يوليو,شارع 26 يوليو,مدينة ميت غمر,السبت إلى الخميس,08:00 ص - 03:00 م,بطاقات رقم قومي وشهادات ميلاد وتوثيق عقاري,https://maps.google.com/?q=30.7188,31.2595,,https://digital.gov.eg\n" +
                "مطعم ومشويات ابن البلد,مطاعم,مأكولات شرقية ومشويات,01099887766,0506915555,201099887766,شارع بورسعيد,شارع بورسعيد,مدينة ميت غمر,يومياً,10:00 ص - 02:00 ص,مشويات وطواجن وكباب وكفتة,https://maps.google.com/?q=30.7195,31.2580,,\n" +
                "صيدلية د. الشناوي,صيدليات,صيدليات 24 ساعة وطوارئ,01005544332,0506908877,201005544332,ميدان المحطة,ميدان المحطة,مدينة ميت غمر,طوال الأسبوع (24 ساعة),00:00 ص - 11:59 م,خدمة 24 ساعة وتوصيل منازل,https://maps.google.com/?q=30.7170,31.2625,,"
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    data class SampleRow(
        val name: String,
        val category: String,
        val specialty: String,
        val phone: String,
        val phone2: String,
        val whatsapp: String,
        val address: String,
        val area: String,
        val city: String,
        val workingDays: String,
        val workingHours: String,
        val description: String,
        val mapsUrl: String,
        val fbUrl: String,
        val webUrl: String
    )
}
