package com.ensoftcorp.atlas.java.demo.jee.selection;

import static com.ensoftcorp.atlas.java.core.script.Common.*;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.ensoftcorp.atlas.java.core.db.graph.Address;
import com.ensoftcorp.atlas.java.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.java.core.query.Attr.Edge;
import com.ensoftcorp.atlas.java.core.query.Attr.Node;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.Common;
import com.ensoftcorp.atlas.java.core.script.CommonQueries;
import com.ensoftcorp.atlas.java.core.script.CommonQueries.TraversalDirection;
import com.ensoftcorp.atlas.java.core.script.StyledResult;
import com.ensoftcorp.atlas.java.demo.jee.JEEUtils;
import com.ensoftcorp.atlas.java.ui.scripts.selections.SelectionDetailScript;

/**
 * Produces a helpful, persistence-relevant graph in response to selections of:
 * 
 * > Persistence annotation types
 * > Classes annotated with @Entitity
 * > Persistent fields of entity classes
 * > Object references annotated with @PersistenceUnit or @PersistenceContext
 * > Persistence API types
 * 
 * @author tom
 *
 */
public class JEEEnterpriseBeans implements SelectionDetailScript{

	@Override
	public String[] getSupportedNodeTags() {
		return new String[]{Node.FIELD, Node.CLASS, Node.ANNOTATION};
	}

	@Override
	public String[] getSupportedEdgeTags() {
		return null;
	}

	@Override
	public StyledResult selectionChanged(SelectionInput input) {
		Q selection = input.getInterpretedSelection();
		
		/* 
		 * The user may have clicked on an EJB annotation. If so, show all the things
		 * with that annotation. We can show:
		 * 
		 * > The annotation
		 * > All classes annotated with this
		 */
		Q ejbAnnotations = CommonQueries.packageStructure("javax.ejb", TraversalDirection.FORWARD).union(
				CommonQueries.packageStructure("javax.enterprise", TraversalDirection.FORWARD)).nodesTaggedWithAny(Node.ANNOTATION);
		Q selectedAnnotations = selection.intersection(ejbAnnotations);
		Q annotatedWithSelected = edges(Edge.ANNOTATION).reverseStep(selectedAnnotations);
		
		/*
		 * The user may have clicked on a class which is a bean. We can show:
		 * 
		 * > The custom class
		 * > Its bean annotations
		 * > All references to the custom class
		 */
		Q ejbAnnotated = stepFrom(edges(Edge.ANNOTATION), ejbAnnotations);
		Q selectedAnnotated = selection.intersection(ejbAnnotated).nodesTaggedWithAny(Node.TYPE);
		Q selectedWithAnnotations = edges(Edge.ANNOTATION).betweenStep(selectedAnnotated, ejbAnnotations);
		Q selectedWithAnnotatedNonJavaReferences = empty();
		String matchRegex = "";
		String nonMatchRegex = "a^";
		Q selectionProjects = edges(Edge.DECLARES).reverse(selectedAnnotated).nodesTaggedWithAny(Node.PROJECT);
		for(GraphElement ge : selectedAnnotated.eval().nodes()){
			String nodeName = (String) ge.attr().get(Node.NAME);
			nodeName = Pattern.quote(Character.toLowerCase(nodeName.charAt(0)) + (nodeName.length() > 1 ? nodeName.substring(1) : ""));
			matchRegex = "(.*)"+nodeName+"(.*)";
			for(GraphElement project : selectionProjects.eval().nodes()){
				selectedWithAnnotatedNonJavaReferences = selectedWithAnnotatedNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
		
		/*
		 * The user may have clicked on the field of a bean. We can show
		 * > The field itself
		 * > The declaring bean and its annotations
		 * > All references to the class/field pair
		 */
		Q ejbFields = edges(Edge.DECLARES).forwardStep(ejbAnnotated).nodesTaggedWithAny(Node.FIELD);
		Q selectedFields = selection.intersection(ejbFields);
		Q selectedFieldClasses = edges(Edge.DECLARES).reverseStep(selectedFields);
		Q selectedFieldClassAnnotations = edges(Edge.ANNOTATION).betweenStep(selectedFieldClasses, ejbAnnotations);
		Q selectedFieldNonJavaReferences = empty();
		selectionProjects = edges(Edge.DECLARES).reverse(selectedFields).nodesTaggedWithAny(Node.PROJECT);
		for(GraphElement ge : selectedFields.eval().nodes()){
			Q fQ = toQ(toGraph(ge));
			String fieldName = (String) ge.attr().get(Node.NAME);
			GraphElement classNode = stepFrom(edges(Edge.DECLARES), fQ).nodesTaggedWithAny(Node.CLASS).eval().nodes().getFirst();
			if(classNode == null) continue;
			String className = (String) classNode.attr().get(Node.NAME);
			className = Character.toLowerCase(className.charAt(0)) + (className.length() > 1 ? className.substring(1) : "");
			matchRegex = "(.*)" + Pattern.quote(className + "." + fieldName) + "(.*)";
			for(GraphElement project : selectionProjects.eval().nodes()){
				selectedFieldNonJavaReferences = selectedFieldNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
		
		Q res = annotatedWithSelected.union(
				selectedWithAnnotations,
				selectedWithAnnotatedNonJavaReferences,
				selectedFields,
				selectedFieldClasses,
				selectedFieldClassAnnotations,
				selectedFieldNonJavaReferences);
		
		return new StyledResult(res);
	}

	@Override
	public String getTitle() {
		return "JEE Enterprise Beans";
	}

}
