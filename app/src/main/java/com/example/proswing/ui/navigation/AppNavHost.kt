package com.example.proswing.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.proswing.R
import com.example.proswing.ui.screens.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val screenTitles = mapOf(
        Destinations.HOME to "Home",
        Destinations.LEARN to "Learn",
        Destinations.ANALYSE to "Analyse",
        Destinations.MYBAG to "My Bag",
        Destinations.YARDAGES to "Yardages",
        Destinations.SETTINGS to "Settings",
        Destinations.ANALYSE_EDITOR to "Analyse Editor"
    )

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    // Track current route
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // ✅ Lock drawer gestures ONLY on AnalyseEditor
    val drawerGesturesEnabled = currentRoute != Destinations.ANALYSE_EDITOR

    // ✅ If we enter AnalyseEditor, force-close the drawer just in case
    LaunchedEffect(currentRoute) {
        if (currentRoute == Destinations.ANALYSE_EDITOR && drawerState.isOpen) {
            drawerState.close()
        }
    }

    // Drawer items — using SVG icons and some built-in Material ones
    val drawerItems = listOf(
        NavDrawerItem("Home", Destinations.HOME, R.drawable.ic_home, true),
        NavDrawerItem("Learn", Destinations.LEARN, R.drawable.ic_learn, true),
        NavDrawerItem("Analyse", Destinations.ANALYSE, R.drawable.ic_analyse, true),
        NavDrawerItem("Caddie", Destinations.CADDIE, R.drawable.ic_caddie, true),
        NavDrawerItem("Handicap", Destinations.HANDICAP, R.drawable.ic_hcp, true),
        NavDrawerItem("Scorecard", Destinations.SCORECARD, R.drawable.ic_scorecard, true),
        NavDrawerItem("My Bag", Destinations.MYBAG, R.drawable.ic_mybag, true),
        NavDrawerItem("Distances", Destinations.YARDAGES, R.drawable.ic_yardages, isSvg = true),
        NavDrawerItem("Settings", Destinations.SETTINGS, null, false)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerGesturesEnabled, // ✅ this is the lock
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.primary,
                drawerContentColor = colors.onPrimary
            ) {
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onPrimary,
                    modifier = Modifier.padding(16.dp)
                )

                drawerItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationDrawerItem(
                        icon = {
                            if (item.isSvg && item.iconRes != null) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.label,
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = colors.onPrimaryContainer,
                            unselectedContainerColor = colors.primary,
                            selectedIconColor = colors.onPrimary,
                            unselectedIconColor = colors.onPrimary.copy(alpha = 0.6f),
                            selectedTextColor = colors.onPrimary,
                            unselectedTextColor = colors.onPrimary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = screenTitles[currentRoute] ?: "ProSwing",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    // ✅ optionally block opening drawer on AnalyseEditor too
                                    if (drawerGesturesEnabled) {
                                        scope.launch { drawerState.open() }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.primary,
                            titleContentColor = colors.onPrimary,
                            navigationIconContentColor = colors.onPrimary
                        )
                    )
                    Divider(
                        color = colors.outline.copy(alpha = 0.5f),
                        thickness = 4.dp
                    )
                }
            },
            bottomBar = { BottomNavigationBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = Destinations.HOME
                ) {
                    composable(Destinations.HOME) { HomeScreen() }
                    composable(Destinations.LEARN) { LearnScreen() }

                    composable(Destinations.ANALYSE) {
                        AnalyseScreen(
                            onAnalyseClick = { navController.navigate(Destinations.ANALYSE_EDITOR) }
                        )
                    }

                    composable(Destinations.ANALYSE_EDITOR) { AnalyseEditorScreen() }

                    composable(Destinations.CADDIE) { CaddieScreen() }
                    composable(Destinations.HANDICAP) { HandicapScreen() }
                    composable(Destinations.SCORECARD) { ScorecardScreen() }
                    composable(Destinations.MYBAG) { MyBagScreen() }
                    composable(Destinations.YARDAGES) { YardagesScreen() }
                    composable(Destinations.SETTINGS) { SettingsScreen() }
                }
            }
        }
    }
}

data class NavDrawerItem(
    val label: String,
    val route: String,
    val iconRes: Int?,
    val isSvg: Boolean
)
