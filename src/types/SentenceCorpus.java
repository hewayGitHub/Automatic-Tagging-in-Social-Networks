package types;

import java.util.*;

import java.io.*;

public class SentenceCorpus implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static Alphabet vocabulary = new Alphabet();
	public static Alphabet citationAlphabet = new Alphabet();
	public static Alphabet docNameAlphabet = new Alphabet(); // save doc name

	public Vector<SentenceDocument> docs;
	public int numDocs = 0;
	public int numSentences = 0;
	public int numUniqueWords;
	public int numUniqueCitations;
	
	public int maxDocLen = 0;
	
	public SentenceCorpus() {
	}
	
	public void addDoc(SentenceDocument doc) {
		if (docs == null) {
			docs = new Vector<SentenceDocument>();
		}
		int index = docNameAlphabet.lookupIndex(doc.getDocName());
		if(docs.size() == index) {
			doc.setDocID(index);
			docs.add(doc);
			
			numSentences += doc.numSentences;//make sure that words are read first than its particular citations
		} else{
			docs.set(index, doc);
			System.out.println("Doc name: " + doc.getDocName() + " has been changed");
		}
		
		numDocs = docs.size();
		numUniqueWords = vocabulary.size();
		numUniqueCitations = citationAlphabet.size();
		
		if(doc.numWords > maxDocLen) maxDocLen = doc.numWords;
	}
	
	
	public void addDocs(Vector<SentenceDocument> documents) {
		for(SentenceDocument doc: documents) {
			addDoc(doc);
		}
	}
	
	
	public SentenceDocument getDoc(int index) {
		if (docs.size() <= index) {
			throw new IllegalArgumentException("Doc id exceed corpus size!");
		}
		
		return docs.get(index);
	}
	
	
	public SentenceDocument getDoc(String docName) {
		int index = docNameAlphabet.lookupIndex(docName, false);
		if(index == -1) {
			return null;
		}
		
		return docs.get(index);
	}
	
	
	public void stopGrowth(){
		docNameAlphabet.stopGrowth();
		vocabulary.stopGrowth();
		citationAlphabet.stopGrowth();
	}
	
	public void writeObject(ObjectOutputStream out) throws IOException {
		out.writeLong(serialVersionUID);
		
		out.writeObject(vocabulary);
		out.writeObject(citationAlphabet);
		out.writeObject(docNameAlphabet);
		
		out.writeInt(numDocs);
		out.writeInt(numSentences);
		out.writeInt(numUniqueWords);
		out.write(numUniqueCitations);
		
		for (int i = 0; i < numDocs; i++)
		    out.writeObject(docs.get(i));
   }

   public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		Long version = in.readLong();
		if(version < serialVersionUID) throw new RuntimeException("Serial version is out of date");
		
		vocabulary = (Alphabet) in.readObject();
		citationAlphabet = (Alphabet) in.readObject();
		docNameAlphabet = (Alphabet) in.readObject();
		
		numDocs = in.readInt();
		numSentences = in.readInt();
		numUniqueWords = in.readInt();
		numUniqueCitations = in.readInt();
		
		docs = new Vector<SentenceDocument>();
	
		 for (int i = 0; i < numDocs; i++)
			 docs.add((SentenceDocument) in.readObject());
   }

}