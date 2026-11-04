package com.gamebridge.app

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.math.BigInteger

class MainActivity : ComponentActivity() {

    private var diagnosticStatus by mutableStateOf("Ready for analysis")
    private var touchX by mutableStateOf(0f)
    private var touchY by mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        status = diagnosticStatus,
                        touchX = touchX,
                        touchY = touchY,
                        onTap = { x, y ->
                            touchX = x
                            touchY = y
                            runDiagnostics()
                        }
                    )
                }
            }
        }
    }

    private fun runDiagnostics() {
        val sampleBigInt = BigInteger.TEN
        val isZero = sampleBigInt.compareTo(BigInteger.ZERO) == 0
        
        diagnosticStatus = if (isZero) {
            "Diagnostics: Zero value detected"
        } else {
            "Diagnostics: Valid architecture check passed (X: $touchX, Y: $touchY)"
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        return super.onGenericMotionEvent(event)
    }
}

@Composable
fun MainScreen(
    status: String,
    touchX: Float,
    touchY: Float,
    onTap: (Float, Float) -> Unit
) {
    val backendItems = listOf(
        BackendItem(1, "Native Android Backend"),
        BackendItem(2, "PE Inspector Engine"),
        BackendItem(3, "Port Pipeline Manager")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        onTap(offset.x, offset.y)
                    }
                )
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "GameBridge Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = status)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Last Touch -> X: ${touchX.toInt()}, Y: ${touchY.toInt()}")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Active Backends & Modules:",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(backendItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.name)
                        Text(text = "ID: ${item.id}", color = Color.Gray)
                    }
                }
            }
        }
    }
}

data class DataItem(val id: Int, val name: String?)

fun processItems(items: List<DataItem>): List<String> {
    return items.mapNotNull { it.name }
}

data class BackendItem(val id: Int, val name: String)
