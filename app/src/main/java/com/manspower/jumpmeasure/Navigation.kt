package com.manspower.jumpmeasure

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.manspower.jumpmeasure.domain.model.JumpCategory
import com.manspower.jumpmeasure.domain.model.JumpGender
import com.manspower.jumpmeasure.ui.camera.CameraScreen
import com.manspower.jumpmeasure.ui.main.MainScreen
import com.manspower.jumpmeasure.ui.history.HistoryScreen
import com.manspower.jumpmeasure.ui.splash.SplashScreen
import com.manspower.jumpmeasure.ui.category.CategoryScreen
import com.manspower.jumpmeasure.ui.category.GenderScreen

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
                onGenderSelected = { jt, cat, gender, name ->
                    val finalName = if (name.isNotBlank()) name.trim() else "Peserta"
                    val encodedName = java.net.URLEncoder.encode(finalName, "UTF-8")
                    navController.navigate("camera/$jt/${cat.name}/${gender.name}/$encodedName/3.0")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Camera / Measurement ──
        composable(
            route = "camera/{jumpType}/{category}/{gender}/{participantName}/{cameraDistance}",
            arguments = listOf(
                navArgument("jumpType")        { type = NavType.StringType },
                navArgument("category")        { type = NavType.StringType },
                navArgument("gender")          { type = NavType.StringType },
                navArgument("participantName") { type = NavType.StringType; defaultValue = "Peserta" },
                navArgument("cameraDistance")  { type = NavType.FloatType; defaultValue = 3.0f }
            )
        ) { backStackEntry ->
            val jumpType        = backStackEntry.arguments?.getString("jumpType")        ?: "VERTICAL"
            val categoryStr     = backStackEntry.arguments?.getString("category")        ?: "SMP"
            val genderStr       = backStackEntry.arguments?.getString("gender")          ?: "PUTRA"
            val rawName         = backStackEntry.arguments?.getString("participantName") ?: "Peserta"
            val participantName = try {
                java.net.URLDecoder.decode(rawName, "UTF-8")
            } catch (_: Exception) {
                rawName
            }
            val cameraDistance  = backStackEntry.arguments?.getFloat("cameraDistance")   ?: 3.0f
            val category = JumpCategory.valueOf(categoryStr)
            val gender   = JumpGender.valueOf(genderStr)

            CameraScreen(
                jumpType        = jumpType,
                category        = category,
                gender          = gender,
                initialParticipantName = participantName,
                cameraDistance  = cameraDistance,
                onBack          = { navController.popBackStack() }
            )
        }

        // ── History ──
        composable("history") {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
