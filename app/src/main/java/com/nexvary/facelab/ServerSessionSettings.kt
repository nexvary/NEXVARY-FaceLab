package com.nexvary.facelab

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun ServerSessionSettings(language: AppLanguage) {
    val ar = language == AppLanguage.Arabic
    var url by remember { mutableStateOf(ServerSession.baseUrl) }
    var token by remember { mutableStateOf(ServerSession.token) }
    var allowed by remember { mutableStateOf(ServerSession.uploadAllowed) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (ar) "خادم المعالجة — هذه الجلسة فقط" else "Processing server — this session only")
            OutlinedTextField(value = url, onValueChange = {
                url = it; ServerSession.baseUrl = it.trim(); allowed = false; ServerSession.uploadAllowed = false
            }, label = { Text(if (ar) "رابط الخادم" else "Server URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = token, onValueChange = { token = it; ServerSession.token = it },
                label = { Text(if (ar) "رمز الخادم" else "Server token") }, visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(if (ar) "السماح يرسل الصور والملفات المختارة إلى الخادم أعلاه عند تشغيل أدوات AI. لا يُحفظ الرمز أو الإذن بعد إغلاق العملية."
                 else "Allowing uploads sends selected images and files to the server above when AI tools run. The token and permission are kept in memory only.")
            Row {
                Switch(checked = allowed, onCheckedChange = { allowed = it; ServerSession.uploadAllowed = it })
                Text(if (ar) "أوافق على رفع الملفات لهذه الجلسة" else "Allow uploads for this session")
            }
        }
    }
}
