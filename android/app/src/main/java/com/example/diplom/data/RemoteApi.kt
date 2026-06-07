package com.example.diplom.data

import android.content.Context
import android.net.Uri
import com.example.diplom.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RemoteApi {
    lateinit var api: ApiService
        private set

    fun init() {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val b = chain.request().newBuilder()
                Session.token?.let { b.header("Authorization", "Bearer $it") }
                chain.proceed(b.build())
            }
            .addInterceptor(logging)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(ApiService::class.java)
    }

    suspend fun uploadAvatar(context: Context, uri: Uri): AvatarUploadResponse = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        val mime = cr.getType(uri) ?: "image/jpeg"
        val ext = when {
            mime.equals("image/png", ignoreCase = true) -> "png"
            mime.equals("image/webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val bytes = cr.openInputStream(uri)!!.use { it.readBytes() }
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("avatar", "avatar.$ext", body)
        api.uploadAvatar(part)
    }
}
