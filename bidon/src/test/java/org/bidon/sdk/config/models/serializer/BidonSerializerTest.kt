package org.bidon.sdk.config.models.serializer

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.json.jsonArray
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.sdk.utils.serializer.BidonSerializer
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable
import org.junit.Test

/**
 * Created by Bidon Team on 14/02/2023.
 */
internal class BidonSerializerTest {

    data class TestClass(
        @field:JsonName("title")
        val title: String,
        @field:JsonName("d")
        val doubleTrouble: Double,
        @field:JsonName("i2")
        val intFields: Int,
        @field:JsonName("boo_lean")
        val boo: Boolean,
        @field:JsonName("list_array")
        val list: List<String>,
        @field:JsonName("list_array2")
        val listInners: List<InnerTestClass>,
        @field:JsonName("inner_test")
        val inner: InnerTestClass,
        @field:JsonName("flo_at")
        val ignoreMe: Float?
    ) : Serializable {

        data class InnerTestClass(
            @field:JsonName("msg")
            val message: String,
            @field:JsonName("loop")
            val loop: Double,
        ) : Serializable
    }

    @Test
    fun `it should serialize`() {
        val result = BidonSerializer.serialize(
            TestClass(
                title = "str",
                doubleTrouble = 1.234,
                intFields = 28,
                boo = true,
                ignoreMe = null,
                inner = TestClass.InnerTestClass(
                    message = "message1",
                    loop = 999.9
                ),
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
        result.assertEquals(
            expectedJsonStructure {
                "title" hasValue "str"
                "d" hasValue 1.234
                "i2" hasValue 28
                "boo_lean" hasValue true
                "inner_test" hasJson expectedJsonStructure {
                    "msg" hasValue "message1"
                    "loop" hasValue 999.9
                }
                "list_array" hasArray jsonArray {
                    putValues(listOf("def", "abc"))
                }
                "list_array2" hasArray jsonArray {
                    putValues(
                        listOf(
                            jsonObject {
                                "msg" hasValue "message2"
                                "loop" hasValue 111.9
                            },
                            jsonObject {
                                "loop" hasValue 112.9
                                "msg" hasValue "message3"
                            },
                        )
                    )
                }
            }
        )
    }
}