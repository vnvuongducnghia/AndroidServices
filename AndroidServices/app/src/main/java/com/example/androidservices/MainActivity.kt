package com.example.androidservices

import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.androidservices.LocalLinks.imagesPath
import com.example.androidservices.LocalLinks.mediaFolder
import com.example.androidservices.LocalLinks.videosType
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import android.R.attr.name
import android.app.NotificationManager

import android.app.NotificationChannel

import android.os.Build

import android.app.Notification

import android.app.PendingIntent

import android.content.Intent

import android.R.attr.name








class MainActivity : AppCompatActivity() {

    private var pref: SharedPreferences? = null
    private var isVisibleActivity = false
//    private val linkDownload =
//        "https://jsoncompare.org/LearningContainer/SampleFiles/Video/MP4/Sample-Video-File-For-Testing.mp4"
//    private val linkDownload =
//        "https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_480_1_5MG.mp4"
    private val linkDownload =
        "https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_1920_18MG.mp4"

    companion object {
        private const val TAG = "MainActivity"
        private const val TRACKING_STATUS_DELAY = 200L
    }

    private var mRequestDownload: DownloadManager.Request? = null
    private var mDownloadId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
//     registerReceiverDownload()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initPermission()
        initDownloadManager()
        mRequestDownload = requestDownload(Uri.parse(linkDownload), videosType)
        btnDownload.setOnClickListener {
            startDownload("VOD_100", mRequestDownload!!)
        }



        btnReadListRequest.setOnClickListener {

        }
    }


    override fun onStart() {
        super.onStart()
        isVisibleActivity = true

        readListRequestDownload()
        startDownloadStatusTracking()
        Log.d(TAG, "registerReceiverDownload: ")



    }

    override fun onStop() {
        super.onStop()
        isVisibleActivity = false

        writeListRequestDownload()
        Log.d(TAG, "unRegisterReceiverDownload: ")
    }

    override fun onDestroy() {
        super.onDestroy()
//          unRegisterReceiverDownload()
    }

    fun requestDownload(
        uri: Uri, fileType: String, title: String? = null, description: String? = null,
    ): DownloadManager.Request {
        val name = "${mediaFolder}/${LocalLinks.time2Name()}${fileType}"
        val request = DownloadManager.Request(uri)
        title?.let { request.setTitle(it) }
        description?.let { request.setDescription(it) }
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, name)
        return request
    }

    var mToast: Toast? = null
    fun toast(message: String) {
        mToast?.cancel()
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        mToast?.show()
    }

    var mPermissionHelper: PermissionHelper? = null
    private fun initPermission() {
        if (mPermissionHelper == null) {
            mPermissionHelper = PermissionHelper(this)
            mPermissionHelper?.apply {
                setOnPermissionListener(object : PermissionHelper.PermissionListener {
                    override fun onGranted(currentType: PermissionHelper.PermissionType?) {
                        isAllowPermissions = true
                        initFolder()
                    }

                    override fun onDenied() {
                        isAllowPermissions = false
                    }

                    override fun onCustomDialog() {
                        showSetting()
                    }
                })
            }
        }
    }

    @Volatile
    private var downloadManager: DownloadManager? = null
    private var mHandler: Handler? = null
    /*private var downloadBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Fetching the download id received with the broadcast
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            //Checking if the received broadcast is for our enqueued download by matching download id

            // Chạy vòng for ở đây để check downloadID và cập nhật progress 100.
            Log.d(TAG, "onReceive: downloadId [$downloadId]")

            if (mListRequestDownload.isNotEmpty()) {
                mListRequestDownload.forEach { e ->
                    if (e.downloadId == downloadId) {
                        runOnUiThread { toast(getStatusMessage(downloadId)) }
                    }
                }
                mListRequestDownload.removeIf { e -> e.downloadId == downloadId }
                Log.d(TAG, "onReceive: da xoa downloadId = $downloadId")
            }
            // downloadMessage()
        }
    }*/
    private var downloadBroadcastReceiver = DownloadBroadcastReceiver()

    fun registerReceiverDownload() {
        registerReceiver(
            downloadBroadcastReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    fun unRegisterReceiverDownload() {
        unregisterReceiver(downloadBroadcastReceiver)
    }

    fun initDownloadManager() {
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    private var mListRequestDownload = ArrayList<RequestDownload>()
    fun startDownload(keyRequest: String, request: DownloadManager.Request) {
        // Check keyRequest
        mListRequestDownload.forEach {
            if (it.keyRequest == keyRequest) {
                return
            }
        }
        // Enqueue download and save into referenceId
        mDownloadId = downloadManager?.enqueue(request) ?: -1
        mListRequestDownload.add(RequestDownload(keyRequest, mDownloadId!!, 0))
        startDownloadStatusTracking()
    }

    fun stopDownload(keyRequest: String) {
        mListRequestDownload.forEach {
            if (it.keyRequest == keyRequest) {
                downloadManager?.remove(it.downloadId)
            }
        }
    }

    private fun startDownloadStatusTracking() {
        if (mListRequestDownload.isEmpty()) return
        if (mHandler == null) mHandler = Handler(Looper.getMainLooper())
        mHandler?.let { _handler ->
            val runnable: Runnable = object : Runnable {
                override fun run() {
                    if (mListRequestDownload.isNotEmpty()) {
                        mListRequestDownload.forEach {
                            updateProgressDownload(it)
                        }
                        _handler.postDelayed(this, TRACKING_STATUS_DELAY)
                    } else {
                        // Vao la null ngay nen, truoc khi vao phai doc file ngay va cap nhat cho listRequestDownload
                        mHandler = null
                    }
                }
            }
            _handler.post(runnable)
        }
    }

    private fun readListRequestDownload() {
        if (pref == null) pref = getSharedPreferences("PREF2", MODE_PRIVATE)
        val jsonString = pref!!.getString(writeListRequestDownloadKey, null)
        if (jsonString != null) {
            val myObj = json2Object(jsonString)
            if (myObj!!.listRequestDownload!!.isNotEmpty()) {
                mListRequestDownload.addAll(myObj.listRequestDownload!!)
            }
        }

     val abc =   pref!!.getString("nghiadeptrai", null)
        Log.d(TAG, "readListRequestDownload: $abc ")
    }

    private val writeListRequestDownloadKey = "writeListRequestDownloadKey"
    private fun writeListRequestDownload() {
        if (pref == null) pref = getSharedPreferences("PREF2", MODE_PRIVATE)
        val jsonString = object2Json(MyRequestDownload().apply {
            this.listRequestDownload = mListRequestDownload
        })
        pref!!.edit().putString(writeListRequestDownloadKey, jsonString).apply()
    }

    private fun updateProgressDownload(request: RequestDownload) {
        val query = DownloadManager.Query()
        query.setFilterById(request.downloadId)
        val cursor = downloadManager?.query(query)
        if (cursor?.moveToFirst() == true) {
            var progress = 0
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_PENDING -> {
                    Log.d(TAG, "updateProgressDownload: STATUS_PENDING")
                }
                DownloadManager.STATUS_RUNNING -> {
                    val total =
                        cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (total >= 0) {
                        val downloaded =
                            cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        progress = (downloaded * 100L / total).toInt()
                        Log.d(TAG, "updateProgressDownload: STATUS_RUNNING... $progress")
                    }
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    progress = 100
                    Log.d(TAG, "updateProgressDownload: STATUS_SUCCESSFUL... $progress")
                    mHandler = null
                }
            }

            // update progress in list request download
            if (progress > 0 && request.progress < progress) {
                request.progress = progress
                pbDownload.setProgress(progress, true)
            }

            cursor.close()
        }
    }

    private fun getStatusMessage(downloadId: Long): String {

        val query = DownloadManager.Query()
        // set the query filter to our previously Enqueued download
        query.setFilterById(downloadId)

        // Query the download manager about downloads that have been requested.
        val cursor = downloadManager?.query(query)
        if (cursor?.moveToFirst() == true) {
            return downloadStatus(cursor)
        }
        return "NO_STATUS_INFO"
    }

    private fun downloadStatus(cursor: Cursor): String {

        // column for download  status
        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val status = cursor.getInt(columnIndex)
        // column for reason code if the download failed or paused
        val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
        val reason = cursor.getInt(columnReason)

        var statusText = ""
        var reasonText = ""

        when (status) {
            DownloadManager.STATUS_FAILED -> {
                statusText = "STATUS_FAILED"
                reasonText = when (reason) {
                    DownloadManager.ERROR_CANNOT_RESUME -> "ERROR_CANNOT_RESUME"
                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> "ERROR_DEVICE_NOT_FOUND"
                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "ERROR_FILE_ALREADY_EXISTS"
                    DownloadManager.ERROR_FILE_ERROR -> "ERROR_FILE_ERROR"
                    DownloadManager.ERROR_HTTP_DATA_ERROR -> "ERROR_HTTP_DATA_ERROR"
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "ERROR_INSUFFICIENT_SPACE"
                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "ERROR_TOO_MANY_REDIRECTS"
                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "ERROR_UNHANDLED_HTTP_CODE"
                    DownloadManager.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
                    else -> ""
                }
            }
            DownloadManager.STATUS_PAUSED -> {
                statusText = "STATUS_PAUSED"
                reasonText = when (reason) {
                    DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "PAUSED_QUEUED_FOR_WIFI"
                    DownloadManager.PAUSED_UNKNOWN -> "PAUSED_UNKNOWN"
                    DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "PAUSED_WAITING_FOR_NETWORK"
                    DownloadManager.PAUSED_WAITING_TO_RETRY -> "PAUSED_WAITING_TO_RETRY"
                    else -> ""
                }
            }
            DownloadManager.STATUS_PENDING -> statusText = "STATUS_PENDING"
            DownloadManager.STATUS_RUNNING -> statusText = "STATUS_RUNNING"
            DownloadManager.STATUS_SUCCESSFUL -> statusText = "STATUS_SUCCESSFUL"
        }

        return "Status: $statusText, $reasonText"
    }

    private fun initFolder() {
        val imagesFolder = File(imagesPath)
        if (!imagesFolder.mkdirs()) {
            imagesFolder.mkdirs()
        }
    }

    private fun downloadMessage() {
        if (isVisibleActivity) {
            toast("Download Thanh Cong")
        }
    }

    private fun object2Json(myObj: MyRequestDownload): String? {
        val gson = Gson()
        return gson.toJson(myObj)
    }

    private fun json2Object(json: String): MyRequestDownload? {
        val gson = Gson()
        return gson.fromJson(json, MyRequestDownload::class.java)
    }

    var fileName = "myBlog.json"

    fun saveData(context: Context, mJsonResponse: String?) {
        try {
            val file = FileWriter(context.filesDir.path + "/" + fileName)
            file.write(mJsonResponse)
            file.flush()
            file.close()
        } catch (e: IOException) {
            Log.e("TAG", "Error in Writing: " + e.localizedMessage)
        }
    }

    fun getData(context: Context): String? {
        return try {
            val f = File(context.filesDir.path + "/" + fileName)
            //check whether file exists
            val fis = FileInputStream(f)
            val size = fis.available()
            val buffer = ByteArray(size)
            fis.read(buffer)
            fis.close()
            String(buffer)
        } catch (e: IOException) {
            Log.e("TAG", "Error in Reading: " + e.localizedMessage)
            null
        }
    }
}

class MyRequestDownload() {
    var listRequestDownload: List<RequestDownload>? = null
}

class RequestDownload(
    var keyRequest: String,
    var downloadId: Long,
    var progress: Int,
    var status: Int = 0
)