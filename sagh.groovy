package com.tbuss.pg
import groovy.transform.*

class SAGH {
    @Memoized
    static List<Track> playLists() {
        new File('playlists.txt')
                .readLines()
                .collect { it.split(/\s/) }
                .flatten()
                .collect { Track.parse(it as String) }
                .asList()
    }

    @Memoized
    static List<String> tracks() { new File('tracks.txt').readLines() }

    @Memoized
    static List<String> artists() { new File('artists.txt').readLines() }
}

@Immutable
class Artist {
    Integer id
    String name = 'unknown'

    String toString() { name }

    def greatestHits(int amount = 1) {
        SAGH.playLists()
                .findAll { it.artist.id == id }
                .countBy { it }
                .sort { -it.value }
                .collect { it.key }
                .take amount
    }

    static Artist enrichArtist(Artist it) {
        it.properties << [name: SAGH.artists().get(it.id-1)] as Artist
    }
}

@Immutable
@EqualsAndHashCode(excludes="artist")
class Track {
    Integer id
    Artist artist
    String name = 'unknown'

    String toString() { "$name by $artist" }
    
    static Track parse(String it) {
        [id    : it.split(':')[1] as Integer,
         artist: [id: it.split(':')[0] as Integer] as Artist] as Track
    }

    static Track enrichTrack(Track input) {
        input.properties << [name: SAGH.tracks().get(input.id-1),
                artist: Artist.enrichArtist(input.artist)] as Track
    }
}

def samplePlaylist = ""
new File('/home/tb/Repos/SAGH/sample.playlist').withReader { samplePlaylist = it.readLine() }
samplePlaylist
        .split(/\s/)    // \s are whitespaces
        .collect { Track.parse(it) }
        .unique { a, b -> a.artist.id <=> b.artist.id }
        .collect { it.artist.greatestHits() }
        .flatten()
        .collect { Track.enrichTrack(it as Track) }
        .forEach { song -> println song }