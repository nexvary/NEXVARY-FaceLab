package com.nexvary.facelab

import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.media.FaceDetector
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FaceLabApp(this) }
    }
}


enum class AppLanguage(val code: String, val short: String, val nativeName: String, val rtl: Boolean = false) {
    Arabic("ar", "AR", "العربية", true),
    English("en", "EN", "English"),
    Turkish("tr", "TR", "Türkçe"),
    Spanish("es", "ES", "Español"),
    German("de", "DE", "Deutsch"),
    Italian("it", "IT", "Italiano"),
    French("fr", "FR", "Français"),
    Urdu("ur", "UR", "اردو", true),
    Persian("fa", "FA", "فارسی", true)
}

internal fun tx(
    lang: AppLanguage,
    ar: String, en: String, tr: String, es: String, de: String, it: String,
    fr: String = en, ur: String = en, fa: String = en
): String = when (lang) {
    AppLanguage.Arabic -> ar
    AppLanguage.English -> en
    AppLanguage.Turkish -> tr
    AppLanguage.Spanish -> es
    AppLanguage.German -> de
    AppLanguage.Italian -> it
    AppLanguage.French -> fr
    AppLanguage.Urdu -> ur
    AppLanguage.Persian -> fa
}

enum class LocalEffect(val ar: String, val en: String, val tr: String, val es: String, val de: String, val it: String) {
    Original("الأصل", "Original", "Orijinal", "Original", "Original", "Originale"),
    Clean("تحسين", "Enhance", "İyileştir", "Mejorar", "Verbessern", "Migliora"),
    Warm("دافئ", "Warm", "Sıcak", "Cálido", "Warm", "Caldo"),
    Mono("أبيض وأسود", "Mono", "Siyah beyaz", "Blanco y negro", "Schwarzweiß", "Bianco e nero"),
    Cool("بارد", "Cool", "Soğuk", "Frío", "Kühl", "Freddo");
    fun label(lang: AppLanguage) = tx(lang, ar, en, tr, es, de, it)
}

enum class ToolCategory(val ar: String, val en: String, val tr: String, val es: String, val de: String, val it: String, val icon: ImageVector) {
    Age("العمر", "Age", "Yaş", "Edad", "Alter", "Età", Icons.Default.Cake),
    Hair("الشعر", "Hair", "Saç", "Cabello", "Haare", "Capelli", Icons.Default.ContentCut),
    Face("الوجه", "Face", "Yüz", "Rostro", "Gesicht", "Viso", Icons.Default.Face),
    Beauty("الجمال", "Beauty", "Güzellik", "Belleza", "Beauty", "Bellezza", Icons.Default.AutoAwesome),
    Background("الخلفية", "Background", "Arka plan", "Fondo", "Hintergrund", "Sfondo", Icons.Default.Layers);
    fun label(lang: AppLanguage) = tx(lang, ar, en, tr, es, de, it)
}

enum class SmartTool(val category: ToolCategory, val ar: String, val en: String, val tr: String, val es: String, val de: String, val it: String, val icon: ImageVector) {
    None(ToolCategory.Face, "بدون", "None", "Yok", "Ninguno", "Keine", "Nessuno", Icons.Default.Block),
    Younger(ToolCategory.Age, "أصغر", "Younger", "Gençleştir", "Rejuvenecer", "Jünger", "Ringiovanisci", Icons.Default.ChildCare),
    Older(ToolCategory.Age, "أكبر", "Older", "Yaşlandır", "Envejecer", "Älter", "Invecchia", Icons.Default.Elderly),
    Beard(ToolCategory.Face, "لحية", "Beard", "Sakal", "Barba", "Bart", "Barba", Icons.Default.Face),
    RemoveBeard(ToolCategory.Face, "إزالة اللحية", "Remove beard", "Sakalı kaldır", "Quitar barba", "Bart entfernen", "Rimuovi barba", Icons.Default.CleaningServices),
    Smile(ToolCategory.Face, "ابتسامة", "Smile", "Gülümseme", "Sonrisa", "Lächeln", "Sorriso", Icons.Default.SentimentSatisfied),
    Hair(ToolCategory.Hair, "تسريحة/مرجع", "Hair / reference", "Saç / referans", "Cabello / referencia", "Haare / Referenz", "Capelli / riferimento", Icons.Default.ContentCut),
    HairColor(ToolCategory.Hair, "لون الشعر", "Hair color", "Saç rengi", "Color de cabello", "Haarfarbe", "Colore capelli", Icons.Default.Palette),
    Makeup(ToolCategory.Beauty, "مكياج", "Makeup", "Makyaj", "Maquillaje", "Make-up", "Trucco", Icons.Default.Brush),
    EyeColor(ToolCategory.Beauty, "لون العين", "Eye color", "Göz rengi", "Color de ojos", "Augenfarbe", "Colore occhi", Icons.Default.Visibility),
    Teeth(ToolCategory.Beauty, "تبييض الأسنان", "Teeth", "Diş beyazlatma", "Blanquear dientes", "Zähne aufhellen", "Sbianca denti", Icons.Default.AutoFixHigh),
    Smooth(ToolCategory.Beauty, "تنعيم البشرة", "Skin smooth", "Cildi yumuşat", "Suavizar piel", "Haut glätten", "Leviga pelle", Icons.Default.BlurOn),
    EnhanceAI(ToolCategory.Beauty, "تحسين AI", "AI Enhance", "AI iyileştirme", "Mejora IA", "KI-Verbesserung", "Migliora IA", Icons.Default.Bolt),
    BackgroundBlur(ToolCategory.Background, "تمويه الخلفية", "Background blur", "Arka plan bulanık", "Desenfocar fondo", "Hintergrund weichzeichnen", "Sfoca sfondo", Icons.Default.BlurCircular),
    BackgroundRemove(ToolCategory.Background, "إزالة الخلفية", "Remove background", "Arka planı kaldır", "Quitar fondo", "Hintergrund entfernen", "Rimuovi sfondo", Icons.Default.PersonRemove),
    Relight(ToolCategory.Beauty, "إضاءة بورتريه", "Portrait relight", "Portre ışığı", "Iluminación de retrato", "Porträtlicht", "Luce ritratto", Icons.Default.LightMode),
    BlemishFix(ToolCategory.Beauty, "إزالة الشوائب", "Blemish cleanup", "Leke temizleme", "Eliminar imperfecciones", "Hautunreinheiten", "Rimuovi imperfezioni", Icons.Default.Healing),
    EyeEnhance(ToolCategory.Beauty, "تحسين العين", "Eye enhance", "Göz iyileştirme", "Mejorar ojos", "Augen verbessern", "Migliora occhi", Icons.Default.RemoveRedEye),
    Headshot(ToolCategory.Face, "صورة شخصية احترافية", "Professional headshot", "Profesyonel portre", "Retrato profesional", "Professionelles Porträt", "Ritratto professionale", Icons.Default.Portrait),
    BackgroundStudio(ToolCategory.Background, "خلفية استوديو", "Studio background", "Stüdyo arka planı", "Fondo de estudio", "Studiohintergrund", "Sfondo studio", Icons.Default.Wallpaper);
    fun label(lang: AppLanguage) = tx(lang, ar, en, tr, es, de, it)
}

enum class EditPreset(val ar: String, val en: String, val tr: String, val es: String, val de: String, val it: String, val strength: Float) {
    Natural("طبيعي", "Natural", "Doğal", "Natural", "Natürlich", "Naturale", .35f),
    Balanced("متوازن", "Balanced", "Dengeli", "Equilibrado", "Ausgewogen", "Bilanciato", .62f),
    Strong("قوي", "Strong", "Güçlü", "Fuerte", "Stark", "Forte", .88f);
    fun label(lang: AppLanguage) = tx(lang, ar, en, tr, es, de, it)
}

enum class ServerState { Unknown, Checking, Online, Offline }

enum class AppScreen(val icon: ImageVector) {
    Editor(Icons.Default.AutoAwesome),
    Wardrobe(Icons.Default.Checkroom),
    Heritage(Icons.Default.Landscape),
    Film(Icons.Default.MovieCreation),
    Gallery(Icons.Default.PhotoLibrary),
    Models(Icons.Default.Memory),
    Settings(Icons.Default.Settings),
    About(Icons.Default.Info)
}




data class HeritageSite(val id:String,val country:String,val city:String,val ar:String,val en:String,val tr:String,val es:String,val de:String,val it:String) {
    fun label(lang:AppLanguage)=tx(lang,ar,en,tr,es,de,it)
}
val HERITAGE_SITES = listOf(
    HeritageSite("eg_giza","EG","Giza","أهرامات الجيزة وأبو الهول","Giza Pyramids & Sphinx","Giza Pyramids & Sphinx","Giza Pyramids & Sphinx","Giza Pyramids & Sphinx","Giza Pyramids & Sphinx"),
    HeritageSite("eg_luxor","EG","Luxor","الأقصر والكرنك","Luxor & Karnak","Luxor & Karnak","Luxor & Karnak","Luxor & Karnak","Luxor & Karnak"),
    HeritageSite("eg_abusimbel","EG","Aswan","أبو سمبل","Abu Simbel","Abu Simbel","Abu Simbel","Abu Simbel","Abu Simbel"),
    HeritageSite("eg_philae","EG","Aswan","معبد فيلة","Philae Temple","Philae Temple","Philae Temple","Philae Temple","Philae Temple"),
    HeritageSite("eg_cairo","EG","Cairo","القاهرة التاريخية","Historic Cairo","Historic Cairo","Historic Cairo","Historic Cairo","Historic Cairo"),
    HeritageSite("eg_qaitbay","EG","Alexandria","قلعة قايتباي","Qaitbay Citadel","Qaitbay Citadel","Qaitbay Citadel","Qaitbay Citadel","Qaitbay Citadel"),
    HeritageSite("eg_saqqara","EG","Giza","سقارة والهرم المدرج","Saqqara & Step Pyramid","Saqqara & Step Pyramid","Saqqara & Step Pyramid","Saqqara & Step Pyramid","Saqqara & Step Pyramid"),
    HeritageSite("eg_valleykings","EG","Luxor","وادي الملوك","Valley of the Kings","Valley of the Kings","Valley of the Kings","Valley of the Kings","Valley of the Kings"),
    HeritageSite("tr_istanbul","TR","Istanbul","إسطنبول التاريخية","Historic Istanbul","Historic Istanbul","Historic Istanbul","Historic Istanbul","Historic Istanbul"),
    HeritageSite("tr_cappadocia","TR","Nevşehir","كابادوكيا","Cappadocia","Cappadocia","Cappadocia","Cappadocia","Cappadocia"),
    HeritageSite("tr_ephesus","TR","İzmir","أفسس","Ephesus","Ephesus","Ephesus","Ephesus","Ephesus"),
    HeritageSite("tr_pamukkale","TR","Denizli","باموكالي وهيرابوليس","Pamukkale & Hierapolis","Pamukkale & Hierapolis","Pamukkale & Hierapolis","Pamukkale & Hierapolis","Pamukkale & Hierapolis"),
    HeritageSite("tr_nemrut","TR","Adıyaman","جبل نمرود","Mount Nemrut","Mount Nemrut","Mount Nemrut","Mount Nemrut","Mount Nemrut"),
    HeritageSite("tr_sumela","TR","Trabzon","دير سوميلا","Sumela Monastery","Sumela Monastery","Sumela Monastery","Sumela Monastery","Sumela Monastery"),
    HeritageSite("tr_troy","TR","Çanakkale","طروادة","Troy","Troy","Troy","Troy","Troy"),
    HeritageSite("tr_ani","TR","Kars","مدينة آني الأثرية","Archaeological Site of Ani","Archaeological Site of Ani","Archaeological Site of Ani","Archaeological Site of Ani","Archaeological Site of Ani"),
    HeritageSite("dz_timgad","DZ","Batna","تيمقاد","Timgad","Timgad","Timgad","Timgad","Timgad"),
    HeritageSite("dz_djemila","DZ","Sétif","جميلة الأثرية","Djémila","Djémila","Djémila","Djémila","Djémila"),
    HeritageSite("dz_tipasa","DZ","Tipaza","تيبازة","Tipasa","Tipasa","Tipasa","Tipasa","Tipasa"),
    HeritageSite("dz_kasbah","DZ","Algiers","قصبة الجزائر","Kasbah of Algiers","Kasbah of Algiers","Kasbah of Algiers","Kasbah of Algiers","Kasbah of Algiers"),
    HeritageSite("dz_mzab","DZ","Ghardaïa","وادي مزاب","M'Zab Valley","M'Zab Valley","M'Zab Valley","M'Zab Valley","M'Zab Valley"),
    HeritageSite("dz_tassili","DZ","Djanet","طاسيلي ناجر","Tassili n'Ajjer","Tassili n'Ajjer","Tassili n'Ajjer","Tassili n'Ajjer","Tassili n'Ajjer"),
    HeritageSite("dz_benihammad","DZ","M'Sila","قلعة بني حماد","Beni Hammad Fort","Beni Hammad Fort","Beni Hammad Fort","Beni Hammad Fort","Beni Hammad Fort"),
    HeritageSite("dz_constantine","DZ","Constantine","جسور قسنطينة","Constantine Bridges","Constantine Bridges","Constantine Bridges","Constantine Bridges","Constantine Bridges"),
    HeritageSite("sa_alula","SA","AlUla","الحِجر - مدائن صالح","Hegra / Madain Salih","Hegra / Madain Salih","Hegra / Madain Salih","Hegra / Madain Salih","Hegra / Madain Salih"),
    HeritageSite("sa_diriyah","SA","Riyadh","حي الطريف بالدرعية","At-Turaif, Diriyah","At-Turaif, Diriyah","At-Turaif, Diriyah","At-Turaif, Diriyah","At-Turaif, Diriyah"),
    HeritageSite("sa_jeddah","SA","Jeddah","جدة التاريخية","Historic Jeddah","Historic Jeddah","Historic Jeddah","Historic Jeddah","Historic Jeddah"),
    HeritageSite("sa_hail","SA","Ha'il","الفنون الصخرية في حائل","Rock Art in Ha'il","Rock Art in Ha'il","Rock Art in Ha'il","Rock Art in Ha'il","Rock Art in Ha'il"),
    HeritageSite("sa_ahsa","SA","Al-Ahsa","واحة الأحساء","Al-Ahsa Oasis","Al-Ahsa Oasis","Al-Ahsa Oasis","Al-Ahsa Oasis","Al-Ahsa Oasis"),
    HeritageSite("sa_hima","SA","Najran","منطقة حِمى الثقافية","Hima Cultural Area","Hima Cultural Area","Hima Cultural Area","Hima Cultural Area","Hima Cultural Area"),
    HeritageSite("sa_riyadh","SA","Riyadh","قصر المصمك","Masmak Fortress","Masmak Fortress","Masmak Fortress","Masmak Fortress","Masmak Fortress"),
    HeritageSite("sa_kaaba","SA","Makkah","المسجد الحرام","Masjid al-Haram","Masjid al-Haram","Masjid al-Haram","Masjid al-Haram","Masjid al-Haram"),
    HeritageSite("ma_marrakech","MA","Marrakech","مدينة مراكش القديمة","Medina of Marrakech","Medina of Marrakech","Medina of Marrakech","Medina of Marrakech","Medina of Marrakech"),
    HeritageSite("ma_fes","MA","Fes","فاس القديمة","Medina of Fez","Medina of Fez","Medina of Fez","Medina of Fez","Medina of Fez"),
    HeritageSite("ma_aitbenhaddou","MA","Ouarzazate","قصر آيت بن حدو","Ait Ben Haddou","Ait Ben Haddou","Ait Ben Haddou","Ait Ben Haddou","Ait Ben Haddou"),
    HeritageSite("ma_essaouira","MA","Essaouira","الصويرة القديمة","Medina of Essaouira","Medina of Essaouira","Medina of Essaouira","Medina of Essaouira","Medina of Essaouira"),
    HeritageSite("ma_volubilis","MA","Meknes","وليلي الأثرية","Volubilis","Volubilis","Volubilis","Volubilis","Volubilis"),
    HeritageSite("ma_rabat","MA","Rabat","الرباط التاريخية","Historic Rabat","Historic Rabat","Historic Rabat","Historic Rabat","Historic Rabat"),
    HeritageSite("ma_chefchaouen","MA","Chefchaouen","شفشاون القديمة","Old Chefchaouen","Old Chefchaouen","Old Chefchaouen","Old Chefchaouen","Old Chefchaouen"),
    HeritageSite("ma_hassan","MA","Rabat","صومعة حسان","Hassan Tower","Hassan Tower","Hassan Tower","Hassan Tower","Hassan Tower"),
    HeritageSite("tn_carthage","TN","Tunis","قرطاج","Carthage","Carthage","Carthage","Carthage","Carthage"),
    HeritageSite("tn_eljem","TN","El Jem","مدرج الجم","Amphitheatre of El Jem","Amphitheatre of El Jem","Amphitheatre of El Jem","Amphitheatre of El Jem","Amphitheatre of El Jem"),
    HeritageSite("tn_kairouan","TN","Kairouan","القيروان","Kairouan","Kairouan","Kairouan","Kairouan","Kairouan"),
    HeritageSite("tn_tunis","TN","Tunis","مدينة تونس العتيقة","Medina of Tunis","Medina of Tunis","Medina of Tunis","Medina of Tunis","Medina of Tunis"),
    HeritageSite("tn_dougga","TN","Béja","دقة الأثرية","Dougga","Dougga","Dougga","Dougga","Dougga"),
    HeritageSite("tn_sousse","TN","Sousse","سوسة القديمة","Medina of Sousse","Medina of Sousse","Medina of Sousse","Medina of Sousse","Medina of Sousse"),
    HeritageSite("tn_bizerte","TN","Bizerte","المدينة القديمة ببنزرت","Old Bizerte","Old Bizerte","Old Bizerte","Old Bizerte","Old Bizerte"),
    HeritageSite("tn_matmata","TN","Gabès","مطماطة","Matmata","Matmata","Matmata","Matmata","Matmata"),
    HeritageSite("it_rome","IT","Rome","الكولوسيوم وروما التاريخية","Colosseum & Historic Rome","Colosseum & Historic Rome","Colosseum & Historic Rome","Colosseum & Historic Rome","Colosseum & Historic Rome"),
    HeritageSite("it_venice","IT","Venice","البندقية","Venice","Venice","Venice","Venice","Venice"),
    HeritageSite("it_florence","IT","Florence","فلورنسا التاريخية","Historic Florence","Historic Florence","Historic Florence","Historic Florence","Historic Florence"),
    HeritageSite("it_pisa","IT","Pisa","برج بيزا","Leaning Tower of Pisa","Leaning Tower of Pisa","Leaning Tower of Pisa","Leaning Tower of Pisa","Leaning Tower of Pisa"),
    HeritageSite("it_pompeii","IT","Naples","بومبي","Pompeii","Pompeii","Pompeii","Pompeii","Pompeii"),
    HeritageSite("it_matera","IT","Matera","ساسي ماتيرا","Sassi of Matera","Sassi of Matera","Sassi of Matera","Sassi of Matera","Sassi of Matera"),
    HeritageSite("it_siena","IT","Siena","سيينا التاريخية","Historic Siena","Historic Siena","Historic Siena","Historic Siena","Historic Siena"),
    HeritageSite("it_verona","IT","Verona","فيرونا التاريخية","Historic Verona","Historic Verona","Historic Verona","Historic Verona","Historic Verona"),
    HeritageSite("es_alhambra","ES","Granada","قصر الحمراء","Alhambra","Alhambra","Alhambra","Alhambra","Alhambra"),
    HeritageSite("es_sagrada","ES","Barcelona","ساغرادا فاميليا","Sagrada Familia","Sagrada Familia","Sagrada Familia","Sagrada Familia","Sagrada Familia"),
    HeritageSite("es_toledo","ES","Toledo","طليطلة التاريخية","Historic Toledo","Historic Toledo","Historic Toledo","Historic Toledo","Historic Toledo"),
    HeritageSite("es_cordoba","ES","Cordoba","قرطبة التاريخية","Historic Cordoba","Historic Cordoba","Historic Cordoba","Historic Cordoba","Historic Cordoba"),
    HeritageSite("es_seville","ES","Seville","إشبيلية التاريخية","Historic Seville","Historic Seville","Historic Seville","Historic Seville","Historic Seville"),
    HeritageSite("es_segovi","ES","Segovia","قناة سيغوفيا","Aqueduct of Segovia","Aqueduct of Segovia","Aqueduct of Segovia","Aqueduct of Segovia","Aqueduct of Segovia"),
    HeritageSite("es_avila","ES","Avila","أسوار أفيلا","Walls of Avila","Walls of Avila","Walls of Avila","Walls of Avila","Walls of Avila"),
    HeritageSite("es_santiago","ES","Santiago","سانتياغو دي كومبوستيلا","Santiago de Compostela","Santiago de Compostela","Santiago de Compostela","Santiago de Compostela","Santiago de Compostela"),
    HeritageSite("gr_acropolis","GR","Athens","أكروبوليس أثينا","Acropolis of Athens","Acropolis of Athens","Acropolis of Athens","Acropolis of Athens","Acropolis of Athens"),
    HeritageSite("gr_delphi","GR","Delphi","دلفي","Delphi","Delphi","Delphi","Delphi","Delphi"),
    HeritageSite("gr_meteora","GR","Thessaly","ميتيورا","Meteora","Meteora","Meteora","Meteora","Meteora"),
    HeritageSite("gr_olympia","GR","Olympia","أولمبيا القديمة","Ancient Olympia","Ancient Olympia","Ancient Olympia","Ancient Olympia","Ancient Olympia"),
    HeritageSite("gr_mycenae","GR","Argolis","موكناي","Mycenae","Mycenae","Mycenae","Mycenae","Mycenae"),
    HeritageSite("gr_epidaurus","GR","Argolis","إبيداوروس","Epidaurus","Epidaurus","Epidaurus","Epidaurus","Epidaurus"),
    HeritageSite("gr_rhodes","GR","Rhodes","مدينة رودس القديمة","Medieval City of Rhodes","Medieval City of Rhodes","Medieval City of Rhodes","Medieval City of Rhodes","Medieval City of Rhodes"),
    HeritageSite("gr_santorini","GR","Santorini","سانتوريني","Santorini","Santorini","Santorini","Santorini","Santorini")
)

enum class HeritageMode { Portrait, FullBody, Travel, Cinematic }
enum class HeritageLight { Day, Sunset, Night }

enum class WardrobeCategory(val ar:String,val en:String,val tr:String,val es:String,val de:String,val it:String) {
    Formal("رسمي","Formal","Resmi","Formal","Formell","Formale"),
    Casual("كاجوال","Casual","Günlük","Casual","Freizeit","Casual"),
    Sport("رياضي","Sport","Spor","Deportivo","Sport","Sportivo"),
    Summer("صيفي","Summer","Yaz","Verano","Sommer","Estivo"),
    Winter("شتوي","Winter","Kış","Invierno","Winter","Invernale"),
    Traditional("تراثي","Traditional","Geleneksel","Tradicional","Traditionell","Tradizionale"),
    Business("عمل","Business","İş","Negocios","Business","Business"),
    Evening("مناسبات","Evening","Gece","Noche","Abend","Sera");
    fun label(lang:AppLanguage)=tx(lang,ar,en,tr,es,de,it)
}



enum class WardrobeGender { All, Men, Women, Unisex }
enum class WardrobeColor(val code:String, val label:String) { Auto("auto","Auto"), Black("black","Black"), Navy("navy","Navy"), White("white","White"), Beige("beige","Beige"), Red("red","Red"), Green("green","Green"), Blue("blue","Blue"), Gold("gold","Gold"), Silver("silver","Silver") }
enum class WardrobeAccessory(val code:String, val label:String, val icon:ImageVector) {
    None("none","None",Icons.Default.Block),
    Glasses("glasses","Glasses",Icons.Default.Visibility),
    Hat("hat","Hat",Icons.Default.Face),
    Watch("watch","Watch",Icons.Default.Watch),
    Scarf("scarf","Scarf",Icons.Default.Style),
    Shoes("shoes","Shoes",Icons.Default.DirectionsWalk),
    Bag("bag","Bag",Icons.Default.Work),
    Necklace("necklace","Necklace",Icons.Default.Diamond)
}

enum class WardrobeShelf(val icon:ImageVector) {
    Clothing(Icons.Default.Checkroom), Outfits(Icons.Default.DashboardCustomize),
    Shoes(Icons.Default.DirectionsWalk), Glasses(Icons.Default.Visibility), Hats(Icons.Default.Face)
}

enum class WardrobeOccasion(val code:String,val ar:String,val en:String,val tr:String,val es:String,val de:String,val it:String,val icon:ImageVector) {
    Work("work","العمل","Work","İş","Trabajo","Arbeit","Lavoro",Icons.Default.BusinessCenter),
    Wedding("wedding","زفاف","Wedding","Düğün","Boda","Hochzeit","Matrimonio",Icons.Default.Celebration),
    Travel("travel","السفر","Travel","Seyahat","Viaje","Reise","Viaggio",Icons.Default.Flight),
    Photo("photo","جلسة تصوير","Photo shoot","Fotoğraf çekimi","Sesión de fotos","Fotoshooting","Servizio foto",Icons.Default.PhotoCamera),
    Sport("sport","رياضة","Sport","Spor","Deporte","Sport","Sport",Icons.Default.FitnessCenter),
    Winter("winter","شتاء","Winter","Kış","Invierno","Winter","Inverno",Icons.Default.AcUnit),
    Evening("evening","سهرة","Evening","Gece","Noche","Abend","Sera",Icons.Default.Nightlife);
    fun label(lang:AppLanguage)=tx(lang,ar,en,tr,es,de,it)
}

data class WardrobeOutfitPack(
    val id:String,
    val titleAr:String,val titleEn:String,val titleTr:String,val titleEs:String,val titleDe:String,val titleIt:String,
    val styleId:String,val color:WardrobeColor,val accessory:WardrobeAccessory,val occasion:WardrobeOccasion,
    val prompt:String
) {
    fun label(lang:AppLanguage)=tx(lang,titleAr,titleEn,titleTr,titleEs,titleDe,titleIt)
}

private val WARDROBE_OUTFITS = listOf(
    WardrobeOutfitPack("executive","تنفيذي فاخر","Executive","Yönetici","Ejecutivo","Executive","Executive","business_executive",WardrobeColor.Navy,WardrobeAccessory.Watch,WardrobeOccasion.Work,"tailored executive suit, coordinated shirt, formal shoes, premium watch"),
    WardrobeOutfitPack("smart_travel","سفر أنيق","Smart travel","Şık seyahat","Viaje elegante","Smart Travel","Viaggio smart","business_smart",WardrobeColor.Beige,WardrobeAccessory.Glasses,WardrobeOccasion.Travel,"smart travel outfit, layered jacket, comfortable trousers, clean sneakers, sunglasses"),
    WardrobeOutfitPack("wedding_black","زفاف كلاسيكي","Classic wedding","Klasik düğün","Boda clásica","Klassische Hochzeit","Matrimonio classico","formal_tuxedo",WardrobeColor.Black,WardrobeAccessory.Watch,WardrobeOccasion.Wedding,"black tuxedo, crisp shirt, formal shoes, elegant watch"),
    WardrobeOutfitPack("winter_city","شتاء المدينة","Winter city","Kış şehir","Invierno urbano","Winter City","Inverno in città","winter_wool_coat",WardrobeColor.Navy,WardrobeAccessory.Scarf,WardrobeOccasion.Winter,"wool coat, turtleneck, tailored trousers, winter shoes, coordinated scarf"),
    WardrobeOutfitPack("gym_pro","رياضي احترافي","Pro gym","Profesyonel spor","Gimnasio pro","Pro Gym","Palestra pro","sport_gym",WardrobeColor.Black,WardrobeAccessory.Shoes,WardrobeOccasion.Sport,"modern gym top, athletic bottoms, performance shoes"),
    WardrobeOutfitPack("heritage","تراث فاخر","Heritage","Miras","Tradición","Heritage","Tradizione","traditional_galabeya",WardrobeColor.White,WardrobeAccessory.None,WardrobeOccasion.Photo,"premium traditional full-body outfit with coordinated footwear"),
    WardrobeOutfitPack("evening_luxe","سهرة فاخرة","Evening luxe","Lüks gece","Noche de lujo","Abend Luxe","Sera lusso","evening_premium",WardrobeColor.Black,WardrobeAccessory.Necklace,WardrobeOccasion.Evening,"luxury evening outfit, elegant footwear, refined accessory")
)



data class WardrobePiece(val code:String,val ar:String,val en:String,val tr:String,val es:String,val de:String,val it:String) {
    fun label(lang:AppLanguage)=tx(lang,ar,en,tr,es,de,it)
}

private val TOP_PIECES = listOf(
    WardrobePiece("auto","تلقائي","Auto","Otomatik","Auto","Auto","Auto"),
    WardrobePiece("shirt","قميص","Shirt","Gömlek","Camisa","Hemd","Camicia"),
    WardrobePiece("tshirt","تيشيرت","T-shirt","Tişört","Camiseta","T-Shirt","T-shirt"),
    WardrobePiece("polo","بولو","Polo","Polo","Polo","Polo","Polo"),
    WardrobePiece("blouse","بلوزة","Blouse","Bluz","Blusa","Bluse","Blusa"),
    WardrobePiece("turtleneck","هاي كول","Turtleneck","Balıkçı yaka","Cuello alto","Rollkragen","Dolcevita")
)
private val BOTTOM_PIECES = listOf(
    WardrobePiece("auto","تلقائي","Auto","Otomatik","Auto","Auto","Auto"),
    WardrobePiece("trousers","بنطلون قماش","Trousers","Kumaş pantolon","Pantalón","Stoffhose","Pantaloni"),
    WardrobePiece("jeans","جينز","Jeans","Kot","Vaqueros","Jeans","Jeans"),
    WardrobePiece("chino","تشينو","Chinos","Chino","Chino","Chino","Chino"),
    WardrobePiece("skirt","تنورة","Skirt","Etek","Falda","Rock","Gonna"),
    WardrobePiece("shorts","شورت","Shorts","Şort","Shorts","Shorts","Shorts")
)
private val OUTER_PIECES = listOf(
    WardrobePiece("none","بدون","None","Yok","Ninguno","Keine","Nessuno"),
    WardrobePiece("blazer","بليزر","Blazer","Blazer","Blazer","Blazer","Blazer"),
    WardrobePiece("jacket","جاكيت","Jacket","Ceket","Chaqueta","Jacke","Giacca"),
    WardrobePiece("coat","معطف","Coat","Palto","Abrigo","Mantel","Cappotto"),
    WardrobePiece("cardigan","كارديجان","Cardigan","Hırka","Cárdigan","Cardigan","Cardigan")
)
private val SHOE_PIECES = listOf(
    WardrobePiece("auto","تلقائي","Auto","Otomatik","Auto","Auto","Auto"),
    WardrobePiece("formal_shoes","حذاء رسمي","Formal shoes","Resmi ayakkabı","Zapatos formales","Businessschuhe","Scarpe formali"),
    WardrobePiece("sneakers","سنيكرز","Sneakers","Spor ayakkabı","Zapatillas","Sneaker","Sneakers"),
    WardrobePiece("boots","بوت","Boots","Bot","Botas","Stiefel","Stivali"),
    WardrobePiece("heels","كعب","Heels","Topuklu","Tacones","Absätze","Tacchi")
)

private fun wardrobeSuggestions(occasion:WardrobeOccasion, gender:WardrobeGender): List<WardrobeStyle> {
    val preferred = when(occasion) {
        WardrobeOccasion.Work -> listOf("business_executive","business_smart","formal_navy_suit","business_blazer")
        WardrobeOccasion.Wedding -> if(gender==WardrobeGender.Women) listOf("evening_gown","evening_black_dress","evening_red_dress") else listOf("formal_tuxedo","formal_three_piece","formal_black_suit")
        WardrobeOccasion.Travel -> listOf("business_smart","casual_bomber","summer_resort","casual_cardigan")
        WardrobeOccasion.Photo -> listOf("business_creative","formal_blazer","evening_premium","traditional_ottoman")
        WardrobeOccasion.Sport -> listOf("sport_gym","sport_running","sport_tracksuit","sport_windbreaker")
        WardrobeOccasion.Winter -> listOf("winter_wool_coat","winter_trench","winter_puffer","winter_turtleneck")
        WardrobeOccasion.Evening -> if(gender==WardrobeGender.Women) listOf("evening_gown","evening_red_dress","evening_gold") else listOf("evening_dinner_suit","evening_velvet","formal_tuxedo")
    }
    return preferred.mapNotNull { id -> WARDROBE_STYLES.firstOrNull { it.id==id } }.take(3)
}

private fun suggestedWardrobeStyle(occasion:WardrobeOccasion, gender:WardrobeGender): WardrobeStyle {
    val ids = when(occasion) {
        WardrobeOccasion.Work -> listOf("business_executive","business_smart","formal_navy_suit")
        WardrobeOccasion.Wedding -> if(gender==WardrobeGender.Women) listOf("evening_gown","evening_black_dress") else listOf("formal_tuxedo","formal_three_piece")
        WardrobeOccasion.Travel -> listOf("business_smart","casual_bomber","summer_resort")
        WardrobeOccasion.Photo -> listOf("business_creative","formal_blazer","evening_premium")
        WardrobeOccasion.Sport -> listOf("sport_gym","sport_running","sport_tracksuit")
        WardrobeOccasion.Winter -> listOf("winter_wool_coat","winter_trench","winter_puffer")
        WardrobeOccasion.Evening -> if(gender==WardrobeGender.Women) listOf("evening_gown","evening_red_dress") else listOf("evening_dinner_suit","evening_velvet")
    }
    return ids.firstNotNullOfOrNull { id -> WARDROBE_STYLES.firstOrNull { it.id==id } } ?: WARDROBE_STYLES.first()
}

private fun wardrobeGender(style: WardrobeStyle): WardrobeGender = when {
    style.id.contains("dress") || style.id.contains("gown") || style.id.contains("abaya") -> WardrobeGender.Women
    style.id.contains("suit") || style.id.contains("tuxedo") || style.id.contains("shirt_tie") || style.id.contains("thobe") || style.id.contains("galabeya") -> WardrobeGender.Men
    else -> WardrobeGender.Unisex
}

data class WardrobeStyle(
    val id:String, val category:WardrobeCategory,
    val ar:String,val en:String,val tr:String,val es:String,val de:String,val it:String
) { fun label(lang:AppLanguage)=tx(lang,ar,en,tr,es,de,it) }

private val WARDROBE_STYLES = listOf(
    WardrobeStyle("formal_black_suit",WardrobeCategory.Formal,"بدلة سوداء كلاسيكية","Classic black suit","Klasik siyah takım","Traje negro clásico","Klassischer schwarzer Anzug","Abito nero classico"),
    WardrobeStyle("formal_navy_suit",WardrobeCategory.Formal,"بدلة كحلي","Navy suit","Lacivert takım","Traje azul marino","Marineblauer Anzug","Abito blu navy"),
    WardrobeStyle("formal_grey_suit",WardrobeCategory.Formal,"بدلة رمادية","Grey suit","Gri takım","Traje gris","Grauer Anzug","Abito grigio"),
    WardrobeStyle("formal_tuxedo",WardrobeCategory.Formal,"سموكن أسود","Black tuxedo","Siyah smokin","Esmoquin negro","Schwarzer Smoking","Smoking nero"),
    WardrobeStyle("formal_white_shirt",WardrobeCategory.Formal,"قميص أبيض رسمي","White dress shirt","Beyaz gömlek","Camisa blanca formal","Weißes Businesshemd","Camicia bianca elegante"),
    WardrobeStyle("formal_blazer",WardrobeCategory.Formal,"بليزر فاخر","Luxury blazer","Şık blazer","Blazer elegante","Eleganter Blazer","Blazer elegante"),
    WardrobeStyle("formal_beige_suit",WardrobeCategory.Formal,"بدلة بيج","Beige suit","Bej takım","Traje beige","Beiger Anzug","Abito beige"),
    WardrobeStyle("formal_pinstripe",WardrobeCategory.Formal,"بدلة مقلمة","Pinstripe suit","Çizgili takım","Traje de rayas","Nadelstreifenanzug","Abito gessato"),
    WardrobeStyle("formal_three_piece",WardrobeCategory.Formal,"بدلة ثلاث قطع","Three-piece suit","Üç parçalı takım","Traje de tres piezas","Dreiteiliger Anzug","Abito tre pezzi"),

    WardrobeStyle("casual_white_tee",WardrobeCategory.Casual,"تيشيرت أبيض","White T-shirt","Beyaz tişört","Camiseta blanca","Weißes T-Shirt","T-shirt bianca"),
    WardrobeStyle("casual_black_tee",WardrobeCategory.Casual,"تيشيرت أسود","Black T-shirt","Siyah tişört","Camiseta negra","Schwarzes T-Shirt","T-shirt nera"),
    WardrobeStyle("casual_denim_jacket",WardrobeCategory.Casual,"جاكيت جينز","Denim jacket","Kot ceket","Chaqueta vaquera","Jeansjacke","Giacca di jeans"),
    WardrobeStyle("casual_hoodie",WardrobeCategory.Casual,"هودي","Hoodie","Kapüşonlu","Sudadera con capucha","Hoodie","Felpa con cappuccio"),
    WardrobeStyle("casual_polo",WardrobeCategory.Casual,"بولو","Polo shirt","Polo yaka","Polo","Poloshirt","Polo"),
    WardrobeStyle("casual_flannel",WardrobeCategory.Casual,"قميص كاروهات","Flannel shirt","Kareli gömlek","Camisa de franela","Flanellhemd","Camicia di flanella"),
    WardrobeStyle("casual_bomber",WardrobeCategory.Casual,"جاكيت بومبر","Bomber jacket","Bomber ceket","Chaqueta bomber","Bomberjacke","Giubbotto bomber"),
    WardrobeStyle("casual_cardigan",WardrobeCategory.Casual,"كارديجان","Cardigan","Hırka","Cárdigan","Cardigan","Cardigan"),
    WardrobeStyle("casual_chino",WardrobeCategory.Casual,"تشينو أنيق","Smart chino look","Şık chino","Look chino elegante","Smarter Chino-Look","Look chino elegante"),

    WardrobeStyle("sport_tracksuit",WardrobeCategory.Sport,"تريننج رياضي","Tracksuit","Eşofman","Chándal","Trainingsanzug","Tuta sportiva"),
    WardrobeStyle("sport_gym",WardrobeCategory.Sport,"ملابس جيم","Gym outfit","Spor salonu kıyafeti","Ropa de gimnasio","Gym-Outfit","Completo palestra"),
    WardrobeStyle("sport_running",WardrobeCategory.Sport,"ملابس جري","Running outfit","Koşu kıyafeti","Ropa de running","Lauf-Outfit","Completo running"),
    WardrobeStyle("sport_football",WardrobeCategory.Sport,"طقم كرة قدم","Football kit","Futbol forması","Equipación de fútbol","Fußballtrikot","Completo da calcio"),
    WardrobeStyle("sport_tennis",WardrobeCategory.Sport,"ملابس تنس","Tennis outfit","Tenis kıyafeti","Ropa de tenis","Tennis-Outfit","Completo tennis"),
    WardrobeStyle("sport_basketball",WardrobeCategory.Sport,"طقم كرة سلة","Basketball kit","Basketbol forması","Equipación de baloncesto","Basketball-Outfit","Completo basket"),
    WardrobeStyle("sport_cycling",WardrobeCategory.Sport,"ملابس دراجات","Cycling outfit","Bisiklet kıyafeti","Ropa de ciclismo","Rad-Outfit","Completo ciclismo"),
    WardrobeStyle("sport_yoga",WardrobeCategory.Sport,"ملابس يوغا","Yoga outfit","Yoga kıyafeti","Ropa de yoga","Yoga-Outfit","Completo yoga"),
    WardrobeStyle("sport_windbreaker",WardrobeCategory.Sport,"جاكيت رياضي خفيف","Sport windbreaker","Spor rüzgarlığı","Cortavientos deportivo","Sport-Windbreaker","Giacca antivento sportiva"),

    WardrobeStyle("summer_linen",WardrobeCategory.Summer,"قميص كتان","Linen shirt","Keten gömlek","Camisa de lino","Leinenhemd","Camicia di lino"),
    WardrobeStyle("summer_hawaiian",WardrobeCategory.Summer,"قميص صيفي مطبوع","Printed summer shirt","Desenli yaz gömleği","Camisa veraniega estampada","Bedrucktes Sommerhemd","Camicia estiva stampata"),
    WardrobeStyle("summer_resort",WardrobeCategory.Summer,"إطلالة منتجع","Resort outfit","Tatil kombini","Look de resort","Resort-Look","Look da resort"),
    WardrobeStyle("summer_light_dress",WardrobeCategory.Summer,"فستان صيفي خفيف","Light summer dress","Hafif yaz elbisesi","Vestido ligero de verano","Leichtes Sommerkleid","Abito estivo leggero"),
    WardrobeStyle("summer_polo",WardrobeCategory.Summer,"بولو صيفي","Summer polo","Yazlık polo","Polo de verano","Sommer-Polo","Polo estiva"),
    WardrobeStyle("summer_shorts",WardrobeCategory.Summer,"إطلالة شورت صيفي","Summer shorts look","Yazlık şort kombini","Look con shorts","Sommer-Shorts-Look","Look con shorts estivi"),
    WardrobeStyle("summer_white",WardrobeCategory.Summer,"إطلالة بيضاء","All-white summer look","Beyaz yaz kombini","Look blanco de verano","Weißer Sommer-Look","Look estivo bianco"),
    WardrobeStyle("summer_coastal",WardrobeCategory.Summer,"ستايل ساحلي","Coastal style","Sahil stili","Estilo costero","Küstenstil","Stile costiero"),
    WardrobeStyle("summer_safari",WardrobeCategory.Summer,"ستايل سفاري","Safari style","Safari stili","Estilo safari","Safari-Stil","Stile safari"),

    WardrobeStyle("winter_wool_coat",WardrobeCategory.Winter,"معطف صوف","Wool coat","Yün palto","Abrigo de lana","Wollmantel","Cappotto di lana"),
    WardrobeStyle("winter_puffer",WardrobeCategory.Winter,"جاكيت مبطن","Puffer jacket","Şişme mont","Chaqueta acolchada","Daunenjacke","Piumino"),
    WardrobeStyle("winter_trench",WardrobeCategory.Winter,"ترنش كوت","Trench coat","Trençkot","Gabardina","Trenchcoat","Trench"),
    WardrobeStyle("winter_turtleneck",WardrobeCategory.Winter,"هاي كول","Turtleneck","Boğazlı kazak","Jersey de cuello alto","Rollkragenpullover","Dolcevita"),
    WardrobeStyle("winter_leather",WardrobeCategory.Winter,"جاكيت جلد","Leather jacket","Deri ceket","Chaqueta de cuero","Lederjacke","Giacca di pelle"),
    WardrobeStyle("winter_parka",WardrobeCategory.Winter,"باركا","Parka","Parka","Parka","Parka","Parka"),
    WardrobeStyle("winter_scarf",WardrobeCategory.Winter,"معطف مع وشاح","Coat with scarf","Atkılı palto","Abrigo con bufanda","Mantel mit Schal","Cappotto con sciarpa"),
    WardrobeStyle("winter_knit",WardrobeCategory.Winter,"كنزة شتوية","Winter knit","Kışlık kazak","Jersey de invierno","Winterstrick","Maglione invernale"),
    WardrobeStyle("winter_shearling",WardrobeCategory.Winter,"جاكيت فرو داخلي","Shearling jacket","Kürklü ceket","Chaqueta de borrego","Lammfelljacke","Giacca shearling"),

    WardrobeStyle("traditional_galabeya",WardrobeCategory.Traditional,"جلابية مصرية","Egyptian galabeya","Mısır galabiyesi","Galabeya egipcia","Ägyptische Galabeya","Galabeya egiziana"),
    WardrobeStyle("traditional_thobe",WardrobeCategory.Traditional,"ثوب عربي","Arab thobe","Arap thobe","Thobe árabe","Arabischer Thobe","Thobe arabo"),
    WardrobeStyle("traditional_abaya",WardrobeCategory.Traditional,"عباية","Abaya","Abaya","Abaya","Abaya","Abaya"),
    WardrobeStyle("traditional_kaftan",WardrobeCategory.Traditional,"قفطان","Kaftan","Kaftan","Caftán","Kaftan","Caftano"),
    WardrobeStyle("traditional_turkish",WardrobeCategory.Traditional,"زي تركي تراثي","Traditional Turkish outfit","Geleneksel Türk kıyafeti","Traje turco tradicional","Traditionelle türkische Kleidung","Abito turco tradizionale"),
    WardrobeStyle("traditional_moroccan",WardrobeCategory.Traditional,"زي مغربي","Moroccan outfit","Fas kıyafeti","Traje marroquí","Marokkanisches Outfit","Abito marocchino"),
    WardrobeStyle("traditional_gulf",WardrobeCategory.Traditional,"زي خليجي","Gulf formal wear","Körfez resmi kıyafeti","Vestimenta formal del Golfo","Golf-Festkleidung","Abito formale del Golfo"),
    WardrobeStyle("traditional_ottoman",WardrobeCategory.Traditional,"زي عثماني","Ottoman-inspired outfit","Osmanlı esintili kıyafet","Traje inspirado en el otomano","Osmanisch inspiriertes Outfit","Abito ispirato all’Ottomano"),
    WardrobeStyle("traditional_folk",WardrobeCategory.Traditional,"زي شعبي أنيق","Elegant folk outfit","Şık halk kıyafeti","Traje folclórico elegante","Elegantes Trachten-Outfit","Abito folkloristico elegante"),

    WardrobeStyle("business_smart",WardrobeCategory.Business,"سمارت كاجوال للعمل","Smart business casual","Akıllı iş stili","Business casual elegante","Smart Business Casual","Business casual elegante"),
    WardrobeStyle("business_shirt_tie",WardrobeCategory.Business,"قميص وربطة عنق","Shirt and tie","Gömlek ve kravat","Camisa y corbata","Hemd und Krawatte","Camicia e cravatta"),
    WardrobeStyle("business_blazer",WardrobeCategory.Business,"بليزر عمل","Business blazer","İş blazerı","Blazer de negocios","Business-Blazer","Blazer business"),
    WardrobeStyle("business_tech",WardrobeCategory.Business,"ستايل تقني عصري","Modern tech professional","Modern teknoloji profesyoneli","Profesional tech moderno","Moderner Tech-Profi","Professionista tech moderno"),
    WardrobeStyle("business_creative",WardrobeCategory.Business,"ستايل عمل إبداعي","Creative professional","Yaratıcı profesyonel","Profesional creativo","Kreativer Profi","Professionista creativo"),
    WardrobeStyle("business_executive",WardrobeCategory.Business,"إطلالة تنفيذية","Executive look","Yönetici görünümü","Look ejecutivo","Executive-Look","Look executive"),
    WardrobeStyle("business_corporate",WardrobeCategory.Business,"ملابس شركات","Corporate attire","Kurumsal kıyafet","Ropa corporativa","Business-Kleidung","Abbigliamento aziendale"),
    WardrobeStyle("business_startup",WardrobeCategory.Business,"ستايل شركة ناشئة","Startup professional","Startup profesyoneli","Profesional de startup","Startup-Profi","Professionista startup"),
    WardrobeStyle("business_consultant",WardrobeCategory.Business,"ستايل مستشار","Consultant look","Danışman görünümü","Look de consultor","Berater-Look","Look consulente"),

    WardrobeStyle("evening_black_dress",WardrobeCategory.Evening,"فستان سهرة أسود","Black evening dress","Siyah gece elbisesi","Vestido negro de noche","Schwarzes Abendkleid","Abito da sera nero"),
    WardrobeStyle("evening_red_dress",WardrobeCategory.Evening,"فستان سهرة أحمر","Red evening dress","Kırmızı gece elbisesi","Vestido rojo de noche","Rotes Abendkleid","Abito da sera rosso"),
    WardrobeStyle("evening_gown",WardrobeCategory.Evening,"فستان طويل فاخر","Elegant evening gown","Şık gece elbisesi","Vestido de gala elegante","Elegantes Abendkleid","Abito da sera elegante"),
    WardrobeStyle("evening_dinner_suit",WardrobeCategory.Evening,"بدلة عشاء","Dinner suit","Gece takımı","Traje de cena","Dinner-Jacket-Look","Abito da cena"),
    WardrobeStyle("evening_velvet",WardrobeCategory.Evening,"بليزر مخمل","Velvet blazer","Kadife blazer","Blazer de terciopelo","Samtblazer","Blazer in velluto"),
    WardrobeStyle("evening_cocktail",WardrobeCategory.Evening,"إطلالة كوكتيل","Cocktail outfit","Kokteyl kıyafeti","Look de cóctel","Cocktail-Outfit","Look cocktail"),
    WardrobeStyle("evening_gold",WardrobeCategory.Evening,"إطلالة ذهبية","Elegant gold look","Şık altın görünüm","Look dorado elegante","Eleganter Gold-Look","Look oro elegante"),
    WardrobeStyle("evening_silver",WardrobeCategory.Evening,"إطلالة فضية","Elegant silver look","Şık gümüş görünüm","Look plateado elegante","Eleganter Silber-Look","Look argento elegante"),
    WardrobeStyle("evening_premium",WardrobeCategory.Evening,"إطلالة مناسبة فاخرة","Premium event outfit","Premium etkinlik kıyafeti","Look premium para evento","Premium-Event-Look","Look premium da evento")
)

data class AiActivity(
    val effect: String,
    val requestId: String,
    val processMs: Long?,
    val cacheState: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun FaceLabApp(activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var effect by remember { mutableStateOf<LocalEffect>(LocalEffect.Original) }
    var tool by remember { mutableStateOf<SmartTool>(SmartTool.None) }
    var category by remember { mutableStateOf<ToolCategory>(ToolCategory.Age) }
    var preset by remember { mutableStateOf<EditPreset>(EditPreset.Balanced) }
    var before by remember { mutableStateOf<Boolean>(false) }
    val prefs = remember { activity.getSharedPreferences("facelab_ui", android.content.Context.MODE_PRIVATE) }
    var language by remember {
        mutableStateOf<AppLanguage>(AppLanguage.entries.firstOrNull { it.code == prefs.getString("language", "ar") } ?: AppLanguage.Arabic)
    }
    var showLanguageDialog by remember { mutableStateOf<Boolean>(false) }
    var intensity by remember { mutableFloatStateOf(EditPreset.Balanced.strength) }
    var busy by remember { mutableStateOf<Boolean>(false) }
    var faceDetected by remember { mutableStateOf<Boolean?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var serverState by remember { mutableStateOf<ServerState>(ServerState.Unknown) }
    var serverDetails by remember { mutableStateOf<String>("") }
    val undoStack = remember { mutableStateListOf<Bitmap>() }
    val redoStack = remember { mutableStateListOf<Bitmap>() }
    val aiActivity = remember { mutableStateListOf<AiActivity>() }
    var lastAiMeta by remember { mutableStateOf<FaceEditResult?>(null) }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Editor) }
    var compareSplit by remember { mutableFloatStateOf(.5f) }
    val sessionGallery = remember { mutableStateListOf<Bitmap>() }
    var modelsDetails by remember { mutableStateOf<String>("") }
    var garmentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var heritageBackground by remember { mutableStateOf<Bitmap?>(null) }
    var heritageSite by remember { mutableStateOf<HeritageSite>(HERITAGE_SITES.first()) }
    var heritageMode by remember { mutableStateOf<HeritageMode>(HeritageMode.Portrait) }
    var heritageLight by remember { mutableStateOf<HeritageLight>(HeritageLight.Day) }
    var wardrobeCategory by remember { mutableStateOf<WardrobeCategory>(WardrobeCategory.Formal) }
    var wardrobeStyle by remember { mutableStateOf<WardrobeStyle>(WARDROBE_STYLES.first()) }
    var wardrobeColor by remember { mutableStateOf<WardrobeColor>(WardrobeColor.Auto) }
    var wardrobeAccessory by remember { mutableStateOf<WardrobeAccessory>(WardrobeAccessory.None) }

    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { loadPersistentGallery(activity) }
        sessionGallery.clear()
        sessionGallery.addAll(saved)
    }

    fun remoteEffectName(selected: SmartTool): String? = when (selected) {
        SmartTool.Younger -> "younger"; SmartTool.Older -> "older"; SmartTool.Beard -> "beard"
        SmartTool.RemoveBeard -> "remove_beard"; SmartTool.Hair -> "hair"; SmartTool.HairColor -> "hair_color"
        SmartTool.Smile -> "smile"; SmartTool.Makeup -> "makeup"; SmartTool.EyeColor -> "eye_color"
        SmartTool.Teeth -> "teeth_whiten"; SmartTool.Smooth -> "skin_smooth"
        SmartTool.BackgroundBlur -> "background_blur"; SmartTool.BackgroundRemove -> "background_remove"
        SmartTool.EnhanceAI -> "enhance"; SmartTool.Relight -> "relight"; SmartTool.BlemishFix -> "blemish_fix"
        SmartTool.EyeEnhance -> "eye_enhance"; SmartTool.Headshot -> "headshot"; SmartTool.BackgroundStudio -> "background_studio"; else -> null
    }

    fun rememberCurrentForUndo() {
        resultBitmap?.let { current ->
            undoStack.add(current.copy(Bitmap.Config.ARGB_8888, false))
            while (undoStack.size > 12) undoStack.removeAt(0)
            redoStack.clear()
        }
    }

    fun rerender() {
        val source = originalBitmap ?: return
        scope.launch {
            busy = true
            val rendered = withContext(Dispatchers.Default) {
                renderBitmap(source, effect, tool, intensity).also { faceDetected = detectFaceBox(source) != null }
            }
            resultBitmap = rendered
            busy = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val bmp = withContext(Dispatchers.IO) { loadBitmap(activity, uri) }
            originalBitmap = bmp; resultBitmap = bmp; referenceBitmap = null
            effect = LocalEffect.Original; tool = SmartTool.None; preset = EditPreset.Balanced
            intensity = preset.strength; faceDetected = bmp?.let { detectFaceBox(it) != null }
            undoStack.clear(); redoStack.clear(); aiActivity.clear(); lastAiMeta = null; before = false; busy = false
        }
    }

    val smartCamera = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val name = result.data?.getStringExtra(SmartCameraActivity.CAPTURE_FILE)
        if (result.resultCode == android.app.Activity.RESULT_OK && name != null) {
            val file = CaptureFiles.resolve(File(activity.filesDir, "camera_pending"), name)
            if (file != null && file.isFile) scope.launch {
                busy = true
                try {
                    val bmp = withContext(Dispatchers.IO) { loadBitmap(activity, Uri.fromFile(file)) }
                    if (bmp != null) {
                        originalBitmap = bmp; resultBitmap = bmp; referenceBitmap = null
                        effect = LocalEffect.Original; tool = SmartTool.None; preset = EditPreset.Balanced
                        intensity = preset.strength; before = false; screen = AppScreen.Editor
                        undoStack.clear(); redoStack.clear(); aiActivity.clear(); lastAiMeta = null
                        faceDetected = withContext(Dispatchers.Default) { detectFaceBox(bmp) != null }
                        withContext(Dispatchers.IO) { persistGalleryBitmap(activity, bmp) }
                        sessionGallery.add(0, bmp)
                        while (sessionGallery.size > 30) sessionGallery.removeAt(sessionGallery.lastIndex)
                    } else message = if (language == AppLanguage.Arabic) "تعذر قراءة الصورة" else "Could not read photo"
                } finally {
                    withContext(Dispatchers.IO) { file.delete() }
                    busy = false
                }
            }
        }
    }

    val referencePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            referenceBitmap = withContext(Dispatchers.IO) { loadBitmap(activity, uri) }
            message = if (referenceBitmap != null) (tx(language,"تم اختيار صورة تسريحة مرجعية","Reference hairstyle selected","Referans saç modeli seçildi","Peinado de referencia seleccionado","Referenzfrisur ausgewählt","Acconciatura di riferimento selezionata"))
            else (tx(language,"تعذر قراءة الصورة المرجعية","Could not read reference image","Referans görüntü okunamadı","No se pudo leer la imagen de referencia","Referenzbild konnte nicht gelesen werden","Impossibile leggere l’immagine di riferimento"))
        }
    }

    val garmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            garmentBitmap = withContext(Dispatchers.IO) { loadBitmap(activity, uri) }
            message = if (garmentBitmap != null) tx(language,"تم استيراد قطعة الملابس ✓","Garment imported ✓","Giysi içe aktarıldı ✓","Prenda importada ✓","Kleidungsstück importiert ✓","Capo importato ✓") else tx(language,"تعذر قراءة صورة الملابس","Could not read garment image","Giysi görüntüsü okunamadı","No se pudo leer la prenda","Kleidungsbild konnte nicht gelesen werden","Impossibile leggere il capo")
        }
    }
    val heritagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { heritageBackground = activity.contentResolver.openInputStream(it)?.use { input -> BitmapFactory.decodeStream(input) } }
    }

    LaunchedEffect(effect, tool, intensity, originalBitmap) { if (originalBitmap != null) rerender() }

    val scheme = darkColorScheme(
        primary = Color(0xFF00E5FF), secondary = Color(0xFF1479FF), tertiary = Color(0xFFFFC857),
        background = Color(0xFF020813), surface = Color(0xFF06182D), surfaceVariant = Color(0xFF0B2038),
        onBackground = Color(0xFFF4FBFF), onSurface = Color(0xFFF4FBFF), onPrimary = Color(0xFF001018)
    )

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalLayoutDirection provides if (language.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        BackHandler {
            if (screen != AppScreen.Editor) screen = AppScreen.Editor else activity.finish()
        }
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color(0xFF020813),
            bottomBar = { FaceLabBottomBar(screen, language, onBack = { if (screen != AppScreen.Editor) screen = AppScreen.Editor else activity.finish() }) { screen = it } }
        ) { pad ->
            if (screen == AppScreen.Editor) Column(
                modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProTopBar(language, serverState, onLanguage = { showLanguageDialog = true })
                androidx.compose.material3.Button(onClick = { smartCamera.launch(Intent(activity, SmartCameraActivity::class.java).putExtra("language", language.code)) }) {
                    androidx.compose.material3.Text("Smart Camera / الكاميرا الذكية")
                }
                Spacer(Modifier.height(10.dp))
                if (!before && originalBitmap != null && resultBitmap != null) {
                    ComparePreviewCard(originalBitmap!!, resultBitmap!!, compareSplit, language, busy, faceDetected,
                        onSplitChange = { compareSplit = it }, onPick = { picker.launch("image/*") })
                } else {
                    PreviewCard(originalBitmap, language, busy, faceDetected) { picker.launch("image/*") }
                }
                Spacer(Modifier.height(10.dp))

                if (originalBitmap != null) {
                    BeforeAfterPro(before, language) { before = it }
                    Spacer(Modifier.height(10.dp))
                    HistoryBar(language, undoStack.isNotEmpty(), redoStack.isNotEmpty(),
                        onUndo = {
                            if (undoStack.isNotEmpty()) {
                                resultBitmap?.let { redoStack.add(it) }
                                resultBitmap = undoStack.removeAt(undoStack.lastIndex); before = false
                            }
                        },
                        onRedo = {
                            if (redoStack.isNotEmpty()) {
                                resultBitmap?.let { undoStack.add(it) }
                                resultBitmap = redoStack.removeAt(redoStack.lastIndex); before = false
                            }
                        })
                    Spacer(Modifier.height(12.dp))

                    SectionCard(title = tx(language,"1. اختر مجموعة التعديل","1. Choose editing group","1. Düzenleme grubunu seç","1. Elige el grupo de edición","1. Bearbeitungsgruppe wählen","1. Scegli il gruppo di modifica")) {
                        CategoryStrip(category, language) { category = it; tool = SmartTool.None }
                    }
                    Spacer(Modifier.height(10.dp))

                    SectionCard(title = tx(language,"2. اختر الأداة","2. Select tool","2. Aracı seç","2. Selecciona la herramienta","2. Werkzeug wählen","2. Scegli lo strumento")) {
                        SmartToolsGrid(category, tool, language) { tool = it }
                        if (tool == SmartTool.Hair) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { referencePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(7.dp))
                                Text(if (referenceBitmap == null) tx(language,"إضافة صورة مرجعية للتسريحة","Add hairstyle reference","Saç modeli referansı ekle","Añadir referencia de peinado","Frisur-Referenz hinzufügen","Aggiungi riferimento acconciatura")
                                else tx(language,"تم اختيار المرجع ✓","Reference selected ✓","Referans seçildi ✓","Referencia seleccionada ✓","Referenz ausgewählt ✓","Riferimento selezionato ✓"))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    SectionCard(title = tx(language,"3. النمط وقوة التأثير","3. Style & strength","3. Stil ve güç","3. Estilo e intensidad","3. Stil & Stärke","3. Stile e intensità")) {
                        PresetStrip(preset, language) { selected -> preset = selected; intensity = selected.strength }
                        Spacer(Modifier.height(6.dp)); IntensityControl(intensity, language) { intensity = it }
                        Spacer(Modifier.height(6.dp)); EffectStrip(effect, language) { effect = it }
                    }
                    Spacer(Modifier.height(10.dp))

                    val remoteName = remoteEffectName(tool)
                    Button(
                        onClick = {
                            val src = originalBitmap
                            if (src == null || remoteName == null) message = tx(language,"اختر أداة ذكاء اصطناعي أولًا","Choose an AI tool first","Önce bir AI aracı seçin","Elige primero una herramienta de IA","Zuerst ein KI-Werkzeug wählen","Scegli prima uno strumento IA")
                            else scope.launch {
                                busy = true; message = tx(language,"جاري تشغيل الذكاء الاصطناعي الخاص…","Running private self-hosted AI…","Özel yerel AI çalışıyor…","Ejecutando IA privada autoalojada…","Private selbst gehostete KI läuft…","IA privata self-hosted in esecuzione…")
                                val apiResult = FaceApiClient.edit(src, remoteName, intensity, if (tool == SmartTool.Hair) referenceBitmap else null)
                                apiResult.onSuccess { meta ->
                                    rememberCurrentForUndo(); resultBitmap = meta.bitmap; before = false; serverState = ServerState.Online
                                    lastAiMeta = meta
                                    val galleryCopy = meta.bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    sessionGallery.add(0, galleryCopy)
                                    while (sessionGallery.size > 30) sessionGallery.removeAt(sessionGallery.lastIndex)
                                    withContext(Dispatchers.IO) { persistGalleryBitmap(activity, galleryCopy) }
                                    aiActivity.add(0, AiActivity(remoteName, meta.requestId, meta.processMs, meta.cacheState))
                                    while (aiActivity.size > 8) aiActivity.removeAt(aiActivity.lastIndex)
                                    val timing = meta.processMs?.let { " • ${it}ms" }.orEmpty()
                                    message = tx(language,"اكتمل تعديل الذكاء الاصطناعي$timing","AI edit completed$timing","AI düzenleme tamamlandı$timing","Edición IA completada$timing","KI-Bearbeitung abgeschlossen$timing","Modifica IA completata$timing")
                                }.onFailure {
                                    serverState = ServerState.Offline; message = tx(language,"خادم AI: ","AI server: ","AI sunucusu: ","Servidor IA: ","KI-Server: ","Server IA: ") + (it.message ?: "error")
                                }
                                busy = false
                            }
                        },
                        enabled = !busy && remoteName != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text(tx(language,"تنفيذ التعديل بالذكاء الاصطناعي","RUN AI EDIT","AI DÜZENLEMEYİ ÇALIŞTIR","EJECUTAR EDICIÓN IA","KI-BEARBEITUNG STARTEN","ESEGUI MODIFICA IA"), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(10.dp))

                    ServerPanel(serverState, serverDetails, language, onCheck = {
                        scope.launch {
                            serverState = ServerState.Checking
                            val health = FaceApiClient.health(); val caps = FaceApiClient.capabilities(); val runtime = FaceApiClient.runtime(); val models = FaceApiClient.models(); val storage = FaceApiClient.storage(); val system = FaceApiClient.system()
                            modelsDetails = listOf(models.getOrElse { "" }, storage.getOrElse { "" }, system.getOrElse { "" }).filter { it.isNotBlank() }.joinToString("\n\n")
                            if (health.isSuccess) {
                                serverState = ServerState.Online
                                serverDetails = when {
                                    runtime.isSuccess && caps.isSuccess -> tx(language,"متصل • الحالة والقدرات جاهزة","Connected • runtime + capabilities ready","Bağlı • çalışma zamanı ve yetenekler hazır","Conectado • estado y capacidades listos","Verbunden • Laufzeit und Fähigkeiten bereit","Connesso • runtime e capacità pronte")
                                    caps.isSuccess -> tx(language,"متصل • قدرات المحرك متاحة","Connected • capabilities available","Bağlı • motor yetenekleri kullanılabilir","Conectado • capacidades disponibles","Verbunden • Fähigkeiten verfügbar","Connesso • capacità disponibili")
                                    else -> tx(language,"متصل","Connected","Bağlı","Conectado","Verbunden","Connesso")
                                }
                            } else {
                                serverState = ServerState.Offline
                                serverDetails = health.exceptionOrNull()?.message ?: "Offline"
                            }
                        }
                    })
                    Spacer(Modifier.height(10.dp))

                    lastAiMeta?.let { meta ->
                        AiResultMetaCard(meta, language)
                        Spacer(Modifier.height(10.dp))
                    }
                    if (aiActivity.isNotEmpty()) {
                        AiActivityCard(aiActivity, language)
                        Spacer(Modifier.height(10.dp))
                    }

                    ActionRow(language,
                        onSave = { resultBitmap?.let { bmp -> scope.launch { val uri = withContext(Dispatchers.IO) { saveBitmap(activity, bmp) }; message = if (uri != null) (tx(language,"تم الحفظ في Pictures/FaceLab","Saved to Pictures/FaceLab","Pictures/FaceLab klasörüne kaydedildi","Guardado en Pictures/FaceLab","In Pictures/FaceLab gespeichert","Salvato in Pictures/FaceLab")) else (tx(language,"تعذر الحفظ","Save failed","Kaydetme başarısız","Error al guardar","Speichern fehlgeschlagen","Salvataggio non riuscito")) } } },
                        onShare = { resultBitmap?.let { bmp -> scope.launch { val uri = withContext(Dispatchers.IO) { saveBitmap(activity, bmp) }; if (uri != null) { val intent = Intent(Intent.ACTION_SEND).apply { type="image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; activity.startActivity(Intent.createChooser(intent, tx(language,"مشاركة الصورة","Share image","Görseli paylaş","Compartir imagen","Bild teilen","Condividi immagine"))) } } } },
                        onReset = { rememberCurrentForUndo(); resultBitmap = originalBitmap; effect = LocalEffect.Original; tool = SmartTool.None; referenceBitmap = null; preset = EditPreset.Balanced; intensity = preset.strength; before = false }
                    )
                }

                message?.let { Spacer(Modifier.height(10.dp)); StatusMessage(it) }
                Spacer(Modifier.height(18.dp))
                Text(tx(language,"NEXVARY FaceLab • محرر صور ذكي خاص","NEXVARY FaceLab • Private AI Portrait Editor","NEXVARY FaceLab • Özel AI Portre Editörü","NEXVARY FaceLab • Editor privado de retratos con IA","NEXVARY FaceLab • Privater KI-Porträteditor","NEXVARY FaceLab • Editor ritratti IA privato"), color = Color(0xFF8293AA), fontSize = 10.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
            } else {
                when (screen) {
                    AppScreen.Wardrobe -> WardrobeScreen(originalBitmap, resultBitmap, garmentBitmap, wardrobeCategory, wardrobeStyle, wardrobeColor, wardrobeAccessory, intensity, busy, language, Modifier.padding(pad),
                        onPickPerson = { picker.launch("image/*") }, onPickGarment = { garmentPicker.launch("image/*") },
                        onCategory = { wardrobeCategory = it; wardrobeStyle = WARDROBE_STYLES.first { s -> s.category == it } },
                        onStyle = { wardrobeStyle = it }, onColor = { wardrobeColor = it }, onAccessory = { wardrobeAccessory = it }, onStrength = { intensity = it },
                        onRun = { occasionCode, outfitPrompt ->
                            val src = originalBitmap
                            if (src == null) message = tx(language,"اختر صورة شخص أولًا","Choose a person photo first","Önce kişi fotoğrafı seçin","Elige primero una foto de persona","Wählen Sie zuerst ein Personenfoto","Scegli prima una foto della persona")
                            else scope.launch {
                                busy = true; message = tx(language,"جاري تغيير الملابس…","Changing outfit…","Kıyafet değiştiriliyor…","Cambiando ropa…","Outfit wird geändert…","Cambio abito in corso…")
                                FaceApiClient.tryOn(src, wardrobeStyle.id, wardrobeCategory.name.lowercase(), intensity, garmentBitmap, wardrobeColor.code, wardrobeAccessory.code, occasionCode, outfitPrompt).onSuccess { meta ->
                                    rememberCurrentForUndo(); resultBitmap = meta.bitmap; before = false; lastAiMeta = meta; serverState = ServerState.Online
                                    val copy = meta.bitmap.copy(Bitmap.Config.ARGB_8888, false); sessionGallery.add(0, copy); while(sessionGallery.size>30) sessionGallery.removeAt(sessionGallery.lastIndex); withContext(Dispatchers.IO){persistGalleryBitmap(activity,copy)}
                                    aiActivity.add(0, AiActivity("wardrobe:${wardrobeStyle.id}",meta.requestId,meta.processMs,meta.cacheState)); while(aiActivity.size>8) aiActivity.removeAt(aiActivity.lastIndex)
                                    message = tx(language,"تم تغيير الملابس بنجاح","Outfit changed successfully","Kıyafet başarıyla değiştirildi","Ropa cambiada correctamente","Outfit erfolgreich geändert","Abito cambiato con successo")
                                }.onFailure { serverState=ServerState.Offline; message=tx(language,"خادم الملابس: ","Wardrobe server: ","Giyim sunucusu: ","Servidor de ropa: ","Wardrobe-Server: ","Server guardaroba: ")+(it.message?:"error") }
                                busy=false
                            }
                        })
                    AppScreen.Heritage -> HeritageScreen(originalBitmap, resultBitmap, heritageBackground, heritageSite, heritageMode, heritageLight, intensity, busy, language, Modifier.padding(pad),
                        onPickPerson={picker.launch("image/*")}, onPickBackground={heritagePicker.launch("image/*")}, onSite={heritageSite=it}, onMode={heritageMode=it}, onLight={heritageLight=it}, onStrength={intensity=it},
                        onRun={
                            val src=originalBitmap; val bg=heritageBackground
                            if(src==null) message=tx(language,"اختر صورة الشخص أولًا","Choose a person photo first","Önce kişi fotoğrafı seçin","Elige primero una foto","Wählen Sie zuerst ein Personenfoto","Scegli prima una foto")
                            else if(bg==null) message=tx(language,"اختر صورة خلفية للمعلم من الحزمة المرخصة أو الهاتف","Choose a licensed landmark background image","Lisanslı bir arka plan seçin","Elige un fondo con licencia","Wählen Sie einen lizenzierten Hintergrund","Scegli uno sfondo con licenza")
                            else scope.launch { busy=true; message=tx(language,"جاري دمج الخلفية ومطابقة الإضاءة…","Compositing background and matching light…","Arka plan ve ışık eşleştiriliyor…","Componiendo fondo e iluminación…","Hintergrund und Licht werden angepasst…","Composizione sfondo e luce…")
                                FaceApiClient.heritage(src,bg,heritageSite.id,heritageMode.name.lowercase(),heritageLight.name.lowercase(),intensity).onSuccess{meta->rememberCurrentForUndo();resultBitmap=meta.bitmap;before=false;lastAiMeta=meta;serverState=ServerState.Online;val copy=meta.bitmap.copy(Bitmap.Config.ARGB_8888,false);sessionGallery.add(0,copy);while(sessionGallery.size>30)sessionGallery.removeAt(sessionGallery.lastIndex);withContext(Dispatchers.IO){persistGalleryBitmap(activity,copy)};message=tx(language,"تم تركيب الخلفية","Heritage background applied","Arka plan uygulandı","Fondo aplicado","Hintergrund angewendet","Sfondo applicato")}.onFailure{message=(it.message?:"error")};busy=false }
                        })
                    AppScreen.Film -> FilmStudioScreen(resultBitmap ?: originalBitmap, language, Modifier.padding(pad))
                    AppScreen.Gallery -> GalleryScreen(sessionGallery, language, Modifier.padding(pad), onOpen = { originalBitmap = it; resultBitmap = it; faceDetected = detectFaceBox(it) != null; screen = AppScreen.Editor; before = false }, onClear = { scope.launch { withContext(Dispatchers.IO) { clearPersistentGallery(activity) }; sessionGallery.clear() } })
                    AppScreen.Models -> ModelsScreen(serverState, serverDetails, modelsDetails, language, Modifier.padding(pad)) {
                        scope.launch {
                            serverState = ServerState.Checking
                            val h = FaceApiClient.health(); val c = FaceApiClient.capabilities(); val m = FaceApiClient.models(); val st = FaceApiClient.storage(); val sy = FaceApiClient.system()
                            modelsDetails = listOf(m.getOrElse { "" }, st.getOrElse { "" }, sy.getOrElse { "" }).filter { it.isNotBlank() }.joinToString("\n\n")
                            serverState = if (h.isSuccess) ServerState.Online else ServerState.Offline
                            serverDetails = if (h.isSuccess && c.isSuccess) tx(language,"الخادم متصل والنماذج قابلة للفحص","Server online and model capabilities available","Sunucu çevrimiçi ve model yetenekleri kullanılabilir","Servidor conectado y capacidades disponibles","Server online und Modellfähigkeiten verfügbar","Server online e capacità modelli disponibili") else (h.exceptionOrNull()?.message ?: "Offline")
                        }
                    }
                    AppScreen.Settings -> SettingsScreen(language, serverState, Modifier.padding(pad), onLanguage = { showLanguageDialog = true })
                    AppScreen.About -> AboutScreen(language, Modifier.padding(pad))
                    AppScreen.Editor -> Unit
                }
            }
        }
        if (showLanguageDialog) LanguagePickerDialog(language, onDismiss = { showLanguageDialog = false }, onSelect = { language = it; prefs.edit().putString("language", it.code).apply(); showLanguageDialog = false })
        }
    }
}

@Composable private fun ProTopBar(language: AppLanguage, state: ServerState, onLanguage: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(top=8.dp),
        shape=RoundedCornerShape(24.dp),
        color=Color(0xFF041326),
        border=BorderStroke(2.dp, Color(0xFFD9E7F2))
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=RoundedCornerShape(16.dp), color=Color(0xFF020813), border=BorderStroke(1.dp,Color(0xFF00E5FF))) {
                Image(painterResource(R.drawable.facelab_icon_electric), null, Modifier.size(58.dp).padding(3.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NEXVARY FaceLab", fontWeight=FontWeight.Black, fontSize=23.sp, color=Color.White, maxLines=1, overflow=TextOverflow.Ellipsis)
                Text(tx(language,"استوديو احترافي لتحرير الوجوه","AI Portrait Studio","AI Portre Stüdyosu","Estudio de retratos IA","KI-Porträtstudio","Studio ritratti IA","Studio de portraits IA","AI پورٹریٹ اسٹوڈیو","استودیوی پرتره AI"), color=Color(0xFF72F1FF), fontSize=12.sp, maxLines=2, overflow=TextOverflow.Ellipsis)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(if(state==ServerState.Online) Color(0xFF43FFB1) else if(state==ServerState.Offline) Color(0xFFFF5D73) else Color(0xFFFFC857), CircleShape))
                    Spacer(Modifier.width(5.dp)); Text("AI", color=Color(0xFFFFC857), fontSize=10.sp, fontWeight=FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick=onLanguage,
                border=BorderStroke(1.5.dp, Color(0xFFD9E7F2)),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFF72F1FF)),
                contentPadding=PaddingValues(horizontal=10.dp, vertical=8.dp)
            ) {
                Text(languageFlag(language), fontSize=20.sp); Spacer(Modifier.width(5.dp)); Text(language.short, fontWeight=FontWeight.Black)
            }
        }
    }
}

private fun languageFlag(language: AppLanguage): String = when(language) {
    AppLanguage.Arabic -> "🇪🇬"
    AppLanguage.English -> "🇬🇧"
    AppLanguage.Turkish -> "🇹🇷"
    AppLanguage.Spanish -> "🇪🇸"
    AppLanguage.German -> "🇩🇪"
    AppLanguage.Italian -> "🇮🇹"
    AppLanguage.French -> "🇫🇷"
    AppLanguage.Urdu -> "🇵🇰"
    AppLanguage.Persian -> "🇮🇷"
}

@Composable private fun LanguagePickerDialog(selected: AppLanguage, onDismiss:()->Unit, onSelect:(AppLanguage)->Unit) {
    Dialog(onDismissRequest=onDismiss) {
        Surface(shape=RoundedCornerShape(26.dp), color=Color(0xFF030B18), border=BorderStroke(2.dp,Color(0xFFD9E7F2))) {
            Column(Modifier.heightIn(max=680.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                    Column {
                        Text(tx(selected,"اختر اللغة","Choose language","Dil seç","Elegir idioma","Sprache wählen","Scegli lingua","Choisir la langue","زبان منتخب کریں","انتخاب زبان"), fontSize=19.sp, fontWeight=FontWeight.Black, color=Color.White)
                        Text("NEXVARY • Electric UI", color=Color(0xFF72F1FF), fontSize=10.sp)
                    }
                    IconButton(onClick=onDismiss){Icon(Icons.Default.Close,null,tint=Color(0xFFFFC857))}
                }
                Spacer(Modifier.height(8.dp))
                AppLanguage.entries.forEach { lang ->
                    val active=selected==lang
                    Surface(
                        Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{onSelect(lang)},
                        shape=RoundedCornerShape(16.dp),
                        color=if(active)Color(0xFF0A2A49)else Color(0xFF06182D),
                        border=BorderStroke(if(active)2.dp else 1.dp,if(active)Color(0xFF00E5FF)else Color(0xFFD9E7F2))
                    ) {
                        Row(Modifier.padding(horizontal=12.dp,vertical=11.dp), verticalAlignment=Alignment.CenterVertically) {
                            Text(languageFlag(lang), fontSize=25.sp, modifier=Modifier.width(40.dp))
                            Column(Modifier.weight(1f)) {
                                Text(lang.nativeName, fontWeight=FontWeight.Bold, color=Color.White)
                                Text(lang.short, color=if(active)Color(0xFFFFC857)else Color(0xFF72F1FF), fontSize=10.sp)
                            }
                            if(active) Icon(Icons.Default.Check,null,tint=Color(0xFF43FFB1))
                        }
                    }
                }
            }
        }
    }
}

@Composable internal fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(22.dp)),
        shape=RoundedCornerShape(22.dp),
        color=Color(0xFF041326),
        border=BorderStroke(2.dp, Color(0xFFD9E7F2))
    ) {
        Surface(
            Modifier.fillMaxWidth().padding(2.dp),
            shape=RoundedCornerShape(20.dp),
            color=Color(0xFF071A31),
            border=BorderStroke(1.dp, Color(0xFF00E5FF))
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Surface(shape=CircleShape,color=Color(0xFF082C4A),border=BorderStroke(1.dp,Color(0xFFFFC857))){
                        Box(Modifier.padding(4.dp).size(8.dp).background(Color(0xFF00E5FF),CircleShape))
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(title, modifier=Modifier.fillMaxWidth(), fontWeight=FontWeight.Black, fontSize=14.sp, color=Color.White, textAlign=TextAlign.Start)
                }
                Spacer(Modifier.height(10.dp)); content()
            }
        }
    }
}

@Composable private fun CategoryStrip(selected: ToolCategory, language: AppLanguage, onSelect: (ToolCategory)->Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { ToolCategory.entries.forEach { item -> FilterChip(selected=selected==item, onClick={onSelect(item)}, leadingIcon={Icon(item.icon,null,Modifier.size(18.dp))}, label={Text(item.label(language))}); Spacer(Modifier.width(7.dp)) } }
}

@Composable private fun SmartToolsGrid(category: ToolCategory, selected: SmartTool, language: AppLanguage, onSelect:(SmartTool)->Unit) {
    val items = SmartTool.entries.filter { it.category==category && it != SmartTool.None }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { items.chunked(3).forEach { rowItems ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) { rowItems.forEach { item ->
            val accent = when(item.category) {
                ToolCategory.Age -> Color(0xFFFFC857); ToolCategory.Hair -> Color(0xFF00E5FF); ToolCategory.Face -> Color(0xFF8B7CFF); ToolCategory.Beauty -> Color(0xFFFF5FC8); ToolCategory.Background -> Color(0xFF43FFB1)
            }
            Surface(Modifier.weight(1f).height(90.dp).clickable{onSelect(item)}, shape=RoundedCornerShape(17.dp), color=if(selected==item) Color(0xFF0B2D49) else Color(0xFF06182D), border=BorderStroke(if(selected==item)2.dp else 1.dp, if(selected==item) accent else Color(0xFFD9E7F2))) {
                Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
                    Surface(shape=CircleShape,color=accent.copy(alpha=.16f),border=BorderStroke(1.dp,accent)){ Icon(item.icon,null,Modifier.padding(7.dp).size(22.dp),tint=accent) }
                    Spacer(Modifier.height(5.dp)); Text(item.label(language), fontSize=10.sp, color=Color.White, textAlign=TextAlign.Center, fontWeight=FontWeight.Bold, maxLines=2)
                }
            }
        }; repeat(3-rowItems.size) { Spacer(Modifier.weight(1f)) } }
    }
}
}

@Composable private fun PresetStrip(selected: EditPreset, language:AppLanguage, onSelect:(EditPreset)->Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(7.dp)) { EditPreset.entries.forEach { item -> FilterChip(selected=selected==item,onClick={onSelect(item)},label={Text(item.label(language))},modifier=Modifier.weight(1f)) } } }

@Composable private fun HistoryBar(language:AppLanguage, canUndo:Boolean, canRedo:Boolean, onUndo:()->Unit, onRedo:()->Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick=onUndo, enabled=canUndo, modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Icon(Icons.Default.Undo,null); Spacer(Modifier.width(6.dp)); Text(tx(language,"تراجع","Undo","Geri al","Deshacer","Rückgängig","Annulla"),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=11.sp) }
    OutlinedButton(onClick=onRedo, enabled=canRedo, modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Icon(Icons.Default.Redo,null); Spacer(Modifier.width(6.dp)); Text(tx(language,"إعادة","Redo","Yinele","Rehacer","Wiederholen","Ripeti"),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=11.sp) }
} }

@Composable private fun ServerPanel(state:ServerState, details:String, language:AppLanguage, onCheck:()->Unit) {
    Surface(Modifier.fillMaxWidth(), shape=RoundedCornerShape(18.dp), color=Color(0xFF06182D), border=BorderStroke(1.dp, Color(0xFF1479FF))) { Row(Modifier.padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
        Icon(Icons.Default.Dns, null, tint=when(state){ServerState.Online->Color(0xFF45E69C);ServerState.Offline->Color(0xFFFF6B6B);else->Color(0xFFFFC857)}); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(when(state){ServerState.Online->tx(language,"محرك AI متصل","AI Engine Online","AI motoru çevrimiçi","Motor IA conectado","KI-Engine online","Motore IA online");ServerState.Offline->tx(language,"محرك AI غير متصل","AI Engine Offline","AI motoru çevrimdışı","Motor IA sin conexión","KI-Engine offline","Motore IA offline");ServerState.Checking->tx(language,"جاري فحص المحرك…","Checking engine…","Motor kontrol ediliyor…","Comprobando motor…","Engine wird geprüft…","Controllo motore…");else->tx(language,"محرك الذكاء الاصطناعي","AI Engine","AI motoru","Motor IA","KI-Engine","Motore IA")},fontWeight=FontWeight.Bold,fontSize=13.sp); Text(details.ifBlank{BuildConfig.FACE_API_BASE_URL},color=Color(0xFF8293AA),fontSize=10.sp,maxLines=2) }
        IconButton(onClick=onCheck,enabled=state!=ServerState.Checking){Icon(Icons.Default.Refresh,null)}
    } }
}

@Composable private fun AiResultMetaCard(meta: FaceEditResult, language: AppLanguage) { Surface(Modifier.fillMaxWidth(), shape=RoundedCornerShape(18.dp), color=Color(0xFF071A31), border=BorderStroke(1.dp, Color(0xFF265269))) { Column(Modifier.padding(12.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Verified, null, tint=Color(0xFF72E9FF)); Spacer(Modifier.width(8.dp)); Text(tx(language,"آخر نتيجة AI","Last AI result","Son AI sonucu","Último resultado IA","Letztes KI-Ergebnis","Ultimo risultato IA"), fontWeight=FontWeight.Bold) }
    Spacer(Modifier.height(7.dp)); Text(tx(language,"الطلب: ","Request: ","İstek: ","Solicitud: ","Anfrage: ","Richiesta: ")+meta.requestId.ifBlank{"—"},fontSize=11.sp,color=Color(0xFFB7C7DA)); Text(tx(language,"المعالجة: ","Processing: ","İşleme: ","Procesamiento: ","Verarbeitung: ","Elaborazione: ")+(meta.processMs?.let{"${it} ms"}?:tx(language,"من الذاكرة المؤقتة","cache hit","önbellek","caché","Cache","cache")),fontSize=11.sp,color=Color(0xFFB7C7DA)); Text(tx(language,"الذاكرة: ","Cache: ","Önbellek: ","Caché: ","Cache: ","Cache: ")+meta.cacheState+(if(meta.engineVersion.isNotBlank())" • v${meta.engineVersion}" else ""),fontSize=11.sp,color=Color(0xFF8293AA))
} } }

@Composable private fun AiActivityCard(items: List<AiActivity>, language: AppLanguage) { SectionCard(tx(language,"سجل تعديلات AI الأخيرة","Recent AI activity","Son AI işlemleri","Actividad IA reciente","Letzte KI-Aktivität","Attività IA recente")) { items.take(5).forEachIndexed { index,item -> Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){ Surface(shape=CircleShape,color=Color(0xFF173B52)){Text("${index+1}",modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp),fontSize=10.sp,color=Color(0xFF72E9FF))};Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(item.effect.replace('_',' '),fontWeight=FontWeight.SemiBold,fontSize=11.sp);Text(item.requestId.ifBlank{"local"},color=Color(0xFF8293AA),fontSize=9.sp)};Text(item.processMs?.let{"${it}ms"}?:item.cacheState,fontSize=10.sp,color=Color(0xFF91FFD1)) } } } }

@Composable private fun StatusMessage(text:String) { Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFF0D2A24),border=BorderStroke(1.dp,Color(0xFF245D4B))){Text(text,modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp),color=Color(0xFF91FFD1),fontSize=12.sp,textAlign=TextAlign.Center)} }

@Composable private fun PreviewCard(bitmap: Bitmap?, language:AppLanguage, busy:Boolean, faceDetected:Boolean?, onPick:()->Unit) { Surface(Modifier.fillMaxWidth().height(if(bitmap==null)330.dp else 390.dp), shape=RoundedCornerShape(26.dp), color=Color(0xFF041326), border=BorderStroke(2.dp,Color(0xFFD9E7F2))) {
    if(bitmap==null) Column(Modifier.fillMaxSize().clickable{onPick()},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.AddPhotoAlternate,null,Modifier.size(64.dp),tint=Color(0xFF00D9FF));Spacer(Modifier.height(10.dp));Text(tx(language,"اختر صورة للوجه","Choose a portrait","Bir portre seçin","Elige un retrato","Porträt auswählen","Scegli un ritratto"),fontSize=20.sp,fontWeight=FontWeight.Bold);Text(tx(language,"المعرض • JPG/PNG","Gallery • JPG/PNG","Galeri • JPG/PNG","Galería • JPG/PNG","Galerie • JPG/PNG","Galleria • JPG/PNG"),color=Color(0xFF8DA0B8),fontSize=12.sp)}
    else Box(Modifier.fillMaxSize()){Image(bitmap=bitmap.asImageBitmap(),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit);Surface(Modifier.align(Alignment.TopEnd).padding(10.dp),shape=RoundedCornerShape(50),color=Color(0xD10A1322)){Row(Modifier.clickable{onPick()}.padding(horizontal=11.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.PhotoLibrary,null,Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text(tx(language,"تغيير","Change","Değiştir","Cambiar","Ändern","Cambia"),fontSize=11.sp)}}; faceDetected?.let{Surface(Modifier.align(Alignment.TopStart).padding(10.dp),shape=RoundedCornerShape(50),color=if(it)Color(0xC0195C43)else Color(0xC06B2B2B)){Text(if(it)tx(language,"الوجه جاهز","Face ready","Yüz hazır","Rostro listo","Gesicht bereit","Viso pronto")else tx(language,"لا يوجد وجه","No face","Yüz yok","Sin rostro","Kein Gesicht","Nessun viso"),modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp),fontSize=10.sp)}};if(busy)Box(Modifier.fillMaxSize().background(Color(0x77000000)),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
} }

@Composable private fun BeforeAfterPro(before:Boolean,language:AppLanguage,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){SingleChoiceSegmentedButtonRow{SegmentedButton(selected=before,onClick={onChange(true)},shape=SegmentedButtonDefaults.itemShape(0,2)){Text(tx(language,"قبل","BEFORE","ÖNCE","ANTES","VORHER","PRIMA"))};SegmentedButton(selected=!before,onClick={onChange(false)},shape=SegmentedButtonDefaults.itemShape(1,2)){Text(tx(language,"بعد","AFTER","SONRA","DESPUÉS","NACHHER","DOPO"))}}}}

@Composable private fun EffectStrip(selected:LocalEffect,language:AppLanguage,onSelect:(LocalEffect)->Unit){Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){LocalEffect.entries.forEach{item->AssistChip(onClick={onSelect(item)},label={Text(item.label(language))},colors=AssistChipDefaults.assistChipColors(containerColor=if(selected==item)Color(0xFF123D52)else Color(0xFF101A2A),labelColor=Color.White),border=BorderStroke(1.dp,if(selected==item)Color(0xFF00D9FF)else Color(0xFF4E7DA8)));Spacer(Modifier.width(7.dp))}}}

@Composable private fun IntensityControl(value:Float,language:AppLanguage,onChange:(Float)->Unit){Column(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(tx(language,"قوة التأثير","Intensity","Yoğunluk","Intensidad","Intensität","Intensità"),fontSize=13.sp,fontWeight=FontWeight.Bold);Text("${(value*100).toInt()}%",fontSize=12.sp,color=Color(0xFF72E9FF))};Slider(value=value,onValueChange=onChange,valueRange=.1f..1f)}}

@Composable private fun ActionRow(language:AppLanguage,onSave:()->Unit,onShare:()->Unit,onReset:()->Unit){
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Button(onClick=onSave,modifier=Modifier.widthIn(min=112.dp)){Icon(Icons.Default.Save,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(tx(language,"حفظ","Save","Kaydet","Guardar","Speichern","Salva","Enregistrer","محفوظ کریں","ذخیره"),maxLines=1)}
        OutlinedButton(onClick=onShare,modifier=Modifier.widthIn(min=112.dp)){Icon(Icons.Default.Share,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(tx(language,"مشاركة","Share","Paylaş","Compartir","Teilen","Condividi","Partager","شیئر","اشتراک"),maxLines=1)}
        OutlinedButton(onClick=onReset,modifier=Modifier.widthIn(min=112.dp)){Icon(Icons.Default.RestartAlt,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(tx(language,"إعادة","Reset","Sıfırla","Restablecer","Zurücksetzen","Ripristina","Réinitialiser","ری سیٹ","بازنشانی"),maxLines=1)}
    }
}


@Composable internal fun ScreenHeader(title:String, subtitle:String, icon:ImageVector) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=10.dp).shadow(12.dp, RoundedCornerShape(24.dp)),
        shape=RoundedCornerShape(24.dp), color=Color(0xFF03101F), border=BorderStroke(2.dp,Color(0xFFD9E7F2))
    ) {
        Surface(Modifier.fillMaxWidth().padding(2.dp),shape=RoundedCornerShape(22.dp),color=Color(0xFF061A31),border=BorderStroke(1.dp,Color(0xFF1479FF))) {
            Row(Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=14.dp), verticalAlignment=Alignment.CenterVertically) {
                Surface(shape=RoundedCornerShape(17.dp), color=Color(0xFF082C4A), border=BorderStroke(2.dp,Color(0xFF00E5FF))) {
                    Icon(icon,null,Modifier.padding(11.dp).size(27.dp),tint=Color(0xFFFFC857))
                }
                Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) {
                    Text(title,Modifier.fillMaxWidth(),fontWeight=FontWeight.Black,fontSize=22.sp,color=Color.White,textAlign=TextAlign.Start,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text(subtitle,Modifier.fillMaxWidth(),color=Color(0xFF72F1FF),fontSize=12.sp,textAlign=TextAlign.Start,maxLines=2,overflow=TextOverflow.Ellipsis)
                }
                Box(Modifier.size(9.dp).background(Color(0xFF43FFB1),CircleShape))
            }
        }
    }
}

@Composable private fun WardrobeScreen(
    person:Bitmap?, result:Bitmap?, garment:Bitmap?,
    category:WardrobeCategory, selectedStyle:WardrobeStyle, selectedColor:WardrobeColor,
    selectedAccessory:WardrobeAccessory, strength:Float, busy:Boolean, language:AppLanguage,
    modifier:Modifier=Modifier,
    onPickPerson:()->Unit, onPickGarment:()->Unit,
    onCategory:(WardrobeCategory)->Unit, onStyle:(WardrobeStyle)->Unit,
    onColor:(WardrobeColor)->Unit, onAccessory:(WardrobeAccessory)->Unit,
    onStrength:(Float)->Unit, onRun:(String,String)->Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val wprefs = remember { context.getSharedPreferences("facelab_wardrobe", android.content.Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf<String>("") }
    var gender by remember { mutableStateOf<WardrobeGender>(WardrobeGender.All) }
    var shelf by remember { mutableStateOf<WardrobeShelf>(WardrobeShelf.Clothing) }
    var occasion by remember { mutableStateOf<WardrobeOccasion>(WardrobeOccasion.Work) }
    var selectedOutfit by remember { mutableStateOf<WardrobeOutfitPack?>(null) }
    var favorites: Set<String> by remember {
        mutableStateOf<Set<String>>(wprefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }
    var savedLooks: Set<String> by remember {
        mutableStateOf<Set<String>>(wprefs.getStringSet("saved_looks", emptySet())?.toSet() ?: emptySet())
    }
    var stylistMessage by remember { mutableStateOf<String>("") }
    var topPiece by remember { mutableStateOf<WardrobePiece>(TOP_PIECES.first()) }
    var bottomPiece by remember { mutableStateOf<WardrobePiece>(BOTTOM_PIECES.first()) }
    var outerPiece by remember { mutableStateOf<WardrobePiece>(OUTER_PIECES.first()) }
    var shoePiece by remember { mutableStateOf<WardrobePiece>(SHOE_PIECES.first()) }
    var closetName by remember { mutableStateOf<String>("") }
    var closets: Set<String> by remember {
        mutableStateOf<Set<String>>(wprefs.getStringSet("closets", emptySet())?.toSet() ?: emptySet())
    }

    fun saveFavs(set: Set<String>) { wprefs.edit().putStringSet("favorites", HashSet(set)).apply() }
    fun styleMatch(st:WardrobeStyle):Boolean {
        val g=wardrobeGender(st)
        val genderOk= gender==WardrobeGender.All || g==gender || g==WardrobeGender.Unisex
        val q=query.trim().lowercase()
        val text=listOf(st.ar,st.en,st.tr,st.es,st.de,st.it).joinToString(" ").lowercase()
        return st.category==category && genderOk && (q.isBlank() || text.contains(q))
    }
    fun shelfLabel(sh:WardrobeShelf):String = when(sh) {
        WardrobeShelf.Clothing -> tx(language,"ملابس","Clothes","Giysiler","Ropa","Kleidung","Abiti")
        WardrobeShelf.Outfits -> tx(language,"أطقم كاملة","Outfits","Kombinler","Conjuntos","Outfits","Outfit")
        WardrobeShelf.Shoes -> tx(language,"أحذية","Shoes","Ayakkabı","Zapatos","Schuhe","Scarpe")
        WardrobeShelf.Glasses -> tx(language,"نظارات","Glasses","Gözlük","Gafas","Brillen","Occhiali")
        WardrobeShelf.Hats -> tx(language,"قبعات","Hats","Şapkalar","Sombreros","Hüte","Cappelli")
    }
    fun accessoryForShelf(sh:WardrobeShelf):WardrobeAccessory? = when(sh) {
        WardrobeShelf.Shoes -> WardrobeAccessory.Shoes
        WardrobeShelf.Glasses -> WardrobeAccessory.Glasses
        WardrobeShelf.Hats -> WardrobeAccessory.Hat
        else -> null
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp)) {
        ScreenHeader(
            tx(language,"خزانة الملابس الذكية","AI Virtual Wardrobe","AI Sanal Gardırop","Armario virtual IA","KI-Kleiderschrank","Guardaroba virtuale IA"),
            tx(language,"متجر بصري + أطقم كاملة + AI Stylist + إكسسوارات","Visual catalog + full outfits + AI Stylist + accessories","Görsel katalog + tam kombinler + AI Stilist","Catálogo visual + conjuntos + AI Stylist","Visueller Katalog + Outfits + KI-Stylist","Catalogo visuale + outfit + AI Stylist"),
            Icons.Default.Checkroom
        )

        Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=Color(0xFF0D1524),border=BorderStroke(1.dp,Color(0xFF29465E))) {
            Column(Modifier.padding(12.dp)) {
                if(person!=null) {
                    Image((result?:person).asImageBitmap(),null,Modifier.fillMaxWidth().height(270.dp),contentScale=ContentScale.Fit)
                } else {
                    Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFF07101B),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){
                        Icon(Icons.Default.Person,null,Modifier.size(54.dp),tint=Color(0xFF61758A))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick=onPickPerson,modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.AddPhotoAlternate,null);Spacer(Modifier.width(7.dp))
                    Text(tx(language,"اختيار صورة الشخص","Choose person photo","Kişi fotoğrafı seç","Elegir foto de persona","Personenfoto wählen","Scegli foto persona"))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"1. أقسام الخزانة","1. Wardrobe sections","1. Gardırop bölümleri","1. Secciones","1. Kleiderschrank","1. Sezioni")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                WardrobeShelf.entries.forEach { sh ->
                    FilterChip(
                        selected=shelf==sh,
                        onClick={
                            shelf=sh
                            accessoryForShelf(sh)?.let { onAccessory(it) }
                        },
                        leadingIcon={Icon(sh.icon,null,Modifier.size(16.dp))},
                        label={Text(shelfLabel(sh))}
                    )
                    Spacer(Modifier.width(7.dp))
                }
            }
        }

        if (shelf == WardrobeShelf.Clothing) {
            Spacer(Modifier.height(10.dp))
            SectionCard(tx(language,"2. البحث والتصفية","2. Search & filters","2. Arama ve filtreler","2. Búsqueda y filtros","2. Suche & Filter","2. Ricerca e filtri")) {
                OutlinedTextField(
                    value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,
                    leadingIcon={Icon(Icons.Default.Search,null)},
                    placeholder={Text(tx(language,"ابحث عن بدلة، فستان، جاكيت…","Search suit, dress, jacket…","Takım, elbise, ceket ara…","Buscar traje, vestido, chaqueta…","Anzug, Kleid, Jacke suchen…","Cerca abito, vestito, giacca…"))}
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    WardrobeGender.entries.forEach { g ->
                        FilterChip(selected=gender==g,onClick={gender=g},label={Text(when(g){
                            WardrobeGender.All->tx(language,"الكل","All","Tümü","Todo","Alle","Tutti")
                            WardrobeGender.Men->tx(language,"رجالي","Men","Erkek","Hombre","Herren","Uomo")
                            WardrobeGender.Women->tx(language,"نسائي","Women","Kadın","Mujer","Damen","Donna")
                            WardrobeGender.Unisex->tx(language,"مشترك","Unisex","Unisex","Unisex","Unisex","Unisex")
                        })})
                        Spacer(Modifier.width(7.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    WardrobeCategory.entries.forEach { c ->
                        FilterChip(selected=c==category,onClick={onCategory(c)},label={Text(c.label(language))})
                        Spacer(Modifier.width(7.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            SectionCard(tx(language,"3. الكتالوج البصري","3. Visual catalog","3. Görsel katalog","3. Catálogo visual","3. Visueller Katalog","3. Catalogo visuale")) {
                val styles=WARDROBE_STYLES.filter(::styleMatch)
                if(styles.isEmpty()) {
                    Text(tx(language,"لا توجد نتائج مطابقة","No matching results","Eşleşen sonuç yok","Sin resultados","Keine Treffer","Nessun risultato"),color=Color(0xFF8EA1B5),modifier=Modifier.padding(12.dp))
                } else {
                    styles.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            row.forEach { st ->
                                val fav=favorites.contains(st.id)
                                Surface(
                                    Modifier.weight(1f).height(150.dp).clickable{selectedOutfit=null;onStyle(st)},
                                    shape=RoundedCornerShape(18.dp),
                                    color=if(st==selectedStyle)Color(0xFF173B52)else Color(0xFF111C2D),
                                    border=BorderStroke(1.dp,if(st==selectedStyle)Color(0xFF00D9FF)else Color(0xFF34475F))
                                ){
                                    Column(Modifier.fillMaxSize().padding(9.dp),verticalArrangement=Arrangement.SpaceBetween) {
                                        Box(Modifier.fillMaxWidth().height(76.dp).background(Color(0xFF0A2233),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
                                            Icon(Icons.Default.Checkroom,null,Modifier.size(42.dp),tint=when(st.category){
                                                WardrobeCategory.Formal->Color(0xFF74D8FF); WardrobeCategory.Casual->Color(0xFFB58CFF)
                                                WardrobeCategory.Sport->Color(0xFF72F0A6); WardrobeCategory.Summer->Color(0xFFFFD166)
                                                WardrobeCategory.Winter->Color(0xFF9EC5FF); WardrobeCategory.Traditional->Color(0xFFFFB36B)
                                                WardrobeCategory.Business->Color(0xFF6FE7FF); WardrobeCategory.Evening->Color(0xFFE59CFF)
                                            })
                                            IconButton(onClick={
                                                val n: MutableSet<String> = favorites.toMutableSet()
                                                if (!n.add(st.id)) { n.remove(st.id) }
                                                favorites = n.toSet()
                                                saveFavs(favorites)
                                            },modifier=Modifier.align(Alignment.TopEnd).size(32.dp)){
                                                Icon(if(fav)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(fav)Color(0xFFFF6B8A)else Color(0xFF8297AA))
                                            }
                                        }
                                        Text(st.label(language),fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=2)
                                    }
                                }
                            }
                            if(row.size==1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        } else if (shelf == WardrobeShelf.Outfits) {
            Spacer(Modifier.height(10.dp))
            SectionCard(tx(language,"2. مجموعات Outfit كاملة","2. Full outfit collections","2. Tam kombin koleksiyonları","2. Conjuntos completos","2. Komplette Outfits","2. Outfit completi")) {
                WARDROBE_OUTFITS.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach { pack ->
                            val selected = selectedOutfit?.id == pack.id
                            Surface(
                                Modifier.weight(1f).height(132.dp).clickable {
                                    selectedOutfit=pack
                                    WARDROBE_STYLES.firstOrNull{it.id==pack.styleId}?.let(onStyle)
                                    onColor(pack.color); onAccessory(pack.accessory); occasion=pack.occasion
                                },
                                shape=RoundedCornerShape(18.dp),
                                color=if(selected)Color(0xFF173B52)else Color(0xFF111C2D),
                                border=BorderStroke(1.dp,if(selected)Color(0xFF00D9FF)else Color(0xFF34475F))
                            ){
                                Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.SpaceBetween) {
                                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                        Icon(Icons.Default.DashboardCustomize,null,tint=Color(0xFFFFD166))
                                        Icon(pack.occasion.icon,null,tint=Color(0xFF7FE8FF))
                                    }
                                    Text(pack.label(language),fontWeight=FontWeight.Bold,fontSize=12.sp)
                                    Text("${pack.color.label} • ${pack.accessory.label}",fontSize=9.sp,color=Color(0xFFD9E7F2))
                                }
                            }
                        }
                        if(row.size==1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            val forced = accessoryForShelf(shelf)!!
            SectionCard("2. ${shelfLabel(shelf)}") {
                LaunchedEffect(shelf) { onAccessory(forced) }
                Text(
                    tx(language,"تم تفعيل هذا النوع من الإكسسوار. اختر اللون أو اللوك ثم نفّذ التغيير.","This accessory type is active. Choose color/look and run the edit.","Bu aksesuar türü etkin.","Este accesorio está activo.","Dieser Accessoire-Typ ist aktiv.","Questo accessorio è attivo."),
                    color=Color(0xFF9FB4C8),fontSize=12.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    repeat(3) { idx ->
                        Surface(Modifier.weight(1f).height(92.dp),shape=RoundedCornerShape(16.dp),color=Color(0xFF101D2D),border=BorderStroke(1.dp,Color(0xFF2F4860))) {
                            Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                                Icon(forced.icon,null,Modifier.size(30.dp),tint=Color(0xFF7DE9FF))
                                Text("#${idx+1}",fontSize=10.sp,color=Color(0xFF8FA5BA))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"4. تركيب اللوك قطعة بقطعة","4. Build look piece by piece","4. Kombini parça parça oluştur","4. Crear look por piezas","4. Look Stück für Stück","4. Crea look pezzo per pezzo")) {
            Text(tx(language,"اختر القطع بشكل مستقل وسيتم دمجها في وصف واحد لمحرك الذكاء الاصطناعي.","Choose pieces independently; FaceLab combines them into one AI outfit description.","Parçaları ayrı ayrı seçin; FaceLab tek bir AI kombin açıklamasında birleştirir.","Elige piezas por separado; FaceLab las combina en una sola descripción IA.","Wähle Teile einzeln; FaceLab kombiniert sie zu einer KI-Outfitbeschreibung.","Scegli i capi separatamente; FaceLab li combina in una descrizione IA."),fontSize=11.sp,color=Color(0xFF9FB4C8))
            Spacer(Modifier.height(8.dp))
            Text(tx(language,"الجزء العلوي","Top","Üst","Parte superior","Oberteil","Top"),fontWeight=FontWeight.Bold,fontSize=11.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { TOP_PIECES.forEach { x -> FilterChip(selected=topPiece==x,onClick={topPiece=x},label={Text(x.label(language))});Spacer(Modifier.width(6.dp)) } }
            Text(tx(language,"الجزء السفلي","Bottom","Alt","Parte inferior","Unterteil","Parte inferiore"),fontWeight=FontWeight.Bold,fontSize=11.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { BOTTOM_PIECES.forEach { x -> FilterChip(selected=bottomPiece==x,onClick={bottomPiece=x},label={Text(x.label(language))});Spacer(Modifier.width(6.dp)) } }
            Text(tx(language,"الطبقة الخارجية","Outerwear","Dış giyim","Capa exterior","Outerwear","Capospalla"),fontWeight=FontWeight.Bold,fontSize=11.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { OUTER_PIECES.forEach { x -> FilterChip(selected=outerPiece==x,onClick={outerPiece=x},label={Text(x.label(language))});Spacer(Modifier.width(6.dp)) } }
            Text(tx(language,"الحذاء","Shoes","Ayakkabı","Calzado","Schuhe","Scarpe"),fontWeight=FontWeight.Bold,fontSize=11.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { SHOE_PIECES.forEach { x -> FilterChip(selected=shoePiece==x,onClick={shoePiece=x},label={Text(x.label(language))});Spacer(Modifier.width(6.dp)) } }
        }

        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"5. AI Stylist — ثلاثة اقتراحات","5. AI Stylist — three suggestions","5. AI Stilist — üç öneri","5. AI Stylist — tres sugerencias","5. KI-Stylist — drei Vorschläge","5. AI Stylist — tre proposte")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                WardrobeOccasion.entries.forEach { o ->
                    FilterChip(selected=occasion==o,onClick={occasion=o},leadingIcon={Icon(o.icon,null,Modifier.size(16.dp))},label={Text(o.label(language))})
                    Spacer(Modifier.width(7.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            val suggestions=wardrobeSuggestions(occasion,gender)
            suggestions.forEachIndexed { index, suggestion ->
                OutlinedButton(onClick={selectedOutfit=null;onCategory(suggestion.category);onStyle(suggestion);stylistMessage=tx(language,"تم اختيار الاقتراح ","Selected suggestion ","Öneri seçildi ","Sugerencia seleccionada ","Vorschlag gewählt ","Suggerimento selezionato ")+"${index+1}: "+suggestion.label(language)},modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AutoAwesome,null,Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text("${index+1}. ${suggestion.label(language)}")
                }
            }
            if(stylistMessage.isNotBlank()){Spacer(Modifier.height(6.dp));Text(stylistMessage,color=Color(0xFF72E9FF),fontSize=11.sp)}
        }

        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"6. غرف الملابس المحفوظة","6. Saved dressing rooms","6. Kayıtlı giyinme odaları","6. Vestidores guardados","6. Gespeicherte Ankleidezimmer","6. Camerini salvati")) {
            OutlinedTextField(value=closetName,onValueChange={closetName=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(tx(language,"اسم المجموعة مثل: سفر تركيا","Collection name e.g. Turkey trip","Koleksiyon adı","Nombre de colección","Sammlungsname","Nome collezione"))})
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick={
                val name=closetName.trim().ifBlank { "Look ${closets.size+1}" }
                val composition="${topPiece.code},${bottomPiece.code},${outerPiece.code},${shoePiece.code}"
                val entry="$name|${selectedStyle.id}|${selectedColor.code}|${selectedAccessory.code}|${occasion.code}|$composition"
                val n: MutableSet<String> = closets.toMutableSet()
                n.add(entry)
                closets = n.toSet()
                wprefs.edit().putStringSet("closets", HashSet(closets)).apply()
                closetName = ""
            },modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Inventory2,null);Spacer(Modifier.width(6.dp));Text(tx(language,"حفظ في غرفة الملابس","Save to dressing room","Giyinme odasına kaydet","Guardar en vestidor","Im Ankleidezimmer speichern","Salva nel camerino")) }
            if(closets.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(tx(language,"الغرف المحفوظة: ","Saved rooms: ","Kayıtlı odalar: ","Vestidores guardados: ","Gespeichert: ","Camerini salvati: ")+closets.size,color=Color(0xFF72E9FF),fontSize=11.sp) }
        }

        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"7. AI Stylist — المناسبة","4. AI Stylist — occasion","4. AI Stilist — etkinlik","4. AI Stylist — ocasión","4. KI-Stylist — Anlass","4. AI Stylist — occasione")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                WardrobeOccasion.entries.forEach { o ->
                    FilterChip(selected=occasion==o,onClick={occasion=o},leadingIcon={Icon(o.icon,null,Modifier.size(16.dp))},label={Text(o.label(language))})
                    Spacer(Modifier.width(7.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick={
                val suggestion=suggestedWardrobeStyle(occasion,gender)
                selectedOutfit=null
                onCategory(suggestion.category)
                onStyle(suggestion)
                stylistMessage=tx(language,"تم اقتراح: ","Suggested: ","Öneri: ","Sugerido: ","Vorschlag: ","Suggerito: ")+suggestion.label(language)
            },modifier=Modifier.fillMaxWidth()){
                Icon(Icons.Default.Psychology,null);Spacer(Modifier.width(7.dp))
                Text(tx(language,"اقترح لي لوك تلقائيًا","Suggest a look","Kombin öner","Sugerir look","Look vorschlagen","Suggerisci look"))
            }
            if(stylistMessage.isNotBlank()){Spacer(Modifier.height(6.dp));Text(stylistMessage,color=Color(0xFF72E9FF),fontSize=11.sp)}
        }

        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"8. اللون والإكسسوارات","8. Color & accessories","5. Renk ve aksesuar","5. Color y accesorios","5. Farbe & Accessoires","5. Colore e accessori")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                WardrobeColor.entries.forEach { c ->
                    FilterChip(selected=c==selectedColor,onClick={onColor(c)},label={Text(c.label)})
                    Spacer(Modifier.width(7.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                WardrobeAccessory.entries.forEach { a ->
                    FilterChip(selected=a==selectedAccessory,onClick={onAccessory(a)},leadingIcon={Icon(a.icon,null,Modifier.size(16.dp))},label={Text(a.label)})
                    Spacer(Modifier.width(7.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"9. استيراد قطعة من الهاتف — اختياري","9. Import garment — optional","6. Giysi içe aktar","6. Importar prenda","6. Kleidungsstück importieren","6. Importa capo")) {
            if(garment!=null) Image(garment.asImageBitmap(),null,Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.Fit)
            OutlinedButton(onClick=onPickGarment,modifier=Modifier.fillMaxWidth()){
                Icon(Icons.Default.Upload,null);Spacer(Modifier.width(7.dp))
                Text(if(garment==null)tx(language,"استيراد صورة الملابس","Import garment image","Giysi görseli içe aktar","Importar imagen de prenda","Kleidungsbild importieren","Importa immagine capo")
                else tx(language,"تغيير صورة القطعة","Change garment image","Giysiyi değiştir","Cambiar prenda","Kleidungsbild ändern","Cambia capo"))
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"10. قوة التغيير","10. Change strength","7. Değişim gücü","7. Intensidad","7. Änderungsstärke","7. Intensità")) {
            Slider(value=strength,onValueChange=onStrength,valueRange=.25f..1f)
            Text("${(strength*100).toInt()}%",color=Color(0xFF72E9FF),fontWeight=FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={
                val outfitId=selectedOutfit?.id?:"custom"
                val key="${selectedStyle.id}|${selectedColor.code}|${selectedAccessory.code}|${occasion.code}|$outfitId"
                val n: MutableSet<String> = savedLooks.toMutableSet()
                n.add(key)
                savedLooks = n.toSet()
                wprefs.edit().putStringSet("saved_looks", HashSet(savedLooks)).apply()
            },modifier=Modifier.weight(1f)){
                Icon(Icons.Default.BookmarkAdd,null);Spacer(Modifier.width(6.dp))
                Text(tx(language,"حفظ اللوك","Save look","Kombini kaydet","Guardar look","Look speichern","Salva look"),fontSize=11.sp)
            }
            AssistChip(onClick={},label={Text(tx(language,
                "المفضلة ${favorites.size} • المحفوظ ${savedLooks.size}",
                "${favorites.size} favorites • ${savedLooks.size} saved",
                "${favorites.size} favori • ${savedLooks.size} kayıtlı",
                "${favorites.size} favoritos • ${savedLooks.size} guardados",
                "${favorites.size} Favoriten • ${savedLooks.size} gespeichert",
                "${favorites.size} preferiti • ${savedLooks.size} salvati"),fontSize=10.sp)},
                leadingIcon={Icon(Icons.Default.Favorite,null,Modifier.size(16.dp))}
            )
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick={
                val customPieces = listOf(
                    "top=${topPiece.code}", "bottom=${bottomPiece.code}", "outerwear=${outerPiece.code}", "shoes=${shoePiece.code}"
                ).joinToString(", ")
                val base = selectedOutfit?.prompt.orEmpty()
                onRun(occasion.code, listOf(base, customPieces).filter { it.isNotBlank() }.joinToString(", "))
            },
            enabled=!busy && person!=null,
            modifier=Modifier.fillMaxWidth().height(56.dp)
        ){
            if(busy) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp) else Icon(Icons.Default.AutoAwesome,null)
            Spacer(Modifier.width(8.dp))
            Text(tx(language,"تطبيق اللوك بالذكاء الاصطناعي","APPLY AI LOOK","AI KOMBİNİ UYGULA","APLICAR LOOK IA","KI-LOOK ANWENDEN","APPLICA LOOK IA"),fontWeight=FontWeight.Black)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            tx(language,
                "Stage 2000: تركيب مستقل للقطع + 3 اقتراحات AI Stylist + غرف ملابس محفوظة + كتالوج بصري متكامل.",
                "Stage 2000: independent pieces + 3 AI Stylist suggestions + saved dressing rooms + visual wardrobe.",
                "Stage 2000: bağımsız parçalar + 3 AI Stilist önerisi + kayıtlı giyinme odaları.",
                "Stage 2000: piezas independientes + 3 sugerencias AI Stylist + vestidores guardados.",
                "Stage 2000: Einzelteile + 3 KI-Stylist-Vorschläge + gespeicherte Ankleidezimmer.",
                "Stage 2000: capi indipendenti + 3 proposte AI Stylist + camerini salvati."),
            color=Color(0xFF8293AA),fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}


private fun countryLabel(code:String, language:AppLanguage):String = when(code) {
    "EG" -> tx(language,"مصر","Egypt","Mısır","Egipto","Ägypten","Egitto")
    "TR" -> tx(language,"تركيا","Türkiye","Türkiye","Turquía","Türkei","Turchia")
    "DZ" -> tx(language,"الجزائر","Algeria","Cezayir","Argelia","Algerien","Algeria")
    "SA" -> tx(language,"السعودية","Saudi Arabia","Suudi Arabistan","Arabia Saudí","Saudi-Arabien","Arabia Saudita")
    "MA" -> tx(language,"المغرب","Morocco","Fas","Marruecos","Marokko","Marocco")
    "TN" -> tx(language,"تونس","Tunisia","Tunus","Túnez","Tunesien","Tunisia")
    "IT" -> tx(language,"إيطاليا","Italy","İtalya","Italia","Italien","Italia")
    "ES" -> tx(language,"إسبانيا","Spain","İspanya","España","Spanien","Spagna")
    "GR" -> tx(language,"اليونان","Greece","Yunanistan","Grecia","Griechenland","Grecia")
    else -> code
}


@Composable private fun HeritageThumbnail(site:HeritageSite) {
    val context=LocalContext.current
    val bitmap=remember(site.id) {
        runCatching { context.assets.open("heritage/thumbs/${site.id}.png").use { BitmapFactory.decodeStream(it) } }.getOrNull()
    }
    if(bitmap!=null) Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    else Icon(Icons.Default.AccountBalance,null,Modifier.size(38.dp),tint=Color(0xFFFFD166))
}

@Composable private fun HeritageScreen(
    original:Bitmap?, result:Bitmap?, background:Bitmap?, selectedSite:HeritageSite,
    selectedMode:HeritageMode, selectedLight:HeritageLight, strength:Float, busy:Boolean,
    language:AppLanguage, modifier:Modifier=Modifier,
    onPickPerson:()->Unit, onPickBackground:()->Unit, onSite:(HeritageSite)->Unit,
    onMode:(HeritageMode)->Unit, onLight:(HeritageLight)->Unit, onStrength:(Float)->Unit, onRun:()->Unit
) {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("heritage_studio", android.content.Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf<String>("") }
    var country by remember { mutableStateOf<String>("ALL") }
    var favorites: Set<String> by remember {
        mutableStateOf<Set<String>>(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }
    var onlyFavorites by remember { mutableStateOf<Boolean>(false) }
    val countries=listOf("ALL","EG","TR","DZ","SA","MA","TN","IT","ES","GR")
    fun siteMatches(site:HeritageSite):Boolean {
        val needle=query.trim().lowercase()
        val countryOk=country=="ALL" || site.country==country
        val favOk=!onlyFavorites || favorites.contains(site.id)
        val queryOk=needle.isBlank() || site.label(language).lowercase().contains(needle) ||
            site.city.lowercase().contains(needle) || countryLabel(site.country,language).lowercase().contains(needle)
        return countryOk && favOk && queryOk
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp)) {
        ScreenHeader(
            tx(language,"استوديو المعالم والتراث","Heritage & Landmarks Studio","Miras ve simge yapılar stüdyosu","Estudio de patrimonio y lugares","Kulturerbe-Studio","Studio patrimonio e luoghi"),
            tx(language,"72 معلمًا • 9 دول • بحث ومفضلة","72 landmarks • 9 countries • search & favorites","72 simge • 9 ülke","72 lugares • 9 países","72 Orte • 9 Länder","72 luoghi • 9 paesi"),
            Icons.Default.Landscape
        )
        SectionCard(tx(language,"1. صورة الشخص والخلفية","1. Person & background","1. Kişi ve arka plan","1. Persona y fondo","1. Person & Hintergrund","1. Persona e sfondo")) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick=onPickPerson,modifier=Modifier.weight(1f)){Icon(Icons.Default.Person,null);Spacer(Modifier.width(6.dp));Text(tx(language,"صورة الشخص","Person","Kişi","Persona","Person","Persona"))}
                OutlinedButton(onClick=onPickBackground,modifier=Modifier.weight(1f)){Icon(Icons.Default.Image,null);Spacer(Modifier.width(6.dp));Text(tx(language,"الخلفية","Background","Arka plan","Fondo","Hintergrund","Sfondo"))}
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if(background==null) tx(language,"اختر صورة خلفية مرخصة من الهاتف أو من حزمة المعالم على الخادم.","Choose a licensed background from the phone or server landmark pack.","Lisanslı arka plan seçin.","Elige un fondo con licencia.","Wähle einen lizenzierten Hintergrund.","Scegli uno sfondo con licenza.")
                else tx(language,"الخلفية جاهزة للدمج","Background ready","Arka plan hazır","Fondo listo","Hintergrund bereit","Sfondo pronto"),
                color=if(background==null)Color(0xFFFFC857)else Color(0xFF5BE7A9),fontSize=11.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"2. الدولة والبحث","2. Country & search","2. Ülke ve arama","2. País y búsqueda","2. Land & Suche","2. Paese e ricerca")) {
            OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text(tx(language,"ابحث عن معلم أو مدينة","Search landmark or city","Simge veya şehir ara","Buscar lugar o ciudad","Ort oder Stadt suchen","Cerca luogo o città"))})
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                countries.forEach { c ->
                    FilterChip(selected=country==c,onClick={country=c},label={Text(if(c=="ALL")tx(language,"الكل","All","Tümü","Todo","Alle","Tutti") else countryLabel(c,language))})
                    Spacer(Modifier.width(6.dp))
                }
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                FilterChip(selected=onlyFavorites,onClick={onlyFavorites=!onlyFavorites},leadingIcon={Icon(if(onlyFavorites)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,Modifier.size(15.dp))},label={Text(tx(language,"المفضلة","Favorites","Favoriler","Favoritos","Favoriten","Preferiti"))})
                Spacer(Modifier.width(8.dp))
                Text("${HERITAGE_SITES.count(::siteMatches)} / ${HERITAGE_SITES.size}",color=Color(0xFF8FA8BC),fontSize=11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"3. مكتبة المعالم","3. Landmark library","3. Simge kütüphanesi","3. Biblioteca de lugares","3. Landmark-Bibliothek","3. Libreria luoghi")) {
            val filtered=HERITAGE_SITES.filter(::siteMatches)
            filtered.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    row.forEach { site ->
                        val fav=favorites.contains(site.id)
                        Surface(
                            Modifier.weight(1f).height(138.dp).clickable{onSite(site)},
                            shape=RoundedCornerShape(18.dp),
                            color=if(site==selectedSite)Color(0xFF173B52)else Color(0xFF101C2C),
                            border=BorderStroke(1.dp,if(site==selectedSite)Color(0xFF00D9FF)else Color(0xFF324A61))
                        ) {
                            Column(Modifier.fillMaxSize().padding(9.dp),verticalArrangement=Arrangement.SpaceBetween) {
                                Box(Modifier.fillMaxWidth().height(70.dp).background(Color(0xFF0A2233),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
                                    HeritageThumbnail(site)
                                    IconButton(
                                        onClick={
                                            val n=favorites.toMutableSet()
                                            if(!n.add(site.id)) n.remove(site.id)
                                            favorites = n.toSet()
                                            prefs.edit().putStringSet("favorites", HashSet(favorites)).apply()
                                        },
                                        modifier=Modifier.align(Alignment.TopEnd).size(32.dp)
                                    ){Icon(if(fav)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(fav)Color(0xFFFF6B8A)else Color(0xFF8297AA))}
                                }
                                Text(site.label(language),fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=2)
                                Text("${countryLabel(site.country,language)} • ${site.city}",fontSize=9.sp,color=Color(0xFF8FA5BA),maxLines=1)
                            }
                        }
                    }
                    if(row.size==1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"4. أسلوب الدمج","4. Composite style","4. Kompozit stili","4. Estilo de composición","4. Compositing-Stil","4. Stile composizione")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                HeritageMode.entries.forEach { m -> FilterChip(selected=selectedMode==m,onClick={onMode(m)},label={Text(m.name)});Spacer(Modifier.width(6.dp)) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                HeritageLight.entries.forEach { l -> FilterChip(selected=selectedLight==l,onClick={onLight(l)},label={Text(l.name)});Spacer(Modifier.width(6.dp)) }
            }
            Text(tx(language,"قوة الدمج","Blend strength","Karışım gücü","Fuerza de mezcla","Mischstärke","Intensità"),fontSize=11.sp,color=Color(0xFF9FB4C8))
            Slider(value=strength,onValueChange=onStrength,valueRange=0.2f..1f)
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick=onRun,enabled=!busy && original!=null && background!=null,modifier=Modifier.fillMaxWidth().height(54.dp)) {
            if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp) else Icon(Icons.Default.AutoAwesome,null)
            Spacer(Modifier.width(8.dp))
            Text(tx(language,"دمج الخلفية الأثرية","Apply heritage background","Miras arka planını uygula","Aplicar fondo patrimonial","Kulturerbe-Hintergrund anwenden","Applica sfondo storico"))
        }
        Spacer(Modifier.height(24.dp))
    }
}



data class TimelineShotUi(val id:String,val index:Int,val start:Double,val end:Double,val duration:Float,val camera:String,val transition:String,val motion:String,val engine:String,val dialogue:String)

private fun parseWaveSamples(json:String): List<Float> = runCatching {
    val arr=JSONObject(json).optJSONArray("samples")?:return@runCatching emptyList()
    List(arr.length()){i->arr.optDouble(i,0.0).toFloat().coerceIn(0f,1f)}
}.getOrDefault(emptyList())

private fun parseCurveSamples(json:String): List<Pair<Float,Float>> = runCatching {
    val arr=JSONObject(json).optJSONArray("curve")?:return@runCatching emptyList()
    if(arr.length()==0) return@runCatching emptyList()
    val raw=List(arr.length()){i-> val o=arr.getJSONObject(i); o.optDouble("at_seconds",0.0).toFloat() to o.optDouble("value",0.0).toFloat() }
    val maxT=max(0.001f,raw.maxOf{it.first}); val minV=raw.minOf{it.second}; val maxV=max(minV+0.001f,raw.maxOf{it.second})
    raw.map{(t,v)->(t/maxT).coerceIn(0f,1f) to ((v-minV)/(maxV-minV)).coerceIn(0f,1f)}
}.getOrDefault(emptyList())

@Composable private fun WaveformStrip(samples:List<Float>, playheadFraction:Float, onScrubFraction:(Float)->Unit) {
    if(samples.isEmpty()) return
    Canvas(Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF07131E),RoundedCornerShape(12.dp)).pointerInput(samples){
        detectHorizontalDragGestures(onDragStart={p->onScrubFraction((p.x/size.width).coerceIn(0f,1f))}){change,_->onScrubFraction((change.position.x/size.width).coerceIn(0f,1f))}
    }) {
        val mid=size.height/2f; val step=size.width/max(1,samples.size-1)
        samples.forEachIndexed{i,v-> val x=i*step; val h=v*size.height*0.44f; drawLine(Color(0xFF70E7FF),Offset(x,mid-h),Offset(x,mid+h),strokeWidth=max(1f,step*0.45f))}
        val px=playheadFraction.coerceIn(0f,1f)*size.width; drawLine(Color(0xFFFFD166),Offset(px,0f),Offset(px,size.height),strokeWidth=3f)
    }
}

@Composable private fun CurveStrip(points:List<Pair<Float,Float>>) {
    if(points.size<2) return
    Canvas(Modifier.fillMaxWidth().height(86.dp).background(Color(0xFF081824),RoundedCornerShape(12.dp))) {
        for(i in 0 until points.lastIndex){val a=points[i];val b=points[i+1];drawLine(Color(0xFFB892FF),Offset(a.first*size.width,(1f-a.second)*size.height),Offset(b.first*size.width,(1f-b.second)*size.height),strokeWidth=3f)}
        points.filterIndexed{i,_->i==0||i==points.lastIndex||i%(max(1,points.size/8))==0}.forEach{p->drawCircle(Color(0xFFFFD166),4f,Offset(p.first*size.width,(1f-p.second)*size.height))}
    }
}

private fun parseTimelineShots(json:String): List<TimelineShotUi> = runCatching {
    val root=JSONObject(json); val tracks=root.getJSONObject("tracks"); val video=tracks.getJSONArray("video"); val motion=tracks.optJSONArray("motion"); val audio=tracks.optJSONArray("audio")
    val motionById=mutableMapOf<String,JSONObject>(); if(motion!=null) for(i in 0 until motion.length()){ val o=motion.getJSONObject(i); motionById[o.optString("shot_id")]=o }
    val audioById=mutableMapOf<String,JSONObject>(); if(audio!=null) for(i in 0 until audio.length()){ val o=audio.getJSONObject(i); audioById[o.optString("shot_id")]=o }
    buildList {
        for(i in 0 until video.length()) {
            val v=video.getJSONObject(i); val id=v.optString("shot_id"); val m=motionById[id]; val a=audioById[id]
            add(TimelineShotUi(id,v.optInt("index",i),v.optDouble("start",0.0),v.optDouble("end",0.0),v.optDouble("duration",5.0).toFloat(),v.optString("camera","default"),v.optString("transition","cut"),m?.optString("motion_preset","idle_natural")?:"idle_natural",m?.optString("engine","")?:"",a?.optString("dialogue","")?:""))
        }
    }
}.getOrDefault(emptyList())

@Composable private fun VisualTimelineEditor(
    shots:List<TimelineShotUi>, selectedId:String, language:AppLanguage, busy:Boolean,
    onSelect:(TimelineShotUi)->Unit, onDuration:(TimelineShotUi,Float)->Unit, onNudge:(TimelineShotUi,Int)->Unit,
    onPreview:(TimelineShotUi)->Unit, onUndo:()->Unit, onRedo:()->Unit, onReload:()->Unit
) {
    if(shots.isEmpty()) return
    val selected=shots.firstOrNull{it.id==selectedId}?:shots.first()
    SectionCard(tx(language,"Timeline مرئي","Visual Timeline","Görsel Timeline","Timeline visual","Visuelle Timeline","Timeline visiva")) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick=onUndo,enabled=!busy,modifier=Modifier.weight(1f)){Icon(Icons.Default.Undo,null);Spacer(Modifier.width(4.dp));Text(tx(language,"تراجع","Undo","Geri al","Deshacer","Rückgängig","Annulla"),fontSize=11.sp)}
            OutlinedButton(onClick=onRedo,enabled=!busy,modifier=Modifier.weight(1f)){Icon(Icons.Default.Redo,null);Spacer(Modifier.width(4.dp));Text(tx(language,"إعادة","Redo","Yinele","Rehacer","Wiederholen","Ripeti"),fontSize=11.sp)}
            IconButton(onClick=onReload,enabled=!busy){Icon(Icons.Default.Refresh,null)}
        }
        Spacer(Modifier.height(8.dp))
        Text(tx(language,"مسار الفيديو / الحركة / الصوت / الترجمة","Video / Motion / Audio / Subtitle tracks","Video / Hareket / Ses / Altyazı","Vídeo / Movimiento / Audio / Subtítulos","Video / Bewegung / Audio / Untertitel","Video / Movimento / Audio / Sottotitoli"),fontSize=10.sp,color=Color(0xFF91A9BE))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            shots.forEach { shot ->
                val active=shot.id==selected.id
                Surface(Modifier.width((92 + (shot.duration*8).toInt()).coerceIn(100,190).dp).height(116.dp).clickable{onSelect(shot)},shape=RoundedCornerShape(16.dp),color=if(active) Color(0xFF12364A) else Color(0xFF0B1824),border=BorderStroke(if(active) 2.dp else 1.dp,if(active) Color(0xFF6FE7FF) else Color(0xFF2A465B))) {
                    Column(Modifier.padding(9.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Movie,null,Modifier.size(16.dp),tint=Color(0xFF7ADFFF));Spacer(Modifier.width(4.dp));Text("#${shot.index+1}",fontWeight=FontWeight.Black,fontSize=12.sp)}
                        Text(shot.id,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=Color(0xFFB9CDE0))
                        Text("${"%.1f".format(shot.duration)}s • ${shot.camera}",fontSize=9.sp,color=Color(0xFF8CA4B8),maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text("⚡ ${shot.motion}",fontSize=9.sp,color=Color(0xFFAADFF0),maxLines=1,overflow=TextOverflow.Ellipsis)
                        if(shot.engine.isNotBlank()) Text(shot.engine,fontSize=8.sp,color=Color(0xFF9CB5C8),maxLines=1,overflow=TextOverflow.Ellipsis)
                        if(shot.dialogue.isNotBlank()) Text("♫ ${shot.dialogue}",fontSize=8.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=Color(0xFFD8B6FF))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(tx(language,"اللقطة المحددة","Selected shot","Seçili çekim","Toma seleccionada","Ausgewählter Shot","Scena selezionata")+": ${selected.id}",fontWeight=FontWeight.Bold,fontSize=12.sp)
        var localDuration by remember(selected.id,selected.duration){ mutableFloatStateOf(selected.duration) }
        Text(tx(language,"المدة","Duration","Süre","Duración","Dauer","Durata")+": ${"%.1f".format(localDuration)}s",fontSize=11.sp,color=Color(0xFF9FB4C8))
        Slider(value=localDuration,onValueChange={localDuration=it},valueRange=1f..min(30f,max(6f,selected.duration+12f)),steps=28)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick={onNudge(selected,-1)},enabled=!busy,modifier=Modifier.weight(1f)){Icon(Icons.Default.ArrowBack,null);Text(" − ")}
            Button(onClick={onDuration(selected,localDuration)},enabled=!busy,modifier=Modifier.weight(1.4f)){Icon(Icons.Default.ContentCut,null);Spacer(Modifier.width(4.dp));Text(tx(language,"Trim","Trim","Kırp","Recortar","Trimmen","Taglia"),fontSize=11.sp)}
            OutlinedButton(onClick={onNudge(selected,1)},enabled=!busy,modifier=Modifier.weight(1f)){Text(" + ");Icon(Icons.Default.ArrowForward,null)}
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick={onPreview(selected)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.PlayArrow,null);Spacer(Modifier.width(6.dp));Text(tx(language,"معاينة اللقطة","Preview shot","Çekimi önizle","Previsualizar toma","Shot-Vorschau","Anteprima scena"))}
    }
}


@Composable private fun ProTimelineStudio(
    jobId: String,
    timelineJson: String,
    selectedShotId: String,
    language: AppLanguage,
    busy: Boolean,
    onTimelineRefresh: (String)->Unit,
    onStatus: (String)->Unit
) {
    if(jobId.isBlank() || timelineJson.isBlank()) return
    val scope=rememberCoroutineScope()
    val shots=remember(timelineJson){parseTimelineShots(timelineJson)}
    if(shots.isEmpty()) return
    val selected=shots.firstOrNull{it.id==selectedShotId}?:shots.first()
    val total=max(0.25f,shots.maxOfOrNull{it.end.toFloat()}?:selected.duration)
    var playhead by remember(jobId){mutableFloatStateOf(0f)}
    var zoom by remember(jobId){mutableFloatStateOf(1f)}
    var inPoint by remember(jobId){mutableFloatStateOf(0f)}
    var outPoint by remember(jobId,total){mutableFloatStateOf(total)}
    var trimHead by remember(selected.id){mutableFloatStateOf(0f)}
    var trimTail by remember(selected.id){mutableFloatStateOf(0f)}
    var markerLabel by remember{mutableStateOf<String>("")}
    var transition by remember(selected.id){mutableStateOf(selected.transition.ifBlank{"cut"})}
    var transitionDuration by remember(selected.id){mutableFloatStateOf(0.35f)}
    var gainDb by remember(selected.id){mutableFloatStateOf(0f)}
    var audioMuted by remember(selected.id){mutableStateOf<Boolean>(false)}
    var subtitleText by remember(selected.id){mutableStateOf(selected.dialogue)}
    var subtitleOffset by remember(selected.id){mutableFloatStateOf(0f)}
    var targetIndex by remember(selected.id){mutableFloatStateOf(selected.index.toFloat())}
    var recoveryInfo by remember{mutableStateOf<String>("")}
    var rangeInfo by remember{mutableStateOf<String>("")}
    var advancedInfo by remember{mutableStateOf<String>("")}
    var waveformJson by remember(selected.id){mutableStateOf<String>("")}
    var curveJson by remember(selected.id){mutableStateOf<String>("")}
    var colorTag by remember(selected.id){mutableStateOf("cyan")}
    var keyframeZoom by remember(selected.id){mutableFloatStateOf(1f)}
    var motionIntensity by remember(selected.id){mutableFloatStateOf(1f)}

    SectionCard(tx(language,"محرر Timeline الاحترافي","Pro Timeline Editor","Profesyonel Timeline","Editor Timeline Pro","Pro-Timeline-Editor","Editor Timeline Pro")) {
        Text(tx(language,"Stage 23000–33500: Timeline + Preview Monitor + SMPTE + Safe Areas + Workspace","Stage 23000–32500: playhead, zoom, In/Out, waveform, snap, keyframes, proxy preview, track controls and ripple trim","Stage 23000–27500 gelişmiş zaman çizelgesi","Stage 23000–27500 timeline avanzado","Stage 23000–27500 erweiterte Timeline","Stage 23000–27500 timeline avanzata"),fontSize=10.sp,color=Color(0xFF8FAEC5))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.ZoomIn,null,tint=Color(0xFF7ADFFF));Spacer(Modifier.width(6.dp));Text("Zoom ${"%.1f".format(zoom)}×",fontWeight=FontWeight.Bold,fontSize=11.sp)}
        Slider(value=zoom,onValueChange={zoom=it},valueRange=0.5f..8f)
        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.PlayCircle,null,tint=Color(0xFF8DFFB3));Spacer(Modifier.width(6.dp));Text("Playhead ${"%.2f".format(playhead)} / ${"%.2f".format(total)} s",fontSize=11.sp)}
        Slider(value=playhead.coerceIn(0f,total),onValueChange={playhead=it},valueRange=0f..total)
        Text("IN ${"%.2f".format(inPoint)}s   →   OUT ${"%.2f".format(outPoint)}s",fontSize=10.sp,color=Color(0xFFFFD77A))
        Slider(value=inPoint.coerceIn(0f,total),onValueChange={inPoint=min(it,outPoint)},valueRange=0f..total)
        Slider(value=outPoint.coerceIn(0f,total),onValueChange={outPoint=max(it,inPoint)},valueRange=0f..total)
        Button(onClick={scope.launch{FaceApiClient.filmTimelineView(jobId,playhead,zoom,inPoint,outPoint,selected.id).onSuccess{onStatus(tx(language,"تم حفظ عرض Timeline ونطاق In/Out","Timeline view and In/Out saved","Timeline kaydedildi","Timeline guardado","Timeline gespeichert","Timeline salvata"))}.onFailure{onStatus(it.message?:"error")}}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Save,null);Spacer(Modifier.width(6.dp));Text(tx(language,"حفظ Playhead / Zoom / In-Out","Save Playhead / Zoom / In-Out","Görünümü kaydet","Guardar vista","Ansicht speichern","Salva vista"))}

        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text(tx(language,"Trim دقيق للرأس والذيل","Precise head/tail trim","Hassas kırpma","Recorte preciso","Präziser Trim","Taglio preciso"),fontWeight=FontWeight.Bold,fontSize=12.sp)
        val trimMax=max(0.1f,selected.duration/2f-0.05f)
        Text("Head ${"%.2f".format(trimHead)}s | Tail ${"%.2f".format(trimTail)}s",fontSize=10.sp)
        Slider(value=trimHead.coerceIn(0f,trimMax),onValueChange={trimHead=it.coerceAtMost(max(0f,selected.duration-trimTail-0.25f))},valueRange=0f..trimMax)
        Slider(value=trimTail.coerceIn(0f,trimMax),onValueChange={trimTail=it.coerceAtMost(max(0f,selected.duration-trimHead-0.25f))},valueRange=0f..trimMax)
        OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelinePreciseTrim(jobId,selected.id,trimHead,trimTail).onSuccess{FaceApiClient.filmTimeline(jobId).onSuccess(onTimelineRefresh);trimHead=0f;trimTail=0f;onStatus(tx(language,"تم تطبيق Trim الدقيق","Precise trim applied","Kırpma uygulandı","Recorte aplicado","Trim angewendet","Taglio applicato"))}.onFailure{onStatus(it.message?:"error")}}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.ContentCut,null);Spacer(Modifier.width(5.dp));Text(tx(language,"تطبيق Trim الدقيق","Apply precise trim","Uygula","Aplicar","Anwenden","Applica"))}

        HorizontalDivider(Modifier.padding(vertical=10.dp))
        OutlinedTextField(value=markerLabel,onValueChange={markerLabel=it},label={Text(tx(language,"اسم العلامة/الفصل","Marker / chapter label","İşaret adı","Nombre del marcador","Marker-Name","Nome marcatore"))},singleLine=true,modifier=Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineAddMarker(jobId,playhead,markerLabel.ifBlank{"Marker"},"marker").onSuccess{markerLabel="";onStatus(tx(language,"تمت إضافة Marker","Marker added","İşaret eklendi","Marcador añadido","Marker hinzugefügt","Marcatore aggiunto"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.AddLocation,null);Text(" Marker")}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineAddMarker(jobId,playhead,markerLabel.ifBlank{"Chapter"},"chapter").onSuccess{markerLabel="";onStatus(tx(language,"تمت إضافة Chapter","Chapter added","Bölüm eklendi","Capítulo añadido","Kapitel hinzugefügt","Capitolo aggiunto"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.BookmarkAdd,null);Text(" Chapter")}
        }

        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text(tx(language,"الانتقال بين اللقطات","Shot transition","Geçiş","Transición","Übergang","Transizione"),fontWeight=FontWeight.Bold,fontSize=12.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){ listOf("cut","fade","dissolve").forEach{t->FilterChip(selected=transition==t,onClick={transition=t},label={Text(t,fontSize=10.sp)},modifier=Modifier.weight(1f))} }
        if(transition!="cut"){Text("${"%.2f".format(transitionDuration)}s",fontSize=10.sp);Slider(value=transitionDuration,onValueChange={transitionDuration=it},valueRange=0.1f..min(3f,max(0.1f,selected.duration/2f)))}
        OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineTransition(jobId,selected.id,transition,transitionDuration).onSuccess{FaceApiClient.filmTimeline(jobId).onSuccess(onTimelineRefresh);onStatus(tx(language,"تم حفظ الانتقال","Transition saved","Geçiş kaydedildi","Transición guardada","Übergang gespeichert","Transizione salvata"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.CompareArrows,null);Spacer(Modifier.width(5.dp));Text(tx(language,"حفظ الانتقال","Save transition","Kaydet","Guardar","Speichern","Salva"))}

        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text(tx(language,"الصوت لكل لقطة","Per-shot audio","Çekim sesi","Audio por toma","Shot-Audio","Audio scena"),fontWeight=FontWeight.Bold,fontSize=12.sp)
        Text("Gain ${"%.1f".format(gainDb)} dB",fontSize=10.sp);Slider(value=gainDb,onValueChange={gainDb=it},valueRange=-24f..12f)
        Row(verticalAlignment=Alignment.CenterVertically){Switch(checked=audioMuted,onCheckedChange={audioMuted=it});Spacer(Modifier.width(6.dp));Text(tx(language,"كتم اللقطة","Mute shot","Sessiz","Silenciar","Stumm","Muto"),fontSize=11.sp)}
        OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineAudio(jobId,selected.id,gainDb,0.25f,0.25f,audioMuted).onSuccess{onStatus(tx(language,"تم حفظ إعدادات الصوت","Audio settings saved","Ses kaydedildi","Audio guardado","Audio gespeichert","Audio salvato"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.GraphicEq,null);Spacer(Modifier.width(5.dp));Text(tx(language,"حفظ الصوت","Save audio","Sesi kaydet","Guardar audio","Audio speichern","Salva audio"))}

        HorizontalDivider(Modifier.padding(vertical=10.dp))
        OutlinedTextField(value=subtitleText,onValueChange={subtitleText=it},label={Text(tx(language,"نص الترجمة لهذه اللقطة","Subtitle for this shot","Altyazı","Subtítulo","Untertitel","Sottotitolo"))},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4)
        Text("Offset ${"%.2f".format(subtitleOffset)}s",fontSize=10.sp);Slider(value=subtitleOffset,onValueChange={subtitleOffset=it},valueRange=-2f..2f)
        OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineSubtitle(jobId,selected.id,subtitleText,subtitleOffset,true).onSuccess{onStatus(tx(language,"تم حفظ الترجمة","Subtitle saved","Altyazı kaydedildi","Subtítulo guardado","Untertitel gespeichert","Sottotitolo salvato"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Subtitles,null);Spacer(Modifier.width(5.dp));Text(tx(language,"حفظ الترجمة","Save subtitle","Kaydet","Guardar","Speichern","Salva"))}

        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text(tx(language,"موضع اللقطة","Shot position","Çekim konumu","Posición","Shot-Position","Posizione"),fontWeight=FontWeight.Bold,fontSize=12.sp)
        Slider(value=targetIndex.coerceIn(0f,max(0,shots.size-1).toFloat()),onValueChange={targetIndex=it},valueRange=0f..max(1,shots.size-1).toFloat(),steps=max(0,shots.size-2))
        OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineMove(jobId,selected.id,targetIndex.toInt()).onSuccess{FaceApiClient.filmTimeline(jobId).onSuccess(onTimelineRefresh);onStatus(tx(language,"تم نقل اللقطة","Shot moved","Çekim taşındı","Toma movida","Shot verschoben","Scena spostata"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.SwapHoriz,null);Spacer(Modifier.width(5.dp));Text(tx(language,"نقل إلى الموضع","Move to position","Konuma taşı","Mover a posición","Verschieben","Sposta")+" #${targetIndex.toInt()+1}")}

        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Button(onClick={scope.launch{FaceApiClient.filmTimelineAutosave(jobId).onSuccess{recoveryInfo=it;onStatus(tx(language,"تم AutoSave","Autosave completed","Otomatik kayıt tamam","Autoguardado","Autosave abgeschlossen","Autosave completato"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.CloudDone,null);Text(" AutoSave")}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineRenderRange(jobId).onSuccess{rangeInfo=it;onStatus(tx(language,"تم إنشاء Render Range","Render range ready","Render aralığı hazır","Rango listo","Render-Bereich bereit","Intervallo pronto"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.MovieFilter,null);Text(" Range")}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineRecovery(jobId).onSuccess{recoveryInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.HealthAndSafety,null);Text(tx(language,"Recovery","Recovery","Recovery","Recovery","Recovery","Recovery"),fontSize=10.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineRestoreRecovery(jobId).onSuccess{FaceApiClient.filmTimeline(jobId).onSuccess(onTimelineRefresh);recoveryInfo=it;onStatus(tx(language,"تم استرجاع آخر حفظ","Recovery restored","Geri yüklendi","Recuperado","Wiederhergestellt","Ripristinato"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.Restore,null);Text(tx(language,"استعادة","Restore","Geri yükle","Restaurar","Wiederherstellen","Ripristina"),fontSize=10.sp)}
        }
        if(recoveryInfo.isNotBlank()) Text(recoveryInfo.take(360),fontSize=8.sp,color=Color(0xFF89A5B8),maxLines=5,overflow=TextOverflow.Ellipsis)
        if(rangeInfo.isNotBlank()) Text(rangeInfo.take(360),fontSize=8.sp,color=Color(0xFF89A5B8),maxLines=5,overflow=TextOverflow.Ellipsis)

        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text(tx(language,"Advanced Timeline — Stage 28000–33000","Advanced Timeline — Stage 28000–33000","Gelişmiş Timeline","Timeline Avanzado","Advanced Timeline","Timeline Avanzata"),fontWeight=FontWeight.Bold,fontSize=12.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineAdvanced(jobId).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.ViewTimeline,null);Text(" Studio",fontSize=10.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineWaveform(jobId,selected.id,160).onSuccess{waveformJson=it;advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.GraphicEq,null);Text(" Wave",fontSize=10.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineSnap(jobId,playhead).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.CenterFocusStrong,null);Text(" Snap",fontSize=10.sp)}
        }
        Text("Camera keyframe zoom ${"%.2f".format(keyframeZoom)}×",fontSize=10.sp);Slider(value=keyframeZoom,onValueChange={keyframeZoom=it},valueRange=0.5f..2.5f)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineCameraKeyframe(jobId,selected.id,(playhead-selected.start.toFloat()).coerceIn(0f,selected.duration),keyframeZoom).onSuccess{advancedInfo=it;onStatus(tx(language,"تمت إضافة Camera Keyframe","Camera keyframe added","Kamera keyframe eklendi","Keyframe añadido","Keyframe hinzugefügt","Keyframe aggiunto"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.Videocam,null);Text(" Camera KF",fontSize=9.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineRippleTrim(jobId,selected.id,0.25f).onSuccess{advancedInfo=it;FaceApiClient.filmTimeline(jobId).onSuccess(onTimelineRefresh)}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.ContentCut,null);Text(" Ripple +",fontSize=9.sp)}
        }
        Text("Motion intensity ${"%.2f".format(motionIntensity)}",fontSize=10.sp);Slider(value=motionIntensity,onValueChange={motionIntensity=it},valueRange=0f..2f)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineMotionKeyframe(jobId,selected.id,(playhead-selected.start.toFloat()).coerceIn(0f,selected.duration),motionIntensity).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.Animation,null);Text(" Motion KF",fontSize=9.sp)}
            Button(onClick={scope.launch{FaceApiClient.filmTimelineProxyPreview(jobId,inPoint,outPoint).onSuccess{advancedInfo=it;onStatus(tx(language,"تم إنشاء Proxy Preview","Proxy preview created","Proxy hazır","Proxy creado","Proxy erstellt","Proxy creato"))}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.Preview,null);Text(" Proxy",fontSize=9.sp)}
        }
        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text(tx(language,"Stage 33000 — Waveform / Scrub / J-K-L / Curves","Stage 33000 — Waveform / Scrub / J-K-L / Curves","Stage 33000","Stage 33000","Stage 33000","Stage 33000"),fontWeight=FontWeight.Bold,fontSize=12.sp)
        val waveSamples=remember(waveformJson){parseWaveSamples(waveformJson)}
        if(waveSamples.isNotEmpty()) WaveformStrip(waveSamples,(playhead-selected.start.toFloat())/max(0.01f,selected.duration)){f->
            playhead=(selected.start.toFloat()+f*selected.duration).coerceIn(0f,total)
            scope.launch{FaceApiClient.filmTimelineScrub(jobId,playhead).onSuccess{advancedInfo=it}}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelinePlayback(jobId,"J").onSuccess{advancedInfo=it}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.FastRewind,null);Text(" J")}
            Button(onClick={scope.launch{FaceApiClient.filmTimelinePlayback(jobId,"K").onSuccess{advancedInfo=it}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.Pause,null);Text(" K")}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelinePlayback(jobId,"L").onSuccess{advancedInfo=it}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.FastForward,null);Text(" L")}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineCurve(jobId,selected.id,"camera","zoom",96).onSuccess{curveJson=it;advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.ShowChart,null);Text(" Curve",fontSize=9.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineProxyCache(jobId).onSuccess{advancedInfo=it}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.Cached,null);Text(" Cache",fontSize=9.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineProxyCachePrune(jobId).onSuccess{advancedInfo=it}}},modifier=Modifier.weight(1f)){Icon(Icons.Default.DeleteSweep,null);Text(" Prune",fontSize=9.sp)}
        }
        CurveStrip(remember(curveJson){parseCurveSamples(curveJson)})
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            listOf("cyan","blue","green","amber","orange","red","purple","silver").forEach{c->FilterChip(selected=colorTag==c,onClick={colorTag=c;scope.launch{FaceApiClient.filmTimelineColorTag(jobId,"shot",selected.id,c).onSuccess{advancedInfo=it}}},label={Text(c,fontSize=9.sp)})}
        }
        HorizontalDivider(Modifier.padding(vertical=10.dp))
        Text(tx(language,"Stage 34000 FIXED — Preview Monitor / SMPTE / Frame Step / Safe Areas","Stage 34000 FIXED — Preview Monitor / SMPTE / Frame Step / Safe Areas","Stage 34000 FIXED — Önizleme Monitörü","Stage 34000 FIXED — Monitor de vista previa","Stage 34000 FIXED — Vorschaumonitor","Stage 34000 FIXED — Monitor anteprima"),fontWeight=FontWeight.Bold,fontSize=12.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineFrameStep(jobId,-1).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Text("◀ Frame",fontSize=9.sp)}
            Button(onClick={scope.launch{FaceApiClient.filmTimelineMonitor(jobId).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Text("Monitor",fontSize=9.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineFrameStep(jobId,1).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Text("Frame ▶",fontSize=9.sp)}
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            listOf("off","broadcast","social","cinema").forEach{preset->
                AssistChip(onClick={scope.launch{FaceApiClient.filmTimelineSafeArea(jobId,preset).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},label={Text("Safe $preset",fontSize=8.sp)})
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineOnion(jobId,true,1,0.35f).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Text("Onion On",fontSize=9.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineOnion(jobId,false).onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Text("Onion Off",fontSize=9.sp)}
            OutlinedButton(onClick={scope.launch{FaceApiClient.filmTimelineWorkspace(jobId,"edit").onSuccess{advancedInfo=it}.onFailure{onStatus(it.message?:"error")}}},modifier=Modifier.weight(1f)){Text("Workspace",fontSize=9.sp)}
        }
        if(advancedInfo.isNotBlank()) Text(advancedInfo.take(520),fontSize=8.sp,color=Color(0xFF89A5B8),maxLines=7,overflow=TextOverflow.Ellipsis)
    }
}

@Composable private fun FilmStudioScreen(source: Bitmap?, language: AppLanguage, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var story by remember { mutableStateOf<String>("") }
    var duration by remember { mutableFloatStateOf(60f) }
    var preset by remember { mutableStateOf("cinematic") }
    var renderMode by remember { mutableStateOf("auto") }
    var aspectRatio by remember { mutableStateOf("16:9") }
    var exportPreset by remember { mutableStateOf("720p") }
    var motionPreset by remember { mutableStateOf("idle_natural") }
    var motionStatus by remember { mutableStateOf<String>("") }
    var currentShot by remember { mutableStateOf<String>("") }
    var renderedShots by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<String>("") }
    var plan by remember { mutableStateOf<String>("") }
    var timelineJson by remember { mutableStateOf<String>("") }
    var selectedTimelineShotId by remember { mutableStateOf<String>("") }
    var busy by remember { mutableStateOf<Boolean>(false) }
    var jobId by remember { mutableStateOf<String>("") }
    var jobState by remember { mutableStateOf<String>("") }
    var videoReady by remember { mutableStateOf<Boolean>(false) }
    var drivingMotionId by remember { mutableStateOf("my_motion") }
    var drivingMotionLabel by remember { mutableStateOf("My Motion") }
    var selectedDrivingMotionId by remember { mutableStateOf("my_motion") }
    var privacyDeleteOriginal by remember { mutableStateOf<Boolean>(false) }
    val motionVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            busy = true
            runCatching {
                val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Cannot read video") }
                val name = drivingMotionId.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_").take(48).ifBlank { "my_motion" }
                FaceApiClient.filmUploadDrivingMotion(bytes, "motion.mp4", name, drivingMotionLabel.ifBlank { name }).getOrThrow()
            }.onSuccess { motionStatus = it.take(1800); selectedDrivingMotionId = drivingMotionId.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_").take(48) }
             .onFailure { motionStatus = it.message ?: "upload error" }
            busy = false
        }
    }
    val presets = listOf(
        "cinematic" to tx(language,"سينمائي","Cinematic","Sinematik","Cinemático","Kinematisch","Cinematico"),
        "historical" to tx(language,"تاريخي","Historical","Tarihî","Histórico","Historisch","Storico"),
        "travel" to tx(language,"سياحي","Travel","Seyahat","Viaje","Reise","Viaggio"),
        "drama" to tx(language,"دراما","Drama","Drama","Drama","Drama","Dramma"),
        "documentary" to tx(language,"وثائقي","Documentary","Belgesel","Documental","Dokumentar","Documentario"),
        "advert" to tx(language,"إعلان","Commercial","Reklam","Anuncio","Werbung","Pubblicità")
    )
    val motions = listOf(
        Triple("idle_natural", tx(language,"وقفة طبيعية","Natural Idle","Doğal duruş","Reposo natural","Natürliche Haltung","Posa naturale"), "LivePortrait"),
        Triple("look_camera", tx(language,"ينظر للكاميرا","Look at Camera","Kameraya bak","Mirar cámara","In Kamera schauen","Guarda camera"), "LivePortrait"),
        Triple("smile", tx(language,"ابتسامة","Smile","Gülümseme","Sonrisa","Lächeln","Sorriso"), "LivePortrait"),
        Triple("nod", tx(language,"يهز رأسه","Nod","Başını salla","Asentir","Nicken","Annuisce"), "LivePortrait"),
        Triple("reaction", tx(language,"رد فعل","Reaction","Tepki","Reacción","Reaktion","Reazione"), "LivePortrait"),
        Triple("wave", tx(language,"يلوح بيده","Wave","El salla","Saludar","Winken","Saluta"), "MimicMotion"),
        Triple("walk_forward", tx(language,"يمشي للأمام","Walk Forward","İleri yürü","Caminar adelante","Vorwärts gehen","Cammina avanti"), "MimicMotion"),
        Triple("sit_down", tx(language,"يجلس","Sit Down","Otur","Sentarse","Hinsetzen","Siediti"), "MimicMotion"),
        Triple("talking", tx(language,"يتحدث","Talking","Konuşma","Hablar","Sprechen","Parla"), "EchoMimic V2"),
        Triple("turn_side", tx(language,"التفات سينمائي","Cinematic Turn","Sinematik dönüş","Giro cinematográfico","Filmische Drehung","Rotazione cinematica"), "Wan 2.2"),
        Triple("hero_pose", tx(language,"وقفة بطولية","Hero Pose","Kahraman pozu","Pose heroica","Heldenpose","Posa eroica"), "Wan 2.2"),
        Triple("travel_reveal", tx(language,"كشف المكان","Travel Reveal","Mekân gösterimi","Revelar lugar","Ortsenthüllung","Rivela luogo"), "Wan 2.2")
    )
    fun extractJob(json: String): String = runCatching { JSONObject(json).optString("job_id") }.getOrDefault("")
    fun updateJobState(json: String) {
        runCatching {
            val obj = JSONObject(json)
            jobState = obj.optString("state")
            videoReady = obj.optBoolean("video_ready", false)
            val progress = obj.optInt("progress", 0)
            val engine = obj.optString("engine")
            currentShot = obj.optString("current_shot")
            renderedShots = obj.optInt("rendered_shots", 0)
            status = tx(language,"حالة الفيلم","Film status","Film durumu","Estado del vídeo","Filmstatus","Stato film") + ": $jobState • $progress%" + if(engine.isNotBlank()) " • $engine" else ""
        }
    }
    fun saveVideo(bytes: ByteArray): Boolean = runCatching {
        val name = "NEXVARY-FaceLab-${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/NEXVARY FaceLab")
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: error("Cannot create video")
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Cannot write video")
        true
    }.getOrDefault(false)

    fun saveAndOpenPreview(bytes: ByteArray): Boolean = runCatching {
        val name = "NEXVARY-Motion-${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply { put(MediaStore.Video.Media.DISPLAY_NAME,name); put(MediaStore.Video.Media.MIME_TYPE,"video/mp4"); if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/NEXVARY FaceLab/Motion Previews") }
        val uri=context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values)?:error("Cannot create preview")
        context.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}?:error("Cannot write preview")
        context.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"video/mp4");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)})
        true
    }.getOrDefault(false)

    LaunchedEffect(jobId) {
        if (jobId.isBlank()) return@LaunchedEffect
        jobState = "queued"
        repeat(300) {
            val result = FaceApiClient.filmJob(jobId)
            result.onSuccess { updateJobState(it) }.onFailure { status = it.message ?: "error" }
            if (jobState == "completed" || jobState == "failed") return@LaunchedEffect
            delay(1000)
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp)) {
        ScreenHeader(
            tx(language,"استوديو الأفلام القصيرة","Short Film Studio","Kısa Film Stüdyosu","Estudio de cortometrajes","Kurzfilm-Studio","Studio cortometraggi"),
            tx(language,"من الصورة النهائية إلى Storyboard ثم Motion Job وفيديو MP4","From final portrait to storyboard, motion job and MP4 video","Son portreden storyboard ve MP4 videoya","Del retrato final al storyboard y vídeo MP4","Vom finalen Porträt zum Storyboard und MP4","Dal ritratto finale allo storyboard e video MP4"),
            Icons.Default.MovieCreation
        )
        source?.let { bmp ->
            Surface(Modifier.fillMaxWidth().height(260.dp), shape=RoundedCornerShape(24.dp), color=Color(0xFF091422), border=BorderStroke(1.dp,Color(0xFF315A72))) {
                Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale=ContentScale.Fit)
            }
        } ?: Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),color=Color(0xFF24191B),border=BorderStroke(1.dp,Color(0xFF6B3A42))) {
            Text(tx(language,"أنهِ تعديل الصورة والملابس والخلفية أولًا ثم افتح الاستوديو","Finish portrait, outfit and background first","Önce portre, kıyafet ve arka planı tamamlayın","Termina primero retrato, ropa y fondo","Zuerst Porträt, Outfit und Hintergrund fertigstellen","Completa prima ritratto, abito e sfondo"),Modifier.padding(14.dp),color=Color(0xFFFFC0C8))
        }
        Spacer(Modifier.height(12.dp))
        SectionCard(tx(language,"1. فكرة الفيلم","1. Film idea","1. Film fikri","1. Idea de la película","1. Filmidee","1. Idea del film")) {
            OutlinedTextField(
                value=story,onValueChange={story=it},modifier=Modifier.fillMaxWidth(),minLines=4,maxLines=8,
                label={Text(tx(language,"اكتب القصة أو الحركة المطلوبة","Write story or desired action","Hikayeyi veya hareketi yaz","Escribe la historia o acción","Geschichte oder Bewegung schreiben","Scrivi storia o azione"))},
                placeholder={Text(tx(language,"مثال: يمشي أمام الأهرامات وقت الغروب ثم يلتفت للكاميرا ويبتسم","Example: walks by the pyramids at sunset, turns to camera and smiles","Örnek: piramitlerde gün batımında yürür","Ejemplo: camina ante las pirámides al atardecer","Beispiel: geht bei Sonnenuntergang an den Pyramiden","Esempio: cammina davanti alle piramidi al tramonto"))}
            )
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"2. Motion Studio","2. Motion Studio","2. Motion Studio","2. Motion Studio","2. Motion Studio","2. Motion Studio")) {
            Text(tx(language,"اختر الحركة الأساسية للشخصية. سيقوم النظام بتوجيه كل لقطة إلى المحرك الأنسب تلقائيًا.","Choose the character's primary motion. Each shot is routed to the most suitable local engine automatically.","Karakterin ana hareketini seçin. Her çekim uygun yerel motora yönlendirilir.","Elige el movimiento principal. Cada toma se dirige al motor local adecuado.","Wählen Sie die Hauptbewegung. Jede Aufnahme wird der passenden lokalen Engine zugewiesen.","Scegli il movimento principale. Ogni scena viene assegnata al motore locale adatto."),fontSize=11.sp,color=Color(0xFF8DA4B9))
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                motions.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        row.forEach { item ->
                            val selected = motionPreset == item.first
                            Surface(Modifier.weight(1f).clickable{motionPreset=item.first},shape=RoundedCornerShape(16.dp),color=if(selected) Color(0xFF123A49) else Color(0xFF0A1420),border=BorderStroke(1.dp,if(selected) Color(0xFF70E7FF) else Color(0xFF25394C))) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(verticalAlignment=Alignment.CenterVertically) { Icon(if(selected) Icons.Default.PlayCircle else Icons.Default.DirectionsRun,null,Modifier.size(18.dp),tint=if(selected)Color(0xFF79F0FF)else Color(0xFF8096A9));Spacer(Modifier.width(6.dp));Text(item.second,fontWeight=FontWeight.Bold,fontSize=11.sp,maxLines=1) }
                                    Spacer(Modifier.height(3.dp));Text(item.third,fontSize=9.sp,color=Color(0xFF7F95AA))
                                }
                            }
                        }
                        if(row.size==1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={scope.launch{busy=true;val a=FaceApiClient.filmMotions().getOrNull().orEmpty();val b=FaceApiClient.filmMotionLibrary().getOrNull().orEmpty();motionStatus=(a+"\n"+b).take(1800);busy=false}},modifier=Modifier.weight(1f)){Icon(Icons.Default.LibraryAddCheck,null);Spacer(Modifier.width(5.dp));Text(tx(language,"مكتبة الحركة","Motion Library","Hareket Kitaplığı","Biblioteca","Bewegungsbibliothek","Libreria"),fontSize=10.sp)}
                OutlinedButton(onClick={scope.launch{busy=true;FaceApiClient.filmDoctor().onSuccess{motionStatus=it.take(1800)}.onFailure{motionStatus=it.message?:"error"};busy=false}},modifier=Modifier.weight(1f)){Icon(Icons.Default.Memory,null);Spacer(Modifier.width(5.dp));Text(tx(language,"Engine Doctor","Engine Doctor","Engine Doctor","Engine Doctor","Engine Doctor","Engine Doctor"),fontSize=10.sp)}
            }
            Spacer(Modifier.height(10.dp))
            Text(tx(language,"إضافة حركة خاصة من الهاتف","Add private motion from phone","Telefondan özel hareket ekle","Añadir movimiento privado","Private Bewegung hinzufügen","Aggiungi movimento privato"),fontWeight=FontWeight.Bold,fontSize=12.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=drivingMotionId,onValueChange={drivingMotionId=it},modifier=Modifier.weight(1f),singleLine=true,label={Text("motion_id")})
                OutlinedTextField(value=drivingMotionLabel,onValueChange={drivingMotionLabel=it},modifier=Modifier.weight(1f),singleLine=true,label={Text(tx(language,"الاسم","Label","Ad","Nombre","Name","Nome"))})
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={motionVideoPicker.launch("video/*")},enabled=!busy,modifier=Modifier.weight(1f)){Icon(Icons.Default.VideoLibrary,null);Spacer(Modifier.width(5.dp));Text(tx(language,"اختيار فيديو حركة","Pick Motion Video","Hareket videosu seç","Elegir vídeo","Video wählen","Scegli video"),fontSize=10.sp)}
                OutlinedButton(onClick={scope.launch{busy=true;FaceApiClient.filmDrivingMotionPreview(selectedDrivingMotionId.ifBlank{drivingMotionId}).onSuccess{saveAndOpenPreview(it)}.onFailure{motionStatus=it.message?:"preview error"};busy=false}},enabled=!busy,modifier=Modifier.weight(1f)){Icon(Icons.Default.PlayArrow,null);Spacer(Modifier.width(5.dp));Text(tx(language,"معاينة الحركة","Preview Motion","Önizle","Vista previa","Vorschau","Anteprima"),fontSize=10.sp)}
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=privacyDeleteOriginal,onCheckedChange={privacyDeleteOriginal=it})
                Text(tx(language,"وضع الخصوصية: بعد إنشاء Motion Template احذف فيديو الحركة الأصلي","Privacy mode: delete original driving video after creating Motion Template","Gizlilik: şablon sonrası videoyu sil","Privacidad: borrar vídeo tras crear plantilla","Datenschutz: Video nach Template löschen","Privacy: elimina video dopo il template"),fontSize=9.sp,color=Color(0xFF8DA4B9))
            }
            OutlinedButton(onClick={scope.launch{val src=source;if(src==null){motionStatus=tx(language,"اختر صورة أولًا","Choose a source portrait first","Önce portre seç","Elige retrato","Porträt wählen","Scegli ritratto");return@launch};busy=true;FaceApiClient.filmMakeMotionTemplate(src,selectedDrivingMotionId.ifBlank{drivingMotionId},privacyDeleteOriginal).onSuccess{motionStatus=it.take(1800)}.onFailure{motionStatus=it.message?:"template error"};busy=false}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Security,null);Spacer(Modifier.width(6.dp));Text(tx(language,"إنشاء Motion Template خاص","Create Private Motion Template","Özel hareket şablonu oluştur","Crear plantilla privada","Privates Motion-Template erstellen","Crea template privato"))}
            if(motionStatus.isNotBlank()){Spacer(Modifier.height(8.dp));Text(motionStatus,fontSize=9.sp,color=Color(0xFF8FC2D8),maxLines=12,overflow=TextOverflow.Ellipsis)}
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"3. النوع والمدة","3. Style & duration","3. Stil ve süre","3. Estilo y duración","3. Stil & Dauer","3. Stile e durata")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                presets.forEach { (code,label) -> FilterChip(selected=preset==code,onClick={preset=code},label={Text(label)});Spacer(Modifier.width(7.dp)) }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(tx(language,"مدة الفيلم","Film duration","Film süresi","Duración","Filmdauer","Durata"),fontWeight=FontWeight.Bold)
                Text("${duration.toInt()} s",color=Color(0xFF72E9FF))
            }
            Slider(value=duration,onValueChange={duration=it},valueRange=30f..300f,steps=8)
            Text(tx(language,"الخطة تدعم 30 ثانية إلى 5 دقائق. Stage 4000 يولد معاينة حركة فعلية قصيرة، والمحرك AI المحلي يولد الحركة الجسدية عند تركيبه.","Plan supports 30 seconds to 5 minutes. Stage 4000 creates a real short motion preview; configured local AI generates body motion.","Plan 30 saniye–5 dakika destekler. Stage 4000 gerçek kısa hareket önizlemesi üretir.","El plan admite 30 s–5 min. Stage 4000 crea una previsualización de movimiento real.","Der Plan unterstützt 30 s–5 min. Stage 4000 erzeugt eine echte kurze Bewegungsvorschau.","Il piano supporta 30 s–5 min. Stage 4000 crea una vera anteprima di movimento."),fontSize=11.sp,color=Color(0xFF879AAF))
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"4. محرك التوليد","4. Render engine","3. Render motoru","3. Motor de render","3. Render-Engine","3. Motore render")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf(
                    "auto" to tx(language,"تلقائي","Auto","Otomatik","Automático","Auto","Auto"),
                    "preview" to tx(language,"معاينة MP4","MP4 Preview","MP4 Önizleme","Vista MP4","MP4-Vorschau","Anteprima MP4"),
                    "ai" to tx(language,"AI محلي","Local AI","Yerel AI","IA local","Lokale KI","IA locale")
                ).forEach { (code,label) -> FilterChip(selected=renderMode==code,onClick={renderMode=code},label={Text(label)});Spacer(Modifier.width(7.dp)) }
            }
            Text(tx(language,"Auto يستخدم AI إذا تم ربط Adapter محلي مُدقَّق، وإلا يستخدم FFmpeg لإنشاء فيديو MP4 متعدد اللقطات.","Auto uses an audited local AI adapter when configured; otherwise FFmpeg creates a multi-shot MP4 preview.","Auto denetlenmiş yerel AI varsa onu, yoksa çoklu çekim FFmpeg önizlemesini kullanır.","Auto usa IA local auditada; si no, crea una vista MP4 multiclip.","Auto nutzt lokale KI, sonst eine mehrteilige FFmpeg-Vorschau.","Auto usa IA locale, altrimenti anteprima multi-shot FFmpeg."),fontSize=10.sp,color=Color(0xFF8DA4B9))
            Spacer(Modifier.height(10.dp))
            Text(tx(language,"نسبة الإطار","Aspect ratio","En-boy oranı","Relación de aspecto","Seitenverhältnis","Formato"),fontWeight=FontWeight.Bold,fontSize=12.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf("16:9","9:16","1:1").forEach { code -> FilterChip(selected=aspectRatio==code,onClick={aspectRatio=code},label={Text(code)});Spacer(Modifier.width(7.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(tx(language,"جودة التصدير","Export quality","Dışa aktarma kalitesi","Calidad de exportación","Exportqualität","Qualità export"),fontWeight=FontWeight.Bold,fontSize=12.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf("preview","720p","1080p").forEach { code -> FilterChip(selected=exportPreset==code,onClick={exportPreset=code},label={Text(code.uppercase())});Spacer(Modifier.width(7.dp)) }
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(tx(language,"5. ثبات الشخصية","5. Continuity locks","4. Süreklilik kilitleri","4. Bloqueos de continuidad","4. Kontinuität","4. Continuità")) {
            listOf(
                tx(language,"قفل الوجه والشخصية","Face / Character Lock","Yüz / karakter kilidi","Rostro/personaje","Gesicht/Charakter","Volto/personaggio"),
                tx(language,"قفل الملابس","Wardrobe Lock","Kıyafet kilidi","Vestuario","Outfit","Abbigliamento"),
                tx(language,"قفل الموقع والخلفية","Location Lock","Konum kilidi","Ubicación","Ort","Luogo"),
                tx(language,"استمرارية الإضاءة والكاميرا","Lighting & Camera Continuity","Işık/kamera sürekliliği","Luz/cámara","Licht/Kamera","Luce/camera")
            ).forEach { Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Lock,null,tint=Color(0xFF91FFD1));Spacer(Modifier.width(8.dp));Text(it,fontSize=12.sp)} }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick={
            if(story.isBlank()){status=tx(language,"اكتب فكرة الفيلم أولًا","Write a film idea first","Önce film fikri yazın","Escribe una idea","Filmidee eingeben","Scrivi un'idea")}
            else scope.launch{busy=true;status=tx(language,"يبني المخرج الذكي الـStoryboard…","AI Director is building the storyboard…","AI yönetmen storyboard hazırlıyor…","Director IA crea el storyboard…","KI-Regisseur erstellt Storyboard…","Regista IA crea storyboard…");FaceApiClient.filmPlan(story,duration.toInt(),preset,aspectRatio,motionPreset).onSuccess{plan=it;status=tx(language,"تم إنشاء مخطط الفيلم","Film plan created","Film planı oluşturuldu","Plan creado","Filmplan erstellt","Piano creato")}.onFailure{status=it.message?:"error"};busy=false}
        },enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp)){
            if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp) else Icon(Icons.Default.AutoAwesome,null)
            Spacer(Modifier.width(8.dp));Text(tx(language,"إنشاء STORYBOARD","CREATE STORYBOARD","STORYBOARD OLUŞTUR","CREAR STORYBOARD","STORYBOARD ERSTELLEN","CREA STORYBOARD"),fontWeight=FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick={
            if(source==null){status=tx(language,"لا توجد صورة نهائية","No final image","Son görüntü yok","No hay imagen final","Kein finales Bild","Nessuna immagine finale")}
            else if(story.isBlank()){status=tx(language,"اكتب فكرة الفيلم أولًا","Write a film idea first","Önce film fikri yazın","Escribe una idea","Filmidee eingeben","Scrivi un'idea")}
            else scope.launch {
                busy=true; videoReady=false; jobState=""; status=tx(language,"تم إرسال مهمة الفيديو…","Video job submitted…","Video işi gönderildi…","Tarea de vídeo enviada…","Videoauftrag gesendet…","Job video inviato…")
                FaceApiClient.filmRender(source,story,duration.toInt(),preset,renderMode,aspectRatio,exportPreset,motionPreset).onSuccess { json ->
                    jobId=extractJob(json); status=if(jobId.isNotBlank()) tx(language,"بدأت مهمة الفيديو","Video job started","Video işi başladı","Tarea iniciada","Videoauftrag gestartet","Job video avviato")+": ${jobId.take(10)}…" else json
                }.onFailure { status=it.message?:"error" }
                busy=false
            }
        },enabled=!busy && source!=null,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF145B72))) {
            Icon(Icons.Default.PlayCircle,null);Spacer(Modifier.width(8.dp));Text(tx(language,"توليد أول فيديو","GENERATE FIRST VIDEO","İLK VİDEOYU ÜRET","GENERAR PRIMER VÍDEO","ERSTES VIDEO ERZEUGEN","GENERA PRIMO VIDEO"),fontWeight=FontWeight.Black)
        }
        if(jobId.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick={scope.launch{busy=true;FaceApiClient.filmJob(jobId).onSuccess{updateJobState(it)}.onFailure{status=it.message?:"error"};busy=false}},modifier=Modifier.fillMaxWidth()){
                Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(7.dp));Text(tx(language,"تحديث حالة التوليد","Refresh render status","Render durumunu yenile","Actualizar estado","Renderstatus aktualisieren","Aggiorna stato"))
            }
            if(currentShot.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(tx(language,"اللقطة الحالية","Current shot","Geçerli çekim","Toma actual","Aktuelle Aufnahme","Scena corrente") + ": $currentShot • $renderedShots",fontSize=11.sp,color=Color(0xFF8EDCFF))
            }
            if(jobState in listOf("queued","running","cancelling")) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick={scope.launch{FaceApiClient.filmCancel(jobId).onSuccess{updateJobState(it)}.onFailure{status=it.message?:"error"}}},modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.StopCircle,null);Spacer(Modifier.width(7.dp));Text(tx(language,"إلغاء التوليد","Cancel render","Render iptal","Cancelar render","Render abbrechen","Annulla render"))
                }
            }
            if(jobState in listOf("failed","cancelled")) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick={scope.launch{FaceApiClient.filmRetry(jobId).onSuccess{updateJobState(it)}.onFailure{status=it.message?:"error"}}},modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.Replay,null);Spacer(Modifier.width(7.dp));Text(tx(language,"إعادة المحاولة","Retry render","Yeniden dene","Reintentar","Erneut versuchen","Riprova"))
                }
            }
            if(videoReady) {
                Spacer(Modifier.height(8.dp))
                Button(onClick={scope.launch{busy=true;status=tx(language,"يتم حفظ الفيديو…","Saving video…","Video kaydediliyor…","Guardando vídeo…","Video wird gespeichert…","Salvataggio video…");FaceApiClient.filmVideo(jobId).onSuccess{bytes->status=if(saveVideo(bytes))tx(language,"تم حفظ الفيديو في Movies/NEXVARY FaceLab","Video saved to Movies/NEXVARY FaceLab","Video kaydedildi","Vídeo guardado","Video gespeichert","Video salvato") else tx(language,"تعذر حفظ الفيديو","Could not save video","Video kaydedilemedi","No se pudo guardar","Video konnte nicht gespeichert werden","Impossibile salvare")}.onFailure{status=it.message?:"error"};busy=false}},modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.Download,null);Spacer(Modifier.width(7.dp));Text(tx(language,"حفظ الفيديو MP4","Save MP4 video","MP4 videoyu kaydet","Guardar vídeo MP4","MP4 speichern","Salva MP4"))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if(jobId.isNotBlank()) {
            val timelineShots = remember(timelineJson) { parseTimelineShots(timelineJson) }
            if(timelineShots.isNotEmpty()) {
                VisualTimelineEditor(
                    shots=timelineShots, selectedId=selectedTimelineShotId, language=language, busy=busy,
                    onSelect={ selectedTimelineShotId=it.id },
                    onDuration={ shot,newDur -> scope.launch { busy=true; FaceApiClient.filmTimelineDuration(jobId,shot.id,newDur).onSuccess{timelineJson=it; selectedTimelineShotId=shot.id; status=tx(language,"تم تعديل مدة اللقطة","Shot duration updated","Çekim süresi güncellendi","Duración actualizada","Shot-Dauer aktualisiert","Durata aggiornata")}.onFailure{status=it.message?:"error"}; busy=false } },
                    onNudge={ shot,dir -> scope.launch { busy=true; FaceApiClient.filmTimelineNudge(jobId,shot.id,dir).onSuccess{timelineJson=it; selectedTimelineShotId=shot.id}.onFailure{status=it.message?:"error"}; busy=false } },
                    onPreview={ shot -> scope.launch { busy=true; FaceApiClient.filmShotPreview(jobId,shot.id).onSuccess{bytes->status=if(saveAndOpenPreview(bytes)) tx(language,"تم فتح معاينة اللقطة","Shot preview opened","Çekim önizlemesi açıldı","Vista previa abierta","Shot-Vorschau geöffnet","Anteprima aperta") else tx(language,"تعذر فتح المعاينة","Could not open preview","Önizleme açılamadı","No se pudo abrir","Vorschau konnte nicht geöffnet werden","Impossibile aprire")}.onFailure{status=it.message?:"error"}; busy=false } },
                    onUndo={ scope.launch { busy=true; FaceApiClient.filmTimelineUndo(jobId).onSuccess{timelineJson=it; status=tx(language,"تم التراجع","Undo applied","Geri alındı","Deshecho","Rückgängig","Annullato")}.onFailure{status=it.message?:"error"}; busy=false } },
                    onRedo={ scope.launch { busy=true; FaceApiClient.filmTimelineRedo(jobId).onSuccess{timelineJson=it; status=tx(language,"تمت الإعادة","Redo applied","Yinelendi","Rehecho","Wiederholt","Ripristinato")}.onFailure{status=it.message?:"error"}; busy=false } },
                    onReload={ scope.launch { busy=true; FaceApiClient.filmTimeline(jobId).onSuccess{timelineJson=it; if(selectedTimelineShotId.isBlank()) selectedTimelineShotId=parseTimelineShots(it).firstOrNull()?.id.orEmpty()}.onFailure{status=it.message?:"error"}; busy=false } }
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={scope.launch{busy=true;FaceApiClient.filmTimeline(jobId).onSuccess{timelineJson=it; selectedTimelineShotId=parseTimelineShots(it).firstOrNull()?.id.orEmpty(); status=tx(language,"تم تحميل Timeline المرئي","Visual timeline loaded","Görsel timeline yüklendi","Timeline visual cargado","Visuelle Timeline geladen","Timeline visiva caricata")}.onFailure{status=it.message?:"error"};busy=false}},modifier=Modifier.weight(1f)){ Icon(Icons.Default.ViewTimeline,null);Spacer(Modifier.width(5.dp));Text(tx(language,"Timeline","Timeline","Timeline","Timeline","Timeline","Timeline")) }
                OutlinedButton(onClick={scope.launch{busy=true;FaceApiClient.filmSnapshot(jobId).onSuccess{status=it}.onFailure{status=it.message?:"error"};busy=false}},modifier=Modifier.weight(1f)){ Icon(Icons.Default.Save,null);Spacer(Modifier.width(5.dp));Text(tx(language,"حفظ نقطة","Snapshot","Anlık görüntü","Snapshot","Snapshot","Snapshot")) }
            }
        }
        OutlinedButton(onClick={scope.launch{busy=true;FaceApiClient.filmRenderQueue().onSuccess{status=it}.onFailure{status=it.message?:"error"};busy=false}},modifier=Modifier.fillMaxWidth()){
            Icon(Icons.Default.QueuePlayNext,null);Spacer(Modifier.width(7.dp));Text(tx(language,"قائمة التوليد","Render Queue","Render Kuyruğu","Cola de render","Render-Warteschlange","Coda render"))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick={scope.launch{busy=true;FaceApiClient.filmStatus().onSuccess{status=it}.onFailure{status=it.message?:"error"};busy=false}},modifier=Modifier.fillMaxWidth()){
            Icon(Icons.Default.Memory,null);Spacer(Modifier.width(7.dp));Text(tx(language,"فحص محركات الفيديو المحلية","Check local video engines","Yerel video motorlarını kontrol et","Comprobar motores locales","Lokale Video-Engines prüfen","Controlla motori locali"))
        }
        if(status.isNotBlank()){Spacer(Modifier.height(10.dp));StatusMessage(status)}
        if(plan.isNotBlank()){Spacer(Modifier.height(10.dp));SectionCard(tx(language,"مخطط الفيلم","Film plan","Film planı","Plan","Filmplan","Piano")){Text(plan,fontSize=10.sp,color=Color(0xFFBFD0E2))}}
        Spacer(Modifier.height(18.dp))
        Text(tx(language,"Stage 34000 FIXED يضيف Preview Monitor مع SMPTE Timecode، Frame Step، Safe Areas، Onion Preview وحفظ Workspace، مع استمرار Waveform وJ/K/L وCurve Editor.","Stage 32500 adds Advanced Timeline Studio with waveform, time ruler, snap, multi-select, camera/motion keyframes, proxy preview, track controls and ripple trim.","Stage 27500 profesyonel timeline, playhead, zoom, In/Out, hassas kırpma ve autosave ekler.","Stage 27500 añade timeline profesional, playhead, zoom, In/Out, recorte preciso y autosave.","Stage 27500 ergänzt eine Pro-Timeline mit Playhead, Zoom, In/Out, präzisem Trim und Autosave.","Stage 27500 aggiunge timeline professionale, playhead, zoom, In/Out, taglio preciso e autosave."),fontSize=10.sp,color=Color(0xFF7D91A8),textAlign=TextAlign.Center)
        Spacer(Modifier.height(24.dp))
    }
}



// Secondary screens moved to SecondaryScreens.kt in Stage 37000.


