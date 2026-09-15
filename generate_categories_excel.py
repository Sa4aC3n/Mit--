import zipfile
import xml.etree.ElementTree as ET
import os

# Categories data extracted from InitialDataSeed.kt
categories_data = [
    {
        "id": "cat_restaurants",
        "name": "مطاعم",
        "desc": "مشويات، كريب، وجبات سريعة، وأسماك طازجة",
        "icon": "Restaurant",
        "subs": [
            ("مأكولات شرقية ومشويات", "كباب، كفتة، لحوم"),
            ("وجبات سريعة وكريب", "برجر، كريب، سندوتشات"),
            ("مأكولات بحرية وأسماك", "سمك، جمبري، طواجن"),
            ("بيتزا وفطائر", "بيتزا، فطير، إيطالي"),
            ("حلويات شرقية وغربية", "بسبوسة، كيك، آيس كريم")
        ]
    },
    {
        "id": "cat_doctors",
        "name": "عيادات وأطباء",
        "desc": "أطباء واستشاريين لكافة التخصصات الطبية",
        "icon": "MedicalServices",
        "subs": [
            ("عظام ومفاصل", "كسور، مفاصل، عمود فقري، غضروف، خشونة"),
            ("باطنة وقلب", "ضغط، سكر، قلب، أوعية دموية"),
            ("أطفال وحديثي الولادة", "أطفال، حديثي الولادة، تطعيمات، نمو، مبتسرين"),
            ("نساء وتوليد", "نساء، توليد، حمل، ولادة، سونار، عقم"),
            ("الفم والأسنان", "أسنان، فم، تقويم، حشو، زراعة، تبييض"),
            ("الأمراض الجلدية", "جلدية، أمراض جلدية، بشرة، ليزر، شعر، حساسية"),
            ("جراحة التجميل", "تجميل، جراحة تجميل، نحت، شفط، شد، ليزر"),
            ("أنف وأذن وحنجرة", "أنف، أذن، حنجرة، جيوب أنفية، لحمية، سمعيات"),
            ("الأمراض الباطنية العامة", "باطنة عامة، أمراض باطنية، كبد، جهاز هضمي، مناعة"),
            ("عظام", "عظام، كسور، جبس، مفصل"),
            ("امراض صدر وحساسية", "صدر، حساسية، ربو، تنفس، رئة، كحة"),
            ("مسالك بولية", "مسالك، كلى، حصوات، بروستاتا، مثانة"),
            ("قلب", "قلب، أوعية دموية، قسطرة، رسم قلب، إيكو"),
            ("الطب النفسي والعصبي", "نفسي، عصبي، مخ وأعصاب، توتر، اكتئاب، علاج نفسي"),
            ("أطفال", "أطفال، تغذية أطفال، كشف أطفال"),
            ("الجراحة العامة", "جراحة عامة، مناظير، فتق، مرارة، استئصال"),
            ("طب العيون", "عيون، رمد، ليزك، نظارات، مياه بيضاء، شبكية"),
            ("جراحة العظام", "جراحة عظام، مناظير مفاصل، تغيير مفاصل، رباط صليبي"),
            ("جراحة المخ والأعصاب", "جراحة مخ وأعصاب، عمود فقري، انزلاق غضروفي، أعصاب"),
            ("جراحة القلب والصدر", "جراحة قلب، جراحة صدر، قلب مفتوح، شرايين"),
            ("جراحة المسالك البولية", "جراحة مسالك، مناظير كلى، تفتيت حصوات، ذكورة وعقم")
        ]
    },
    {
        "id": "cat_medical_centers",
        "name": "مراكز طبية",
        "desc": "مراكز طبية متخصصة، مجمعات عيادات، ومراكز جراحة اليوم الواحد",
        "icon": "MedicalServices",
        "subs": [
            ("مجمعات عيادات تخصصية", "مجمع عيادات، استشاريين، تخصصات"),
            ("مراكز عيون وجراحة ليزك", "عيون، نظارات، ليزك، مياه بيضاء"),
            ("مراكز علاج طبيعي وتأهيل", "علاج طبيعي، تأهيل، فقرات، إصابات")
        ]
    },
    {
        "id": "cat_pharmacies",
        "name": "صيدليات",
        "desc": "صيدليات تعمل على مدار 24 ساعة وخدمة توصيل الأدوية",
        "icon": "LocalPharmacy",
        "subs": [
            ("صيدليات 24 ساعة وطوارئ", "طوارئ، ليلية، دواء، توصيل"),
            ("مستلزمات أطفال وألبان", "لبن أطفال، حفاضات، رعاية"),
            ("مستحضرات تجميل وعناية", "بشرة، واقي شمس، ميك أب")
        ]
    },
    {
        "id": "cat_government",
        "name": "مصالح حكومية",
        "desc": "مجلس المدينة، السجل المدني، الشهر العقاري، البريد، ومكاتب الصحة والتموين",
        "icon": "AccountBalance",
        "subs": [
            ("مجلس المدينة والوحدات المحلية", "مجلس المدينة، وحدة محلية، تنظيم، تراخيص"),
            ("سجل مدني وشهر عقاري", "سجل مدني، بطاقات، شهر عقاري، توثيق"),
            ("مكاتب البريد والتأمينات", "بريد، معاشات، تأمينات اجتماعية، حوالات"),
            ("مكاتب التموين والمرور والصحة", "تموين، مرور، رخص، صحة، مكتب صحة")
        ]
    },
    {
        "id": "cat_banks",
        "name": "بنوك",
        "desc": "البنوك الوطنية والاستثمارية، ماكينات ATM، وشركات الصرافة",
        "icon": "AccountBalanceWallet",
        "subs": [
            ("فروع البنوك الرئيسية", "البنك الأهلي، بنك مصر، بنك القاهرة، CIB، البنك الزراعي"),
            ("ماكينات صراف آلي ATM", "ATM، صراف آلي، سحب وإيداع"),
            ("شركات صرافة وتحويل أموال", "صرافة، تحويل أموال، دولار، ويسترن يونيون")
        ]
    },
    {
        "id": "cat_universities",
        "name": "جامعات",
        "desc": "جامعة الأزهر فرع تفهنا الأشراف، المعاهد العليا والكليات التخصصية",
        "icon": "School",
        "subs": [
            ("جامعة الأزهر تفهنا الأشراف", "الأزهر، شريعة وقانون، تربية، تجارة، تفهنا"),
            ("معاهد عليا وكليات", "معهد عالي، هندسة، تكنولوجيا، دراسات عليا")
        ]
    },
    {
        "id": "cat_schools",
        "name": "مدارس",
        "desc": "مدارس رسمية لغات، رسمية، معاهد أزهرية، ومدارس خاصة",
        "icon": "School",
        "subs": [
            ("مدارس رسمية لغات ورسمية", "لغات، تجريبي، ابتدائي، إعدادي، ثانوي"),
            ("معاهد أزهرية", "أزهر، معهد أزهري، ابتدائي أزهري، ثانوي أزهري"),
            ("مدارس خاصة وفنية", "خاصة، صنايع، تمريض، تجارة")
        ]
    },
    {
        "id": "cat_libraries",
        "name": "مكتبات",
        "desc": "مكتبات أدوات مدرسية، أدوات جامعية، طباعة ونسخ مستندات وكتب",
        "icon": "MenuBook",
        "subs": [
            ("أدوات مدرسية وجامعية", "أدوات مكتبية، كشكول، أقلام، شنط"),
            ("طباعة وتصوير وخدمات طابعات", "تصوير، طباعة، ملزمات، مجلات"),
            ("بيع كتب وروايات", "كتب، روايات، قصص، دينية")
        ]
    },
    {
        "id": "cat_shops",
        "name": "محلات",
        "desc": "محلات ملابس، أحذية، أدوات منزلية، أجهزة ومواد غذائية",
        "icon": "Storefront",
        "subs": [
            ("سوبرماركت ومواد غذائية", "بقالة، تموين، ماركت، ألبان"),
            ("ملابس وأحذية ونوفوتيه", "رجالي، حريمي، أطفال، أحذية، شنط"),
            ("أدوات منزلية وأجهزة كهربائية", "أجهزة، مطبخ، أدوات منزلية"),
            ("موبايلات وإلكترونيات", "موبايل، صيانة، اكسسوارات")
        ]
    },
    {
        "id": "cat_companies",
        "name": "شركات",
        "desc": "شركات المقاولات، الشحن والتوصيل، البرمجيات والدعاية والإعلان",
        "icon": "Business",
        "subs": [
            ("شركات مقاولات وديكور وعقارات", "مقاولات، تشطيبات، ديكور، عقارات"),
            ("شركات شحن ونقل بضائع", "شحن، توصيل، بضائع، نقل"),
            ("شركات برمجيات ودعاية وتسويق", "برمجيات، تسويق، دعاية، إعلان، تصميم")
        ]
    },
    {
        "id": "cat_syndicates",
        "name": "نقابات",
        "desc": "نقابة المهندسين، المعلمين، التجاريين، والأطباء والصيادلة والفرعيات",
        "icon": "Groups",
        "subs": [
            ("نقابة المهندسين والفرعيات", "مهندسين، كارنيه، مشروع علاج"),
            ("نقابة المعلمين والتجاريين", "معلمين، تجاريين، معاشات"),
            ("نقابة الأطباء والصيادلة والأسنان", "أطباء، صيادلة، علاج")
        ]
    },
    {
        "id": "cat_charities",
        "name": "جمعيات خيرية ومؤسسات أهلية",
        "desc": "الجمعيات الخيرية، كفالة الأيتام، المساعدات الاجتماعية والمؤسسات الأهلية",
        "icon": "VolunteerActivism",
        "subs": [
            ("جمعيات كفالة الأيتام والمساعدات", "أيتام، كفالة، إطعام، مساعدات، صدقات"),
            ("مؤسسات أهلية وتنمية مجتمع", "تنمية مجتمع، خدمات أهلية، تدريب"),
            ("مستوصفات خيرية وبنوك طعام", "مستوصف خيري، علاج خيري، بنك طعام")
        ]
    },
    {
        "id": "cat_technicians",
        "name": "فنيين وصنايعية",
        "desc": "كهربائي، سباك، نجار، فني تكييف وأجهزة",
        "icon": "Handyman",
        "subs": [
            ("فني كهرباء منازل", "كهربائي، تأسيس، إضاءة"),
            ("سباكة وصيانة شبكات", "سباك، خلاطات، تسريب"),
            ("صيانة تكييفات وتبريد", "تكييف، فريون، شحن"),
            ("نجارة وأثاث", "نجار، غرف، مطبخ"),
            ("نقاشة وطلاء", "نقاش، ديكور، دهان"),
            ("صيانة أجهزة منزلية", "غسالات، ثلاجات، بوتاجاز")
        ]
    },
    {
        "id": "cat_factories",
        "name": "مصانع ورش",
        "desc": "مصانع ألومنيوم، ورش حدادة وخراطة ومسابك",
        "icon": "Factory",
        "subs": [
            ("مصانع وتشكيل ألومنيوم", "ألومنيوم، قطاعات"),
            ("ورش حدادة وكريتال", "حديد، بوابات"),
            ("خراطة ومسابك ألومنيوم", "مخرطة، سباكة")
        ]
    },
    {
        "id": "cat_cafes",
        "name": "كافيهات",
        "desc": "كافيهات عائلية وجلسات شبابية ومشروبات طازجة",
        "icon": "Coffee",
        "subs": [
            ("كافيهات عائلية", "عائلات، هدوء"),
            ("عصائر وفرش", "قصب، مانجو، كوكتيل")
        ]
    },
    {
        "id": "cat_hospitals",
        "name": "مستشفيات",
        "desc": "مستشفيات عامة وخاصة ومراكز طوارئ",
        "icon": "LocalHospital",
        "subs": [
            ("مستشفيات خاصة", "عناية، عمليات"),
            ("مراكز طوارئ وإسعاف", "طوارئ، حوادث")
        ]
    },
    {
        "id": "cat_radiology",
        "name": "مراكز أشعة وتحاليل",
        "desc": "معامل تحاليل ومراكز أشعة مقطعية وتلفزيونية",
        "icon": "Biotech",
        "subs": [
            ("معامل تحاليل طبية", "دم، سكر، تحاليل شاملة"),
            ("مراكز أشعة ورنين", "رنين، مقطعية، سونار")
        ]
    },
    {
        "id": "cat_automotive",
        "name": "خدمات سيارات",
        "desc": "ميكانيكي، كهربائي سيارات، قطع غيار ومغاسل",
        "icon": "DirectionsCar",
        "subs": [
            ("ميكانيكا وسرفيس سيارات", "عفشة، محرك، صيانة"),
            ("قطع غيار واكسسوارات", "زيت، فلاتر، بطاريات")
        ]
    },
    {
        "id": "cat_clubs",
        "name": "نوادي ومراكز رياضية",
        "desc": "جيم، صالات لياقة بدنية، وملاعب خماسية",
        "icon": "FitnessCenter",
        "subs": [
            ("صالات جيم كمال أجسام", "حديد، فتنس، لياقة"),
            ("ملاعب خماسية كورة", "حجز، نجيل صناعي، كرة قدم")
        ]
    },
    {
        "id": "cat_home_events",
        "name": "خدمات منزلية ومناسبات",
        "desc": "قاعات أفراح، فراشة، وتنسيق مناسبات",
        "icon": "Event",
        "subs": [
            ("قاعات أفراح ومناسبات", "زفاف، خطوبة، مناسبات")
        ]
    }
]

# Generate SpreadsheetML / XML Excel format (.xls) which opens natively in Microsoft Excel and Google Sheets
xls_content = """<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
  <Title>التصنيفات الرئيسية والفرعية - دليل ميت غمر</Title>
  <Subject>فهرس التصنيفات والتخصصات الفرعية الرسمية</Subject>
  <Author>دليل ميت غمر الذكي</Author>
  <Company>Ajilika Technologies</Company>
 </DocumentProperties>
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="11" ss:Color="#1E293B"/>
  </Style>
  <Style ss:ID="HeaderTitle">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="16" ss:Bold="1" ss:Color="#0F172A"/>
   <Interior ss:Color="#E2E8F0" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="TableHeader">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0F172A"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0F172A"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="11" ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#0F172A" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="TableHeaderGold">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#B8860B"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#B8860B"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="11" ss:Bold="1" ss:Color="#0F172A"/>
   <Interior ss:Color="#D4AF37" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="RowEven">
   <Alignment ss:Vertical="Center" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
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
   <Interior ss:Color="#FFFFFF" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="CategoryCell">
   <Alignment ss:Vertical="Center" ss:Horizontal="Center" ss:ReadingOrder="RightToLeft"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
   </Borders>
   <Font ss:FontName="Segoe UI" x:CharSet="178" ss:Size="11" ss:Bold="1" ss:Color="#0369A1"/>
   <Interior ss:Color="#F0F9FF" ss:Pattern="Solid"/>
  </Style>
 </Styles>

 <Worksheet ss:Name="التصنيفات الشاملة">
  <Table ss:DefaultColumnWidth="60" ss:DefaultRowHeight="24">
   <Column ss:Width="40"/>
   <Column ss:Width="160"/>
   <Column ss:Width="200"/>
   <Column ss:Width="260"/>
   <Column ss:Width="280"/>
   <Row ss:Height="36">
    <Cell ss:MergeAcross="4" ss:StyleID="HeaderTitle"><Data ss:Type="String">دليل ميت غمر - جدول التصنيفات الرئيسية والتخصصات الفرعية الرسمية</Data></Cell>
   </Row>
   <Row ss:Height="28">
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">م</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">التصنيف الرئيسي</Data></Cell>
    <Cell ss:StyleID="TableHeaderGold"><Data ss:Type="String">التخصص / التصنيف الفرعي</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">الكلمات الدلالية ومفردات البحث</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">وصف التصنيف الرئيسي</Data></Cell>
   </Row>
"""

counter = 1
for cat in categories_data:
    cat_name = cat["name"]
    cat_desc = cat["desc"]
    subs = cat["subs"]
    for i, (sub_name, keywords) in enumerate(subs):
        style = "RowEven" if counter % 2 == 0 else "RowOdd"
        xls_content += f"""   <Row ss:Height="24">
    <Cell ss:StyleID="{style}"><Data ss:Type="Number">{counter}</Data></Cell>
    <Cell ss:StyleID="CategoryCell"><Data ss:Type="String">{cat_name}</Data></Cell>
    <Cell ss:StyleID="{style}"><Data ss:Type="String">{sub_name}</Data></Cell>
    <Cell ss:StyleID="{style}"><Data ss:Type="String">{keywords}</Data></Cell>
    <Cell ss:StyleID="{style}"><Data ss:Type="String">{cat_desc if i == 0 else ''}</Data></Cell>
   </Row>
"""
        counter += 1

xls_content += """  </Table>
  <WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
   <PageSetup>
    <Layout x:Orientation="Landscape"/>
   </PageSetup>
   <DisplayRightToLeft/>
   <FreezePanes/>
   <FrozenNoSplit/>
   <SplitHorizontal>2</SplitHorizontal>
   <TopRowBottomPane>2</TopRowBottomPane>
  </WorksheetOptions>
 </Worksheet>

 <Worksheet ss:Name="ملخص التصنيفات الرئيسية">
  <Table ss:DefaultColumnWidth="60" ss:DefaultRowHeight="24">
   <Column ss:Width="40"/>
   <Column ss:Width="160"/>
   <Column ss:Width="80"/>
   <Column ss:Width="300"/>
   <Column ss:Width="140"/>
   <Row ss:Height="36">
    <Cell ss:MergeAcross="4" ss:StyleID="HeaderTitle"><Data ss:Type="String">قائمة التصنيفات الرئيسية الـ 21 المعتمدة في دليل ميت غمر</Data></Cell>
   </Row>
   <Row ss:Height="28">
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">م</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">اسم التصنيف الرئيسي</Data></Cell>
    <Cell ss:StyleID="TableHeaderGold"><Data ss:Type="String">عدد الفرعيات</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">وصف التصنيف ونطاقه</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">معرّف النظام (ID)</Data></Cell>
   </Row>
"""

for idx, cat in enumerate(categories_data, 1):
    style = "RowEven" if idx % 2 == 0 else "RowOdd"
    sub_count = len(cat["subs"])
    xls_content += f"""   <Row ss:Height="24">
    <Cell ss:StyleID="{style}"><Data ss:Type="Number">{idx}</Data></Cell>
    <Cell ss:StyleID="CategoryCell"><Data ss:Type="String">{cat['name']}</Data></Cell>
    <Cell ss:StyleID="{style}"><Data ss:Type="Number">{sub_count}</Data></Cell>
    <Cell ss:StyleID="{style}"><Data ss:Type="String">{cat['desc']}</Data></Cell>
    <Cell ss:StyleID="{style}"><Data ss:Type="String">{cat['id']}</Data></Cell>
   </Row>
"""

xls_content += """  </Table>
  <WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
   <DisplayRightToLeft/>
   <FreezePanes/>
   <FrozenNoSplit/>
   <SplitHorizontal>2</SplitHorizontal>
   <TopRowBottomPane>2</TopRowBottomPane>
  </WorksheetOptions>
 </Worksheet>
</Workbook>
"""

# Save as .xls (Excel XML Workbook)
with open("dalil_mit_ghamr_categories.xls", "w", encoding="utf-8") as f:
    f.write(xls_content)

print("Saved dalil_mit_ghamr_categories.xls successfully!")
