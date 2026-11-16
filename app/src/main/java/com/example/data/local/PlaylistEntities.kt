package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.ScheduleUnit
import com.example.domain.model.WallpaperPlaylist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val intervalValue: Int = 1,
    val intervalUnit: String = ScheduleUnit.DAYS.name,
    val isShuffle: Boolean = false,
    val isActive: Boolean = false,
    val currentIndex: Int = 0,
    val lastSwitchEpochMs: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WallpaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallpaperId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("wallpaperId")]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playlistId: Long,
    val wallpaperId: Long,
    val sortOrder: Int
)

data class PlaylistWithItems(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistItemEntity::class,
            parentColumn = "playlistId",
            entityColumn = "wallpaperId"
        )
    )
    val wallpapers: List<WallpaperEntity>
) {
    fun toDomain(orderedWallpapers: List<WallpaperEntity> = wallpapers): WallpaperPlaylist =
        WallpaperPlaylist(
            id = playlist.id,
            name = playlist.name,
            intervalValue = playlist.intervalValue,
            intervalUnit = runCatching { ScheduleUnit.valueOf(playlist.intervalUnit) }
                .getOrDefault(ScheduleUnit.DAYS),
            isShuffle = playlist.isShuffle,
            isActive = playlist.isActive,
            currentIndex = playlist.currentIndex,
            lastSwitchEpochMs = playlist.lastSwitchEpochMs,
            createdAt = playlist.createdAt,
            items = orderedWallpapers.map { it.toDomain() }
        )
}
