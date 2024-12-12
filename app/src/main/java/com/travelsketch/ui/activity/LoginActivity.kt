package com.travelsketch.ui.activity

import SelectViewType
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.travelsketch.R
import com.travelsketch.data.local.AppDatabase
import com.travelsketch.data.repository.FirebaseClient
import com.travelsketch.ui.composable.FindID
import com.travelsketch.ui.composable.Login
import com.travelsketch.ui.composable.ResetPassword
import com.travelsketch.ui.composable.SignUp
import com.travelsketch.ui.layout.UserLayout
import com.travelsketch.viewmodel.LoginViewModel

class LoginActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { (permission, isGranted) ->
                when (permission) {
                    Manifest.permission.CAMERA -> {
                        if (!isGranted) {
                            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Manifest.permission.POST_NOTIFICATIONS -> {
                        if (!isGranted) {
                            Toast.makeText(this, "Notification permission is required", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

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

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } else {
            // Android 13 미만에서는 카메라 권한만 체크
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginViewModel.setActivity(this)

        checkAndRequestPermissions()

        val googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOption)

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()

        loginViewModel.setDatabase(database)
        FirebaseClient.initViewTypeDao(database.viewTypeDao())

        if (loginViewModel.currentUser() != null) {
            loginViewModel.checkSavedViewType()
        } else {
            loginViewModel.setCurrentScreen("Login")
        }

        setContent {
            val currentScreen by loginViewModel.currentScreen.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val isLoading by loginViewModel.isLoading.collectAsState()

            BackHandler {
                if (loginViewModel.currentUser() != null) {
                    loginViewModel.setCurrentScreen("SelectViewType")
                } else {
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
                            },
                            loginViewModel = loginViewModel
                        )
                        "FindID" -> FindID(
                            loginViewModel = loginViewModel
                        )
                        "ResetPassword" -> ResetPassword(
                            onResetPasswordClick = { email ->
                                loginViewModel.sendPasswordResetEmail(email)
                            },
                            loginViewModel = loginViewModel
                        )
                        "SelectViewType" -> SelectViewType(loginViewModel)
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