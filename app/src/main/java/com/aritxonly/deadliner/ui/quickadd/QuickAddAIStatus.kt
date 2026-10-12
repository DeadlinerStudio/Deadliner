package com.aritxonly.deadliner.ui.quickadd

import com.deadliner.domain.ai.error.AIError
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.UnknownHostException

internal fun quickAddAIError(error: Exception): String = when (error) {
    AIError.MissingAPIKey -> "请先在 AI 设置中填写 API Key"
    AIError.EmptyResponse -> "AI 返回内容为空，请重试"
    AIError.ParsingFailed, is SerializationException -> "AI 返回格式无法解析，请检查模型与接口配置"
    is ResponseException -> when (error.response.status.value) {
        401, 403 -> "AI 接口鉴权失败，请检查 API Key"
        404 -> "AI 接口不存在，请检查接口地址与模型"
        429 -> "AI 接口限流或额度不足，请稍后重试"
        else -> "AI 接口请求失败（HTTP ${error.response.status.value}）"
    }
    is TimeoutCancellationException -> "AI 请求超时，请检查网络或稍后重试"
    is UnknownHostException -> "无法连接 AI 接口，请检查网络与接口地址"
    is IOException, is AIError.NetworkError -> "AI 网络请求失败，请检查网络与接口地址"
    is AIError.ConfigError -> "AI 配置无效，请检查接口地址与模型"
    else -> "AI 请求失败，请检查配置后重试"
}
