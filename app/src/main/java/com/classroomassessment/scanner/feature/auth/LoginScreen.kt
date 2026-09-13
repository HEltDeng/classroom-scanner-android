package com.classroomassessment.scanner.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classroomassessment.scanner.ui.InlineError
import com.classroomassessment.scanner.ui.ModeChip

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    isMock: Boolean,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf(if (isMock) "teacher@example.com" else "") }
    var password by rememberSaveable { mutableStateOf(if (isMock) "demo-password" else "") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                ) {
                    Icon(
                        Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("课堂扫描器", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("可靠扫描，课堂不中断", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(28.dp))
            ModeChip(isMock)
            Spacer(Modifier.height(24.dp))
            Text("老师 / 助教登录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("账号") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            if (errorMessage != null) {
                Spacer(Modifier.height(14.dp))
                InlineError(errorMessage, onClearError)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onLogin(username, password) },
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("进入课堂", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            if (isMock) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { onLogin("teacher@example.com", "demo-password") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("使用演示账号")
                }
            }
        }
    }
}
