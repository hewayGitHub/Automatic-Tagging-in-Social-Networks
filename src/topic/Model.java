package topic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import types.Alphabet;
import types.Corpus;
import types.IDSorter;
import types.IDocument;
import utils.Randoms;
import gnu.trove.TIntIntHashMap;

public abstract class Model implements Serializable {
	private static final long serialVersionUID = 1L;
	public String modelName;
	
	public Corpus corpus = null;
	public Alphabet wordAlphabet = null;
	public Alphabet citationAlphabet = null;
	public Alphabet docNameAlphabet = null;

	public int numDocs = 0;
	public int numUniqueWords;
	public int numUniqueCitations;

	public String modelParasFile;
	
	public double[] alpha;
	public double alphaSum;
	public double beta;
	public double betaSum;
	public double gamma;
	public double gammaSum;

	public int numTopics;
	public int numTopicWord;
	public int numIterations;
	public int burninPeriod;
	public boolean printLogLikelihood = false;
	
	public double[][] theta;
	public double[][] phi;
	public double[][] psi;

	public int numSamples = 0;
	public Random r = new Random();
	public Randoms random;

	public int[] docLengthCounts;
	public int[][] topicDocCounts;
	
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public void setCorpus(Corpus corpus) {
		this.corpus = corpus;
		numDocs = corpus.numDocs;
		numUniqueWords = Corpus.wordAlphabet.size();
		numUniqueCitations = Corpus.citationAlphabet.size();
		
		wordAlphabet = Corpus.wordAlphabet;
		citationAlphabet = Corpus.citationAlphabet;
		docNameAlphabet = Corpus.docNameAlphabet;
		
		corpus.stopGrowth();
	}

	public void InitializeParameters(String modelParasFile) {
		if (corpus == null)
			throw new RuntimeException(
					"setCorpus() should be called first, since the corpus is empty now!");
		
		ModelParas.setModelPara(modelParasFile, this);
		
		alphaSum = 0;
		for (int i = 0; i < numTopics; i++) {
			alphaSum += alpha[i];
		}
		betaSum = beta * numUniqueWords;
		gammaSum = gamma * numUniqueCitations;

		theta = new double[numDocs][numTopics]; // doc topic vector
		phi = new double[numTopics][numUniqueWords]; // topic-word
		psi = new double[numTopics][numUniqueCitations]; // topic-citation
	}
	
	public abstract void InitializeAssignments();

	abstract public void estimate();

	abstract public void estimateParameters();

	/**
	 * Return an array of sorted sets (one set per topic). Each set contains
	 * IDSorter objects with integer keys into the alphabet. To get direct
	 * access to the Strings, use getTopWords().
	 */
	public ArrayList<TreeSet<IDSorter>> getSortedTokens(int topN, String tokenType) {
		ArrayList<TreeSet<IDSorter>> topicSortedTokens = new ArrayList<TreeSet<IDSorter>>(
				numTopics);

		// Initialize the tree sets
		for (int topic = 0; topic < numTopics; topic++) {
			topicSortedTokens.add(new TreeSet<IDSorter>());
		}
		
		double[][] topicProb = null;
		int numTokens = 0;
		if(tokenType.equalsIgnoreCase("word")) {
			topicProb = phi;
			numTokens = numUniqueWords;
		} else {
			topicProb = psi;
			numTokens = numUniqueCitations;
		}
		
		// Collect counts
		int[] tokenCount = new int[numTopics];
		for (int t = 0; t < numTokens; t++) {
			for (int k = 0; k < numTopics; k++) {
				if (tokenCount[k] < topN) {
					topicSortedTokens.get(k).add(
							new IDSorter(t, topicProb[k][t]));

					tokenCount[k]++;
				} else {
					IDSorter curMin = topicSortedTokens.get(k).first();
					if (curMin.getWeight() < topicProb[k][t]) {
						topicSortedTokens.get(k).pollFirst();
						topicSortedTokens.get(k).add(
								new IDSorter(t, topicProb[k][t]));
					}
				}
			}
		}

		return topicSortedTokens;
	}
	
	/**
	 * Return an array (one element for each topic) of arrays of words, which
	 * are the most probable words for that topic in descending order. These are
	 * returned as Objects, but will probably be Strings.
	 * 
	 * @param topN
	 *            The maximum length of each topic's array of words (may be
	 *            less).
	 */

	public Object[][] getTopTokens(int topN, String tokenType) {
		ArrayList<TreeSet<IDSorter>> topicSortedTokens = getSortedTokens(topN, tokenType);
		Object[][] result = new Object[numTopics][];
		
		Alphabet alphabet = null;
		if(tokenType.equalsIgnoreCase("word")) {
			alphabet = wordAlphabet;
		} else {
			alphabet = citationAlphabet;
		}
		
		for (int topic = 0; topic < numTopics; topic++) {
			TreeSet<IDSorter> sortedTokens = topicSortedTokens.get(topic);

			// How many words should we report? Some topics may have fewer than
			// the default number of words with non-zero weight.
			int limit = topN;
			if (sortedTokens.size() < topN) {
				limit = sortedTokens.size();
			}

			result[topic] = new Object[limit];

			Iterator<IDSorter> iterator = sortedTokens.iterator();
			for (int i = 0; i < limit; i++) {
				IDSorter info = iterator.next();
				result[topic][i] = alphabet.lookupObject(info.getID());
			}
		}

		return result;
	}
	
	public Set<Integer> getTopTokenIDSet(int topN, String tokenType) {
		ArrayList<TreeSet<IDSorter>> topicSortedTokens = getSortedTokens(topN, tokenType);
		Set<Integer> result = new HashSet<Integer>();

		for (int topic = 0; topic < numTopics; topic++) {
			TreeSet<IDSorter> sortedWords = topicSortedTokens.get(topic);

			// How many words should we report? Some topics may have fewer than
			// the default number of words with non-zero weight.
			int limit = topN;
			if (sortedWords.size() < topN) {
				limit = sortedWords.size();
			}

			Iterator<IDSorter> iterator = sortedWords.iterator();
			for (int i = 0; i < limit; i++) {
				IDSorter info = iterator.next();
				result.add(info.getID());
			}
		}

		return result;
	}

	public void printTopTokens(PrintWriter out, int topN, boolean usingNewLines, String tokenType) {
		ArrayList<TreeSet<IDSorter>> topicSortedTokens = getSortedTokens(topN, tokenType);
		
		Alphabet alphabet = null;
		if(tokenType.equalsIgnoreCase("word")) {
			alphabet = wordAlphabet;
		} else {
			alphabet = citationAlphabet;
		}
		
		for (int topic = 0; topic < numTopics; topic++) {
			TreeSet<IDSorter> sortedWords = topicSortedTokens.get(topic);

			if (usingNewLines) {
				out.format("#%d:\n", topic);
				for(IDSorter info: sortedWords) {
					out.format("\t%s (%.5f)\n", alphabet.lookupObject(info.getID()), info.getWeight());
				}
			} else {
				out.format("#%d:\t", topic);

				for(IDSorter info: sortedWords) {
					out.format("%s (%.5f) ", alphabet.lookupObject(info.getID()),
							info.getWeight());
				}
				out.format("\n");
			}
		}
		
		out.flush();
	}
	
	public void displayTopTokens(int topN, boolean usingNewLines, String tokenType) {
		printTopTokens(new PrintWriter(new OutputStreamWriter(System.out)), 
				topN, usingNewLines, tokenType);
	}
	
	public void printDocTopicVector(String filePath) {
		PrintWriter out = null;
		
		try{
			out = new PrintWriter(filePath);
			
			for(int dPos = 0; dPos < numDocs; dPos++) {
				out.print(docNameAlphabet.lookupObject(dPos) + "\t");

				for(int k = 0; k < numTopics; k++) {
					out.format("%.5f ", theta[dPos][k]);
				}
				out.println();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if(out != null) {
				out.close();
			}
		}
	}
	
	public void printTopicTokenVector(PrintWriter out, String tokenType) {
		int numTokens = 0;
		double[][] topicProb = null;
		if(tokenType.equalsIgnoreCase("word")) {
			topicProb = phi;
			numTokens = numUniqueWords;
		} else {
			topicProb = psi;
			numTokens = numUniqueCitations;
		}
		
		for(int k = 0; k < numTopics; k++) {
			out.print("#" + k + "\t");

			for(int t = 0; t < numTokens; t++) {
				out.format("%.5f ", topicProb[k][t]);
			}
			out.println();
		}
		
		out.flush();
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

		out.writeObject(theta);
		out.writeObject(phi);
		out.writeObject(psi);

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

		theta = (double[][]) in.readObject();
		phi = (double[][]) in.readObject();
		psi = (double[][]) in.readObject();

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

	public static Model read(File f) throws Exception {

		Model topicModel = null;

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
		topicModel = (Model) ois.readObject();
		ois.close();

		return topicModel;
	}

}
