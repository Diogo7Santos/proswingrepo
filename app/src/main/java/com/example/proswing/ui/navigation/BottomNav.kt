package com.example.proswing.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.proswing.ui.screens.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.example.proswing.R
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size



@Composable
fun BottomNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(Destinations.HOME) { HomeScreen() }
        composable(Destinations.ANALYSE) { AnalyseScreen() }
        composable(Destinations.LEARN) { LearnScreen() }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    // Temporary standard icons — you can replace these with your SVGs later
    val items = listOf(
        NavItem("Learn", R.drawable.ic_learn, Destinations.LEARN),
        NavItem("Home", R.drawable.ic_home, Destinations.HOME),
        NavItem("Analyse", R.drawable.ic_analyse, Destinations.ANALYSE)
    )

    val colors = MaterialTheme.colorScheme
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column {
        // 🔹 Divider to separate bottom bar from screen background
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            thickness = 4.dp
        )

        NavigationBar(
            containerColor = colors.primary,
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.label,
                            modifier = Modifier.size(30.dp)   // 🔹 standard Material size
                        )
                    },
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.onPrimary,            // selected icon tint
                        unselectedIconColor = colors.onPrimary.copy(alpha = 0.5f), // unselected icon tint
                        selectedTextColor = colors.onPrimary,            // selected text
                        unselectedTextColor = colors.onPrimary.copy(alpha = 0.5f), // unselected text
                        indicatorColor = colors.background               // indicator background
                    )
                )
            }
        }
    }
}

data class NavItem(
    val label: String,
    val iconRes: Int, // resource ID instead of ImageVector
    val route: String
)
