package com.example.androidservices

import android.os.Environment
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by nghia.vuong on 24,May,2021
 */
object LocalLinks {

    var mediaFolder = "NghiaDepTraiMedia"

    var videosType = ".mp4"
    var videosMineType = "video/mp4"

    var imagesType = ".png"
    var imagesMineType = "image/png"
    var imagesPathQ = "Pictures/$mediaFolder"

    var appName = "SECOM Sights"

    @Suppress("DEPRECATION")
    var imagesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        .toString() + "/$mediaFolder"

    fun time2Name(): String {
        return SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.JAPAN).format(Date()).toString()
    }

    fun imageSnapshotName(cameraName: String): String {
        return "$appName-$cameraName-${
            SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.JAPAN).format(
                Date()
            )
        }"
    }

}