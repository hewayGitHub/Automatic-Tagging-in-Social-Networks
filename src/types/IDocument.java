package types;

import java.io.Serializable;

public abstract class IDocument implements Serializable{
	private static final long serialVersionUID = 4672583436767162793L;
	
	public String docName;  //doc name should be unique
	public int docID;
	public int docLabel;
	
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
