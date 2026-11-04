package com.gamebridge.app

import android.content.Context
import org.json.JSONObject

data class GameProfile(
 val title: String = "Untitled game",
 val executableName: String = "",
 val architecture: String = "Unknown",
 val graphicsApi: String = "Auto",
 val resolution: String = "1280 × 720",
 val targetFps: Int = 60,
 val touchControls: Boolean = true,
 val bluetoothController: Boolean = true,
 val touchOpacity: Float = 0.75f
)

object ProfileStore {
 private const val PREFS = "gamebridge_profiles"
 private const val KEY = "current_profile"
 fun save(context: Context, p: GameProfile) {
  val json = JSONObject().apply {
   put("title",p.title); put("executableName",p.executableName); put("architecture",p.architecture)
   put("graphicsApi",p.graphicsApi); put("resolution",p.resolution); put("targetFps",p.targetFps)
   put("touchControls",p.touchControls); put("bluetoothController",p.bluetoothController); put("touchOpacity",p.touchOpacity.toDouble())
  }
  context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,json.toString()).apply()
 }
 fun load(context: Context): GameProfile {
  val raw = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,null) ?: return GameProfile()
  return try {
   val j=JSONObject(raw)
   GameProfile(j.optString("title","Untitled game"),j.optString("executableName",""),
    j.optString("architecture","Unknown"),j.optString("graphicsApi","Auto"),
    j.optString("resolution","1280 × 720"),j.optInt("targetFps",60),
    j.optBoolean("touchControls",true),j.optBoolean("bluetoothController",true),
    j.optDouble("touchOpacity",0.75).toFloat())
  } catch (_: Exception) { GameProfile() }
 }
}
