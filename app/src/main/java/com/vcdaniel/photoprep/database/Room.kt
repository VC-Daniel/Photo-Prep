package com.vcdaniel.photoprep.database

import android.content.Context
import androidx.room.*

/** Stores information related to photo shoot locations and prep items in the common prep library*/
@Database(
    entities = [DatabaseCommonPrep::class, DatabaseWeatherForecast::class, DatabasePhotoShootPrep::class, DatabasePhotoShootOverview::class],
    version = 10,
    exportSchema = false
)
abstract class PhotoPrepDatabase : RoomDatabase() {
    abstract val photoPrepDao: PhotoPrepDao
}

/** Specifies available interactions with the photo prep database */
@Dao
interface PhotoPrepDao {
    /** Get the common prep data with the specified [prepName] */
    @Query("select * from DatabaseCommonPrep where prepName = :prepName")
    fun getCommonPrep(prepName: String): DatabaseCommonPrep

    /** Get the common prep data with the specified id] */
    @Query("select * from DatabaseCommonPrep where id = :id")
    fun getCommonPrep(id: Long): DatabaseCommonPrep

    /** Get all the common prep items in the common prep library ordered by prepName */
    @Query("select * from DatabaseCommonPrep order by prepName")
    fun getAllCommonPrep(): List<DatabaseCommonPrep>

    /** Insert new common prep item(s) into the database */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCommonPrep(vararg commonPrep: DatabaseCommonPrep)

    /** Remove a common prep item with the supplied [prepName] */
    @Query("delete from DatabaseCommonPrep where prepName = :prepName")
    fun removeCommonPrepItem(prepName: String): Int

    /** Remove a common prep item with the supplied [id] */
    @Query("delete from DatabaseCommonPrep where id = :id")
    fun removeCommonPrepItem(id: Long): Int

    /** Insert a photoShootOverview */
    @Insert
    suspend fun insertPhotoShootOverview(vararg photoShootOverview: DatabasePhotoShootOverview)

    /** Insert a weather forecast */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherForecast(weatherForecast: DatabaseWeatherForecast)

    /** Update the overview information for a photo shoot location */
    @Update
    suspend fun updatePhotoShootOverview(vararg photoShootOverview: DatabasePhotoShootOverview)

    /** Update photo shoot prep */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePhotoShootPrep(vararg photoShootPrep: DatabasePhotoShootPrep)

    /** Insert photo shoot prep */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoShootPrep(vararg photoShootPrep: DatabasePhotoShootPrep)

    /** Retrieve all the photo shoot locations ordered by photoShootLocationName */
    @Transaction
    @Query("SELECT * FROM DatabasePhotoShootOverview ORDER BY photoShootLocationName")
    suspend fun getPhotoShootLocations(): List<DatabasePhotoShootLocation>

    /** Delete the photo shoot overview with the [photoShootLocationId] */
    @Transaction
    @Query("DELETE FROM DatabasePhotoShootOverview WHERE photoShootLocationId = :photoShootLocationId")
    suspend fun deletePhotoShootOverview(photoShootLocationId: Long)

    /** Retrieve the photo shoot location with the matching [photoShootLocationId] */
    @Transaction
    @Query("SELECT * FROM DatabasePhotoShootOverview WHERE photoShootLocationId = :photoShootLocationId")
    suspend fun getPhotoShootLocation(photoShootLocationId: Long): DatabasePhotoShootLocation

    /** Remove a photo shoot prep item with the supplied [prepId] in the photo shoot that corresponds
     * to the supplied [photoShootId]*/
    @Query("delete from DatabasePhotoShootPrep where id = :prepId and photoShootId = :photoShootId")
    suspend fun removePhotoShootPrepItem(prepId: Long, photoShootId: Long)

    /** Delete the weather forecast that is for the photo shoot with the id matching the
     * supplied [photoShootId] */
    @Query("delete from DatabaseWeatherForecast where photoShootId = :photoShootId")
    suspend fun deleteWeatherForecast(photoShootId: Long)
}

private lateinit var INSTANCE: PhotoPrepDatabase
private const val DATABASE_NAME = "photoPrep"

/** Returns the photo prep database*/
fun getDatabase(context: Context): PhotoPrepDatabase {
    synchronized(PhotoPrepDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                PhotoPrepDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration().build()
        }
    }
    return INSTANCE
}