package com.mazmodzzz.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class AppInfo(val pkg: String, val label: String, val icon: ImageBitmap)
data class EditTarget(val app: AppInfo, val existing: CloneEntry?)

fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { it.activityInfo.packageName }
        .distinct()
        .filter { it != ctx.packageName }
        .mapNotNull { p ->
            try {
                val ai = pm.getApplicationInfo(p, 0)
                AppInfo(
                    p,
                    pm.getApplicationLabel(ai).toString(),
                    pm.getApplicationIcon(ai).toBitmap(96, 96).asImageBitmap()
                )
            } catch (e: Exception) {
                null
            }
        }
        .sortedBy { it.label.lowercase() }
}

fun appInfoFor(ctx: Context, pkg: String): AppInfo? {
    return try {
        val pm = ctx.packageManager
        val ai = pm.getApplicationInfo(pkg, 0)
        AppInfo(pkg, pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai).toBitmap(96, 96).asImageBitmap())
    } catch (e: Exception) {
        null
    }
}

@Composable
fun MazApp() {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf(CloneStore.load(ctx)) }
    var picking by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    fun persist(list: List<CloneEntry>) {
        entries = list
        CloneStore.save(ctx, list)
    }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

    fun launch(e: CloneEntry) {
        val engine: CloneEngine? = if (e.mode == "virtual") Engines.virtual else ShortcutEngine
        val ok = try { engine?.launch(ctx, e) == true } catch (t: Throwable) { false }
        if (!ok) toast("Couldn't launch ${e.label}. Is the app still installed?")
    }

    BackHandler(enabled = editing != null || picking) {
        if (editing != null) editing = null else picking = false
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        Box(
            Modifier.size(340.dp).offset((-130).dp, (-110).dp)
                .background(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.22f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier.size(320.dp).align(Alignment.BottomEnd).offset(110.dp, 90.dp)
                .background(Brush.radialGradient(listOf(Violet.copy(alpha = 0.20f), Color.Transparent)), CircleShape)
        )

        val ed = editing
        when {
            ed != null -> EditScreen(
                target = ed,
                onBack = { editing = null },
                onSave = { entry, bmp ->
                    val f = CloneStore.iconFile(ctx, entry.id)
                    if (bmp != null) {
                        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } else if (f.exists()) {
                        f.delete()
                    }
                    val exists = entries.any { it.id == entry.id }
                    persist(if (exists) entries.map { if (it.id == entry.id) entry else it } else entries + entry)
                    editing = null
                    picking = false
                }
            )
            picking -> PickScreen(
                onBack = { picking = false },
                onPick = { app -> editing = EditTarget(app, null) }
            )
            else -> HomeScreen(
                entries = entries,
                onAdd = { picking = true },
                onLaunch = { launch(it) },
                onPin = { e ->
                    val ok = try { ShortcutEngine.pin(ctx, e) } catch (t: Throwable) { false }
                    if (!ok) toast("Shortcut not allowed. On Xiaomi: Settings > Apps > Permissions > Home screen shortcuts > maz_modzzz > Allow.")
                },
                onEdit = { e ->
                    val info = appInfoFor(ctx, e.pkg)
                    if (info != null) editing = EditTarget(info, e) else toast("That app is no longer installed.")
                },
                onDelete = { e ->
                    CloneStore.iconFile(ctx, e.id).delete()
                    persist(entries.filter { it.id != e.id })
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    entries: List<CloneEntry>,
    onAdd: () -> Unit,
    onLaunch: (CloneEntry) -> Unit,
    onPin: (CloneEntry) -> Unit,
    onEdit: (CloneEntry) -> Unit,
    onDelete: (CloneEntry) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Text(
                "maz_modzzz",
                style = TextStyle(brush = BrandBrush, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            )
            Text(
                "${entries.size} launcher${if (entries.size == 1) "" else "s"} ready",
                color = Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(18.dp))

            if (entries.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(120.dp).clip(CircleShape)
                            .border(2.dp, BrandBrush, CircleShape)
                            .background(Panel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Cyan, modifier = Modifier.size(52.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Nothing here yet", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap Add app, pick an app, and give it your own name and logo.",
                        color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(entries, key = { it.id }) { e ->
                        CloneCard(e, { onLaunch(e) }, { onPin(e) }, { onEdit(e) }, { onDelete(e) })
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            containerColor = Cyan,
            contentColor = Ink,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add app", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}

@Composable
fun CloneCard(
    e: CloneEntry,
    onLaunch: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val icon = remember(e) { IconMaker.forEntry(ctx, e).asImageBitmap() }
    val accent = Color(e.accent)
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier.fillMaxWidth().clip(shape).background(Panel)
            .border(1.dp, accent.copy(alpha = 0.35f), shape).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(icon, null, Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.label, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(e.pkg, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Chip(if (e.mode == "virtual") "VIRTUAL" else "SHORTCUT", accent)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onLaunch,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink)
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("Launch", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onPin) { Icon(Icons.Filled.Home, "Add to home screen", tint = Muted) }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit", tint = Muted) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete", tint = Muted) }
        }
    }
}

@Composable
fun PickScreen(onBack: () -> Unit, onPick: (AppInfo) -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.Default) { loadApps(ctx) } }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text("Pick an app", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Cyan) }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                items(list.filter { it.label.contains(q, ignoreCase = true) }, key = { it.pkg }) { a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .clickable { onPick(a) }.padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(a.icon, null, Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(a.label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(a.pkg, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(target: EditTarget, onBack: () -> Unit, onSave: (CloneEntry, Bitmap?) -> Unit) {
    val ctx = LocalContext.current
    val ex = target.existing
    var name by remember { mutableStateOf(ex?.name ?: target.app.label) }
    var tag by remember { mutableStateOf(ex?.tag ?: "d0") }
    var accent by remember { mutableStateOf(ex?.accent ?: Accents[0]) }
    var badge by remember { mutableStateOf(ex?.badge ?: true) }
    var mode by remember { mutableStateOf(ex?.mode ?: "shortcut") }
    var custom by remember {
        mutableStateOf<Bitmap?>(if (ex != null && ex.customIcon) IconMaker.loadCustom(ctx, ex.id) else null)
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = IconMaker.decodeUri(ctx, uri)
            if (bmp != null) custom = bmp else Toast.makeText(ctx, "Couldn't read that image", Toast.LENGTH_SHORT).show()
        }
    }

    val preview = remember(custom, tag, accent, badge) {
        IconMaker.render(custom ?: IconMaker.appIcon(ctx, target.app.pkg), tag, accent, badge).asImageBitmap()
    }
    val finalLabel = if (tag.isBlank()) name else "[${tag.trim()}] ${name.trim()}"
    val virtualAvailable = Engines.virtual != null

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text(if (ex == null) "New launcher" else "Edit launcher", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Panel)
                .border(1.dp, Color(accent).copy(alpha = 0.4f), RoundedCornerShape(28.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(preview, null, Modifier.size(104.dp).clip(RoundedCornerShape(26.dp)))
            Spacer(Modifier.height(12.dp))
            Text(finalLabel, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("real app: ${target.app.label}", color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = tag, onValueChange = { tag = it.take(3) },
                label = { Text("Tag") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.width(96.dp)
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("Accent colour", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Accents.forEach { c ->
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(Color(c))
                        .border(if (c == accent) 3.dp else 0.dp, Color.White, CircleShape)
                        .clickable { accent = c }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Show tag badge on icon", Modifier.weight(1f))
            Switch(checked = badge, onCheckedChange = { badge = it })
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { pickImage.launch("image/*") },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)
            ) { Text(if (custom == null) "Choose logo" else "Change logo") }
            OutlinedButton(
                onClick = { custom = null },
                enabled = custom != null,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)
            ) { Text("Reset logo") }
        }

        Spacer(Modifier.height(18.dp))
        Text("How to open", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = mode == "shortcut", onClick = { mode = "shortcut" }, label = { Text("Shortcut") })
            FilterChip(
                selected = mode == "virtual", onClick = { mode = "virtual" },
                enabled = virtualAvailable, label = { Text("Virtual") }
            )
        }
        Text(
            if (virtualAvailable) "Virtual runs the app inside this app's sandbox."
            else "Virtual is off: the virtual engine isn't built into this APK. Shortcut opens the real app.",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp))
                .background(BrandBrush)
                .clickable {
                    val id = ex?.id ?: UUID.randomUUID().toString().take(8)
                    onSave(
                        CloneEntry(
                            id = id, pkg = target.app.pkg,
                            name = name.trim().ifBlank { target.app.label },
                            tag = tag.trim(), customIcon = custom != null,
                            accent = accent, badge = badge, mode = mode
                        ),
                        custom
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Save", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(30.dp))
    }
}
