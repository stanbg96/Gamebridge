package com.gamebridge.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp


@Composable
private fun NativeSampleScene(modifier: Modifier = Modifier) {
 var player by remember { mutableStateOf(Offset(0.5f, 0.68f)) }
 Canvas(modifier.pointerInput(Unit) { androidx.compose.foundation.gestures.detectTapGestures { tap ->
  player = Offset((tap.x/size.width).coerceIn(.08f,.92f),(tap.y/size.height).coerceIn(.12f,.9f))
 } }) {
  drawRect(Color(0xFF17251F))
  val tile=48f
  var y=0f
  while(y<size.height) {
   var x=0f
   while(x<size.width) {
    if (((x/tile).toInt()+(y/tile).toInt())%2==0)
     drawRect(Color(0xFF24382B),topLeft=Offset(x,y),size=androidx.compose.ui.geometry.Size(tile,tile))
    x+=tile
   }
   y+=tile
  }
  // landmarks / obstacles
  drawCircle(Color(0xFF6B4D32),22f,Offset(size.width*.25f,size.height*.28f))
  drawCircle(Color(0xFF6B4D32),22f,Offset(size.width*.72f,size.height*.42f))
  drawCircle(Color(0xFFB9D8F5),13f,Offset(size.width*player.x,size.height*player.y))
  drawCircle(Color(0xFF477FB8),6f,Offset(size.width*player.x,size.height*player.y))
 }
}

class MainActivity : ComponentActivity() {
 private var chosenFolder by mutableStateOf<Uri?>(null)
 private var scanReport by mutableStateOf("Select a folder containing game files.")
 private var controllerReport by mutableStateOf("Checking connected controllers…")
 private var assetReport by mutableStateOf("No single file inspected yet.")
 private var inspectedAsset by mutableStateOf<IntakeAsset?>(null)
 private var manifestReport by mutableStateOf("No port manifest created yet.")
 private var manifestJson by mutableStateOf<String?>(null)
 private val manifestSaver=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
  val json=manifestJson
  if(uri!=null && json!=null) try { contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }; manifestReport="Manifest saved successfully." } catch(e:Exception) { manifestReport="Save failed: ${e.message}" }\n }
 private val filePicker=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if(uri!=null) try {
   contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
   inspectedAsset=AssetIntake.inspect(contentResolver,uri)
   assetReport=AssetIntake.report(inspectedAsset!!)
  } catch(e:Exception) { assetReport="File inspection error: ${e.message}" }
 }
 private val picker=registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
  chosenFolder=uri
  if(uri!=null) try {
   contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
   scanTree(uri)
  } catch(e:Exception) { scanReport="Folder access error: ${e.message}" }
 }
 private fun scanTree(tree:Uri) {
  val reports=mutableListOf<String>(); var count=0
  fun visit(parent:Uri,depth:Int) {
   if(depth>5 || count>500) return
   val parentId=DocumentsContract.getDocumentId(parent)
   val children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parentId)
   contentResolver.query(children,arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use { c ->
    val idCol=c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
    val nameCol=c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    val mimeCol=c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
    while(c.moveToNext() && count++<500) {
     val id=c.getString(idCol); val name=c.getString(nameCol) ?: "Unnamed"; val mime=c.getString(mimeCol)
     val child=DocumentsContract.buildDocumentUriUsingTree(tree,id)
     if(mime==DocumentsContract.Document.MIME_TYPE_DIR) visit(child,depth+1)
     else if(name.endsWith(".exe",true)||name.endsWith(".dll",true)) {
      val p=PeInspector.inspect(contentResolver,child,name)
      reports += "${p.fileName} — ${if(p.isPe) "PE" else "not identified"}\n${p.architecture} · ${p.machine} · ${p.subsystem}\n${p.note}"
     }
    }
   }
  }
  try {
   val root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree))
   visit(root,0)
   scanReport=if(reports.isEmpty()) "No executable/library files detected (scan limit 500 entries, depth 5)." else reports.joinToString("\n\n")
  } catch(e:Exception) { scanReport="Scan error: ${e.message}" }
 }
 private fun refreshControllers() {
  val ids=InputDevice.getDeviceIds()
  val list: List<InputDevice> = ids
        .mapNotNull { deviceId -> InputDevice.getDevice(deviceId) }
        .filter { device ->
            (device.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (device.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        }

    controllerReport = if (list.isEmpty()) {
        "No gamepad detected. Pair a Bluetooth controller and refresh."
    } else {
        list.joinToString("\n") { device ->
            "• ${device.name} (id ${device.id})"
        }
    }
 override fun dispatchKeyEvent(event:KeyEvent):Boolean {
  if(event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
     event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) return super.dispatchKeyEvent(event)
  return super.dispatchKeyEvent(event)
 }
 override fun dispatchGenericMotionEvent(event:MotionEvent):Boolean {
  if(event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) { /* analog axes hook */ }
  return super.dispatchGenericMotionEvent(event)
 }
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  val saved=ProfileStore.load(this)
  setContent {
   MaterialTheme {
    var title by remember { mutableStateOf(saved.title) }
    var exe by remember { mutableStateOf(saved.executableName) }
    var arch by remember { mutableStateOf(saved.architecture) }
    var touch by remember { mutableStateOf(saved.touchControls) }
    var bluetooth by remember { mutableStateOf(saved.bluetoothController) }
    var fps by remember { mutableIntStateOf(saved.targetFps) }
    var res by remember { mutableStateOf(saved.resolution) }
    var api by remember { mutableStateOf(saved.graphicsApi) }
    var opacity by remember { mutableFloatStateOf(saved.touchOpacity) }
    var status by remember { mutableStateOf("Profile loaded.") }
    var pipelineReport by remember { mutableStateOf("No pipeline run yet.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
     verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Text("GameBridge",style=MaterialTheme.typography.headlineLarge)
     Text("Porting Workbench · v0.19",style=MaterialTheme.typography.titleMedium)
     Text("Native sample scene · tap to move", style=MaterialTheme.typography.titleSmall)
     NativeSampleScene(Modifier.fillMaxWidth().height(220.dp))
     OutlinedTextField(title,{title=it},label={Text("Game title")},singleLine=true)
     OutlinedTextField(exe,{exe=it},label={Text("Executable name")},singleLine=true)
     OutlinedTextField(arch,{arch=it},label={Text("Detected architecture")},singleLine=true)
     Button(onClick={picker.launch(null)}) { Text("Scan game folder") }
     Button(onClick={filePicker.launch(arrayOf("*/*"))}) { Text("Inspect one game file") }
     Text(assetReport,style=MaterialTheme.typography.bodySmall)
     Button(onClick={
      val asset=inspectedAsset
      manifestJson=if(asset==null) null else PortManifest.create(asset,arch,api,fps).toString(2)
      manifestReport=manifestJson ?: "Choose a file first."
     }) { Text("Create Android port manifest") }
     Text(manifestReport,style=MaterialTheme.typography.bodySmall)
     Button(onClick={ if(manifestJson!=null) manifestSaver.launch("gamebridge-port-manifest.json") }) { Text("Export manifest JSON") }
     Text(chosenFolder?.toString() ?: "No folder selected",style=MaterialTheme.typography.bodySmall)
     Text("Executable report",style=MaterialTheme.typography.titleMedium)
     Text(scanReport,style=MaterialTheme.typography.bodySmall)
     HorizontalDivider()
     Text("Touch & controller profile",style=MaterialTheme.typography.titleMedium)
     Row { Checkbox(touch,{touch=it}); Text("Virtual touch joystick",Modifier.padding(top=12.dp)) }
     Row { Checkbox(bluetooth,{bluetooth=it}); Text("Bluetooth / USB controller",Modifier.padding(top=12.dp)) }
     Text("Touch opacity ${(opacity*100).toInt()}%")
     Slider(opacity,{opacity=it},valueRange=0.2f..1f)
     Button(onClick={refreshControllers()}) { Text("Refresh controller list") }
     Text(controllerReport,style=MaterialTheme.typography.bodySmall)
     Text("Touch layout preview")
     Canvas(Modifier.fillMaxWidth().height(130.dp)) {
      drawRoundRect(Color(0xFF202A36),cornerRadius=androidx.compose.ui.geometry.CornerRadius(14f,14f))
      drawCircle(Color(0x667EA7FF),radius=38f,center=Offset(65f,size.height-55f))
      drawCircle(Color(0xCC7EA7FF),radius=17f,center=Offset(65f,size.height-55f))
      drawCircle(Color(0x667EA7FF),radius=20f,center=Offset(size.width-55f,size.height-70f))
      drawCircle(Color(0x667EA7FF),radius=20f,center=Offset(size.width-100f,size.height-35f))
     }
     HorizontalDivider()
     Text("Graphics settings",style=MaterialTheme.typography.titleMedium)
     Text("FPS: $fps"); Slider(fps.toFloat(),{fps=it.toInt()},valueRange=30f..120f,steps=5)
     Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
      listOf("854 × 480","1280 × 720","1920 × 1080").forEach { OutlinedButton(onClick={res=it}) { Text(it) } }
     }
     Text("Resolution: $res")
     Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
      listOf("Auto","Vulkan","OpenGL").forEach { OutlinedButton(onClick={api=it}) { Text(it) } }
     }
     Text("API preference: $api")
     Button(onClick={
      ProfileStore.save(this@MainActivity,GameProfile(title,exe,arch,api,res,fps,touch,bluetooth,opacity))
      status="Saved locally on this device."
     }) { Text("Save game profile") }
     Text(status)
     HorizontalDivider()
     Text("Port backend status",style=MaterialTheme.typography.titleMedium)
     Button(onClick={
      pipelineReport = PortPipeline.plan(title, exe, arch, api, res, fps)
     }) { Text("Run porting preflight") }
     Button(onClick={
      pipelineReport = NativeDemoBackend.run(title, res, fps, api)
     }) { Text("Run connected native demo backend") }
     Text(pipelineReport,style=MaterialTheme.typography.bodySmall)
     Text("Preflight is diagnostic. The native demo backend exercises the Android-side pipeline only; it does not load or convert third-party game binaries.")
    }
   }
  }
  refreshControllers()
 }
}
