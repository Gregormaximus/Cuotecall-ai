package com.kuote.agent.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_profile")
data class CompanyProfile(
    @PrimaryKey val id: String = "tenant_starlink_batavia",
    val name: String = "Skynet One",
    val industry: String = "Starlink & Satellite Installation",
    val autoSmsTemplate: String = "Sorry we missed your call from Skynet One! Reply with your service need or tap: ",
    val defaultDeposit: Double = 50.00,
    val baseServiceFee: Double = 100.00,
    val stripeAccountId: String = "acct_starlink_batavia",
    val isAgentActive: Boolean = true,
    val phone: String = "(630) 555-0199",
    val hqAddress: String = "701 Branson Dr, Batavia, IL 60510",
    val logoUrl: String = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=150&auto=format&fit=crop&q=80",
    val enableMarketplace: Boolean = false,
    val dispatchRadiusMiles: Double = 25.0,
    val isAcceptingDispatches: Boolean = true
)

@Entity(tableName = "field_services")
data class FieldService(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val basePrice: Double,
    val ratePerMile: Double = 0.0,
    val ratePerHour: Double = 0.0,
    val aiKeywords: List<String> = emptyList(),
    val status: String = "ACTIVE" // "ACTIVE", "LIMITED_SUPPLY", "INACTIVE"
)

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey val id: String,
    val customerPhone: String,
    val customerLocation: String,
    val timeAgoText: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val serviceCategory: String, // "TOWING", "PLUMBING", "ELECTRICAL", etc.
    val audioUrl: String? = null,
    val audioDurationText: String? = null,
    val photoUrl: String? = null,
    val aiSummary: String,
    val estimatedTotal: Double,
    val requiredDeposit: Double = 50.00,
    val platformFee: Double = 2.50,
    val status: String = "PENDING_APPROVAL", // "PENDING_APPROVAL", "APPROVED", "DEPOSIT_PAID", "DECLINED"
    val stripePaymentLink: String? = null,
    val rawCustomerNote: String? = null
)

@Entity(tableName = "company_web_config")
data class CompanyWebConfig(
    @PrimaryKey val companyId: String = "tenant_starlink_batavia",
    val siteTitle: String = "Skynet One",
    val siteSubtitle: String = "Starlink & Satellite Installation - 701 Branson Dr, Batavia, IL 60510",
    val themeColorHex: String = "#00E5FF",
    val voiceCallButtonText: String = "Instant AI Dispatch",
    val voiceCallDescription: String = "Speak directly with our AI dispatcher",
    val quickDepositFee: Double = 50.00,
    val deployedUrl: String = "https://quotebit.app/?slug=skynet-one",
    val logoUrl: String = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=150&auto=format&fit=crop&q=80"
)

data class MarketplaceJob(
    val jobId: String = "",
    val tenantId: String = "tenant_starlink_batavia",
    val serviceTitle: String = "",
    val customerLocation: String = "",
    val estimatedTotal: Double = 0.0,
    val depositAmount: Double = 50.0,
    val claimLink: String = "",
    val status: String = "UNCLAIMED", // "UNCLAIMED", "CLAIMED", "DISPATCHED"
    val timestamp: Long = System.currentTimeMillis()
)

data class BrandingChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String = "AI", // "USER" or "AI"
    val messageText: String = "",
    val attachmentType: String? = null, // "URL", "PHOTO", "DOCUMENT"
    val timestampMillis: Long = System.currentTimeMillis()
)

data class IndustryPreset(
    val name: String,
    val categoryGroup: String, // "Technical", "Emergency", "Lawn & Garden", "Home Services", "Automotive", "Other"
    val iconName: String
)

object IndustryCategories {
    val GROUPS = listOf("All", "Technical", "Emergency", "Lawn & Garden", "Automotive", "Home Services", "Care & Personal")

    val PRESETS = listOf(
        // Emergency & Automotive
        IndustryPreset("Starlink Installation", "Satellite", "satellite_alt"),
        IndustryPreset("Locksmith", "Emergency", "vpn_key"),
        IndustryPreset("Roadside Assistance", "Emergency", "car_repair"),
        IndustryPreset("Mobile Mechanic", "Automotive", "build"),
        IndustryPreset("Auto Detailing", "Automotive", "directions_car"),
        IndustryPreset("Tire Service", "Automotive", "tire_repair"),
        
        // Technical & Home
        IndustryPreset("Plumbing", "Technical", "plumbing"),
        IndustryPreset("Electrical", "Technical", "bolt"),
        IndustryPreset("HVAC & Heating", "Technical", "ac_unit"),
        IndustryPreset("Appliance Repair", "Technical", "home_repair_service"),
        IndustryPreset("Roofing", "Technical", "roofing"),
        IndustryPreset("Pest Control", "Technical", "bug_report"),

        // Lawn & Garden
        IndustryPreset("Landscaping", "Lawn & Garden", "grass"),
        IndustryPreset("Lawn Care", "Lawn & Garden", "content_cut"),
        IndustryPreset("Tree Trimming", "Lawn & Garden", "nature"),
        IndustryPreset("Snow Removal", "Lawn & Garden", "ac_unit"),
        IndustryPreset("Irrigation & Sprinklers", "Lawn & Garden", "water_drop"),

        // Home Services & Moving
        IndustryPreset("Cleaning Services", "Home Services", "cleaning_services"),
        IndustryPreset("Junk Removal", "Home Services", "delete_sweep"),
        IndustryPreset("Movers & Hauling", "Home Services", "local_shipping"),
        IndustryPreset("Handyman", "Home Services", "handyman"),
        IndustryPreset("Painter", "Home Services", "format_paint"),
        IndustryPreset("Pressure Washing", "Home Services", "water"),
        IndustryPreset("Window Cleaning", "Home Services", "crop_original"),
        IndustryPreset("Pool Maintenance", "Home Services", "pool"),

        // Other 40+ presets to cover all 66 field industries
        IndustryPreset("Mobile Notary", "Care & Personal", "verified"),
        IndustryPreset("Carpet Cleaning", "Home Services", "dry_cleaning"),
        IndustryPreset("Chimney Sweep", "Technical", "fireplace"),
        IndustryPreset("Dog Grooming", "Care & Personal", "pets"),
        IndustryPreset("Solar Panel Care", "Technical", "solar_power"),
        IndustryPreset("Fence Installation", "Technical", "fence"),
        IndustryPreset("Garage Door Repair", "Technical", "garage"),
        IndustryPreset("Lockout & Security", "Emergency", "security"),
        IndustryPreset("Septic Tank Care", "Technical", "water_damage"),
        IndustryPreset("Water Damage Restoration", "Emergency", "water_damage"),
        IndustryPreset("Mold Remediation", "Technical", "biotech"),
        IndustryPreset("Asphalt Paving", "Technical", "add_road"),
        IndustryPreset("Concrete & Masonry", "Technical", "foundation"),
        IndustryPreset("Deck Building", "Technical", "deck"),
        IndustryPreset("Drywall Installation", "Technical", "square"),
        IndustryPreset("Flooring Specialist", "Technical", "grid_view"),
        IndustryPreset("Gutter Cleaning", "Lawn & Garden", "waterfall_chart"),
        IndustryPreset("Insulation Installer", "Technical", "thermostat"),
        IndustryPreset("Interior Decorator", "Care & Personal", "palette"),
        IndustryPreset("Stump Grinding", "Lawn & Garden", "forest"),
        IndustryPreset("Welding & Fabrication", "Technical", "precision_manufacturing"),
        IndustryPreset("Mobile Vet", "Care & Personal", "medical_services"),
        IndustryPreset("Personal Trainer", "Care & Personal", "fitness_center"),
        IndustryPreset("Event Setup", "Care & Personal", "event"),
        IndustryPreset("Catering Operations", "Care & Personal", "restaurant"),
        IndustryPreset("Security Patrol", "Emergency", "shield"),
        IndustryPreset("IT Onsite Tech", "Technical", "computer"),
        IndustryPreset("Audio/Video Install", "Technical", "tv"),
        IndustryPreset("Smart Home Automation", "Technical", "smart_toy"),
        IndustryPreset("Siding Contractor", "Technical", "house_siding"),
        IndustryPreset("Excavation Service", "Technical", "agriculture"),
        IndustryPreset("Demolition Service", "Technical", "construction"),
        IndustryPreset("Boat & Marine Service", "Automotive", "sailing"),
        IndustryPreset("RV Repair", "Automotive", "rv_hookup"),
        IndustryPreset("Commercial Refrigeration", "Technical", "kitchen"),
        IndustryPreset("Fire Protection Install", "Emergency", "local_fire_department"),
        IndustryPreset("Cabinet Refinishing", "Technical", "cabinet"),
        IndustryPreset("Tile & Grout Cleaning", "Home Services", "clean_hands"),
        IndustryPreset("Signage Installation", "Technical", "badge"),
        IndustryPreset("Drone Property Inspection", "Technical", "flight")
    )
}

data class MonthRevenue(
    val monthKey: String, // e.g., "2026-03"
    val monthLabel: String, // e.g., "Mar"
    val depositRevenue: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val totalJobsCount: Int = 0
)

data class DayActivity(
    val dayLabel: String, // e.g., "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    val missedCalls: Int = 0,
    val smsSent: Int = 0,
    val siteClicks: Int = 0,
    val depositsAmount: Double = 0.0
)

data class AnalyticsStats(
    val missedCallsHandled: Long = 0,
    val autoSmsSent: Long = 0,
    val microSiteClicks: Long = 0,
    val ctrPercentage: Double = 0.0,
    val totalDepositsCollected: Double = 0.00,
    val momGrowthPercentage: Double = 0.0,
    val weeklyOverview: List<DayActivity> = listOf(
        DayActivity("Mon"),
        DayActivity("Tue"),
        DayActivity("Wed"),
        DayActivity("Thu"),
        DayActivity("Fri"),
        DayActivity("Sat"),
        DayActivity("Sun")
    ),
    val monthlyRevenueTrend: List<MonthRevenue> = emptyList()
)

data class ConversationLog(
    val id: String = "",
    val customerPhone: String = "",
    val customerName: String = "Client",
    val lastSmsText: String = "",
    val generatedQuoteAmount: Double = 0.0,
    val depositAmount: Double = 50.0,
    val status: String = "SENT_SMS", // "SENT_SMS", "CLIENT_REPLIED", "DEPOSIT_PAID", "LOCATION_DISPATCHED"
    val timestamp: Long = System.currentTimeMillis(),
    val serviceCategory: String = "Emergency Service",
    val gpsLocation: String? = null
)

data class DispatchGpsData(
    val dispatchId: String = "",
    val lat: Double = 37.77492,
    val lng: Double = -122.41942,
    val coordsFormatted: String = "37.77492, -122.41942",
    val status: String = "GPS_RECEIVED",
    val timestamp: Long = System.currentTimeMillis()
)

