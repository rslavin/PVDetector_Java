package edu.utsa.cs.privacy.violation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * The main class for detecting violations
 * @author sean
 *
 */
public class ViolationDetector {
    //API map from phrase to APIs
    private Hashtable<String, Set<String>> APIMap = new Hashtable<String, Set<String>>();
    //inverse API map from APIs to Phrases
    private Hashtable<String, Set<String>> invertAPIMap = new Hashtable<String, Set<String>>();
    private PhraseTree ontology;
//    private Set<String> phraseSet;
//    private Set<String> APIFullSet;

    public PhraseTree getOntology() {
	return this.ontology;
    }
    /**
     * Load API map from the file at the path
     * @param path
     * @throws IOException
     */
    private void loadAPIMap(String path) throws IOException {
	BufferedReader in = new BufferedReader(new FileReader(path));
	for (String line = in.readLine(); line != null; line = in.readLine()) {
	    int index = line.indexOf("<");
	    String phrase = line.substring(0, index).trim();
	    String api = line.substring(index, line.indexOf(">", index) + 1);

	    Set<String> apiset = this.APIMap.get(phrase);
	    if (apiset == null) {
		apiset = new HashSet<String>();
		this.APIMap.put(phrase, apiset);
	    }
	    apiset.add(api);
	    
	    Set<String> phraseSet = this.invertAPIMap.get(api);
	    if (phraseSet == null) {
		phraseSet = new HashSet<String>();
		this.invertAPIMap.put(api, phraseSet);
	    }
	    if(phrase.length() > 0){
		phraseSet.add(phrase);
	    }
	}
	in.close();
    }

    
    public ViolationDetector(String mapPath, String treePath)
	    throws IOException, MultiOrNoRootException {
	loadAPIMap(mapPath);
	this.ontology = PhraseTree.createTree(treePath);
    }


    /**
     * load the result of flowdroid to find all API methods whose return values are 
     * sent to the network
     * @param apiLog path to the apilog file
     * @return a set API methods
     * @throws IOException
     */
    private Set<String> loadAPI(String apiLog) throws IOException {
	Set<String> apiset = new HashSet<String>();
	BufferedReader in = new BufferedReader(new FileReader(apiLog));
	for (String line = in.readLine(); line != null; line = in.readLine()) {
	    int index = line.indexOf(">");
	    String api = line.substring(0, index + 1);
	    apiset.add(api);
	}
	in.close();
	return apiset;
    }

    /**
     * return a list of privacy-related phrases in the policyPath
     * @param policyPath path to the policy file
     * @return
     * @throws IOException
     */
    private Set<String> search(String policyPath) throws IOException {
	Set<String> phrases = new HashSet<String>();
	BufferedReader in = new BufferedReader(new FileReader(policyPath));
	for(String line = in.readLine(); line!=null; line = in.readLine()){
	    phrases.add(line.trim());
	}
	in.close();
	return phrases;
    }

    /**
     * Detection privacy violations
     * @param policyPath path to the policy file
     * @param apiLog path to the result file of flowdroid
     * @param name  name of the apk file
     * @param excludeRoot root node is ignored for detecting implicit violations
     * @return a list of violations
     * @throws IOException
     */
    public List<Violation> detect(String policyPath, String apiLog,
	    String name, boolean excludeRoot) throws IOException {
	
	List<Violation> violations = new ArrayList<Violation>();
	Set<String> phrases = search(policyPath);
	Set<String> APIs = loadAPI(apiLog);
	
	
	for(String api : APIs){
	    //needPhrases are all phrases directly mapped to an API
	    Set<String> needPhrases = this.invertAPIMap.get(api);
	    if(needPhrases == null || needPhrases.size() == 0){
		;
	    }else{
		//mappedPhrases are all phrases which are synonyms of any need phrase
		Set<String> mappedPhrases = new HashSet<String>();
		//superPhrases are all phrases which are super phrases of any need phrase
		Set<String> superPhrases = new HashSet<String>();

		for(String phrase : needPhrases){
		    PhraseTree node = this.ontology.searchPhrase(phrase);
		    mappedPhrases.add(node.getValue());
		    mappedPhrases.addAll(node.getSynonyms());
		    superPhrases.addAll(node.getSuperPhrases(true, false));
		}
		
		//retain only phrases existing in the policy
		mappedPhrases.retainAll(phrases);
		superPhrases.retainAll(phrases);

		
		if(mappedPhrases.isEmpty()){
		    //A violation is detected if no mapped phrase found in the policy
		    if(superPhrases.isEmpty()){
			//An explicit violation is detected if no super phrases of any need phrase found in the policy
			violations.add(new Violation(name, api, ViolationType.EXPLICIT, needPhrases, null));
		    }else{
			violations.add(new Violation(name, api, ViolationType.IMPLICIT, needPhrases, superPhrases));
		    }
		}
	    }
	}
	return violations;
    }
}
