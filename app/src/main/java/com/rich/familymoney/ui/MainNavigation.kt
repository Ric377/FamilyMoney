// MainNavigation.kt
package com.rich.familymoney.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rich.familymoney.repository.GroupRepository
import com.rich.familymoney.viewmodel.MainViewModel
import com.rich.familymoney.viewmodel.MainViewModelFactory

@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController(),
    groupId: String,
    onLogoutClick: () -> Unit,
    // Этот коллбэк будет вызван, когда пользователь покинул группу,
    // чтобы вы могли переключить экран на JoinGroupScreen
    onGroupLeft: () -> Unit
) {
    // ViewModel теперь создается здесь, чтобы быть доступной для MainScreen
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(groupId, GroupRepository())
    )

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(
                groupId = groupId,
                navController = navController,
                // Передаем ViewModel в MainScreen
                viewModel = mainViewModel,
                onAddPaymentClick = { navController.navigate("addPayment") },
                // ЗАМЕНЯЕМ /* TODO */ НА РЕАЛЬНЫЙ ВЫЗОВ
                onLeaveGroupClick = {
                    mainViewModel.leaveGroup(onSuccess = onGroupLeft)
                },
                onLogoutClick = onLogoutClick
            )
        }
        composable("addPayment") {
            AddPaymentScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("debt_screen/{groupId}") { backStackEntry ->
            val screenGroupId = backStackEntry.arguments?.getString("groupId")
            if (screenGroupId != null) {
                DebtScreen(groupId = screenGroupId, navController = navController)
            }
        }
    }
}