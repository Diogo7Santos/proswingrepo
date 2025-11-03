package com.example.proswing.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    // Drawer items — use SVGs for first three, Material icon for Settings
    val drawerItems = listOf(
        NavDrawerItem("Home", Destinations.HOME, R.drawable.ic_home, isSvg = true),
        NavDrawerItem("Learn", Destinations.LEARN, R.drawable.ic_learn, isSvg = true),
        NavDrawerItem("Analyse", Destinations.ANALYSE, R.drawable.ic_analyse, isSvg = true),
        NavDrawerItem("Settings", Destinations.SETTINGS, null, isSvg = false)
    )

    // Observe the current route to highlight the selected item
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                            unselectedIconColor = colors.onPrimary.copy(alpha = 0.5f),
                            selectedTextColor = colors.onPrimary,
                            unselectedTextColor = colors.onPrimary.copy(alpha = 0.5f)
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
                        title = { Text("ProSwing") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
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
                    composable(Destinations.ANALYSE) { AnalyseScreen() }
                    composable(Destinations.SETTINGS) { SettingsScreen() }
                }
            }
        }
    }
}

// Data class for drawer items
data class NavDrawerItem(
    val label: String,
    val route: String,
    val iconRes: Int?, // nullable for non-SVG icons
    val isSvg: Boolean
)
