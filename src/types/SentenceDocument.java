package types;

import gnu.trove.*;
import java.io.*;

public class SentenceDocument implements Serializable {
	public String docName; // should be unique
	public int docID;
	public int docLabel;
	public int numWords;
	public int numCitations;
	
	public int numSentences;
	public int[] sentenceEndPos;
	
	// sequence representation of words and citations in the document
	public FeatureSequence wordSequence = null; 
	public FeatureSequence citationSequence = null;
	
	// bag representation of words and citations in document
	public TIntIntHashMap wordCounts = null; 
	public TIntIntHashMap citationCounts = null;
	
	// sequence representation of topic assignments
	public int[] wordTopicAssignment;
	public int[] citationTopicAssignment;
	public LabelSequence wordTopicSequence = null;
	public LabelSequence citationTopicSequence = null;
	
	public TIntIntHashMap topicCounts = null;
	
	public SentenceDocument() {
	}
	
	public void addContent(String name, int label, String[][] sentences) {
		if(sentences == null || sentences.length == 0) {
			throw new IllegalArgumentException("addContent: sentences cannot be empty");
		}
		
		docName = name;
		docLabel = label;
		numSentences = sentences.length;
		sentenceEndPos = new int[numSentences];
		
		wordSequence = new FeatureSequence(Corpus.vocabulary, numWords);
		wordCounts = new TIntIntHashMap();
		
		int senIndex = 0;
		for(String[] words: sentences) {
			numWords += words.length;
			sentenceEndPos[senIndex++] = words.length;
			for (String word: words) {
				int index = Corpus.vocabulary.lookupIndex(word, true);
				wordSequence.add(index);
				wordCounts.adjustOrPutValue(index, 1, 1);
			}
		}
	}
	
	public void addCitations(String name, int label, String[] citations) {
		if(citations == null || citations.length == 0) {
			throw new IllegalArgumentException("addCitaitons: citations cannot be empty");
		}
		
		docName = name;
		docLabel = label;
		numCitations = citations.length;
		
		citationSequence = new FeatureSequence(Corpus.citationAlphabet, numCitations);
		citationCounts = new TIntIntHashMap();
		
		for (String citation: citations) {
			int index = Corpus.citationAlphabet.lookupIndex(citation, true);
			citationSequence.add(index);
			citationCounts.adjustOrPutValue(index, 1, 1);
		}
	}

	public void addData(String name, int label, String[][] sentences, String[] citations) {
		if(sentences != null && sentences.length != 0) {
			addContent(name, label, sentences);
		}
		
		if(citations != null && citations.length != 0) {
			addCitations(name, label, citations);
		}
	}
	
	public void setDocID(int docID) {
		this.docID = docID;
	}
	public String getDocName() {
		return this.docName;
	}
	
	private static final long serialVersionUID = 1;
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeLong(serialVersionUID);
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		long version = in.readLong();
		if(version < serialVersionUID) throw new RuntimeException("Serial version is out of date");
		
		in.defaultReadObject();
	}

}
