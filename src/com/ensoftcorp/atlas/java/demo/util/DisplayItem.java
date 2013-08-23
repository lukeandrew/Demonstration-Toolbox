package com.ensoftcorp.atlas.java.demo.util;

import com.ensoftcorp.atlas.java.core.highlight.Highlighter;
import com.ensoftcorp.atlas.java.core.query.Attr.Edge;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.Common;
import com.ensoftcorp.atlas.ui.viewer.graph.DisplayUtil;

/**
 * A class which wraps a Q as well as a Highlighter for display. 
 * @author Tom Deering
 *
 */
public class DisplayItem {
	Q q; 
	Highlighter highlighter;
	
	public DisplayItem(Q q, Highlighter highlighter) {
		super();
		this.q = q;
		this.highlighter = highlighter;
	}

	public Q getQ() {
		return q;
	}

	public Highlighter getHighlighter() {
		return highlighter;
	}
	
	public void show(){
	    Q displayExpr = Common.extend(q, Edge.DECLARES);  
	    DisplayUtil.displayGraph(displayExpr.eval(), highlighter);
	}
}
