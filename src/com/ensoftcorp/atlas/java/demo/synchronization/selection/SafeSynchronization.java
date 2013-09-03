package com.ensoftcorp.atlas.java.demo.synchronization.selection;
import static com.ensoftcorp.atlas.java.demo.synchronization.RaceCheck.raceCheck;

import com.ensoftcorp.atlas.java.core.query.Attr;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.Common;
import com.ensoftcorp.atlas.java.core.script.StyledResult;
import com.ensoftcorp.atlas.java.demo.util.DisplayItem;
import com.ensoftcorp.atlas.java.ui.scripts.selections.SelectionDetailScript;
public class SafeSynchronization implements SelectionDetailScript{

	@Override
	public String[] getSupportedNodeTags() {
		return new String[]{Attr.Node.FIELD, Attr.Node.TYPE};
	}

	@Override
	public String[] getSupportedEdgeTags() {
		return null;
	}


	@Override
	public StyledResult selectionChanged(SelectionInput input) {
		Q selection = input.getInterpretedSelection();
		
		/*
		 * The user may have selected a class or a specific field. 
		 * If they have selected the class, we need to get it's declared fields.
		 */
		Q fields = Common.fieldsOf(selection);
		
		DisplayItem di = raceCheck(fields);
		return new StyledResult(di.getQ(), di.getHighlighter());
	}

	@Override
	public String getTitle() {
		return "Safe Synchronization";
	}
}