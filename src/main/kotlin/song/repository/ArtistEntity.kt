package com.wafflestudio.seminar.spring2023.song.repository

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity(name = "artists")
class ArtistEntity(
        @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
        var name: String,
        @OneToMany(mappedBy = "artist", cascade = [CascadeType.ALL])
    val albums: MutableList<AlbumEntity> = mutableListOf(),
)