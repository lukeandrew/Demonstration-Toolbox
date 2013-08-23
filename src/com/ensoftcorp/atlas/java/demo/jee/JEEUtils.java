package com.ensoftcorp.atlas.java.demo.jee;

import static com.ensoftcorp.atlas.java.core.script.Common.index;
import static com.ensoftcorp.atlas.java.core.script.Common.toQ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ensoftcorp.atlas.java.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.java.core.db.view.View;
import com.ensoftcorp.atlas.java.core.index.common.SourceCorrespondence;
import com.ensoftcorp.atlas.java.core.index.common.WorkspaceResourceNotFoundException;
import com.ensoftcorp.atlas.java.core.index.summary.SummaryGraph.SummaryNode;
import com.ensoftcorp.atlas.java.core.query.Attr.Edge;
import com.ensoftcorp.atlas.java.core.query.Attr.Node;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.UniverseManipulator;
import com.ensoftcorp.atlas.java.core.script.UniverseManipulator.Manipulation;

public class JEEUtils {
	public static final String FILE = "FILE";
	public static final String NON_JAVA_STRING_REFERENCE = "NON_JAVA_STRING_REFERENCE";
	
	/**
	 * 
	 * @param projectName
	 * @param toFind
	 * @return
	 */
	@SuppressWarnings("serial")
	public static Q getRawStringReferences(String projectName, String matchRegex, String notMatchRegex, String... fileExtensions){
		// Get all files in the given project with one of the given extensions
		List<File> files = projectFilesMatchingExtension(projectName, fileExtensions);
		
		View v = new View();
		UniverseManipulator um = new UniverseManipulator();
		
		GraphElement decEdge = index().edgesTaggedWithAny(Edge.DECLARES).eval().edges().getFirst();
		
		// For each file
		for(final File file : files){
			// Read in the lines of the file
			List<Line> lines = readLines(file);
			
			// Get the lines which match the regex
			List<Line> matches = linesMatchingRegex(lines, matchRegex, notMatchRegex);
			
			// If at least one line matches the regex, add stuff to the view.
			if(matches.size() > 0){
				final IFile iFile = fileToIFile(file);
				
				Set<String> tags = new HashSet<String>(){{
					add(FILE);
				}};
				
				Map<String, Object> attributes;
				SourceCorrespondence sc = null;
				try {
					sc = SourceCorrespondence.fromString("0", Long.toString(file.length()), iFile);
				} catch (WorkspaceResourceNotFoundException e) {}
				final SourceCorrespondence sc2 = sc;
				attributes = new HashMap<String, Object>(){{
					put(Node.NAME, file.getName().trim());
					put(SummaryNode.SC_RANGE, sc2);
				}};
				
				Manipulation fileNode = um.createNode(tags, attributes);
				um.addToView(fileNode, v);
				
				tags = new HashSet<String>(){{
					add(NON_JAVA_STRING_REFERENCE);
				}};
				for(final Line line : matches){
					try {
						sc = SourceCorrespondence.fromString(Integer.toString(line.getStartOffset()), 
															 Integer.toString(line.getEndOffset()),
															 iFile);
					} catch (WorkspaceResourceNotFoundException e) {}
					
					final SourceCorrespondence sc3 = sc;
					attributes = new HashMap<String, Object>(){{
						put(Node.NAME, line.getLine().trim());
						put(SummaryNode.SC_RANGE, sc3);
					}};
					
					Manipulation lineNode = um.createNode(tags, attributes);
					Manipulation lineDeclaration = um.createEdge(decEdge, fileNode, lineNode);
					um.addToView(lineNode, v);
					um.addToView(lineDeclaration, v);
				}
			}
		}

		um.perform();
		return toQ(v);
	}
	
	public static Map<String, String> getComponentTagMapping(Q projects){
		Map<String, String> componentToTag = new HashMap<String, String>();
		
		final String COMPONENT_TYPE = "component-type";
		final String TAG_NAME = "tag-name";
		
		for(GraphElement project : projects.nodesTaggedWithAny(Node.PROJECT).eval().nodes()){
			String projectName = (String) project.attr().get(Node.NAME);
			
			// Get the component nodes from the taglib XML files
			List<File> taglibFiles = projectFilesMatchingExtension(projectName, "xml","XML");
			List<org.w3c.dom.Node> componentTypeNodes = XMLNodesByName(taglibFiles, COMPONENT_TYPE);
			
			for(org.w3c.dom.Node componentType : componentTypeNodes){
				try{
					org.w3c.dom.Node component = componentType.getParentNode();
					org.w3c.dom.Node tag = component.getParentNode();
					NodeList tagChildren = tag.getChildNodes();
					for(int i = 0; i < tagChildren.getLength(); i++){
						org.w3c.dom.Node tagName = tagChildren.item(i);
						
						if(tagName.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && 
						   TAG_NAME.equalsIgnoreCase(tagName.getNodeName())){
							String componentTypeValue = componentType.getTextContent();
							String tagNameValue = tagName.getTextContent();
							componentToTag.put(componentTypeValue, tagNameValue);
							break;
						}
					}
				} catch(Exception e){}
			}
		}
		
		return componentToTag;
	}
	
	private static IFile fileToIFile(File file){
		IWorkspace workspace = ResourcesPlugin.getWorkspace();    
		IPath location = Path.fromOSString(file.getAbsolutePath()); 
		IFile iFile = workspace.getRoot().getFileForLocation(location);
		return iFile;
	}
	
	private static List<Line> linesMatchingRegex(List<Line> lines, String matchRegex, String notMatchRegex){
		List<Line> matches = new LinkedList<Line>();
		
		for(Line line : lines){
			if(line.getLine().matches(matchRegex) && 
			   !line.getLine().matches(notMatchRegex)) matches.add(line);
		}
		
		return matches;
	}
	
	private static List<Line> readLines(File f){
		List<Line> lines = new LinkedList<Line>();

		int startOffset = 0;
		int endOffset = 0;
		BufferedReader bfr = null;
		try {
			bfr = new BufferedReader(new FileReader(f));
			String line;
			while((line = bfr.readLine()) != null) {
				endOffset = startOffset + line.length();
				lines.add(new Line(line, startOffset, endOffset)); 
				startOffset = endOffset + 1;
			}

		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}
		finally{
			if(bfr != null)
				try {
					bfr.close();
				} catch (IOException e) {}
		}
		
		return lines;
	}

	private static List<org.w3c.dom.Node> XMLNodesByName(List<File> xmlFiles, String name){
		List<org.w3c.dom.Node> nodeList = new LinkedList<org.w3c.dom.Node>();
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			for(File f : xmlFiles){
				try {
					Document document = docBuilder.parse(f);
					NodeList nl = document.getElementsByTagName(name);
					for(int i=0; i < nl.getLength(); i++) nodeList.add(nl.item(i));
				} 
				catch (SAXException e) {} 
				catch (IOException e) {}
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return nodeList;
	}
	
	private static List<File> projectFilesMatchingExtension(String projectName, String... fileExtensions){
		List<File> res = new LinkedList<File>();
		
		Set<String> extensionSet = new HashSet<String>(fileExtensions.length * 2);
		for(String extension : fileExtensions) extensionSet.add(extension);
		
		// If the project has a src folder, use that. Else use the project root.
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		File root = project.getLocation().toFile();
		for(File child : root.listFiles()){
			if(child.isDirectory() && child.getName().equalsIgnoreCase("src")){
				root = child;
				break;
			}
		}
		
		projectFilesMatchingExtension(root, res, extensionSet);
		
		return res;
	}
	
	private static void projectFilesMatchingExtension(File current, List<File> list, Set<String> fileExtensions){
		if(current.isDirectory()){
			File[] containedFiles = current.listFiles();
			for(File f : containedFiles) projectFilesMatchingExtension(f, list, fileExtensions);
		} else if (current.exists()){
			String[] split = current.getName().split("\\.");
			if(split.length > 0 && fileExtensions.contains(split[split.length - 1])){
				list.add(current);
			}
		}
	}
	
	private static class Line{
		private String line;
		private int startOffset, endOffset;
		public Line(String line, int startOffset, int endOffset) {
			super();
			this.line = line;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}
		public String getLine() {
			return line;
		}
		public int getStartOffset() {
			return startOffset;
		}
		public int getEndOffset() {
			return endOffset;
		}
	}
}
