package com.ensoftcorp.atlas.java.demo.util;

import static com.ensoftcorp.atlas.java.core.script.Common.toQ;
import static com.ensoftcorp.atlas.java.core.script.Common.universe;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ensoftcorp.atlas.java.core.db.graph.Graph;
import com.ensoftcorp.atlas.java.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.java.core.db.graph.operation.InducedGraph;
import com.ensoftcorp.atlas.java.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.java.core.db.set.EmptyAtlasSet;
import com.ensoftcorp.atlas.java.core.highlight.Highlighter;
import com.ensoftcorp.atlas.java.core.query.Q;

/**
 * A class for advanced highlighting functionality, including color blending.
 * @author Tom Deeringg
 *
 */
public class Artist {
	/**
	 * Allows the user to specify whether they want to paint nodes, edges, or both.
	 * @author tom
	 *
	 */
	public enum PaintMode{NODES,EDGES,BOTH}
	
	/**
	 * A map of the various colors that have been applied to each graph element, for later blending.
	 */
	private Map<GraphElement,List<Color>> canvas = new HashMap<GraphElement, List<Color>>();

	/**
	 * Adds the given color with the given mode to the given Q.
	 * 
	 * @param addTo Graph elements to color
	 * @param color The color to add
	 * @param mode Whether to paint nodes, edges, or both
	 */
	public void addColor(Q addTo, Color color, PaintMode mode){
		Graph addToG = addTo.eval();
		
		if(mode.equals(PaintMode.NODES) || mode.equals(PaintMode.BOTH)){
			for(GraphElement ge : addToG.nodes()) add(ge, color);
		}
		
		if(mode.equals(PaintMode.EDGES) || mode.equals(PaintMode.BOTH)){
			for(GraphElement ge : addToG.edges()) add(ge, color);			
		}
	}
	
	/**
	 * Private convenience method for adding a new color to the canvas.
	 * @param key Graph element to color
	 * @param value The color to add
	 */
	private void add(GraphElement key, Color value){
		List<Color> list = canvas.get(key);
		if(list == null){
			list = new LinkedList<Color>();
			canvas.put(key, list);
		}
		list.add(value);
	}
	
	/**
	 * Given a particular element that the artist has painted, performs color blending
	 * of the various colors that have been applied in order to arrive at the final color.
	 * @param key The graph element for which to derive a color
	 * @return
	 */
	private Color colorForElement(GraphElement key){
		int r=0, g=0, b=0;

		List<Color> list = canvas.get(key);
		for(Color c : list){
			r += c.getRed();
			g += c.getGreen();
			b += c.getBlue();
		}
		
		return new Color(r/list.size(), g/list.size(), b/list.size());	
	}
	
	/**
	 * Returns a highlighter that represents the painting work done by this artist.
	 * @return
	 */
	public Highlighter getHighlighter(){
		Highlighter h = new Highlighter();
				
		for(GraphElement ge : canvas.keySet()){
			Color color = colorForElement(ge);
			
			boolean edge = universe().eval().edges().contains(ge);
			boolean node = universe().eval().nodes().contains(ge);
			AtlasHashSet<GraphElement> geSet = new AtlasHashSet<GraphElement>();
			geSet.add(ge);
			if(node){
				h.highlightNodes(toQ(new InducedGraph(geSet, EmptyAtlasSet.<GraphElement>instance())), color);
			}
			if(edge){
				h.highlightEdges(toQ(new InducedGraph(universe().eval().nodes(),geSet)), color);
			}
		}
		return h;
	}
}
