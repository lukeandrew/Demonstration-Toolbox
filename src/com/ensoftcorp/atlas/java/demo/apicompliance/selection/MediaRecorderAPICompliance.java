package com.ensoftcorp.atlas.java.demo.apicompliance.selection;
import static com.ensoftcorp.atlas.java.demo.apicompliance.APICompliance.androidAudioCapture;
import static com.ensoftcorp.atlas.java.core.script.Common.edges;
import static com.ensoftcorp.atlas.java.core.script.Common.methodSelect;

import com.ensoftcorp.atlas.java.core.query.Attr;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.StyledResult;
import com.ensoftcorp.atlas.java.demo.util.DisplayItem;
import com.ensoftcorp.atlas.java.ui.scripts.selections.AtlasSmartViewScript;

public class MediaRecorderAPICompliance implements AtlasSmartViewScript{

	@Override
	public String[] getSupportedNodeTags() {
		return new String[]{};
	}

	@Override
	public String[] getSupportedEdgeTags() {
		return null;
	}

	@Override
	public StyledResult selectionChanged(SelectionInput input) {
		Q selection = input.getInterpretedSelection();
		
		// Get ahold of the relevant API methods
	    Q mediaRecorder = methodSelect("android.media","MediaRecorder","MediaRecorder");
	    Q setAudioSource = methodSelect("android.media","MediaRecorder","setAudioSource");
	    Q setOutputFormat = methodSelect("android.media","MediaRecorder","setOutputFormat");
	    Q setOutputFile = methodSelect("android.media","MediaRecorder","setOutputFile");
	    Q setAudioEncoder = methodSelect("android.media","MediaRecorder","setAudioEncoder");
	    Q prepare = methodSelect("android.media","MediaRecorder","prepare");
	    Q start = methodSelect("android.media","MediaRecorder","start");
	    Q stop = methodSelect("android.media","MediaRecorder","stop");
	    Q release = methodSelect("android.media","MediaRecorder","release");
	    Q apis = mediaRecorder.union(setAudioSource, setOutputFormat, setOutputFile,
	            setAudioEncoder, prepare, start, stop, release); 
		
	    // Check the compliance of API calls made somewhere underneath the current selection
	    Q declaredUnderSelection = edges(Attr.Edge.DECLARES).forward(selection);
	    Q callsUnderSelection = edges(Attr.Edge.CALL).betweenStep(declaredUnderSelection, apis);
	    
		DisplayItem di = androidAudioCapture(callsUnderSelection);

		return new StyledResult(di.getQ(), di.getHighlighter());
	}

	@Override
	public String getTitle() {
		return "Media Recorder API Compliance";
	}
}