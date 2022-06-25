package com.example.screenrecorder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private var screenDensity:Int = 0
    private var projectionManager:MediaProjectionManager?=null
    private var mediaProjection:MediaProjection?=null
    private var virtualDisplay:VirtualDisplay?=null
    private var mediaProjectionCallback:MediaprojectionCallback?=null
    private var mediaRecorder:MediaRecorder?=null

    private var videoUri:String=""

    companion object{
        private const val REQUEST_CODE = 100
        private const val  REQUEST_PERMISSIONC= 100
        private var DISPLAY_WIDTH = 700
        private var DISPLAY_HEIGHT = 1260
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0,90)
            ORIENTATIONS.append(Surface.ROTATION_90,0)
            ORIENTATIONS.append(Surface.ROTATION_180,270)
            ORIENTATIONS.append(Surface.ROTATION_270,180)



        }
    }

   inner class MediaprojectionCallback: MediaProjection.Callback(){
       //ctrl+o
        override fun onStop() {
            if (toggleButton.isChecked)
            {
                toggleButton.isChecked=false
                mediaRecorder!!.stop()
                mediaRecorder!!.reset()
            }
            mediaProjection = null
            stopscreenRecord()
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val metrics =DisplayMetrics()
        getMetrics()
        screenDensity=metrics.densityDpi

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        DISPLAY_HEIGHT = metrics.heightPixels
        DISPLAY_WIDTH = metrics.widthPixels
        //Event
        toggleButton.setOnClickListener{

            if ((ContextCompat.checkSelfPermission(this@MainActivity,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) + ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.RECORD_AUDIO
                        )) != PackageManager.PERMISSION_GRANTED
            )
            {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,android.Manifest.permission.RECORD_AUDIO))
                {
                   toggleButton.isChecked = false
                    Snackbar.make(rootLayout,"permissions",Snackbar.LENGTH_INDEFINITE)
                        .setAction("ENABLE") {
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.RECORD_AUDIO
                                ), REQUEST_PERMISSIONC
                            )
                        }.show()
                }
                else ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.RECORD_AUDIO), REQUEST_PERMISSIONC)
            }
        else
            {
                startrecordind(videoview)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode)
        {
            REQUEST_PERMISSIONC->{
                if(grantResults.isNotEmpty() && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    startrecordind(toggleButton)
         else
                {
                    toggleButton.isChecked = false
                    Snackbar.make(rootLayout,"permissions",Snackbar.LENGTH_INDEFINITE)
                        .setAction("ENABLE") {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_SETTINGS
                            intent.addCategory(Intent.CATEGORY_DEFAULT)
                            intent.data = Uri.parse("package:$packageName")
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            startActivity(intent)
                        }.show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun startrecordind( v :View?) {
        if ((v as ToggleButton).isChecked)
        {
            initRecorder()
            sharescreen()
        }
        else
        {
            mediaRecorder!!.stop()
            mediaRecorder!!.reset()
            stopscreenRecord()

            //play video in video view
            videoview.visibility = View.VISIBLE
            videoview.setVideoURI(Uri.parse(videoUri))
            videoview.start()

        }

    }

    private fun sharescreen() {
        if (mediaProjection == null)
        {
            startActivityForResult(projectionManager!!.createScreenCaptureIntent() , REQUEST_CODE)
            return

        }
        virtualDisplay = createVirtualDisplay()
        mediaRecorder!!.start()
    }

    private fun createVirtualDisplay() : VirtualDisplay? {
        return mediaProjection!!.createVirtualDisplay("mainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT,screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface,null,null)
    }

    //
    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE)
        {
            return
        }
        if (resultCode != Activity.RESULT_OK)
        {
            Toast.makeText(this,"screen permission denied",Toast.LENGTH_LONG) .show()
            return
        }

        mediaProjectionCallback = MediaprojectionCallback()
        mediaProjection = projectionManager!!.getMediaProjection(resultCode, data!!)
        mediaProjection!!.registerCallback(mediaProjectionCallback,null)
        virtualDisplay = createVirtualDisplay()
        mediaRecorder!!.start()
    }

    @SuppressLint("SimpleDateFormat")
    private fun initRecorder() {
        try {
            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)

            videoUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + StringBuilder("/")
                .append("EDMT_Record_")
                .append(SimpleDateFormat("dd-MM-yyy-hh_mm_ss").format(Date()))
                .append(".mp4")
                .toString()

            mediaRecorder!!.setOutputFile(videoUri)
            mediaRecorder!!.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder!!.setVideoEncodingBitRate(512*1000)
            mediaRecorder!!.setVideoFrameRate(30)

            val rotation = windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS.get(rotation + 90)
            mediaRecorder!!.setOrientationHint(orientation)
            mediaRecorder!!.prepare()
            } catch (e :IOException)
        {
                e.printStackTrace()
        }
            }

    private fun stopscreenRecord() {
        if (virtualDisplay == null)
            return
        virtualDisplay!!.release()
            destoryMediaprojection()

    }

    private fun destoryMediaprojection() {
        if (mediaProjection != null)
        {
            mediaProjection!!.unregisterCallback(mediaProjectionCallback)
            mediaProjection!!.stop()
            mediaProjection = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destoryMediaprojection()
    }
}

private fun getMetrics() {
    TODO("Not yet implemented")
}

//private fun MediaProjection.Callback.oncreate(savedInstanceState: Bundle?) {

//}
//private fun MediaProjection?.createVirtualDisplay(
  //  s: String,
    //displayWidth: Int,
    //displayHeight: Int,
 //   screenDensity: Int,
   // virtualDisplayFlagAutoMirror: Int,
    //unit: Unit,
    //nothing: Nothing?,
    //nothing1: Nothing?
//) {

//}

//private fun Display.getMetrics() {
//}




