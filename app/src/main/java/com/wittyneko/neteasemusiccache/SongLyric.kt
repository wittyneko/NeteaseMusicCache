package com.wittyneko.neteasemusiccache

data class SongLyric(var sgc: Boolean,
                     var sfy: Boolean,
                     var qfy: Boolean,
                     var transUser: TransUser,
                     var lyricUser: LyricUser,
                     var lrc: Lrc,
                     var klyric: Klyric,
                     var tlyric: Tlyric,
                     var code: Int) {
    data class TransUser(var id: Int,
                         var status: Int,
                         var demand: Int,
                         var userid: Int,
                         var nickname: String,
                         var uptime: Long)

    data class LyricUser(var id: Int,
                         var status: Int,
                         var demand: Int,
                         var userid: Int,
                         var nickname: String,
                         var uptime: Long)

    data class Lrc(var version: Int,
                   var lyric: String)

    data class Klyric(var version: Int,
                      var lyric: String)

    data class Tlyric(var version: Int,
                      var lyric: String)
}