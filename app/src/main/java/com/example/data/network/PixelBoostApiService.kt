package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val status: String,
    val service: String? = null,
    val device: String? = null,
    @Json(name = "model_weights_loaded") val modelWeightsLoaded: Boolean? = null,
    @Json(name = "max_upload_mb") val maxUploadMb: Int? = null,
    @Json(name = "max_output_pixels") val maxOutputPixels: Long? = null,
    @Json(name = "supported_resolutions") val supportedResolutions: List<String>? = null,
    @Json(name = "supported_modes") val supportedModes: List<String>? = null
)

interface PixelBoostApiService {
    @GET("/health")
    suspend fun checkHealth(): Response<HealthResponse>

    @Multipart
    @POST("/enhance")
    suspend fun enhanceImage(
        @Part file: MultipartBody.Part,
        @Part("quality") quality: RequestBody,
        @Part("mode") mode: RequestBody,
        @Part("output_format") outputFormat: RequestBody,
        @Part("jpeg_quality") jpegQuality: RequestBody,
        @Part("enable_face_enhance") enableFaceEnhance: RequestBody
    ): Response<ResponseBody>

    companion object {
        fun create(baseUrl: String): PixelBoostApiService {
            val validUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(240, TimeUnit.SECONDS) // Allow ample time for 16K deep tiling inference
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(validUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(PixelBoostApiService::class.java)
        }
    }
}
