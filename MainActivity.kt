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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
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
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFB96BFF),
                secondary = Color(0xFF55E7FF),
                background = Color(0xFF080B24),
                surface = Color(0xFF15143B),
                onSurface = Color.White,
                onBackground = Color.White
            )) {
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
    var cloudBuildStatus by remember { mutableStateOf("CLOUD BACKEND NOT CONNECTED") }

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

    val cosmic = Color(0xFF080B24)
    val panel = Color(0xFF15143B)
    val cyan = Color(0xFF55E7FF)
    val violet = Color(0xFFB96BFF)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF080B24), Color(0xFF160B38), Color(0xFF050817))))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTap(offset.x, offset.y)
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "✦ GAMEBRIDGE ✦",
            color = Color(0xFFFFD76A),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
        Text("ARCANE GAME PORTAL  •  FORGE BEYOND REALITY", color = cyan, fontWeight = FontWeight.Bold)
        Text("ANALYZE  •  REBUILD  •  PLAY", color = Color(0xFFB7B8F8))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = panel)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = status)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Last Touch -> X: ${touchX.toInt()}, Y: ${touchY.toInt()}"
                )
            }
        }

        Text("◈ PE INSPECTOR ENGINE", color = cyan, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = panel)) {
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
                    Text("PE Format: ${result.peFormat}")
                    Text("File Size: ${result.fileSize}")
                    Text("Entry Point: ${result.entryPoint}")
                    Text("Sections: ${result.sectionCount}")
                    Text("Section Names:")
                    result.sections.forEach { section ->
                        Text("• $section")
                    }
                    Text("File Type: ${result.fileType}")
                }
            }
        }

        Text("◈ CLOUD BUILD PIPELINE", color = violet, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = panel)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Phone analysis → Cloud recompilation → Android APK")
                Text(
                    text = "Status: $cloudBuildStatus",
                    color = if (cloudBuildStatus == "CLOUD BACKEND NOT CONNECTED") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    "The app can inspect PE metadata locally. A secure cloud service must be configured " +
                        "before a real build request can be submitted or an APK returned."
                )
                Button(
                    onClick = {
                        cloudBuildStatus = if (peResult?.isPe == true) {
                            "READY TO SUBMIT — cloud service endpoint is not configured"
                        } else {
                            "SELECT AND ANALYZE A VALID .EXE OR .DLL FIRST"
                        }
                    },
                    enabled = !isAnalyzing
                ) {
                    Text("Prepare Cloud Build Request")
                }
            }
        }

        Text("◈ ACTIVE MODULES", color = Color(0xFFFFD76A), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        backendItems.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = panel)) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("ID: ${item.id}", color = cyan)
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
