package edu.utsa.cs.privacy.violation;

import java.util.Set;

/**
 * The class that represents a privacy violation
 * @author sean
 *
 */
public class Violation {
    private String api;
    private Set<String> missingPhrases;
    public Set<String> getMissingPhrases() {
        return missingPhrases;
    }

    public void setMissingPhrases(Set<String> missingPhrases) {
        this.missingPhrases = missingPhrases;
    }

    private Set<String> abstractPhrase;
    private String appName;
    private ViolationType type;
    
    public Violation(String appName, String api, ViolationType type, Set<String> missingPhrases, Set<String> superPhrases) {
	this.api = api;
	this.appName = appName;
	this.type = type;
	this.abstractPhrase = superPhrases;
	this.missingPhrases = missingPhrases;
    }
    
    public int hashCode(){
	return (this.api + this.appName).hashCode();
    }
    public boolean equals(Object obj){
	if(obj instanceof Violation){
	    Violation other = (Violation)obj;
	    return other.api.equals(this.api) && other.appName.equals(this.appName);
	}
	return false;
    }

    public String toString(){
	if(this.type == ViolationType.EXPLICIT){
	    return this.type + "\t" + this.api + "\tMissing" + this.missingPhrases;
	}else{
	    return this.type + "\t" + this.api + "\tMissing:" + this.missingPhrases + "\tGiven:" + this.abstractPhrase;
	}
    }
    
    public String toStringWithName(){
	if(this.type == ViolationType.EXPLICIT){
	    return this.appName + "\t" + this.type + "\t" + this.api + "\tMissing" + this.missingPhrases + "\tGiven:[]";
	}else{
	    return this.appName + "\t" + this.type + "\t" + this.api + "\tMissing:" + this.missingPhrases + "\tGiven:" + this.abstractPhrase;
	}
    }

    
    public ViolationType getType() {
	return type;
    }

    public String getApi() {
	return api;
    }

    public String getAppName() {
	return appName;
    }
}
