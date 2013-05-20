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

object ComprehensionUtils {

  def typeHierarchy(start:Q):Q = {
    empty
  }
  
}