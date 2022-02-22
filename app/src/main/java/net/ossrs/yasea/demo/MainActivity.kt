package net.ossrs.yasea.demo

import android.Manifest
import android.support.v7.app.AppCompatActivity
import com.github.faucamp.simplertmp.RtmpHandler.RtmpListener
import net.ossrs.yasea.SrsRecordHandler.SrsRecordListener
import net.ossrs.yasea.SrsEncodeHandler.SrsEncodeListener
import android.content.SharedPreferences
import android.os.Environment
import net.ossrs.yasea.SrsPublisher
import net.ossrs.yasea.SrsCameraView
import android.os.Bundle
import android.view.WindowManager
import net.ossrs.yasea.demo.R
import android.content.pm.ActivityInfo
import android.os.Build
import android.support.v4.content.ContextCompat
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Camera
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import net.ossrs.yasea.demo.MainActivity
import android.widget.EditText
import net.ossrs.yasea.SrsEncodeHandler
import com.github.faucamp.simplertmp.RtmpHandler
import net.ossrs.yasea.SrsRecordHandler
import net.ossrs.yasea.SrsCameraView.CameraCallbacksHandler
import com.seu.magicfilter.utils.MagicFilterType
import android.widget.Toast
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.net.SocketException
import java.util.*

class MainActivity : AppCompatActivity(), RtmpListener, SrsRecordListener, SrsEncodeListener {
    private var btnPublish: Button? = null

    /*    private Button btnSwitchCamera;
    private Button btnRecord;
    private Button btnSwitchEncoder;
    private Button btnPause;*/
    private var sp: SharedPreferences? = null

    //    private String rtmpUrl = "rtmp://ossrs.net/" + getRandomAlphaString(3) + '/' + getRandomAlphaDigitString(5);
    private var rtmpUrl = "rtmp://192.168.50.12:10935/live/stream_4"
    private val recPath = Environment.getExternalStorageDirectory().path + "/test.mp4"
    private var mPublisher: SrsPublisher? = null
    private var mCameraView: SrsCameraView? = null
    private val mWidth = 640
    private val mHeight = 480
    private var isPermissionGranted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        // response screen rotation event
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        requestPermission()
    }

    private fun requestPermission() {
        //1. 检查是否已经有该权限
        if (Build.VERSION.SDK_INT >= 23 && ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED))) {
            //2. 权限没有开启，请求权限
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), RC_CAMERA)
        } else {
            //权限已经开启，做相应事情
            isPermissionGranted = true
            init()
        }
    }

    //3. 接收申请成功或者失败回调
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_CAMERA) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限被用户同意,做相应的事情
                isPermissionGranted = true
                init()
            } else {
                //权限被用户拒绝，做相应的事情
                finish()
            }
        }
    }

    private fun init() {
        // restore data.
        sp = getSharedPreferences("Yasea", MODE_PRIVATE)
        rtmpUrl = sp!!.getString("rtmpUrl", rtmpUrl)

        // initialize url.
        val efu = findViewById<View>(R.id.url) as EditText
        efu.setText(rtmpUrl)
        btnPublish = findViewById<View>(R.id.publish) as Button
        /*  btnSwitchCamera = (Button) findViewById(R.id.swCam);
        btnRecord = (Button) findViewById(R.id.record);
        btnSwitchEncoder = (Button) findViewById(R.id.swEnc);
        btnPause = (Button) findViewById(R.id.pause);

        btnPause.setEnabled(false);*/mCameraView = findViewById<View>(R.id.glsurfaceview_camera) as SrsCameraView
        mPublisher = SrsPublisher(mCameraView)
        mPublisher!!.setEncodeHandler(SrsEncodeHandler(this))
        mPublisher!!.setRtmpHandler(RtmpHandler(this))
        mPublisher!!.setRecordHandler(SrsRecordHandler(this))
        mPublisher!!.setPreviewResolution(mWidth, mHeight)
        mPublisher!!.setOutputResolution(mHeight, mWidth) // 这里要和preview反过来
        mPublisher!!.setVideoHDMode()
        mPublisher!!.startCamera()
        mCameraView!!.setCameraCallbacksHandler(object : CameraCallbacksHandler() {
            override fun onCameraParameters(params: Camera.Parameters) {
                //params.setFocusMode("custom-focus");
                //params.setWhiteBalance("custom-balance");
                //etc...
            }
        })
        btnPublish!!.setOnClickListener {
            if (btnPublish!!.text.toString().contentEquals("publish")) {
                rtmpUrl = efu.text.toString()
                val editor = sp!!.edit()
                editor.putString("rtmpUrl", rtmpUrl)
                editor.apply()
                mPublisher!!.startPublish(rtmpUrl)
                mPublisher!!.startCamera()

                /*   if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                        Toast.makeText(getApplicationContext(), "Use hard encoder", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Use soft encoder", Toast.LENGTH_SHORT).show();
                    }*/btnPublish!!.text = "stop"
                /*         btnSwitchEncoder.setEnabled(false);
                    btnPause.setEnabled(true);*/
            } else if (btnPublish!!.text.toString().contentEquals("stop")) {
                mPublisher!!.stopPublish()
                mPublisher!!.stopRecord()
                btnPublish!!.text = "publish"
                /*btnRecord.setText("record");
                    btnSwitchEncoder.setEnabled(true);
                    btnPause.setEnabled(false);*/
            }
        }
        /*
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btnPause.getText().toString().equals("Pause")){
                    mPublisher.pausePublish();
                    btnPause.setText("resume");
                }else{
                    mPublisher.resumePublish();
                    btnPause.setText("Pause");
                }
            }
        });

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPublisher.switchCameraFace((mPublisher.getCameraId() + 1) % Camera.getNumberOfCameras());
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnRecord.getText().toString().contentEquals("record")) {
                    if (mPublisher.startRecord(recPath)) {
                        btnRecord.setText("pause");
                    }
                } else if (btnRecord.getText().toString().contentEquals("pause")) {
                    mPublisher.pauseRecord();
                    btnRecord.setText("resume");
                } else if (btnRecord.getText().toString().contentEquals("resume")) {
                    mPublisher.resumeRecord();
                    btnRecord.setText("pause");
                }
            }
        });

        btnSwitchEncoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                    mPublisher.switchToSoftEncoder();
                    btnSwitchEncoder.setText("hard encoder");
                } else if (btnSwitchEncoder.getText().toString().contentEquals("hard encoder")) {
                    mPublisher.switchToHardEncoder();
                    btnSwitchEncoder.setText("soft encoder");
                }
            }
        });
*/
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_settings) {
            return true
        } else {
            when (id) {
                R.id.cool_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.COOL)
                R.id.beauty_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.BEAUTY)
                R.id.early_bird_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.EARLYBIRD)
                R.id.evergreen_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.EVERGREEN)
                R.id.n1977_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.N1977)
                R.id.nostalgia_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.NOSTALGIA)
                R.id.romance_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.ROMANCE)
                R.id.sunrise_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.SUNRISE)
                R.id.sunset_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.SUNSET)
                R.id.tender_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.TENDER)
                R.id.toast_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.TOASTER2)
                R.id.valencia_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.VALENCIA)
                R.id.walden_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.WALDEN)
                R.id.warm_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.WARM)
                R.id.original_filter -> mPublisher!!.switchCameraFilter(MagicFilterType.NONE)
                else -> mPublisher!!.switchCameraFilter(MagicFilterType.NONE)
            }
        }
        title = item.title
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (mPublisher!!.camera == null && isPermissionGranted) {
            //if the camera was busy and available again
            mPublisher!!.startCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        val btn = findViewById<View>(R.id.publish) as Button
        btn.isEnabled = true
        mPublisher!!.resumeRecord()
    }

    override fun onPause() {
        super.onPause()
        mPublisher!!.pauseRecord()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPublisher!!.stopPublish()
        mPublisher!!.stopRecord()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mPublisher!!.stopEncode()
        mPublisher!!.stopRecord()
        //        btnRecord.setText("record");
        mPublisher!!.setScreenOrientation(newConfig.orientation)
        if (btnPublish!!.text.toString().contentEquals("stop")) {
            mPublisher!!.startEncode()
        }
        mPublisher!!.startCamera()
    }

    private fun handleException(e: Exception) {
        try {
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            mPublisher!!.stopPublish()
            mPublisher!!.stopRecord()
            btnPublish!!.text = "publish"
            //            btnRecord.setText("record");
//            btnSwitchEncoder.setEnabled(true);
        } catch (e1: Exception) {
            //
        }
    }

    // Implementation of SrsRtmpListener.
    override fun onRtmpConnecting(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpConnected(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpVideoStreaming() {}
    override fun onRtmpAudioStreaming() {}
    override fun onRtmpStopped() {
        Toast.makeText(applicationContext, "Stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpDisconnected() {
        Toast.makeText(applicationContext, "Disconnected", Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpVideoFpsChanged(fps: Double) {
        Log.i(TAG, String.format("Output Fps: %f", fps))
    }

    override fun onRtmpVideoBitrateChanged(bitrate: Double) {
        val rate = bitrate.toInt()
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Video bitrate: %f kbps", bitrate / 1000))
        } else {
            Log.i(TAG, String.format("Video bitrate: %d bps", rate))
        }
    }

    override fun onRtmpAudioBitrateChanged(bitrate: Double) {
        val rate = bitrate.toInt()
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000))
        } else {
            Log.i(TAG, String.format("Audio bitrate: %d bps", rate))
        }
    }

    override fun onRtmpSocketException(e: SocketException) {
        handleException(e)
    }

    override fun onRtmpIOException(e: IOException) {
        handleException(e)
    }

    override fun onRtmpIllegalArgumentException(e: IllegalArgumentException) {
        handleException(e)
    }

    override fun onRtmpIllegalStateException(e: IllegalStateException) {
        handleException(e)
    }

    // Implementation of SrsRecordHandler.
    override fun onRecordPause() {
        Toast.makeText(applicationContext, "Record paused", Toast.LENGTH_SHORT).show()
    }

    override fun onRecordResume() {
        Toast.makeText(applicationContext, "Record resumed", Toast.LENGTH_SHORT).show()
    }

    override fun onRecordStarted(msg: String) {
        Toast.makeText(applicationContext, "Recording file: $msg", Toast.LENGTH_SHORT).show()
    }

    override fun onRecordFinished(msg: String) {
        Toast.makeText(applicationContext, "MP4 file saved: $msg", Toast.LENGTH_SHORT).show()
    }

    override fun onRecordIOException(e: IOException) {
        handleException(e)
    }

    override fun onRecordIllegalArgumentException(e: IllegalArgumentException) {
        handleException(e)
    }

    // Implementation of SrsEncodeHandler.
    override fun onNetworkWeak() {
        Toast.makeText(applicationContext, "Network weak", Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkResume() {
        Toast.makeText(applicationContext, "Network resume", Toast.LENGTH_SHORT).show()
    }

    override fun onEncodeIllegalArgumentException(e: IllegalArgumentException) {
        handleException(e)
    }

    companion object {
        private const val TAG = "Yasea"
        const val RC_CAMERA = 100
        private fun getRandomAlphaString(length: Int): String {
            val base = "abcdefghijklmnopqrstuvwxyz"
            val random = Random()
            val sb = StringBuilder()
            for (i in 0 until length) {
                val number = random.nextInt(base.length)
                sb.append(base[number])
            }
            return sb.toString()
        }

        private fun getRandomAlphaDigitString(length: Int): String {
            val base = "abcdefghijklmnopqrstuvwxyz0123456789"
            val random = Random()
            val sb = StringBuilder()
            for (i in 0 until length) {
                val number = random.nextInt(base.length)
                sb.append(base[number])
            }
            return sb.toString()
        }
    }
}