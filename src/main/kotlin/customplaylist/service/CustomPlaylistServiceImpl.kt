package com.wafflestudio.seminar.spring2023.customplaylist.service

import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistEntity
import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistRepository
import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistSongEntity
import com.wafflestudio.seminar.spring2023.song.repository.SongRepository
import com.wafflestudio.seminar.spring2023.song.service.Song
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

/**
 * 스펙:
 *  1. 커스텀 플레이리스트 생성시, 자동으로 생성되는 제목은 "내 플레이리스트 #{내 커스텀 플레이리스트 갯수 + 1}"이다.
 *  2. 곡 추가 시  CustomPlaylistSongEntity row 생성, CustomPlaylistEntity의 songCnt의 업데이트가 atomic하게 동작해야 한다. (둘 다 모두 성공하거나, 둘 다 모두 실패해야 함)
 *
 * 조건:
 *  1. Synchronized 사용 금지.
 *  2. 곡 추가 요청이 동시에 들어와도 동시성 이슈가 없어야 한다.(PlaylistViewServiceImpl에서 동시성 이슈를 해결한 방법과는 다른 방법을 사용할 것)
 *  3. JPA의 변경 감지 기능을 사용해야 한다.
 */
@Service
class CustomPlaylistServiceImpl(
        private val customPlaylistRepository: CustomPlaylistRepository,
        private val songRepository: SongRepository,
        private val txManager: PlatformTransactionManager,
) : CustomPlaylistService {
    @Transactional
    override fun get(userId: Long, customPlaylistId: Long): CustomPlaylist {
        val customPlaylist = customPlaylistRepository.findById(customPlaylistId).orElseThrow { CustomPlaylistNotFoundException() }

        return CustomPlaylist(
                id = customPlaylistId,
                title = customPlaylist.title,
                songs = customPlaylist.songs.map { Song(it.song) }
        )
    }

    override fun gets(userId: Long): List<CustomPlaylistBrief> {
        val playlists = customPlaylistRepository.findAllByUserId(userId)

        if (playlists.isEmpty()) {
            throw CustomPlaylistNotFoundException()
        }

        return playlists.map { CustomPlaylistBrief(id = it.id, title = it.title, songCnt = it.songCnt) }
    }
    @Transactional
    override fun create(userId: Long): CustomPlaylistBrief {
        val playlists = customPlaylistRepository.findAllByUserId(userId)
        val playlistCnt = playlists.size

        val customPlaylist = CustomPlaylistEntity(userId = userId, title = "내 플레이리스트 #%d".format(playlistCnt + 1))
        customPlaylistRepository.save(customPlaylist)
        return CustomPlaylistBrief(id = customPlaylist.id, title = customPlaylist.title, songCnt = customPlaylist.songCnt)
    }

    @Transactional
    override fun patch(userId: Long, customPlaylistId: Long, title: String): CustomPlaylistBrief {
        val customPlaylist = customPlaylistRepository.findById(customPlaylistId).orElseThrow { CustomPlaylistNotFoundException() }
        if (customPlaylist.userId != userId) throw CustomPlaylistNotFoundException()

        customPlaylist.title = title

        return CustomPlaylistBrief(id = customPlaylistId, title = title, songCnt = customPlaylist.songCnt)
    }
    @Transactional
    override fun addSong(userId: Long, customPlaylistId: Long, songId: Long): CustomPlaylistBrief {
        val customPlaylist = customPlaylistRepository.findById(customPlaylistId).orElseThrow { CustomPlaylistNotFoundException() }

        if (customPlaylist.userId != userId) throw CustomPlaylistNotFoundException()

        val songToAdd = songRepository.findById(songId).orElseThrow { SongNotFoundException() }
        val songsInCustomPlaylist = customPlaylist.songs.map { it.song }

        if (!songsInCustomPlaylist.contains(songToAdd)) {
            customPlaylist.songs.add(CustomPlaylistSongEntity(customPlaylist = customPlaylist, song = songToAdd))
            customPlaylistRepository.increaseSongCnt(customPlaylistId)
        }

        return CustomPlaylistBrief(id = customPlaylistId, title = customPlaylist.title, songCnt = customPlaylist.songCnt)
    }
}
