package com.wafflestudio.seminar.spring2023.customplaylist.repository

import jakarta.persistence.LockModeType
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query


interface CustomPlaylistRepository : JpaRepository<CustomPlaylistEntity, Long> {
    fun findAllByUserId(userId: Long): List<CustomPlaylistEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE custom_playlists c SET c.songCnt = c.songCnt + 1 WHERE c.id = :playlistID")
    fun increaseSongCnt(playlistID: Long)
}
