package com.objetivo70.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "weights")
data class WeightEntry(@PrimaryKey val date: String, val weightKg: Double)

@Entity(tableName = "steps")
data class StepEntry(@PrimaryKey val date: String, val steps: Int, val source: String = "manual")

@Entity(tableName = "daily_compliance")
data class DailyCompliance(
    @PrimaryKey val date: String,
    val mealPlan: Boolean = false,
    val water: Boolean = false,
    val stepsGoal: Boolean = false,
    val trained: Boolean = false,
    val sleep: Boolean = false
) {
    val completedCount: Int get() = listOf(mealPlan, water, stepsGoal, trained, sleep).count { it }
    val percent: Int get() = completedCount * 20
    val allDone: Boolean get() = completedCount == 5
}

@Entity(tableName = "workouts")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val notes: String = ""
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutSession::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exercise: String,
    val setNumber: Int,
    val reps: Int? = null,
    val seconds: Int? = null
)

data class WorkoutWithSets(
    @Embedded val workout: WorkoutSession,
    @Relation(parentColumn = "id", entityColumn = "workoutId") val sets: List<WorkoutSet>
)

@Dao
interface HealthDao {
    @Query("SELECT * FROM weights ORDER BY date ASC") fun weights(): Flow<List<WeightEntry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWeight(entry: WeightEntry)
    @Query("DELETE FROM weights WHERE date = :date") suspend fun deleteWeight(date: String)

    @Query("SELECT * FROM steps ORDER BY date ASC") fun steps(): Flow<List<StepEntry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSteps(entry: StepEntry)

    @Query("SELECT * FROM daily_compliance ORDER BY date ASC") fun compliance(): Flow<List<DailyCompliance>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCompliance(entry: DailyCompliance)

    @Insert suspend fun insertWorkout(session: WorkoutSession): Long
    @Insert suspend fun insertSets(sets: List<WorkoutSet>)
    @Transaction @Query("SELECT * FROM workouts ORDER BY date DESC, id DESC") fun workouts(): Flow<List<WorkoutWithSets>>
    @Query("DELETE FROM workouts WHERE id = :id") suspend fun deleteWorkout(id: Long)
}

@Database(
    entities = [WeightEntry::class, StepEntry::class, DailyCompliance::class, WorkoutSession::class, WorkoutSet::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao
    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "objetivo70.db"
        ).build()
    }
}

class HealthRepository(private val db: AppDatabase) {
    private val dao = db.healthDao()
    val weights = dao.weights()
    val steps = dao.steps()
    val compliance = dao.compliance()
    val workouts = dao.workouts()

    suspend fun saveWeight(date: LocalDate, kg: Double) = dao.upsertWeight(WeightEntry(date.toString(), kg))
    suspend fun deleteWeight(date: String) = dao.deleteWeight(date)
    suspend fun saveSteps(date: LocalDate, steps: Int, source: String = "manual") = dao.upsertSteps(StepEntry(date.toString(), steps, source))
    suspend fun saveCompliance(item: DailyCompliance) = dao.upsertCompliance(item)
    suspend fun deleteWorkout(id: Long) = dao.deleteWorkout(id)

    @Transaction
    suspend fun saveWorkout(date: LocalDate, values: Map<String, List<ExerciseValue>>) {
        val id = dao.insertWorkout(WorkoutSession(date = date.toString()))
        val rows = values.flatMap { (exercise, sets) ->
            sets.mapIndexed { index, value ->
                WorkoutSet(
                    workoutId = id,
                    exercise = exercise,
                    setNumber = index + 1,
                    reps = value.reps,
                    seconds = value.seconds
                )
            }
        }
        dao.insertSets(rows)
    }
}

data class ExerciseValue(val reps: Int? = null, val seconds: Int? = null)
