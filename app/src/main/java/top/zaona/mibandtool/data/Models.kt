package top.zaona.mibandtool.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.text.DecimalFormat
import java.util.Date

data class WatchfaceResource(
    val id: Int,
    val name: String,
    val creator: String,
    val description: String,
    val previewUrl: String,
    val downloads: Int,
    val views: Int,
    val deviceType: String,
    val isShare: Boolean,
    val createdAt: Date?,
    val updatedAt: Date?,
    val isRecommend: Boolean,
    val fileSize: Int,
    val mitanId: String?,
    val mitanType: String?,
) {
    val shortDescription: String
        get() {
            if (description.trim().isEmpty()) return "暂无简介"
            return description.replace("\\n", "\n").trim()
        }

    val fileSizeKb: String
        get() = DecimalFormat("#0").format(fileSize / 1024.0)

    companion object {
        fun fromJson(json: JsonObject): WatchfaceResource {
            val createdAt = parseTimestamp(json.get("createdAt") ?: json.get("createtime"))
            val updatedAt = parseTimestamp(json.get("updatedAt") ?: json.get("updatetime"))
            val nickname = json.string("nickname")
            val username = json.string("username")
            return WatchfaceResource(
                id = json.int("id"),
                name = json.string("name")?.trim().takeIf { !it.isNullOrBlank() } ?: "未命名资源",
                creator = nickname?.trim().takeIf { !it.isNullOrBlank() }
                    ?: username?.trim().takeIf { !it.isNullOrBlank() }
                    ?: "未知创作者",
                description = json.string("desc") ?: "",
                previewUrl = json.string("preview") ?: "",
                downloads = json.int("downloadTimes"),
                views = json.int("views"),
                deviceType = json.string("type") ?: "",
                isShare = json.int("isShare") == 1,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isRecommend = json.int("isRecommend") == 1,
                fileSize = json.int("filesize"),
                mitanId = json.string("mitantid")?.trim(),
                mitanType = json.string("mitantype")?.trim(),
            )
        }
    }
}

data class Comment(
    val id: Int,
    val nickname: String,
    val avatar: String,
    val content: String,
    val time: Date,
    val isDeleted: Boolean,
) {
    companion object {
        fun fromJson(json: JsonObject): Comment {
            return Comment(
                id = json.int("id"),
                nickname = json.string("nickname") ?: "匿名",
                avatar = json.string("avator") ?: "",
                content = json.string("content") ?: "",
                time = Date(json.long("time")),
                isDeleted = json.bool("delflag"),
            )
        }
    }
}

data class DevicePreset(
    val label: String,
    val value: String,
)

val devicePresets = listOf(
    DevicePreset("小米手环10", "o66"),
    DevicePreset("小米手环9", "n66"),
    DevicePreset("小米手环9Pro", "n67"),
    DevicePreset("小米手环8", "mi8"),
    DevicePreset("小米手环8Pro", "mi8pro"),
    DevicePreset("小米手环7", "mi7"),
    DevicePreset("小米手环7Pro", "mi7pro"),
    DevicePreset("小米手表S3/S4Sport", "ws3"),
    DevicePreset("小米手表S4", "o62"),
    DevicePreset("红米手表4", "rw4"),
    DevicePreset("红米手表5", "o65"),
    DevicePreset("红米手表6", "p65"),
)

private fun JsonObject.string(name: String): String? =
    this.get(name)?.takeIf { it.isJsonPrimitive }?.asString

private fun JsonObject.int(name: String): Int = parseInt(this.get(name))

private fun JsonObject.long(name: String): Long = parseLong(this.get(name))

private fun JsonObject.bool(name: String): Boolean =
    this.get(name)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false

private fun parseInt(value: JsonElement?): Int {
    if (value == null || value.isJsonNull) return 0
    return when {
        value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asInt
        else -> value.asString.toIntOrNull() ?: 0
    }
}

private fun parseLong(value: JsonElement?): Long {
    if (value == null || value.isJsonNull) return 0L
    return when {
        value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asLong
        else -> value.asString.toLongOrNull() ?: 0L
    }
}

private fun parseTimestamp(value: JsonElement?): Date? {
    if (value == null || value.isJsonNull) return null
    val millis = parseLong(value)
    return if (millis == 0L) null else Date(millis)
}
