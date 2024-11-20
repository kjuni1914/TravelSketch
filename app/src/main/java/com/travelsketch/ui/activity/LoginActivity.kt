package com.travelsketch.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.travelsketch.ui.composable.*
import com.travelsketch.ui.layout.*
import com.travelsketch.viewmodel.*

class LoginActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentScreen by loginViewModel.currentScreen.collectAsState()
            val eventFlow = loginViewModel.eventFlow
            val snackbarHostState = remember { SnackbarHostState() }

            BackHandler(enabled = currentScreen != "Login") {
                loginViewModel.setCurrentScreen("Login")
            }

            LaunchedEffect(eventFlow) {
                eventFlow.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            UserLayout(
                title = when (currentScreen) {
                    "Login" -> "Login"
                    "SignUp" -> "SignUp"
                    "RegistrationSuccess" -> "Registration Successful"
                    "FindID" -> "FindID"
                    "ResetPassword" -> "ResetPassword"
                    else -> "Login"
                },
                snackbarHostState = snackbarHostState
            ) {
                when (currentScreen) {
                    "Login" -> Login(
                        onSignUpClick = { loginViewModel.setCurrentScreen("SignUp") },
                        onLoginClick = { email, password ->
                            loginViewModel.loginUser(email, password)
                            // TODO: 유저가 캔버스뷰타입을 선택했으면 바로 캔버스뷰로 쏴주고 아니면 뷰타입 선택화면 쏴주기
                        },
                        onFindIDClick = { loginViewModel.setCurrentScreen("FindID") },
                        onResetPasswordClick = { loginViewModel.setCurrentScreen("ResetPassword") }
                    )
                    "SignUp" -> SignUp(
                        onRegisterClick = { email, password, phoneNumber ->
                            loginViewModel.registerUser(email, password, phoneNumber)
                        }
                    )
                    "RegistrationSuccess" -> RegistrationSuccessScreen(
                        onLoginClick = { loginViewModel.setCurrentScreen("Login") }
                    )
                    "FindID" -> FindID(
                        onFindIDClick = {
                            /* TODO: loginViewModel에서 연결 */
                        }
                    )
                    "ResetPassword" -> ResetPassword(
                        onResetPasswordClick = {
                            /* TODO: loginViewModel에서 연결 */
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Login(
    onSignUpClick: () -> Unit,
    onLoginClick: (email: String, password: String) -> Unit,
    onFindIDClick: () -> Unit,
    onResetPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val emailFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp),
                    color = Color.Gray
                )
                Text(
                    text = "Find ID",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            onFindIDClick()
                            // TODO: 아이디 찾기로 연결
                        },
                    color = Color.Blue
                )
                Divider(
                    modifier = Modifier
//                        .weight(1f)
                        .width(24.dp)
                        .height(1.dp),
                    color = Color.Gray
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp),
                    color = Color.Gray
                )
                Text(
                    text = "Reset Password",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            onResetPasswordClick()
                        },
                    color = Color.Blue
                )
                Divider(
                    modifier = Modifier
//                        .weight(1f)
                        .width(24.dp)
                        .height(1.dp),
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isEmpty()) {
                    emailFocusRequester.requestFocus()
                } else {
                    onLoginClick(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { onSignUpClick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
    }
}

@Composable
fun SignUp(
    onRegisterClick: (email: String, password: String, phoneNumber: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneNumberState by remember { mutableStateOf(PhoneNumberState()) }
    var showEmailError by remember { mutableStateOf(false) }
    var showConfirmPasswordError by remember { mutableStateOf(false) }

    val isPasswordMatching = password == confirmPassword && confirmPassword.isNotEmpty()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (showEmailError) showEmailError = false
            },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password
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

        // Phone Number
        PhoneNumberInput(
            phoneNumber = phoneNumberState,
            onPhoneNumberChange = { phoneNumberState = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sign Up Button
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
                    else -> {
                        onRegisterClick(email, password, fullPhoneNumber)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
    }
}


@Composable
fun PasswordInput(
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordMatching: Boolean,
    showConfirmPasswordError: Boolean,
    passwordFocusRequester: FocusRequester,
    confirmPasswordFocusRequester: FocusRequester
) {
    PasswordField(
        label = "Password",
        password = password,
        onPasswordChange = onPasswordChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(passwordFocusRequester)
    )

    Spacer(modifier = Modifier.height(4.dp))

    PasswordField(
        label = "Confirm Password",
        password = confirmPassword,
        onPasswordChange = onConfirmPasswordChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(confirmPasswordFocusRequester)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        if (showConfirmPasswordError && !isPasswordMatching) {
            Text(
                text = "Passwords do not match.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RegistrationSuccessScreen(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Registration successful! Please log in.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Login Screen")
        }
    }
}

@Composable
fun FindID(
    onFindIDClick: () -> Unit
) {
    var phoneNumberState by remember { mutableStateOf(PhoneNumberState()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        PhoneNumberInput(
            phoneNumber = phoneNumberState,
            onPhoneNumberChange = { phoneNumberState = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val fullPhoneNumber = phoneNumberState.fullNumber()
                if (fullPhoneNumber.isNotEmpty()) {
                    onFindIDClick()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Find ID")
        }
    }
}

@Composable
fun ResetPassword(
    onResetPasswordClick: () -> Unit
) {
    Text("ResetPasswordUI")
    // TODO: 비밀번호 재설정 구현
}