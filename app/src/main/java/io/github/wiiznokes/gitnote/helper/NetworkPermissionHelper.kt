package io.github.wiiznokes.gitnote.helper

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.PermissionChecker
import java.net.InetAddress
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkPermissionHelper {
    companion object {
        fun isPermissionGranted(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < 37) return true
            return PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) == PermissionChecker.PERMISSION_GRANTED
        }

        suspend fun requiresLocalNetworkPermission(urlString: String): Boolean = withContext(Dispatchers.IO) {
            try {
                val host = if (urlString.contains("://")) {
                    URI(urlString).host
                } else {
                    val afterAt = urlString.substringAfter('@', urlString)
                    afterAt.substringBefore(':')
                }

                if (host == null) return@withContext false

                if (host.endsWith(".local", ignoreCase = true)) {
                    return@withContext true
                }

                val addresses = InetAddress.getAllByName(host)

                addresses.any { address ->
                    address.isSiteLocalAddress ||
                    address.isLinkLocalAddress
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
