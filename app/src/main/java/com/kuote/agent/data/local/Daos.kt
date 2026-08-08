package com.kuote.agent.data.local

import androidx.room.*
import com.kuote.agent.data.model.CompanyProfile
import com.kuote.agent.data.model.CompanyWebConfig
import com.kuote.agent.data.model.FieldService
import com.kuote.agent.data.model.Job
import com.kuote.agent.data.model.Quote
import com.kuote.agent.data.model.SmsLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profile WHERE id = :id LIMIT 1")
    fun getCompanyProfileFlow(id: String = "company_default"): Flow<CompanyProfile?>

    @Query("SELECT * FROM company_profile WHERE id = :id LIMIT 1")
    suspend fun getCompanyProfile(id: String = "company_default"): CompanyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: CompanyProfile)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM field_services ORDER BY name ASC")
    fun getAllServicesFlow(): Flow<List<FieldService>>

    @Query("SELECT * FROM field_services")
    suspend fun getAllServices(): List<FieldService>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: FieldService)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<FieldService>)

    @Delete
    suspend fun deleteService(service: FieldService)

    @Query("DELETE FROM field_services")
    suspend fun deleteAllServices()
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY timestampMillis DESC")
    fun getAllQuotesFlow(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun getQuoteById(id: String): Quote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: Quote)

    @Query("UPDATE quotes SET status = :status WHERE id = :quoteId")
    suspend fun updateQuoteStatus(quoteId: String, status: String)

    @Query("DELETE FROM quotes WHERE id = :quoteId")
    suspend fun deleteQuote(quoteId: String)

    @Query("DELETE FROM quotes")
    suspend fun deleteAllQuotes()
}

@Dao
interface WebConfigDao {
    @Query("SELECT * FROM company_web_config WHERE companyId = :id LIMIT 1")
    fun getWebConfigFlow(id: String = "company_default"): Flow<CompanyWebConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWebConfig(config: CompanyWebConfig)
}

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY timestampMillis DESC")
    fun getAllJobsFlow(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobById(jobId: String): Job?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateJob(job: Job)

    @Query("UPDATE jobs SET status = :status, finalSettlementMethod = :method, externalPaymentType = :extType, balancePaymentIntentId = :piId, receiptUrl = :receipt WHERE id = :jobId")
    suspend fun settleJob(jobId: String, status: String, method: String, extType: String?, piId: String?, receipt: String?)

    @Query("DELETE FROM jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("DELETE FROM jobs")
    suspend fun deleteAllJobs()
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestampMillis DESC")
    fun getAllSmsLogsFlow(): Flow<List<SmsLog>>

    @Query("SELECT * FROM sms_logs WHERE id = :id LIMIT 1")
    suspend fun getSmsLogById(id: String): SmsLog?

    @Query("SELECT * FROM sms_logs WHERE relatedQuoteId = :quoteId ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getSmsLogByQuoteId(quoteId: String): SmsLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(smsLog: SmsLog)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAllSmsLogs()
}
