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
    groupId: String, // <-- Изменили на Non-nullable
    onLogoutClick: () -> Unit
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(
                groupId = groupId, // <-- Передаём groupId в MainScreen
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
    }
}