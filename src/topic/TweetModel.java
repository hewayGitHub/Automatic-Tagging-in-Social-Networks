package topic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import types.Alphabet;
import types.IDocument;
import gnu.trove.TIntIntHashMap;

public abstract class TweetModel extends Model {
	private static final long serialVersionUID = 1L;
	public double betaB;
	public double betaBSum;
	public double delta;
	public double deltaSum;
	
	public double[] rho;

	public void InitializeParameters(String modelParasFile) {
		if (corpus == null)
			throw new RuntimeException(
					"setCorpus() should be called first, since the corpus is empty now!");
		
		TweetModelParas.setModelPara(modelParasFile, this);
		
		alphaSum = 0;
		for (int i = 0; i < numTopics; i++) {
			alphaSum += alpha[i];
		}
		betaSum = beta * numUniqueWords;
		gammaSum = gamma * numUniqueCitations;
		betaBSum = betaB * numUniqueWords;
		deltaSum = 2 * delta;
		
		theta = new double[numDocs][numTopics+1]; // doc topic vector
		phi = new double[numTopics+1][numUniqueWords]; // topic-word
		psi = new double[numTopics+1][numUniqueCitations]; // topic-citation
		rho = new double[2];
	}
	
	public double computePerplexity() {
		double logSum = 0;
		int sampleSize = 0;

		for (int dPos = 0; dPos < numDocs; dPos++) {
			IDocument doc = corpus.getDoc(dPos);
			sampleSize += doc.numWords;

			TIntIntHashMap wordCounts = doc.wordCounts;
			int[] keys = wordCounts.keys();
			for (int w : keys) {
				double probWord = 0.0;
				for (int t = 0; t < numTopics; t++) {
					probWord += theta[dPos][t] * phi[t][w];
				}
				logSum += (Math.log(probWord) * wordCounts.get(w));
			}

		}

		return Math.exp(-1 * logSum / sampleSize);
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeLong(serialVersionUID);
		
		out.writeObject(modelName);
		
		out.writeObject(wordAlphabet);
		out.writeObject(citationAlphabet);
		out.writeObject(docNameAlphabet);

		out.writeInt(numTopics);
		out.writeInt(numDocs);
		out.writeInt(numUniqueWords);
		out.writeInt(numUniqueCitations);

		out.writeObject(alpha);
		out.writeDouble(alphaSum);
		out.writeDouble(beta);
		out.writeDouble(betaSum);
		out.writeDouble(gamma);
		out.writeDouble(gammaSum);
		
		out.writeDouble(betaB);
		out.writeDouble(betaBSum);
		out.writeDouble(delta);
		out.writeDouble(deltaSum);
		
		out.writeObject(theta);
		out.writeObject(phi);
		out.writeObject(psi);
		out.writeObject(rho);

		out.writeInt(numSamples);
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		Long version = in.readLong();
		if (version < serialVersionUID)
			throw new RuntimeException("Serial version is out of date");
		
		modelName = (String) in.readObject();
		
		wordAlphabet = (Alphabet) in.readObject();
		citationAlphabet = (Alphabet) in.readObject();
		docNameAlphabet = (Alphabet) in.readObject();

		numTopics = in.readInt();
		numDocs = in.readInt();
		numUniqueWords = in.readInt();
		numUniqueCitations = in.readInt();

		alpha = (double[]) in.readObject();
		alphaSum = in.readDouble();
		beta = in.readDouble();
		betaSum = in.readDouble();
		gamma = in.readDouble();
		gammaSum = in.readDouble();
		
		betaB = in.readDouble();
		betaBSum = in.readDouble();
		delta = in.readDouble();
		deltaSum = in.readDouble();
		
		theta = (double[][]) in.readObject();
		phi = (double[][]) in.readObject();
		psi = (double[][]) in.readObject();
		
		rho = (double[]) in.readObject();
		
		numSamples = in.readInt();
	}

	public void write(File serializedModelFile) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(serializedModelFile));
			oos.writeObject(this);
			oos.close();
		} catch (IOException e) {
			System.err
					.println("Problem serializing ParallelTopicModel to file "
							+ serializedModelFile + ": " + e);
		}
	}

	public static TweetModel read(File f) throws Exception {

		TweetModel topicModel = null;

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
		topicModel = (TweetModel) ois.readObject();
		ois.close();

		return topicModel;
	}

}
