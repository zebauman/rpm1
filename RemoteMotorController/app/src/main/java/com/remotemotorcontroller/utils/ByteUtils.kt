package com.remotemotorcontroller.utils

fun ByteArray.toHex(): String = joinToString(""){ "%02X".format(it.toInt() and 0xFF)}

fun String.hexToBytesOrNull(): ByteArray? =
    if(length % 2 == 0) runCatching{
        chunked(2).map{it.toInt(16).toByte()}.toByteArray()
    }.getOrNull() else null

