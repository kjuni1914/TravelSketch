package com.travelsketch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val _currentScreen = MutableStateFlow("Login")
    val currentScreen = _currentScreen.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val firebaseAuth = FirebaseAuth.getInstance()

    fun currentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    fun setCurrentScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun loginUser(email: String, password: String) {
        _isLoading.value = true
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    setCurrentScreen("Next")
                    showSnackbar("Login successful!")
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

                val user = mapOf(
                    "phone_number" to phoneNumber,
//                    "password" to password,
                    "view_type" to false, // false -> 리스트 뷰, true -> 맵 뷰?
                    "canvas_ids" to "",
                    "friend_ids" to ""
                )
                firebaseDatabase.reference.child("users").child(userId).setValue(user).await()

                _currentScreen.value = "RegistrationSuccess"
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
                if (task.isSuccessful) {
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId != null) {
                        val userRef = firebaseDatabase.getReference("users").child(userId)
                        userRef.get().addOnSuccessListener { dataSnapshot ->
                            if (!dataSnapshot.exists()) {
                                val userData = mapOf(
                                    "phone_number" to "",
                                    "view_type" to false,
                                    "canvas_ids" to "",
                                    "friend_ids" to ""
                                )
                                userRef.setValue(userData)
                                    .addOnCompleteListener { dbTask ->
                                        _isLoading.value = false
                                        if (dbTask.isSuccessful) {
                                            setCurrentScreen("Next")
                                            showSnackbar("Google sign-in successful!")
                                        } else {
                                            showSnackbar("Database update failed: ${dbTask.exception?.message}")
                                        }
                                    }
                            } else {
                                _isLoading.value = false
                                setCurrentScreen("Next")
                                showSnackbar("Google sign-in successful!")
                            }
                        }.addOnFailureListener { dbTask ->
                            _isLoading.value = false
                            showSnackbar("Database read failed: ${dbTask.message}")
                        }
                    } else {
                        _isLoading.value = false
                        showSnackbar("User ID is null.")
                    }
                } else {
                    _isLoading.value = false
                    showSnackbar("Google sign-in failed: ${task.exception?.message}")
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
}
