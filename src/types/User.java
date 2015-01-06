package types;

import gnu.trove.*;

import java.io.*;

public class User extends IDocument {
	private static final long serialVersionUID = 1;
	
	public int numTweets = 0;
	public int numWords = 0;
	public int numCitations = 0;
	
	public int[][] wordSequence = null; //sentence word
	public int[] citationSequence = null;
	
	public TIntIntHashMap wordCounts = null; 
	public TIntIntHashMap citationCounts = null;
	
	public User() {
	}
	
	public void addWords(String[][] tweets, Alphabet wordAlphabet) {
		if(tweets == null || tweets.length == 0) {
			throw new IllegalArgumentException("Words cannot be empty");
		}

		numTweets = tweets.length;
		wordSequence = new int[numTweets][];
		wordCounts = new TIntIntHashMap();
		for(int t = 0; t < numTweets; t++) {
			String[] words = tweets[t];
			numWords += words.length;
			
			wordSequence[t] = new int[words.length];
			for(int pos = 0; pos < words.length; pos++){
				int index = wordAlphabet.lookupIndex(words[pos], true);
				wordSequence[t][pos] = index;
				wordCounts.adjustOrPutValue(index, 1, 1);
			}
		}
		docLen += numWords;
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

	public void addContent(String[][] tweets, String[] citations, Alphabet wordAlphabet, Alphabet citationAlphabet) {
		if(tweets != null && tweets.length != 0) {
			addWords(tweets, wordAlphabet);
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
		out.writeInt(numTweets);
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
		numTweets = in.readInt();
		numWords = in.readInt();
		numCitations = in.readInt();
		
		wordSequence = (int[][]) in.readObject();
		citationSequence = (int[]) in.readObject();
		
		wordCounts = (TIntIntHashMap) in.readObject();
		citationCounts = (TIntIntHashMap) in.readObject();
	}

}
