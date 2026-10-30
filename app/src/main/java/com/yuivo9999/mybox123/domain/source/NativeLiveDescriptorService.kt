package com.yuivo9999.mybox123.domain.source

import com.yuivo9999.mybox123.core.network.NativeHttpClient
import com.yuivo9999.mybox123.data.movie.StaticCatalogData
import com.yuivo9999.mybox123.data.source.NativeContentSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service for loading and parsing live TV sources (TV1 dedicated #genre#, standard M3U, JSON, and built-in channels).
 * Generates channels and stream descriptors for native playback without auto-starting video.
 */
class NativeLiveDescriptorService(
    private val httpClient: NativeHttpClient = NativeHttpClient()
) {

    private data class StreamItem(val name: String, val url: String)

    private class ChannelBuilder(
        val channelId: String,
        val name: String,
        val category: String,
        var logo: String,
        val sourceId: String,
        val sourceItemId: String,
        val streams: MutableList<StreamItem> = mutableListOf()
    ) {
        fun toJsonObject(): JSONObject {
            val streamsArr = JSONArray()
            streams.forEachIndexed { index, s ->
                streamsArr.put(
                    JSONObject()
                        .put("streamId", "stream_${index + 1}")
                        .put("name", s.name)
                        .put("url", s.url)
                        .put("playUrl", s.url)
                )
            }
            return JSONObject()
                .put("channelId", channelId)
                .put("name", name)
                .put("category", category)
                .put("logo", logo)
                .put("currentProgram", "实时直播")
                .put("sourceId", sourceId)
                .put("sourceItemId", sourceItemId)
                .put("streams", streamsArr)
        }
    }

    /**
     * Load channels from a given source.
     * Guaranteed to return channel definitions without triggering playback.
     */
    fun loadChannels(source: NativeContentSource, forceRefresh: Boolean = false): List<JSONObject> {
        if (source.sourceId == "builtin_live") {
            return StaticCatalogData.liveChannels.map { JSONObject(it.toString()) }
        }

        val rawText = fetchOrGetContent(source)
        if (rawText.isBlank()) {
            return emptyList()
        }

        return parseLiveContent(source, rawText)
    }

    /**
     * Load stream candidates for a channel.
     */
    fun loadStreams(source: NativeContentSource, channel: JSONObject, forceRefresh: Boolean = false): List<JSONObject> {
        val streams = channel.optJSONArray("streams")
        val list = mutableListOf<JSONObject>()
        if (streams != null) {
            for (i in 0 until streams.length()) {
                streams.optJSONObject(i)?.let { list.add(it) }
            }
        }
        if (list.isEmpty()) {
            val url = channel.optString("url").ifBlank { channel.optString("playUrl") }
            if (url.isNotBlank()) {
                list.add(
                    JSONObject()
                        .put("streamId", "stream_1")
                        .put("name", "默认线路")
                        .put("url", url)
                        .put("playUrl", url)
                )
            }
        }
        return list
    }

    private fun fetchOrGetContent(source: NativeContentSource): String {
        // 1. Literal content stored in source
        if (source.content.isNotBlank()) {
            return source.content
        }

        val ref = source.sourceRef.trim()

        // 2. If sourceRef itself contains newline or #genre#, it was pasted as content
        if (ref.contains("#genre#", ignoreCase = true) || ref.contains("\n") || ref.startsWith("#EXTM3U", ignoreCase = true)) {
            return ref
        }

        // 3. Network URL: fetch via HTTP client
        if (ref.startsWith("http://", ignoreCase = true) || ref.startsWith("https://", ignoreCase = true)) {
            val response = httpClient.execute(
                NativeHttpClient.NativeHttpRequest(
                    url = ref,
                    timeoutMs = 15_000L
                )
            )
            if (response.ok && response.body.isNotBlank()) {
                return response.body
            }
        }

        // 4. Fallback: check rawJson
        try {
            val json = JSONObject(source.rawJson)
            val embedded = json.optString("content")
            if (embedded.isNotBlank()) return embedded
            val embeddedUrl = json.optString("url")
            if (embeddedUrl.startsWith("http://", ignoreCase = true) || embeddedUrl.startsWith("https://", ignoreCase = true)) {
                val res = httpClient.execute(NativeHttpClient.NativeHttpRequest(url = embeddedUrl, timeoutMs = 15_000L))
                if (res.ok && res.body.isNotBlank()) return res.body
            }
        } catch (_: Exception) {}

        return ""
    }

    private fun parseLiveContent(source: NativeContentSource, text: String): List<JSONObject> {
        val trimmed = text.removePrefix("\uFEFF").trim()
        return when {
            trimmed.startsWith("#EXTM3U", ignoreCase = true) || trimmed.contains("#EXTINF:", ignoreCase = true) -> {
                parseM3uLive(source, trimmed)
            }
            (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]")) -> {
                parseJsonLive(source, trimmed)
            }
            else -> {
                parseTxtLive(source, trimmed)
            }
        }
    }

    /**
     * TV1 dedicated and TVBox / DIYP #genre# format parser.
     * Example:
     *   央视频道,#genre#
     *   CCTV-1 综合,http://...#http://...$备用线路
     *   卫视频道,#genre#
     *   湖南卫视,http://...
     */
    private fun parseTxtLive(source: NativeContentSource, text: String): List<JSONObject> {
        val lines = text.lines()
        var currentCategory = "央视"
        val channelMap = linkedMapOf<String, ChannelBuilder>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            // Category group header: e.g. "央视频道,#genre#" or "[央视频道]"
            if (line.contains("#genre#", ignoreCase = true) || (line.startsWith("[") && line.endsWith("]"))) {
                var cat = if (line.contains("#genre#", ignoreCase = true)) {
                    line.replace(Regex("[,，]?\\s*#genre#.*", RegexOption.IGNORE_CASE), "").trim()
                } else {
                    line.removeSurrounding("[", "]").trim()
                }
                if (cat.isNotBlank()) {
                    currentCategory = cat
                }
                continue
            }

            if (line.startsWith("#") || line.startsWith("//") || line.startsWith(";")) continue

            var name = ""
            var rawUrls = ""
            val commaIndex = line.indexOfAny(charArrayOf(',', '，'))
            if (commaIndex != -1) {
                name = line.substring(0, commaIndex).trim()
                rawUrls = line.substring(commaIndex + 1).trim()
            } else {
                val match = Regex("""^([^\s]+)\s+((?:https?|rtmp|rtsp|p2p|mitv|mms)://.+)""", RegexOption.IGNORE_CASE).find(line)
                if (match != null) {
                    name = match.groupValues[1].trim()
                    rawUrls = match.groupValues[2].trim()
                }
            }

            if (name.isEmpty() || rawUrls.isEmpty()) continue

            val mapKey = "$currentCategory:::$name"
            val builder = channelMap.getOrPut(mapKey) {
                val id = "${source.sourceId}_${channelMap.size + 1}_${name.hashCode()}"
                ChannelBuilder(
                    channelId = id,
                    name = name,
                    category = currentCategory,
                    logo = "",
                    sourceId = source.sourceId,
                    sourceItemId = id
                )
            }

            val candidates = rawUrls.split("#").map { it.trim() }.filter { it.isNotEmpty() }
            for (cand in candidates) {
                var streamUrl = cand
                var streamLabel = "线路 ${builder.streams.size + 1}"
                if (streamUrl.contains("$")) {
                    val parts = streamUrl.split("$")
                    streamUrl = parts[0].trim()
                    if (parts.size > 1 && parts[1].trim().isNotEmpty()) {
                        streamLabel = parts[1].trim()
                    }
                }
                if (streamUrl.startsWith("http://", true) ||
                    streamUrl.startsWith("https://", true) ||
                    streamUrl.startsWith("rtmp://", true) ||
                    streamUrl.startsWith("rtsp://", true) ||
                    streamUrl.contains(".m3u8", true) ||
                    streamUrl.contains(".flv", true) ||
                    streamUrl.contains(".mp4", true)
                ) {
                    builder.streams.add(StreamItem(streamLabel, streamUrl))
                }
            }
        }

        return channelMap.values.map { it.toJsonObject() }
    }

    /**
     * Standard M3U playlist format parser (#EXTM3U and #EXTINF).
     */
    private fun parseM3uLive(source: NativeContentSource, text: String): List<JSONObject> {
        val lines = text.lines()
        val channels = mutableListOf<JSONObject>()
        var pendingAttrs: MutableMap<String, String>? = null
        var pendingName = ""

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val attrs = mutableMapOf<String, String>()
                val attrRegex = Regex("""([\w-]+)="([^"]*)"""")
                attrRegex.findAll(line).forEach { match ->
                    attrs[match.groupValues[1].lowercase()] = match.groupValues[2]
                }
                val commaIdx = line.indexOf(',')
                val title = if (commaIdx != -1) line.substring(commaIdx + 1).trim() else ""
                pendingName = title.ifBlank { attrs["tvg-name"] ?: "频道 ${channels.size + 1}" }
                pendingAttrs = attrs
                continue
            }

            if (line.startsWith("#")) continue

            if (pendingAttrs != null || pendingName.isNotEmpty()) {
                val attrs = pendingAttrs ?: emptyMap()
                val category = attrs["group-title"]?.trim()?.ifBlank { "未分类" } ?: "未分类"
                val logo = attrs["tvg-logo"].orEmpty()
                val channelId = "${source.sourceId}_m3u_${channels.size + 1}"
                val streamUrl = line

                val streams = JSONArray().apply {
                    put(
                        JSONObject()
                            .put("streamId", "stream_1")
                            .put("name", attrs["tvg-name"]?.ifBlank { "主线路" } ?: "主线路")
                            .put("url", streamUrl)
                            .put("playUrl", streamUrl)
                    )
                }

                val channelObj = JSONObject()
                    .put("channelId", channelId)
                    .put("name", pendingName)
                    .put("category", category)
                    .put("logo", logo)
                    .put("currentProgram", "实时直播")
                    .put("sourceId", source.sourceId)
                    .put("sourceItemId", channelId)
                    .put("streams", streams)

                channels.add(channelObj)
                pendingAttrs = null
                pendingName = ""
            }
        }
        return channels
    }

    /**
     * JSON live format parser.
     */
    private fun parseJsonLive(source: NativeContentSource, text: String): List<JSONObject> {
        val channels = mutableListOf<JSONObject>()
        try {
            val trimmed = text.trim()
            val jsonArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val root = JSONObject(trimmed)
                root.optJSONArray("channels")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("list")
                    ?: JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val name = item.optString("name").ifBlank { item.optString("title") }.ifBlank { "频道 ${i + 1}" }
                val category = item.optString("category").ifBlank { item.optString("group") }.ifBlank { "未分类" }
                val logo = item.optString("logo").ifBlank { item.optString("icon") }
                val channelId = item.optString("channelId").ifBlank { "${source.sourceId}_json_${i + 1}" }

                val streams = JSONArray()
                val rawStreams = item.optJSONArray("streams") ?: item.optJSONArray("urls")
                if (rawStreams != null) {
                    for (sIdx in 0 until rawStreams.length()) {
                        val sObj = rawStreams.opt(sIdx)
                        if (sObj is JSONObject) {
                            streams.put(
                                JSONObject()
                                    .put("streamId", sObj.optString("streamId").ifBlank { "stream_${sIdx + 1}" })
                                    .put("name", sObj.optString("name").ifBlank { sObj.optString("label") }.ifBlank { "线路 ${sIdx + 1}" })
                                    .put("url", sObj.optString("url").ifBlank { sObj.optString("mediaUrl") })
                                    .put("playUrl", sObj.optString("url").ifBlank { sObj.optString("mediaUrl") })
                            )
                        } else if (sObj is String && sObj.isNotBlank()) {
                            streams.put(
                                JSONObject()
                                    .put("streamId", "stream_${sIdx + 1}")
                                    .put("name", "线路 ${sIdx + 1}")
                                    .put("url", sObj)
                                    .put("playUrl", sObj)
                            )
                        }
                    }
                } else {
                    val singleUrl = item.optString("url").ifBlank { item.optString("mediaUrl") }
                    if (singleUrl.isNotBlank()) {
                        streams.put(
                            JSONObject()
                                .put("streamId", "stream_1")
                                .put("name", "主线路")
                                .put("url", singleUrl)
                                .put("playUrl", singleUrl)
                        )
                    }
                }

                channels.add(
                    JSONObject()
                        .put("channelId", channelId)
                        .put("name", name)
                        .put("category", category)
                        .put("logo", logo)
                        .put("currentProgram", item.optString("currentProgram").ifBlank { "实时直播" })
                        .put("sourceId", source.sourceId)
                        .put("sourceItemId", channelId)
                        .put("streams", streams)
                )
            }
        } catch (_: Exception) {}
        return channels
    }
}
