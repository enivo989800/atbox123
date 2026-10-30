package com.yuivo9999.mybox123.data.source

import org.json.JSONObject

data class NativeContentSource(
    val sourceId: String,
    val name: String,
    val sourceType: String, // "movie" or "live"
    val enabled: Boolean = true,
    val order: Int = 0,
    val api: String = "",
    val ext: String = "",
    val jar: String = "",
    val type: Int = 0,
    val sourceRef: String = "",
    val content: String = "",
    val liveMode: String = "",
    val rawJson: String = "{}"
) {
    val displayRef: String get() = sourceRef.ifBlank { api.ifBlank { ext } }

    fun toJson(): JSONObject = try {
        JSONObject(rawJson)
            .put("sourceId", sourceId)
            .put("name", name)
            .put("sourceType", sourceType)
            .put("enabled", enabled)
            .put("order", order)
            .put("api", api)
            .put("ext", ext)
            .put("jar", jar)
            .put("type", type)
            .put("sourceRef", sourceRef)
            .put("content", content)
            .put("liveMode", liveMode)
    } catch (_: Exception) {
        JSONObject()
            .put("sourceId", sourceId)
            .put("name", name)
            .put("sourceType", sourceType)
            .put("enabled", enabled)
            .put("order", order)
            .put("api", api)
            .put("ext", ext)
            .put("jar", jar)
            .put("type", type)
            .put("sourceRef", sourceRef)
            .put("content", content)
            .put("liveMode", liveMode)
    }

    companion object {
        fun fromJson(json: JSONObject, order: Int = 0): NativeContentSource {
            val key = json.optString("key").ifBlank { json.optString("id") }
            val sourceId = json.optString("sourceId").ifBlank { key }.ifBlank { "source_${System.currentTimeMillis()}" }
            val name = json.optString("name").ifBlank { "未命名源" }
            val sourceType = json.optString("sourceType").ifBlank {
                if (json.has("channels") || json.optString("type") == "live") "live" else "movie"
            }
            val api = json.optString("api")
            val ext = json.optString("ext")
            val rawRef = json.optString("sourceRef")
                .ifBlank { json.optString("url") }
                .ifBlank { api }
                .ifBlank { ext }
            val content = json.optString("content")
            val liveMode = json.optString("liveMode")

            return NativeContentSource(
                sourceId = sourceId,
                name = name,
                sourceType = sourceType,
                enabled = json.optBoolean("enabled", true),
                order = json.optInt("order", order),
                api = api,
                ext = ext,
                jar = json.optString("jar"),
                type = json.optInt("type", 0),
                sourceRef = rawRef,
                content = content,
                liveMode = liveMode,
                rawJson = json.toString()
            )
        }
    }
}
