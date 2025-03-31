package com.thoughtworks.bleconn.app.ui.views.bleperf

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thoughtworks.bleconn.app.R
import com.thoughtworks.bleconn.app.di.Dependency
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlePerfScreen(dependency: Dependency) {
    val factory = remember { BlePerfViewModelFactory(dependency) }
    val viewModel: BlePerfViewModel = viewModel(factory = factory)
    val state = viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val event = viewModel.uiEvent
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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
                    IconButton(onClick = { viewModel.sendAction(BlePerfAction.NavigateBack) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back_24_black),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                CardView(state.value)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.value.log.toList()) { logMessage ->
                        val color =
                            if (logMessage.contains("[ERROR]")) Color.Red else Color.Unspecified
                        Text(
                            text = logMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = color
                        )
                    }
                }
            }
        }
    )

    LaunchedEffect(state.value.log.size) {
        if (state.value.log.isNotEmpty()) {
            val lastIndex = state.value.log.size - 1
            val fifthLastIndex = (lastIndex - 5).coerceAtLeast(0)
            val fifthLastItemVisible =
                listState.layoutInfo.visibleItemsInfo.any { it.index == fifthLastIndex }
            if (fifthLastItemVisible) {
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sendAction(BlePerfAction.StartScan)
        event.onEach { event ->
            when (event) {
                is BlePerfEvent.ShowToast -> {
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

@Composable
fun CardView(state: BlePerfState) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(primaryColor)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(primaryColor)
        ) {
            StatRow(
                label = "Scan",
                successCount = state.scanSuccessCount,
                failCount = state.scanFailCount
            )
            StatRow(
                label = "Connect",
                successCount = state.connectSuccessCount,
                failCount = state.connectFailCount
            )
            StatRow(
                label = "Discover",
                successCount = state.discoverSuccessCount,
                failCount = state.discoverFailCount
            )
            StatRow(
                label = "Request MTU",
                successCount = state.requestMtuSuccessCount,
                failCount = state.requestMtuFailCount
            )
            StatRow(
                label = "Read",
                successCount = state.readSuccessCount,
                failCount = state.readFailCount
            )
            StatRow(
                label = "Write",
                successCount = state.writeSuccessCount,
                failCount = state.writeFailCount
            )
        }
    }
}

@Composable
fun StatRow(label: String, successCount: Int, failCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Success: ",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Text(
            "$successCount",
            color = Color.Green,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Text(
            " Fail: ",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Text(
            "$failCount",
            color = Color.Red,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

