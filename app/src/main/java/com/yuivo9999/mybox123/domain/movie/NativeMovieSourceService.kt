package com.yuivo9999.mybox123.domain.movie

import com.yuivo9999.mybox123.core.network.NativeHttpClient
import com.yuivo9999.mybox123.data.movie.NativeMovieCategory
import com.yuivo9999.mybox123.data.movie.NativeMovieItem
import com.yuivo9999.mybox123.data.movie.StaticCatalogData
import com.yuivo9999.mybox123.data.source.NativeContentSource
import com.yuivo9999.mybox123.data.source.NativeContentSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URI
import java.net.URLEncoder

class NativeMovieSourceService(
    private val sourceRepository: NativeContentSourceRepository? = null,
    private val httpClient: NativeHttpClient = NativeHttpClient()
) {

    private val userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private fun findSource(sourceId: String): NativeContentSource? {
        if (sourceId == "builtin" || sourceId.isBlank()) return null
        return sourceRepository?.getAll()?.firstOrNull { it.sourceId == sourceId }
    }

    private fun getEffectiveApiUrl(source: NativeContentSource): String {
        if (source.api.startsWith("http://") || source.api.startsWith("https://")) {
            return source.api.trim()
        }
        val ref = source.sourceRef.trim()
        if (ref.startsWith("http://") || ref.startsWith("https://")) {
            return ref
        }
        if (source.content.isNotBlank()) {
            try {
                val json = JSONObject(source.content)
                if (json.has("api")) {
                    val a = json.optString("api")
                    if (a.startsWith("http")) return a.trim()
                }
                if (json.has("sites")) {
                    val sites = json.optJSONArray("sites")
                    if (sites != null && sites.length() > 0) {
                        val s0 = sites.optJSONObject(0)
                        val a = s0?.optString("api").orEmpty()
                        if (a.startsWith("http")) return a.trim()
                    }
                }
            } catch (_: Exception) {}
        }
        return ""
    }

    private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
        val base = baseUrl.trim()
        if (base.isEmpty()) return ""
        val queryParams = mutableMapOf<String, String>()
        try {
            val uri = URI(base)
            if (uri.query != null && uri.query.isNotEmpty()) {
                uri.query.split("&").forEach { pair ->
                    val idx = pair.indexOf('=')
                    if (idx > 0) {
                        val k = pair.substring(0, idx)
                        val v = pair.substring(idx + 1)
                        queryParams[k] = v
                    } else if (pair.isNotEmpty()) {
                        queryParams[pair] = ""
                    }
                }
            }
        } catch (_: Exception) {}

        val cleanBase = if (base.contains("?")) base.substringBefore("?") else base
        params.forEach { (k, v) ->
            if (v.isNotEmpty()) {
                queryParams[k] = v
            }
        }
        val queryStr = queryParams.entries.joinToString("&") { (k, v) ->
            val ek = URLEncoder.encode(k, "UTF-8")
            val ev = URLEncoder.encode(v, "UTF-8")
            "$ek=$ev"
        }
        return if (queryStr.isNotEmpty()) "$cleanBase?$queryStr" else cleanBase
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
    }

    private fun parseEpisodes(vodPlayFrom: String, vodPlayUrl: String): List<JSONObject> {
        if (vodPlayUrl.isBlank()) return emptyList()

        val fromList = vodPlayFrom.split("$$$").map { it.trim() }.filter { it.isNotEmpty() }
        val urlGroups = vodPlayUrl.split("$$$").map { it.trim() }.filter { it.isNotEmpty() }

        data class RawEp(val title: String, val url: String, val lineLabel: String)
        val lineEpisodes = mutableListOf<List<RawEp>>()

        for (lineIdx in urlGroups.indices) {
            val groupStr = urlGroups[lineIdx]
            val lineLabel = fromList.getOrNull(lineIdx)?.ifBlank { "线路 ${lineIdx + 1}" } ?: "线路 ${lineIdx + 1}"
            val epStrings = groupStr.split(Regex("[#\n\r]+")).map { it.trim() }.filter { it.isNotEmpty() }
            val eps = epStrings.mapIndexed { epIdx, epStr ->
                val title: String
                val url: String
                if (epStr.contains("$")) {
                    title = epStr.substringBefore("$").trim().ifBlank { "第 ${epIdx + 1} 集" }
                    url = epStr.substringAfter("$").trim()
                } else if (epStr.startsWith("http://") || epStr.startsWith("https://")) {
                    title = if (epStrings.size == 1) "正片" else "第 ${epIdx + 1} 集"
                    url = epStr
                } else {
                    title = epStr
                    url = ""
                }
                RawEp(title, url, lineLabel)
            }.filter { it.url.startsWith("http://") || it.url.startsWith("https://") }
            if (eps.isNotEmpty()) {
                lineEpisodes.add(eps)
            }
        }

        if (lineEpisodes.isEmpty()) return emptyList()

        val maxEps = lineEpisodes.maxOf { it.size }
        val result = mutableListOf<JSONObject>()

        for (i in 0 until maxEps) {
            val candidatesArr = JSONArray()
            var epTitle = "第 ${i + 1} 集"
            var primaryUrl = ""

            lineEpisodes.forEachIndexed { lineIdx, eps ->
                val raw = eps.getOrNull(i)
                if (raw != null && raw.url.isNotBlank()) {
                    if (primaryUrl.isEmpty()) {
                        primaryUrl = raw.url
                        epTitle = raw.title
                    }
                    candidatesArr.put(
                        JSONObject()
                            .put("label", "${raw.lineLabel} (线路 ${lineIdx + 1})")
                            .put("mediaUrl", raw.url)
                            .put("url", raw.url)
                    )
                }
            }

            if (primaryUrl.isNotBlank()) {
                val epJson = JSONObject()
                    .put("episodeId", "ep_${i + 1}")
                    .put("title", epTitle)
                    .put("name", epTitle)
                    .put("url", primaryUrl)
                    .put("playUrl", primaryUrl)
                    .put("mediaUrl", primaryUrl)
                    .put("candidates", candidatesArr)
                result.add(epJson)
            }
        }

        return result
    }

    private fun parseXmlCatalog(xmlString: String, source: NativeContentSource): List<NativeMovieItem> {
        val items = mutableListOf<NativeMovieItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlString))
            var eventType = parser.eventType
            var inVideo = false
            var currentTag = ""
            var vodId = ""
            var vodName = ""
            var typeName = ""
            var vodPic = ""
            var vodYear = ""
            var vodArea = ""
            var vodRemarks = ""
            var vodActor = ""
            var vodDirector = ""
            var vodContent = ""
            val playFromList = mutableListOf<String>()
            val playUrlList = mutableListOf<String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("video", ignoreCase = true)) {
                            inVideo = true
                            vodId = ""; vodName = ""; typeName = ""; vodPic = ""; vodYear = ""; vodArea = ""
                            vodRemarks = ""; vodActor = ""; vodDirector = ""; vodContent = ""
                            playFromList.clear(); playUrlList.clear()
                        } else if (inVideo && currentTag.equals("dd", ignoreCase = true)) {
                            val flag = parser.getAttributeValue(null, "flag") ?: "默认"
                            playFromList.add(flag)
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inVideo) {
                            val text = parser.text.trim()
                            if (text.isNotEmpty()) {
                                when (currentTag.lowercase()) {
                                    "id" -> vodId += text
                                    "name" -> vodName += text
                                    "type" -> typeName += text
                                    "pic" -> vodPic += text
                                    "year" -> vodYear += text
                                    "area" -> vodArea += text
                                    "note", "state" -> vodRemarks += text
                                    "actor" -> vodActor += text
                                    "director" -> vodDirector += text
                                    "des" -> vodContent += text
                                    "dd" -> playUrlList.add(text)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("video", ignoreCase = true) && inVideo) {
                            inVideo = false
                            if (vodName.isNotBlank()) {
                                val episodes = parseEpisodes(
                                    playFromList.joinToString("$$$"),
                                    playUrlList.joinToString("$$$")
                                )
                                val item = NativeMovieItem(
                                    sourceId = source.sourceId,
                                    sourceName = source.name,
                                    sourceItemId = vodId,
                                    contentId = vodId,
                                    title = vodName,
                                    poster = vodPic,
                                    category = typeName,
                                    year = vodYear,
                                    region = vodArea,
                                    updateInfo = vodRemarks,
                                    director = vodDirector,
                                    actors = vodActor.split(Regex("[,，/\\s]+")).filter { it.isNotBlank() },
                                    description = stripHtml(vodContent),
                                    episodes = episodes,
                                    rawJson = JSONObject().put("vod_id", vodId).put("vod_name", vodName).toString()
                                )
                                items.add(item)
                            }
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return items
    }

    private fun parseXmlCategories(xmlString: String): List<NativeMovieCategory> {
        val list = mutableListOf<NativeMovieCategory>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlString))
            var eventType = parser.eventType
            var inClass = false
            var currentTag = ""
            var catId = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("class", ignoreCase = true)) {
                            inClass = true
                        } else if (inClass && currentTag.equals("ty", ignoreCase = true)) {
                            catId = parser.getAttributeValue(null, "id") ?: ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inClass && currentTag.equals("ty", ignoreCase = true)) {
                            val name = parser.text.trim()
                            if (catId.isNotBlank() && name.isNotBlank()) {
                                list.add(NativeMovieCategory(id = catId, name = name))
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("class", ignoreCase = true)) {
                            inClass = false
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return list
    }

    suspend fun getCategories(sourceId: String): List<NativeMovieCategory> = withContext(Dispatchers.IO) {
        val source = findSource(sourceId) ?: return@withContext StaticCatalogData.categories
        val apiUrl = getEffectiveApiUrl(source)
        if (apiUrl.isBlank()) return@withContext StaticCatalogData.categories

        val requestUrl = buildUrl(apiUrl, mapOf("ac" to "list"))
        val response = httpClient.execute(
            NativeHttpClient.NativeHttpRequest(
                url = requestUrl,
                headers = mapOf("User-Agent" to userAgent, "Accept" to "application/json, text/xml, */*"),
                timeoutMs = 12_000L
            )
        )

        if (!response.ok || response.body.isBlank()) {
            return@withContext StaticCatalogData.categories
        }

        val body = response.body.trim()
        if (body.startsWith("<")) {
            val xmlCats = parseXmlCategories(body)
            if (xmlCats.isNotEmpty()) return@withContext xmlCats
        }

        try {
            val json = JSONObject(body)
            val classArr = json.optJSONArray("class")
            if (classArr != null && classArr.length() > 0) {
                val result = mutableListOf<NativeMovieCategory>()
                for (i in 0 until classArr.length()) {
                    val obj = classArr.optJSONObject(i) ?: continue
                    val id = obj.optString("type_id").ifBlank { obj.optInt("type_id").toString() }
                    val name = obj.optString("type_name")
                    if (id.isNotBlank() && name.isNotBlank()) {
                        result.add(NativeMovieCategory(id = id, name = name, rawJson = obj.toString()))
                    }
                }
                if (result.isNotEmpty()) return@withContext result
            }
        } catch (_: Exception) {}

        StaticCatalogData.categories
    }

    suspend fun getList(
        sourceId: String,
        page: Int = 1,
        pageSize: Int = 30,
        categoryId: String? = null
    ): NativeMoviePage = withContext(Dispatchers.IO) {
        val source = findSource(sourceId)
        if (source == null) {
            val all = StaticCatalogData.movies
            val filtered = all.filter { movie ->
                categoryId == null || categoryId == "all" || when (categoryId) {
                    "movie" -> movie.category == "电影"
                    "series" -> movie.category == "电视剧"
                    "anime" -> movie.category == "动漫"
                    "variety" -> movie.category == "综艺"
                    "short_drama" -> movie.category == "短剧"
                    else -> movie.category.contains(categoryId)
                }
            }
            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = page,
                pageSize = pageSize,
                total = filtered.size.toLong(),
                items = filtered,
                httpStatus = 200
            )
        }

        val apiUrl = getEffectiveApiUrl(source)
        if (apiUrl.isBlank()) {
            return@withContext NativeMoviePage(sourceId = sourceId, page = page, pageSize = pageSize, total = 0, items = emptyList())
        }

        val params = mutableMapOf(
            "ac" to "detail",
            "pg" to page.toString()
        )
        if (!categoryId.isNullOrBlank() && categoryId != "all") {
            params["t"] = categoryId
        }

        var requestUrl = buildUrl(apiUrl, params)
        var response = httpClient.execute(
            NativeHttpClient.NativeHttpRequest(
                url = requestUrl,
                headers = mapOf("User-Agent" to userAgent, "Accept" to "application/json, text/xml, */*"),
                timeoutMs = 12_000L
            )
        )

        // Fallback to ac=videolist if ac=detail failed or was empty
        if (!response.ok || response.body.isBlank()) {
            params["ac"] = "videolist"
            requestUrl = buildUrl(apiUrl, params)
            response = httpClient.execute(
                NativeHttpClient.NativeHttpRequest(
                    url = requestUrl,
                    headers = mapOf("User-Agent" to userAgent, "Accept" to "application/json, text/xml, */*"),
                    timeoutMs = 12_000L
                )
            )
        }

        if (!response.ok || response.body.isBlank()) {
            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = page,
                pageSize = pageSize,
                total = 0,
                items = emptyList(),
                httpStatus = response.status
            )
        }

        val body = response.body.trim()
        if (body.startsWith("<")) {
            val xmlItems = parseXmlCatalog(body, source)
            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = page,
                pageSize = pageSize,
                total = xmlItems.size.toLong(),
                items = xmlItems,
                httpStatus = response.status
            )
        }

        try {
            val json = JSONObject(body)
            val listArr = json.optJSONArray("list")
            val total = json.optLong("total", listArr?.length()?.toLong() ?: 0L)
            val pg = json.optInt("page", page)
            val items = mutableListOf<NativeMovieItem>()

            if (listArr != null) {
                for (i in 0 until listArr.length()) {
                    val obj = listArr.optJSONObject(i) ?: continue
                    val vodId = obj.optString("vod_id").ifBlank { obj.optInt("vod_id").toString() }
                    val vodName = obj.optString("vod_name").ifBlank { obj.optString("title") }
                    val vodPic = obj.optString("vod_pic").ifBlank { obj.optString("pic") }
                    val typeName = obj.optString("type_name").ifBlank { obj.optString("category") }
                    val vodYear = obj.optString("vod_year").ifBlank { obj.optString("year") }
                    val vodArea = obj.optString("vod_area").ifBlank { obj.optString("area") }
                    val vodScore = obj.optString("vod_score").ifBlank { obj.optString("score") }
                    val vodRemarks = obj.optString("vod_remarks").ifBlank { obj.optString("remarks") }
                    val vodDirector = obj.optString("vod_director").ifBlank { obj.optString("director") }
                    val vodActor = obj.optString("vod_actor").ifBlank { obj.optString("actor") }
                    val vodContent = stripHtml(obj.optString("vod_content").ifBlank { obj.optString("content") })
                    val vodPlayFrom = obj.optString("vod_play_from")
                    val vodPlayUrl = obj.optString("vod_play_url")
                    val episodes = parseEpisodes(vodPlayFrom, vodPlayUrl)

                    items.add(
                        NativeMovieItem(
                            sourceId = source.sourceId,
                            sourceName = source.name,
                            sourceItemId = vodId,
                            contentId = vodId,
                            title = vodName,
                            poster = vodPic,
                            category = typeName,
                            year = vodYear,
                            region = vodArea,
                            rating = vodScore,
                            updateInfo = vodRemarks,
                            backdrop = vodPic,
                            director = vodDirector,
                            actors = vodActor.split(Regex("[,，/\\s]+")).filter { it.isNotBlank() },
                            description = vodContent,
                            episodes = episodes,
                            rawJson = obj.toString()
                        )
                    )
                }
            }

            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = pg,
                pageSize = pageSize,
                total = total,
                items = items,
                httpStatus = response.status,
                rawJson = body
            )
        } catch (_: Exception) {
            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = page,
                pageSize = pageSize,
                total = 0,
                items = emptyList(),
                httpStatus = response.status
            )
        }
    }

    suspend fun search(
        sourceId: String,
        keyword: String,
        page: Int = 1,
        pageSize: Int = 30,
        categoryId: String? = null
    ): NativeMoviePage = withContext(Dispatchers.IO) {
        val source = findSource(sourceId)
        if (source == null) {
            val all = StaticCatalogData.movies
            val filtered = all.filter { movie ->
                movie.title.contains(keyword, ignoreCase = true) ||
                movie.category.contains(keyword, ignoreCase = true) ||
                movie.description.contains(keyword, ignoreCase = true)
            }
            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = page,
                pageSize = pageSize,
                total = filtered.size.toLong(),
                items = filtered,
                httpStatus = 200
            )
        }

        val apiUrl = getEffectiveApiUrl(source)
        if (apiUrl.isBlank()) {
            return@withContext NativeMoviePage(sourceId = sourceId, page = page, pageSize = pageSize, total = 0, items = emptyList())
        }

        val params = mutableMapOf(
            "ac" to "detail",
            "wd" to keyword,
            "pg" to page.toString()
        )
        if (!categoryId.isNullOrBlank() && categoryId != "all") {
            params["t"] = categoryId
        }

        var requestUrl = buildUrl(apiUrl, params)
        var response = httpClient.execute(
            NativeHttpClient.NativeHttpRequest(
                url = requestUrl,
                headers = mapOf("User-Agent" to userAgent, "Accept" to "application/json, text/xml, */*"),
                timeoutMs = 12_000L
            )
        )

        // Fallback to ac=videolist or direct wd
        if (!response.ok || response.body.isBlank()) {
            params["ac"] = "videolist"
            requestUrl = buildUrl(apiUrl, params)
            response = httpClient.execute(
                NativeHttpClient.NativeHttpRequest(
                    url = requestUrl,
                    headers = mapOf("User-Agent" to userAgent, "Accept" to "application/json, text/xml, */*"),
                    timeoutMs = 12_000L
                )
            )
        }

        if (!response.ok || response.body.isBlank()) {
            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = page,
                pageSize = pageSize,
                total = 0,
                items = emptyList(),
                httpStatus = response.status
            )
        }

        val body = response.body.trim()
        if (body.startsWith("<")) {
            val xmlItems = parseXmlCatalog(body, source)
            val filtered = xmlItems.filter { it.title.contains(keyword, ignoreCase = true) }
            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = page,
                pageSize = pageSize,
                total = filtered.size.toLong(),
                items = filtered,
                httpStatus = response.status
            )
        }

        try {
            val json = JSONObject(body)
            val listArr = json.optJSONArray("list")
            val total = json.optLong("total", listArr?.length()?.toLong() ?: 0L)
            val pg = json.optInt("page", page)
            val items = mutableListOf<NativeMovieItem>()

            if (listArr != null) {
                for (i in 0 until listArr.length()) {
                    val obj = listArr.optJSONObject(i) ?: continue
                    val vodId = obj.optString("vod_id").ifBlank { obj.optInt("vod_id").toString() }
                    val vodName = obj.optString("vod_name").ifBlank { obj.optString("title") }
                    val vodPic = obj.optString("vod_pic").ifBlank { obj.optString("pic") }
                    val typeName = obj.optString("type_name").ifBlank { obj.optString("category") }
                    val vodYear = obj.optString("vod_year").ifBlank { obj.optString("year") }
                    val vodArea = obj.optString("vod_area").ifBlank { obj.optString("area") }
                    val vodScore = obj.optString("vod_score").ifBlank { obj.optString("score") }
                    val vodRemarks = obj.optString("vod_remarks").ifBlank { obj.optString("remarks") }
                    val vodDirector = obj.optString("vod_director").ifBlank { obj.optString("director") }
                    val vodActor = obj.optString("vod_actor").ifBlank { obj.optString("actor") }
                    val vodContent = stripHtml(obj.optString("vod_content").ifBlank { obj.optString("content") })
                    val vodPlayFrom = obj.optString("vod_play_from")
                    val vodPlayUrl = obj.optString("vod_play_url")
                    val episodes = parseEpisodes(vodPlayFrom, vodPlayUrl)

                    items.add(
                        NativeMovieItem(
                            sourceId = source.sourceId,
                            sourceName = source.name,
                            sourceItemId = vodId,
                            contentId = vodId,
                            title = vodName,
                            poster = vodPic,
                            category = typeName,
                            year = vodYear,
                            region = vodArea,
                            rating = vodScore,
                            updateInfo = vodRemarks,
                            backdrop = vodPic,
                            director = vodDirector,
                            actors = vodActor.split(Regex("[,，/\\s]+")).filter { it.isNotBlank() },
                            description = vodContent,
                            episodes = episodes,
                            rawJson = obj.toString()
                        )
                    )
                }
            }

            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = pg,
                pageSize = pageSize,
                total = total,
                items = items,
                httpStatus = response.status,
                rawJson = body
            )
        } catch (_: Exception) {
            return@withContext NativeMoviePage(
                sourceId = sourceId,
                page = page,
                pageSize = pageSize,
                total = 0,
                items = emptyList(),
                httpStatus = response.status
            )
        }
    }

    suspend fun getDetail(sourceId: String, sourceItemId: String): NativeMovieItem? = withContext(Dispatchers.IO) {
        val source = findSource(sourceId)
        if (source == null) {
            return@withContext StaticCatalogData.movies.firstOrNull {
                it.contentId == sourceItemId || it.sourceItemId == sourceItemId || it.title.equals(sourceItemId, ignoreCase = true)
            }
        }

        val apiUrl = getEffectiveApiUrl(source)
        if (apiUrl.isBlank()) return@withContext null

        val requestUrl = buildUrl(apiUrl, mapOf("ac" to "detail", "ids" to sourceItemId))
        val response = httpClient.execute(
            NativeHttpClient.NativeHttpRequest(
                url = requestUrl,
                headers = mapOf("User-Agent" to userAgent, "Accept" to "application/json, text/xml, */*"),
                timeoutMs = 12_000L
            )
        )

        if (!response.ok || response.body.isBlank()) {
            // Fallback: search by ID or name
            val searchPage = search(sourceId, sourceItemId, page = 1)
            return@withContext searchPage.items.firstOrNull { it.sourceItemId == sourceItemId || it.title.equals(sourceItemId, ignoreCase = true) }
        }

        val body = response.body.trim()
        if (body.startsWith("<")) {
            val xmlItems = parseXmlCatalog(body, source)
            return@withContext xmlItems.firstOrNull { it.sourceItemId == sourceItemId || it.contentId == sourceItemId }
        }

        try {
            val json = JSONObject(body)
            val listArr = json.optJSONArray("list")
            if (listArr != null && listArr.length() > 0) {
                val obj = listArr.getJSONObject(0)
                val vodId = obj.optString("vod_id").ifBlank { obj.optInt("vod_id").toString() }
                val vodName = obj.optString("vod_name").ifBlank { obj.optString("title") }
                val vodPic = obj.optString("vod_pic").ifBlank { obj.optString("pic") }
                val typeName = obj.optString("type_name").ifBlank { obj.optString("category") }
                val vodYear = obj.optString("vod_year").ifBlank { obj.optString("year") }
                val vodArea = obj.optString("vod_area").ifBlank { obj.optString("area") }
                val vodScore = obj.optString("vod_score").ifBlank { obj.optString("score") }
                val vodRemarks = obj.optString("vod_remarks").ifBlank { obj.optString("remarks") }
                val vodDirector = obj.optString("vod_director").ifBlank { obj.optString("director") }
                val vodActor = obj.optString("vod_actor").ifBlank { obj.optString("actor") }
                val vodContent = stripHtml(obj.optString("vod_content").ifBlank { obj.optString("content") })
                val vodPlayFrom = obj.optString("vod_play_from")
                val vodPlayUrl = obj.optString("vod_play_url")
                val episodes = parseEpisodes(vodPlayFrom, vodPlayUrl)

                return@withContext NativeMovieItem(
                    sourceId = source.sourceId,
                    sourceName = source.name,
                    sourceItemId = vodId,
                    contentId = vodId,
                    title = vodName,
                    poster = vodPic,
                    category = typeName,
                    year = vodYear,
                    region = vodArea,
                    rating = vodScore,
                    updateInfo = vodRemarks,
                    backdrop = vodPic,
                    director = vodDirector,
                    actors = vodActor.split(Regex("[,，/\\s]+")).filter { it.isNotBlank() },
                    description = vodContent,
                    episodes = episodes,
                    rawJson = obj.toString()
                )
            }
        } catch (_: Exception) {}

        null
    }
}
