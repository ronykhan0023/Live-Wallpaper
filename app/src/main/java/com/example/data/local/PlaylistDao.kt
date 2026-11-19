package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAllPlaylists(): Flow<List<PlaylistWithItems>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    fun observeActivePlaylist(): Flow<PlaylistWithItems?>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistWithItems(id: Long): PlaylistWithItems?

    @Transaction
    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlaylistOnce(): PlaylistWithItems?

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    suspend fun getOrderedEntries(playlistId: Long): List<PlaylistItemEntity>

    @Query("SELECT COUNT(DISTINCT playlistId) FROM playlist_items WHERE wallpaperId = :wallpaperId")
    suspend fun countPlaylistsContainingWallpaper(wallpaperId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(items: List<PlaylistItemEntity>)

    @Query("UPDATE playlists SET isActive = 0")
    suspend fun clearAllActive()

    @Query("UPDATE playlists SET isActive = 1, currentIndex = :currentIndex, lastSwitchEpochMs = :switchEpochMs WHERE id = :id")
    suspend fun markPlaylistActive(id: Long, currentIndex: Int, switchEpochMs: Long)

    @Query("UPDATE playlists SET currentIndex = :currentIndex, lastSwitchEpochMs = :switchEpochMs WHERE id = :id")
    suspend fun updatePlaylistProgress(id: Long, currentIndex: Int, switchEpochMs: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Transaction
    suspend fun createPlaylistWithItems(playlist: PlaylistEntity, wallpaperIds: List<Long>): Long {
        val playlistId = insertPlaylist(playlist)
        val entries = wallpaperIds.mapIndexed { index, wallpaperId ->
            PlaylistItemEntity(
                playlistId = playlistId,
                wallpaperId = wallpaperId,
                sortOrder = index
            )
        }
        insertPlaylistItems(entries)
        return playlistId
    }

    @Query("UPDATE playlists SET name = :name, intervalValue = :intervalValue, intervalUnit = :intervalUnit, isShuffle = :isShuffle WHERE id = :id")
    suspend fun updatePlaylistMetadata(
        id: Long,
        name: String,
        intervalValue: Int,
        intervalUnit: String,
        isShuffle: Boolean
    )

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deletePlaylistItems(playlistId: Long)

    @Transaction
    suspend fun updatePlaylistWithItems(
        playlistId: Long,
        name: String,
        intervalValue: Int,
        intervalUnit: String,
        isShuffle: Boolean,
        wallpaperIds: List<Long>
    ) {
        updatePlaylistMetadata(playlistId, name, intervalValue, intervalUnit, isShuffle)
        deletePlaylistItems(playlistId)
        val entries = wallpaperIds.mapIndexed { index, wallpaperId ->
            PlaylistItemEntity(
                playlistId = playlistId,
                wallpaperId = wallpaperId,
                sortOrder = index
            )
        }
        insertPlaylistItems(entries)
    }

    @Transaction
    suspend fun activatePlaylist(id: Long, currentIndex: Int, switchEpochMs: Long) {
        clearAllActive()
        markPlaylistActive(id, currentIndex, switchEpochMs)
    }
}
