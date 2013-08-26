package com.ensoftcorp.atlas.java.demo.apicompliance

import java.awt.Color
import scala.collection.JavaConversions.iterableAsScalaIterable
import com.ensoftcorp.atlas.java.core.db.Accuracy.EXACT
import com.ensoftcorp.atlas.java.core.db.Trinary.FALSE
import com.ensoftcorp.atlas.java.core.query.Attr
import com.ensoftcorp.atlas.java.core.query.Attr.Edge
import com.ensoftcorp.atlas.java.core.query.Q
import com.ensoftcorp.atlas.java.core.query.Q
import com.ensoftcorp.atlas.java.core.script.Common._
import com.ensoftcorp.atlas.java.core.script.Common
import com.ensoftcorp.atlas.java.demo.util.Artist
import com.ensoftcorp.atlas.java.demo.util.Artist.PaintMode
import com.ensoftcorp.atlas.java.demo.util.DisplayItem
import com.ensoftcorp.atlas.java.demo.util.ScriptUtils._
import com.ensoftcorp.atlas.java.core.db.graph.GraphElement

/**
 * Demonstration J-Atlas scripts to detect problems of API usage
 */
object APICompliance {  
  
  /**
   * Looks for events corresponding to calls to the Android MediaRecorder APIs in the current index.
   * For audio recording, Android has a 9-step API usage pattern. This script returns a DisplayItem
   * encapsulating the call events which have their prerequisite events satisfied (blue), as well as
   * call events for which it appears the prerequisite calls are unsatisfied (red).
   */
  def androidAudioCapture(toCheck:Q = universe):DisplayItem = {
    var callContext = edges(Edge.CALL).forwardStep(toCheck).reverseStep(toCheck);
    var cfContext = edges(Edge.CONTROL_FLOW)
    
    // Grab the relevant API methods
    var mediaRecorder = methodSelect("android.media","MediaRecorder","MediaRecorder")
    var setAudioSource = methodSelect("android.media","MediaRecorder","setAudioSource")
    var setOutputFormat = methodSelect("android.media","MediaRecorder","setOutputFormat")
    var setOutputFile = methodSelect("android.media","MediaRecorder","setOutputFile")
    var setAudioEncoder = methodSelect("android.media","MediaRecorder","setAudioEncoder")
    var prepare = methodSelect("android.media","MediaRecorder","prepare")
    var start = methodSelect("android.media","MediaRecorder","start")
    var stop = methodSelect("android.media","MediaRecorder","stop")
    var release = methodSelect("android.media","MediaRecorder","release")
    var apis = mediaRecorder.union(setAudioSource, setOutputFormat, setOutputFile,
        setAudioEncoder, prepare, start, stop, release)
    
    // The order in which we expect the calls to take place
    val requisiteOrder = Array(mediaRecorder, setAudioSource, setOutputFormat, setOutputFile, 
        setAudioEncoder, prepare, start, stop, release)
        
    // Grab the program's control flow graph 
    var cfGraph = inducedGraph(app.nodesTaggedWithAny(Attr.Node.CONTROL_FLOW), cfContext)
    
    // Find calls from the CF graph to the API methods
    var apiCalls = callContext.forwardStep(cfGraph) intersection callContext.reverseStep(apis)
    
    // Remove irrelevant CF blocks from the CF graph
    cfGraph = cfGraph difference (cfGraph difference cfGraph.between(apiCalls, apiCalls))
    
    // Build the combined projection of the CF graph plus API calls
    var cfEnhanced = cfGraph union apiCalls
    
    // For each API method call, check that prerequisite calls were made
    // Sort into good and bad buckets based on this criteria
    val (goodCalls, badCalls) = precedenceCheck(requisiteOrder, cfEnhanced)
    
    // Find those APIs which were not called
    var uncalledAPIs = apis difference apiCalls

    // Color APIs yellow, uncalled APIs and bad calls red, and good calls blue
    var artist = new Artist
    artist.addColor(apis, Color.YELLOW, PaintMode.NODES)
    artist.addColor(uncalledAPIs, Color.RED, PaintMode.NODES)
    artist.addColor(stepFrom(apiCalls, goodCalls), Color.BLUE, PaintMode.NODES)
    artist.addColor(goodCalls, Color.BLUE, PaintMode.EDGES)
    artist.addColor(stepFrom(apiCalls, badCalls), Color.RED, PaintMode.NODES)
    artist.addColor(badCalls, Color.RED, PaintMode.EDGES)
    
    new DisplayItem(cfEnhanced union apis, artist.getHighlighter)
  }
  
  /**
   * Given a set of methods in an order array and a context which includes control
   * flow nodes/edges and call edges to methods, checks whether each call to an 
   * ordered method was preceded by calls to ordered methods in lower indexes.
   * 
   * Returns a tuple of such calls, sorted into good and bad buckets.
   */
  private def precedenceCheck(order:Array[Q], cfEnhanced:Q):(Q,Q) = {
    var goodCalls = empty
    var badCalls = empty
    
    var cg = cfEnhanced.edgesTaggedWithAny(Edge.CALL)
    var cfg = cfEnhanced.edgesTaggedWithAny(Edge.CONTROL_FLOW)
    
    // For each call edge
    var ge = 0
    for(ge <- cg.eval.edges){
      // Grab the caller and the thing that was called
      var caller = toQ(toGraph(ge.getNode(GraphElement.EdgeDirection.FROM)))
      var called = toQ(toGraph(ge.getNode(GraphElement.EdgeDirection.TO)))
      
      // Find out where the called thing is in our ordering
      var idx = order.indexWhere((x) => (x intersection called).eval.nodes.size > 0)
      object AllDone extends Exception { }
      try{
        // Check that each prereq call is satisfied
        var i = 0
        for(i <- 0 until idx){
	        var prereq = order(i)
	        var pCallers = stepFrom(cg, prereq)
	        
	        // The prereq method was not called. Add to bad calls
	        if((cfg.reverse(caller) intersection pCallers).eval().nodes().size() == 0){
	          badCalls = badCalls union (cg.forwardStep(caller) intersection cg.reverseStep(called))
	          throw AllDone
	        }
        }
        // All prereqs satisfied, add to good calls
        goodCalls = goodCalls union (cg.forwardStep(caller) intersection cg.reverseStep(called))
      } catch {
        case AllDone =>
      }   
    }
    (goodCalls, badCalls)
  }
}