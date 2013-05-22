package com.ensoftcorp.atlas.java.demo.util

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
import scala.util.matching.Regex

/**
 * Broadly-useful queries for writing other scripts or using directly on the interpreter.
 */
object ScriptUtils {
  /**
   * Types which represent arrays of other types
   *
   * NOTE: These nodes are NOT declared by anything. They are outside of any project.
   */
  val arrayTypes = universe.selectNode(Node.IS_ARRAYTYPE)

  /**
   * Types which represent language primitive types
   *
   * NOTE: These nodes are NOT declared by anything. They are outside of any project.
   */
  val primitiveTypes = universe.selectNode(Node.IS_PRIMITIVE)

  /**
   * Summary invoke nodes, representing invocations on methods.
   *
   * NOTE: These nodes are NOT declared by anything. They are outside of any project.
   */
  val invokeNodes = universe.nodesTaggedWithAny(Node.INVOKE)

  /**
   * Everything declared under any of the known API projects, if they are in the index.
   */
  val libraries = declarations(universe.nodesTaggedWithAll(Attr.Node.LIBRARY)).difference(arrayTypes, primitiveTypes, invokeNodes)
        
  /**
   * Everything in the universe which is declared under the app itself
   */
  val app = universe.difference(libraries, invokeNodes, arrayTypes, primitiveTypes)
  
  /**
   * Everything declared underneath the given origin.
   * 
   * @param origin The parent under which to find declared things
   * @param scope Q, the operational universe. Should include all needed nodes and edges.
   * @return A graph of any NODE type, no edges
   */
  def declarations(origin:Q, scope:Q=universe):Q = { 
    universe.edgesTaggedWithAny(Edge.DECLARES).forward(origin).retainNodes
  }
  
  /**
   * Everything declared under the given method, but NOT declared under further
   * methods and classes which the method declares. Only retrieves things within
   * the method proper.
   * 
   * @param origin The parent methods under which to find declared things
   * @param scope Q, the operational universe. Should include all needed nodes and edges.
   * @return A graph of any NODE type, no edges
   */
  def methodDeclarations(origin:Q, scope:Q=universe):Q = {
    var mOrigin = origin.nodesTaggedWithAny(Node.METHOD)
    var context = toQ(new InducedGraph((scope difference scope.nodesTaggedWithAny(Node.TYPE)).eval.nodes, 
                                        scope.edgesTaggedWithAny(Edge.DECLARES).eval.edges))
    
    declarations(origin, context)                                
  }
  
  /**
   * Everything declared underneath the project with the given name.
   * 
   * @param name The name of the project under which to find declarations
   * @param scope Q, the operational universe. Should include all needed nodes and edges.
   */
  def projectDeclarations(name:String, scope:Q=universe):Q = { 
    declarations(universe.project(name)) 
  }
  
  /**
   * Everything declared underneath the package with the given name.
   * 
   * IMPORTANT: Does not get anything declared within sub-packages! For that, use
   *            packageTransitiveDeclarations(projectName, packageName)
   *            
   * @param name The name of the package under which to find declared things
   * @param scope Q, the operational universe. Should include all needed nodes and edges.
   * @return A graph of any NODE type, no edges
   */
  def packageDeclarations(name:String, scope:Q=universe):Q = { 
    declarations(universe.pkg(name), scope)
  }
  
  /**
   * Everything declared underneath the package with the given name, as well as
   * any subpackages. Optionally, a filtering project name can be provided.
   * 
   * @param name The name of the package under which to find declared things
   * @param scope Q, the operational universe. Should include all needed nodes and edges.
   * @return A graph of any NODE type, no edges
   */
  def packageTransitiveDeclarations(packageName:String, projectName:String = "", scope:Q=universe):Q = {
    var result = new AtlasHashSet[GraphElement]
    
    // Start from the project name if given, or else the universe.
    var projects = if(projectName==null || projectName.isEmpty()) universe else projectDeclarations(projectName, scope)
    
    // Find all package nodes within the given projects
    var pkgNodes = projects.nodesTaggedWithAny(Node.PACKAGE)
    
    // Go over all package nodes
    var itr = pkgNodes.eval.nodes.iterator()
    while(itr.hasNext()){
      var nextPkg = itr.next()
      var nextName = nextPkg.attr.get(Node.NAME)
  
      // If the name has the given package name as a prefix, it is a subpackage.
      // Add everything declared under this subpackage
      if(nextName.asInstanceOf[String].startsWith(packageName)){
        result.addAll(packageDeclarations(nextName.asInstanceOf[String], scope).eval.nodes)
      }
    }

    toQ(new InducedGraph(result, EmptyAtlasSet.instance[GraphElement]))
  }
  
  private var nameMap = null.asInstanceOf[HashMap[String, AtlasSet[GraphElement]]]
  private def addToMap(key:String, value:GraphElement, map:HashMap[String, AtlasSet[GraphElement]]) = {
    var set = map.get(key)
    if(set == null){
      set = new AtlasHashSet[GraphElement]
      map.put(key, set)
    }
    set.add(value)
  }
  
  def nodesMatchingRegex(regex:String):Q = {
    // Initialize the map of names if not yet initialized
    if(nameMap == null){
    	nameMap = new HashMap[String, AtlasSet[GraphElement]]
    	var ge = 0
    	for(ge <- universe.eval.nodes){
    	  addToMap(ge.attr.get(Node.NAME).asInstanceOf[String], ge, nameMap)
    	}
    }
    
    // The result we will build then return
    var result = empty
    
    // Go through the map of names and add all nodes for which the regex is a full match
    var str = ""
    for(str <- nameMap.keySet){
      if(str matches regex){
        result = result union toQ(new InducedGraph(nameMap.get(str), EmptyAtlasSet.instance[GraphElement]))
      }
    }
    
    result
  }
  
  /**
   * Removes *only edges* from the context which are at a CF block level of granularity
   */
  def differenceEdges(context:Q, toRemove:Q):Q = {
    var nonPerCFEdges = new DifferenceSet(context.eval.edges, toRemove.eval.edges)
    toQ(new InducedGraph(context.eval.nodes, nonPerCFEdges))
  }
}