package io.github.wiiznokes.gitnote.helper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri
import io.github.wiiznokes.gitnote.BuildConfig
import io.github.wiiznokes.gitnote.helper.StoragePermissionHelper.Companion.isPermissionGranted


class StoragePermissionHelper {
    companion object {
        fun isPermissionGranted() =
            Environment.isExternalStorageManager()
    }

    private val storagePermissionName = Manifest.permission.MANAGE_EXTERNAL_STORAGE


    fun permissionContract(): Pair<ActivityResultContract<String, Boolean>, String> {
        val contract = RequestManageStorageContract()
        return contract to storagePermissionName
    }
}

class RequestManageStorageContract(private val forceLaunch: Boolean = false) :
    ActivityResultContract<String, Boolean>() {
    override fun createIntent(context: Context, input: String): Intent {
        val uri = "package:${BuildConfig.APPLICATION_ID}".toUri()
        return Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            uri
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = isPermissionGranted()

    // determine if this is necessary to launch an activity. if not, the result will be automatically used
    override fun getSynchronousResult(
        context: Context,
        input: String
    ): SynchronousResult<Boolean>? =
        if (!forceLaunch && isPermissionGranted()) SynchronousResult(true) else null

}

