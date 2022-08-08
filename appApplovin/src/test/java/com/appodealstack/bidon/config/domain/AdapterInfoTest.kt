package com.appodealstack.bidon.config.domain

import org.junit.Test

class AdapterInfoTest {

    @Test
    fun `annotation test`(){
        val adapterInfo = AdapterInfo(
            id = "a1",
            adapterVersion = "b2",
            bidonSdkVersion = "c3"
        )
        val ann = adapterInfo::class.java.getAnnotation(JsonFieldName::class.java)
        println(adapterInfo)
        println(ann)

        // Truth.assertThat(ann)
    }
}