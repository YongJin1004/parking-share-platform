package com.parking.share.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.parking.share.presentation.auth.CertificationScreen
import com.parking.share.presentation.auth.LoginScreen
import com.parking.share.presentation.auth.RegisterScreen
import com.parking.share.presentation.auth.RegisterWithCertScreen
import com.parking.share.presentation.home.HomeScreen
import com.parking.share.presentation.host.AddParkingSpaceScreen
import com.parking.share.presentation.host.HostScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Certification : Screen("certification")
    object RegisterWithCert : Screen("register_with_cert/{impUid}/{name}/{phone}") {
        fun createRoute(impUid: String, name: String, phone: String): String {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val encodedPhone = URLEncoder.encode(phone, "UTF-8")
            return "register_with_cert/$impUid/$encodedName/$encodedPhone"
        }
    }
    object Register : Screen("register")  // 기존 회원가입 (본인인증 없이)
    object Home : Screen("home")
    object Host : Screen("host")
    object AddParkingSpace : Screen("add_parking_space")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // 본인인증 화면으로 이동
                    navController.navigate(Screen.Certification.route)
                }
            )
        }

        // 본인인증 화면
        composable(Screen.Certification.route) {
            CertificationScreen(
                onCertificationSuccess = { impUid, name, phone ->
                    navController.navigate(Screen.RegisterWithCert.createRoute(impUid, name, phone)) {
                        popUpTo(Screen.Certification.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 본인인증 후 회원가입 화면
        composable(
            route = Screen.RegisterWithCert.route,
            arguments = listOf(
                navArgument("impUid") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("phone") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val impUid = backStackEntry.arguments?.getString("impUid") ?: ""
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", "UTF-8")
            val phone = URLDecoder.decode(backStackEntry.arguments?.getString("phone") ?: "", "UTF-8")

            RegisterWithCertScreen(
                impUid = impUid,
                certifiedName = name,
                certifiedPhone = phone,
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 기존 회원가입 (본인인증 없이) - 개발용으로 유지
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToHost = {
                    navController.navigate(Screen.Host.route)
                },
                onNavigateToGuest = {
                    // TODO: Guest 화면으로 이동
                },
                onNavigateToMyPage = {
                    // TODO: 마이페이지로 이동
                }
            )
        }

        // Host 화면 - 내 주차 공간 목록
        composable(Screen.Host.route) {
            HostScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddParkingSpace = {
                    navController.navigate(Screen.AddParkingSpace.route)
                }
            )
        }

        // 주차 공간 등록 화면
        composable(Screen.AddParkingSpace.route) {
            AddParkingSpaceScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}
