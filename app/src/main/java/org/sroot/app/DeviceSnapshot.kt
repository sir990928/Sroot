package org.sroot.app

import android.os.Build
import android.system.Os
import android.system.OsConstants

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val device: String,
    val kernelRelease: String,
    val buildId: String,
    val fingerprint: String,
    val androidRelease: String,
    val sdk: Int,
    val abi: String,
    val pageSize: Long,
) {
    val isKnownTarget: Boolean
        get() = model == "SM-S9380" &&
            fingerprint.startsWith("samsung/pa3qzhx/pa3q:15/AP3A.240905.015")

    companion object {
        fun current(): DeviceSnapshot {
            val uname = Os.uname()
            return DeviceSnapshot(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                device = Build.DEVICE,
                kernelRelease = uname.release,
                buildId = Build.DISPLAY,
                fingerprint = Build.FINGERPRINT,
                androidRelease = Build.VERSION.RELEASE,
                sdk = Build.VERSION.SDK_INT,
                abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                pageSize = Os.sysconf(OsConstants._SC_PAGESIZE),
            )
        }
    }
}
