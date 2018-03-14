package com.vagabond.memory

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Button
import android.widget.Toast
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var recordBtn: Button
    private lateinit var listenBtn: Button
    private lateinit var deleteBtn: Button

    private var isRecording = false
    private var isPlaying = false

    private var mRecorder:MediaRecorder ?= null


    val MY_PERMISSIONS_REQUEST = 89
    val MEMORY_SP = "memory_sp"
    val LATEST_TRACK = "last_track"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recordBtn = findViewById(R.id.record)
        listenBtn = findViewById(R.id.listen)
        deleteBtn = findViewById(R.id.delete_all)


        checkForPermissionsAndStartApp()
    }

    private fun checkForPermissionsAndStartApp() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST)


        } else {
           startApp()
        }
    }

    private fun startApp() {
        setButtonListeners()
    }

    private fun setButtonListeners() {
        var color: Int = 0
        var txt = 0
        recordBtn.setOnClickListener({ view ->
            if (isPlaying) {
                Toast.makeText(this@MainActivity, R.string.wait_for_play_finish, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (isRecording) {
                color = ContextCompat.getColor(this, R.color.colorBTNBack)
                txt = R.string.start_record
                stopRecording()
            } else {
                color = ContextCompat.getColor(this, R.color.colorAccent)
                txt = R.string.stop_record
                startRecording()
            }
            isRecording = !isRecording
            view.setBackgroundColor(color)
            (view as Button).setText(txt)
        })

        listenBtn.setOnClickListener({view ->
            startPlaying()
        })

        deleteBtn.setOnClickListener({view ->

            deleteAllFiles()
        })
    }

    private fun deleteAllFiles() {
        val lastTrack = getLastTrack()
        if(lastTrack == -1){
            return
        }
        for (i in 0.. lastTrack){
            val file = File(getFileNameForIndex(i))
            file.delete()
        }
        saveLatestTrackNum(-1)
    }

    private val MAX_DURATION = 30000

    private fun startRecording() {
        mRecorder = MediaRecorder()
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        val nextTrack = getLastTrack() + 1
        mRecorder?.setOutputFile(getFileNameForIndex(nextTrack))
        saveLatestTrackNum(nextTrack)
        mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mRecorder?.setMaxDuration(MAX_DURATION)
        try {
            mRecorder?.prepare()
        } catch (e: IOException) {
            Log.e("media_record", "prepare() failed")
        }


        mRecorder?.start()
    }

    private fun stopRecording() {
        mRecorder?.stop();
        mRecorder?.release();
        mRecorder = null;
    }

    private fun startPlaying(){
        if(isPlaying){
            Toast.makeText(this@MainActivity, R.string.wait_for_play_finish, Toast.LENGTH_LONG).show()
            return
        }
        isPlaying = true
        val mediaPlayers = ArrayList<MediaPlayer>()
        val lastTrack = getLastTrack()
        if(lastTrack == -1){
            Toast.makeText(this, getString(R.string.nothing_to_play), Toast.LENGTH_LONG).show()
            isPlaying = false
            return
        }
        for(i in lastTrack downTo 0){
            mediaPlayers.add(createMediaPlayer(getFileNameForIndex(i)))
        }

        startPlaying(mediaPlayers)
    }

    private fun startPlaying(mediaPlayers: MutableList<MediaPlayer>) {
        val completionListener = object: MediaPlayer.OnCompletionListener {
            override fun onCompletion(mp: MediaPlayer?) {
                mp?.release()
                mediaPlayers.remove(mp)
                if (mediaPlayers.size > 0){
                    val nextMp = mediaPlayers.get(0)
                    nextMp.setOnCompletionListener(this)
                    nextMp.start()
                }
                else{
                    isPlaying = false
                }
            }

        }

        if(mediaPlayers.size > 0){
            val firstMp = mediaPlayers.get(0)
            firstMp.setOnCompletionListener(completionListener)
            firstMp.start()
        }
        else {
            isPlaying = false
        }

    }

    private fun createMediaPlayer(fileName:String):MediaPlayer{
       val mPlayer = MediaPlayer()
        try {
            mPlayer.setDataSource(fileName)
            mPlayer.prepare()
        } catch (e: IOException) {
            Log.e("media play", "prepare() failed")
        }
        return mPlayer
    }
    private fun saveLatestTrackNum(track:Int){
        getSharedPreferences(MEMORY_SP, Context.MODE_PRIVATE).edit().putInt(LATEST_TRACK, track).apply()
    }
    private fun getLastTrack():Int{
        return getSharedPreferences(MEMORY_SP, Context.MODE_PRIVATE).getInt(LATEST_TRACK, -1)
    }

    private fun getFileNameForIndex(index:Int):String{
        return filesDir.absolutePath + "/"+ index + ".mp4"
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        if (requestCode == MY_PERMISSIONS_REQUEST){
          if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
              startApp()
          }else{
              Toast.makeText(this, "App cannot function without permission", Toast.LENGTH_LONG).show()
          }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
