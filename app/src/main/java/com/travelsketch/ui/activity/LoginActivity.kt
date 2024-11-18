package com.travelsketch.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
                "SignIn" -> "Sign Up"
                "RegistrationSuccess" -> "Registration Successful"
                else -> "Login"
            }) {
                when (currentScreen) {
                    "Login" -> Login(
                        onSignInClick = { loginViewModel.setCurrentScreen("SignIn") },
                        onLoginClick = { email, password ->
                            loginViewModel.loginUser(email, password)
                        }
                    )
                    "SignIn" -> SignIn(
                        onRegisterClick = { email, password, phoneNumber ->
                            loginViewModel.registerUser(email, password, phoneNumber)
                        }
                    )
                    "RegistrationSuccess" -> RegistrationSuccessScreen(
                        onLoginClick = { loginViewModel.setCurrentScreen("Login") }
                    )
                }
            }
        }
    }
}

@Composable
fun Login(
    onSignInClick: () -> Unit,
    onLoginClick: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
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

        Button(
            onClick = { onLoginClick(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { onSignInClick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
    }
}

@Composable
fun SignIn(
    onRegisterClick: (email: String, password: String, phoneNumber: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showEmailError by remember { mutableStateOf(false) }
    val isPasswordMatching = password == confirmPassword

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
            modifier = Modifier.fillMaxWidth()
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

        PasswordInput(
            password = password,
            onPasswordChange = { password = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it },
            isPasswordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
            isConfirmPasswordVisible = confirmPasswordVisible,
            onConfirmPasswordVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible },
            isPasswordMatching = isPasswordMatching
        )

        Spacer(modifier = Modifier.height(8.dp))

        PhoneNumberInput(
            onPhoneNumberComplete = { phoneNumber = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                    showEmailError = true
                } else if (isPasswordMatching && phoneNumber.isNotEmpty()) {
                    onRegisterClick(email, password, phoneNumber)
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
    isPasswordMatching: Boolean
) {
    PasswordField(
        label = "Password",
        password = password,
        onPasswordChange = onPasswordChange,
        isPasswordVisible = isPasswordVisible,
        onVisibilityChange = onPasswordVisibilityChange
    )

    Spacer(modifier = Modifier.height(4.dp))

    PasswordField(
        label = "Confirm Password",
        password = confirmPassword,
        onPasswordChange = onConfirmPasswordChange,
        isPasswordVisible = isConfirmPasswordVisible,
        onVisibilityChange = onConfirmPasswordVisibilityChange
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        PasswordCheck(isPasswordMatching = isPasswordMatching, confirmPassword = confirmPassword)
    }
}

@Composable
fun PasswordField(
    label: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onVisibilityChange: () -> Unit
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
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
fun PasswordCheck(isPasswordMatching: Boolean, confirmPassword: String) {
    if (!isPasswordMatching && confirmPassword.isNotEmpty()) {
        Text(
            text = "Passwords do not match.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PhoneNumberInput(
    onPhoneNumberComplete: (String) -> Unit
) {
    var part1 by remember { mutableStateOf("") }
    var part2 by remember { mutableStateOf("") }
    var part3 by remember { mutableStateOf("") }

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = part1,
            onValueChange = {
                if (it.length <= 3) {
                    part1 = it
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
                    part2 = it
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
                    part3 = it
                    if (it.length == 4 && part1.length == 3 && part2.length == 4) {
                        onPhoneNumberComplete("$part1$part2$part3")
                    }
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
