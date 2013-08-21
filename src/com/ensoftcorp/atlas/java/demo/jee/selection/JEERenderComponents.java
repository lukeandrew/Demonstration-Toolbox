package com.ensoftcorp.atlas.java.demo.jee.selection;

import static com.ensoftcorp.atlas.java.core.script.Common.*;
import static com.ensoftcorp.atlas.java.core.script.Common.stepFrom;
import static com.ensoftcorp.atlas.java.core.script.Common.typeSelect;
import static com.ensoftcorp.atlas.java.demo.jee.JEEUtils.getComponentTagMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.ensoftcorp.atlas.java.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.java.core.query.Attr.Edge;
import com.ensoftcorp.atlas.java.core.query.Attr.Node;
import com.ensoftcorp.atlas.java.core.query.Q;
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
public class JEERenderComponents implements SelectionDetailScript{

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
		 * The user may have clicked on render or component annotation. We can show:
		 * 
		 * > The annotation
		 * > All classes annotated with this
		 */
		Q annotations = typeSelect("javax.faces.render","FacesRenderer").union(
				typeSelect("javax.faces.render","FacesComponent"));
		Q selectedAnnotations = selection.intersection(annotations);
		Q selectedClassesforAnnotations = edges(Edge.ANNOTATION).reverseStep(selectedAnnotations);
		
		/*
		 * The user may have clicked on a custom component or renderer class. We can show:
		 * 
		 * > The class and its annotations
		 * > All XHTML references which use this renderer.
		 */
		Q components = stepFrom(edges(Edge.ANNOTATION), annotations);
		Q selectedComponents = selection.intersection(components);
		Q annotationsForSelectedComponents = edges(Edge.ANNOTATION).betweenStep(selectedComponents, annotations);
		Q selectedComponentProjects = edges(Edge.DECLARES).reverse(selectedComponents).nodesTaggedWithAny(Node.PROJECT);
		
		// Construct a mapping from component types to tags from the taglib files
		Map<String, String> typeToTag = getComponentTagMapping(selectedComponentProjects);
		
		// Get the types for the selected components and renderers
		// FIXME: Replace hard-coding with Atlas queries after parameterized annotation representation is updated.
		Map<GraphElement, String> classToType = new HashMap<GraphElement, String>(){{
			put(typeSelect("javaeetutorial.dukesbookstore.renderers","AreaRenderer").eval().nodes().getFirst(), "DemoArea");
			put(typeSelect("javaeetutorial.dukesbookstore.renderers","MapRenderer").eval().nodes().getFirst(), "DemoMap");
			put(typeSelect("javaeetutorial.dukesbookstore.components","AreaComponent").eval().nodes().getFirst(), "DemoArea");
			put(typeSelect("javaeetutorial.dukesbookstore.components","MapComponent").eval().nodes().getFirst(), "DemoMap");
		}};

		// Get the XML references to the component types (from taglib files)
		Q nonJavaComponentTypeReferences = empty();
		Q nonJavaComponentTagReferences = empty();
		String nonMatchRegex = "a^";
		String matchRegex = "";
		for(GraphElement ge : selectedComponents.eval().nodes()) {
			String type = classToType.get(ge);
			if(type == null) continue;
			String tag = typeToTag.get(type);
			for(GraphElement project : selectedComponentProjects.eval().nodes()){
				matchRegex = "(.*)" + Pattern.quote(type) + "(.*)";
				nonJavaComponentTypeReferences = nonJavaComponentTypeReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "xml", "XML"));
				if(tag != null){
					matchRegex = "(.*)" + Pattern.quote(":"+tag) + "(.*)";
					nonJavaComponentTagReferences = nonJavaComponentTagReferences.union(
							JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "xhtml", "XHTML"));
				}
			}
		}

		Q res = selectedClassesforAnnotations.union(
				annotationsForSelectedComponents,
				nonJavaComponentTypeReferences,
				nonJavaComponentTagReferences);
		
		return new StyledResult(res);
	}

	@Override
	public String getTitle() {
		return "JEE Renderers/Components";
	}

}
