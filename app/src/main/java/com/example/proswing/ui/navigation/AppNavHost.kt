package com.example.proswing.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.proswing.ui.screens.*
import kotlinx.coroutines.launch
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    // Drawer items with icons
    val drawerItems = listOf(
        NavDrawerItem("Home", Destinations.HOME, Icons.Default.Home),
        NavDrawerItem("Learn", Destinations.LEARN, Icons.Default.Info),
        NavDrawerItem("Analyse", Destinations.ANALYSE, Icons.Default.Person),
        NavDrawerItem("Settings", Destinations.SETTINGS, Icons.Default.Settings)
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
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                // Navigate and update route selection
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = colors.onPrimaryContainer, // background highlight
                            unselectedContainerColor = colors.primary,         // background normal
                            selectedIconColor = colors.onPrimary,               // icon when selected
                            unselectedIconColor = colors.onPrimary.copy(alpha = 0.5f), // icon when unselected
                            selectedTextColor = colors.onPrimary,               // text when selected
                            unselectedTextColor = colors.onPrimary.copy(alpha = 0.5f)  // text when unselected
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
                            containerColor = MaterialTheme.colorScheme.primary,     // background color
                            titleContentColor = MaterialTheme.colorScheme.onPrimary, // title text color
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary // icon color
                        )
                    )
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
