// MainNavigation.kt
package com.rich.familymoney.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController(),
    groupId: String,
    onLogoutClick: () -> Unit
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(
                groupId = groupId,
                // Передаём NavController, чтобы из MainScreen можно было навигироваться
                navController = navController,
                onAddPaymentClick = { navController.navigate("addPayment") },
                onLeaveGroupClick = { /* TODO */ },
                onLogoutClick = onLogoutClick
            )
        }
        composable("addPayment") {
            AddPaymentScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() }
            )
        }
        // --- НОВЫЙ МАРШРУТ ---
        composable("debt_screen/{groupId}") { backStackEntry ->
            val screenGroupId = backStackEntry.arguments?.getString("groupId")
            if (screenGroupId != null) {
                DebtScreen(groupId = screenGroupId, navController = navController)
            }
        }
    }
}