package top.zaona.mibandtool.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiException(
    message: String,
    val statusCode: Int? = null,
) : Exception(message)

class MiBandApi(
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: HttpUrl = "https://www.mibandtool.club:9073/".toHttpUrl(),
) {

    suspend fun fetchHomeResources(
        deviceType: String,
        tag: String = "0",
        page: Int,
        pageSize: Int = 10,
    ): List<WatchfaceResource> {
        val path = "watchface/listbytag/$tag/$page/$pageSize/9999"
        return fetchResourceList(deviceType = deviceType, path = path)
    }

    suspend fun fetchPaidResources(
        deviceType: String,
        page: Int,
        pageSize: Int = 10,
    ): List<WatchfaceResource> {
        val path = "watchface/list/recommendsbytag/$page/$pageSize/9999"
        return fetchResourceList(deviceType = deviceType, path = path)
    }

    suspend fun searchResources(
        keyword: String,
        deviceType: String,
        page: Int,
    ): List<WatchfaceResource> {
        val url = baseUrl.newBuilder()
            .addPathSegments("watchface/searchForPage")
            .addQueryParameter("keyword", keyword)
            .addQueryParameter("page", page.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .addHeader("type", deviceType)
            .build()
        val json = execute(request)
        return parseResourceList(json)
    }

    suspend fun fetchDownloadUrl(
        resourceId: Int,
        deviceType: String,
    ): String {
        val url = baseUrl.newBuilder()
            .addPathSegments("watchface/downloadUsr")
            .addQueryParameter("id", resourceId.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .addHeader("type", deviceType)
            .build()
        val json = execute(request)
        val data = json["data"]?.takeIf { it.isJsonPrimitive }?.asString
        if (data.isNullOrBlank()) {
            throw ApiException("未能获取下载链接，请稍后再试")
        }
        return data!!
    }

    suspend fun fetchComments(
        resourceId: Int,
        deviceType: String,
        page: Int,
    ): List<Comment> {
        val url = baseUrl.newBuilder()
            .addPathSegments("comment/get")
            .addQueryParameter("relationid", resourceId.toString())
            .addQueryParameter("type", "wf")
            .addQueryParameter("page", page.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .addHeader("type", deviceType)
            .build()
        val json = execute(request)
        val data = json["data"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        return data.mapNotNull { element ->
            if (element.isJsonObject) Comment.fromJson(element.asJsonObject) else null
        }
    }

    private suspend fun fetchResourceList(
        deviceType: String,
        path: String,
    ): List<WatchfaceResource> {
        val url = baseUrl.newBuilder()
            .addPathSegments(path)
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("type", deviceType)
            .build()
        val json = execute(request)
        return parseResourceList(json)
    }

    private fun parseResourceList(json: JsonObject): List<WatchfaceResource> {
        val data = json["data"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        return data.mapNotNull { element ->
            if (element.isJsonObject) WatchfaceResource.fromJson(element.asJsonObject) else null
        }
    }

    private suspend fun execute(request: Request): JsonObject = withContext(Dispatchers.IO) {
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw ApiException("请求失败(${response.code})", statusCode = response.code)
        }
        val body = response.body?.string().orEmpty()
        response.close()
        if (body.isBlank()) {
            throw ApiException("响应为空")
        }
        try {
            JsonParser.parseString(body).asJsonObject
        } catch (error: Exception) {
            throw ApiException("解析响应失败: ${error.message ?: error}")
        }
    }
}

object ApiProvider {
    val gson: Gson = Gson()
    val api: MiBandApi = MiBandApi()
}
