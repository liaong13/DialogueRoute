package io.github.liaong13.dialogueroute

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.liaong13.dialogueroute.core.kb.Contact
import io.github.liaong13.dialogueroute.core.kb.KbStore
import io.github.liaong13.dialogueroute.core.kb.Note
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

@Composable
internal fun KnowledgeScreen() {
    val context = LocalContext.current
    val store = remember(context) { KbStore.get(context) }
    var revision by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var editNote by remember { mutableStateOf<Note?>(null) }
    var editContact by remember { mutableStateOf<Contact?>(null) }
    var editor by remember { mutableStateOf("") }
    var confirmTitle by remember { mutableStateOf("") }
    var confirmMessage by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var alwaysOn by remember { mutableStateOf(false) }
    var aliases by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }

    fun notice(message: String) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    fun openNote(note: Note?) {
        editNote = note
        title = note?.title.orEmpty()
        body = note?.content.orEmpty()
        tags = note?.tags?.joinToString("，").orEmpty()
        enabled = note?.enabled ?: true
        alwaysOn = note?.alwaysOn ?: false
        editor = "note"
    }
    fun openContact(contact: Contact?) {
        editContact = contact
        title = contact?.name.orEmpty()
        body = contact?.notes.orEmpty()
        aliases = contact?.aliases?.joinToString("\n").orEmpty()
        relationship = contact?.relationship.orEmpty()
        editor = "contact"
    }
    fun confirm(heading: String, message: String, action: () -> Unit) {
        confirmTitle = heading
        confirmMessage = message
        confirmAction = action
    }

    val notes = remember(revision) { store.notes().sortedByDescending { it.updatedAt } }
    val contacts = remember(revision) { store.contacts().sortedByDescending { it.updatedAt } }
    val dark = LocalAppDarkTheme.current
    val noteTint = if (dark) Color(0xFFAFCBFF) else Color(0xFF3972BE)
    val contactTint = if (dark) Color(0xFF88DED9) else Color(0xFF23827D)
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeading("知识库", "记下重要的小事，让回应更贴近你。") }
        item {
            GlassTabs(tabs = listOf("笔记 ${notes.size}", "联系人 ${contacts.size}"),
                selectedTabIndex = page, onTabSelected = { page = it })
        }
        if (page == 0) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        PrimaryAction("新建笔记", icon = AppIcons.Edit) { openNote(null) }
                    }
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        SecondaryAction("从文本导入", icon = AppIcons.Import) {
                            body = ""; editor = "import"
                        }
                    }
                }
            }
            if (notes.isEmpty()) item {
                EmptyState("从一件小事开始",
                    "记录习惯、忌口或约定。启用的笔记会按上下文参与分析，常驻笔记则每次都会带上。",
                    icon = AppIcons.KnowledgeSelected, actionLabel = "写下第一条笔记") { openNote(null) }
            }
            items(notes, key = { "note:${it.id}" }) { note ->
                GlassCard(modifier = Modifier.animateItem(), padding = PaddingValues(20.dp),
                    onClick = { openNote(note) }) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccentIconTile(AppIcons.Notes, noteTint)
                        Text(note.title.ifBlank { "未命名笔记" }, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (note.alwaysOn) StatusBadge("常驻", highlighted = note.enabled)
                        Icon(AppIcons.ChevronForward, contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(note.content.replace('\n', ' '), modifier = Modifier.padding(start = 56.dp),
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (note.tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(note.tags.joinToString(" · "), modifier = Modifier.padding(start = 56.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    RoundedSwitchPreference(title = "参与分析", summary = "点击卡片编辑笔记", checked = note.enabled,
                        onCheckedChange = {
                            if (store.saveNote(note.copy(enabled = it))) revision++ else notice("保存失败")
                        })
                }
            }
        } else {
            item { PrimaryAction("新建联系人", icon = AppIcons.Add) { openContact(null) } }
            if (contacts.isEmpty()) item {
                EmptyState("让每段对话都有背景",
                    "添加联系人，记录你们的关系与相处细节。也可在聊天中长按悬浮球保存当前会话。",
                    icon = AppIcons.ContactsBook, actionLabel = "添加第一位联系人") {
                    openContact(null)
                }
            }
            items(contacts, key = { "contact:${it.id}" }) { contact ->
                GlassCard(modifier = Modifier.animateItem(), padding = PaddingValues(20.dp),
                    onClick = { openContact(contact) }) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccentIconTile(AppIcons.ContactsCircle, contactTint)
                        Text(contact.name, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface)
                        Icon(AppIcons.ChevronForward, contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(contact.relationship.ifBlank { "尚未填写关系" },
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (contact.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(contact.notes, modifier = Modifier.padding(start = 56.dp),
                            maxLines = 3, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val count = remember(revision, contact.id) { store.logSize(contact.id) }
                    Spacer(Modifier.height(12.dp))
                    Text("$count 条历史 · 点击编辑资料", modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(AppIcons.Info, contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("资料保存在本机；选中的内容会随分析发送至你配置的模型服务。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    AppDialog(show = editor.isNotEmpty(),
        title = when (editor) {
            "note" -> if (editNote == null) "新建笔记" else "编辑笔记"
            "contact" -> if (editContact == null) "新建联系人" else "编辑联系人"
            else -> "从文本导入"
        }, onDismissRequest = { editor = "" }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (editor) {
                "note" -> {
                    UiField("标题", title, { title = it })
                    UiField("正文", body, { body = it }, singleLine = false)
                    UiField("标签（逗号分隔）", tags, { tags = it })
                    RoundedSwitchPreference(title = "每次分析都带上", checked = alwaysOn,
                        onCheckedChange = { alwaysOn = it })
                    RoundedSwitchPreference(title = "启用", checked = enabled,
                        onCheckedChange = { enabled = it })
                    PrimaryAction("保存") {
                        if (title.isBlank() && body.isBlank()) notice("标题和正文不能都空着")
                        else {
                            val ok = store.saveNote(Note(editNote?.id ?: KbStore.newId(),
                                title.trim(), body.trim(), splitKnowledgeList(tags), alwaysOn, enabled))
                            if (ok) { revision++; editor = "" } else notice("保存失败")
                        }
                    }
                }
                "contact" -> {
                    UiField("姓名", title, { title = it })
                    UiField("别名（每行一个）", aliases, { aliases = it }, singleLine = false)
                    UiField("关系", relationship, { relationship = it })
                    UiField("备注", body, { body = it }, singleLine = false)
                    PrimaryAction("保存") {
                        if (title.isBlank()) notice("名字不能空")
                        else {
                            val ok = store.saveContact(Contact(editContact?.id ?: KbStore.newId(),
                                title.trim(), aliases.lines().map { it.trim() }.filter { it.isNotEmpty() },
                                editContact?.apps ?: emptyList(), relationship.trim(), body.trim(),
                                editContact?.autoSummary.orEmpty()))
                            if (ok) { revision++; editor = "" } else notice("保存失败")
                        }
                    }
                }
                "import" -> {
                    Text("按空行分段，每段第一行是标题，其余是正文。",
                        color = MaterialTheme.colorScheme.onSurface)
                    UiField("导入文本", body, { body = it }, singleLine = false)
                    PrimaryAction("导入") {
                        var count = 0
                        body.split(Regex("\\r?\\n[ \\t]*\\r?\\n")).forEach { chunk ->
                            val lines = chunk.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            if (lines.isNotEmpty() && store.saveNote(Note(KbStore.newId(),
                                    lines.first(), lines.drop(1).joinToString("\n")))) count++
                        }
                        notice(if (count == 0) "没有导入内容" else "已导入 $count 条")
                        revision++
                        editor = ""
                    }
                }
            }
            if (editor == "note") editNote?.let { note ->
                RoundedPreference(title = "删除笔记", summary = "删除后无法恢复", onClick = {
                    editor = ""
                    confirm("删除笔记", "删除「${note.title}」？此操作无法恢复。") {
                        if (store.deleteNote(note.id)) revision++ else notice("删除失败")
                    }
                })
            }
            if (editor == "contact") editContact?.let { contact ->
                val count = remember(revision, contact.id) { store.logSize(contact.id) }
                if (count > 0) RoundedPreference(title = "清空聊天历史", summary = "$count 条历史，联系人会保留",
                    onClick = {
                        editor = ""
                        confirm("清空历史", "删除「${contact.name}」的 $count 条历史？联系人会保留。") {
                            store.clearLog(contact.id); revision++
                        }
                    })
                RoundedPreference(title = "删除联系人", summary = "同时删除关联的聊天历史", onClick = {
                    editor = ""
                    confirm("删除联系人", "删除「${contact.name}」及其历史？此操作无法恢复。") {
                        if (store.deleteContact(contact.id)) revision++ else notice("删除失败")
                    }
                })
            }
            SecondaryAction("取消") { editor = "" }
        }
    }
    AppDialog(show = confirmAction != null, title = confirmTitle,
        summary = confirmMessage, onDismissRequest = { confirmAction = null }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryAction("确认") { confirmAction?.invoke(); confirmAction = null }
            SecondaryAction("取消") { confirmAction = null }
        }
    }
}

private fun splitKnowledgeList(raw: String) = raw.split(",", "，", "、")
    .map { it.trim() }.filter { it.isNotEmpty() }
