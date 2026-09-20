package com.objetivo70.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.objetivo70.app.data.*
import com.objetivo70.app.notifications.ReminderManager
import com.objetivo70.app.sensors.StepSensorManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val navItems = listOf(
    NavItem("home", "Inicio", Icons.Default.Home),
    NavItem("weight", "Peso", Icons.Default.MonitorWeight),
    NavItem("plan", "Plan", Icons.Default.Restaurant),
    NavItem("daily", "Día", Icons.Default.CheckCircle),
    NavItem("more", "Más", Icons.Default.GridView)
)
private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun Objetivo70App(vm: MainViewModel) {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val route = nav.currentBackStackEntryAsState().value?.destination?.route
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = route == item.route,
                        onClick = { nav.navigate(item.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(item.icon, null) }, label = { Text(item.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { DashboardScreen(vm, onGoWeight = { nav.navigate("weight") }) }
            composable("weight") { WeightScreen(vm) }
            composable("plan") { MealPlanScreen() }
            composable("daily") { DailyScreen(vm) }
            composable("more") { MoreScreen(onSteps={ nav.navigate("steps") }, onWorkout={nav.navigate("workout")}, onStats={nav.navigate("stats")}, onNotif={nav.navigate("notifications")}) }
            composable("steps") { StepsScreen(vm) }
            composable("workout") { WorkoutScreen(vm) }
            composable("stats") { StatsScreen(vm) }
            composable("notifications") { NotificationsScreen() }
        }
    }
}

@Composable private fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp)) { Text(title, style=MaterialTheme.typography.labelLarge, color=MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(6.dp)); Text(value, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold) } }
}

@Composable
fun DashboardScreen(vm: MainViewModel, onGoWeight: () -> Unit) {
    val d by vm.dashboard.collectAsStateWithLifecycle()
    val steps by vm.steps.collectAsStateWithLifecycle()
    val todaySteps = steps.lastOrNull { it.date == LocalDate.now().toString() }?.steps ?: 0
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("Objetivo 70", "Tu progreso, un día cada vez") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal=20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                        Column { Text("Peso actual", color=MaterialTheme.colorScheme.onSurfaceVariant); Text("%.1f kg".format(d.currentWeight), style=MaterialTheme.typography.displaySmall, fontWeight=FontWeight.Bold) }
                        Column(horizontalAlignment=Alignment.End) { Text("Objetivo", color=MaterialTheme.colorScheme.onSurfaceVariant); Text("%.0f kg".format(d.goalWeight), style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(18.dp)); LinearProgressIndicator({ d.progress.toFloat() }, Modifier.fillMaxWidth().height(10.dp), strokeCap=StrokeCap.Round)
                    Spacer(Modifier.height(8.dp)); Text("${(d.progress*100).roundToInt()}% · ${"%.1f".format(d.remainingKg)} kg restantes")
                }
            }
        }
        item { Spacer(Modifier.height(14.dp)); Row(Modifier.padding(horizontal=20.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)) { MetricCard("Próximo pesaje", d.nextWeighIn.format(dateFmt), Modifier.weight(1f)); MetricCard("Racha", "${d.streak} días", Modifier.weight(1f)) } }
        item { Spacer(Modifier.height(12.dp)); Row(Modifier.padding(horizontal=20.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)) { MetricCard("Pasos hoy", "$todaySteps / 10.000", Modifier.weight(1f)); MetricCard("Entrenos", d.workoutCount.toString(), Modifier.weight(1f)) } }
        item { Button(onGoWeight, Modifier.fillMaxWidth().padding(20.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Registrar peso") } }
    }
}

@Composable
fun WeightScreen(vm: MainViewModel) {
    val weights by vm.weights.collectAsStateWithLifecycle()
    var kg by remember { mutableStateOf("") }
    val current = weights.lastOrNull()?.weightKg ?: 82.0
    val initial = weights.firstOrNull()?.weightKg ?: 82.0
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("Seguimiento de peso", "Recomendado: domingo al despertar, tras ir al baño y antes de desayunar") }
        item { Row(Modifier.padding(horizontal=20.dp), horizontalArrangement=Arrangement.spacedBy(10.dp)) { MetricCard("Inicial", "%.1f kg".format(initial), Modifier.weight(1f)); MetricCard("Actual", "%.1f kg".format(current), Modifier.weight(1f)); MetricCard("Perdido", "%.1f kg".format((initial-current).coerceAtLeast(0.0)), Modifier.weight(1f)) } }
        item { Spacer(Modifier.height(12.dp)); MetricCard("Promedio semanal", "%.2f kg/sem".format(weeklyWeightAverageLoss(weights)), Modifier.fillMaxWidth().padding(horizontal=20.dp)) }
        item { WeightChart(weights, Modifier.fillMaxWidth().height(220.dp).padding(20.dp)) }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal=20.dp)) { Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                OutlinedTextField(kg, {kg=it}, label={Text("Peso de hoy (kg)")}, modifier=Modifier.weight(1f), singleLine=true)
                Spacer(Modifier.width(10.dp)); Button(onClick={ kg.replace(',', '.').toDoubleOrNull()?.let { vm.saveWeight(LocalDate.now(), it); kg="" } }) { Text("Guardar") }
            } }
        }
        items(weights.asReversed(), key={it.date}) { e -> ListItem(headlineContent={Text("${e.weightKg} kg")}, supportingContent={Text(LocalDate.parse(e.date).format(dateFmt))}, trailingContent={IconButton({vm.deleteWeight(e.date)}) {Icon(Icons.Default.Delete, "Eliminar")}}) }
    }
}

@Composable
private fun WeightChart(weights: List<WeightEntry>, modifier: Modifier = Modifier) {
    Card(modifier) {
        if (weights.size < 2) Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center) { Text("Añade al menos 2 registros para ver la evolución") }
        else Canvas(Modifier.fillMaxSize().padding(20.dp)) {
            val vals = weights.map { it.weightKg }; val min = vals.min()-1; val max = vals.max()+1; val range=(max-min).coerceAtLeast(1.0)
            val pts = vals.mapIndexed { i, v -> Offset(i*(size.width/(vals.size-1)), size.height-(((v-min)/range)*size.height).toFloat()) }
            pts.zipWithNext().forEach { (a,b) -> drawLine(color=androidx.compose.ui.graphics.Color(0xFF2E7D32), start=a, end=b, strokeWidth=6f, cap=StrokeCap.Round) }
            pts.forEach { drawCircle(androidx.compose.ui.graphics.Color(0xFF66BB6A), 8f, it) }
        }
    }
}

@Composable fun MealPlanScreen() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("Plan de comidas", "Plan semanal guardado en la app y disponible offline") }
        items(weeklyMealPlan) { day ->
            Card(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=6.dp)) { Column(Modifier.padding(16.dp)) {
                Text(day.day.uppercase(), fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.primary)
                day.meals.forEach { meal -> Spacer(Modifier.height(10.dp)); Text(meal.name, fontWeight=FontWeight.SemiBold); meal.items.forEach { Text("• $it", Modifier.padding(start=8.dp, top=2.dp)) } }
            } }
        }
    }
}

@Composable fun DailyScreen(vm: MainViewModel) {
    val list by vm.compliance.collectAsStateWithLifecycle(); val today=LocalDate.now().toString(); val saved=list.lastOrNull{it.date==today} ?: DailyCompliance(today)
    var state by remember(saved) { mutableStateOf(saved) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Checklist diaria", "Completa tus cinco hábitos del día")
        CircularProgressIndicator(state.percent/100f, Modifier.size(110.dp).align(Alignment.CenterHorizontally), strokeWidth=10.dp)
        Text("${state.percent}% cumplido", Modifier.align(Alignment.CenterHorizontally).padding(12.dp), style=MaterialTheme.typography.titleMedium)
        val rows = listOf(
            "Seguí el plan de comidas" to state.mealPlan,
            "Tomé suficiente agua" to state.water,
            "Alcancé 10.000 pasos" to state.stepsGoal,
            "Entrené hoy" to state.trained,
            "Dormí al menos 7 horas" to state.sleep
        )
        rows.forEachIndexed { i, (label, checked) -> Row(Modifier.fillMaxWidth().padding(horizontal=20.dp), verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked, { v -> state = when(i){0->state.copy(mealPlan=v);1->state.copy(water=v);2->state.copy(stepsGoal=v);3->state.copy(trained=v);else->state.copy(sleep=v)}; vm.saveCompliance(state) }); Text(label)
        } }
    }
}

@Composable fun MoreScreen(onSteps:()->Unit,onWorkout:()->Unit,onStats:()->Unit,onNotif:()->Unit) {
    Column(Modifier.fillMaxSize()) { ScreenTitle("Más")
        listOf(Triple("Pasos",Icons.Default.DirectionsWalk,onSteps), Triple("Entrenamiento",Icons.Default.FitnessCenter,onWorkout), Triple("Estadísticas",Icons.Default.BarChart,onStats), Triple("Notificaciones",Icons.Default.Notifications,onNotif)).forEach { (title, icon, click) ->
            ListItem(modifier=Modifier.fillMaxWidth().clickable(onClick=click), headlineContent={Text(title)}, leadingContent={Icon(icon,null)}, trailingContent={Icon(Icons.Default.ChevronRight,null)}); HorizontalDivider()
        }
    }
}

@Composable fun StepsScreen(vm: MainViewModel) {
    val context=LocalContext.current; val all by vm.steps.collectAsStateWithLifecycle(); var manual by remember{mutableStateOf("")}; var sensorCount by remember{mutableIntStateOf(0)}
    val sensor=remember{StepSensorManager(context)}
    val permission=Manifest.permission.ACTIVITY_RECOGNITION
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ if(it) sensor.start() }
    DisposableEffect(Unit){ sensor.onSteps={sensorCount=it}; if(Build.VERSION.SDK_INT<29 || ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED) sensor.start(); onDispose{sensor.stop()} }
    val last7=all.takeLast(7); val avg=if(last7.isEmpty())0 else last7.map{it.steps}.average().roundToInt()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { ScreenTitle("Pasos", "Objetivo diario: 10.000")
        Row(Modifier.padding(horizontal=20.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)){MetricCard("Sensor", if(sensor.available) "$sensorCount" else "No disponible", Modifier.weight(1f)); MetricCard("Media 7 días", avg.toString(), Modifier.weight(1f))}
        if(sensor.available && Build.VERSION.SDK_INT>=29 && ContextCompat.checkSelfPermission(context,permission)!=PackageManager.PERMISSION_GRANTED) Button({launcher.launch(permission)},Modifier.padding(20.dp)){Text("Permitir sensor de pasos")}
        if(sensor.available) Button({vm.saveSteps(LocalDate.now(),sensorCount,"sensor")},Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp)){Text("Guardar pasos del sensor")}
        Row(Modifier.padding(20.dp), verticalAlignment=Alignment.CenterVertically){OutlinedTextField(manual,{manual=it},label={Text("Pasos manuales")},Modifier.weight(1f),singleLine=true);Spacer(Modifier.width(8.dp));Button({manual.toIntOrNull()?.let{vm.saveSteps(LocalDate.now(),it);manual=""}}){Text("Guardar")}}
        Text("Historial semanal", Modifier.padding(horizontal=20.dp), style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold)
        last7.asReversed().forEach { ListItem(headlineContent={Text("${it.steps} pasos")}, supportingContent={Text("${LocalDate.parse(it.date).format(dateFmt)} · ${it.source}")}) }
        Text("Nota: TYPE_STEP_COUNTER cuenta desde el último reinicio. La app toma una base diaria desde el primer uso de cada día.", Modifier.padding(20.dp), color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val exercises = listOf("Flexiones","Sentadillas","Sentadillas búlgaras","Fondos entre sillas","Remo con mochila","Plancha")
@Composable fun WorkoutScreen(vm: MainViewModel) {
    val history by vm.workouts.collectAsStateWithLifecycle(); var inputs by remember { mutableStateOf(exercises.associateWith { List(3){""} }) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("Entrenamiento", "Registra 3 series por ejercicio; en plancha introduce segundos") }
        exercises.forEach { ex -> item { Card(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=6.dp)) { Column(Modifier.padding(14.dp)){Text(ex,fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){inputs[ex]!!.forEachIndexed{i,v->OutlinedTextField(v,{nv->inputs=inputs.toMutableMap().also{m->m[ex]=m[ex]!!.toMutableList().also{it[i]=nv}}},label={Text("S${i+1}")},modifier=Modifier.weight(1f),singleLine=true)}}} } } }
        item { Button(onClick={ val map=inputs.mapValues{(ex,vs)->vs.mapNotNull{n->n.toIntOrNull()?.let{if(ex=="Plancha")ExerciseValue(seconds=it) else ExerciseValue(reps=it)}}}.filterValues{it.isNotEmpty()}; if(map.isNotEmpty()){vm.saveWorkout(LocalDate.now(),map);inputs=exercises.associateWith{List(3){""}}} },Modifier.fillMaxWidth().padding(20.dp)){Text("Guardar entrenamiento")} }
        item { Text("Historial", Modifier.padding(horizontal=20.dp), style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold) }
        items(history,key={it.workout.id}) { w -> Card(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=6.dp)){Column(Modifier.padding(14.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(LocalDate.parse(w.workout.date).format(dateFmt),fontWeight=FontWeight.Bold);IconButton({vm.deleteWorkout(w.workout.id)}){Icon(Icons.Default.Delete,null)}};w.sets.groupBy{it.exercise}.forEach{(ex,sets)->Text("$ex: "+sets.joinToString(" / "){it.reps?.toString() ?: "${it.seconds}s"})}}} }
    }
}

@Composable fun StatsScreen(vm: MainViewModel) {
    val weights by vm.weights.collectAsStateWithLifecycle(); val steps by vm.steps.collectAsStateWithLifecycle(); val comp by vm.compliance.collectAsStateWithLifecycle(); val workouts by vm.workouts.collectAsStateWithLifecycle()
    val initial=weights.firstOrNull()?.weightKg?:82.0; val current=weights.lastOrNull()?.weightKg?:initial; val avgSteps=if(steps.isEmpty())0 else steps.takeLast(7).map{it.steps}.average().roundToInt()
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) { item{ScreenTitle("Estadísticas")}; items(listOf("Peso inicial" to "%.1f kg".format(initial),"Peso actual" to "%.1f kg".format(current),"Kg perdidos" to "%.1f kg".format((initial-current).coerceAtLeast(0.0)),"Promedio semanal" to "%.2f kg/sem".format(weeklyWeightAverageLoss(weights)),"Días cumplidos" to comp.count{it.allDone}.toString(),"Entrenamientos" to workouts.size.toString(),"Pasos promedio" to avgSteps.toString())){(a,b)->MetricCard(a,b,Modifier.fillMaxWidth())} }
}

@Composable fun NotificationsScreen() {
    val context=LocalContext.current; val prefs=remember{context.getSharedPreferences("settings",0)}; var meals by remember{mutableStateOf(prefs.getBoolean("meal_reminders",false))}; var sunday by remember{mutableStateOf(prefs.getBoolean("sunday_reminder",false))}
    val notifLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    Column(Modifier.fillMaxSize()) { ScreenTitle("Notificaciones", "Recordatorios locales; no requieren conexión")
        ListItem(headlineContent={Text("Comidas")},supportingContent={Text("08:00 desayuno · 14:00 comida · 21:00 cena")},trailingContent={Switch(meals,{v->meals=v;prefs.edit().putBoolean("meal_reminders",v).apply();ReminderManager.scheduleDaily(context,v);if(v&&Build.VERSION.SDK_INT>=33)notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)})})
        HorizontalDivider(); ListItem(headlineContent={Text("Pesaje del domingo")},supportingContent={Text("Recordatorio semanal a las 08:05")},trailingContent={Switch(sunday,{v->sunday=v;prefs.edit().putBoolean("sunday_reminder",v).apply();ReminderManager.scheduleSundayWeighIn(context,v);if(v&&Build.VERSION.SDK_INT>=33)notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)})})
    }
}
