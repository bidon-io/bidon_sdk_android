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
import java.io.*
import java.lang.ref.WeakReference
import java.util.regex.Pattern

internal class SessionDataSourceImpl(
    private val context: Context,
) : SessionDataSource {

    private var weakActivityManager: WeakReference<ActivityManager>? = null
    private val MAX_CPU_FREQUENCY: MutableMap<Int, Float> = HashMap()
    private val MIN_CPU_FREQUENCY: MutableMap<Int, Float> = HashMap()

    private var totalStorageSize: Long = 0
    private var totalRamSize: Long = 0
    private var maxRamAllocatedSize: Long = 0
    private var coreCount = 0

    /**
     * Get general free storage space of device using [StatFs].
     *
     * @return free storage space in bytes.
     */
    override fun getStorageFree(): Long {
        try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            return stat.availableBlocks.toLong() * stat.blockSize.toLong()
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return 0
    }

    override fun getStorageUsed(): Long {
        try {
            return getStorageSize() - getStorageFree()
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return 0
    }

    /**
     * Get total ram size using [ActivityManager.MemoryInfo].
     *
     * @return total ram size in bytes.
     */
    override fun getRamSize(): Long {
        try {
            if (totalRamSize == 0L) {
                totalRamSize = getMemoryInfo(context).totalMem
            }
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return totalRamSize
    }

    /**
     * Get free ram size using [android.os.Debug.MemoryInfo].
     *
     * @return used ram value in bytes.
     */
    override fun getRamUsed(): Long {
        try {
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            return memInfo.totalPss * 1024L
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return 0
    }

    /**
     * Get cpu usage
     *
     * @return cpu usage in the range from 0 to 1.
     */
    override fun getCpuUsage(): Float {
        try {
            val coreCount = getNumCores()
            var freqSum = 0f
            var minFreqSum = 0f
            var maxFreqSum = 0f
            for (i in 0 until coreCount) {
                freqSum += getCurCpuFreq(i)
                minFreqSum += getMinCpuFreq(i)
                maxFreqSum += getMaxCpuFreq(i)
            }
            return getAverageClock(freqSum, minFreqSum, maxFreqSum)
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return 0f
    }

    override fun getId(): String {
        TODO("Not yet implemented")
    }

    override fun getLaunchTs(): Long {
        TODO("Not yet implemented")
    }

    override fun getLaunchMonotonicTs(): Long {
        TODO("Not yet implemented")
    }

    override fun getStartTs(): Long {
        TODO("Not yet implemented")
    }

    override fun getMonotonicStartTs(): Long {
        TODO("Not yet implemented")
    }

    override fun getTs(): Long {
        return System.currentTimeMillis()
    }

    override fun getMonotonicTs(): Long {
        TODO("Not yet implemented")
    }

    override fun getMemoryWarningsTs(): List<Long> {
        TODO("Not yet implemented")
    }

    override fun getMemoryWarningsMonotonicTs(): List<Long> {
        TODO("Not yet implemented")
    }

    override fun getBattery(): Float {
        try {
            val batteryStatus: Intent? = getBatteryIntent(context)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                return if (level == -1 || scale == -1) {
                    -1f
                } else {
                    val percentMultiplier = 100.0f
                    level.toFloat() / scale.toFloat() * percentMultiplier
                }
            }
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return -1f
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
     * Get max ram memory, which app can used using [ActivityManager]
     *
     * @return free ram size in bytes.
     */
    private fun getMaxRamAllocatedSize(context: Context): Long {
        try {
            if (maxRamAllocatedSize == 0L) {
                maxRamAllocatedSize =
                    getActivityManager(context)
                    .largeMemoryClass * 1024L * 1024L
            }
        } catch (throwable: Throwable) {
            logError(message = throwable.message ?: "", error = throwable)
        }
        return maxRamAllocatedSize
    }

    /**
     * Get [ActivityManager] from [WeakReference] or create new.
     *
     * @return [ActivityManager].
     */
    private fun getActivityManager(context: Context): ActivityManager {
        val activityManager: ActivityManager
        if (weakActivityManager?.get() != null) {
            activityManager = requireNotNull(weakActivityManager?.get())
        } else {
            activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            weakActivityManager = WeakReference(activityManager)
        }
        return activityManager
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
        if (maxFreqSum - minFreqSum <= 0) {
            return 0f
        }
        return if (maxFreqSum >= 0) {
            (currentFreqSum - minFreqSum) / (maxFreqSum - minFreqSum)
        } else {
            0f
        }
    }

    /**
     * Get number of cores using contents of the system folder /sys/devices/system/cpu/
     *
     * @return core count of CPU.
     */
    private fun getNumCores(): Int {
        if (coreCount == 0) {
            try {
                val dir = File("/sys/devices/system/cpu/")
                val files = dir.listFiles { pathname ->
                    Pattern.matches(
                        "cpu[0-9]",
                        pathname.name
                    )
                }
                if (files != null) {
                    coreCount = files.size
                } else {
                    coreCount = Runtime.getRuntime().availableProcessors()
                }
            } catch (throwable: Throwable) {
                coreCount = Runtime.getRuntime().availableProcessors()
                logError(message = throwable.message ?: "", error = throwable)
            }
        }
        return coreCount
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
        return if (MAX_CPU_FREQUENCY.containsKey(coreNum)) {
            requireNotNull(MAX_CPU_FREQUENCY.get(coreNum))
        } else {
            val path =
                String.format("/sys/devices/system/cpu/cpu%s/cpufreq/cpuinfo_max_freq", coreNum)
            val result = readIntegerFile(path)
            if (result > 0) {
                MAX_CPU_FREQUENCY[coreNum] = result
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
        return if (MIN_CPU_FREQUENCY.containsKey(coreNum)) {
            requireNotNull(MIN_CPU_FREQUENCY.get(coreNum))
        } else {
            val path =
                String.format("/sys/devices/system/cpu/cpu%s/cpufreq/cpuinfo_min_freq", coreNum)
            val result = readIntegerFile(path)
            if (result > 0) {
                MIN_CPU_FREQUENCY[coreNum] = result
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
        var closeable: Closeable? = null
        val line: String
        try {
            val fileInputStream = FileInputStream(filePath)
            closeable = fileInputStream
            val inputStreamReader = InputStreamReader(fileInputStream)
            closeable = inputStreamReader
            val bufferedReader = BufferedReader(inputStreamReader, 1024)
            closeable = bufferedReader
            line = bufferedReader.readLine()
            if (!TextUtils.isEmpty(line)) {
                return line.toFloat()
            }
        } catch (e: Throwable) {
            // ignore
        } finally {
            close(closeable)
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