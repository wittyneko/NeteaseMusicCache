package com.wittyneko.neteasemusiccache

import android.util.Log
import kotlinx.coroutines.experimental.async
import okhttp3.*
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio
import org.jetbrains.anko.coroutines.experimental.bg
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * 文件下载
 * Created by wittyneko on 2017/11/2.
 */

private val progressInterceptor = ProgressInterceptor(null)

private val client = OkHttpClient.Builder()
        //.addInterceptor(progressInterceptor)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

fun asyncDownload(url: String, file: File, onFailure: ((e: IOException) -> Unit)? = null,
                  progressListener: ((bytesRead: Long, contentLength: Long, done: Boolean) -> Unit)?): Call? = run {

    if (url.isEmpty()) {
        onFailure?.invoke(IOException("空链接"))
        return null
    }

    val request = Request.Builder()
            .url(url)
            .build()

    val call = client.newCall(request)
    bg {
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //log("下载失败")
                onFailure?.invoke(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = ProgressResponseBody(response.body()!!, progressListener)
                body.source().inputStream().use { input ->
                    writeFile(input, file)
                }
            }
        })
    }
    call
}

fun writeFile(inputStream: InputStream, file: File) {

    file.parentFile?.mkdirs()
    file.apply { if (exists()) delete() }

    file.outputStream().use { out ->
        var len = 0
        val buffer = ByteArray(1024 * 4)
        while (inputStream.read(buffer).also { len = it } > 0) {
            out.write(buffer, 0, len)
        }
        out.flush()

    }
}

class ProgressInterceptor(var progressListener: ((bytesRead: Long,
                                                  contentLength: Long, done: Boolean) -> Unit)?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = run {
        val originResponse = chain.proceed(chain.request())
        originResponse.newBuilder()
                .body(ProgressResponseBody(originResponse.body()!!, progressListener))
                .build()
    }
}

class ProgressResponseBody(private val responseBody: ResponseBody,
                           val progressListener: ((bytesRead: Long, contentLength: Long, done: Boolean) -> Unit)?) : ResponseBody() {

    var bufferedSource: BufferedSource? = null

    override fun contentLength(): Long = responseBody.contentLength()

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun source(): BufferedSource = responseBody.source().let {
        var totalBytesRead = 0L
        if (bufferedSource == null) {
            val forwarding = object : ForwardingSource(it) {
                override fun read(sink: Buffer?, byteCount: Long) = run {
                    val bytesRead = super.read(sink, byteCount)
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0L
                    progressListener?.invoke(totalBytesRead, contentLength(), bytesRead == -1L)
                    bytesRead
                }
            }
            bufferedSource = Okio.buffer(forwarding)
        }
        bufferedSource!!
    }
}