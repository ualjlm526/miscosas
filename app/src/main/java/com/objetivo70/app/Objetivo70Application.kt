package com.objetivo70.app

import android.app.Application
import com.objetivo70.app.data.AppDatabase
import com.objetivo70.app.data.HealthRepository

class Objetivo70Application : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { HealthRepository(database) }
}
