package com.objetivo70.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as Objetivo70Application
        setContent {
            Objetivo70Theme {
                val vm: MainViewModel = viewModel(factory = MainViewModelFactory(app.repository))
                Objetivo70App(vm)
            }
        }
    }
}
