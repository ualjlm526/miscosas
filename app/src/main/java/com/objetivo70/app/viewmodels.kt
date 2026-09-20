package com.objetivo70.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.objetivo70.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

private const val START_WEIGHT = 82.0
const val GOAL_WEIGHT = 70.0

class MainViewModel(private val repository: HealthRepository) : ViewModel() {
    val weights = repository.weights.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val steps = repository.steps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val compliance = repository.compliance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val workouts = repository.workouts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dashboard = combine(weights, steps, compliance, workouts) { w, s, c, wo ->
        val current = w.lastOrNull()?.weightKg ?: START_WEIGHT
        val initial = w.firstOrNull()?.weightKg ?: START_WEIGHT
        val lost = (initial - current).coerceAtLeast(0.0)
        val totalToLose = (initial - GOAL_WEIGHT).coerceAtLeast(0.1)
        val progress = (lost / totalToLose).coerceIn(0.0, 1.0)
        val today = LocalDate.now()
        val nextSunday = generateSequence(today) { it.plusDays(1) }.first { it.dayOfWeek == DayOfWeek.SUNDAY }
        val streak = calculateStreak(c, today)
        DashboardState(initial, current, GOAL_WEIGHT, max(0.0, current - GOAL_WEIGHT), progress, nextSunday, streak, wo.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun saveWeight(date: LocalDate, kg: Double) = viewModelScope.launch { repository.saveWeight(date, kg) }
    fun deleteWeight(date: String) = viewModelScope.launch { repository.deleteWeight(date) }
    fun saveSteps(date: LocalDate, count: Int, source: String = "manual") = viewModelScope.launch { repository.saveSteps(date, count, source) }
    fun saveCompliance(item: DailyCompliance) = viewModelScope.launch { repository.saveCompliance(item) }
    fun saveWorkout(date: LocalDate, values: Map<String, List<ExerciseValue>>) = viewModelScope.launch { repository.saveWorkout(date, values) }
    fun deleteWorkout(id: Long) = viewModelScope.launch { repository.deleteWorkout(id) }

    private fun calculateStreak(items: List<DailyCompliance>, today: LocalDate): Int {
        val done = items.filter { it.allDone }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
        var cursor = today
        if (cursor !in done) cursor = cursor.minusDays(1)
        var streak = 0
        while (cursor in done) { streak++; cursor = cursor.minusDays(1) }
        return streak
    }
}

data class DashboardState(
    val initialWeight: Double = START_WEIGHT,
    val currentWeight: Double = START_WEIGHT,
    val goalWeight: Double = GOAL_WEIGHT,
    val remainingKg: Double = START_WEIGHT - GOAL_WEIGHT,
    val progress: Double = 0.0,
    val nextWeighIn: LocalDate = LocalDate.now(),
    val streak: Int = 0,
    val workoutCount: Int = 0
)

class MainViewModelFactory(private val repository: HealthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
}

fun weeklyWeightAverageLoss(weights: List<WeightEntry>): Double {
    if (weights.size < 2) return 0.0
    val first = weights.first(); val last = weights.last()
    val days = ChronoUnit.DAYS.between(LocalDate.parse(first.date), LocalDate.parse(last.date)).coerceAtLeast(1)
    val weeks = days / 7.0
    return ((first.weightKg - last.weightKg) / weeks).coerceAtLeast(0.0)
}
