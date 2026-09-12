package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable object Splash : Screen()
    @Serializable object Welcome : Screen()
    @Serializable object Login : Screen()
    @Serializable object SignIn : Screen()
    @Serializable object CitizenDashboard : Screen()
    @Serializable object LawEnforcerDashboard : Screen()
    @Serializable object AdminDashboard : Screen()
    @Serializable object PostCriminal : Screen()
    @Serializable object SubmitReport : Screen()
    @Serializable object SubmitTip : Screen()
    @Serializable data class MapScreen(val latitude: Double? = null, val longitude: Double? = null) : Screen()
    @Serializable object SOSScreen : Screen()
    @Serializable object Profile : Screen()
    @Serializable object Analytics : Screen()
    @Serializable object PublicViewing : Screen()
    @Serializable object About : Screen()
    @Serializable object EmergencyContacts : Screen()
}
