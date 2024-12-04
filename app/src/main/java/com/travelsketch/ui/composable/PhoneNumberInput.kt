package com.travelsketch.ui.composable
// 준희 추가
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelsketch.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberInput(
    phoneNumber: PhoneNumberState,
    onPhoneNumberChange: (PhoneNumberState) -> Unit,
    onSendVerificationCode: (String) -> Unit,
    onVerifyCode: (String) -> Unit,
    loginViewModel: LoginViewModel
) {
    val focusRequester1: FocusRequester = remember { FocusRequester() }
    val focusRequester2: FocusRequester = remember { FocusRequester() }
    val focusRequester3: FocusRequester = remember { FocusRequester() }
    val focusRequester4: FocusRequester = remember { FocusRequester() }
    var countryExpanded by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    val isTimerRunning by loginViewModel.isTimerRunning.collectAsState()
    var remainingSeconds by remember { mutableStateOf(120) }
    val isPhoneVerified by loginViewModel.isPhoneVerified.collectAsState()

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            remainingSeconds = 120
            try {
                focusRequester4.requestFocus()
            } catch (e: Exception) {}
        }

        while (isTimerRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            if (remainingSeconds == 0) {
                loginViewModel.stopVerificationTimer()
            }
        }
    }

    val countryOptions = listOf(
        "+82" to "South Korea",
        "+1" to "United States",
        "+81" to "Japan",
        "+86" to "China"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = countryExpanded,
                onExpandedChange = { countryExpanded = !countryExpanded },
                modifier = Modifier.weight(3f)
            ) {
                OutlinedTextField(
                    value = phoneNumber.countryCode,
                    onValueChange = { },
                    label = { Text("Country") },
                    readOnly = true,
                    modifier = Modifier.menuAnchor(),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )
                ExposedDropdownMenu(
                    expanded = countryExpanded,
                    onDismissRequest = { countryExpanded = false }
                ) {
                    countryOptions.forEach { (code, country) ->
                        DropdownMenuItem(
                            text = { Text("$code ($country)") },
                            onClick = {
                                onPhoneNumberChange(phoneNumber.copy(countryCode = code))
                                countryExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = phoneNumber.part1,
                onValueChange = { newValue ->
                    if (newValue.length <= 3 && newValue.all { it.isDigit() }) {
                        onPhoneNumberChange(phoneNumber.copy(part1 = newValue))
                        if (newValue.length == 3) {
                            focusRequester2.requestFocus()
                        }
                    }
                },
                label = { Text("") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .weight(3f)
                    .focusRequester(focusRequester1),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            )

            OutlinedTextField(
                value = phoneNumber.part2,
                onValueChange = { newValue ->
                    if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                        onPhoneNumberChange(phoneNumber.copy(part2 = newValue))
                        if (newValue.length == 4) {
                            focusRequester3.requestFocus()
                        }
                    }
                },
                label = { Text("") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .weight(4f)
                    .focusRequester(focusRequester2)
                    .onKeyEvent { event ->
                        if (event.key == Key.Backspace &&
                            phoneNumber.part2.isEmpty() &&
                            event.type == KeyEventType.KeyUp) {
                            focusRequester1.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            )

            OutlinedTextField(
                value = phoneNumber.part3,
                onValueChange = { newValue ->
                    if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                        onPhoneNumberChange(phoneNumber.copy(part3 = newValue))
                    }
                },
                label = { Text("") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .weight(4f)
                    .focusRequester(focusRequester3)
                    .onKeyEvent { event ->
                        if (event.key == Key.Backspace &&
                            phoneNumber.part3.isEmpty() &&
                            event.type == KeyEventType.KeyUp) {
                            focusRequester2.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            )
        }


        Button(
            onClick = { onSendVerificationCode(phoneNumber.fullNumber()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneNumber.isValid() && !isPhoneVerified
        ) {
            Text(if (isTimerRunning) "Resend Code" else "Send Verification Code")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { if (it.length <= 6) verificationCode = it },
                label = { Text("Verification Code") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester4),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                enabled = isTimerRunning && remainingSeconds > 0 && !isPhoneVerified
            )

            if (isTimerRunning && !isPhoneVerified) {
                Text(
                    text = "${remainingSeconds / 60}:${String.format("%02d", remainingSeconds % 60)}",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = { onVerifyCode(verificationCode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = verificationCode.length == 6 && isTimerRunning && remainingSeconds > 0 && !isPhoneVerified
        ) {
            if (isPhoneVerified) {
                Text("Verify Complete!")
            } else {
                Text("Verify Code")
            }
        }
    }
}

data class PhoneNumberState(
    val countryCode: String = "+82",
    val part1: String = "",
    val part2: String = "",
    val part3: String = ""
) {
    fun fullNumber(): String = if (isValid()) {
        "$countryCode$part1$part2$part3"
    } else {
        ""
    }

    fun isValid(): Boolean =
        countryCode.isNotEmpty() &&
                part1.length == 3 &&
                part2.length == 4 &&
                part3.length == 4
}