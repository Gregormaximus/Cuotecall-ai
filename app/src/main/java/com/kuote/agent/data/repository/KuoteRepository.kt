package com.kuote.agent.data.repository

import android.content.Context
import com.kuote.agent.data.local.KuoteDatabase
import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.CompanyWebConfig
import com.kuote.agent.data.model.FieldService
import com.kuote.agent.data.model.Job
import com.kuote.agent.data.model.JobStatus
import com.kuote.agent.data.model.Quote
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

    val companyProfileFlow: Flow<CompanyProfile?> = profileDao.getCompanyProfileFlow()
    val servicesFlow: Flow<List<FieldService>> = serviceDao.getAllServicesFlow()
    val quotesFlow: Flow<List<Quote>> = quoteDao.getAllQuotesFlow()
    val webConfigFlow: Flow<CompanyWebConfig?> = webConfigDao.getWebConfigFlow()
    val jobsFlow: Flow<List<Job>> = jobDao.getAllJobsFlow()

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        if (profileDao.getCompanyProfile() == null) {
            val defaultProfile = CompanyProfile(
                id = "company_default",
                name = "Apex Electric Pros",
                industry = "Electrical",
                autoSmsTemplate = "Sorry we missed your call! I'm currently on a job. Reply with your service need and I'll get right back to you or tap: ",
                defaultDeposit = 50.00,
                baseServiceFee = 120.00,
                stripeAccountId = "acct_1N987654321",
                isAgentActive = true
            )
            profileDao.insertOrUpdateProfile(defaultProfile)
        }

        if (serviceDao.getAllServices().isEmpty()) {
            val defaultServices = listOf(
                FieldService(
                    id = "s_towing",
                    name = "Emergency Towing",
                    category = "TOWING",
                    basePrice = 85.00,
                    ratePerMile = 4.50,
                    aiKeywords = listOf("Flat Tire", "Breakdown", "Accident Recovery"),
                    status = "ACTIVE"
                ),
                FieldService(
                    id = "s_lockout",
                    name = "Lockout Service",
                    category = "LOCKSMITH",
                    basePrice = 75.00,
                    ratePerMile = 0.0,
                    aiKeywords = listOf("Locked Out", "Lost Keys", "Door Unlock"),
                    status = "ACTIVE"
                ),
                FieldService(
                    id = "s_fuel",
                    name = "Fuel Delivery",
                    category = "ROADSIDE",
                    basePrice = 45.00,
                    ratePerMile = 2.00,
                    aiKeywords = listOf("Out of Gas", "Gasoline", "Fuel"),
                    status = "LIMITED_SUPPLY"
                )
            )
            serviceDao.insertServices(defaultServices)
        }

        val defaultJobs = listOf(
            Job(
                id = "job_101",
                customerName = "Marcus Vance",
                customerPhone = "+1 (555) 382-9102",
                customerLocation = "1428 Elm Street, Suite 4",
                serviceTitle = "Main Circuit Panel Breaker Replacement",
                serviceCategory = "ELECTRICAL",
                status = JobStatus.DEPOSIT_PAID,
                estimatedTotal = 380.00,
                depositAmount = 50.00,
                depositPaymentIntentId = "pi_dep_9812739182",
                savedPaymentMethodId = "pm_card_visa_tok772",
                notes = "Customer reported main circuit breaker tripping repeatedly under heavy load."
            ),
            Job(
                id = "job_102",
                customerName = "Sarah Jenkins",
                customerPhone = "+1 (555) 721-4491",
                customerLocation = "890 Bayview Blvd",
                serviceTitle = "EV Charger Outlet Installation (50A)",
                serviceCategory = "ELECTRICAL",
                status = JobStatus.IN_PROGRESS,
                estimatedTotal = 520.00,
                depositAmount = 50.00,
                depositPaymentIntentId = "pi_dep_882371239",
                savedPaymentMethodId = "pm_card_mc_tok881",
                notes = "NEMA 14-50 dedicated outlet install in home garage."
            )
        )
        for (job in defaultJobs) {
            if (jobDao.getJobById(job.id) == null) {
                jobDao.insertOrUpdateJob(job)
            }
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
}

typealias CallCatchRepository = KuoteRepository
