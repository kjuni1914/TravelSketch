package com.travelsketch.ui.activity

import SelectViewType
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
