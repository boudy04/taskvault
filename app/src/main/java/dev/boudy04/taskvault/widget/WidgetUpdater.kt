package dev.boudy04.taskvault.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.boudy04.taskvault.data.source.local.TaskDao
import dev.boudy04.taskvault.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps the home-screen widget in sync: observes Room on the application scope and pushes an
 * update on every task-table change; also callable directly (e.g. after a successful sync).
 */
@Singleton
class WidgetUpdater @Inject constructor(
    private val taskDao: TaskDao,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {

    fun observe() {
        scope.launch {
            taskDao.observeAll().collect { update() }
        }
    }

    fun update() {
        scope.launch { TaskVaultWidget().updateAll(context) }
    }
}
