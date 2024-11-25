package com.travelsketch.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FindID(
    onFindIDClick: () -> Unit
) {
    var phoneNumberState by remember { mutableStateOf(PhoneNumberState()) }
    var verificationNumber by remember { mutableStateOf("") }

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
            onClick = {},
            modifier = Modifier.fillMaxWidth(0.3f)
        ) {
            Text("Send Code")
        }

        OutlinedTextField(
            value = verificationNumber,
            onValueChange = { newValue ->
                verificationNumber = newValue
            },
            label = { Text("Verification Code") },
            placeholder = { Text("Enter your code") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val fullPhoneNumber = phoneNumberState.fullNumber()
                if (fullPhoneNumber.isNotEmpty()) {
                    onFindIDClick()
                }
            },
            modifier = Modifier.fillMaxWidth(0.3f)
        ) {
            Text("Find ID")
        }
    }
}