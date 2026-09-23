package io.github.liaong13.dialogueroute.capture

import android.content.res.Resources
import android.view.accessibility.AccessibilityNodeInfo
import io.github.liaong13.dialogueroute.core.ChatSnapshot

/**
 * Per-app capture rules. An adapter turns one messaging app's open chat window
 * into a neutral [ChatSnapshot]; everything downstream (Jev judgment, overlay,
 * fill) is app-agnostic.
 *
 * [extract]'s three-way contract (v1.3 B stage — the service depends on it):
 * - `null`            → not in this app's chat window (list screen, moments,
 *                       settings…). The service does nothing at all.
 * - messages empty    → in a chat window, but the tree carries no message text.
 *                       The service may fall back to screenshot + OCR. Each
 *                       adapter names below what proves "we are in a chat".
 * - messages non-empty→ normal capture.
 *
 * The disguised accessibility service (registered as SelectToSpeakService) lets
 * us read the node tree of apps that obfuscate it for normal services (WeChat).
 * Feishu/Lark does not obfuscate, so its adapter reads plain resource-ids.
 */
interface ChatAppAdapter {
    val pkg: String
    fun extract(root: AccessibilityNodeInfo, res: Resources): ChatSnapshot?
}
