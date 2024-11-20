package com.travelsketch.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLayout(
    title: String,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            content()
        }
    }
}

