package com.appodealstack.bidon.utilities.datasource.session

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug
import android.os.Environment
import android.os.StatFs
import android.text.TextUtils
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInternal
import java.io.*
import java.lang.ref.WeakReference
import java.util.regex.Pattern

internal class SessionDataSourceImpl(
    private val context: Context,
    private val sessionTracker: SessionTracker,
) : SessionDataSource {

    private var weakActivityManager: WeakReference<ActivityManager>? = null

    private val maxCpuFrequency: MutableMap<Int, Float> = HashMap()
    private val minCpuFrequency: MutableMap<Int, Float> = HashMap()

    private var totalStorageSize: Long = 0
    private var totalRamSize: Long = 0
    private var coreCount = 0

    /**
     * Get general free storage space of device using [StatFs].
     *
     * @return free storage space in bytes.
     */
    @Suppress("DEPRECATION")
    override fun getStorageFree(): Long = try {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        stat.availableBlocks.toLong() * stat.blockSize.toLong()
    } catch (throwable: Throwable) {
        logError(Tag, throwable.message ?: "", throwable)
        0L
    }

    override fun getStorageUsed(): Long = try {
        getStorageSize() - getStorageFree()
    } catch (throwable: Throwable) {
        logError(Tag, throwable.message ?: "", throwable)
        0L
    }

    /**
     * Get total ram size using [ActivityManager.MemoryInfo].
     *
     * @return total ram size in bytes.
     */
    override fun getRamSize(): Long = totalRamSize.takeIf { it != 0L }
        ?: try {
            getMemoryInfo(context).totalMem.also { totalRamSize = it }
        } catch (e: Exception) {
            logError(Tag, e.message ?: "", e)
            0L
        }

    /**
     * Get free ram size using [android.os.Debug.MemoryInfo].
     *
     * @return used ram value in bytes.
     */
    override fun getRamUsed(): Long = try {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        memInfo.totalPss * 1024L
    } catch (throwable: Throwable) {
        logError(Tag, throwable.message ?: "", throwable)
        0
    }

    /**
     * Get cpu usage
     *
     * @return cpu usage in the range from 0 to 1.
     */
    override fun getCpuUsage(): Float = try {
        val coreCount = getNumCores()
        var freqSum = 0f
        var minFreqSum = 0f
        var maxFreqSum = 0f
        for (i in 0 until coreCount) {
            freqSum += getCurCpuFreq(i)
            minFreqSum += getMinCpuFreq(i)
            maxFreqSum += getMaxCpuFreq(i)
        }
        getAverageClock(freqSum, minFreqSum, maxFreqSum)
    } catch (throwable: Throwable) {
        logError(Tag, throwable.message ?: "", throwable)
        0f
    }

    override fun getId(): String = sessionTracker.sessionId
    override fun getLaunchTs(): Long = sessionTracker.launchTs
    override fun getLaunchMonotonicTs(): Long = sessionTracker.launchMonotonicTs
    override fun getStartTs(): Long = sessionTracker.startTs
    override fun getMonotonicStartTs(): Long = sessionTracker.startMonotonicTs
    override fun getTs(): Long = sessionTracker.ts
    override fun getMonotonicTs(): Long = sessionTracker.monotonicTs
    override fun getMemoryWarningsTs(): List<Long> = sessionTracker.memoryWarningsTs
    override fun getMemoryWarningsMonotonicTs(): List<Long> = sessionTracker.memoryWarningsMonotonicTs

    override fun getBattery(): Float {
        try {
            val batteryStatus: Intent? = getBatteryIntent(context)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    val percentMultiplier = 100.0f
                    return level.toFloat() / scale.toFloat() * percentMultiplier
                }
            }
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return NoBatteryData
    }

    private fun getBatteryIntent(context: Context): Intent? {
        return context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val activityManager: ActivityManager = getActivityManager(context)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }

    /**
     * Get general storage size of device using [StatFs].
     *
     * @return storage size in bytes.
     */
    private fun getStorageSize(): Long {
        try {
            if (totalStorageSize == 0L) {
                val stat = StatFs(Environment.getDataDirectory().absolutePath)
                totalStorageSize = stat.blockCountLong * stat.blockSizeLong
            }
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return totalStorageSize
    }

    /**
     * Get [ActivityManager] from [WeakReference] or create new.
     *
     * @return [ActivityManager].
     */
    private fun getActivityManager(context: Context): ActivityManager =
        weakActivityManager?.get()
            ?: (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).also {
                weakActivityManager = WeakReference(it)
            }

    /**
     * Calculate cpu usage.
     *
     * @return cpu usage in the range from 0 to 1.
     */
    private fun getAverageClock(
        currentFreqSum: Float,
        minFreqSum: Float,
        maxFreqSum: Float
    ): Float {
        val diff = maxFreqSum - minFreqSum
        return when {
            diff <= 0 -> 0f
            maxFreqSum >= 0 -> (currentFreqSum - minFreqSum) / diff
            else -> 0f
        }
    }

    /**
     * Get number of cores using contents of the system folder /sys/devices/system/cpu/
     *
     * @return core count of CPU.
     */
    private fun getNumCores(): Int = coreCount.takeIf { it != 0 }
        ?: try {
            val dir = File("/sys/devices/system/cpu/")
            val files = dir.listFiles { pathname ->
                Pattern.matches("cpu[0-9]", pathname.name)
            }
            (files?.size ?: Runtime.getRuntime().availableProcessors()).also {
                coreCount = it
            }
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
            Runtime.getRuntime().availableProcessors().also {
                coreCount = it
            }
        }

    /**
     * Get current frequency of core using contents of the system folder
     * /sys/devices/system/cpu/cpu%s/cpufreq/scaling_cur_freq
     *
     * @return current frequency of core in Hz
     */
    private fun getCurCpuFreq(coreNum: Int): Float {
        val path = String.format("/sys/devices/system/cpu/cpu%s/cpufreq/scaling_cur_freq", coreNum)
        return readIntegerFile(path)
    }

    /**
     * Get max frequency of core using contents of the system folder
     * /sys/devices/system/cpu/cpu%s/cpufreq/cpuinfo_max_freq
     *
     * @return max frequency of core in Hz
     */
    private fun getMaxCpuFreq(coreNum: Int): Float {
        val maxCpuFrequency = maxCpuFrequency
        return if (maxCpuFrequency.containsKey(coreNum)) {
            maxCpuFrequency[coreNum] ?: 0f
        } else {
            val path =
                String.format("/sys/devices/system/cpu/cpu%s/cpufreq/cpuinfo_max_freq", coreNum)
            val result = readIntegerFile(path)
            if (result > 0) {
                maxCpuFrequency[coreNum] = result
            }
            result
        }
    }

    /**
     * Get current frequency of core using contents of the system folder
     * /sys/devices/system/cpu/cpu%s/cpufreq/scaling_cur_freq
     *
     * @return current frequency of core in Hz
     */
    private fun getMinCpuFreq(coreNum: Int): Float {
        val minCpuFrequency = minCpuFrequency
        return if (minCpuFrequency.containsKey(coreNum)) {
            minCpuFrequency[coreNum] ?: 0f
        } else {
            val path = String.format("/sys/devices/system/cpu/cpu%s/cpufreq/cpuinfo_min_freq", coreNum)
            val result = readIntegerFile(path)
            if (result > 0) {
                minCpuFrequency[coreNum] = result
            }
            result
        }
    }

    /**
     * Helpful method to read info about cpu from device.
     *
     * @return [Float] value from file.
     */
    private fun readIntegerFile(filePath: String): Float {
        var fileInputStream: Closeable? = null
        var inputStreamReader: Closeable? = null
        var bufferedReader: Closeable? = null
        val line: String
        try {
            fileInputStream = FileInputStream(filePath)
            inputStreamReader = InputStreamReader(fileInputStream)
            bufferedReader = BufferedReader(inputStreamReader, 1024)
            line = bufferedReader.readLine()
            if (!TextUtils.isEmpty(line)) {
                return line.toFloat()
            }
        } catch (e: Throwable) {
            // ignore
        } finally {
            close(fileInputStream)
            close(inputStreamReader)
            close(bufferedReader)
        }
        return 0f
    }

    private fun close(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: Throwable) {
            logError(message = e.message ?: "", error = e)
        }
    }
}

private const val Tag = "SessionDataSource"

private const val NoBatteryData = -1f