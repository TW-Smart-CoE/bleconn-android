package com.thoughtworks.bleconn.app.ui.views.bleclient

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thoughtworks.bleconn.app.R
import com.thoughtworks.bleconn.app.di.Dependency
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleClientScreen(
    dependency: Dependency,
    address: String,
) {
    val factory = remember { BleClientViewModelFactory(dependency, address) }
    val viewModel: BleClientViewModel = viewModel(factory = factory)
    val state = viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val event = viewModel.uiEvent
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.sendAction(BleClientAction.NavigateBack) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back_24_black),
                            contentDescription = "Back"
                        )
                    }
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
                    Text(
                        modifier = Modifier
                            .padding(top = 16.dp),
                        text = context.getString(R.string.ble_client),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        modifier = Modifier
                            .padding(top = 16.dp),
                        text = "isConnected: ${state.value.isConnected}",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Button(
                        onClick = {
                            viewModel.sendAction(
                                BleClientAction.WriteWiFiConfig(
                                    "ssid",
                                    "password"
                                )
                            )
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                    ) {
                        Text(context.getString(R.string.write_wifi_config))
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        event.onEach { event ->
            when (event) {
                is BleClientEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        event.text,
                        if (event.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.launchIn(scope)
    }
}