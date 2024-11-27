package com.travelsketch.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.travelsketch.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun FindID(
    loginViewModel: LoginViewModel
) {
    var phoneNumberState by remember { mutableStateOf(PhoneNumberState()) }
    val isPhoneVerified by loginViewModel.isPhoneVerified.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        PhoneNumberInput(
            phoneNumber = phoneNumberState,
            onPhoneNumberChange = { phoneNumberState = it },
            onSendVerificationCode = { phoneNumber ->
                loginViewModel.sendVerificationCode(phoneNumber)
            },
            onVerifyCode = { code ->
                loginViewModel.verifyCode(code)
            },
            loginViewModel = loginViewModel
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    loginViewModel._isLoading.value = true
                    try {
                        val email = loginViewModel.findEmailByPhoneNumber(phoneNumberState.fullNumber())
                        if (email != null) {
                            loginViewModel.showSnackbar("Your email is: $email")
                        } else {
                            loginViewModel.showSnackbar("No account found with this phone number")
                        }
                    } catch (e: Exception) {
                        loginViewModel.showSnackbar("Error finding email: ${e.message}")
                    } finally {
                        loginViewModel._isLoading.value = false
                    }
                }
            },
            enabled = isPhoneVerified,
            modifier = Modifier.fillMaxWidth(0.3f)
        ) {
            Text("Find ID")
        }
    }
}