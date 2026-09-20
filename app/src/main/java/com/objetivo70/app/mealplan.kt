package com.objetivo70.app

data class Meal(val name: String, val items: List<String>)
data class DayPlan(val day: String, val meals: List<Meal>)

val weeklyMealPlan = listOf(
    DayPlan("Domingo", listOf(Meal("Cena", listOf("Pescado", "Tomate o pepino", "Yogur griego")))),
    DayPlan("Lunes", listOf(
        Meal("Desayuno", listOf("Pancakes", "Frutos rojos", "Yogur griego")),
        Meal("Comida", listOf("Caldo", "2 huevos cocidos")),
        Meal("Cena", listOf("2 latas de atún", "Tomate", "Pepino", "Patata"))
    )),
    DayPlan("Martes", listOf(
        Meal("Desayuno", listOf("Pancakes", "Manzana", "Yogur griego")),
        Meal("Comida", listOf("Arroz", "Lomo o hamburguesa")),
        Meal("Cena", listOf("Tortilla de 3 huevos", "Ensalada"))
    )),
    DayPlan("Miércoles", listOf(
        Meal("Desayuno", listOf("Pancakes", "Plátano", "Yogur griego")),
        Meal("Comida", listOf("Patata", "Jamón cocido", "Huevos")),
        Meal("Cena", listOf("Yogur griego", "Jamón cocido", "Huevos", "Tomate", "Pepino"))
    )),
    DayPlan("Jueves", listOf(
        Meal("Desayuno", listOf("Pancakes", "Frutos rojos", "Yogur griego")),
        Meal("Comida", listOf("Pasta", "Salsa", "Atún")),
        Meal("Cena", listOf("Tortilla", "Jamón cocido", "Ensalada"))
    )),
    DayPlan("Viernes", listOf(
        Meal("Desayuno", listOf("Pancakes", "Manzana", "Yogur griego")),
        Meal("Comida", listOf("Arroz", "Carne", "Tomate", "Pepino"))
    ))
)
