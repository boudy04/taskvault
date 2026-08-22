package dev.boudy04.taskvault.data.source.local

import androidx.room.TypeConverter
import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.data.TaskStatus

class Converters {
    @TypeConverter fun statusToString(s: TaskStatus): String = s.name
    @TypeConverter fun stringToStatus(v: String): TaskStatus = TaskStatus.valueOf(v)
    @TypeConverter fun priorityToString(p: TaskPriority): String = p.name
    @TypeConverter fun stringToPriority(v: String): TaskPriority = TaskPriority.valueOf(v)
    @TypeConverter fun opTypeToString(o: PendingOpType): String = o.name
    @TypeConverter fun stringToOpType(v: String): PendingOpType = PendingOpType.valueOf(v)
    @TypeConverter fun opStateToString(s: PendingOpState): String = s.name
    @TypeConverter fun stringToOpState(v: String): PendingOpState = PendingOpState.valueOf(v)
}
