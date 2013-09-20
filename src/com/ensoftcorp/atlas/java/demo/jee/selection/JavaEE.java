package com.ensoftcorp.atlas.java.demo.jee.selection;

import static com.ensoftcorp.atlas.java.core.script.Common.edges;
import static com.ensoftcorp.atlas.java.core.script.Common.empty;
import static com.ensoftcorp.atlas.java.core.script.Common.index;
import static com.ensoftcorp.atlas.java.core.script.Common.methodSelect;
import static com.ensoftcorp.atlas.java.core.script.Common.methodsOf;
import static com.ensoftcorp.atlas.java.core.script.Common.stepFrom;
import static com.ensoftcorp.atlas.java.core.script.Common.stepTo;
import static com.ensoftcorp.atlas.java.core.script.Common.toGraph;
import static com.ensoftcorp.atlas.java.core.script.Common.toQ;
import static com.ensoftcorp.atlas.java.core.script.Common.typeSelect;
import static com.ensoftcorp.atlas.java.demo.jee.JEEUtils.getComponentTagMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 * Produces graphs relevant to JEE Faces Converters, Listeners, and Validators.
 * 
 * @author tom
 *
 */
public class JavaEE implements SelectionDetailScript{

	@Override
	public String[] getSupportedNodeTags() {
		return new String[]{};
	}

	@Override
	public String[] getSupportedEdgeTags() {
		return null;
	}

	@Override
	public StyledResult selectionChanged(SelectionInput input) {
		Q selection = input.getInterpretedSelection();

		Q res = convertersListenersValidators(selection).union(
				persistence(selection),
				enterpriseBeans(selection),
				renderComponents(selection));
		
		return new StyledResult(res);
	}

	private Q convertersListenersValidators(Q selection){
		/*
		 * The user may have clicked on the FacesConverter annotation. We can show:
		 * 
		 * > The annotation and decorated classes
		 * > All uses of converters in XHTML
		 */
		Q facesConverter = typeSelect("javax.faces.convert", "FacesConverter");
		Q selectedFacesConverter = selection.intersection(facesConverter);
		Q selectedFacesConverterAnnotated = edges(Edge.ANNOTATION).reverse(selectedFacesConverter);
		Q allConverterNonJavaReferences = empty();
		if(selectedFacesConverter.eval().nodes().size() > 0)
			allConverterNonJavaReferences = JEEUtils.getRawStringReferences((Q)null, "(f:convert|converter)", "", null, "XHTML", "xhtml");

		
		/*
		 * The user may have clicked on custom converter type. We can show:
		 * 
		 * > The type and its converter annotation
		 * > References to any converter in xhtml (Atlas needs better indexing of parameterized annotations)
		 */
		Q selectedConverters = selection.intersection(stepFrom(edges(Edge.ANNOTATION), facesConverter));
		Q selectedConverterAnnotations = edges(Edge.ANNOTATION).betweenStep(selectedConverters, facesConverter);
		Q converterNonJavaReferences = empty();
		if(selectedConverters.eval().nodes().size() > 0)
			converterNonJavaReferences = JEEUtils.getRawStringReferences((Q)null, "(f:convert|converter)", "", "(.*)(BigDecimal|BigInteger|Boolean|Byte|Character|DateTime|Double|Float|Integer|Long|Number|Short)(.*)", "XHTML", "xhtml");
		
		/*
		 * The user may have clicked on the ActionListener interface type. We can show:
		 * 
		 * > All listener implementers
		 * > All references to value change listeners in the app
		 */
		Q actionListener = typeSelect("javax.faces.event","ActionListener");
		Q selectedActionListener = selection.intersection(actionListener);
		Q allActionListeners = edges(Edge.SUPERTYPE).reverse(selectedActionListener);
		Q allActionListenerNonJavaReferences = empty();
		if(selectedActionListener.eval().nodes().size() > 0)
			allActionListenerNonJavaReferences = JEEUtils.getRawStringReferences((Q)null, "actionListener", "", null, "XHTML", "xhtml");
		
		/*
		 * The user may have clicked on the ValueChangeListener interface type. We can show:
		 * 
		 * > All listener implementers
		 * > All references to value change listeners in the app
		 */
		Q valueListener = typeSelect("javax.faces.event","ValueChangeListener");
		Q selectedValueListener = selection.intersection(valueListener);
		Q allValueListeners = edges(Edge.SUPERTYPE).reverse(selectedValueListener);
		Q allValueListenerNonJavaReferences = empty();
		if(selectedValueListener.eval().nodes().size() > 0)
			allValueListenerNonJavaReferences =  JEEUtils.getRawStringReferences((Q)null, "valueChangeListener", "", null, "XHTML", "xhtml");
				
		/*
		 * The user may have clicked on a custom listener implementer. We can show:
		 * 
		 * > The class and the interface it implemented
		 * > References in xhtml to this listener 
		 */
		Q listenerInterfaces = actionListener.union(valueListener);
		Q allCustomListeners = stepFrom(edges(Edge.SUPERTYPE), listenerInterfaces);
		Q selectedCustomListeners = selection.intersection(allCustomListeners);
		Q customListenerInterfaces = edges(Edge.SUPERTYPE).between(selectedCustomListeners, listenerInterfaces);
		Q customListenerNonJavaReferences = empty();
		if(selectedCustomListeners.eval().nodes().size() > 0)
			customListenerNonJavaReferences = JEEUtils.getRawStringReferences(selectedCustomListeners, "type", "", null, "XHTML", "xhtml");
		
		
		/*
		 * The user may have clicked on the Validator interface. We can show:
		 * 
		 * > All validators and their uses
		 */
		Q validator = typeSelect("javax.faces.validator", "Validator");
		Q selectedValidator = selection.intersection(validator);
		Q allValidators = edges(Edge.SUPERTYPE).reverse(selectedValidator);
		Q allValidatorNonJavaReferences = empty();
		if(selectedValidator.eval().nodes().size() > 0)
			allValidatorNonJavaReferences = JEEUtils.getRawStringReferences((Q)null, "(f:validate|validator)", "", null, "XHTML", "xhtml");
		
		/*
		 * The user may have clicked on a custom validator type. We can show: 
		 * 
		 * > The validator and its uses
		 * 
		 */
		Q selectedCustomValidator = selection.intersection(stepFrom(edges(Edge.SUPERTYPE), validator));
		Q customValidatorStructure = edges(Edge.SUPERTYPE).between(selectedCustomValidator, validator);
		Q customValidatorNonJavaReferences = JEEUtils.getRawStringReferences(selectedCustomValidator, "validator", "", "(.*)(validateBean|validateDoubleRange|validateLength|validateLongRange|validateRegEx|validateRequired)(.*)", "XHTML", "xhtml");
		
		Q res = selectedFacesConverterAnnotated.union(
				allConverterNonJavaReferences,
				selectedConverterAnnotations,
				converterNonJavaReferences,
				allActionListeners,
				allActionListenerNonJavaReferences,
				allValueListeners,
				allValueListenerNonJavaReferences,
				customListenerInterfaces,
				customListenerNonJavaReferences,
				allValidators,
				allValidatorNonJavaReferences,
				customValidatorStructure,
				customValidatorNonJavaReferences);
		
		return res;
	}
	
	private Q persistence(Q selection){
		/*
		 * The user may select a class which is annotated as an Entity. Entities are 
		 * persistence items which are stored by the framework in DB tables. 
		 * Entities define database tables, and instances of Entities define database
		 * rows. If the user selected an entity, we can show:
		 * 
		 * > The persistence annotations on the Entity
		 * > Places where the Entity is constructed
		 * > Entities that this Entity inherits from
		 */
		Q allEntities = stepFrom(edges(Edge.ANNOTATION), typeSelect("javax.persistence", "Entity"));
		Q selectedEntities = selection.intersection(allEntities);
		Q entityAnnotations = edges(Edge.ANNOTATION).forwardStep(selectedEntities);
		Q entityConstructions = edges(Edge.DF_TYPE).betweenStep(selectedEntities,
				index().nodesTaggedWithAny(Node.IS_NEW));
		Q entityInheritance = edges(Edge.SUPERTYPE).between(selectedEntities, allEntities);
		
		/*
		 * The user may select a field of an Entity class. Fields are the database column values.
		 * We can show:
		 * 
		 * > Annotations on the fields, such as @Id (primary key)
		 * > Places where the fields are mutated by the outside world.
		 */
		Q selectedEntityFields = selection.intersection(Common.fieldsOf(allEntities));
		// Transient fields are not persisted
		selectedEntityFields = selectedEntityFields.difference(selectedEntityFields.nodesTaggedWithAny(Node.IS_TRANSIENT), 
				stepFrom(edges(Edge.ANNOTATION), typeSelect("javax.persistence", "Transient")));
		Q entityFieldAnnotations = edges(Edge.ANNOTATION).forwardStep(selectedEntityFields);
		Q entityFieldFlow = edges(Edge.DF_LOCAL, Edge.DF_INTERPROCEDURAL).reverse(selectedEntityFields); 
		
		/*
		 * The user may select a persistence annotation type. We can show:
		 * 
		 * > The annotations themselves
		 * > Types which are annotated with these annotations
		 */
		Q persistenceAnnotations = CommonQueries.packageDeclarations("javax.persistence")
				.nodesTaggedWithAny(Node.ANNOTATION);
		Q selectedPersistenceAnnotations = selection.intersection(persistenceAnnotations);
		Q persistenceAnnotated = edges(Edge.ANNOTATION).reverseStep(selectedPersistenceAnnotations);
		
		/*
		 * The user may select a data flow node which is a reference to a PersistenceUnit. 
		 * These are what the platform uses to inject hooks into persisted
		 * stuff. (You use these to get to persistence contexts). We can show:
		 * 
		 * > The units themselves
		 * > Where a persistence unit is used to construct a persistence context
		 */
		Q allPersistenceUnits = stepFrom(edges(Edge.ANNOTATION), typeSelect("javax.persistence", "PersistenceUnit"));		
		Q selectedPersistenceUnits = selection.intersection(allPersistenceUnits);
	
		Q createEntityManager = methodSelect("javax.persistence","EntityManagerFactory","createEntityManager");
		Q createEMParams = edges(Edge.DECLARES).forwardStep(createEntityManager).
				nodesTaggedWithAny(Node.PARAMETER);
		Q persistenceContextCreation = edges(Edge.DF_LOCAL, Edge.DF_INTERPROCEDURAL).between(selectedPersistenceUnits, createEMParams); 
		
		Q creationSiteEdges = persistenceContextCreation.selectEdge(Edge.CALL_SITE_ID);
		Set<Address> callsites = new HashSet<Address>();
		for(GraphElement edge : creationSiteEdges.eval().edges()) callsites.add((Address) edge.attr().get(Edge.CALL_SITE_ID));
		
		Q createdContexts = Common.empty();
		if(callsites.size() > 0){
			Q ret = edges(Edge.DECLARES).forwardStep(createEntityManager).nodesTaggedWithAny(Node.IS_MASTER_RETURN);
			Address[] arr = new Address[callsites.size()];
			createdContexts = stepTo(edges(Edge.DF_INTERPROCEDURAL).selectEdge(Edge.CALL_SITE_ID, (Object[]) callsites.toArray(arr)),
					ret);
		}
		
		/*
		 * The user may select a data flow node which is a reference to a PersistenceContext. 
		 * These are what the platform uses to inject hooks into persisted
		 * stuff. (You use these to get to persisted entities). We can show:
		 * 
		 * > The contexts themselves
		 * > Where a persistence context is used to make a persistence call
		 */
		Q persistenceContexts = stepFrom(edges(Edge.ANNOTATION), typeSelect("javax.persistence", "PersistenceContext")).union(createdContexts);
		Q selectedPersistenceContexts = selection.intersection(persistenceContexts);
		Q entityManagerMethods = methodsOf(typeSelect("javax.persistence","EntityManager"));
		Q emmThis = edges(Edge.DECLARES).forwardStep(entityManagerMethods).nodesTaggedWithAny(Node.IS_THIS);
		Q persistenceCalls = edges(Edge.DF_LOCAL, Edge.DF_INTERPROCEDURAL).between(selectedPersistenceContexts, emmThis);
		
		/*
		 * The user may select an object reference to a type from the persistence API.
		 * We can show:
		 * 
		 * > The type itself
		 * > References to objects of that type
		 */
		Q persistenceTypes = CommonQueries.packageDeclarations("javax.persistence").nodesTaggedWithAny(Node.TYPE);
		Q selectedPersistenceTypes = selection.intersection(persistenceTypes);
		Q persistenceTypeReferences = edges(Edge.DF_TYPE).forwardStep(selectedPersistenceTypes);		
		
		Q res = entityAnnotations.union(
				entityConstructions,
				entityInheritance,
				entityFieldAnnotations,
				entityFieldFlow,
				persistenceAnnotated,
				selectedPersistenceUnits,
				persistenceContextCreation,
				createdContexts,
				selectedPersistenceContexts,
				persistenceCalls,
				persistenceTypeReferences);
		
		return res;
	}
	
	private Q renderComponents(Q selection){
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
		@SuppressWarnings("serial")
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
		
		return res;
	}
	
	private Q enterpriseBeans(Q selection){
		/* 
		 * The user may have clicked on an EJB annotation. If so, show all the things
		 * with that annotation. We can show:
		 * 
		 * > The annotation
		 * > All classes annotated with this
		 */
		Q ejbAnnotations = CommonQueries.packageDeclarations("javax.ejb").union(
				CommonQueries.packageDeclarations("javax.enterprise")).nodesTaggedWithAny(Node.ANNOTATION);
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
						JEEUtils.getRawStringReferences((String) project.attr().get(Node.NAME), matchRegex, "^a", "XHTML", "xhtml"));
			}
		}
		
		Q res = annotatedWithSelected.union(
				selectedWithAnnotations,
				selectedWithAnnotatedNonJavaReferences,
				selectedFieldClasses,
				selectedFieldClassAnnotations,
				selectedFieldNonJavaReferences);
		
		return res;
	}
	
	@Override
	public String getTitle() {
		return "Java EE";
	}

}
