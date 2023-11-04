package com.wafflestudio.seminar.spring2023.playlist.repository

import org.springframework.data.jpa.repository.JpaRepository

interface PlaylistViewRepository : JpaRepository<PlaylistViewEntity, Long> {

    fun findByPlaylistIdAndUserId(playlistId: Long, userId: Long): PlaylistViewEntity?

    fun findAllByPlaylistId(playlistId: Long): List<PlaylistViewEntity>?
}
