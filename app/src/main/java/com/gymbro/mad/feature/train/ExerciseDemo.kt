package com.gymbro.mad.feature.train

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

private const val DEMO_CDN = "https://cdn.jsdelivr.net/gh/yuhonas/free-exercise-db@main/exercises/"
private const val DEMO_RAW = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/master/exercises/"

internal data class ExerciseDemoData(
    val exerciseName: String,
    val demoName: String,
    val imagePaths: List<String>,
)

private object ExerciseDemoCatalog {
    private var cached: Map<String, ExerciseDemoData>? = null

    @Synchronized
    fun load(context: android.content.Context): Map<String, ExerciseDemoData> {
        cached?.let { return it }
        val json = context.assets.open("exercise-demos.json").bufferedReader().use { it.readText().trimStart('\uFEFF') }
        val array = JSONArray(json)
        val records = buildMap {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val paths = item.getJSONArray("images")
                put(
                    item.getString("name"),
                    ExerciseDemoData(
                        exerciseName = item.getString("name"),
                        demoName = item.getString("demoName"),
                        imagePaths = (0 until paths.length()).map(paths::getString),
                    ),
                )
            }
        }
        cached = records
        return records
    }
}

private object ExerciseDemoImages {
    private val bitmaps = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun get(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val key = DEMO_CDN + path
        bitmaps.get(key) ?: listOf(DEMO_CDN, DEMO_RAW).firstNotNullOfOrNull { host ->
            runCatching {
                val connection = URL(host + path).openConnection().apply {
                    connectTimeout = 5_000
                    readTimeout = 8_000
                }
                connection.getInputStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()?.also { bitmaps.put(key, it) }
        }
    }
}

@Composable
internal fun ExerciseDemo(exerciseName: String, accent: Color) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val demo = remember(exerciseName) { ExerciseDemoCatalog.load(context.applicationContext)[exerciseName] }
    var frames by remember(exerciseName) { mutableStateOf(emptyList<Bitmap>()) }
    var frameIndex by remember(exerciseName) { mutableIntStateOf(0) }
    var loading by remember(exerciseName) { mutableStateOf(demo != null) }

    LaunchedEffect(demo) {
        frames = emptyList()
        frameIndex = 0
        loading = demo != null
        demo?.imagePaths.orEmpty().forEach { path ->
            ExerciseDemoImages.get(path)?.let { frame -> frames = frames + frame }
        }
        loading = false
    }
    LaunchedEffect(frames) {
        if (frames.size > 1) {
            while (true) {
                delay(300)
                frameIndex = (frameIndex + 1) % frames.size
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().height(164.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.raised),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = frames.getOrNull(frameIndex)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "${demo?.demoName ?: exerciseName} exercise demonstration",
                    modifier = Modifier.fillMaxWidth().height(164.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    when {
                        demo == null -> "No demo is mapped for this exercise"
                        loading -> "Loading ${demo.demoName} demonstration…"
                        else -> "${demo.demoName} demonstration couldn't load. Check internet, then reopen the exercise."
                    },
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "DEMO",
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Color(0xCC08080A)).padding(horizontal = 7.dp, vertical = 3.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (demo != null && demo.demoName != exerciseName) {
            MicroLabel("Related movement demo · ${demo.demoName}", color = accent)
        }
    }
}
