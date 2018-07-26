package com.wittyneko.neteasemusiccache

import android.Manifest
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.wittyneko.neteasemusiccache.databinding.ActivityMainBinding
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import okhttp3.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.images.StandardArtwork
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.textResource
import org.jetbrains.anko.toast
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.xor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    val adapter = Adapter()
    val handler = Handler(Looper.getMainLooper())
    val PERMISSION_REQUEST = 110

    val dateFormat = SimpleDateFormat("yyyy")

    val baseUrl = "https://api.imjad.cn/"
    val TAG = "MainActivity"

    val suffixFlac = "flac"
    val flacFormat = byteArrayOf(0x66, 0x4C, 0x61, 0x43, 0x00, 0x00, 0x00, 0x22)
    var decryptJob: Job? = null
    var coveJob: Job? = null
    var lyricJob: Job? = null

    val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    val apiRetrofit = Retrofit.Builder()
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .build()
    val cacheMap = hashMapOf<String, Pair<String, String>>()

    val externalStorageDirectory = Environment.getExternalStorageDirectory()
    val inputDir = File(externalStorageDirectory, "netease/cloudmusic/Cache/Music1")
    val cacheLyricDir = File(externalStorageDirectory, "netease/cloudmusic/Cache/Lyric")

    val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    val neteaseDir = File(musicDir, "netease")
    val neteaseCacheDir = File(neteaseDir, "cache")
    val neteaseCoverDir = File(neteaseDir, "cover")
    val neteaseLyricDir = File(neteaseDir, "lyric")
    val neteaseMusicDir = File(neteaseDir, "music")
    val cacheFile = File(neteaseDir, "cache.txt")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lvList.adapter = adapter

        binding.btnDecrypt.onClick {
            if (decryptJob != null && decryptJob!!.isActive) {

                binding.btnDecrypt.textResource = R.string.cancel_await
                binding.btnDecrypt.isEnabled = false
                Log.e(TAG, "cancel")
                decryptJob?.cancelAndJoin()
                Log.e(TAG, "cancel join")
                decryptStop().join()
                Log.e(TAG, "write join")
                binding.btnDecrypt.textResource = R.string.decrypt
                binding.btnDecrypt.isEnabled = true
            } else {
                binding.tvProgress.text = ""
                adapter.clear()
                decryptLaunch()
            }
        }

        binding.btnCover.onClick {
            if (coveJob != null && coveJob!!.isActive) {

                binding.btnCover.textResource = R.string.cancel_await
                binding.btnCover.isEnabled = false
                coveJob?.cancelAndJoin()
                binding.btnCover.textResource = R.string.cover
                binding.btnCover.isEnabled = true
            } else {
                binding.tvProgress.text = ""
                adapter.clear()
                coverLaunch()
            }
        }

        binding.btnLyric.onClick {
            if (lyricJob != null && lyricJob!!.isActive) {

                binding.btnLyric.textResource = R.string.cancel_await
                binding.btnLyric.isEnabled = false
                lyricJob?.cancelAndJoin()
                binding.btnLyric.textResource = R.string.lyric
                binding.btnLyric.isEnabled = true
            } else {
                binding.tvProgress.text = ""
                adapter.clear()
                lyricLaunch()
            }
        }

        init()

        //decryptLaunch()
    }

    fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST)
            }
        }
        // 异常处理
        val catchHandler = object : Thread.UncaughtExceptionHandler {
            val defHandler = Thread.getDefaultUncaughtExceptionHandler()
            override fun uncaughtException(t: Thread, e: Throwable?) {
                Log.e(TAG, "thread: ${t.name}", e)
                defHandler.uncaughtException(t, e)
            }

        }
        Thread.setDefaultUncaughtExceptionHandler(catchHandler)
        Log.e(TAG, "${musicDir.absolutePath}\n" +
                "${neteaseDir.absolutePath}\n" +
                "${neteaseCacheDir.absolutePath}\n" +
                "${neteaseCoverDir.absolutePath}\n" +
                "${neteaseLyricDir.absolutePath}\n" +
                "${neteaseMusicDir.absolutePath}\n" +
                "${cacheFile.absolutePath}")
        TagOptionSingleton.getInstance().isAndroid = true
    }

    fun mkdirs() {
        neteaseCacheDir.mkdirs()
        neteaseCoverDir.mkdirs()
        neteaseLyricDir.mkdirs()
        neteaseMusicDir.mkdirs()
    }

    fun lyricLaunch() = launch(UI) {
        binding.btnLyric.textResource = R.string.cancel

        val deferred = async {
            // 获取缓存列表
            val musicFiles = neteaseMusicDir.listFiles()
            showInfo("开始 ${System.currentTimeMillis()}")
            musicFiles.forEach {
                val fileName = parseFile(it)
                showInfo(">. 检测文件 \n${fileName.file.name}")

                val audio = AudioFileIO.read(fileName.file)
                val header = audio.audioHeader
                Log.e(TAG, "format: ${header.format}")
                val tag = audio.tag

                val isLyric = tag.hasField(FieldKey.LYRICS)
                if (!isLyric) {
                    // 下载封面
                    showInfo(">. 获取歌词信息")
                    val response = apiRetrofit.create(ApiInterface::class.java)
                            .getLyric(fileName.id)
                            .execute()
                    Log.e(TAG, "response: ${response?.body().toString()}")
                    val json = GsonBuilder().setPrettyPrinting().create().fromJson(response.body(), SongLyric::class.java)
                    val lrc = json.lrc.lyric
                    val file = File(neteaseCoverDir, "${fileName.id}-${fileName.br}-${fileName.md5}.lrc")
                    file.apply { if (exists()) delete() }
                    file.writeText(lrc)
                    //tag.addField(FieldKey.LYRICS, lrc)

                    showInfo(">. 写入歌词 \n" +
                            "cover: $lrc"
                    )

                } else {
                    showInfo("已有歌词")
                }
                //audio.commit()
                tag.fields.forEach {
                    Log.e(TAG, "tag: ${it.id}, ${tag.getFirst(it.id)}")
                }
                if (tag is FlacTag) {
                    tag.images.forEach {
                        Log.e(TAG, "imgFlac: $it")
                    }
                } else if (tag is AbstractID3v2Tag) {
                    tag.artworkList.forEach {
                        Log.e(TAG, "imgID3: ${it.imageUrl}, $it")
                    }
                }

                showInfo("------------------------").join()
            }
            async(UI) {
                binding.tvProgress.textResource = R.string.finish
                binding.btnLyric.textResource = R.string.lyric
                Unit
            }.join()
            Log.e(TAG, "结束 ${System.currentTimeMillis()}")
            Unit
        }
        lyricJob = deferred
        Log.e(TAG, "完成: ${deferred.await()}")
    }

    fun coverLaunch() = launch(UI) {
        binding.btnCover.textResource = R.string.cancel

        val deferred = async {
            // 获取缓存列表
            val musicFiles = neteaseCacheDir.listFiles()
            showInfo("开始 ${System.currentTimeMillis()}")
            musicFiles.forEach {
                val fileName = parseFile(it)
                showInfo(">. 检测文件 \n${fileName.file.name}")

                val audio = AudioFileIO.read(fileName.file)
                val header = audio.audioHeader
                Log.e(TAG, "format: ${header.format}")
                val tag = audio.tag

                val isCover = if (tag is FlacTag) {
                    tag.images.isNotEmpty()
                } else if (tag is AbstractID3v2Tag) {
                    tag.artworkList.isNotEmpty()
                } else {
                    false
                }
                if (!isCover) {
                    // 下载封面
                    showInfo(">. 获取封面信息")
                    val response = getName(fileName)
                    Log.e(TAG, "response: ${response?.body().toString()}")
                    val json = Gson().fromJson(response.body(), SongDetail::class.java)
                    val song = json.songs[0]
                    val cover = song.al.picUrl
                    val file = File(neteaseCoverDir, "${fileName.id}-${fileName.br}-${fileName.md5}${cover.run { substring(lastIndexOf('.')) }}")
                    file.apply { if (exists()) delete() }


                    val timer = Timer(0.0, 60) { time, pair ->
                        async(UI) {
                            //binding.tvProgress.text = "${String.format("%.2f", pair.second * 100)} %"
                            binding.tvProgress.text = "进度: ${(pair.second * 100).roundToInt()} %"
                        }
                    }

                    val coverResponses = client.newCall(Request.Builder().url(cover).build()).execute()
                    ProgressResponseBody(coverResponses.body()!!) { bytesRead, contentLength, done ->

                        val progress = bytesRead / contentLength.toDouble()
                        timer.add(progress)
                    }.source().inputStream().use { input ->
                        writeFile(input, file)
                    }

                    tag.setField(StandardArtwork.createArtworkFromFile(file))
                    showInfo(">. 写入封面 \n" +
                            "cover: $cover"
                    )

                } else {
                    showInfo("已有封面")
                }
                audio.commit()
                tag.fields.forEach {
                    Log.e(TAG, "tag: ${it.id}, ${tag.getFirst(it.id)}")
                }
                if (tag is FlacTag) {
                    tag.images.forEach {
                        Log.e(TAG, "imgFlac: $it")
                    }
                } else if (tag is AbstractID3v2Tag) {
                    tag.artworkList.forEach {
                        Log.e(TAG, "imgID3: ${it.imageUrl}, $it")
                    }
                }

                showInfo("------------------------").join()
            }
            async(UI) {
                binding.tvProgress.textResource = R.string.finish
                binding.btnCover.textResource = R.string.cover
                Unit
            }.join()
            Log.e(TAG, "结束 ${System.currentTimeMillis()}")
            Unit
        }
        coveJob = deferred
        Log.e(TAG, "完成: ${deferred.await()}")
    }

    fun decryptStop() = async {
        cacheFile.outputStream().use {
            val output = BufferedOutputStream(it)
            cacheMap.forEach { key, value ->
                output.write("$key-${value.first}-${value.second}\n".toByteArray())
            }
            output.flush()
        }
    }

    fun decryptLaunch() = launch(UI) {
        binding.btnDecrypt.textResource = R.string.cancel
        val pool = newSingleThreadContext("single-pool")
        // 获取已解析列表
        async {
            cacheMap.clear()
            // Plan A
            neteaseCacheDir.listFiles { file ->
                val name = file.name.toLowerCase()
                //arrayListOf(".flac", ".mp3")
                name.endsWith(".flac") || name.endsWith(".mp3")
            }.forEach { file ->
                val fileName = parseFile(file)
                Log.e(TAG, "file: $file")
                cacheMap[fileName.id]?.let {
                    if (fileName.br.toInt() > it.first.toInt()) {
                        cacheMap.put(fileName.id, Pair(fileName.br, fileName.md5))
                    }
                } ?: run { cacheMap.put(fileName.id, Pair(fileName.br, fileName.md5)) }
            }

            // Plan B
            if (cacheFile.exists()) {
                cacheFile.forEachLine {
                    //Log.e(TAG, "read: $it")
                    val listSplit = it.split('-')
                    //val id = name.substring(0 until name.indexOfFirst { it == '-' })
                    val id = listSplit[0]
                    val br = listSplit[1]
                    var md5 = listSplit[2]
                    cacheMap[id]?.let {
                        if (br.toInt() > it.first.toInt()) {
                            cacheMap.put(id, Pair(br, md5))
                        }
                    } ?: run {
                        cacheMap.put(id, Pair(br, md5))
                    }
                }
            }
        }.join()

        // 获取缓存列表
        val cacheFileList = async {

            val musicFile = inputDir.listFiles { dir, name ->
                //Log.e(TAG, "$name, $dir")
                name.endsWith("uc!", true)
            }
            //Log.e(TAG, "async cache file list")
            musicFile

        }
        val musicFiles = cacheFileList.await()
        //Log.e(TAG, "join cache file list")
        val decrypt = async {
            showInfo("开始 ${System.currentTimeMillis()}")
            musicFiles.forEach {
                val fileName = parseFile(it)
                if (checkFile(fileName)) {
                    showInfo(">. 开始解码 \n" +
                            "id: ${fileName.id}\n" +
                            "br: ${fileName.br}\n" +
                            "md5: ${fileName.md5}"
                    )
                    val file = decrypt(fileName)
                    showInfo(">. 获取作品信息")
                    val response = getName(fileName)
                    Log.e(TAG, "response: ${response?.body().toString()}")
                    val json = Gson().fromJson(response.body(), SongDetail::class.java)
                    val song = json.songs[0]
                    val title = song.name
                    val artist = song.ar.joinToString("/") { it.name }
                    val album = song.al.name
                    val cover = song.al.picUrl
                    val year = dateFormat.format(Date(song.publishTime))
                    setAudioTag(file, title, artist, album, year)
                    cacheMap.put(fileName.id, Pair(fileName.br, fileName.md5))
                    showInfo(">. 写入作品信息 \n" +
                            "title: $title \n" +
                            "artist: $artist \n" +
                            "album: $album \n" +
                            "year: $year"
                    )
                } else {
                    Log.e(TAG, "invalid file")
                }
                showInfo("------------------------").join()
            }
            decryptStop().join()
            async(UI) {
                binding.tvProgress.textResource = R.string.finish
                binding.btnDecrypt.textResource = R.string.decrypt
                Unit
            }.join()
            Log.e(TAG, "结束 ${System.currentTimeMillis()}")
            Unit
        }
        decryptJob = decrypt
        Log.e(TAG, "完成: ${decrypt.await()}")


    }

    fun decrypt(fileName: FileName) = run {
        val inputFile = fileName.file

        //val name = fileName.file.name
        //val outputName = name.substring(0 until name.lastIndexOf('.'))
        val outputName = "${fileName.id}-${fileName.br}-${fileName.md5}"
        val outputFile = File(neteaseCacheDir, outputName)

        outputFile.apply { if (exists()) delete() }
        outputFile.outputStream().use { output ->
            val length = inputFile.length().toDouble()
            var readSize = 0L

            val timer = Timer(0.0, 60) { time, pair ->
                uiThread {
                    //binding.tvProgress.text = "${String.format("%.2f", pair.second * 100)} %"
                    binding.tvProgress.text = "进度: ${(pair.second * 100).roundToInt()} %"
                }
            }
            //val originMd5Digest = MessageDigest.getInstance("MD5")
            //val decryptMd5Digest = MessageDigest.getInstance("MD5")
            var count = 0
            inputFile.forEachBlock(DEFAULT_BUFFER_SIZE) { buffer, bytesRead ->
                val bufferRead = if (buffer.size != bytesRead) {
                    //Log.e(TAG, "buffer ${buffer.size}, $bytesRead")
                    buffer.sliceArray(0..bytesRead)
                } else {
                    buffer
                }
                val bufferConvert = bufferRead.map { it xor 0xa3.toByte() }.toByteArray()
                //originMd5Digest.update(bufferRead)
                //decryptMd5Digest.update(bufferConvert)
                output.write(bufferConvert)
                readSize += bytesRead
                val progress = readSize / length
                timer.add(progress)
                if (count == 0) {
                    val format = bufferConvert.run { if (size < flacFormat.size) this else sliceArray(0 until flacFormat.size) }
                    val isFalc = format.contentEquals(flacFormat)
                    if (isFalc) {
                        fileName.format = suffixFlac
                    }
                    showInfo("格式: ${fileName.format}: ${format.joinToString { it.toString(16) }}")
                }
                count++
                //Log.e(TAG, "read $readSize, $length $progress")
            }
            //val originMd5Byte = originMd5Digest.digest()
            //val decryptMd5Byte = decryptMd5Digest.digest()
            //val originMd5 = originMd5Byte.joinToString {
            //    val md5 = Integer.toHexString((it.toInt() and 0xff))
            //    if (md5.length < 2) "0$md5" else md5
            //}
            //val decryptMd5 = decryptMd5Byte.joinToString {
            //    val md5 = Integer.toHexString((it.toInt() and 0xff))
            //    if (md5.length < 2) "0$md5" else md5
            //}
            //Log.e(TAG, "md5: $originMd5")
            //Log.e(TAG, "md5: $decryptMd5")
            timer.stop()
            showInfo("大小: $readSize, ${length.roundToLong()}")
            Unit
        }
        val musicFile = File("${outputFile.absoluteFile}.${fileName.format}")
        musicFile.apply { if (exists()) delete() }
        outputFile.renameTo(musicFile)

        musicFile
    }

    fun setAudioTag(file: File, title: String, artist: String, album: String, year: String) {

        val audio = AudioFileIO.read(file)
        val header = audio.audioHeader
        Log.e(TAG, "format: ${header.format}")
        val tag = audio.tag
        tag.apply {
            setField(FieldKey.TITLE, title)
            setField(FieldKey.ARTIST, artist)
            setField(FieldKey.ALBUM, album)
            //setField(StandardArtwork.createArtworkFromFile())
            val yearTag = createField(FieldKey.YEAR, year)
            //Log.e(TAG, "yearID: ${yearTag.id} $year")
            if (!hasField(yearTag.id)) setField(yearTag)
        }
        audio.commit()
//        tag.fields.forEach {
//            Log.e(TAG, "tag: ${it.id}, ${tag.getFirst(it.id)}")
//        }
//        if (tag is FlacTag) {
//            tag.images.forEach {
//                Log.e(TAG, "imgFlac: $it")
//            }
//        } else if (tag is AbstractID3v2Tag) {
//            tag.artworkList.forEach {
//                Log.e(TAG, "imgID3: ${it.imageUrl}, $it")
//            }
//        }
    }

    fun getName(fileName: FileName) = run {
        val json = apiRetrofit.create(ApiInterface::class.java)
                .getDetail(fileName.id)
                .execute()
        json
    }

    fun checkFile(fileName: FileName) = run {
        showInfo(">. 检测文件 \n${fileName.file.name}")

        cacheMap[fileName.id]?.let {
            val isHightBr = fileName.br.toInt() > it.first.toInt()
            if (!isHightBr) showInfo("已存在更高解析文件")
            isHightBr
        } ?: run {
            val idxFile = File(fileName.file.parent, "${fileName.id}-${fileName.br}-${fileName.md5}.${fileName.suffix}.idx!")
            val error = if (idxFile.isFile) {
                try {
                    val json = Gson().fromJson(idxFile.readText(), JsonObject::class.java)
                    json["filesize"].asLong <= fileName.file.length()
                } catch (e: Throwable) {
                    false
                }
            } else false
            if (!error) showInfo("无效文件")
            error
        }
    }

    fun parseFile(file: File) = run {

        val name = file.name
        val listSplit = name.split('-')
        //val id = name.substring(0 until name.indexOfFirst { it == '-' })
        val id = listSplit[0]
        val br = listSplit[1]
        var md5Name = listSplit[2]
        //val md5 = md5Name.substring(0 until md5Name.indexOf('.'))
        val titleSplit = md5Name.split('.')
        val md5 = titleSplit[0]
        val suffix = titleSplit.getOrElse(1, { "" })
        Log.e(TAG, "id: $id, $br, $md5Name, $name")
        //Log.e(TAG, "md5: ${titleSplit[0]}, ${titleSplit[1]}, ${titleSplit[2]}")
        FileName(file, id, br, md5, suffix)
    }

    fun parseName(name: String) =run {

        val listSplit = name.split('-')
        //val id = name.substring(0 until name.indexOfFirst { it == '-' })
        val id = listSplit[0]
        val br = listSplit[1]
        var md5Name = listSplit[2]
        //val md5 = md5Name.substring(0 until md5Name.indexOf('.'))
        val titleSplit = md5Name.split('.')
        val md5 = titleSplit[0]
        val suffix = titleSplit.getOrElse(1, { "" })
        Log.e(TAG, "id: $id, $br, $md5Name, $name")
        //Log.e(TAG, "md5: ${titleSplit[0]}, ${titleSplit[1]}, ${titleSplit[2]}")
        FileName(File(""), id, br, md5, suffix)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun showInfo(info: String) = async(UI) {
        Log.e(TAG, info)
        adapter.addData(info)
        binding.lvList.scrollToPosition(adapter.list.size - 1)
    }

    inline fun uiThread(crossinline block: () -> Unit) {
        if (Looper.getMainLooper().thread == Thread.currentThread()) {
            block()
        } else {
            handler.post { block() }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                toast("权限获取失败")
            } else {
                mkdirs()
            }
        }
    }

    class FileName(
            var file: File,
            var id: String,
            var br: String,
            var md5: String,
            var suffix: String,
            var format: String = suffix
    )

    class Timer<T>(def: T, val delay: Long = 1000, val block: (time: Long, Pair<T, T>) -> Unit) {
        val START = -1L
        val STOP = -2L
        var startTime = STOP
        var first = def
        var second = def

        val isStart get() = startTime != STOP

        @Suppress("NOTHING_TO_INLINE")
        private inline fun start(value: T = first) {
            if (startTime == STOP) {
                startTime = START
                first = value
                second = value
                block(startTime, Pair(first, second))
                startTime = System.currentTimeMillis()
            }
        }

        fun stop() {
            startTime = STOP
            block(startTime, Pair(first, second))
        }

        fun add(value: T) {
            start(value)

            second = value
            val currentTime = System.currentTimeMillis()
            if (currentTime - startTime >= delay) {
                block(startTime, Pair(first, second))
                first = second
                startTime = currentTime
            }
        }
    }

    class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        val list = mutableListOf<String>()
        var id: String = ""
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = run {
            ViewHolder(TextView(parent.context).apply { backgroundColor = Color.WHITE })
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val textView = holder.itemView as TextView
            val item = list[position]
            textView.text = item
        }

        fun addData(value: String) {
            list += value
            notifyItemChanged(list.size)
        }

        fun clear() {
            list.clear()
            notifyDataSetChanged()
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}
    }
}
