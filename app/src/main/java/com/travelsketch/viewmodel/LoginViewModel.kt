package com.travelsketch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val _currentScreen = MutableStateFlow("Login")
    val currentScreen: StateFlow<String> = _currentScreen

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun setCurrentScreen(screen: String) {
        _currentScreen.value = screen
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

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                _eventFlow.emit("Login successful!")
            } catch (e: Exception) {
                _eventFlow.emit("Login failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}
