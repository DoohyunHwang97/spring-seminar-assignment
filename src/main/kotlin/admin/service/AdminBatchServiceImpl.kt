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
    private val threads = Executors.newFixedThreadPool(8)

    override fun insertAlbums(albumInfos: List<BatchAlbumInfo>) {
        albumInfos.forEach { albumInfo ->
            val future = threads.submit {
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
                            if (songEntity.album.title == albumEntity.title) {
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
                                songInfo.artists.forEach { artistName ->
                                    var songArtist = artistRepository.findByName(artistName)
                                    songArtist = saveIfNewArtist(songArtist, artistName)
                                    songEntity.artists.add(SongArtistEntity(song = songEntity, artist = songArtist))
                                }
                                songRepository.save(songEntity)
                                albumEntity.songs.add(songEntity)
                            }


                            songInfo.artists.forEach { artistName ->
                                var songArtist = artistRepository.findByName(artistName)
                                if (songArtist == null) {
                                    songArtist = ArtistEntity(name = artistName)
                                    artistRepository.save(songArtist!!)
                                }
                                songEntity.artists.add(SongArtistEntity(song = songEntity, artist = songArtist!!))
                                albumEntity.songs.add(songEntity)
                            }
                        } else {
                            if (songEntities.filter { it.album.title == albumEntity.title }.size == 1) {
                                songEntity = songEntities.filter { it.album.title == albumEntity.title }.first()
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

                            songInfo.artists.forEach { artistName ->
                                var songArtist = artistRepository.findByName(artistName)
                                if (songArtist == null) {
                                    songArtist = ArtistEntity(name = artistName)
                                    artistRepository.save(songArtist!!)
                                }
                                songEntity.artists.add(SongArtistEntity(song = songEntity, artist = songArtist!!))
                            }
                            songRepository.save(songEntity)
                            albumEntity.songs.add(songEntity)
                        }

                    }
                }
            }
            future.get()
        }
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
