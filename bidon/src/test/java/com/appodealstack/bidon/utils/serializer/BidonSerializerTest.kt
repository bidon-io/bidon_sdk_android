package com.appodealstack.bidon.utils.serializer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 14/02/2023.
 */
internal class BidonSerializerTest {

    private val testee by lazy {
        BidonSerializer
    }

    data class TestClass(
        @field:JsonFieldName("title")
        val title: String,
        @field:JsonFieldName("d")
        val doubleTrouble: Double,
        @field:JsonFieldName("i2")
        val intFields: Int,
        @field:JsonFieldName("boo_lean")
        val boo: Boolean,
        @field:JsonFieldName("list_array")
        val list: List<String>,
        @field:JsonFieldName("list_array2")
        val listInners: List<InnerTestClass>,
//        @field:JsonFieldName("inner_test")
//        val inner: InnerTestClass,
        @field:JsonFieldName("flo_at")
        val ignoreMe: Float
    ) : Serializable {

        data class InnerTestClass(
            @field:JsonFieldName("msg")
            val message: String,
            @field:JsonFieldName("loop")
            val loop: Double,
        ) : Serializable
    }

    @Test
    fun `it should parse`() {
        val srcJson =
            """{"flo_at":44.234,"d":1.234,"list_array":["abc","def"],"list_array2":[{"msg":"message2","loop":111.9},{"msg":"message3","loop":112.9}],"i2":28,"inner_test":{"msg":"message1","loop":999.9},"title":"str","boo_lean":true}"""
        val result = testee.parse<TestClass>(srcJson)
        println(result)
        assertThat(result).isEqualTo(
            TestClass(
                title = "str",
                doubleTrouble = 1.234,
                intFields = 28,
                boo = true,
                ignoreMe = 44.234f,
//                inner = TestClass.InnerTestClass(
//                    message = "message1",
//                    loop = 999.9
//                ),
                list = listOf("abc", "def"),
                listInners = listOf(
                    TestClass.InnerTestClass(
                        message = "message2",
                        loop = 111.9
                    ),
                    TestClass.InnerTestClass(
                        message = "message3",
                        loop = 112.9
                    ),
                )
            )
        )
        error("---")
    }

    @Test
    fun `it should serialize`() {
        val result = testee.serialize(
            TestClass(
                title = "str",
                doubleTrouble = 1.234,
                intFields = 28,
                boo = true,
                ignoreMe = 44.234f,
//                inner = TestClass.InnerTestClass(
//                    message = "message1",
//                    loop = 999.9
//                ),
                list = listOf("abc", "def"),
                listInners = listOf(
                    TestClass.InnerTestClass(
                        message = "message2",
                        loop = 111.9
                    ),
                    TestClass.InnerTestClass(
                        message = "message3",
                        loop = 112.9
                    ),
                )
            )
        )
        assertThat(result.toString()).isEqualTo(
            """
            {
              "d": 1.234,
              "list_array": [
                "abc",
                "def"
              ],
              "list_array2": [
                {
                  "msg": "message2",
                  "loop": 111.9
                },
                {
                  "msg": "message3",
                  "loop": 112.9
                }
              ],
              "i2": 28,
              "inner_test": {
                "msg": "message1",
                "loop": 999.9
              },
              "title": "str",
              "boo_lean": true
            }
        """.trimIndent()
        )
    }
}