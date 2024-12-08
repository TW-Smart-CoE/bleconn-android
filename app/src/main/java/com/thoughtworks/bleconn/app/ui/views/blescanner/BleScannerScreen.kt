package com.thoughtworks.bleconn.app.ui.views.blescanner

import android.bluetooth.le.ScanResult
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thoughtworks.bleconn.app.R
import com.thoughtworks.bleconn.app.di.Dependency
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScannerScreen(dependency: Dependency) {
    val factory = remember { BleScannerViewModelFactory(dependency) }
    val viewModel: BleScannerViewModel = viewModel(factory = factory)
    val state = viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
                    IconButton(onClick = { viewModel.sendAction(BleScannerAction.NavigateBack) }) {
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
                        text = context.getString(R.string.ble_scanner),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Button(
                        onClick = {
                            viewModel.sendAction(BleScannerAction.StartScan)
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        enabled = !state.value.isScanStarted
                    ) {
                        Text(context.getString(R.string.start_scan))
                    }

                    Button(
                        onClick = {
                            viewModel.sendAction(BleScannerAction.StopScan)
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        enabled = state.value.isScanStarted
                    ) {
                        Text(context.getString(R.string.stop_scan))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.value.scanResults) { scanResult ->
                            ScanResultItem(scanResult, viewModel::sendAction)
                        }
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        event.onEach { event ->
            when (event) {
                is BleScannerEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        event.text,
                        if (event.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.launchIn(scope)
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.sendAction(BleScannerAction.StopScan)
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun ScanResultItem(
    scanResult: ScanResult,
    dispatch: (BleScannerAction) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        onClick = {
            dispatch(BleScannerAction.ConnectToDevice(scanResult.device.address))
        }
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                text = "Address: ${scanResult.device.address}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "RSSI: ${scanResult.rssi}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}