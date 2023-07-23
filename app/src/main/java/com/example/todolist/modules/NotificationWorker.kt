package com.example.todolist.modules

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.todolist.R

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Retrieve the task details from input data
        val taskId = inputData.getLong("taskId", -1)
        val taskName = inputData.getString("taskName")
        val expirationTime = inputData.getString("expirationTime")

        // Check if the app has the required permission to post notifications
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, handle the situation (e.g., show a message or request permission)
            // You can implement your own logic here based on your app's requirements
            // For example, you can show a message to the user or request the permission
            // using a dialog or an in-app prompt.
            // Remember to handle the permission request response as well.
            return Result.failure()
        }

        // Create the notification channel if it doesn't exist
        createNotificationChannel()

        // Create the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Task Expired")
            .setContentText("Task '$taskName' has expired!")
            .setSmallIcon(R.drawable.bar_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Show the notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(taskId.toInt(), notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for task notifications"

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "task_notification_channel"
    }
}