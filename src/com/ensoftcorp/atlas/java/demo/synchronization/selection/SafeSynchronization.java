package com.ensoftcorp.atlas.java.demo.synchronization.selection;
import static com.ensoftcorp.atlas.java.demo.synchronization.RaceCheck.raceCheck;

import com.ensoftcorp.atlas.java.core.query.Attr;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.StyledResult;
import com.ensoftcorp.atlas.java.demo.util.DisplayItem;
import com.ensoftcorp.atlas.java.ui.scripts.selections.SelectionDetailScript;
public class SafeSynchronization implements SelectionDetailScript{

	@Override
	public String[] getSupportedNodeTags() {
		return new String[]{Attr.Node.FIELD};
	}

	@Override
	public String[] getSupportedEdgeTags() {
		return null;
	}

	@Override
	public StyledResult selectionChanged(SelectionInput input) {
		Q selection = input.getInterpretedSelection();
		DisplayItem di = raceCheck(selection);
		return new StyledResult(di.getQ(), di.getHighlighter());
	}

	@Override
	public String getTitle() {
		return "Safe Synchronization";
	}
}