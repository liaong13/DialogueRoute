package io.github.liaong13.dialogueroute.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One saved connection. Models reference its id, so an account can serve several routes. */
data class ModelProvider(
    val id: String = UUID.randomUUID().toString(),
    val kind: String,
    val name: String,
    val baseUrl: String,
    val key: String,
) {
    val canJudge: Boolean get() = kind == OPENROUTER || kind == TYPESAFE || kind == CUSTOM_JUDGE
    val canChat: Boolean get() = kind != TYPESAFE && kind != CUSTOM_JUDGE
    val canVision: Boolean get() = canChat && kind != DEEPSEEK

    fun judgeBase(): String = when (kind) {
        OPENROUTER -> Prefs.DEFAULT_JUDGE_BASE_OPENROUTER
        TYPESAFE -> Prefs.DEFAULT_JUDGE_BASE_TYPESAFE
        else -> baseUrl.trim()
    }

    fun chatBase(): String = when (kind) {
        OPENROUTER -> Prefs.DEFAULT_REPLY_BASE
        DEEPSEEK -> Prefs.DEEPSEEK_BASE
        DASHSCOPE -> Prefs.DASHSCOPE_BASE
        else -> baseUrl.trim()
    }

    fun judgeMode(): String = when (kind) {
        OPENROUTER -> Prefs.PROVIDER_OPENROUTER
        TYPESAFE -> Prefs.PROVIDER_TYPESAFE
        else -> Prefs.PROVIDER_CUSTOM
    }

    companion object {
        const val OPENROUTER = "openrouter"
        const val TYPESAFE = "typesafe"
        const val DEEPSEEK = "deepseek"
        const val DASHSCOPE = "dashscope"
        const val CUSTOM_CHAT = "custom_chat"
        const val CUSTOM_JUDGE = "custom_judge"

        fun label(kind: String): String = when (kind) {
            OPENROUTER -> "OpenRouter"
            TYPESAFE -> "TypeSafe 直连"
            DEEPSEEK -> "DeepSeek 官方"
            DASHSCOPE -> "通义兼容"
            CUSTOM_JUDGE -> "自定义判断接口"
            else -> "自定义兼容接口"
        }

        fun create(kind: String): ModelProvider = ModelProvider(
            kind = kind, name = label(kind), baseUrl = "", key = "")

        fun decode(raw: String): List<ModelProvider> = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id")
                val kind = item.optString("kind")
                if (id.isBlank() || kind !in setOf(OPENROUTER, TYPESAFE, DEEPSEEK,
                        DASHSCOPE, CUSTOM_CHAT, CUSTOM_JUDGE)) return@mapNotNull null
                ModelProvider(id, kind, item.optString("name"),
                    item.optString("baseUrl"), item.optString("key"))
            }.distinctBy { it.id }
        }.getOrDefault(emptyList())

        fun encode(providers: List<ModelProvider>): String = JSONArray().apply {
            providers.forEach { provider -> put(JSONObject()
                .put("id", provider.id).put("kind", provider.kind)
                .put("name", provider.name).put("baseUrl", provider.baseUrl)
                .put("key", provider.key)) }
        }.toString()

        /** Convert existing route settings only for the settings editor; no network calls. */
        fun fromLegacy(prefs: Prefs): ProviderSelection {
            val providers = mutableListOf<ModelProvider>()
            fun add(kind: String, base: String, key: String): String {
                val storedBase = if (kind == CUSTOM_CHAT || kind == CUSTOM_JUDGE) base else ""
                val existing = providers.firstOrNull { it.kind == kind &&
                    it.baseUrl.trimEnd('/') == storedBase.trimEnd('/') && it.key == key }
                if (existing != null) return existing.id
                val defaultName = label(kind)
                val name = if (providers.any { it.name == defaultName })
                    "$defaultName ${providers.size + 1}" else defaultName
                return ModelProvider(kind = kind, name = name, baseUrl = storedBase, key = key)
                    .also { providers += it }.id
            }
            val judgeKind = when {
                prefs.judgeProvider == Prefs.PROVIDER_OPENROUTER &&
                    prefs.judgeBaseUrl.trimEnd('/') == Prefs.DEFAULT_JUDGE_BASE_OPENROUTER -> OPENROUTER
                prefs.judgeProvider == Prefs.PROVIDER_TYPESAFE &&
                    prefs.judgeBaseUrl.trimEnd('/') == Prefs.DEFAULT_JUDGE_BASE_TYPESAFE -> TYPESAFE
                else -> CUSTOM_JUDGE
            }
            val judge = add(judgeKind, prefs.judgeBaseUrl, prefs.judgeKey)
            fun chatKind(base: String): String = when (base.trimEnd('/')) {
                Prefs.DEFAULT_REPLY_BASE -> OPENROUTER
                Prefs.DEEPSEEK_BASE -> DEEPSEEK
                Prefs.DASHSCOPE_BASE -> DASHSCOPE
                else -> CUSTOM_CHAT
            }
            val reply = add(chatKind(prefs.replyBaseUrl), prefs.replyBaseUrl,
                prefs.effectiveReplyKey())
            val visionBase = prefs.visionBaseUrl.ifBlank { Prefs.DEFAULT_VISION_BASE }
            val vision = add(chatKind(visionBase), visionBase, prefs.effectiveVisionKey())
            return ProviderSelection(providers, judge, reply, vision)
        }
    }
}

data class ProviderSelection(
    val providers: List<ModelProvider>,
    val judgeId: String,
    val replyId: String,
    val visionId: String,
)
