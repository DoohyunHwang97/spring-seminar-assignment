package com.wafflestudio.seminar.spring2023.playlist.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PlaylistRepository : JpaRepository<PlaylistEntity, Long> {
    @Query("""
        SELECT p FROM playlists p 
        JOIN FETCH p.songs ps
        WHERE p.id = :id
    """)
    fun findByIdWithSongs(id: Long): PlaylistEntity?

    @Modifying
    @Query("UPDATE playlists p SET p.viewCnt = p.viewCnt + 1 WHERE p.id = :id")
    fun updateViewCnt(id: Long)

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM playlists p WHERE p.id = :id")
    fun findByIdUsingLock(id: Long): PlaylistEntity?
}
