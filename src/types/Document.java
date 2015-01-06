package types;

import gnu.trove.*;

import java.io.*;

public class Document extends IDocument {
	private static final long serialVersionUID = 1;
	
	public int numWords = 0;
	public int numCitations = 0;
	
	public int[] wordSequence = null; 
	public int[] citationSequence = null;
	
	public TIntIntHashMap wordCounts = null; 
	public TIntIntHashMap citationCounts = null;
	
	public Document() {
	}
	
	public void addWords(String[] words, Alphabet wordAlphabet) {
		if(words == null || words.length == 0) {
			throw new IllegalArgumentException("Words cannot be empty");
		}

		numWords = words.length;
		docLen += numWords;
		
		wordSequence = new int[numWords];
		wordCounts = new TIntIntHashMap();
		
		for(int pos = 0; pos < words.length; pos++){
			int index = wordAlphabet.lookupIndex(words[pos], true);
			wordSequence[pos] = index;
			wordCounts.adjustOrPutValue(index, 1, 1);
		}
	}
	
	public void addCitations(String[] citations, Alphabet citationAlphabet) {
		if(citations == null || citations.length == 0) {
			throw new IllegalArgumentException("Citations cannot be empty");
		}
		
		numCitations = citations.length;
		docLen += numCitations;
		
		citationSequence = new int[numCitations];
		citationCounts = new TIntIntHashMap();
		
		for(int pos = 0; pos < citations.length; pos++){
			int index = citationAlphabet.lookupIndex(citations[pos], true);
			citationSequence[pos] = index;
			citationCounts.adjustOrPutValue(index, 1, 1);
		}
	}

	public void addContent(String[] words, String[] citations, Alphabet wordAlphabet, Alphabet citationAlphabet) {
		if(words != null && words.length != 0) {
			addWords(words, wordAlphabet);
		}
		
		if(citations != null && citations.length != 0) {
			addCitations(citations, citationAlphabet);
		}
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeLong(serialVersionUID);
		
		out.writeObject(docName);
		out.writeInt(docID);
		out.writeInt(docLabel);
		
		out.writeInt(docLen);
		out.writeInt(numWords);
		out.writeInt(numCitations);
		
		out.writeObject(wordSequence);
		out.writeObject(citationSequence);
		
		out.writeObject(wordCounts);
		out.writeObject(citationCounts);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		long version = in.readLong();
		if(version < serialVersionUID) throw new RuntimeException("Serial version is out of date");
		
		docName = (String) in.readObject();
		docID = in.readInt();
		docLabel = in.readInt();
		
		docLen = in.readInt();
		numWords = in.readInt();
		numCitations = in.readInt();
		
		wordSequence = (int[]) in.readObject();
		citationSequence = (int[]) in.readObject();
		
		wordCounts = (TIntIntHashMap) in.readObject();
		citationCounts = (TIntIntHashMap) in.readObject();
	}

}
