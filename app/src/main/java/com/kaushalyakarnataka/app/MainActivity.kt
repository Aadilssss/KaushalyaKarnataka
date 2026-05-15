package com.kaushalyakarnataka.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.data.UserRole
import com.kaushalyakarnataka.app.ui.AppLanguage
import com.kaushalyakarnataka.app.ui.MainViewModel
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.screens.*
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import com.kaushalyakarnataka.app.ui.theme.KaushalyaTheme
import com.kaushalyakarnataka.app.ui.stringsFor
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val DASHBOARD = "dashboard"
    const val MY_HIRES = "my_hires"
    const val REQUESTS = "requests"
    const val CHATS = "chats"
    const val PROFILE = "profile"
    const val WORKER = "worker/{workerId}"
    const val CHAT = "chat/{chatId}"
}

private data class TabDest(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaushalyaTheme {
                val repo = remember { KaushalyaRepository() }
                val vm: MainViewModel = viewModel(factory = MainViewModel.factory(application, repo))
                val session by vm.session.collectAsStateWithLifecycle()
                val language by vm.language.collectAsStateWithLifecycle()
                val strings = remember(language) { stringsFor(language) }

                val snackbar = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                fun toast(msg: String) {
                    scope.launch { snackbar.showSnackbar(msg) }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when {
                            session.error != null -> {
                                ErrorState(
                                    message = session.error!!,
                                    onRetry = { vm.retrySync() }
                                )
                            }
                            
                            session.isAuthLoading || session.isProfileLoading -> {
                                LoadingState(strings.loading)
                            }

                            session.uid == null -> {
                                LoginScreen(
                                    strings = strings,
                                    onSignInWithGoogle = vm::signInWithGoogle,
                                    onMessage = ::toast,
                                )
                            }

                            session.profile == null && session.isNewUser -> {
                                val uid = session.uid ?: return@Box
                                val user = FirebaseAuth.getInstance().currentUser
                                OnboardingScreen(
                                    uid = uid,
                                    strings = strings,
                                    authDisplayName = user?.displayName,
                                    authPhotoUrl = user?.photoUrl?.toString(),
                                    onComplete = vm::completeOnboarding,
                                    onDone = { },
                                    onMessage = ::toast,
                                )
                            }

                            session.profile != null -> {
                                MainShell(
                                    profile = session.profile!!,
                                    strings = strings,
                                    language = language,
                                    repo = repo,
                                    vm = vm,
                                    onToast = ::toast,
                                )
                            }
                            
                            else -> {
                                LoadingState(strings.loading)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(label: String) {
    Box(Modifier.fillMaxSize().background(KaushalyaColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(label, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun MainShell(
    profile: UserProfile,
    strings: Strings,
    language: AppLanguage,
    repo: KaushalyaRepository,
    vm: MainViewModel,
    onToast: (String) -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route.orEmpty()
    val hideBottomNav = currentRoute.startsWith("worker/") || currentRoute.startsWith("chat/")

    val tabs = remember(profile.role, strings) {
        when (profile.role) {
            UserRole.customer -> listOf(
                TabDest(Routes.HOME, strings.home, Icons.Outlined.Home),
                TabDest(Routes.MY_HIRES, strings.myHires, Icons.AutoMirrored.Outlined.ListAlt),
                TabDest(Routes.CHATS, strings.chats, Icons.AutoMirrored.Outlined.Chat),
                TabDest(Routes.PROFILE, strings.profile, Icons.Outlined.Person),
            )
            UserRole.worker -> listOf(
                TabDest(Routes.DASHBOARD, strings.dashboard, Icons.Outlined.WorkOutline),
                TabDest(Routes.REQUESTS, strings.requests, Icons.AutoMirrored.Outlined.ListAlt),
                TabDest(Routes.CHATS, strings.chats, Icons.AutoMirrored.Outlined.Chat),
                TabDest(Routes.PROFILE, strings.profile, Icons.Outlined.Person),
            )
        }
    }

    val start = if (profile.role == UserRole.worker) Routes.DASHBOARD else Routes.HOME

    Scaffold(
        bottomBar = {
            if (!hideBottomNav) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, start, Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(profile, strings, language, { vm.setLanguage(if (language == AppLanguage.EN) AppLanguage.KN else AppLanguage.EN) }, repo, { navController.navigate("worker/$it") }) }
            composable(Routes.MY_HIRES) { MyHiresScreen(profile, strings, repo, { navController.navigate("chat/$it") }, onToast, vm::submitReview) }
            composable(Routes.DASHBOARD) { DashboardScreen(profile, strings, repo, { navController.navigate(Routes.REQUESTS) }, { navController.navigate("chat/$it") }, onToast) }
            composable(Routes.REQUESTS) { RequestsScreen(profile, strings, repo, vm::acceptRequest, vm::rejectRequest, vm::completeJob, onToast) }
            composable(Routes.CHATS) { ChatsScreen(profile, strings, repo, { navController.navigate("chat/$it") }) }
            composable(Routes.PROFILE) { ProfileScreen(profile, strings, language, repo, vm::updateProfile, vm::addService, vm::deleteService, { vm.setLanguage(if (language == AppLanguage.EN) AppLanguage.KN else AppLanguage.EN) }, { vm.signOut() }, onToast) }
            composable(Routes.WORKER, arguments = listOf(navArgument("workerId") { type = NavType.StringType })) { WorkerProfileScreen(it.arguments?.getString("workerId").orEmpty(), profile, strings, repo, { navController.popBackStack() }, { navController.navigate(Routes.MY_HIRES) }, onToast) }
            composable(Routes.CHAT, arguments = listOf(navArgument("chatId") { type = NavType.StringType })) { ChatDetailScreen(it.arguments?.getString("chatId").orEmpty(), profile, strings, repo, { navController.popBackStack() }) }
        }
    }
}
