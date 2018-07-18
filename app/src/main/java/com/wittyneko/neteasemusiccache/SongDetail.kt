package com.wittyneko.neteasemusiccache

data class SongDetail(var code: Int,
                      var songs: List<Songs>,
                      var privileges: List<Privileges>) {
    data class Songs(var name: String,
                     var id: Int,
                     var pst: Int,
                     var t: Int,
                     var pop: Int,
                     var st: Int,
                     var rt: String,
                     var fee: Int,
                     var v: Int,
                     var crbt: Any,
                     var cf: String,
                     var al: Al,
                     var dt: Int,
                     var h: H,
                     var m: M,
                     var l: L,
                     var a: Any,
                     var cd: String,
                     var no: Int,
                     var rtUrl: Any,
                     var ftype: Int,
                     var djId: Int,
                     var copyright: Int,
                     var s_id: Int,
                     var rtype: Int,
                     var rurl: Any,
                     var mst: Int,
                     var cp: Int,
                     var mv: Int,
                     var publishTime: Long,
                     var ar: List<Ar>,
                     var alia: List<String>,
                     var rtUrls: List<String>) {
        data class Al(var id: Int,
                      var name: String,
                      var picUrl: String,
                      var pic_str: String,
                      var pic: Long,
                      var tns: List<String>)

        data class H(var br: Int,
                     var fid: Int,
                     var size: Int,
                     var vd: Int)

        data class M(var br: Int,
                     var fid: Int,
                     var size: Int,
                     var vd: Int)

        data class L(var br: Int,
                     var fid: Int,
                     var size: Int,
                     var vd: Int)

        data class Ar(var id: Int,
                      var name: String,
                      var tns: List<String>,
                      var alias: List<String>)
    }

    data class Privileges(var id: Int,
                          var fee: Int,
                          var payed: Int,
                          var st: Int,
                          var pl: Int,
                          var dl: Int,
                          var sp: Int,
                          var cp: Int,
                          var subp: Int,
                          var cs: Boolean,
                          var maxbr: Int,
                          var fl: Int,
                          var toast: Boolean,
                          var flag: Int)
}