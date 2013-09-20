package com.ensoftcorp.atlas.java.demo.synchronization
import com.ensoftcorp.atlas.java.core.query.Q
import java.lang.Boolean
import scala.collection.JavaConversions.iterableAsScalaIterable
import com.ensoftcorp.atlas.java.core.db.Accuracy.EXACT
import com.ensoftcorp.atlas.java.core.db.Trinary.FALSE
import com.ensoftcorp.atlas.java.core.db.graph.operation.InducedGraph
import com.ensoftcorp.atlas.java.core.query.Attr.Edge
import com.ensoftcorp.atlas.java.core.query.Attr.Node
import com.ensoftcorp.atlas.java.core.query.Attr
import com.ensoftcorp.atlas.java.core.query.Q
import com.ensoftcorp.atlas.java.core.script.Common._
import com.ensoftcorp.atlas.java.core.script.Common
import com.ensoftcorp.atlas.java.core.db.graph.GraphElement
import com.ensoftcorp.atlas.java.core.db.set.AtlasHashSet
import com.ensoftcorp.atlas.java.core.db.set.EmptyAtlasSet
import com.ensoftcorp.atlas.java.core.db.set.AtlasSet
import java.util.HashSet
import com.ensoftcorp.atlas.java.core.db.graph.Graph
import com.ensoftcorp.atlas.java.core.db.graph.operation.DifferenceGraph
import com.ensoftcorp.atlas.java.core.db.set.DifferenceSet
import java.util.HashMap
import com.ensoftcorp.atlas.java.demo.util.DisplayItem
import com.ensoftcorp.atlas.java.demo.util.Artist
import java.awt.Color
import com.ensoftcorp.atlas.java.demo.util.Artist.PaintMode
import com.ensoftcorp.atlas.java.demo.util.ScriptUtils._

/**
 * Demonstration Atlas scripts to help look for safe, synchronized access of objects
 */
object RaceCheck {  
  
  /**
   * Given a Q which holds references to data structures which should be read or written only
   * when synchronized, returns a DisplayItem encapsulating safe accesses (blue) and unsafe accesses (red).
   */
  def raceCheck(sharedTokens:Q):DisplayItem = {
    var decContext = edges(Edge.DECLARES)
    var rwContext = edges(Edge.READ, Edge.WRITE)
    rwContext = differenceEdges(rwContext, rwContext.edgesTaggedWithAny(Edge.PER_METHOD))
    
    // Everywhere a read or write to the shared token occurs
    var accessors = stepTo(rwContext, sharedTokens) union stepFrom(rwContext, sharedTokens)
    
    // Ignore accesses by initializer blocks, since these write to fields when defining them
    accessors = accessors difference declarations(methods("<init>") union methods("<clinit>"))
    
    var synchronizeBlocks = universe.nodesTaggedWithAny(Node.SYNCHRONIZED)
    // Special logic to ignore "CF blocks" which represent the argument of synchronization statements. Hacktacular!
    var decBySynchronized = decContext.forwardStep(synchronizeBlocks)
    accessors = accessors difference (edges(Edge.CONTROL_FLOW).reverseStep(decBySynchronized) difference decBySynchronized)
    
    var goodAccesses = decContext.between(synchronizeBlocks, accessors) intersection accessors
    var badAccesses = accessors difference goodAccesses
    
    // Add the edges to the shared tokens
    goodAccesses = (rwContext.forwardStep(goodAccesses) intersection rwContext.reverseStep(sharedTokens)) union 
    			   (rwContext.reverseStep(goodAccesses) intersection rwContext.forwardStep(sharedTokens))
    badAccesses = (rwContext.forwardStep(badAccesses) intersection rwContext.reverseStep(sharedTokens)) union 
    			   (rwContext.reverseStep(badAccesses) intersection rwContext.forwardStep(sharedTokens))
    
    // The tokens themselves will be yellow, good accesses blue, and bad accesses red
    var artist = new Artist
    artist.addColor(sharedTokens, Color.YELLOW, PaintMode.NODES)
    artist.addColor(badAccesses difference sharedTokens, Color.RED, PaintMode.NODES)
    artist.addColor(badAccesses, Color.RED, PaintMode.EDGES)
    artist.addColor(goodAccesses difference sharedTokens, Color.BLUE, PaintMode.NODES)
    artist.addColor(goodAccesses, Color.BLUE, PaintMode.EDGES)
    
    new DisplayItem(sharedTokens union goodAccesses union badAccesses, artist.getHighlighter())
  }
}