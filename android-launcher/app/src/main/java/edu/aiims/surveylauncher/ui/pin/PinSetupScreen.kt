package edu.aiims.surveylauncher.ui.pin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import edu.aiims.surveylauncher.util.PinManager

@Composable
fun PinSetupScreen(onPinSet: () -> Unit) {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1 = enter, 2 = confirm

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
                    text = if (step == 1) "Set Your PIN" else "Confirm Your PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = if (step == 1) pin else confirmPin,
                    onValueChange = { 
                        if (it.length <= 4 && it.all(Char::isDigit)) {
                            if (step == 1) pin = it else confirmPin = it
                            errorMessage = ""
                        }
                    },
                    label = { Text("Enter 4-digit PIN") },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    ),
                    visualTransformation = VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Use digits only", style = MaterialTheme.typography.bodySmall) }
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        when (step) {
                            1 -> {
                                if (pin.length != 4) {
                                    errorMessage = "PIN must be exactly 4 digits"
                                } else {
                                    step = 2
                                }
                            }
                            2 -> {
                                if (pin.length != 4 || confirmPin.length != 4) {
                                    errorMessage = "PIN must be exactly 4 digits"
                                } else if (pin == confirmPin) {
                                    pinManager.setPin(pin)
                                    onPinSet()
                                } else {
                                    errorMessage = "PINs do not match"
                                    confirmPin = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = if (step == 1) pin.length >= 4 else confirmPin.length >= 4
                ) {
                    Text(if (step == 1) "Next" else "Confirm")
                }

                if (step == 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            step = 1
                            pin = ""
                            confirmPin = ""
                            errorMessage = ""
                        }
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}
