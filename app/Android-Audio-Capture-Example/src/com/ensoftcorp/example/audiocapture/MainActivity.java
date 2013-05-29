package com.ensoftcorp.example.audiocapture;

import com.example.audiocapture.R;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;

/**
 * An example Android application which uses the MediaRecorder APIs to record audio.
 * MediaRecorder has a 9-step API for audio recording. For more information, see:
 * 
 * http://developer.android.com/guide/topics/media/audio-capture.html
 * 
 * To use the Demonstration Toolbox with this application,
 * 
 * 1. Import the Demonstration Toolbox project into your workspace
 * 2. Open an Atlas Interpreter view
 * 3. Index the application.
 * 4. Run 'androidAudioCapture.show'
 * 
 * See Demonstration-Toolbox/com.ensoftcorp.atlas.java.demo.apicompliance.APICompliance
 * 
 * @author Tom Deering
 *
 */
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
			mr.prepare();
		} catch (Exception e) {}
		mr.start();
		mr.stop();
		mr.release();
	}
}