package com.nexvary.facelab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ElectricCyan = Color(0xFF00E5FF)
private val ElectricBlue = Color(0xFF1479FF)
private val ElectricGold = Color(0xFFFFC857)
private val ElectricSilver = Color(0xFFD9E7F2)
private val DeepNavy = Color(0xFF030A16)
private val PanelNavy = Color(0xFF071425)

private fun navLabel(item: AppScreen, language: AppLanguage): String = when(item) {
    AppScreen.Editor -> tx(language,"تحرير","Edit","Düzenle","Editar","Bearbeiten","Modifica","Modifier","ترمیم","ویرایش")
    AppScreen.Wardrobe -> tx(language,"الملابس","Wardrobe","Kıyafet","Ropa","Kleidung","Guardaroba","Tenues","لباس","لباس")
    AppScreen.Heritage -> tx(language,"المعالم","Landmarks","Miras","Lugares","Kulturerbe","Luoghi","Sites","مقامات","مکان‌ها")
    AppScreen.Film -> tx(language,"أفلام","Films","Film","Películas","Filme","Film","Films","فلم","فیلم")
    AppScreen.Gallery -> tx(language,"المعرض","Gallery","Galeri","Galería","Galerie","Galleria","Galerie","گیلری","گالری")
    AppScreen.Models -> tx(language,"النماذج","Models","Modeller","Modelos","Modelle","Modelli","Modèles","ماڈلز","مدل‌ها")
    AppScreen.Settings -> tx(language,"الإعدادات","Settings","Ayarlar","Ajustes","Einstellungen","Impostazioni","Réglages","ترتیبات","تنظیمات")
    AppScreen.About -> tx(language,"عنا","About","Hakkında","Acerca de","Über","Info","À propos","ہمارے بارے میں","درباره ما")
}

private fun navIconRes(item: AppScreen?): Int = when(item) {
    AppScreen.Editor -> R.drawable.nav_edit_electric
    AppScreen.Wardrobe -> R.drawable.nav_wardrobe_electric
    AppScreen.Heritage -> R.drawable.nav_landmarks_electric
    AppScreen.Film -> R.drawable.nav_films_electric
    AppScreen.Gallery -> R.drawable.nav_gallery_electric
    AppScreen.Models -> R.drawable.nav_models_electric
    AppScreen.Settings -> R.drawable.nav_settings_electric
    AppScreen.About -> R.drawable.nav_about_electric
    null -> R.drawable.nav_more_electric
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FaceLabBottomBar(selected: AppScreen, language: AppLanguage, onBack:()->Unit, onSelect:(AppScreen)->Unit) {
    var showMore by remember { mutableStateOf(false) }
    val primary = listOf(AppScreen.Editor, AppScreen.Wardrobe, AppScreen.Heritage, AppScreen.Film)
    val secondary = listOf(AppScreen.Gallery, AppScreen.Models, AppScreen.Settings, AppScreen.About)
    val secondarySelected = selected in secondary

    Surface(color=DeepNavy, tonalElevation=10.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            if (selected != AppScreen.Editor) {
                Row(Modifier.fillMaxWidth().padding(horizontal=10.dp, vertical=2.dp)) {
                    OutlinedButton(
                        onClick=onBack,
                        modifier=Modifier.heightIn(min=48.dp),
                        border=BorderStroke(1.dp, ElectricSilver),
                        colors=ButtonDefaults.outlinedButtonColors(contentColor=ElectricCyan)
                    ) {
                        Icon(if(language.rtl) Icons.Default.ArrowForward else Icons.Default.ArrowBack, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tx(language,"رجوع","Back","Geri","Atrás","Zurück","Indietro","Retour","واپس","بازگشت"))
                    }
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment=Alignment.Center) {
                Row(
                    Modifier.widthIn(max=640.dp).fillMaxWidth().padding(horizontal=8.dp, vertical=7.dp),
                    horizontalArrangement=Arrangement.spacedBy(6.dp)
                ) {
                    primary.forEach { item ->
                        ElectricNavCell(item, navLabel(item, language), selected == item, Modifier.weight(1f)) { onSelect(item) }
                    }
                    ElectricNavCell(null, tx(language,"المزيد","More","Daha","Más","Mehr","Altro","Plus","مزید","بیشتر"), secondarySelected, Modifier.weight(1f)) { showMore = true }
                }
            }
        }
    }

    if (showMore) {
        ModalBottomSheet(onDismissRequest={showMore=false}, containerColor=Color(0xFF040B18)) {
            Text(
                tx(language,"المزيد","More","Daha fazla","Más","Mehr","Altro","Plus","مزید","بیشتر"),
                Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=8.dp),
                fontWeight=FontWeight.Black, fontSize=20.sp, color=ElectricSilver, textAlign=TextAlign.Start
            )
            secondary.forEach { item ->
                val active = selected == item
                Surface(
                    modifier=Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=5.dp),
                    shape=RoundedCornerShape(20.dp),
                    color=if(active) Color(0xFF0A2847) else Color(0xFF071425),
                    border=BorderStroke(2.dp, if(active) ElectricCyan else ElectricSilver)
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min=60.dp).clickable { showMore=false; onSelect(item) }.padding(14.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Icon(painterResource(navIconRes(item)),null,Modifier.size(28.dp),tint=Color.Unspecified)
                        Spacer(Modifier.width(14.dp))
                        Text(navLabel(item,language),Modifier.weight(1f),fontSize=15.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Start)
                        if(active) Text("●", color=ElectricGold, fontSize=18.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ElectricNavCell(item: AppScreen?, label:String, active:Boolean, modifier:Modifier, onClick:()->Unit) {
    Surface(
        modifier=modifier.height(78.dp).shadow(if(active) 10.dp else 3.dp, RoundedCornerShape(20.dp)),
        shape=RoundedCornerShape(20.dp),
        color=if(active) ElectricCyan.copy(alpha=.28f) else ElectricSilver.copy(alpha=.16f),
        border=BorderStroke(if(active) 2.dp else 1.dp, if(active) ElectricSilver else ElectricSilver.copy(alpha=.85f))
    ) {
        Surface(
            modifier=Modifier.fillMaxWidth().height(74.dp).padding(2.dp).clickable(onClick=onClick),
            shape=RoundedCornerShape(18.dp),
            color=if(active) Color(0xFF0B2D49) else PanelNavy,
            border=BorderStroke(1.dp, if(active) ElectricGold else ElectricBlue.copy(alpha=.72f))
        ) {
            Column(
                Modifier.fillMaxWidth().height(70.dp).padding(horizontal=3.dp,vertical=7.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center
            ) {
                Icon(painterResource(navIconRes(item)),null,Modifier.size(30.dp),tint=Color.Unspecified)
                Spacer(Modifier.height(4.dp))
                Text(
                    label,maxLines=1,fontSize=9.sp,
                    color=if(active)Color.White else Color(0xFFD7E7F4),
                    fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,
                    overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center
                )
            }
        }
    }
}
