package com.appodealstack.bidon

import com.appodealstack.bidon.utilities.network.HttpClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object TempApi {
    private const val BaseUrl = "https://herokuapp.appodeal.com/android_bidon_config"
//    private const val BaseUrl = "https://1e69e7f9-a8f2-4cc2-9d30-5a71dd5d6db2.mock.pstmn.io"

    fun start() {
        GlobalScope.launch {
            HttpClient.Json.enqueue(
                method = HttpClient.Method.POST,
                url = BaseUrl,
                parser = {
                    String(it ?: byteArrayOf())
                },
                body = byteArrayOf(),
                useUniqueRequestId = false
            ).onSuccess {

            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}