package com.learnmate.ai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.learnmate.ai.ui.theme.DeepBlueCard
import com.learnmate.ai.ui.theme.ElectricBlue
import com.learnmate.ai.ui.theme.TextPrimary
import com.learnmate.ai.ui.theme.TextTertiary

sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavItem("dashboard", "Home", Icons.Default.Dashboard)
    object SkillAnalysis : NavItem("skills", "Skills", Icons.Default.QueryStats)
    object Roadmap : NavItem("roadmap", "Roadmap", Icons.Default.Map)
    object Planner : NavItem("planner", "Plan", Icons.Default.CalendarMonth)
    object Tutor : NavItem("tutor", "AI Tutor", Icons.Default.AutoAwesome)
    object Quiz : NavItem("quiz", "Quiz", Icons.Default.Quiz)
}

@Composable
fun LearnMateBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem.Dashboard,
        NavItem.SkillAnalysis,
        NavItem.Roadmap,
        NavItem.Planner,
        NavItem.Tutor
    )

    NavigationBar(
        containerColor = DeepBlueCard
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricBlue,
                    selectedTextColor = ElectricBlue,
                    indicatorColor = DeepBlueCard,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary
                )
            )
        }
    }
}
