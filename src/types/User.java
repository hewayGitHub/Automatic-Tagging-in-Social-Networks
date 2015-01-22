package types;

import gnu.trove.*;

import java.io.*;
import java.util.Arrays;

public class User extends IDocument {
	private static final long serialVersionUID = 1;
	
	public int numTweets = 0;
	
	public int[][] wordSequence = null; //sentence word
	public int[] citationSequence = null;
	
	public User() {
	}
	
	public void addWords(String[] tweets, Alphabet wordAlphabet, boolean addIfNotPresent) {
		if(tweets == null || tweets.length == 0) {
			throw new IllegalArgumentException("Words cannot be empty");
		}
		
		if(numWords != 0) {
			throw new RuntimeException("addWords() has been called more than once for one document.");
		}
		
		wordSequence = new int[tweets.length][];
		wordCounts = new TIntIntHashMap();
		for(int t = 0; t < tweets.length; t++) {
			String[] words = tweets[t].split(" ");
			
			int[] tempTweet = new int[words.length];
			int tempNumWords = 0;
			for(int pos = 0; pos < words.length; pos++){
				if(OrdinaryWord.isOrdinaryWord(words[pos])) continue;
				
				int index = wordAlphabet.lookupIndex(words[pos], addIfNotPresent);
				
				if(index != -1) {
					tempTweet[tempNumWords++] = index;
					wordCounts.adjustOrPutValue(index, 1, 1);
				}
			}
			
			if(tempNumWords != 0) {
				numWords += tempNumWords;
				
				if(tempNumWords != words.length) tempTweet = Arrays.copyOf(tempTweet, tempNumWords);
				wordSequence[numTweets++] = tempTweet;
			}
		}
		
		docLen += numWords;
		if(numTweets != tweets.length) {
			wordSequence = Arrays.copyOf(wordSequence, numTweets);
		}
	}
	
	public void addCitations(String[] citations, Alphabet citationAlphabet, boolean addIfNotPresent) {
		if(citations == null || citations.length == 0) {
			throw new IllegalArgumentException("Citations cannot be empty");
		}
		
		if(numCitations != 0) {
			throw new RuntimeException("addCitations() has been called more than once for one document.");
		}
		
		citationSequence = new int[citations.length];
		citationCounts = new TIntIntHashMap();
		
		for(int pos = 0; pos < citations.length; pos++){
			int index = citationAlphabet.lookupIndex(citations[pos], addIfNotPresent);
			
			if(index != -1) {
				citationSequence[numCitations++] = index;
				citationCounts.adjustOrPutValue(index, 1, 1);
			}
		}
		
		docLen += numCitations;
		if(numCitations != citations.length) {
			citationSequence = Arrays.copyOf(citationSequence, numCitations);
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
