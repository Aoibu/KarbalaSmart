package iq.gov.smartkarbala.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── USER MODEL ──────────────────────────────────────────

@Entity(tableName = "users")
data class User(
    @PrimaryKey val nationalId: String,
    val fullName: String,
    val fullNameArabic: String,
    val dateOfBirth: String,
    val gender: String,
    val governorate: String,
    val religion: String,
    val issueDate: String,
    val expiryDate: String,
    val motherName: String,
    val fatherName: String,
    val bloodType: String,
    val maritalStatus: String,
    val profilePhotoPath: String? = null,
    val registeredAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun getAge(): Int {
        return try {
            val parts = dateOfBirth.split("/")
            val birthYear = parts[2].toInt()
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            currentYear - birthYear
        } catch (e: Exception) { 0 }
    }

    fun getDisplayName(): String = fullNameArabic.ifEmpty { fullName }
}

// ── PAYMENT CARD MODEL ──────────────────────────────────

@Entity(tableName = "payment_cards")
data class PaymentCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val maskedPan: String,
    val encryptedPan: String,    // AES-256 encrypted
    val expiryDate: String,
    val cardholderName: String,
    val cardType: String,         // VISA, MASTERCARD, etc.
    val bankName: String,
    val colorScheme: String = "#1A2B4A", // card background color
    val isDefault: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = 0L,
    val nickname: String = ""
) {
    fun getDisplayName(): String = nickname.ifEmpty { "$cardType •••• ${maskedPan.takeLast(4)}" }
    fun isExpired(): Boolean {
        return try {
            val parts = expiryDate.split("/")
            val expMonth = parts[0].toInt()
            val expYear = parts[1].toInt() + 2000
            val cal = java.util.Calendar.getInstance()
            val currentYear = cal.get(java.util.Calendar.YEAR)
            val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
            expYear < currentYear || (expYear == currentYear && expMonth < currentMonth)
        } catch (e: Exception) { false }
    }
}

// ── COMPLAINT MODEL ──────────────────────────────────────

@Entity(tableName = "complaints")
data class Complaint(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String,
    val type: ComplaintType,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val photoPath: String? = null,
    val status: ComplaintStatus = ComplaintStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val referenceNumber: String = "KRB-${System.currentTimeMillis() % 100000}",
    val isSynced: Boolean = false
)

enum class ComplaintType(val arabicName: String, val icon: String) {
    ROAD_HOLE("حفرة في الطريق", "🕳️"),
    TRAFFIC("مشكلة مرورية", "🚦"),
    LIGHTING("إنارة معطلة", "💡"),
    SEWAGE("مجاري", "🔧"),
    WASTE("نفايات", "🗑️"),
    ELECTRICITY("كهرباء", "⚡"),
    WATER("ماء", "💧"),
    OTHER("أخرى", "📋")
}

enum class ComplaintStatus(val arabicName: String, val color: String) {
    PENDING("قيد الانتظار", "#FFB300"),
    IN_REVIEW("قيد المراجعة", "#2196F3"),
    IN_PROGRESS("جاري المعالجة", "#FF9800"),
    RESOLVED("تم الحل", "#4CAF50"),
    REJECTED("مرفوض", "#F44336")
}

// ── PLACE MODEL (Karbala POIs) ───────────────────────────

@Entity(tableName = "places")
data class Place(
    @PrimaryKey val id: String,
    val name: String,
    val nameArabic: String,
    val category: PlaceCategory,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phone: String = "",
    val website: String = "",
    val openingHours: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val photos: String = "[]", // JSON array of photo URLs
    val priceRange: String = "",
    val isOpen: Boolean = true,
    val isFeatured: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

enum class PlaceCategory(val arabicName: String, val icon: String, val pinColor: String) {
    MOSQUE("مساجد ومراقد", "🕌", "#C9A84C"),
    RESTAURANT("مطاعم", "🍽️", "#FF6B6B"),
    HOTEL("فنادق", "🏨", "#4ECDC4"),
    HOSPITAL("مستشفيات", "🏥", "#FF4757"),
    GOVERNMENT("دوائر حكومية", "🏛️", "#1A6EBD"),
    TOURISM("مواقع سياحية", "🏛️", "#A29BFE"),
    MARKET("أسواق", "🛒", "#FD9644"),
    PHARMACY("صيدليات", "💊", "#26de81"),
    BANK("مصارف", "🏦", "#45aaf2"),
    SCHOOL("مدارس وجامعات", "🎓", "#fd9644"),
    PARK("حدائق", "🌳", "#20bf6b"),
    FUEL("محطات وقود", "⛽", "#778ca3")
}

// ── WEATHER MODEL ─────────────────────────────────────────

data class WeatherData(
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double,
    val uvIndex: Double = 0.0,
    val visibility: Double = 0.0,
    val pressure: Int = 0,
    val cityName: String = "كربلاء المقدسة",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getTempDisplay(): String = "${temperature.toInt()}°"
    fun getFeelsLikeDisplay(): String = "${feelsLike.toInt()}°"
    fun getWindDisplay(): String = "${windSpeed.toInt()} كم/س"
    fun getHumidityDisplay(): String = "$humidity%"
}
