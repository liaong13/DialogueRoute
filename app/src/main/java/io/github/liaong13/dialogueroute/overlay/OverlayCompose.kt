package io.github.liaong13.dialogueroute.overlay

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.liaong13.dialogueroute.R
import io.github.liaong13.dialogueroute.core.Analysis
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.RankedReply
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun OverlayBubble(
    dangerLevel: Double?,
    onTouch: (MotionEvent) -> Boolean,
    onOpen: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        Modifier.size(48.dp)
            .semantics {
                role = Role.Button
                contentDescription = "对话攻略悬浮球"
                onClick("展开分析") { onOpen(); true }
                onLongClick("打开菜单") { onMenu(); true }
            }
            .pointerInteropFilter(onTouchEvent = onTouch),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(44.dp).background(MiuixTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            RouteIcon(28.dp, MiuixTheme.colorScheme.onPrimary)
        }
        dangerLevel?.let { score ->
            Box(
                Modifier.size(10.dp).align(Alignment.TopEnd)
                    .background(dangerColor(score.roundToInt()), CircleShape)
                    .border(2.dp, MiuixTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun OverlayPanel(
    analysis: Analysis?,
    replies: List<RankedReply>?,
    judging: Boolean,
    generatingReplies: Boolean,
    error: String?,
    replyError: String?,
    notes: Int,
    history: Int,
    note: String?,
    preview: ChatSnapshot?,
    menuVisible: Boolean,
    panelWidth: Dp,
    contentHeight: Dp,
    opacity: Float,
    onClose: () -> Unit,
    onPanelTouch: (MotionEvent) -> Boolean,
    onSizeChanged: (Int, Int) -> Unit,
    onMenu: () -> Unit,
    onSettings: () -> Unit,
    onManualAnalyze: () -> Unit,
    onCopy: (String) -> Unit,
    onFill: (String) -> Unit,
    canFill: Boolean,
    onSaveContact: (() -> Unit)?,
    onOcrCapture: (() -> Unit)?,
    onInspectCapture: (() -> Unit)?,
    onHide: () -> Unit,
    onDismissPreview: () -> Unit
) {
    val colors = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(panelWidth)
            .shadow(10.dp, shape)
            .clip(shape)
            .background(colors.surface.copy(alpha = opacity))
            .border(0.8.dp, colors.primary.copy(alpha = 0.22f), shape)
            .onSizeChanged { onSizeChanged(it.width, it.height) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 顶栏 Header
        Row(
            Modifier.fillMaxWidth().heightIn(min = 38.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f).heightIn(min = 38.dp)
                    .pointerInteropFilter(onTouchEvent = onPanelTouch)
                    .semantics { contentDescription = "按住标题移动悬浮窗" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RouteIcon(20.dp, colors.primary)
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        if (menuVisible) "快捷操作" else "对话攻略",
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "按住移动",
                        color = colors.onSurfaceVariantSummary.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                }
            }
            HeaderAction(
                if (menuVisible) "返回分析" else "打开菜单",
                if (menuVisible) HeaderSymbol.BACK else HeaderSymbol.MENU,
                onMenu
            )
            HeaderAction("收起为悬浮球", HeaderSymbol.MINIMIZE, onClose)
        }

        // 内容区
        Column(
            Modifier.fillMaxWidth()
                .heightIn(max = contentHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (menuVisible) {
                onOcrCapture?.let { OverlayMenuItem("截", "截屏识别一次", it) }
                onInspectCapture?.let { OverlayMenuItem("核", "核对本次采集", it) }
                onSaveContact?.let { OverlayMenuItem("存", "存为联系人", it) }
                OverlayMenuItem("设", "打开设置", onSettings)
                Box(
                    Modifier.fillMaxWidth().height(1.dp)
                        .background(colors.onSurface.copy(alpha = 0.12f))
                )
                OverlayMenuItem("隐", "隐藏助手（本次）", onHide)
            } else if (preview != null) {
                OverlayHint("Xposed 本次采集 · ${preview.messages.size} 条（展示最近 6 条）")
                if (preview.messages.isEmpty()) OverlayHint("本次采集没有正文")
                preview.messages.takeLast(6).forEach { message ->
                    Text(
                        "${if (message.side == "me") "我" else "对方"}：${message.text}",
                        color = colors.onSurface,
                        fontSize = 12.sp
                    )
                }
                preview.note?.takeIf { it.isNotBlank() }?.let { OverlayHint(it) }
                OverlayAction("返回分析", onDismissPreview, Modifier.fillMaxWidth(), primary = true)
            } else {
                if (analysis != null || replies != null || judging || generatingReplies || error != null) {
                    OverlayHint(
                        if (notes == 0 && history == 0) "本次未用知识库"
                        else "知识库 $notes 条 · 历史 $history 条"
                    )
                    note?.takeIf { it.isNotBlank() }?.let { OverlayHint(it) }
                } else OverlayHint("就绪 · 可手动分析当前对话")

                if (judging) OverlayHint("正在深入分析对方心理…")
                error?.let { Text("判断 / 采集出错：$it", color = colors.error, fontSize = 12.sp) }

                // 研判信息卡片化
                analysis?.let { JudgmentCard(it) }

                if (analysis != null || replies != null || judging || generatingReplies || replyError != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "回复建议",
                            color = colors.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (replies != null) "${replies.size} 条候选" else "生成中",
                            color = colors.onSurfaceVariantSummary,
                            fontSize = 10.sp
                        )
                    }
                    if (generatingReplies) OverlayHint("高情商回复生成中…")
                    replyError?.let { Text("回复接口出错：$it", color = colors.error, fontSize = 12.sp) }
                    replies?.forEachIndexed { index, reply ->
                        ReplyCard(index + 1, reply, opacity, onCopy, onFill, canFill)
                    }
                    if (replies?.isEmpty() == true && replyError == null) OverlayHint("（未生成候选回复）")
                }
            }
        }

        // 底部操作区
        if (!menuVisible) {
            Column(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OverlayAction(
                    label = if (analysis == null && error == null && replies == null) "分析当前对话" else "重新分析",
                    action = onManualAnalyze,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !judging && !generatingReplies,
                    primary = true
                )
                if (analysis != null || replies != null) {
                    Text(
                        "填入后由您手动确认发送",
                        color = colors.onSurfaceVariantSummary.copy(alpha = 0.7f),
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}

/** 研判卡片组件 */
@Composable
private fun JudgmentCard(analysis: Analysis) {
    val colors = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(colors.primaryContainer.copy(alpha = 0.28f))
            .border(0.6.dp, colors.primary.copy(alpha = 0.20f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // 头部：危险等级与把握度
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            analysis.dangerLevel?.let {
                val level = it.score.roundToInt()
                Box(
                    Modifier.size(6.dp).background(dangerColor(level), CircleShape)
                )
                Text(
                    "危险 $level/${it.maxLevel} · ${dangerWord(level)}",
                    color = dangerColor(level),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.weight(1f))
            analysis.trueIntent?.let {
                Text(
                    "把握 ${(it.confidence * 100).roundToInt()}%",
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp
                )
            }
        }

        // 真意图
        analysis.trueIntent?.let {
            Text(
                INTENT[it.choice] ?: it.choice,
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )
        }

        // 需求与对策
        val bits = buildList {
            analysis.sheNeeds?.let { add("要${NEEDS[it.choice] ?: it.choice}") }
            analysis.bestAction?.let { add(ACTION[it.choice] ?: it.choice) }
            analysis.shouldReplyNow?.let { add(if (it >= 0.5) "可给实质" else "先别给实质") }
        }
        if (bits.isNotEmpty()) {
            Text(
                bits.joinToString(" · "),
                color = colors.onSurfaceVariantSummary,
                fontSize = 11.sp
            )
        }

        // 紧张缓解状态
        analysis.tensionResolved?.let {
            if (it >= 0.7) {
                Text(
                    "✓ 紧张氛围已缓解",
                    color = dangerColor(0),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/** 回复选项卡片组件 */
@Composable
private fun ReplyCard(
    rank: Int,
    reply: RankedReply,
    opacity: Float,
    onCopy: (String) -> Unit,
    onFill: (String) -> Unit,
    canFill: Boolean
) {
    val colors = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val isTopRank = rank == 1

    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(
                if (isTopRank) colors.primary.copy(alpha = 0.12f)
                else colors.surfaceVariant.copy(alpha = (opacity + 0.08f).coerceAtMost(1f))
            )
            .border(
                if (isTopRank) 1.dp else 0.5.dp,
                if (isTopRank) colors.primary.copy(alpha = 0.38f) else colors.primary.copy(alpha = 0.12f),
                shape
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 回复文本
        Text(
            reply.text,
            color = colors.onSurface,
            fontSize = 12.5.sp,
            lineHeight = 17.5.sp,
            fontWeight = if (isTopRank) FontWeight.Medium else FontWeight.Normal
        )

        // 底部操作栏：序号/置信度 + [复制] + [填入]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 推荐 Badge / 序号
            Box(
                Modifier.clip(RoundedCornerShape(4.dp))
                    .background(colors.primary.copy(alpha = if (isTopRank) 0.18f else 0.08f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    if (isTopRank) "推荐 · #1 · ${(reply.prob * 100).roundToInt()}%"
                    else "#$rank · ${(reply.prob * 100).roundToInt()}%",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.weight(1f))

            // 复制胶囊按钮
            ReplyChipButton(
                label = "复制",
                onClick = { onCopy(reply.text) },
                primary = false,
                enabled = true
            )

            // 填入胶囊按钮
            ReplyChipButton(
                label = "填入",
                onClick = { onFill(reply.text) },
                primary = true,
                enabled = canFill
            )
        }
    }
}

/** 胶囊按钮 */
@Composable
private fun ReplyChipButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    enabled: Boolean
) {
    val colors = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.height(26.dp)
            .clip(shape)
            .background(
                if (primary && enabled) colors.primary
                else colors.onSurface.copy(alpha = 0.06f)
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (primary && enabled) colors.onPrimary else colors.onSurface.copy(alpha = if (enabled) 0.85f else 0.4f),
            fontSize = 11.sp,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun OverlayAction(
    label: String,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false
) {
    val colors = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.height(34.dp)
            .clip(shape)
            .background(
                if (primary && enabled) colors.primary
                else colors.primary.copy(alpha = 0.08f)
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = action),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (primary && enabled) colors.onPrimary else colors.primary.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OverlayMenuItem(symbol: String, label: String, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                .background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                symbol,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Text(label, color = colors.onSurface, fontSize = 12.sp)
    }
}

@Composable
private fun OverlayHint(text: String) {
    Text(
        text,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontSize = 10.5.sp
    )
}

@Composable
private fun RouteIcon(size: Dp, color: Color) {
    Image(
        painterResource(R.drawable.ic_overlay_route),
        contentDescription = null,
        modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(color)
    )
}

private enum class HeaderSymbol { MENU, BACK, MINIMIZE }

@Composable
private fun HeaderAction(label: String, symbol: HeaderSymbol, action: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = action),
        contentAlignment = Alignment.Center
    ) {
        val color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        Canvas(Modifier.size(16.dp)) {
            val stroke = 2.dp.toPx()
            when (symbol) {
                HeaderSymbol.MENU -> for (fraction in listOf(0.24f, 0.5f, 0.76f)) {
                    drawCircle(
                        color, radius = 1.2.dp.toPx(),
                        center = Offset(size.width * fraction, size.height * 0.5f)
                    )
                }
                HeaderSymbol.BACK -> {
                    drawLine(
                        color, Offset(size.width * 0.60f, size.height * 0.24f),
                        Offset(size.width * 0.34f, size.height * 0.5f), stroke, cap = StrokeCap.Round
                    )
                    drawLine(
                        color, Offset(size.width * 0.34f, size.height * 0.5f),
                        Offset(size.width * 0.60f, size.height * 0.76f), stroke, cap = StrokeCap.Round
                    )
                }
                HeaderSymbol.MINIMIZE -> drawLine(
                    color,
                    Offset(size.width * 0.22f, size.height * 0.5f),
                    Offset(size.width * 0.78f, size.height * 0.5f), stroke, cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun dangerColor(level: Int): Color = when {
    level >= 6 -> MiuixTheme.colorScheme.error
    level >= 3 -> Color(0xFFD99B26)
    else -> Color(0xFF38A169)
}

private fun dangerWord(level: Int): String = when {
    level >= 8 -> "极度危险"
    level >= 6 -> "偏危险"
    level >= 3 -> "需留意"
    else -> "安全"
}

private val INTENT = mapOf(
    "confirm_you_care" to "确认你在不在乎", "vent_anger" to "发泄情绪",
    "request_action" to "要你办事", "seek_explanation" to "要个解释",
    "casual_chat" to "随便聊聊", "close_topic" to "事情过去了"
)
private val NEEDS = mapOf(
    "apology" to "道歉", "action" to "具体行动", "explanation" to "解释",
    "care" to "你的在乎", "nothing" to "（不用做什么）"
)
private val ACTION = mapOf(
    "check_history" to "翻聊天记录", "apologize" to "先道歉", "give_commitment" to "给承诺",
    "explain" to "解释清楚", "acknowledge" to "接住情绪", "say_less" to "少说两句",
    "make_plan" to "定个安排"
)
