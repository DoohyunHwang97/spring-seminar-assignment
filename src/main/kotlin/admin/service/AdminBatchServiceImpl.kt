package com.wafflestudio.seminar.spring2023.admin.service

import com.wafflestudio.seminar.spring2023.song.repository.*
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Future

@Service
class AdminBatchServiceImpl(
        private val albumRepository: AlbumRepository,
        private val songRepository: SongRepository,
        private val artistRepository: ArtistRepository,
        private val txManager: PlatformTransactionManager,
) : AdminBatchService {
    private val threads = Executors.newFixedThreadPool(4)

    override fun insertAlbums(albumInfos: List<BatchAlbumInfo>) {
        val futures: List<Future<*>>  = ArrayList()
        albumInfos.map { albumInfo ->
            threads.submit {
                TransactionTemplate(txManager).executeWithoutResult {
                    // 아티스트 유무 확인 후 생성
                    var artistEntity = artistRepository.findByName(albumInfo.artist)
                    artistEntity = saveIfNewArtist(artistEntity, albumInfo.artist)

                    // 앨범 유무 확인 후 생성 혹은 업데이트
                    var albumEntity = albumRepository.findByTitle(albumInfo.title)
                    if (albumEntity == null) {
                        albumEntity = AlbumEntity(
                                title = albumInfo.title,
                                image = albumInfo.image,
                                artist = artistEntity
                        )
                        albumRepository.save(albumEntity)
                    } else {
                        albumEntity.title = albumInfo.title
                        albumEntity.image = albumInfo.image
                        albumEntity.artist = artistEntity
                        albumEntity.songs.clear()
                    }

                    // 곡 업데이트
                    albumInfo.songs.forEach { songInfo ->
                        var songEntities = songRepository.findAllByTitle(songInfo.title)!!
                        var songEntity: SongEntity
                        if (hasNoSong(songEntities)) {
                            songEntity = SongEntity(
                                    title = songInfo.title,
                                    duration = songInfo.duration,
                                    album = albumEntity
                            )
                            addSongArtistRelation(songInfo, songEntity)
                            songRepository.save(songEntity)
                            albumEntity.songs.add(songEntity)

                        } else if (hasOneSong(songEntities)) {
                            songEntity = songEntities.first()
                            // 곡이 이름만 같은 곡이 아닐 때
                            if (songEntity.album.title == albumEntity.title) {
                                songEntity.title = songInfo.title
                                songEntity.duration = songInfo.duration
                                songEntity.album = albumEntity
                                songEntity.artists.clear()
                            }
                            // 이름만 같은 곡일 때
                            else {
                                songEntity = songEntity(songEntity, songInfo, albumEntity)
                            }
                            songRepository.save(songEntity)
                            addSongArtistRelation(songInfo, songEntity)
                            albumEntity.songs.add(songEntity)
                        }
                        // 이름이 같은 곡이 두개 이상일 때
                        else {
                            val matchingEntity = songEntities.filter { it.album.title == albumEntity.title }
                            if (matchingEntity.size == 1) {
                                songEntity = matchingEntity.first()
                                songEntity.title = songInfo.title
                                songEntity.duration = songInfo.duration
                                songEntity.album = albumEntity
                                songEntity.artists.clear()
                            } else {
                                songEntity = SongEntity(
                                        title = songInfo.title,
                                        duration = songInfo.duration,
                                        album = albumEntity
                                )
                            }
                            addSongArtistRelation(songInfo, songEntity)
                            songRepository.save(songEntity)
                            albumEntity.songs.add(songEntity)
                        }

                    }
                }
            }
        }.forEach {future ->
            future.get()
        }
    }

    private fun songEntity(songEntity: SongEntity, songInfo: BatchAlbumInfo.BatchSongInfo, albumEntity: AlbumEntity): SongEntity {
        // 새로운 song 생성
        var songEntity1 = songEntity
        songEntity1 = SongEntity(
                title = songInfo.title,
                duration = songInfo.duration,
                album = albumEntity
        )
        // songInfo 내의 artists 로 관계 설정 후 저장
        addSongArtistRelation(songInfo, songEntity1)
        songRepository.save(songEntity1)
        // 앨범과 song 관계 설정
        albumEntity.songs.add(songEntity1)
        return songEntity1
    }

    private fun hasOneSong(songEntities: List<SongEntity>) =
            songEntities.size == 1

    private fun hasNoSong(songEntities: List<SongEntity>) =
            songEntities.isEmpty()

    private fun addSongArtistRelation(songInfo: BatchAlbumInfo.BatchSongInfo, songEntity: SongEntity) {
        songInfo.artists.forEach { artistName ->
            var songArtist = artistRepository.findByName(artistName)
            songArtist = saveIfNewArtist(songArtist, artistName)
            songEntity.artists.add(SongArtistEntity(song = songEntity, artist = songArtist))
        }
    }

    private fun saveIfNewArtist(artist: ArtistEntity?, artistName: String): ArtistEntity {
        if (artist == null) {
            val newArtist = ArtistEntity(name = artistName)
            artistRepository.save(newArtist)
            return newArtist
        }
        return artist
    }
}
