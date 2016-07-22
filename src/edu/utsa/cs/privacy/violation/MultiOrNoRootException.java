package edu.utsa.cs.privacy.violation;

public class MultiOrNoRootException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String message;
    public MultiOrNoRootException(String str) {
	this.message = str;
    }
    public String getMessage(){
	return this.message;
    }


}
