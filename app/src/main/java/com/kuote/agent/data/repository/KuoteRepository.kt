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
        if (currentProfile == null || currentProfile.name == "Apex Electric Pros") {
            val defaultProfile = CompanyProfile(
                id = "company_default",
                name = "My Business",
                industry = "Field Services",
                autoSmsTemplate = "Sorry we missed your call! I'm currently on a job. Reply with your service need or tap: ",
                defaultDeposit = 50.00,
                baseServiceFee = 100.00,
                stripeAccountId = "",
                isAgentActive = true,
                phone = ""
            )
            profileDao.insertOrUpdateProfile(defaultProfile)
            
            // Purge legacy sample/dummy records for a fresh clean slate
            jobDao.deleteAllJobs()
            quoteDao.deleteAllQuotes()
            smsLogDao.clearAllSmsLogs()
            serviceDao.deleteAllServices()
        }

        if (webConfigDao.getWebConfigFlow("company_default") == null) {
            webConfigDao.insertOrUpdateWebConfig(
                CompanyWebConfig(
                    companyId = "company_default",
                    siteTitle = "My Business",
                    siteSubtitle = "Professional Field Services",
                    deployedUrl = "https://quotebit.app/?slug=my-business"
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
