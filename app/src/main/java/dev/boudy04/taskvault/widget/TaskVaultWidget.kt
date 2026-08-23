package dev.boudy04.taskvault.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.TodoActivity
import dev.boudy04.taskvault.data.TaskStatus
import dev.boudy04.taskvault.data.source.local.LocalTask
import dev.boudy04.taskvault.data.source.local.TaskDao
import kotlinx.coroutines.flow.first

/** Immutable snapshot rendered by [TaskVaultWidget]. */
data class WidgetState(val openCount: Int, val titles: List<String>)

const val WIDGET_MAX_TITLES = 5

/**
 * Active tasks (TODO or IN_PROGRESS) for the home-screen widget: full open count plus up to
 * [WIDGET_MAX_TITLES] titles, newest first (server timestamps compare lexicographically;
 * tasks without timestamps sort last).
 */
fun List<LocalTask>.toWidgetState(): WidgetState {
    val open = filter { !it.isCompleted && it.status != TaskStatus.DONE }
        .sortedWith(compareByDescending { it.updatedAt ?: it.createdAt ?: "" })
    return WidgetState(open.size, open.take(WIDGET_MAX_TITLES).map(LocalTask::title))
}

class TaskVaultWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = EntryPointAccessors.fromApplication(context, TaskVaultWidgetEntryPoint::class.java).taskDao()
        val state = dao.observeAll().first().toWidgetState()
        provideContent { Content(state, context) }
    }

    @Composable
    private fun Content(state: WidgetState, context: Context) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(BACKGROUND))
                .clickable(actionStartActivity<TodoActivity>()),
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(PRIMARY))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = context.getString(R.string.widget_open_count, state.openCount),
                    style = TextStyle(color = ColorProvider(ON_PRIMARY), fontSize = 13.sp),
                )
            }
            if (state.titles.isEmpty()) {
                Text(
                    text = context.getString(R.string.widget_no_active_tasks),
                    style = TextStyle(color = ColorProvider(ON_BACKGROUND)),
                    modifier = GlanceModifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            } else {
                state.titles.forEach { title ->
                    Text(
                        text = title,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(ON_BACKGROUND)),
                        modifier = GlanceModifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }

    private companion object {
        val PRIMARY = Color(0xFFFF5B29)
        val ON_PRIMARY = Color(0xFF14161D)
        val BACKGROUND = Color(0xFF0E0F13)
        val ON_BACKGROUND = Color(0xFFF3F3F0)
    }
}

class TaskVaultWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskVaultWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TaskVaultWidgetEntryPoint {
    fun taskDao(): TaskDao
}
