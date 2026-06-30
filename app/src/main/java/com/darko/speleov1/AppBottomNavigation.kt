package com.darko.speleov1

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun SovBottomNavigationBar(
    currentTab: AppTab,
    onSearch: () -> Unit,
    onMap: () -> Unit,
    onFieldPackages: () -> Unit,
    onTools: () -> Unit,
    onOffline: () -> Unit
) {
    val language = LocalAppLanguage.current
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = currentTab == AppTab.SEARCH,
            onClick = onSearch,
            colors = sovNavigationItemColors("search"),
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(language.pick("Pretraga", "Search")) }
        )
        NavigationBarItem(
            selected = currentTab == AppTab.MAP,
            onClick = onMap,
            colors = sovNavigationItemColors("map karta"),
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            label = { Text(language.pick("Karta", "Map")) }
        )
        NavigationBarItem(
            selected = currentTab == AppTab.FIELD_PACKAGES,
            onClick = onFieldPackages,
            colors = sovNavigationItemColors("field package trip izleti"),
            icon = { Icon(Icons.Default.Event, contentDescription = null) },
            label = { Text(language.pick("Izleti", "Trips")) }
        )
        NavigationBarItem(
            selected = currentTab == AppTab.TOOLS || currentTab == AppTab.CALCULATOR || currentTab == AppTab.SPELEO_RUNNER,
            onClick = onTools,
            colors = sovNavigationItemColors("tools kalkulator"),
            icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
            label = { Text(language.pick("Alati", "Tools")) }
        )
        NavigationBarItem(
            selected = currentTab == AppTab.OFFLINE,
            onClick = onOffline,
            colors = sovNavigationItemColors("offline overlay"),
            icon = { Icon(Icons.Default.Map, contentDescription = null) },
            label = { Text(language.pick("Slojevi", "Layers")) }
        )
    }
}

@Composable
private fun sovNavigationItemColors(key: String) = NavigationBarItemDefaults.colors(
    selectedIconColor = premiumIconTint(key, active = true),
    selectedTextColor = premiumIconTint(key, active = true),
    indicatorColor = premiumIconContainer(key, active = true),
    unselectedIconColor = premiumIconTint(key).copy(alpha = 0.82f),
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)
