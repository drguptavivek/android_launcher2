package edu.aiims.surveylauncher.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.clickable
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

    fun advanceIfReady(value: String) {
        input = value.take(4)
        if (input.length == 4) {
            when (stage) {
                PinStage.CURRENT -> {
                    if (!pinManager.isPinSet() || pinManager.validatePin(input)) {
                        stage = PinStage.NEW
                        input = ""
                        error = ""
                    } else {
                        error = "Incorrect current PIN"
                        input = ""
                    }
                }
                PinStage.NEW -> {
                    newPin = input
                    stage = PinStage.CONFIRM
                    input = ""
                    error = ""
                }
                PinStage.CONFIRM -> {
                    if (input == newPin) {
                        pinManager.setPin(input)
                        error = ""
                        onComplete()
                    } else {
                        error = "PINs do not match"
                        input = ""
                    }
                }
            }
        } else {
            error = ""
        }
    }

    fun onDigit(d: String) {
        if (input.length < 4) {
            advanceIfReady(input + d)
        }
    }

    fun onBackspace() {
        if (input.isNotEmpty()) {
            input = input.dropLast(1)
            error = ""
        }
    }

    val title = when (stage) {
        PinStage.CURRENT -> "Enter Current PIN"
        PinStage.NEW -> "New PIN"
        PinStage.CONFIRM -> "Confirm PIN"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        PinDots(pinLength = input.length)

        Spacer(modifier = Modifier.height(24.dp))

        BasicTextField(
            value = input,
            onValueChange = { value ->
                // Keep synced if hardware keyboard used
                val digitsOnly = value.filter { it.isDigit() }
                input = digitsOnly.take(4)
            },
            modifier = Modifier.size(0.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent)
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

        NumberPad(
            onDigit = { onDigit(it) },
            onBackspace = { onBackspace() },
            onSubmit = { advanceIfReady(input) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    onCancel()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    // Retry the same stage with current input
                    advanceIfReady(input)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Confirm")
            }
        }
    }
}

@Composable
private fun PinDots(pinLength: Int, total: Int = 4) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val filled = index < pinLength
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("←", "0", "✓")
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { label ->
                    val modifier = Modifier
                        .size(72.dp)
                    Surface(
                        modifier = modifier,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.clickable {
                                when (label) {
                                    "←" -> onBackspace()
                                    "✓" -> onSubmit()
                                    else -> onDigit(label)
                                }
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
