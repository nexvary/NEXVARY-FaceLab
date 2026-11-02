package com.nexvary.facelab

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ComparePreviewCard(
    original: Bitmap,
    result: Bitmap,
    split: Float,
    language: AppLanguage,
    busy: Boolean,
    faceDetected: Boolean?,
    onSplitChange: (Float) -> Unit,
    onPick: () -> Unit
) {
    var previewWidthPx by remember { mutableIntStateOf(1) }
    val safeSplit = split.coerceIn(0.05f, 0.95f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF061426),
        border = BorderStroke(2.dp, Color(0xFFD9E7F2))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { measured -> previewWidthPx = measured.width.coerceAtLeast(1) }
                .pointerInput(previewWidthPx) {
                    detectHorizontalDragGestures { change, _ ->
                        val fraction = change.position.x / previewWidthPx.toFloat()
                        onSplitChange(fraction.coerceIn(0.05f, 0.95f))
                    }
                }
        ) {
            Image(
                bitmap = original.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Keep the compare overlay deliberately simple and compiler-safe:
            // no constraint-scope receiver values, Dp arithmetic, or overloaded offset calls.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(safeSplit)
                    .align(Alignment.CenterStart)
                    .clipToBounds()
            ) {
                Image(
                    bitmap = result.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(Color(0xFF00E5FF))
                        .align(Alignment.CenterEnd)
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xDD061426)
            ) {
                Text(
                    tx(language, "قبل", "BEFORE", "ÖNCE", "ANTES", "VORHER", "PRIMA"),
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 9.sp
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xDD061426)
            ) {
                Text(
                    tx(language, "بعد • اسحب للمقارنة", "AFTER • drag to compare", "SONRA • karşılaştırmak için sürükle", "DESPUÉS • arrastra para comparar", "NACHHER • zum Vergleichen ziehen", "DOPO • trascina per confrontare"),
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 9.sp
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xE0061426)
            ) {
                Row(
                    modifier = Modifier.clickable(onClick = onPick).padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(tx(language, "تغيير", "Change", "Değiştir", "Cambiar", "Ändern", "Cambia"), fontSize = 11.sp)
                }
            }

            faceDetected?.let { detected ->
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    shape = RoundedCornerShape(50),
                    color = if (detected) Color(0xC0195C43) else Color(0xC06B2B2B)
                ) {
                    Text(
                        if (detected) tx(language, "الوجه جاهز", "Face ready", "Yüz hazır", "Rostro listo", "Gesicht bereit", "Viso pronto")
                        else tx(language, "لا يوجد وجه", "No face", "Yüz yok", "Sin rostro", "Kein Gesicht", "Nessun viso"),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 10.sp
                    )
                }
            }

            if (busy) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0x77000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

