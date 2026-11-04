package com.gamebridge.app

import org.json.JSONArray
import org.json.JSONObject

/**
 * Creates a portable, human-readable porting manifest from inspected asset metadata.
 * This is planning output only; it never claims to produce a runnable port.
 */
object PortManifest {
    fun create(asset: IntakeAsset, target: String = "android-arm64-v8a",
               renderer: String = "vulkan", fps: Int = 30): JSONObject {
        val checks = JSONArray()
        checks.put(check("Asset metadata", true, "Filename and size are available"))
        checks.put(check("Source code / engine SDK", false, "Not detected by metadata inspection"))
        checks.put(check("Android-compatible engine backend", false, "Must be supplied per title"))
        checks.put(check("Legal asset / executable access", false, "User must verify ownership and permissions"))
        return JSONObject()
            .put("format", "gamebridge-port-manifest")
            .put("manifestVersion", 1)
            .put("asset", JSONObject()
                .put("name", asset.displayName)
                .put("extension", asset.extension)
                .put("kind", asset.kind.name)
                .put("sizeBytes", asset.sizeBytes ?: JSONObject.NULL))
            .put("target", JSONObject()
                .put("abi", target)
                .put("renderer", renderer)
                .put("fpsCap", fps))
            .put("checks", checks)
            .put("status", "planning_only")
            .put("note", "Manifest does not convert or execute the selected game.")
    }

    private fun check(name: String, passed: Boolean, detail: String) =
        JSONObject().put("name", name).put("passed", passed).put("detail", detail)
}
