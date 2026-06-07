package com.example.diplom.data

import com.google.gson.Gson
import retrofit2.HttpException

class ApiException(val code: String?, message: String, val httpCode: Int = 0) : Exception(message)

suspend fun <T> apiCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: HttpException) {
        val raw = e.response()?.errorBody()?.string()
        val parsed = runCatching { Gson().fromJson(raw, ApiErrorBody::class.java) }.getOrNull()
        val msg = parsed?.error?.message ?: raw ?: e.message()
        val code = parsed?.error?.code
        Result.failure(ApiException(code, msg ?: "Ошибка сети", e.code()))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
