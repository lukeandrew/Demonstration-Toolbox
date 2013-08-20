package com.ensoftcorp.atlas.java.demo.jee.selection;

import static com.ensoftcorp.atlas.java.core.script.Common.edges;
import static com.ensoftcorp.atlas.java.core.script.Common.index;
import static com.ensoftcorp.atlas.java.core.script.Common.methodSelect;
import static com.ensoftcorp.atlas.java.core.script.Common.methodsOf;
import static com.ensoftcorp.atlas.java.core.script.Common.stepFrom;
import static com.ensoftcorp.atlas.java.core.script.Common.stepTo;
import static com.ensoftcorp.atlas.java.core.script.Common.typeSelect;

import java.util.HashSet;
import java.util.Set;

import com.ensoftcorp.atlas.java.core.db.graph.Address;
import com.ensoftcorp.atlas.java.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.java.core.query.Attr.Edge;
import com.ensoftcorp.atlas.java.core.query.Attr.Node;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.Common;
import com.ensoftcorp.atlas.java.core.script.CommonQueries;
import com.ensoftcorp.atlas.java.core.script.CommonQueries.TraversalDirection;
import com.ensoftcorp.atlas.java.core.script.StyledResult;
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
public class JEEPersistence implements SelectionDetailScript{

	@Override
	public String[] getSupportedNodeTags() {
		return new String[]{Node.DATA_FLOW, Node.FIELD, Node.CLASS, Node.ANNOTATION};
	}

	@Override
	public String[] getSupportedEdgeTags() {
		return null;
	}

	@Override
	public StyledResult selectionChanged(SelectionInput input) {
		Q selection = input.getInterpretedSelection();
		
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
		Q entities = selection.intersection(allEntities);
		Q entityAnnotations = edges(Edge.ANNOTATION).forwardStep(entities);
		Q entityConstructions = edges(Edge.DF_TYPE).betweenStep(entities,
				index().nodesTaggedWithAny(Node.IS_NEW));
		Q entityInheritance = edges(Edge.SUPERTYPE).between(entities, allEntities);
		
		/*
		 * The user may select a field of an Entity class. Fields are the database column values.
		 * We can show:
		 * 
		 * > Annotations on the fields, such as @Id (primary key)
		 * > Places where the fields are mutated by the outside world.
		 */
		Q entityFields = selection.intersection(Common.fieldsOf(allEntities));
		// Transient fields are not persisted
		entityFields = entityFields.difference(entityFields.nodesTaggedWithAny(Node.IS_TRANSIENT), 
				stepFrom(edges(Edge.ANNOTATION), typeSelect("javax.persistence", "Transient")));
		Q entityFieldAnnotations = edges(Edge.ANNOTATION).forwardStep(entityFields);
		Q entityFieldFlow = edges(Edge.DF_LOCAL, Edge.DF_INTERPROCEDURAL).reverse(entityFields); 
		
		/*
		 * The user may select a persistence annotation type. We can show:
		 * 
		 * > The annotations themselves
		 * > Types which are annotated with these annotations
		 */
		Q persistenceAnnotations = selection.intersection(
				CommonQueries.packageStructure("javax.persistence", TraversalDirection.FORWARD).nodesTaggedWithAny(Node.ANNOTATION));
		Q persistenceAnnotated = edges(Edge.ANNOTATION).reverseStep(persistenceAnnotations);
		
		/*
		 * The user may select a data flow node which is a reference to a PersistenceUnit. 
		 * These are what the platform uses to inject hooks into persisted
		 * stuff. (You use these to get to persistence contexts). We can show:
		 * 
		 * > The units themselves
		 * > Where a persistence unit is used to construct a persistence context
		 */
		Q allPersistenceUnits = stepFrom(edges(Edge.ANNOTATION), typeSelect("javax.persistence", "PersistenceUnit"));		
		Q persistenceUnits = selection.intersection(allPersistenceUnits);
	
		Q createEntityManager = methodSelect("javax.persistence","EntityManagerFactory","createEntityManager");
		Q createEMParams = edges(Edge.DECLARES).forwardStep(createEntityManager).
				nodesTaggedWithAny(Node.PARAMETER);
		Q persistenceContextCreation = edges(Edge.DF_LOCAL, Edge.DF_INTERPROCEDURAL).between(persistenceUnits, createEMParams); 
		
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
		Q persistenceContexts = selection.intersection(
				stepFrom(edges(Edge.ANNOTATION), typeSelect("javax.persistence", "PersistenceContext")).union(createdContexts));
		Q entityManagerMethods = methodsOf(typeSelect("javax.persistence","EntityManager"));
		Q emmThis = edges(Edge.DECLARES).forwardStep(entityManagerMethods).nodesTaggedWithAny(Node.IS_THIS);
		Q persistenceCalls = edges(Edge.DF_LOCAL, Edge.DF_INTERPROCEDURAL).between(persistenceContexts, emmThis);
		
		/*
		 * The user may select an object reference to a type from the persistence API.
		 * We can show:
		 * 
		 * > The type itself
		 * > References to objects of that type
		 */
		Q persistenceTypes = selection.intersection(
				CommonQueries.packageStructure("javax.persistence", TraversalDirection.FORWARD).nodesTaggedWithAny(Node.TYPE));
		Q persistenceTypeReferences = edges(Edge.DF_TYPE).forwardStep(persistenceTypes);		
		
		Q res = entities.union(
				entityAnnotations,
				entityConstructions,
				entityInheritance,
				entityFields,
				entityFieldAnnotations,
				entityFieldFlow,
				persistenceAnnotations,
				persistenceAnnotated,
				persistenceUnits,
				persistenceContextCreation,
				createdContexts,
				persistenceContexts,
				persistenceCalls,
				persistenceTypes,
				persistenceTypeReferences);
		
		return new StyledResult(res);
	}

	@Override
	public String getTitle() {
		return "JEE Persistence";
	}

}
