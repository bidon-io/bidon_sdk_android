package com.appodealstack.bidon.core.ext

fun ByteArray.toHexString(): String {
    val hexArray = "0123456789abcdef".toCharArray()
    val hexChars = CharArray(this.size * 2)
    for (i in this.indices) {
        val v = this[i].toInt() and 0xFF
        hexChars[i * 2] = hexArray[v ushr 4]
        hexChars[i * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}
