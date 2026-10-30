package com.yuivo9999.mybox123.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuivo9999.mybox123.ui.theme.AppTheme

@Composable
fun CustomYearInputDialog(
    initialYear: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var yearText by remember { mutableStateOf(initialYear?.takeIf { it.all { c -> c.isDigit() } } ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val handleConfirm = {
        val trimmed = yearText.trim()
        val yearNum = trimmed.toIntOrNull()
        if (yearNum == null || yearNum < 1900 || yearNum > 2100) {
            errorText = "请输入1900至2100之间的有效4位公历年份（例如：1999）"
        } else {
            errorText = null
            onConfirm(trimmed)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.card,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AppTheme.colors.accent,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "精准自定义年份",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "输入特定历史年份（如 1999、1994、1982 等），精准在服务端数据库与源站历史长河中检索该年度影视作品。",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            yearText = input
                            errorText = null
                        }
                    },
                    label = { Text("年份 (例如 1999)") },
                    placeholder = { Text("1999") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleConfirm() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.accent,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary,
                        focusedLabelColor = AppTheme.colors.accent,
                        unfocusedLabelColor = AppTheme.colors.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = handleConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.accent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("确定并应用", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("取消", color = AppTheme.colors.textSecondary)
            }
        }
    )
}
