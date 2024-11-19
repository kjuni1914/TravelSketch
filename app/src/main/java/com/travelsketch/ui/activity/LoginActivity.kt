package com.travelsketch.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.travelsketch.ui.layout.UserLayout
import com.travelsketch.viewmodel.LoginViewModel

class LoginActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentScreen by loginViewModel.currentScreen.collectAsState()
            val context = LocalContext.current
            val eventFlow = loginViewModel.eventFlow

            LaunchedEffect(eventFlow) {
                eventFlow.collect { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

            UserLayout(title = when (currentScreen) {
                "Login" -> "Login"
                "SignUp" -> "SignUp"
                "RegistrationSuccess" -> "Registration Successful"
                "FindID" -> "FindID"
                "ResetPassword" -> "ResetPassword"
                else -> "Login"
            }) {
                when (currentScreen) {
                    "Login" -> Login(
                        onSignUpClick = { loginViewModel.setCurrentScreen("SignUp") },
                        onLoginClick = { email, password ->
                            loginViewModel.loginUser(email, password)
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
                        /* TODO: loginViewModel에서 연결 */  }
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
                            // TODO: 비밀번호 재설정으로 연결
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
    var phoneNumber by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showEmailError by remember { mutableStateOf(false) }
    var showConfirmPasswordError by remember { mutableStateOf(false) }

    val isPasswordMatching = password == confirmPassword && confirmPassword.isNotEmpty()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester1 = remember { FocusRequester() }
    val phoneFocusRequester2 = remember { FocusRequester() }
    val phoneFocusRequester3 = remember { FocusRequester() }

    var part1 by remember { mutableStateOf("") }
    var part2 by remember { mutableStateOf("") }
    var part3 by remember { mutableStateOf("") }

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            if (showEmailError) {
                Text(
                    text = "Invalid email format.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Password
        PasswordInput(
            password = password,
            onPasswordChange = {
                password = it
            },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = {
                confirmPassword = it
                if (showConfirmPasswordError) {
                    showConfirmPasswordError = false
                }
            },
            isPasswordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
            isConfirmPasswordVisible = confirmPasswordVisible,
            onConfirmPasswordVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible },
            isPasswordMatching = isPasswordMatching,
            showConfirmPasswordError = showConfirmPasswordError,
            passwordFocusRequester = passwordFocusRequester,
            confirmPasswordFocusRequester = confirmPasswordFocusRequester
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Phone
        PhoneNumberInput(
            part1 = part1,
            part2 = part2,
            part3 = part3,
            onPart1Change = { part1 = it },
            onPart2Change = { part2 = it },
            onPart3Change = { part3 = it },
            focusRequester1 = phoneFocusRequester1,
            focusRequester2 = phoneFocusRequester2,
            focusRequester3 = phoneFocusRequester3
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sign Up Button
        Button(
            onClick = {
                phoneNumber = if (part1.isNotEmpty() && part2.isNotEmpty() && part3.isNotEmpty()) {
                    "$part1$part2$part3"
                } else {
                    ""
                }

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
                    phoneNumber.isEmpty() -> {
                        phoneFocusRequester1.requestFocus()
                    }
                    else -> {
                        onRegisterClick(email, password, phoneNumber)
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
    isPasswordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    isConfirmPasswordVisible: Boolean,
    onConfirmPasswordVisibilityChange: () -> Unit,
    isPasswordMatching: Boolean,
    showConfirmPasswordError: Boolean,
    passwordFocusRequester: FocusRequester,
    confirmPasswordFocusRequester: FocusRequester
) {
    PasswordField(
        label = "Password",
        password = password,
        onPasswordChange = onPasswordChange,
        isPasswordVisible = isPasswordVisible,
        onVisibilityChange = onPasswordVisibilityChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(passwordFocusRequester)
    )

    Spacer(modifier = Modifier.height(4.dp))

    PasswordField(
        label = "Confirm Password",
        password = confirmPassword,
        onPasswordChange = onConfirmPasswordChange,
        isPasswordVisible = isConfirmPasswordVisible,
        onVisibilityChange = onConfirmPasswordVisibilityChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(confirmPasswordFocusRequester)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        PasswordCheck(
            isPasswordMatching = isPasswordMatching,
            showConfirmPasswordError = showConfirmPasswordError
        )
    }
}

@Composable
fun PasswordField(
    label: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onVisibilityChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(label) },
        modifier = modifier,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password"
                )
            }
        }
    )
}

@Composable
fun PasswordCheck(isPasswordMatching: Boolean, showConfirmPasswordError: Boolean) {
    if (showConfirmPasswordError && !isPasswordMatching) {
        Text(
            text = "Passwords do not match.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PhoneNumberInput(
    part1: String,
    part2: String,
    part3: String,
    onPart1Change: (String) -> Unit,
    onPart2Change: (String) -> Unit,
    onPart3Change: (String) -> Unit,
    focusRequester1: FocusRequester,
    focusRequester2: FocusRequester,
    focusRequester3: FocusRequester
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = part1,
            onValueChange = {
                if (it.length <= 3) {
                    onPart1Change(it)
                    if (it.length == 3) focusRequester2.requestFocus()
                }
            },
            label = { Text("Phone") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester1)
        )

        OutlinedTextField(
            value = part2,
            onValueChange = {
                if (it.length <= 4) {
                    onPart2Change(it)
                    if (it.length == 4) focusRequester3.requestFocus()
                }
            },
            label = { Text("") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester2)
        )

        OutlinedTextField(
            value = part3,
            onValueChange = {
                if (it.length <= 4) {
                    onPart3Change(it)
                }
            },
            label = { Text("") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester3)
        )
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
    Text("findIDUI")
    // TODO: 아이디 찾기 구현
}

@Composable
fun ResetPassword(
    onResetPasswordClick: () -> Unit
) {
    Text("ResetPasswordUI")
    // TODO: 비밀번호 재설정 구현
}