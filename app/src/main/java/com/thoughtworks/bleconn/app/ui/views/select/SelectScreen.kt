package com.thoughtworks.bleconn.app.ui.views.select

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thoughtworks.bleconn.app.R
import com.thoughtworks.bleconn.app.di.Dependency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectScreen(
    dependency: Dependency,
) {
    val factory = remember { SelectViewModelFactory(dependency) }
    val viewModel: SelectViewModel = viewModel(factory = factory)
    val state = viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.sendAction(SelectAction.ClickBleServer)
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(context.getString(R.string.ble_server))
                    }

                    Button(
                        onClick = {
                            viewModel.sendAction(SelectAction.ClickBleScanner)
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(context.getString(R.string.ble_scanner))
                    }
                }
            }
        }
    )
}