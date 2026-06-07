package com.example.diplom.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.diplom.DiplomApplication
import com.example.diplom.ui.screens.ForYouTabScreen
import com.example.diplom.ui.screens.ProfileTabScreen
import com.example.diplom.ui.screens.ReportTabScreen
import com.example.diplom.ui.screens.TrainingsHomeScreen

@Composable
fun MainShell(rootNav: NavController, app: DiplomApplication, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                    label = { Text("Тренировки") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    label = { Text("Для вас") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Assessment, contentDescription = null) },
                    label = { Text("Отчёт") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Профиль") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            0 -> TrainingsHomeScreen(Modifier.padding(padding), rootNav)
            1 -> ForYouTabScreen(Modifier.padding(padding), rootNav)
            2 -> ReportTabScreen(Modifier.padding(padding), rootNav)
            3 -> ProfileTabScreen(Modifier.padding(padding), app, rootNav, onLogout)
        }
    }
}
