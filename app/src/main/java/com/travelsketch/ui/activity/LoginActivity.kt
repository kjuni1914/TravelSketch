package com.travelsketch.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.travelsketch.R
import com.travelsketch.ui.composable.PasswordField
import com.travelsketch.ui.composable.PhoneNumberInput
import com.travelsketch.ui.composable.PhoneNumberState
import com.travelsketch.ui.layout.UserLayout
import com.travelsketch.viewmodel.LoginViewModel

class LoginActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                loginViewModel.firebaseAuthWithGoogle(idToken)
            } else {
                loginViewModel.showSnackbar("Google sign-in failed.")
            }
        } catch (e: ApiException) {
            loginViewModel.showSnackbar("Google sign-in failed: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOption)
        loginViewModel.userReload()

        if (loginViewModel.currentUser() != null) {
            loginViewModel.setCurrentScreen("Next")
        } else {
            loginViewModel.setCurrentScreen("Login")
        }

        setContent {
            val currentScreen by loginViewModel.currentScreen.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val isLoading by loginViewModel.isLoading.collectAsState()

            BackHandler {
                if (loginViewModel.currentUser() != null) {
                    //로그인 시 메인화면
                    loginViewModel.setCurrentScreen("Next")
                } else {
                    //로그인 전 메인화면
                    loginViewModel.setCurrentScreen("Login")
                }
            }

            LaunchedEffect(key1 = true) {
                loginViewModel.eventFlow.collect { message ->
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
                    "Next" -> "Next"
                    else -> "Login"
                },
                snackbarHostState = snackbarHostState
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        "Login" -> Login(
                            onSignUpClick = { loginViewModel.setCurrentScreen("SignUp") },
                            onLoginClick = { email, password ->
                                loginViewModel.loginUser(email, password)
                            },
                            onFindIDClick = { loginViewModel.setCurrentScreen("FindID") },
                            onResetPasswordClick = { loginViewModel.setCurrentScreen("ResetPassword") },
                            onGoogleLoginClick = {
                                val signInIntent = googleSignInClient.signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            }
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
                                /* TODO: FindID 연결 */
                            }
                        )
                        "ResetPassword" -> ResetPassword(
                            onResetPasswordClick = {
                                /* TODO: ResetPassword 연결 */
                            }
                        )
                        "Next" -> Next()
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                        )
                    }
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
    onResetPasswordClick: () -> Unit,
    onGoogleLoginClick: () -> Unit
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
            // Find ID
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
                            // TODO: Find ID 연결
                        },
                    color = Color.Blue
                )
                Divider(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp),
                    color = Color.Gray
                )
            }

            // Reset Password
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

        Image(
            painter = painterResource(id = R.drawable.continue_with_google),
            contentDescription = "Continue with Google",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGoogleLoginClick() }
        )

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
        // Email Input
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

        // Password Input
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

        // Phone Number Input
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

@Composable
fun Next() {
    // TODO: Implement "Next" screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("WOW! Welcome to the Next Screen.")
    }
}
