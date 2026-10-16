package com.gymbro.mad

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymbro.mad.feature.body.BodyScreen
import com.gymbro.mad.feature.cardio.CardioScreen
import com.gymbro.mad.feature.deck.DeckScreen
import com.gymbro.mad.feature.fuel.FoodSearchScreen
import com.gymbro.mad.feature.fuel.FuelScreen
import com.gymbro.mad.feature.history.HistoryScreen
import com.gymbro.mad.feature.history.SessionDetailScreen
import com.gymbro.mad.feature.profile.ProfileScreen
import com.gymbro.mad.feature.stats.StatsScreen
import com.gymbro.mad.feature.train.ExercisePickerScreen
import com.gymbro.mad.feature.train.TrainScreen
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.nav.GymBroBottomBar
import com.gymbro.mad.ui.nav.GymBroTopBar
import com.gymbro.mad.ui.nav.InnerRoutes
import com.gymbro.mad.ui.nav.Tab
import com.gymbro.mad.ui.theme.GymBroTheme
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.ThemeId

/**
 * The whole app: theme, chrome, and one navigation graph.
 *
 * Every route in the app is declared here and nowhere else, so adding a screen
 * is one entry in this file plus one file of your own. Screens receive
 * `onOpenX` lambdas rather than the NavController itself - a screen that can
 * navigate anywhere is a screen you cannot preview or test, and four people
 * calling navigate() from inside their own features is how a back stack stops
 * making sense.
 */
@Composable
fun GymBroApp() {
    // Resets on relaunch by design: persisting it is ticket A-6, and a
    // half-persisted setting is worse than an obviously temporary one.
    var themeId by rememberSaveable { mutableStateOf(ThemeId.MOLTEN) }

    GymBroTheme(themeId) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val route = backStackEntry?.destination?.route
        val palette = LocalPalette.current

        Scaffold(
            containerColor = palette.bg,
            topBar = {
                GymBroTopBar(
                    route = route,
                    onToggleTheme = { themeId = themeId.next() },
                    onOpenProfile = { navController.open(Routes.PROFILE) },
                    onBack = if (route in InnerRoutes) {
                        { navController.popBackStack() }
                    } else {
                        null
                    },
                )
            },
            bottomBar = {
                GymBroBottomBar(
                    current = Tab.ofRoute(route),
                    onSelect = { tab -> navController.switchTab(tab) },
                )
            },
        ) { insets ->
            NavHost(
                navController = navController,
                startDestination = Routes.DECK,
                modifier = Modifier.padding(insets),
            ) {
                composable(Routes.DECK) {
                    DeckScreen(
                        onOpenTrain = { navController.switchTab(Tab.TRAIN) },
                        onOpenFuel = { navController.switchTab(Tab.FUEL) },
                        onOpenBody = { navController.switchTab(Tab.BODY) },
                        onOpenCardio = { navController.open(Routes.CARDIO) },
                    )
                }

                composable(Routes.TRAIN) {
                    TrainScreen(onPickExercise = { navController.open(Routes.EXERCISE_PICKER) })
                }
                composable(Routes.EXERCISE_PICKER) {
                    ExercisePickerScreen(
                        // P-1 hands the chosen exercise back; until the session
                        // state machine in P-3 exists there is nowhere to put it,
                        // so for now picking just returns to Train.
                        onPicked = { navController.popBackStack() },
                    )
                }

                composable(Routes.FUEL) {
                    FuelScreen(onFindFood = { navController.open(Routes.FOOD_SEARCH) })
                }
                composable(Routes.FOOD_SEARCH) {
                    FoodSearchScreen(onPicked = { navController.popBackStack() })
                }

                composable(Routes.BODY) {
                    BodyScreen(onOpenExercises = { navController.open(Routes.EXERCISE_PICKER) })
                }

                composable(Routes.STATS) { StatsScreen() }

                composable(Routes.HISTORY) {
                    HistoryScreen(
                        onOpenSession = { id -> navController.open(Routes.sessionDetail(id)) },
                    )
                }
                composable(Routes.SESSION_DETAIL) { entry ->
                    SessionDetailScreen(
                        sessionId = entry.arguments?.getString("sessionId").orEmpty(),
                    )
                }

                composable(Routes.CARDIO) { CardioScreen() }

                composable(Routes.PROFILE) {
                    ProfileScreen(themeId = themeId, onThemeChange = { themeId = it })
                }
            }
        }
    }
}

/**
 * Tab switching, with the back stack a tab bar is supposed to have: one entry
 * per tab at most, state kept per tab, and back from any tab landing on the
 * Deck rather than walking every tab you visited.
 */
private fun NavHostController.switchTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** A push onto the current tab: back returns to where you came from. */
private fun NavHostController.open(route: String) {
    navigate(route) { launchSingleTop = true }
}

private fun ThemeId.next(): ThemeId {
    val all = ThemeId.entries
    return all[(ordinal + 1) % all.size]
}
