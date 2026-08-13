package com.example.jumpmeasure

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.jumpmeasure.domain.model.JumpCategory
import com.example.jumpmeasure.domain.model.JumpGender
import com.example.jumpmeasure.ui.camera.CameraScreen
import com.example.jumpmeasure.ui.main.MainScreen
import com.example.jumpmeasure.ui.history.HistoryScreen
import com.example.jumpmeasure.ui.splash.SplashScreen
import com.example.jumpmeasure.ui.category.CategoryScreen
import com.example.jumpmeasure.ui.category.GenderScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {

        // ── Splash ──
        composable("splash") {
            SplashScreen(onNavigateToMain = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        // ── Home ──
        composable("home") {
            MainScreen(
                onNavigateToCategory = { jumpType ->
                    navController.navigate("category/$jumpType")
                },
                onNavigateToHistory = {
                    navController.navigate("history")
                }
            )
        }

        // ── Category Selection ──
        composable(
            route = "category/{jumpType}",
            arguments = listOf(navArgument("jumpType") { type = NavType.StringType })
        ) { backStackEntry ->
            val jumpType = backStackEntry.arguments?.getString("jumpType") ?: "VERTICAL"
            CategoryScreen(
                jumpType = jumpType,
                onCategorySelected = { jt, category ->
                    navController.navigate("gender/$jt/${category.name}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Gender Selection ──
        composable(
            route = "gender/{jumpType}/{category}",
            arguments = listOf(
                navArgument("jumpType")  { type = NavType.StringType },
                navArgument("category")  { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jumpType = backStackEntry.arguments?.getString("jumpType") ?: "VERTICAL"
            val categoryStr = backStackEntry.arguments?.getString("category") ?: "SMP"
            val category = JumpCategory.valueOf(categoryStr)

            GenderScreen(
                jumpType = jumpType,
                category = category,
                onGenderSelected = { jt, cat, gender ->
                    navController.navigate("camera/$jt/${cat.name}/${gender.name}/3.0")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Camera / Measurement ──
        composable(
            route = "camera/{jumpType}/{category}/{gender}/{cameraDistance}",
            arguments = listOf(
                navArgument("jumpType")      { type = NavType.StringType },
                navArgument("category")      { type = NavType.StringType },
                navArgument("gender")        { type = NavType.StringType },
                navArgument("cameraDistance") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val jumpType       = backStackEntry.arguments?.getString("jumpType")       ?: "VERTICAL"
            val categoryStr    = backStackEntry.arguments?.getString("category")       ?: "SMP"
            val genderStr      = backStackEntry.arguments?.getString("gender")         ?: "PUTRA"
            val cameraDistance = backStackEntry.arguments?.getFloat("cameraDistance")  ?: 3.0f
            val category = JumpCategory.valueOf(categoryStr)
            val gender   = JumpGender.valueOf(genderStr)

            CameraScreen(
                jumpType       = jumpType,
                category       = category,
                gender         = gender,
                cameraDistance = cameraDistance,
                onBack         = { navController.popBackStack() }
            )
        }

        // ── History ──
        composable("history") {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
