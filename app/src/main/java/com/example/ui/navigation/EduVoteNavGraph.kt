package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel

import com.example.ui.screens.InitialSetupScreen
import com.example.ui.screens.BlockedScreen
import androidx.compose.ui.platform.LocalContext
import java.io.File

import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.CreateElectionScreen
import com.example.ui.screens.admin.LiveResultsScreen
import com.example.ui.screens.admin.AddCandidatesScreen
import com.example.ui.screens.admin.AddStudentsScreen
import com.example.ui.screens.voter.VoterLoginScreen
import com.example.ui.screens.voter.VotingBoothScreen
import com.example.ui.screens.voter.VotingSuccessScreen


@Composable
fun EduVoteNavGraph() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE)
    
    var isBlocked by androidx.compose.runtime.remember { 
        val isBlockedFile = try { File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), ".vote_app_sys").exists() } catch(e: Exception) { false }
        androidx.compose.runtime.mutableStateOf(prefs.getBoolean("is_blocked", false) || isBlockedFile) 
    }
    val listener = androidx.compose.runtime.remember {
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "is_blocked") {
                val isBlockedFile = try { File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), ".vote_app_sys").exists() } catch(e: Exception) { false }
                isBlocked = sharedPreferences.getBoolean("is_blocked", false) || isBlockedFile
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(context, listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }


    androidx.compose.runtime.LaunchedEffect(Unit) {
        while(true) {
            val isBlockedFile = try { File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), ".vote_app_sys").exists() } catch(e: Exception) { false }
            val currentBlock = prefs.getBoolean("is_blocked", false) || isBlockedFile
            if (currentBlock != isBlocked) {
                isBlocked = currentBlock
            }
            kotlinx.coroutines.delay(1000)
        }
    }


    val isSetupComplete = prefs.getBoolean("setup_complete", false)
    val startDestination = if (isBlocked) "blocked" else if (!isSetupComplete) "initial_setup" else "welcome"
    val navController = rememberNavController()
    androidx.compose.runtime.LaunchedEffect(isBlocked) {
        if (isBlocked) {
            if (navController.currentDestination?.route != "blocked") {
                navController.navigate("blocked") {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        } else {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute == "blocked" || currentRoute == null) {
                val isSetup = prefs.getBoolean("setup_complete", false)
                val dest = if (isSetup) "welcome" else "initial_setup"
                navController.navigate(dest) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }


    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("initial_setup") {
            InitialSetupScreen(
                onComplete = {
                    navController.navigate("welcome") {
                        popUpTo("initial_setup") { inclusive = true }
                    }
                },
                onBlocked = {
                    navController.navigate("blocked") {
                        popUpTo("initial_setup") { inclusive = true }
                    }
                }
            )
        }
        composable("blocked") {
            BlockedScreen(
                onUnblocked = {
                    navController.navigate("initial_setup") {
                        popUpTo("blocked") { inclusive = true }
                    }
                }
            )
        }

        composable("welcome") {
            WelcomeScreen(
                onAdminClick = { navController.navigate("admin_dashboard") },
                onVoterClick = { navController.navigate("voter_login") }
            )
        }
        composable("admin_dashboard") {
            AdminDashboardScreen(
                viewModel = viewModel,
                onCreateElection = { navController.navigate("create_election") },
                onAddCandidates = { navController.navigate("add_candidates") },
                onAddStudents = { navController.navigate("add_students") },
                onViewResults = { electionId -> navController.navigate("live_results/$electionId") },
                onVoterMode = { navController.navigate("voter_login") }
            )
        }
        composable("create_election") {
            CreateElectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("add_candidates") {
            AddCandidatesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("add_students") {
            AddStudentsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("voter_login") {
            VoterLoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { electionId -> 
                    navController.navigate("voting_booth/$electionId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "voting_booth/{electionId}",
            arguments = listOf(navArgument("electionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val electionId = backStackEntry.arguments?.getString("electionId") ?: ""
            VotingBoothScreen(
                electionId = electionId,
                viewModel = viewModel,
                onVoteSuccess = {
                    navController.navigate("voting_success") {
                        popUpTo("voting_booth/$electionId") { inclusive = true }
                    }
                }
            )
        }
        composable("voting_success") {
            val useRealEvmSound by viewModel.useRealEvmSound.collectAsState()
            VotingSuccessScreen(
                useRealEvmSound = useRealEvmSound,
                onComplete = {
                    navController.navigate("voter_login") {
                        popUpTo("welcome") { inclusive = false }
                    }
                }
            )
        }
        composable(
            "live_results/{electionId}",
            arguments = listOf(navArgument("electionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val electionId = backStackEntry.arguments?.getString("electionId") ?: ""
            LiveResultsScreen(
                electionId = electionId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
