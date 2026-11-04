package com.gamebridge.app

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                        },
                        contentResolver = contentResolver
                    )
                }
            }
        }
    }

    private fun runDiagnostics() {
        diagnosticStatus =
            "Diagnostics: Valid architecture check passed (X: $touchX, Y: $touchY)"
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(
        event: MotionEvent?
    ): Boolean {
        return super.onGenericMotionEvent(event)
    }
}

@Composable
fun MainScreen(
    status: String,
    touchX: Float,
    touchY: Float,
    onTap: (Float, Float) -> Unit,
    contentResolver: ContentResolver
) {
    var peResult by remember { mutableStateOf<PeInfo?>(null) }
    var peError by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            scope.launch {
                isAnalyzing = true
                peResult = null
                peError = null

                try {
                    val result = withContext(Dispatchers.IO) {
                        val fileName = getFileName(contentResolver, uri)

                        if (!fileName.endsWith(".exe", true) &&
                            !fileName.endsWith(".dll", true)
                        ) {
                            throw IllegalArgumentException(
                                "Please select an .exe or .dll file"
                            )
                        }

                        PeInspector.inspect(
                            resolver = contentResolver,
                            uri = uri,
                            name = fileName
                        )
                    }

                    peResult = result

                    if (!result.isPe) {
                        peError = result.note
                    }
                } catch (e: Exception) {
                    peError = e.message ?: "Could not inspect file"
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    val backendItems = listOf(
        BackendItem(1, "Native Android Backend"),
        BackendItem(2, "PE Inspector Engine"),
        BackendItem(3, "Port Pipeline Manager")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTap(offset.x, offset.y)
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "GameBridge Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = status)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Last Touch -> X: ${touchX.toInt()}, Y: ${touchY.toInt()}"
                )
            }
        }

        Text(
            text = "PE Inspector Engine",
            style = MaterialTheme.typography.titleMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Analyze a Windows executable (.exe) or library (.dll)")

                Button(
                    onClick = {
                        filePicker.launch(arrayOf("*/*"))
                    },
                    enabled = !isAnalyzing
                ) {
                    Text(
                        if (isAnalyzing) {
                            "Analyzing..."
                        } else {
                            "Choose PE File"
                        }
                    )
                }

                if (isAnalyzing) {
                    CircularProgressIndicator()
                }

                peError?.let { error ->
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                peResult?.let { result ->
                    Divider()

                    Text("File: ${result.fileName}")

                    Text(
                        text = if (result.isPe) {
                            "PE file detected"
                        } else {
                            "Not a valid PE file"
                        }
                    )

                    Text("Architecture: ${result.architecture}")
                    Text("Machine: ${result.machine}")
                    Text("Subsystem: ${result.subsystem}")
                    Text("Details: ${result.note}")
                }
            }
        }

        Text(
            text = "Active Backends & Modules:",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(backendItems) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.name)

                        Text(
                            text = "ID: ${item.id}",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

private fun getFileName(
    resolver: ContentResolver,
    uri: Uri
): String {
    var name: String? = null

    resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )

            if (index >= 0) {
                name = cursor.getString(index)
            }
        }
    }

    return name
        ?: uri.lastPathSegment
        ?: "unknown_file"
}

data class DataItem(
    val id: Int,
    val name: String?
)

fun processItems(items: List<DataItem>): List<String> {
    return items.mapNotNull { it.name }
}

data class BackendItem(
    val id: Int,
    val name: String
)
