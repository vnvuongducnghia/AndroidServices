package com.example.androidservices

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat


/**
 * Created by ${nghia.vuong} on 18,November,2021
 */
class DownloadBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val pref = context!!.getSharedPreferences("PREF2", AppCompatActivity.MODE_PRIVATE)
            pref!!.edit().putString("nghiadeptrai", "nghia dep trai id = $downloadId").apply()

            //Show a notification
            val mBuilder = NotificationCompat.Builder(context!!.applicationContext, "notify_001")
            val ii = Intent(context.applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, ii, 0)

            val bigText = NotificationCompat.BigTextStyle()
            bigText.bigText("downloadId $downloadId")
            bigText.setBigContentTitle("Today's Bible Verse")
            bigText.setSummaryText("Text in detail")

            mBuilder.setContentIntent(pendingIntent)
            mBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
            mBuilder.setContentTitle("Your Title")
            mBuilder.setContentText("Your text")
            mBuilder.priority = Notification.PRIORITY_HIGH
            mBuilder.setStyle(bigText)

            val mNotificationManager: NotificationManager =
                context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager

            // Removed some obsoletes
            val channelId = "Your_channel_id"
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            mBuilder.setChannelId(channelId)

            mNotificationManager.notify(0, mBuilder.build())
        }
    }

}