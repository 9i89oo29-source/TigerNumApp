package com.tigernum.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tigernum.app.R
import com.tigernum.app.ui.navigation.Screen   // 👈 إضافة هذا الاستيراد
import com.tigernum.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) {
            navController.navigate(Screen.Register.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("${stringResource(R.string.profile_name)}: ${uiState.name}")
                Text("${stringResource(R.string.profile_email)}: ${uiState.email}")
                Text("${stringResource(R.string.profile_phone)}: ${uiState.phone}")
            }
        }
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(R.string.logout_button))
        }
    }
}
