package types;

import java.util.*;
import java.io.*;

public class Corpus implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static Alphabet wordAlphabet = new Alphabet();
	public static Alphabet citationAlphabet = new Alphabet();
	public static Alphabet docNameAlphabet = new Alphabet();

	public Vector<IDocument> docs;
	public int numDocs = 0;
	
	public int maxDocLen = 0;
	
	public Corpus() {
	}
	
	public void addDoc(IDocument doc) {
		if (docs == null) {
			docs = new Vector<IDocument>();
		}
		
		int index = docNameAlphabet.lookupIndex(doc.getDocName(), true);
		if(docs.size() == index) {
			doc.setDocID(index);
			docs.add(doc);
		} else{
			throw new RuntimeException("Doc name(" + doc.getDocName() + ") has been already added!");
		}
		
		numDocs++;
		
		if(doc.docLen > maxDocLen) maxDocLen = doc.docLen;
	}
	
	public void addDocs(Vector<IDocument> documents) {
		for(IDocument doc: documents) {
			addDoc(doc);
		}
	}
	
	public IDocument getDoc(int index) {
		if (docs.size() <= index) {
			throw new IllegalArgumentException("Doc id exceed corpus size!");
		}
		
		return docs.get(index);
	}
	
	public IDocument getDoc(String docName) {
		int index = docNameAlphabet.lookupIndex(docName, false);
		if(index == -1) {
			return null;
		}
		
		return docs.get(index);
	}
		
	public void stopGrowth(){
		docNameAlphabet.stopGrowth();
		wordAlphabet.stopGrowth();
		citationAlphabet.stopGrowth();
	}
	
	public boolean isStopGrowth(){
		return wordAlphabet.isGrowthStopped() || citationAlphabet.isGrowthStopped() 
				|| docNameAlphabet.isGrowthStopped();
	}
	
	public void writeObject(ObjectOutputStream out) throws IOException {
		out.writeLong(serialVersionUID);
		
		out.writeObject(wordAlphabet);
		out.writeObject(citationAlphabet);
		out.writeObject(docNameAlphabet);
		
		out.writeInt(numDocs);
		out.writeInt(maxDocLen);
		
		for (int i = 0; i < numDocs; i++)
		    out.writeObject(docs.get(i));
   }

   public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		Long version = in.readLong();
		if(version < serialVersionUID) throw new RuntimeException("Serial version is out of date");
		
		wordAlphabet = (Alphabet) in.readObject();
		citationAlphabet = (Alphabet) in.readObject();
		docNameAlphabet = (Alphabet) in.readObject();
		
		numDocs = in.readInt();
		maxDocLen = in.readInt();
		
		docs = new Vector<IDocument>();
		for (int i = 0; i < numDocs; i++)
			docs.add((IDocument) in.readObject());
   }

}