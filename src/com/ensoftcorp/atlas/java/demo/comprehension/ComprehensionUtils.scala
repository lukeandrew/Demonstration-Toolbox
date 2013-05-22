package com.ensoftcorp.atlas.java.demo.comprehension

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
import com.ensoftcorp.atlas.java.core.db.graph.Graph
import com.ensoftcorp.atlas.java.core.db.graph.operation.DifferenceGraph
import com.ensoftcorp.atlas.java.core.db.set.DifferenceSet
import com.ensoftcorp.atlas.java.core.highlight.Highlighter
import java.awt.Color

import com.ensoftcorp.atlas.java.demo.util.Artist;
import com.ensoftcorp.atlas.java.demo.util.DisplayItem;
import com.ensoftcorp.atlas.java.demo.util.Artist.PaintMode;
import com.ensoftcorp.atlas.java.demo.util.ScriptUtils._;
import com.ensoftcorp.atlas.java.core.db.graph.EdgeGraph

/**
 * A class which contains some comprehension-focused queries.
 * 
 * @author Tom Deering
 */
object ComprehensionUtils {
  
  /**
   * Computes a bidirectional traversal from the given origin using the given edge kinds,
   * and colors the results in an attractive fashion
   */
  private def bidirectional(start:Q, edgeKinds:String*):DisplayItem = {
    var context = universe.edgesTaggedWithAny(edgeKinds:_*)
    context = differenceEdges(context, context.edgesTaggedWithAny(Edge.PER_CONTROL_FLOW))
    
    var ancestors = context.reverse(start)
    var decendents = context.forward(start)
    
    var artist = new Artist
    artist.addTint(start, Color.YELLOW, PaintMode.NODES)
    artist.addTint(ancestors difference start, Color.RED, PaintMode.NODES)
    artist.addTint(decendents difference start, Color.BLUE, PaintMode.NODES)

    new DisplayItem(ancestors union decendents, artist.getHighlighter)
  }
  
  /**
   * Returns a bidirectional type hierarchy from the given types.
   */
  def typeHierarchy(start:Q):DisplayItem = {
    bidirectional(start, Edge.SUPERTYPE)
  }
  
  /**
   * Returns a bidirectional call graph from the given method.
   */
  def callGraph(start:Q):DisplayItem = {
    bidirectional(start, Edge.CALL)
  }
  
  /**
   * Returns a bidirectional data flow graph from the given nodes.
   */
  def dataFlow(start:Q):DisplayItem = {
    bidirectional(start, Edge.DF_LOCAL, Edge.DF_INTERPROCEDURAL)
  }
  
  /**
   * Returns the bidirectional declarations from the given nodes.
   */
  def declarations(start:Q):DisplayItem = {
    bidirectional(start, Edge.DECLARES)
  }
  
  /**
   * Returns the bidirectional override graph from the given methods.
   */
  def overrides(start:Q):DisplayItem = {
    bidirectional(start, Edge.OVERRIDES)
  }
  
  /**
   * Returns all direct edges of the given types between the first and second Q.
   */
  def interactions(first:Q, second:Q, edgeTypes:String*):DisplayItem = {
    // Get all edges of the allowed types and with only PER_METHOD granularity
    var edgeContext = universe.edgesTaggedWithAny(edgeTypes:_*) 
    edgeContext = differenceEdges(edgeContext, edgeContext.edgesTaggedWithAny(Edge.PER_CONTROL_FLOW))
    
    // Narrow the edge context to only the edges one step between the two subgraphs
    var firstToSecond = edgeContext.forwardStep(first) intersection edgeContext.reverseStep(second)
    var secondToFirst = edgeContext.forwardStep(second) intersection edgeContext.reverseStep(first)
    edgeContext = (firstToSecond union secondToFirst).retainEdges
    
    var artist = new Artist
    artist.addTint(first, Color.RED, PaintMode.NODES)
    artist.addTint(firstToSecond, Color.RED, PaintMode.EDGES)
    artist.addTint(second, Color.BLUE, PaintMode.NODES)
    artist.addTint(secondToFirst, Color.BLUE, PaintMode.EDGES)

    new DisplayItem(edgeContext, artist.getHighlighter)
  }
}