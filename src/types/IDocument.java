package types;

import gnu.trove.TIntIntHashMap;

import java.io.Serializable;

public abstract class IDocument implements Serializable{
	private static final long serialVersionUID = 4672583436767162793L;
	
	public String docName;  //doc name should be unique
	public int docID;
	public int docLabel;
	
	public int numWords = 0;
	public int numCitations = 0;
	
	public TIntIntHashMap wordCounts = null; 
	public TIntIntHashMap citationCounts = null;
	
	abstract public void addWords(String[] words, Alphabet wordAlphabet, boolean addIfNotPresent);
	abstract public void addCitations(String[] citations, Alphabet citationAlphabet, boolean addIfNotPresent);
	
	public void addContent(String[] words, String[] citations, 
			Alphabet wordAlphabet, Alphabet citationAlphabet, boolean addIfNotPresent) {
		if(words != null && words.length != 0) {
			addWords(words, wordAlphabet, addIfNotPresent);
		}
		
		if(citations != null && citations.length != 0) {
			addCitations(citations, citationAlphabet, addIfNotPresent);
		}
	}
	
	public int docLen;
	
	public int getDocLen() {
		return docLen;
	}
	public void setDocLen(int docLen) {
		this.docLen = docLen;
	}
	public String getDocName() {
		return docName;
	}
	public void setDocName(String docName) {
		this.docName = docName;
	}
	public int getDocID() {
		return docID;
	}
	public void setDocID(int docID) {
		this.docID = docID;
	}
	public int getDocLabel() {
		return docLabel;
	}
	public void setDocLabel(int docLabel) {
		this.docLabel = docLabel;
	}
}
