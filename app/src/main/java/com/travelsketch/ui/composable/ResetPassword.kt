package com.travelsketch.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.travelsketch.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun ResetPassword(
    onResetPasswordClick: suspend (String) -> Unit,
    loginViewModel: LoginViewModel
) {
    var emailState by remember { mutableStateOf("") }
    var phoneNumberState by remember { mutableStateOf(PhoneNumberState()) }
    val isPhoneVerified by loginViewModel.isPhoneVerified.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = emailState,
            onValueChange = { emailState = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        PhoneNumberInput(
            phoneNumber = phoneNumberState,
            onPhoneNumberChange = { phoneNumberState = it },
            onSendVerificationCode = { phoneNumber ->
                scope.launch {
                    try {
                        if (emailState.isEmpty()) {
                            loginViewModel.showSnackbar("Please enter your email first")
                            return@launch
                        }

                        val storedNumber = loginViewModel.findPhoneNumberByEmail(emailState)
                        if (storedNumber == null) {
                            loginViewModel.showSnackbar("No account found with this email")
                            return@launch
                        }

                        if (phoneNumber == storedNumber) {
                            loginViewModel.sendVerificationCode(phoneNumber)
                        } else {
                            loginViewModel.showSnackbar("Phone number doesn't match the registered email")
                        }
                    } catch (e: Exception) {
                        loginViewModel.showSnackbar("Error: ${e.message}")
                    }
                }
            },
            onVerifyCode = { code -> loginViewModel.verifyCode(code) },
            loginViewModel = loginViewModel
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    loginViewModel._isLoading.value = true
                    try {
                        onResetPasswordClick(emailState)
                    } finally {
                        loginViewModel._isLoading.value = false
                    }
                }
            },
            enabled = isPhoneVerified,
            modifier = Modifier.fillMaxWidth(0.3f)
        ) {
            Text("Reset Password")
        }
    }
}