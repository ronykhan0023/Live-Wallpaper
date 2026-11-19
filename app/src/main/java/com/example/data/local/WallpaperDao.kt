package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    @Query("SELECT * FROM wallpapers ORDER BY createdAt DESC")
    fun observeAllWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE lastUsedAt > 0 ORDER BY lastUsedAt DESC LIMIT 10")
    fun observeRecentlyUsed(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE isActive = 1 LIMIT 1")
    fun observeActiveWallpaper(): Flow<WallpaperEntity?>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    fun observeWallpaperById(id: Long): Flow<WallpaperEntity?>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    suspend fun getWallpaperById(id: Long): WallpaperEntity?

    @Query("SELECT * FROM wallpapers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveWallpaper(): WallpaperEntity?

    @Query("SELECT * FROM wallpapers")
    suspend fun getAllWallpapersOnce(): List<WallpaperEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(entity: WallpaperEntity): Long

    @Update
    suspend fun updateWallpaper(entity: WallpaperEntity)

    @Query("UPDATE wallpapers SET title = :newTitle WHERE id = :id")
    suspend fun renameWallpaper(id: Long, newTitle: String)

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE wallpapers SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE wallpapers SET isActive = 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun markAsActive(id: Long, timestamp: Long)

    @Transaction
    suspend fun setActiveWallpaper(id: Long, timestamp: Long) {
        clearActiveFlags()
        markAsActive(id, timestamp)
    }

    @Query(
        """
        UPDATE wallpapers SET
            playbackSpeed = :playbackSpeed,
            isLooping = :isLooping,
            isMuted = :isMuted,
            volume = :volume,
            brightness = :brightness,
            scaleMode = :scaleMode,
            zoom = :zoom,
            offsetX = :offsetX,
            offsetY = :offsetY,
            trimStartMs = :trimStartMs,
            trimEndMs = :trimEndMs
        WHERE id = :id
        """
    )
    suspend fun updateCustomization(
        id: Long,
        playbackSpeed: Float,
        isLooping: Boolean,
        isMuted: Boolean,
        volume: Float,
        brightness: Float,
        scaleMode: String,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        trimStartMs: Long,
        trimEndMs: Long
    )

    @Query("DELETE FROM wallpapers WHERE id = :id")
    suspend fun deleteWallpaperById(id: Long)
}
