package com.nova.assistant.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nova.assistant.ui.screen.HomeScreen
import com.nova.assistant.ui.screen.SettingsScreen
import com.nova.assistant.ui.screen.MemoryScreen

sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Settings : Screen("settings")
    object Memory   : Screen("memory")
    object Routines : Screen("routines")
}

@Composable
fun NovaNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToMemory   = { navController.navigate(Screen.Memory.route) },
                onNavigateToRoutines = { navController.navigate(Screen.Routines.route) }
            )
        }
        composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Memory.route)   { MemoryScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Routines.route) { com.nova.assistant.ui.screen.RoutineScreen(onBack = { navController.popBackStack() }) }
    }
}
