package com.travelsketch.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.travelsketch.R
import com.travelsketch.ui.composable.FindID
import com.travelsketch.ui.composable.Login
import com.travelsketch.ui.composable.NewPasswordInput
import com.travelsketch.ui.composable.ResetPassword
import com.travelsketch.ui.composable.SignUp
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
            loginViewModel.setCurrentScreen("SelectViewType")
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
                    loginViewModel.setCurrentScreen("SelectViewType")
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
                    "NewPasswordInput" -> "NewPasswordInput"
                    "SelectViewType" -> "SelectViewType"
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
                        "FindID" -> FindID(
                            onFindIDClick = {
                                /* TODO: FindID 연결 */
                            }
                        )
                        "ResetPassword" -> ResetPassword(
                            onResetPasswordClick = {
                                loginViewModel.setCurrentScreen("NewPasswordInput")
                            }
                        )
                        "NewPasswordInput" -> NewPasswordInput()
                        "SelectViewType" -> SelectViewType()
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
fun SelectViewType() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Select your canvas view type",
            style = TextStyle(
                fontSize = 32.sp
            )
        )

        IconButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.LightGray, shape = RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "Map View",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        IconButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Gray, shape = RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "List View",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
