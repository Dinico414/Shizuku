package moe.shizuku.manager.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher

object SettingsHelper {

    private const val TAG = "SettingsHelper"

    @JvmStatic
    fun putGlobalInt(context: Context, key: String, value: Int): Boolean =
        putSetting(context, "Settings.Global \"$key\"") {
            Settings.Global.putInt(it, key, value)
        }

    @JvmStatic
    fun putGlobalLong(context: Context, key: String, value: Long): Boolean =
        putSetting(context, "Settings.Global \"$key\"") {
            Settings.Global.putLong(it, key, value)
        }

    @JvmStatic
    fun putSecureString(context: Context, key: String, value: String): Boolean =
        putSetting(context, "Settings.Secure \"$key\"") {
            Settings.Secure.putString(it, key, value)
        }

    private inline fun putSetting(
        context: Context,
        key: String,
        write: (ContentResolver) -> Boolean
    ): Boolean = try {
        write(context.contentResolver)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to write $key", e)
        false
    }

    @JvmStatic
    fun ensureAdbEnabled(context: Context): AdbEnabledState {
        val writable = putGlobalInt(context, Settings.Global.ADB_ENABLED, 1)
        putGlobalLong(context, "adb_allowed_connection_time", 0L)

        if (!writable) {
            return AdbEnabledState.UNKNOWN
        }

        return if (Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) > 0) {
            AdbEnabledState.ENABLED
        } else {
            AdbEnabledState.DISABLED
        }
    }

    enum class AdbEnabledState {
        ENABLED,
        DISABLED,
        UNKNOWN,
    }

    fun launchOrHighlightWirelessDebugging(context: Context) {
        val adbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)
        if (adbEnabled > 0) {
            SettingsPage.Developer.WirelessDebugging.launch(context)
        } else SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context, launcher: ActivityResultLauncher<Intent>? = null) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            setData(Uri.parse("package:" + context.packageName))
        }
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            context.startActivity(intent)
        }
    }

}