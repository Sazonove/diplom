package com.example.diplom.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.diplom.DiplomApplication
import com.example.diplom.data.ApiException
import com.example.diplom.data.RemoteApi
import com.example.diplom.data.Session
import com.example.diplom.data.apiCall
import com.example.diplom.ui.screens.FriendProfileScreen
import com.example.diplom.ui.screens.FriendsSearchScreen
import com.example.diplom.ui.screens.LoginScreen
import com.example.diplom.ui.screens.OnboardingFlow
import com.example.diplom.ui.screens.ProgramDetailScreen
import com.example.diplom.ui.screens.ProgramsScreen
import com.example.diplom.ui.screens.RegisterScreen
import com.example.diplom.ui.screens.SubscriptionScreen
import com.example.diplom.ui.screens.SurveyScreen
import com.example.diplom.ui.screens.WorkoutHistoryScreen
import com.example.diplom.ui.screens.WorkoutScreen
import com.example.diplom.ui.theme.DiplomTheme
import kotlinx.coroutines.launch

@Composable
fun DiplomApp(app: DiplomApplication) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bootDone by remember { mutableStateOf(false) }
    var startRoute by remember { mutableStateOf<String?>(null) }

    fun navigateAfterAuth() {
        scope.launch {
            val me = apiCall { RemoteApi.api.me() }
            me.onSuccess { m ->
                when (resolveDestinationAfterAuth(app, m.profile?.surveyCompletedAt)) {
                    AfterAuthDestination.Main ->
                        nav.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    is AfterAuthDestination.Survey ->
                        nav.navigate("survey") {
                            popUpTo("login") { inclusive = true }
                        }
                }
            }.onFailure {
                Toast.makeText(context, (it as? ApiException)?.message ?: "Ошибка", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val onboardingDone = app.onboardingStore.isOnboardingCompleted()
        when {
            !onboardingDone -> startRoute = "onboarding"
            Session.token == null -> startRoute = "login"
            else -> {
                val r = apiCall { RemoteApi.api.me() }
                r.onSuccess { me ->
                    startRoute =
                        if (me.profile?.surveyCompletedAt == null) "survey" else "main"
                }.onFailure {
                    Session.token = null
                    scope.launch { app.tokenStore.setToken(null) }
                    startRoute = "login"
                }
            }
        }
        bootDone = true
    }

    DiplomTheme {
        if (!bootDone || startRoute == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            NavHost(navController = nav, startDestination = startRoute!!) {
                composable("onboarding") {
                    OnboardingFlow(app) {
                        nav.navigate("login") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
                composable("login") {
                    LoginScreen(
                        app = app,
                        onLoggedIn = { navigateAfterAuth() },
                        onRegister = { nav.navigate("register") },
                    )
                }
                composable("register") {
                    RegisterScreen(
                        app = app,
                        onAuthSuccess = { navigateAfterAuth() },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("survey") {
                    SurveyScreen(app) {
                        nav.navigate("main") {
                            popUpTo("survey") { inclusive = true }
                        }
                    }
                }
                composable("main") {
                    MainShell(
                        rootNav = nav,
                        app = app,
                        onLogout = {
                            nav.navigate("login") {
                                popUpTo(nav.graph.id) { inclusive = true }
                            }
                            startRoute = "login"
                        },
                    )
                }
                composable("programs") {
                    ProgramsScreen(
                        onOpenProgram = { id -> nav.navigate("program/$id") },
                        onBack = { nav.popBackStack() },
                        onSubscribe = { nav.navigate("subscription") },
                    )
                }
                composable(
                    "program/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    ProgramDetailScreen(
                        programId = id,
                        onWorkout = { pid, did -> nav.navigate("workout/$pid/$did") },
                        onBack = { nav.popBackStack() },
                        onSubscribe = { nav.navigate("subscription") },
                    )
                }
                composable(
                    "workout/{programId}/{dayId}",
                    arguments = listOf(
                        navArgument("programId") { type = NavType.StringType },
                        navArgument("dayId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val pid = entry.arguments?.getString("programId") ?: return@composable
                    val did = entry.arguments?.getString("dayId") ?: return@composable
                    WorkoutScreen(
                        programId = pid,
                        dayId = did,
                        onFinished = {
                            nav.navigate("main") {
                                popUpTo("main") { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("subscription") {
                    SubscriptionScreen(onBack = { nav.popBackStack() })
                }
                composable("workoutHistory") {
                    WorkoutHistoryScreen(onBack = { nav.popBackStack() })
                }
                composable("friendsSearch") {
                    FriendsSearchScreen(onBack = { nav.popBackStack() })
                }
                composable(
                    "friendProfile/{userId}",
                    arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                ) { entry ->
                    val userId = entry.arguments?.getString("userId") ?: return@composable
                    FriendProfileScreen(
                        userId = userId,
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
    }
}
