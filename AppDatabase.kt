package iq.gov.smartkarbala.data.db

import android.content.Context
import androidx.room.*
import iq.gov.smartkarbala.data.model.*

@Database(
    entities = [User::class, PaymentCard::class, Complaint::class, Place::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun cardDao(): CardDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun placeDao(): PlaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartkarbala_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ── TYPE CONVERTERS ──────────────────────────────────────

class Converters {
    @TypeConverter fun fromComplaintType(value: ComplaintType): String = value.name
    @TypeConverter fun toComplaintType(value: String): ComplaintType = ComplaintType.valueOf(value)
    @TypeConverter fun fromComplaintStatus(value: ComplaintStatus): String = value.name
    @TypeConverter fun toComplaintStatus(value: String): ComplaintStatus = ComplaintStatus.valueOf(value)
    @TypeConverter fun fromPlaceCategory(value: PlaceCategory): String = value.name
    @TypeConverter fun toPlaceCategory(value: String): PlaceCategory = PlaceCategory.valueOf(value)
}

// ── DAOs ─────────────────────────────────────────────────

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE nationalId = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users ORDER BY registeredAt DESC LIMIT 1")
    suspend fun getLatestUser(): User?

    @Query("UPDATE users SET profilePhotoPath = :path WHERE nationalId = :id")
    suspend fun updatePhoto(id: String, path: String)

    @Query("DELETE FROM users WHERE nationalId = :id")
    suspend fun deleteUser(id: String)
}

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: PaymentCard): Long

    @Query("SELECT * FROM payment_cards WHERE userId = :userId ORDER BY isDefault DESC, addedAt DESC")
    suspend fun getCardsForUser(userId: String): List<PaymentCard>

    @Query("SELECT * FROM payment_cards WHERE userId = :userId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultCard(userId: String): PaymentCard?

    @Query("UPDATE payment_cards SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefault(userId: String)

    @Query("UPDATE payment_cards SET isDefault = 1 WHERE id = :cardId")
    suspend fun setDefault(cardId: Long)

    @Query("DELETE FROM payment_cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Long)

    @Query("UPDATE payment_cards SET lastUsed = :timestamp WHERE id = :cardId")
    suspend fun updateLastUsed(cardId: Long, timestamp: Long)
}

@Dao
interface ComplaintDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: Complaint)

    @Query("SELECT * FROM complaints WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getComplaintsForUser(userId: String): List<Complaint>

    @Query("SELECT * FROM complaints WHERE isSynced = 0")
    suspend fun getUnsynced(): List<Complaint>

    @Query("UPDATE complaints SET isSynced = 1, status = :status WHERE id = :id")
    suspend fun markSynced(id: String, status: ComplaintStatus = ComplaintStatus.IN_REVIEW)

    @Query("UPDATE complaints SET status = :status, updatedAt = :time WHERE id = :id")
    suspend fun updateStatus(id: String, status: ComplaintStatus, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM complaints WHERE userId = :userId AND status = 'PENDING'")
    suspend fun getPendingCount(userId: String): Int
}

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<Place>)

    @Query("SELECT * FROM places ORDER BY isFeatured DESC, rating DESC")
    suspend fun getAllPlaces(): List<Place>

    @Query("SELECT * FROM places WHERE category = :category ORDER BY rating DESC")
    suspend fun getByCategory(category: PlaceCategory): List<Place>

    @Query("SELECT * FROM places WHERE nameArabic LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' ORDER BY rating DESC")
    suspend fun search(query: String): List<Place>

    @Query("SELECT * FROM places WHERE isFeatured = 1 ORDER BY rating DESC LIMIT 10")
    suspend fun getFeatured(): List<Place>

    @Query("SELECT COUNT(*) FROM places")
    suspend fun getCount(): Int

    @Query("DELETE FROM places")
    suspend fun clearAll()
}
