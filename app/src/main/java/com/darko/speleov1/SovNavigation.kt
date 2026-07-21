package com.darko.speleov1

object SovAppRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val MAP = "map"
    const val CLOUD = "cloud"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val LOGIN = "login"
    const val CALCULATOR = "calculator"
    const val SPELEO_RUNNER = "speleo_runner"
    const val OFFLINE = "offline"
    const val FIELD_PACKAGES = "field_packages"
    const val GPS_STATUS = "gps_status"
    const val COMPASS_STATUS = "compass_status"
    const val SIGNAL_STATUS = "signal_status"
    const val BATTERY_STATUS = "battery_status"
    const val MY_BASE = "my_base"
    const val ORUZARSTVO = "oruzarstvo"
    const val ARHIVA_NACRTI = "arhiva_nacrti"
    const val ARHIVA_PREDANE_JAME = "arhiva_predane_jame"
    const val TOOLS_CALENDAR = "tools_calendar"
    const val LAPTOP_HUB = "laptop_hub"
}

val AppTab.route: String
    get() = when (this) {
        AppTab.HOME -> SovAppRoutes.HOME
        AppTab.SEARCH -> SovAppRoutes.SEARCH
        AppTab.MAP -> SovAppRoutes.MAP
        AppTab.CLOUD -> SovAppRoutes.CLOUD
        AppTab.TOOLS -> SovAppRoutes.TOOLS
        AppTab.SETTINGS -> SovAppRoutes.SETTINGS
        AppTab.LOGIN -> SovAppRoutes.LOGIN
        AppTab.CALCULATOR -> SovAppRoutes.CALCULATOR
        AppTab.SPELEO_RUNNER -> SovAppRoutes.SPELEO_RUNNER
        AppTab.OFFLINE -> SovAppRoutes.OFFLINE
        AppTab.FIELD_PACKAGES -> SovAppRoutes.FIELD_PACKAGES
    }

fun appTabFromRoute(route: String?): AppTab? = when (route) {
    SovAppRoutes.HOME -> AppTab.HOME
    SovAppRoutes.SEARCH -> AppTab.SEARCH
    SovAppRoutes.MAP -> AppTab.MAP
    SovAppRoutes.CLOUD -> AppTab.CLOUD
    SovAppRoutes.TOOLS -> AppTab.TOOLS
    SovAppRoutes.SETTINGS -> AppTab.SETTINGS
    SovAppRoutes.LOGIN -> AppTab.LOGIN
    SovAppRoutes.CALCULATOR -> AppTab.CALCULATOR
    SovAppRoutes.SPELEO_RUNNER -> AppTab.SPELEO_RUNNER
    SovAppRoutes.OFFLINE -> AppTab.OFFLINE
    SovAppRoutes.FIELD_PACKAGES -> AppTab.FIELD_PACKAGES
    else -> null
}
