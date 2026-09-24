package io.github.liaong13.dialogueroute.jev

import android.graphics.Bitmap
import android.util.Base64
import io.github.liaong13.dialogueroute.core.Prefs
import java.io.ByteArrayOutputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * The vision route: an OpenAI-compatible `/chat/completions` endpoint that
 * accepts `image_url` content parts. The capture service uses it when the user
 * selects vision-model OCR; the settings page also uses it for a connection test.
 *
 * Reads visionBaseUrl / visionKey / visionModel from [Prefs]. The base URL does
 * not inherit from the reply route (a DeepSeek-style host has no vision
 * endpoint). Saved provider profiles use only the selected provider's key;
 * legacy settings still support their earlier key fallback.
 *
 * Wire format notes that cost real debugging time:
 * - JPEG, not PNG: a screenshot as PNG base64 is several times larger.
 * - `Base64.NO_WRAP`: Android's default inserts newlines, which corrupts the
 *   data URL.
 * - The image part goes BEFORE the text part — DashScope's compatible-mode
 *   rejects the other order.
 */
class VisionClient(private val prefs: Prefs) {
    // 在发起截图请求的线程固定地址、密钥和模型，避免设置更新时跨供应商混用。
    private val endpoint = prefs.visionEndpoint()
    private val key = prefs.effectiveVisionKey()
    private val model = prefs.visionModel
    private val providerId = prefs.visionProviderId

    fun matchesCurrentSelection(): Boolean =
        providerId == prefs.visionProviderId && endpoint == prefs.visionEndpoint() &&
            key == prefs.effectiveVisionKey() && model == prefs.visionModel

    /**
     * Send the visible chat area and get labeled bubbles back as plain text.
     *
     * @param imageBase64Jpeg base64 of a JPEG, without the `data:` prefix.
     */
    fun extractDialog(imageBase64Jpeg: String): String = ask(
        imageBase64Jpeg,
        "你是聊天截图转写助手。只转写图中可见的聊天气泡，忽略状态栏、按钮、时间和输入框。" +
            "若清楚看到会话标题，首行写 `标题：名称`；然后按从上到下的顺序，每个气泡一行，" +
            "格式只能是 `我：正文` 或 `对方：正文`。通常右侧气泡是我、左侧是对方；" +
            "有明确发送方标识时以标识为准，无法确定时不要猜测。" +
            "不要解释，不要输出 Markdown。"
    )

    /** Generic single-question call against the image (used by the settings test). */
    fun ask(imageBase64Jpeg: String, prompt: String): String {
        val url = endpoint
        // Image first, then text: DashScope compatible-mode requires this order.
        val content = JSONArray()
            .put(JSONObject()
                .put("type", "image_url")
                .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$imageBase64Jpeg")))
            .put(JSONObject().put("type", "text").put("text", prompt))
        val messages = JSONArray().put(
            JSONObject().put("role", "user").put("content", content))
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.0)
        val resp = HttpJson.post(url, key, body, Route.VISION, HttpJson.headersFor(url))
        return resp.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content") ?: ""
    }

    companion object {
        /** Bitmap -> JPEG base64 in the exact form [ask] expects. */
        fun encodeJpeg(bitmap: Bitmap, quality: Int = 80): String {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

        /** DeepSeek's official API has no vision model; `image_url` is rejected. */
        fun supportsVision(baseUrl: String): Boolean =
            !baseUrl.contains("api.deepseek.com", ignoreCase = true)
    }
}
