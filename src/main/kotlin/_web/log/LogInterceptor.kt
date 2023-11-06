package com.wafflestudio.seminar.spring2023._web.log

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.lang.Exception

/**
 * 1. preHandle을 수정하여 logRequest
 * 2. preHandle, afterCompletion을 수정하여 logSlowResponse
 */
@Component
class LogInterceptor(
    private val logRequest: LogRequest,
    private val logSlowResponse: AlertSlowResponse,
) : HandlerInterceptor {
    private val startTime = ThreadLocal<Long>()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        startTime.set(System.currentTimeMillis())
        logRequest.invoke(Request(method = request.method, path = request.contextPath))
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime.get()
        if (processingTime >= 3000) {
            logSlowResponse.invoke(SlowResponse(request.method, request.contextPath, processingTime))
        }
    }
}
