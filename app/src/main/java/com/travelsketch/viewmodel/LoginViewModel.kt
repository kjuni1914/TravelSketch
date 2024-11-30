package com.travelsketch.viewmodel

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.travelsketch.data.local.AppDatabase
import com.travelsketch.data.local.ViewTypeEntity
import com.travelsketch.data.model.User
import com.travelsketch.data.model.ViewType
import com.travelsketch.ui.activity.ListViewActivity
import com.travelsketch.ui.activity.MapViewActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class LoginViewModel : ViewModel() {
    private val _currentScreen = MutableStateFlow("Login")
    val currentScreen = _currentScreen.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow = _eventFlow.asSharedFlow()

    internal val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val firebaseAuth = FirebaseAuth.getInstance()

    private var verificationId: String? = null

    fun currentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    fun setCurrentScreen(screen: String) {
        _isPhoneVerified.value = false
        _isTimerRunning.value = false
        stopVerificationTimer()

        _isLoading.value = false
        verificationId = null
        _currentScreen.value = screen
    }

    private var activity: ComponentActivity? = null
    fun setActivity(activity: ComponentActivity) {
        this.activity = activity
    }

    private val _isPhoneVerified = MutableStateFlow(false)
    val isPhoneVerified = _isPhoneVerified.asStateFlow()

    fun loginUser(email: String, password: String) {
        _isLoading.value = true
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    showSnackbar("Login successful!")
                    // TODO: 적절한 액티비티 쏴주기
//                    checkSavedViewType() // 설정된 뷰타입 참조해서 적절한 화면 쏴주기
                } else {
                    showSnackbar("Login failed: ${task.exception?.message}")
                }
            }
    }

    fun registerUser(email: String, password: String, phoneNumber: String) {
        viewModelScope.launch {
            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("Failed to retrieve user ID.")

                val newUser = User(
                    email = email,
                    phoneNumber = phoneNumber,
                    canvasIds = "",
                    friendIds = ""
                )

                firebaseDatabase.reference
                    .child("users")
                    .child(userId)
                    .setValue(newUser)
                    .await()

                _eventFlow.emit("Registration successful!")
            } catch (e: Exception) {
                _eventFlow.emit("Registration failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        _isLoading.value = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _isLoading.value = false
                    showSnackbar("Google sign-in failed: ${task.exception?.message}")
                    return@addOnCompleteListener
                }

                val userId = firebaseAuth.currentUser?.uid
                if (userId == null) {
                    _isLoading.value = false
                    showSnackbar("User ID is null.")
                    return@addOnCompleteListener
                }

                val userRef = firebaseDatabase.getReference("users").child(userId)
                userRef.get()
                    .addOnSuccessListener { dataSnapshot ->
                        if (!dataSnapshot.exists()) {
                            val newUser = User(
                                email = firebaseAuth.currentUser?.email ?: "",
                                phoneNumber = "",
                                canvasIds = "",
                                friendIds = ""
                            )
                            userRef.setValue(newUser)
                                .addOnCompleteListener { dbTask ->
                                    _isLoading.value = false
                                    if (dbTask.isSuccessful) {
                                        showSnackbar("Google sign-in successful!")
                                        checkSavedViewType()
                                    } else {
                                        showSnackbar("Database update failed: ${dbTask.exception?.message}")
                                    }
                                }
                        } else {
                            _isLoading.value = false
                            showSnackbar("Google sign-in successful!")
                            checkSavedViewType()
                        }
                    }
                    .addOnFailureListener { dbTask ->
                        _isLoading.value = false
                        showSnackbar("Database read failed: ${dbTask.message}")
                    }
            }
    }


    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(message)
        }
    }

    fun userReload() {
        firebaseAuth.signOut()
        firebaseAuth.currentUser?.reload()
    }

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    fun startVerificationTimer() {
        _isTimerRunning.value = true
    }

    fun stopVerificationTimer() {
        _isTimerRunning.value = false
    }

    fun sendVerificationCode(phoneNumber: String) {
        _isLoading.value = true

        if (activity == null) {
            viewModelScope.launch {
                _isLoading.value = false
                _eventFlow.emit("Error: Activity reference not found")
            }
            return
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(120L, TimeUnit.SECONDS)
            .setActivity(activity!!)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    _isLoading.value = false
                    viewModelScope.launch {
                        _eventFlow.emit("Verification completed automatically")
                        _isPhoneVerified.value = true
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _isLoading.value = false
                    stopVerificationTimer()
                    viewModelScope.launch {
                        _eventFlow.emit("Verification failed: ${e.message}")
                        _isPhoneVerified.value = false
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@LoginViewModel.verificationId = verificationId
                    _isLoading.value = false
                    startVerificationTimer()
                    viewModelScope.launch {
                        _eventFlow.emit("Verification code sent")
                    }
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(code: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)

                firebaseAuth.signInWithCredential(credential).await()
                _eventFlow.emit("Phone number verified successfully")
                _isPhoneVerified.value = true
                firebaseAuth.currentUser?.delete()?.await()

            } catch (e: Exception) {
                _eventFlow.emit("Verification failed: ${e.message}")
                _isPhoneVerified.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun checkPhoneNumberExists(phoneNumber: String): Boolean {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("users")
                .orderByChild("phone_number")
                .equalTo(phoneNumber)
                .get()
                .await()

            snapshot.exists()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun findEmailByPhoneNumber(phoneNumber: String): String? {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("users")
                .orderByChild("phone_number")
                .equalTo(phoneNumber)
                .get()
                .await()

            if (snapshot.exists()) {
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    return user?.email
                }
            }
            null
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            _eventFlow.emit("Password reset link sent to your email")
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun findPhoneNumberByEmail(email: String): String? {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("users")
                .get()
                .await()

            if (snapshot.exists()) {
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user?.email == email) {
                        return user.phoneNumber
                    }
                }
            }
            null
        } catch (e: Exception) {
            throw e
        }
    }

    private var database: AppDatabase? = null

    fun setDatabase(db: AppDatabase) {
        database = db
    }

    fun saveViewType(viewType: ViewType) {
        Log.d("Room", "saveViewType called in ViewModel")
        viewModelScope.launch {
            try {
                val userId = currentUser()?.uid ?: throw Exception("User not found")
                Log.d("Room", "Before database call")
                database?.viewTypeDao()?.setViewType(ViewTypeEntity(userId, viewType))
                Log.d("Room", "After database call")

                activity?.let { currentActivity ->
                    when (viewType) {
                        ViewType.MAP -> {
                            val intent = Intent(currentActivity, MapViewActivity::class.java)
                            currentActivity.startActivity(intent)
                            currentActivity.finish()
                        }
                        ViewType.LIST -> {
                            val intent = Intent(currentActivity, ListViewActivity::class.java)
                            currentActivity.startActivity(intent)
                            currentActivity.finish()
                        }
                        ViewType.NOT_SET -> setCurrentScreen("SelectViewType")
                    }
                }
                Log.d("Room", "viewType set $viewType")
            } catch (e: Exception) {
                Log.d("Room", "Failed to save view type")
                showSnackbar("Failed to save view type: ${e.message}")
            }
        }
    }

    fun checkSavedViewType() {
        viewModelScope.launch {
            try {
                val userId = currentUser()?.uid
                if (userId == null) {
                    setCurrentScreen("SelectViewType")
                    return@launch
                }
                val savedViewType = database?.viewTypeDao()?.getViewType(userId)
                activity?.let { currentActivity ->
                    when (savedViewType?.viewType) {
                        ViewType.MAP -> {
                            val intent = Intent(currentActivity, MapViewActivity::class.java)
                            currentActivity.startActivity(intent)
                            currentActivity.finish()
                        }
                        ViewType.LIST -> {
                            val intent = Intent(currentActivity, ListViewActivity::class.java)
                            currentActivity.startActivity(intent)
                            currentActivity.finish()
                        }
                        ViewType.NOT_SET, null -> setCurrentScreen("SelectViewType")
                    }
                }
            } catch (e: Exception) {
                showSnackbar("Failed to load view type: ${e.message}")
            }
        }
    }
}
