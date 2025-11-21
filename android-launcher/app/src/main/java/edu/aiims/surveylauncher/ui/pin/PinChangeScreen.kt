package edu.aiims.surveylauncher.ui.pin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.aiims.surveylauncher.util.PinManager

private enum class PinStage { CURRENT, NEW, CONFIRM }

@Composable
fun PinChangeScreen(
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val pinManager = remember(context) { PinManager(context) }
    var stage by remember { mutableStateOf(PinStage.CURRENT) }
    var input by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val title = when (stage) {
        PinStage.CURRENT -> "Enter Current PIN"
        PinStage.NEW -> "New PIN"
        PinStage.CONFIRM -> "Confirm PIN"
    }

    fun handleContinue() {
        when (stage) {
            PinStage.CURRENT -> {
                if (input.length != 4) {
                    error = "Enter your current 4-digit PIN"
                } else if (!pinManager.isPinSet() && input.isEmpty()) {
                    stage = PinStage.NEW
                    input = ""
                    error = ""
                } else if (pinManager.validatePin(input)) {
                    stage = PinStage.NEW
                    input = ""
                    error = ""
                } else {
                    error = "Incorrect current PIN"
                    input = ""
                }
            }
            PinStage.NEW -> {
                if (input.length != 4) {
                    error = "PIN must be exactly 4 digits"
                } else {
                    newPin = input
                    stage = PinStage.CONFIRM
                    input = ""
                    error = ""
                }
            }
            PinStage.CONFIRM -> {
                if (input.length != 4) {
                    error = "PIN must be exactly 4 digits"
                } else if (input == newPin) {
                    pinManager.setPin(input)
                    error = ""
                    onComplete()
                } else {
                    error = "PINs do not match"
                    input = ""
                }
            }
        }
    }

    fun onInputChange(value: String) {
        val digits = value.filter(Char::isDigit).take(4)
        input = digits
        if (error.isNotEmpty()) error = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = { onInputChange(it) },
                    label = {
                        Text(
                            when (stage) {
                                PinStage.CURRENT -> "Current PIN"
                                PinStage.NEW -> "New PIN"
                                PinStage.CONFIRM -> "Confirm PIN"
                            }
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    ),
                    visualTransformation = VisualTransformation.None,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Enter 4 digits", style = MaterialTheme.typography.bodySmall)
                    }
                )

                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { onCancel() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { handleContinue() },
                        modifier = Modifier.weight(1f),
                        enabled = input.isNotEmpty()
                    ) {
                        Text(
                            text = when (stage) {
                                PinStage.CURRENT -> "Next"
                                PinStage.NEW -> "Next"
                                PinStage.CONFIRM -> "Confirm"
                            }
                        )
                    }
                }
            }
        }
    }
}
