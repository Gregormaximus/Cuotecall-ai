package com.kuote.agent

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuote.agent.ui.MainViewModel
import com.kuote.agent.ui.components.AddServiceDialog
import com.kuote.agent.ui.components.KuoteBottomBar
import com.kuote.agent.ui.components.KuoteTopBar
import com.kuote.agent.ui.components.RevenueCatPaywallDialog
import com.kuote.agent.ui.components.WebRtcVoiceSimDialog
import com.kuote.agent.ui.screens.AccountScreen
import com.kuote.agent.ui.screens.AuthScreen
import com.kuote.agent.ui.screens.DashboardScreen
import com.kuote.agent.ui.screens.JobDetailScreen
import com.kuote.agent.ui.screens.ProfileScreen
import com.kuote.agent.ui.screens.ScheduleScreen
import com.kuote.agent.ui.screens.ServiceCatalogScreen
import com.kuote.agent.ui.screens.SiteBuilderScreen
import com.kuote.agent.ui.theme.KuoteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val revenueCatState by viewModel.revenueCatState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Permissions result handled by system */ }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.READ_PHONE_STATE,
                        android.Manifest.permission.READ_CALL_LOG,
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.SEND_SMS,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                )
                
                // Start the background service to monitor for calls
                com.kuote.agent.service.MissedCallAgentService.startService(this@MainActivity)
                
                // Initialize Contact Cache
                com.kuote.agent.util.ContactCache.init(this@MainActivity)
            }

            LaunchedEffect(uiState.toastMessage) {
                uiState.toastMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }

            KuoteTheme(darkTheme = uiState.isDarkMode) {
                val isSignedIn = uiState.userState.isSignedIn

                Scaffold(
                    topBar = {
                        if (isSignedIn && uiState.selectedJobForDetail == null) {
                            KuoteTopBar(
                                isAgentActive = uiState.isAgentActive,
                                onToggleAgent = { viewModel.toggleAgentActive(it) },
                                onAccountClick = { viewModel.selectTab(4) }
                            )
                        }
                    },
                    bottomBar = {
                        if (isSignedIn && uiState.selectedJobForDetail == null) {
                            KuoteBottomBar(
                                selectedTab = uiState.selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isSignedIn) innerPadding else PaddingValues(0.dp))
                    ) {
                        if (!isSignedIn) {
                            AuthScreen(
                                onSignInWithGoogle = { viewModel.signInWithGoogle(this@MainActivity) },
                                onSignInAsGuest = { viewModel.signInAsGuest() },
                                onSignInWithEmail = { email, pass -> viewModel.signInWithEmail(email, pass) },
                                onSignUpWithEmail = { email, pass -> viewModel.signUpWithEmail(email, pass) }
                            )
                        } else if (uiState.selectedJobForDetail != null) {
                            JobDetailScreen(
                                job = uiState.selectedJobForDetail!!,
                                dispatches = uiState.dispatches,
                                onBack = { viewModel.selectJobForDetail(null) },
                                onSettleJobWithSavedCard = { viewModel.settleJobWithSavedCard(it) },
                                onSettleJobDynamicLink = { viewModel.settleJobDynamicLink(it) },
                                onSettleJobNfc = { viewModel.settleJobNfc(it) },
                                onSettleJobManualCard = { job, card, m, y, cvc ->
                                    viewModel.settleJobManualCard(job, card, m, y, cvc)
                                },
                                onSettleJobExternal = { job, method ->
                                    viewModel.settleJobExternal(job, method)
                                }
                            )
                        } else {
                            when (uiState.selectedTab) {
                                0 -> DashboardScreen(
                                    quotes = uiState.quotes,
                                    jobs = uiState.jobs,
                                    analyticsStats = uiState.analyticsStats,
                                    recentConversations = uiState.recentConversations,
                                    onApproveQuote = { viewModel.approveAndSendStripeQuote(it) },
                                    onDeclineQuote = { viewModel.declineQuote(it) },
                                    onSelectJob = { viewModel.selectJobForDetail(it) },
                                    onSimulateMissedCall = { viewModel.triggerSimulatedMissedCall() },
                                    onRecordSiteClick = { viewModel.recordMicroSiteClick() }
                                )
                                1 -> ScheduleScreen(
                                    jobs = uiState.jobs,
                                    onSelectJob = { viewModel.selectJobForDetail(it) },
                                    onSyncCalendar = { viewModel.syncGoogleCalendar() }
                                )
                                2 -> SiteBuilderScreen(
                                    config = uiState.webConfig,
                                    promptText = uiState.sitePromptText,
                                    isBuilding = uiState.isBuildingSite,
                                    onPromptChange = { viewModel.setSitePromptText(it) },
                                    onGenerate = { viewModel.generateMicroSite(it) },
                                    onOpenVoiceSim = { viewModel.toggleWebRtcSim(true) }
                                )
                                3 -> ProfileScreen(
                                    profile = uiState.companyProfile,
                                    onSaveProfile = { name, industry, sms, deposit, base ->
                                        viewModel.updateCompanyProfile(name, industry, sms, deposit, base)
                                    }
                                )
                                4 -> ServiceCatalogScreen(
                                    services = uiState.services,
                                    onAddServiceClick = { viewModel.setAddServiceDialogOpen(true) },
                                    onDeleteService = { viewModel.deleteService(it) }
                                )
                                5 -> AccountScreen(
                                    profile = uiState.companyProfile,
                                    revenueCatState = revenueCatState,
                                    userState = uiState.userState,
                                    isDarkMode = uiState.isDarkMode,
                                    onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                                    onSignInWithGoogle = { viewModel.signInWithGoogle(this@MainActivity) },
                                    onSignInAsGuest = { viewModel.signInAsGuest() },
                                    onSignOut = { viewModel.signOut() },
                                    onOpenPaywall = { viewModel.openPaywall() },
                                    onSimulateMissedCall = { viewModel.triggerSimulatedMissedCall() }
                                )
                            }
                        }

                        // Paywall Overlay Dialog
                        if (uiState.isPaywallOpen || revenueCatState.isPaywallVisible) {
                            RevenueCatPaywallDialog(
                                onDismiss = { viewModel.closePaywall() },
                                onStartTrial = { viewModel.purchaseSubscription() },
                                monthlyPriceText = revenueCatState.monthlyPriceText
                            )
                        }

                        // WebRTC Live Voice AI Simulation Dialog
                        if (uiState.isWebRtcSimOpen) {
                            WebRtcVoiceSimDialog(
                                config = uiState.webConfig,
                                onDismiss = { viewModel.toggleWebRtcSim(false) },
                                onDepositPay = {
                                    Toast.makeText(this@MainActivity, "Deposit Paid! Calendar Slot Secured.", Toast.LENGTH_LONG).show()
                                    viewModel.toggleWebRtcSim(false)
                                }
                            )
                        }

                        // Add Service Dialog
                        if (uiState.isAddServiceDialogOpen) {
                            AddServiceDialog(
                                onDismiss = { viewModel.setAddServiceDialogOpen(false) },
                                onAdd = { name, cat, base, rate, kw ->
                                    viewModel.addService(name, cat, base, rate, kw)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
