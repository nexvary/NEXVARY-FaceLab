package com.nexvary.facelab

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun GalleryScreen(items: List<Bitmap>, language: AppLanguage, modifier:Modifier=Modifier, onOpen:(Bitmap)->Unit, onClear:()->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp)) {
        ScreenHeader(tx(language,"معرض FaceLab","FaceLab Gallery","FaceLab Galerisi","Galería FaceLab","FaceLab-Galerie","Galleria FaceLab"), tx(language,"نتائجك المحفوظة تبقى متاحة بعد إغلاق التطبيق","Saved results remain available after closing the app","Kaydedilen sonuçlar uygulama kapandıktan sonra da kalır","Los resultados guardados siguen disponibles tras cerrar la app","Gespeicherte Ergebnisse bleiben nach dem Schließen verfügbar","I risultati salvati restano disponibili dopo la chiusura"), Icons.Default.PhotoLibrary)
        if (items.isEmpty()) {
            Surface(Modifier.fillMaxWidth().padding(top=20.dp),shape=RoundedCornerShape(24.dp),color=Color(0xFF06182D),border=BorderStroke(1.dp,Color(0xFF1479FF))) {
                Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Default.Collections,null,Modifier.size(44.dp),tint=Color(0xFF657A91));Spacer(Modifier.height(10.dp));Text(tx(language,"لا توجد نتائج بعد","No results yet","Henüz sonuç yok","Aún no hay resultados","Noch keine Ergebnisse","Nessun risultato"),fontWeight=FontWeight.Bold);Text(tx(language,"نفّذ تعديل AI وسيظهر هنا تلقائيًا","Run an AI edit and it will appear here automatically","Bir AI düzenlemesi çalıştırın; burada görünecek","Ejecuta una edición IA y aparecerá aquí","Führen Sie eine KI-Bearbeitung aus; sie erscheint hier","Esegui una modifica IA e apparirà qui"),color=Color(0xFF8395A8),fontSize=12.sp,textAlign=TextAlign.Center) }
            }
        } else {
            items.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) { row.forEach { bmp -> Surface(Modifier.weight(1f).aspectRatio(1f).clickable{onOpen(bmp)},shape=RoundedCornerShape(18.dp),color=Color(0xFF06182D),border=BorderStroke(2.dp,Color(0xFFD9E7F2))) { Image(bmp.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop) } }; if(row.size==1) Spacer(Modifier.weight(1f)) }; Spacer(Modifier.height(10.dp)) }
            OutlinedButton(onClick=onClear, modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.DeleteSweep,null); Spacer(Modifier.width(7.dp)); Text(tx(language,"مسح المعرض المحلي","Clear local gallery","Yerel galeriyi temizle","Borrar galería local","Lokale Galerie leeren","Svuota galleria locale")) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable internal fun ModelsScreen(state:ServerState, details:String, modelsDetails:String, language:AppLanguage, modifier:Modifier=Modifier, onRefresh:()->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp)) {
        ScreenHeader(tx(language,"إدارة محركات AI","AI Engine Manager","AI Motor Yöneticisi","Gestor de motores IA","KI-Engine-Verwaltung","Gestione motori IA"),tx(language,"حالة الخادم والمحركات الذاتية الاستضافة","Status of your self-hosted engines","Self-hosted motorlarınızın durumu","Estado de motores autoalojados","Status Ihrer selbst gehosteten Engines","Stato dei motori self-hosted"),Icons.Default.Memory)
        val online=state==ServerState.Online
        Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=Color(0xFF06182D),border=BorderStroke(1.dp,if(online)Color(0xFF1E6B52)else Color(0xFF3A4656))) {
            Column(Modifier.padding(16.dp)) { Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(11.dp).background(if(online)Color(0xFF43FFB1)else Color(0xFFFFC857),CircleShape));Spacer(Modifier.width(8.dp));Text(if(online)tx(language,"الخادم متصل","Server online","Sunucu çevrimiçi","Servidor conectado","Server online","Server online")else tx(language,"الخادم غير مؤكد","Server not verified","Sunucu doğrulanmadı","Servidor no verificado","Server nicht verifiziert","Server non verificato"),fontWeight=FontWeight.Bold)}; if(details.isNotBlank()){Spacer(Modifier.height(8.dp));Text(details,color=Color(0xFF8CA0B5),fontSize=12.sp)};Spacer(Modifier.height(12.dp));Button(onClick=onRefresh,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(7.dp));Text(tx(language,"فحص المحركات","Check engines","Motorları kontrol et","Comprobar motores","Engines prüfen","Controlla motori"))} }
        }
        Spacer(Modifier.height(12.dp))
        listOf("Enhance / GFPGAN","Generative Inpainting","Hair reference / IP-Adapter","Virtual Wardrobe / VTON","Background tools").forEachIndexed { index,name ->
            Surface(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(16.dp),color=Color(0xFF041326),border=BorderStroke(2.dp,Color(0xFFD9E7F2))) { Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(index==0)Icons.Default.AutoFixHigh else Icons.Default.Extension,null,tint=Color(0xFF00E5FF));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(name,fontWeight=FontWeight.Bold);Text(tx(language,"موصل ذاتي الاستضافة — بدون رسوم API لكل صورة","Self-hosted connector — no per-image API fees","Self-hosted bağlayıcı — görüntü başına API ücreti yok","Conector autoalojado — sin tarifa API por imagen","Self-hosted Connector — keine API-Gebühr pro Bild","Connettore self-hosted — nessun costo API per immagine"),color=Color(0xFF8293A8),fontSize=10.sp)}} }
        }
        if (modelsDetails.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            SectionCard(tx(language,"تقرير النماذج من الخادم","Server model report","Sunucu model raporu","Informe de modelos del servidor","Server-Modellbericht","Rapporto modelli server")) {
                Text(modelsDetails.take(1400), color=Color(0xFF9FB4C8), fontSize=10.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable internal fun SettingsScreen(language:AppLanguage,state:ServerState,modifier:Modifier=Modifier,onLanguage:()->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp)) {
        ScreenHeader(tx(language,"الإعدادات","Settings","Ayarlar","Ajustes","Einstellungen","Impostazioni"),tx(language,"اللغة والخصوصية وسلوك التحرير","Language, privacy and editor behavior","Dil, gizlilik ve düzenleyici davranışı","Idioma, privacidad y comportamiento","Sprache, Datenschutz und Verhalten","Lingua, privacy e comportamento"),Icons.Default.Settings)
        SectionCard(title=tx(language,"اللغة","Language","Dil","Idioma","Sprache","Lingua")){OutlinedButton(onClick=onLanguage,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Language,null);Spacer(Modifier.width(7.dp));Text("${language.nativeName} (${language.short})")}}
        Spacer(Modifier.height(10.dp))
        ServerSessionSettings(language)
        Spacer(Modifier.height(10.dp))
        SectionCard(title=tx(language,"الخصوصية","Privacy","Gizlilik","Privacidad","Datenschutz","Privacy")){Text(tx(language,"الصور تبقى داخل التطبيق أو تُرسل فقط إلى خادمك الذاتي عند تنفيذ AI.","Images stay in the app or are sent only to your self-hosted server when AI runs.","Görüntüler uygulamada kalır veya yalnızca AI çalışırken kendi sunucunuza gönderilir.","Las imágenes permanecen en la app o se envían solo a tu servidor autoalojado al usar IA.","Bilder bleiben in der App oder werden bei KI nur an Ihren eigenen Server gesendet.","Le immagini restano nell'app o vengono inviate solo al tuo server self-hosted durante l'IA."),color=Color(0xFFB9C8D7),fontSize=12.sp)}
        Spacer(Modifier.height(10.dp))
        SectionCard(title=tx(language,"حالة الاتصال","Connection","Bağlantı","Conexión","Verbindung","Connessione")){Text(if(state==ServerState.Online)tx(language,"متصل","Online","Çevrimiçi","Conectado","Online","Online")else tx(language,"غير متصل/غير مفحوص","Offline / unchecked","Çevrimdışı / kontrol edilmedi","Desconectado / sin comprobar","Offline / ungeprüft","Offline / non verificato"),color=if(state==ServerState.Online)Color(0xFF43FFB1)else Color(0xFFFFC857),fontWeight=FontWeight.Bold)}
        Spacer(Modifier.height(24.dp))
    }
}

@Composable internal fun AboutScreen(language:AppLanguage,modifier:Modifier=Modifier) {
    val context = LocalContext.current
    fun openUrl(url:String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
    fun sendMail() { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@nexvary.com"))) } }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp)) {
        ScreenHeader("NEXVARY FaceLab",tx(language,"استوديو تحرير وجوه بالذكاء الاصطناعي","AI portrait editing studio","AI portre düzenleme stüdyosu","Estudio de edición de retratos con IA","KI-Porträtbearbeitungsstudio","Studio di modifica ritratti IA","Studio de retouche de portraits IA","AI پورٹریٹ ایڈیٹنگ اسٹوڈیو","استودیوی ویرایش پرتره با هوش مصنوعی"),Icons.Default.Info)
        SectionCard(tx(language,"عن التطبيق","About the app","Uygulama hakkında","Acerca de la app","Über die App","Informazioni sull'app","À propos de l'application","ایپ کے بارے میں","درباره برنامه")) {
            Text(tx(language,
                "محرر صور ووجوه احترافي يجمع أدوات التحرير المحلية مع محركات ذكاء اصطناعي ذاتية الاستضافة، مع الاهتمام بالخصوصية وتجربة الاستخدام.",
                "A professional portrait editor combining local editing tools with self-hosted AI engines, focused on privacy and usability.",
                "Yerel düzenleme araçlarını self-hosted AI motorlarıyla birleştiren profesyonel portre düzenleyici.",
                "Editor profesional de retratos que combina herramientas locales con motores de IA autoalojados.",
                "Professioneller Porträteditor mit lokalen Werkzeugen und selbst gehosteten KI-Engines.",
                "Editor professionale di ritratti con strumenti locali e motori IA self-hosted.",
                "Éditeur de portraits professionnel combinant des outils locaux et des moteurs IA auto-hébergés.",
                "ایک پروفیشنل پورٹریٹ ایڈیٹر جو مقامی ٹولز اور خود میزبان AI انجنز کو یکجا کرتا ہے۔",
                "ویرایشگر حرفه‌ای پرتره با ابزارهای محلی و موتورهای هوش مصنوعی خودمیزبان."
            ),modifier=Modifier.fillMaxWidth(),color=Color(0xFFB7C7D7),fontSize=13.sp,textAlign=TextAlign.Start)
            Spacer(Modifier.height(12.dp)); AssistChip(onClick={},label={Text("FaceLab • 39500.0.0 • Electric Edition")},leadingIcon={Icon(Icons.Default.Verified,null)})
        }
        Spacer(Modifier.height(12.dp))
        SectionCard(tx(language,"تواصل معنا","Contact & social","İletişim","Contacto","Kontakt","Contatti","Contact","رابطہ","ارتباط")) {
            val links = listOf(
                Triple(Icons.Default.Language,"Website","https://nexvary.com/"),
                Triple(Icons.Default.Facebook,"Facebook","https://www.facebook.com/share/14p9krEn5ij/"),
                Triple(Icons.Default.Email,"Email","mailto:info@nexvary.com"),
                Triple(Icons.Default.SmartDisplay,"YouTube","https://www.youtube.com/@NexvaryInc"),
                Triple(Icons.Default.AlternateEmail,"X","https://x.com/Nexvary")
            )
            links.forEach { (icon,label,url) ->
                OutlinedButton(onClick={ if(url.startsWith("mailto:")) sendMail() else openUrl(url) }, modifier=Modifier.fillMaxWidth().padding(vertical=3.dp), contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp)) {
                    Icon(icon,null,Modifier.size(20.dp)); Spacer(Modifier.width(9.dp)); Text(label,Modifier.weight(1f),textAlign=if(language.rtl)TextAlign.Right else TextAlign.Left); Icon(Icons.Default.OpenInNew,null,Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

