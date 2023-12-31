package org.bidon.sdk.databinders.session

import org.bidon.sdk.databinders.DataSource

internal interface SessionDataSource : DataSource {
    fun getId(): String
    fun getLaunchTs(): Long
    fun getLaunchMonotonicTs(): Long
    fun getStartTs(): Long
    fun getMonotonicStartTs(): Long
    fun getTs(): Long
    fun getMonotonicTs(): Long
    fun getMemoryWarningsTs(): List<Long>
    fun getMemoryWarningsMonotonicTs(): List<Long>
    fun getRamUsed(): Long
    fun getRamSize(): Long
    fun getStorageFree(): Long
    fun getStorageUsed(): Long
    fun getBattery(): Float
    fun getCpuUsage(): Float
}