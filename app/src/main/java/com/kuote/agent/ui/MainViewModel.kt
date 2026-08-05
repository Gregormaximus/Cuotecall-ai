package com.kuote.agent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kuote.agent.ai.MicroSiteGeneratorEngine
import com.kuote.agent.ai.MultimodalIntakeEngine
import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.CompanyWebConfig
import com.kuote.agent.data.model.FieldService
import com.kuote.agent.data.model.Job
import com.kuote.agent.data.model.JobStatus
import com.kuote.agent.data.model.Quote
import com.kuote.agent.data.model.AnalyticsStats
import com.kuote.agent.data.model.ConversationLog
import com.kuote.agent.data.model.DispatchGpsData
import com.kuote.agent.data.model.DayActivity
import com.kuote.agent.data.model.SettlementMethod
import com.kuote.agent.data.repository.AuthRepository
import com.kuote.agent.data.repository.CalendarRepository
import com.kuote.agent.data.repository.KuoteRepository
import com.kuote.agent.data.repository.StripeRepository
import com.kuote.agent.data.repository.UserState
import com.kuote.agent.monetization.RevenueCatManager
import com.kuote.agent.service.MissedCallAgentService
import com.kuote.agent.service.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class MainUiState(
    val companyProfile: CompanyProfile = CompanyProfile(),
    val services: List<FieldService> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val analyticsStats: AnalyticsStats = AnalyticsStats(),
    val recentConversations: List<ConversationLog> = emptyList(),
    val dispatches: List<DispatchGpsData> = emptyList(),
    val selectedJobForDetail: Job? = null,
    val webConfig: CompanyWebConfig = CompanyWebConfig(),
    val isAgentActive: Boolean = true,
    val isAnalyzingAi: Boolean = false,
    val isBuildingSite: Boolean = false,
    val selectedTab: Int = 0, // 0: Dashboard, 1: Schedule, 2: Site, 3: Catalog, 4: Account
    val isPaywallOpen: Boolean = false,
    val isWebRtcSimOpen: Boolean = false,
    val isAddServiceDialogOpen: Boolean = false,
    val searchQuery: String = "",
    val sitePromptText: String = "Create a sleek dark cyan landing page for my Towing business with a big WebRTC Voice Call button.",
    val isVoiceRecording: Boolean = false,
    val toastMessage: String? = null,
    val userState: UserState = UserState(),
    val isDarkMode: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KuoteRepository(application)
    private val stripeRepository = StripeRepository(application)
    private val calendarRepository = CalendarRepository(application)
    private val authRepository = AuthRepository(application)
    private val revenueCatManager = RevenueCatManager.getInstance()
    private val multimodalEngine = MultimodalIntakeEngine()
    private val siteEngine = MicroSiteGeneratorEngine()
    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val revenueCatState = revenueCatManager.state

    init {
        revenueCatManager.initialize(application)
        
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
        }

        viewModelScope.launch {
            repository.companyProfileFlow.collect { profile ->
                if (profile != null) {
                    _uiState.update { 
                        it.copy(
                            companyProfile = profile,
                            isAgentActive = profile.isAgentActive
                        ) 
                    }
                    if (profile.isAgentActive) {
                        MissedCallAgentService.startService(getApplication())
                    } else {
                        MissedCallAgentService.stopService(getApplication())
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.servicesFlow.collect { list ->
                _uiState.update { it.copy(services = list) }
            }
        }

        viewModelScope.launch {
            repository.quotesFlow.collect { list ->
                _uiState.update { it.copy(quotes = list) }
            }
        }

        viewModelScope.launch {
            repository.jobsFlow.collect { list ->
                _uiState.update { current ->
                    // Update selected job reference if currently open
                    val updatedSel = current.selectedJobForDetail?.let { sel -> list.find { it.id == sel.id } }
                    current.copy(jobs = list, selectedJobForDetail = updatedSel ?: current.selectedJobForDetail)
                }
            }
        }

        viewModelScope.launch {
            repository.webConfigFlow.collect { config ->
                if (config != null) {
                    _uiState.update { it.copy(webConfig = config) }
                }
            }
        }

        viewModelScope.launch {
            authRepository.userStateFlow.collect { user ->
                _uiState.update { it.copy(userState = user) }
            }
        }

        viewModelScope.launch {
            authRepository.getAnalyticsStatsFlow().collect { stats ->
                _uiState.update { it.copy(analyticsStats = stats) }
            }
        }

        viewModelScope.launch {
            authRepository.getConversationsFlow().collect { conversations ->
                _uiState.update { it.copy(recentConversations = conversations) }
            }
        }

        viewModelScope.launch {
            authRepository.getDispatchesFlow().collect { dispatchesList ->
                _uiState.update { it.copy(dispatches = dispatchesList) }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex, selectedJobForDetail = null) }
    }

    fun selectJobForDetail(job: Job?) {
        _uiState.update { it.copy(selectedJobForDetail = job) }
    }

    fun toggleAgentActive(active: Boolean) {
        viewModelScope.launch {
            repository.toggleAgentActive(active)
            if (active) {
                MissedCallAgentService.startService(getApplication())
            } else {
                MissedCallAgentService.stopService(getApplication())
            }
        }
    }

    fun updateCompanyProfile(name: String, industry: String, smsTemplate: String, deposit: Double, baseFee: Double) {
        viewModelScope.launch {
            val updated = _uiState.value.companyProfile.copy(
                name = name,
                industry = industry,
                autoSmsTemplate = smsTemplate,
                defaultDeposit = deposit,
                baseServiceFee = baseFee
            )
            repository.saveCompanyProfile(updated)
            authRepository.syncCompanyProfileToFirestore(updated)
            _uiState.update { it.copy(toastMessage = "Profile saved & synced to Firestore!") }
        }
    }

    fun addService(name: String, category: String, basePrice: Double, ratePerMile: Double, keywords: String) {
        viewModelScope.launch {
            val keywordList = keywords.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val newService = FieldService(
                id = "s_" + UUID.randomUUID().toString().take(6),
                name = name,
                category = category,
                basePrice = basePrice,
                ratePerMile = ratePerMile,
                aiKeywords = keywordList,
                status = "ACTIVE"
            )
            repository.saveService(newService)
            _uiState.update { it.copy(isAddServiceDialogOpen = false, toastMessage = "Service '$name' added!") }
        }
    }

    fun deleteService(service: FieldService) {
        viewModelScope.launch {
            repository.deleteService(service)
            _uiState.update { it.copy(toastMessage = "Service removed") }
        }
    }

    fun approveAndSendStripeQuote(quote: Quote) {
        viewModelScope.launch {
            repository.updateQuoteStatus(quote.id, "APPROVED")
            val link = quote.stripePaymentLink ?: "https://checkout.stripe.com/pay/default?amt=${quote.requiredDeposit}"
            val message = "Your ${quote.serviceCategory} estimate is ready: $${quote.estimatedTotal}. Secure your spot with a $${quote.requiredDeposit} deposit: $link"
            notificationHelper.sendInstantSms(quote.customerPhone, message)

            // Convert quote into an Active Job upon deposit approval
            val newJob = Job(
                id = "job_" + quote.id.takeLast(6),
                customerName = "Customer " + quote.customerPhone.takeLast(4),
                customerPhone = quote.customerPhone,
                customerLocation = quote.customerLocation,
                serviceTitle = "${quote.serviceCategory} Service Call",
                serviceCategory = quote.serviceCategory,
                status = JobStatus.DEPOSIT_PAID,
                estimatedTotal = quote.estimatedTotal,
                depositAmount = quote.requiredDeposit,
                notes = quote.aiSummary
            )
            repository.saveJob(newJob)
            
            // Sync 2-Way Google Calendar Event upon deposit
            val calResult = calendarRepository.createJobCalendarEvent(newJob)

            val currentStats = _uiState.value.analyticsStats
            val updatedMonthlyTrend = currentStats.monthlyRevenueTrend.mapIndexed { idx, item ->
                if (idx == currentStats.monthlyRevenueTrend.lastIndex) {
                    item.copy(
                        depositRevenue = item.depositRevenue + quote.requiredDeposit,
                        totalRevenue = item.totalRevenue + quote.requiredDeposit,
                        totalJobsCount = item.totalJobsCount + 1
                    )
                } else {
                    item
                }
            }
            val updatedStats = currentStats.copy(
                totalDepositsCollected = currentStats.totalDepositsCollected + quote.requiredDeposit,
                monthlyRevenueTrend = updatedMonthlyTrend
            )
            _uiState.update { it.copy(analyticsStats = updatedStats, toastMessage = "Approved! Deposit link sent & Google Calendar event created.") }
            authRepository.syncAnalyticsToFirestore(updatedStats)

            // Log conversation update to Firestore
            authRepository.addConversationLog(
                ConversationLog(
                    id = "conv_" + quote.id.takeLast(6),
                    customerPhone = quote.customerPhone,
                    customerName = "Customer " + quote.customerPhone.takeLast(4),
                    lastSmsText = message,
                    generatedQuoteAmount = quote.estimatedTotal,
                    depositAmount = quote.requiredDeposit,
                    status = "DEPOSIT_PAID",
                    timestamp = System.currentTimeMillis(),
                    serviceCategory = quote.serviceCategory
                )
            )
        }
    }

    fun syncGoogleCalendar() {
        viewModelScope.launch {
            val slots = calendarRepository.getAvailableTimeSlots("Today")
            _uiState.update { it.copy(toastMessage = "Google Calendar synced! ${slots.size} free slots available.") }
        }
    }

    fun declineQuote(quote: Quote) {
        viewModelScope.launch {
            repository.updateQuoteStatus(quote.id, "DECLINED")
            _uiState.update { it.copy(toastMessage = "Quote declined") }
        }
    }

    // Stripe Connect 5 Settlement Handler Functions
    fun settleJobWithSavedCard(job: Job) {
        viewModelScope.launch {
            val result = stripeRepository.settleWithSavedCard(job, _uiState.value.companyProfile.stripeAccountId)
            if (result.success) {
                repository.settleJob(
                    jobId = job.id,
                    status = result.status,
                    method = SettlementMethod.SAVED_CARD,
                    extType = null,
                    piId = result.paymentIntentId,
                    receipt = result.receiptUrl
                )
                _uiState.update { it.copy(toastMessage = result.message) }
            }
        }
    }

    fun settleJobDynamicLink(job: Job) {
        viewModelScope.launch {
            val link = stripeRepository.generateDynamicPaymentLink(job, _uiState.value.companyProfile.stripeAccountId)
            val smsText = "Your final balance of $${job.balanceDue} for ${job.serviceTitle} is ready. Tap to pay via Apple Pay, Google Pay, or Card: $link"
            notificationHelper.sendInstantSms(job.customerPhone, smsText)
            
            repository.settleJob(
                jobId = job.id,
                status = JobStatus.COMPLETED_PAID_STRIPE,
                method = SettlementMethod.DYNAMIC_QR_LINK,
                extType = null,
                piId = "pi_link_" + UUID.randomUUID().toString().take(6),
                receipt = link
            )
            _uiState.update { it.copy(toastMessage = "Dynamic Stripe Payment Link dispatched via SMS!") }
        }
    }

    fun settleJobNfc(job: Job) {
        viewModelScope.launch {
            val result = stripeRepository.initiateTapToPayNfc(job, _uiState.value.companyProfile.stripeAccountId)
            if (result.success) {
                repository.settleJob(
                    jobId = job.id,
                    status = result.status,
                    method = SettlementMethod.TAP_TO_PAY_NFC,
                    extType = null,
                    piId = result.paymentIntentId,
                    receipt = result.receiptUrl
                )
                _uiState.update { it.copy(toastMessage = result.message) }
            }
        }
    }

    fun settleJobManualCard(job: Job, cardNumber: String, expMonth: Int, expYear: Int, cvc: String) {
        viewModelScope.launch {
            val result = stripeRepository.processManualKeyedCard(
                job, cardNumber, expMonth, expYear, cvc, _uiState.value.companyProfile.stripeAccountId
            )
            if (result.success) {
                repository.settleJob(
                    jobId = job.id,
                    status = result.status,
                    method = SettlementMethod.MANUAL_KEYED_CARD,
                    extType = null,
                    piId = result.paymentIntentId,
                    receipt = result.receiptUrl
                )
                _uiState.update { it.copy(toastMessage = result.message) }
            }
        }
    }

    fun settleJobExternal(job: Job, externalMedium: String) {
        viewModelScope.launch {
            val result = stripeRepository.settlePaidExternally(job, externalMedium)
            if (result.success) {
                repository.settleJob(
                    jobId = job.id,
                    status = result.status,
                    method = SettlementMethod.EXTERNAL_CASH_VENMO_CHECK,
                    extType = externalMedium,
                    piId = null,
                    receipt = result.receiptUrl
                )
                val smsReceipt = "Receipt from ${_uiState.value.companyProfile.name}: Job #${job.id.takeLast(6).uppercase()} Paid in Full ($${job.balanceDue}) via $externalMedium. Thank you!"
                notificationHelper.sendInstantSms(job.customerPhone, smsReceipt)
                _uiState.update { it.copy(toastMessage = "Job marked paid via $externalMedium. SMS Receipt sent!") }
            }
        }
    }

    fun generateMicroSite(prompt: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuildingSite = true) }
            val config = siteEngine.generateMicroSiteConfig(prompt, _uiState.value.companyProfile)
            repository.saveWebConfig(config)
            authRepository.syncWebConfigToFirestore(config, _uiState.value.companyProfile)
            _uiState.update { it.copy(isBuildingSite = false, toastMessage = "AI Micro-Site Deployed & Persisted to Firestore!") }
        }
    }

    fun triggerSimulatedMissedCall(
        phone: String = "+16304480230",
        location: String = "Redwood City",
        customerNote: String = "Need towing for flat tire on Highway 101."
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingAi = true) }
            val company = repository.getCompanyProfileDirect()
            val services = repository.getServicesDirect()

            // Send instant simulated SMS
            val slug = company.name.lowercase().trim().replace(Regex("[^a-z0-9]+"), "-").removeSuffix("-")
            val smsMessage = "${company.autoSmsTemplate} https://quotecall.ai/$slug"
            notificationHelper.sendInstantSms(phone, smsMessage)

            // Multimodal AI analysis
            val quote = multimodalEngine.analyzeCustomerRequest(
                customerPhone = phone,
                location = location,
                textInput = customerNote,
                companyProfile = company,
                services = services
            )

            repository.saveQuote(quote)
            notificationHelper.showQuoteNotification(quote, company.name)

            // Update Analytics in Firestore
            val currentStats = _uiState.value.analyticsStats
            val newCalls = currentStats.missedCallsHandled + 1
            val newSms = currentStats.autoSmsSent + 1
            val newCtr = if (newSms > 0) (currentStats.microSiteClicks.toDouble() / newSms.toDouble() * 100.0) else 0.0
            val updatedStats = currentStats.copy(
                missedCallsHandled = newCalls,
                autoSmsSent = newSms,
                ctrPercentage = newCtr
            )
            _uiState.update { it.copy(analyticsStats = updatedStats, isAnalyzingAi = false, toastMessage = "Simulated missed call processed by Gemini AI!") }
            authRepository.syncAnalyticsToFirestore(updatedStats)

            // Log conversation in Firestore
            authRepository.addConversationLog(
                ConversationLog(
                    id = "conv_" + quote.id.takeLast(6),
                    customerPhone = phone,
                    customerName = "Caller " + phone.takeLast(4),
                    lastSmsText = smsMessage,
                    generatedQuoteAmount = quote.estimatedTotal,
                    depositAmount = quote.requiredDeposit,
                    status = "SENT_SMS",
                    timestamp = System.currentTimeMillis(),
                    serviceCategory = quote.serviceCategory
                )
            )
        }
    }

    fun recordMicroSiteClick() {
        viewModelScope.launch {
            val currentStats = _uiState.value.analyticsStats
            val newClicks = currentStats.microSiteClicks + 1
            val newSms = currentStats.autoSmsSent
            val newCtr = if (newSms > 0) (newClicks.toDouble() / newSms.toDouble() * 100.0) else 100.0
            val updatedStats = currentStats.copy(
                microSiteClicks = newClicks,
                ctrPercentage = newCtr
            )
            _uiState.update { it.copy(analyticsStats = updatedStats, toastMessage = "Micro-Site Link Clicked! CTR updated.") }
            authRepository.syncAnalyticsToFirestore(updatedStats)
        }
    }

    fun openPaywall() {
        revenueCatManager.showPaywall()
        _uiState.update { it.copy(isPaywallOpen = true) }
    }

    fun closePaywall() {
        revenueCatManager.hidePaywall()
        _uiState.update { it.copy(isPaywallOpen = false) }
    }

    fun purchaseSubscription() {
        revenueCatManager.purchaseProAccess {
            _uiState.update { it.copy(isPaywallOpen = false, toastMessage = "Unlocked QuoteCall Pro Access!") }
        }
    }

    fun toggleWebRtcSim(open: Boolean) {
        _uiState.update { it.copy(isWebRtcSimOpen = open) }
    }

    fun setAddServiceDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddServiceDialogOpen = open) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleDarkMode(darkMode: Boolean) {
        _uiState.update { it.copy(isDarkMode = darkMode) }
    }

    fun setSitePromptText(prompt: String) {
        _uiState.update { it.copy(sitePromptText = prompt) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun signInWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle()
            result.onSuccess { user ->
                _uiState.update { it.copy(toastMessage = "Welcome, ${user.displayName}! Signed in with Firebase.") }
            }.onFailure { err ->
                _uiState.update { it.copy(toastMessage = "${err.message}") }
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, pass)
            result.onSuccess { user ->
                _uiState.update { it.copy(toastMessage = "Signed in as ${user.email}") }
            }.onFailure { err ->
                _uiState.update { it.copy(toastMessage = "Login error: ${err.localizedMessage ?: "Invalid credentials"}") }
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, pass)
            result.onSuccess { user ->
                _uiState.update { it.copy(toastMessage = "Account created & signed in as ${user.email}") }
            }.onFailure { err ->
                _uiState.update { it.copy(toastMessage = "Sign up error: ${err.localizedMessage ?: "Registration failed"}") }
            }
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            val result = authRepository.signInAnonymously()
            result.onSuccess { user ->
                _uiState.update { it.copy(toastMessage = "Signed in as Guest (${user.displayName})") }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update { it.copy(toastMessage = "Signed out of Firebase Auth") }
    }
}
