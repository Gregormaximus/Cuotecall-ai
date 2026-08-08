package com.kuote.agent.data.repository

import android.content.Context
import com.kuote.agent.data.local.KuoteDatabase
import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.CompanyWebConfig
import com.kuote.agent.data.model.FieldService
import com.kuote.agent.data.model.Job
import com.kuote.agent.data.model.JobStatus
import com.kuote.agent.data.model.Quote
import com.kuote.agent.data.model.SmsLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KuoteRepository(context: Context) {

    private val db = KuoteDatabase.getDatabase(context)
    private val profileDao = db.companyProfileDao()
    private val serviceDao = db.serviceDao()
    private val quoteDao = db.quoteDao()
    private val webConfigDao = db.webConfigDao()
    private val jobDao = db.jobDao()
    private val smsLogDao = db.smsLogDao()

    val companyProfileFlow: Flow<CompanyProfile?> = profileDao.getCompanyProfileFlow()
    val servicesFlow: Flow<List<FieldService>> = serviceDao.getAllServicesFlow()
    val quotesFlow: Flow<List<Quote>> = quoteDao.getAllQuotesFlow()
    val webConfigFlow: Flow<CompanyWebConfig?> = webConfigDao.getWebConfigFlow()
    val jobsFlow: Flow<List<Job>> = jobDao.getAllJobsFlow()
    val smsLogsFlow: Flow<List<SmsLog>> = smsLogDao.getAllSmsLogsFlow()

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val currentProfile = profileDao.getCompanyProfile()
        if (currentProfile == null || currentProfile.name == "My Business" || currentProfile.name == "Apex Electric Pros") {
            val defaultProfile = CompanyProfile(
                id = "tenant_starlink_batavia",
                name = "Skynet One",
                industry = "Starlink & Satellite Installation",
                autoSmsTemplate = "Sorry we missed your call from Skynet One! Tap link for instant quote & booking: ",
                defaultDeposit = 50.00,
                baseServiceFee = 100.00,
                stripeAccountId = "acct_starlink_batavia",
                isAgentActive = true,
                phone = "(630) 555-0199",
                hqAddress = "701 Branson Dr, Batavia, IL 60510"
            )
            profileDao.insertOrUpdateProfile(defaultProfile)
            
            // Purge legacy sample/dummy records for a fresh clean slate
            jobDao.deleteAllJobs()
            quoteDao.deleteAllQuotes()
            smsLogDao.clearAllSmsLogs()
            serviceDao.deleteAllServices()

            // Seed clean Starlink installation services
            serviceDao.insertService(
                FieldService(
                    id = "s_starlink_mount",
                    name = "Starlink Roof Mount Installation",
                    category = "SATELLITE",
                    basePrice = 200.0,
                    ratePerMile = 0.0,
                    aiKeywords = listOf("Starlink", "Roof Mount", "Dish Installation", "Mounting"),
                    status = "ACTIVE"
                )
            )
            serviceDao.insertService(
                FieldService(
                    id = "s_starlink_cable",
                    name = "Custom Cable Routing & Setup",
                    category = "SATELLITE",
                    basePrice = 75.0,
                    ratePerMile = 0.0,
                    aiKeywords = listOf("Cable Routing", "Wall Pass-through", "Ethernet Run", "Network Config"),
                    status = "ACTIVE"
                )
            )
        }

        if (webConfigDao.getWebConfigFlow("tenant_starlink_batavia") == null) {
            webConfigDao.insertOrUpdateWebConfig(
                CompanyWebConfig(
                    companyId = "tenant_starlink_batavia",
                    siteTitle = "Skynet One",
                    siteSubtitle = "Starlink & Satellite Installation - 701 Branson Dr, Batavia, IL 60510",
                    deployedUrl = "https://quotebit.app/?slug=skynet-one"
                )
            )
        }
    }

    suspend fun saveCompanyProfile(profile: CompanyProfile) = withContext(Dispatchers.IO) {
        profileDao.insertOrUpdateProfile(profile)
    }

    suspend fun toggleAgentActive(isActive: Boolean) = withContext(Dispatchers.IO) {
        val current = profileDao.getCompanyProfile() ?: CompanyProfile()
        profileDao.insertOrUpdateProfile(current.copy(isAgentActive = isActive))
    }

    suspend fun saveService(service: FieldService) = withContext(Dispatchers.IO) {
        serviceDao.insertService(service)
    }

    suspend fun deleteService(service: FieldService) = withContext(Dispatchers.IO) {
        serviceDao.deleteService(service)
    }

    suspend fun saveQuote(quote: Quote) = withContext(Dispatchers.IO) {
        quoteDao.insertQuote(quote)
    }

    suspend fun updateQuoteStatus(quoteId: String, status: String) = withContext(Dispatchers.IO) {
        quoteDao.updateQuoteStatus(quoteId, status)
    }

    suspend fun saveWebConfig(config: CompanyWebConfig) = withContext(Dispatchers.IO) {
        webConfigDao.insertOrUpdateWebConfig(config)
    }

    suspend fun saveJob(job: Job) = withContext(Dispatchers.IO) {
        jobDao.insertOrUpdateJob(job)
    }

    suspend fun settleJob(
        jobId: String,
        status: String,
        method: String,
        extType: String?,
        piId: String?,
        receipt: String?
    ) = withContext(Dispatchers.IO) {
        jobDao.settleJob(jobId, status, method, extType, piId, receipt)
    }

    suspend fun getCompanyProfileDirect(): CompanyProfile {
        return withContext(Dispatchers.IO) {
            profileDao.getCompanyProfile() ?: CompanyProfile()
        }
    }

    suspend fun getServicesDirect(): List<FieldService> {
        return withContext(Dispatchers.IO) {
            serviceDao.getAllServices()
        }
    }

    suspend fun saveSmsLog(smsLog: SmsLog) = withContext(Dispatchers.IO) {
        smsLogDao.insertSmsLog(smsLog)
    }

    suspend fun clearSmsLogs() = withContext(Dispatchers.IO) {
        smsLogDao.clearAllSmsLogs()
    }

    suspend fun getSmsLogByQuoteId(quoteId: String): SmsLog? {
        return withContext(Dispatchers.IO) {
            smsLogDao.getSmsLogByQuoteId(quoteId)
        }
    }
}

typealias CallCatchRepository = KuoteRepository
