package com.travelsketch.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.travelsketch.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun SignUp(
    onRegisterClick: (email: String, password: String, phoneNumber: String) -> Unit,
    loginViewModel: LoginViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneNumberState by remember { mutableStateOf(PhoneNumberState()) }
    var showEmailError by remember { mutableStateOf(false) }
    var showConfirmPasswordError by remember { mutableStateOf(false) }

    val isPhoneVerified by loginViewModel.isPhoneVerified.collectAsState()
    val isPasswordMatching = password == confirmPassword && confirmPassword.isNotEmpty()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (showEmailError) showEmailError = false
            },
            label = { Text("Email") },
            isError = showEmailError,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            if (showEmailError) {
                Text(
                    text = "Please enter a valid email.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PasswordInput(
            password = password,
            onPasswordChange = { password = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = {
                confirmPassword = it
                if (showConfirmPasswordError) showConfirmPasswordError = false
            },
            isPasswordMatching = isPasswordMatching,
            showConfirmPasswordError = showConfirmPasswordError,
            passwordFocusRequester = passwordFocusRequester,
            confirmPasswordFocusRequester = confirmPasswordFocusRequester
        )

        Spacer(modifier = Modifier.height(8.dp))

        PhoneNumberInput(
            phoneNumber = phoneNumberState,
            onPhoneNumberChange = { phoneNumberState = it },
            onSendVerificationCode = { phoneNumber ->
                scope.launch {
                    try {
                        loginViewModel._isLoading.value = true
                        val exists = loginViewModel.checkPhoneNumberExists(phoneNumber)
                        if (exists) {
                            loginViewModel.showSnackbar("This phone number is already registered")
                            return@launch
                        }
                        loginViewModel.sendVerificationCode(phoneNumber)
                    } catch (e: Exception) {
                        loginViewModel.stopVerificationTimer()
                        loginViewModel.showSnackbar("Error checking phone number: ${e.message}")
                    } finally {
                        loginViewModel._isLoading.value = false
                    }
                }
            },
            onVerifyCode = { code ->
                loginViewModel.verifyCode(code)
            },
            loginViewModel = loginViewModel
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val fullPhoneNumber = phoneNumberState.fullNumber()
                when {
                    !email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) -> {
                        showEmailError = true
                        emailFocusRequester.requestFocus()
                    }
                    password.isEmpty() -> {
                        passwordFocusRequester.requestFocus()
                    }
                    confirmPassword.isEmpty() || !isPasswordMatching -> {
                        showConfirmPasswordError = true
                        confirmPasswordFocusRequester.requestFocus()
                    }
                    fullPhoneNumber.isEmpty() -> {
                    }
                    !isPhoneVerified -> {
                        loginViewModel.showSnackbar("Please verify your phone number first")
                    }
                    else -> {
                        onRegisterClick(email, password, fullPhoneNumber)
                    }
                }
            },
            enabled = isPhoneVerified,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }

        val showCelebration by loginViewModel.showCelebration.collectAsState()

        if (showCelebration) {
            CelebrationDialog(
                onDismiss = {
                    loginViewModel._showCelebration.value = false
                    loginViewModel.setCurrentScreen("SelectViewType")
                }
            )
        }
        Button(
            onClick = {
                loginViewModel._showCelebration.value = true
            }

        ) {
            Text("로그인이펙트")
        }
    }

}