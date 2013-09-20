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
 * 2. Index the application (main menu, Atlas->Index Workspace)
 * 3. Right click on the Demonstration Toolbox project and select Atlas->Open Atlas Smart View
 * 4. In the Atlas Smart View toolbar, click the down arrow and select 
 *    Script->Media Recorder API Compliance
 * 5. The API Compliance script checks for the expected ordering of calls. 
 *    It builds a control flow graph of our application, 
 *    and identifies where the API call events occur. 
 *    Finally, each call event is sorted into a “good” or “bad” group based upon whether 
 *    all prerequisite calls were made prior to making this call. 
 *    
 *    In the resulting graph, correct API calls are shown in blue, while incorrect calls 
 *    are shown in red.
 * 6. Click on the method initWrong().  In initWrong(), the call to prepare() is missing.
 * 7. To see what it looks like after adding the call to prepare(), click initCorrect(). 
 *    
 *    
 * The API compliance script can be found at:
 *   Demonstration-Toolbox/com.ensoftcorp.atlas.java.demo.apicompliance.APICompliance
 *   
 * The custom Atlas Smart View:
 *   Demonstration-Toolbox/com.ensoftcorp.atlas.java.demo.apicompliance.selection.MediaRecorderAPICompliance
 *   
 * 
 * @author Tom Deering
 *
 */
public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initWrong();
		initCorrect();
	}
	
	private void initWrong() {
		MediaRecorder mr = new MediaRecorder();
		
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
		mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mr.setOutputFile("some/file");
		mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		try {
			//mr.prepare();
		} catch (Exception e) {}
		mr.start();
		mr.stop();
		mr.release();
	}
	
	private void initCorrect() {
		MediaRecorder mr = new MediaRecorder();
		
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
		mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
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