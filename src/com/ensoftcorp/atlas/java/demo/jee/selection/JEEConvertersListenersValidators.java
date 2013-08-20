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

public class JEEConvertersListenersValidators implements SelectionDetailScript{

	@Override
	public String[] getSupportedNodeTags() {
		return new String[]{Node.TYPE};
	}

	@Override
	public String[] getSupportedEdgeTags() {
		return null;
	}

	@Override
	public StyledResult selectionChanged(SelectionInput input) {
		Q selection = input.getInterpretedSelection();

		/*
		 * The user may have clicked on the FacesConverter annotation. We can show:
		 * 
		 * > The annotation and decorated classes
		 * > All uses of converters
		 */
		Q facesConverter = selection.intersection(typeSelect("javax.faces.convert", "FacesConverter"));
		Q facesConverterAnnotated = edges(Edge.ANNOTATION).reverse(facesConverter);
		String matchRegex = "(.*)(f:convert|converter)(.*)";
		String nonMatchRegex = "a^";
		Q allConverterNonJavaReferences = empty();
		Q indexProjects = index().nodesTaggedWithAny(Node.PROJECT);
		if(facesConverter.eval().nodes().size() > 0){
			for(GraphElement project : indexProjects.eval().nodes()){
				allConverterNonJavaReferences = allConverterNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
		
		/*
		 * The user may have clicked on custom converter type. We can show:
		 * 
		 * > The type and its converter annotation
		 * > References to any converter in xhtml (Atlas needs better indexing of parameterized annotations)
		 */
		Q converters = selection.intersection(stepFrom(edges(Edge.ANNOTATION), typeSelect("javax.faces.convert", "FacesConverter")));
		Q converterAnnotations = edges(Edge.ANNOTATION).forwardStep(converters);
		nonMatchRegex = "(.*)(BigDecimal|BigInteger|Boolean|Byte|Character|DateTime|Double|Float|Integer|Long|Number|Short)(.*)";
		Q selectionProjects = edges(Edge.DECLARES).reverse(selection).nodesTaggedWithAny(Node.PROJECT);
		Q converterNonJavaReferences = empty();
		if(converters.eval().nodes().size() > 0){
			for(GraphElement project : selectionProjects.eval().nodes()){
				converterNonJavaReferences = converterNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
		
		/*
		 * The user may have clicked on the ActionListener interface type. We can show:
		 * 
		 * > All listener implementers
		 * > All references to value change listeners in the app
		 */
		Q actionListener = typeSelect("javax.faces.event","ActionListener");
		Q actionListeners = selection.intersection(actionListener);
		Q allActionListeners = edges(Edge.SUPERTYPE).reverse(actionListeners);
		Q allActionListenerNonJavaReferences = empty();
		matchRegex = "(.*)(actionListener)(.*)";
		nonMatchRegex = "a^";
		if(actionListeners.eval().nodes().size() > 0){
			for(GraphElement project : indexProjects.eval().nodes()){
				allActionListenerNonJavaReferences = allActionListenerNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
		
		/*
		 * The user may have clicked on the ValueChangeListener interface type. We can show:
		 * 
		 * > All listener implementers
		 * > All references to value change listeners in the app
		 */
		Q valueListener = typeSelect("javax.faces.event","ValueChangeListener");
		Q valueListeners = selection.intersection(valueListener);
		Q allValueListeners = edges(Edge.SUPERTYPE).reverse(valueListeners);
		Q allValueListenerNonJavaReferences = empty();
		matchRegex = "(.*)(valueChangeListener)(.*)";
		nonMatchRegex = "a^";
		if(valueListeners.eval().nodes().size() > 0){
			for(GraphElement project : indexProjects.eval().nodes()){
				allValueListenerNonJavaReferences = allValueListenerNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
				
		/*
		 * The user may have clicked on a custom listener implementer. We can show:
		 * 
		 * > The class and the interface it implemented
		 * > References in xhtml to this listener 
		 */
		Q listenerInterfaces = actionListener.union(valueListener);
		Q customListeners = selection.intersection(stepFrom(edges(Edge.SUPERTYPE), listenerInterfaces));
		Q customListenerInterfaces = edges(Edge.SUPERTYPE).between(customListeners, listenerInterfaces);
		Q customListenerNonJavaReferences = empty();
		nonMatchRegex = "a^";
		for(GraphElement listener : customListeners.eval().nodes()){
			matchRegex = "(.*)type(.*)" + Pattern.quote((String) listener.attr().get(Node.NAME)) + "(.*)";
			for(GraphElement project : selectionProjects.eval().nodes()){
				customListenerNonJavaReferences = customListenerNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
		
		/*
		 * The user may have clicked on the Validator interface. We can show:
		 * 
		 * > All validators and their uses
		 */
		Q validator = selection.intersection(typeSelect("javax.faces.validator", "Validator"));
		Q allValidators = edges(Edge.SUPERTYPE).reverse(validator);
		matchRegex = "(.*)(f:validate|validator)(.*)";
		nonMatchRegex = "a^";
		Q allValidatorNonJavaReferences = empty();
		if(validator.eval().nodes().size() > 0){
			for(GraphElement project : indexProjects.eval().nodes()){
				allValidatorNonJavaReferences = allValidatorNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
		
		/*
		 * The user may have clicked on a custom validator type. We can show: 
		 * 
		 * > The validator and its uses
		 * 
		 */
		Q customValidator = selection.intersection(stepFrom(edges(Edge.SUPERTYPE), validator));
		Q customValidatorStructure = edges(Edge.SUPERTYPE).between(customValidator, validator);
		nonMatchRegex = "(.*)(validateBean|validateDoubleRange|validateLength|validateLongRange|validateRegEx|validateRequired)(.*)";
		Q customValidatorNonJavaReferences = empty();
		for(GraphElement ge : customValidator.eval().nodes()){
			matchRegex = "(.*)(validator)(.*)" + Pattern.quote((String) ge.attr().get(Node.NAME)) + "(.*)";
			for(GraphElement project : selectionProjects.eval().nodes()){
				customValidatorNonJavaReferences = customValidatorNonJavaReferences.union(
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, nonMatchRegex, "XHTML", "xhtml"));
			}
		}
		
		Q res = facesConverter.union(
				facesConverterAnnotated,
				allConverterNonJavaReferences,
				converters,
				converterAnnotations,
				converterNonJavaReferences,
				actionListeners,
				allActionListeners,
				allActionListenerNonJavaReferences,
				valueListeners,
				allValueListeners,
				allValueListenerNonJavaReferences,
				customListeners,
				customListenerInterfaces,
				customListenerNonJavaReferences,
				validator,
				allValidators,
				allValidatorNonJavaReferences,
				customValidator,
				customValidatorStructure,
				customValidatorNonJavaReferences);
		
		return new StyledResult(res);
	}

	@Override
	public String getTitle() {
		return "JEE Converters/Listeners/Validators";
	}

}
