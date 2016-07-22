package edu.utsa.cs.privacy.violation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import edu.utsa.cs.privacy.violation.relations.EquivalenceRelation;
import edu.utsa.cs.privacy.violation.relations.SubClassRelation;

/**
 * This class present the internal data structure for the ontology
 * @author sean
 *
 */

public class PhraseTree {
    private Set<PhraseTree> children;
    private String value; // The phrase on the tree node
    private Set<String> synonyms; //phrase synonyms stored on the tree node
    private Set<PhraseTree> parents; //There can be multiple parents

    public PhraseTree(String val) {
	this.setValue(val);
	this.children = new HashSet<PhraseTree>();
	this.synonyms = new HashSet<String>();
	this.parents = new HashSet<PhraseTree>();
    }

    public String toString(){
	return this.value + this.synonyms ;
	
    }
    public boolean equals(Object obj){
	if(obj instanceof PhraseTree){
	    PhraseTree pt = (PhraseTree)obj;
	    return pt.value.equals(this.value);
	}
	return false;
    }

    public void addSynonyms(String syn) {
	this.synonyms.add(syn);
    }
    
    public Set<String> getSynonyms(){
	return this.synonyms;
    }

    public void addChild(PhraseTree node) {
	this.children.add(node);
	node.parents.add(this);
    }

    public Set<PhraseTree> getParents() {
	return this.parents;
    }

    /**
     * The method to create a phrease tree from an owl file
     * @param path
     * @return
     * @throws IOException
     * @throws MultiOrNoRootException
     */
    public static PhraseTree createTree(String path) throws IOException, MultiOrNoRootException {

	//read all relations from the owl file
	List<EquivalenceRelation> equivRelations = new ArrayList<EquivalenceRelation>();
	List<SubClassRelation> subRelations = new ArrayList<SubClassRelation>();
	
	BufferedReader in = new BufferedReader(new FileReader(path));
	for (String line = in.readLine(); line != null; line = in.readLine()) {
	    if(line.startsWith("%")){continue;}
	    if (line.trim().startsWith("<EquivalentClasses>")) {
		String first = fetchClassName(in.readLine());
		String secondLine = in.readLine();
		if (!secondLine.trim().startsWith("<Object")) {
		    String second = fetchClassName(secondLine);
		    equivRelations.add(new EquivalenceRelation(first, second));
		}
	    }
	    if (line.trim().startsWith("<SubClassOf>")) {
		String first = fetchClassName(in.readLine());
		String secondLine = in.readLine();
		if (!secondLine.trim().startsWith("<Object")) {
		    String second = fetchClassName(secondLine);
		    subRelations.add(new SubClassRelation(first, second));
		}
	    }
	}
	in.close();

	
	// merge all equivalent phrases to one node
	//equivtTable stores the map from a phrase to a tree node
	Hashtable<String, PhraseTree> equivTable = new Hashtable<String, PhraseTree>();
	for(EquivalenceRelation equiv : equivRelations){
	    String first = equiv.getFirst();
	    String second = equiv.getSecond();
	    PhraseTree firstTree = equivTable.get(first);
	    PhraseTree secondTree = equivTable.get(second);
	    if(firstTree == null && secondTree !=null){	
		//add first phrase to second if second exists
		secondTree.addSynonyms(first);
		equivTable.put(first, secondTree);
	    }else if(firstTree != null && secondTree ==null){ 
		//add second phrase to first if first exists
		firstTree.addSynonyms(second);
		equivTable.put(second, firstTree);
	    }else if(firstTree == null && secondTree == null){
		//add a new tree node if neither exist
		PhraseTree t = new PhraseTree(first);
		t.addSynonyms(second);
		equivTable.put(first, t);
		equivTable.put(second, t);
	    }else if(firstTree != null && secondTree != null){
		//merge to the first tree node, if both exist
		firstTree.getSynonyms().add(second);
		firstTree.getSynonyms().addAll(secondTree.getSynonyms());
		equivTable.put(second, firstTree);
		//update the record of phrases in the second tree node
		for(String syn : secondTree.getSynonyms()){
		    equivTable.put(syn, firstTree);
		}
	    }
	}
	
	for(SubClassRelation sub : subRelations){
	    String first = sub.getFirst();
	    String second = sub.getSecond();
	    PhraseTree firstTree = equivTable.get(first);
	    PhraseTree secondTree = equivTable.get(second);
	    if(firstTree == null){
		firstTree = new PhraseTree(first);
		equivTable.put(first, firstTree);
	    }
	    if(secondTree == null){
		secondTree = new PhraseTree(second);
		equivTable.put(second, secondTree);
	    }
	    //if two nodes are not the same node, add the sub class relationship
	    if(secondTree!=firstTree){
		secondTree.addChild(firstTree);
		firstTree.getParents().add(secondTree);
	    }
	}
	//return the root of the tree
	return findRoot(equivTable);
    }

    /**
     * Find the root of a tree from the equivTable, just return the nodes having zero parents
     * @param equivTable
     * @return
     * @throws MultiOrNoRootException
     */
    private static PhraseTree findRoot(Hashtable<String, PhraseTree> equivTable) throws MultiOrNoRootException {
	List<PhraseTree> roots = new ArrayList<PhraseTree>();
	for(String key : equivTable.keySet()){
	    PhraseTree t = equivTable.get(key);
	    if(t.getParents().size()==0){
		roots.add(t);
	    }
	}
	if(roots.size()!=1){
	    throw new MultiOrNoRootException("Ontology Tree has multiple or no roots, number of roots is " + roots.size() + ":" + roots);
	}
	return roots.get(0);
    }
    
    /**
     * Find the class name from a line in owl
     * @param line
     * @return
     */
    private static String fetchClassName(String line) {
	int index = line.indexOf("#");
	
	if(index==-1){
	    return "owl:Thing";
	}

	String className = line.substring(index + 1,
		line.indexOf("\"", index + 1)).toLowerCase();
	return className;
    }

    public String getValue() {
	return value;
    }

    public void setValue(String value) {
	this.value = value;
    }

    /**
     * Search the tree node with a query phrase
     * @param query a phrase as query
     * @return the tree node corresponds to the query
     */
    public PhraseTree searchPhrase(String query) {
	if (this.value.equals(query) || this.synonyms.contains(query)) {
	    return this;
	} else {
	    for (PhraseTree child : this.children) {
		PhraseTree n = child.searchPhrase(query);
		if (n != null) {
		    return n;
		}
	    }
	}
	return null;
    }

    public boolean isLeaf() {
	return this.children.size() == 0;
    }
    
    /**
     * Get all super phrases of the current tree node
     * @param excludeRoot true if root node is excluded 
     * @param onlyMain true if synonyms are ignored
     * @return
     */
    public Set<String> getSuperPhrases(boolean excludeRoot, boolean onlyMain){
	Set<String> res = new HashSet<String>();
	if(excludeRoot && (this.value.equals("information") || this.value.equals("event") || this.value.equals("technology")|| this.value.equals("owl:Thing"))){
	    return res;
	}
	res.add(this.value);
	if(!onlyMain){
	    res.addAll(this.synonyms);
	}
	for (PhraseTree parent : this.parents) {
	    Set<String> supres = parent.getSuperPhrases(excludeRoot, onlyMain);
	    res.addAll(supres);
	}
	return res;
    }
    
    /**
     * Get all super phrases of the current tree node
     * @param onlyLeaf true if only leaf nodes are included
     * @param onlyMain true if synonyms are ignored
     * @return
     */
    public Set<String> getSubPhrases(boolean onlyLeaf, boolean onlyMain) {
	Set<String> res = new HashSet<String>();
	if (onlyLeaf) {
	    if (this.isLeaf()) {
		res.add(this.value);
	    }
	} else {
	    res.add(this.value);
	}
	if (!onlyMain) {
	    res.addAll(this.synonyms);
	}
	for (PhraseTree child : this.children) {
	    Set<String> subres = child.getSubPhrases(onlyLeaf, onlyMain);
	    res.addAll(subres);
	}
	return res;
    }
    
    /**
     * return all the sub phrase tree nodes of the node, including itself
     * @return
     */
    public Set<PhraseTree> fetchAllPhraseTrees() {
	Set<PhraseTree> ret = new HashSet<PhraseTree>();
	for(PhraseTree child : this.children){
	    if(!ret.contains(child)){
		ret.addAll(child.fetchAllPhraseTrees());
	    }
	}
	ret.add(this);
	return ret;
    }
    
    /**
     * return all the sub phrases of the node, including itself, same with getSubPhrases(false, false)
     * @return
     */
    public Set<String> fetchAllPhrases(){
	Set<String> ret = new HashSet<String>();
	Set<PhraseTree> pts = this.fetchAllPhraseTrees();
	for(PhraseTree pt : pts){
	    ret.addAll(pt.getSynonyms());
	    ret.add(pt.getValue());
	}
	return ret;
    }
}
