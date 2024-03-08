// Copyright (c) 2024 Tailscale Inc & AUTHORS All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.tailscale.ipn

import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tailscale.ipn.mdm.MDMSettings
import com.tailscale.ipn.mdm.ShowHideSetting
import com.tailscale.ipn.mdm.ShowHideValue
import com.tailscale.ipn.ui.service.IpnManager
import com.tailscale.ipn.ui.theme.AppTheme
import com.tailscale.ipn.ui.view.AboutView
import com.tailscale.ipn.ui.view.BugReportView
import com.tailscale.ipn.ui.view.ExitNodePicker
import com.tailscale.ipn.ui.view.MainView
import com.tailscale.ipn.ui.view.MainViewNavigation
import com.tailscale.ipn.ui.view.PeerDetails
import com.tailscale.ipn.ui.view.Settings
import com.tailscale.ipn.ui.view.SettingsNav
import com.tailscale.ipn.ui.viewModel.ExitNodePickerViewModel
import com.tailscale.ipn.ui.viewModel.MainViewModel
import com.tailscale.ipn.ui.viewModel.PeerDetailsViewModel
import com.tailscale.ipn.ui.viewModel.SettingsViewModel


class MainActivity : ComponentActivity() {
    private val manager = IpnManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    val mainViewNav = MainViewNavigation(
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToPeerDetails = {
                                navController.navigate("peerDetails/${it.StableID}")
                            },
                            onNavigateToExitNodes = { navController.navigate("exitNodes") }
                    )

                    val settingsNav = SettingsNav(
                            onNavigateToBugReport = { navController.navigate("bugReport") },
                            onNavigateToAbout = { navController.navigate("about") }
                    )

                    composable("main") {
                        MainView(viewModel = MainViewModel(manager.model, manager.actions), navigation = mainViewNav)
                    }
                    composable("settings") {
                        Settings(SettingsViewModel(manager.model, manager.actions, settingsNav))
                    }
                    composable("exitNodes") {
                        ExitNodePicker(ExitNodePickerViewModel(manager.model))
                    }
                    composable("peerDetails/{nodeId}", arguments = listOf(navArgument("nodeId") { type = NavType.StringType })) {
                        PeerDetails(PeerDetailsViewModel(manager.model, nodeId = it.arguments?.getString("nodeId")
                                ?: ""))
                    }
                    composable("bugReport") {
                        BugReportView()
                    }
                    composable("about") {
                        AboutView()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val restrictionsManager = this.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        manager.mdmSettings = MDMSettings(restrictionsManager)
    }
}
