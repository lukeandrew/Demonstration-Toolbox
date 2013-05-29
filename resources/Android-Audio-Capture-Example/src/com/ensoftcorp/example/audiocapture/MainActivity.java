package com.ensoftcorp.example.audiocapture;

import com.example.audiocapture.R;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		MediaRecorder mr = new MediaRecorder();
		
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
		mr.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
		mr.setOutputFile("some/file");
		mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		try {
			//mr.prepare();
		} catch (Exception e) {}
		mr.start();
		mr.stop();
		mr.release();
	}
}