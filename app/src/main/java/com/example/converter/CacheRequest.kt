package com.example.converter


import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser


// класс для кастомного запроза для поддержки кэширования
class CacheRequest(
    method: Int,
    url: String,
    listener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
) :
    Request<NetworkResponse>(method, url, errorListener) {

    private val mListener: Response.Listener<NetworkResponse> = listener
    private val mErrorListener: Response.ErrorListener = errorListener

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {

        var cacheEntry = HttpHeaderParser.parseCacheHeaders(response)

        if (cacheEntry == null) {
            cacheEntry = Cache.Entry()
        }
        val cacheHitButRefreshed =
            (3 * 60 * 1000).toLong()
        val cacheExpired = (24 * 60 * 60 * 1000).toLong()

        val now = System.currentTimeMillis()
        val softExpire = now + cacheHitButRefreshed
        val ttl = now + cacheExpired

        cacheEntry.data = response.data
        cacheEntry.softTtl = softExpire
        cacheEntry.ttl = ttl

        var headerValue = response.headers["Date"]
        if (headerValue != null) {
            cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue)
        }
        headerValue = response.headers["Last-Modified"]
        if (headerValue != null) {
            cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue)
        }

        cacheEntry.responseHeaders = response.headers
        return Response.success(response, cacheEntry)
    }

    override fun deliverResponse(response: NetworkResponse) {
        mListener.onResponse(response)
    }

    override fun parseNetworkError(volleyError: VolleyError): VolleyError {
        return super.parseNetworkError(volleyError)
    }

    override fun deliverError(error: VolleyError) {
        mErrorListener.onErrorResponse(error)
    }
}