package topic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.TreeSet;

import types.Alphabet;
import types.Corpus;
import types.Dirichlet;
import types.Document;
import types.IDSorter;
import utils.FileUtil;
import utils.Randoms;
import gnu.trove.TIntIntHashMap;

public class TwitterModel implements Serializable {
	private static final long serialVersionUID = 1L;
	public Corpus corpus = null;
	public Alphabet wordAlphabet = null;
	public Alphabet citationAlphabet = null;
	public Alphabet docNameAlphabet = null;
	
	public int numTopics;

	public int numDocs = 0;
	public int numUniqueWords;
	public int numUniqueCitations;

	public double[] alpha;
	public double alphaSum;
	public double beta;
	public double betaSum;
	public double betaB;
	public double gamma;
	public double gammaSum;
	

	public TIntIntHashMap[] wordTopicCounts;
	public TIntIntHashMap[] citationTopicCounts;
	public TIntIntHashMap[] docTopicCounts;
	public int[] wordsPerTopic;
	public int[] citationsPerTopic;
	public int[][] wordTopicAssignments;
	public int[][] citationTopicAssignments;

	public double[][] theta_train;
	public double[][] phi_train;
	public double[][] psi_train;

	public int numIterations;
	public int burninPeriod;
	public boolean printLogLikelihood = true;
	public int numSamples = 0;
	public Random r = new Random();
	public Randoms random;

	public int[] docLengthCounts;
	public int[][] topicDocCounts;
	
	public enum ModelParas {
		numTopics, alpha, beta, gamma, numIterations, burninPeriod, printLogLikelihood;
	}

	private void setModelPara(String paraFile) {
		ArrayList<String> inputlines = new ArrayList<String>();
		FileUtil.readLines(paraFile, inputlines);
		for (int i = 0; i < inputlines.size(); i++) {
			int index = inputlines.get(i).indexOf(":");
			String para = inputlines.get(i).substring(0, index).trim()
					.toLowerCase();
			String value = inputlines.get(i)
					.substring(index + 1, inputlines.get(i).length()).trim()
					.toLowerCase();
			switch (ModelParas.valueOf(para)) {
			case numTopics:
				numTopics = Integer.parseInt(value);
				break;
			case alpha:
				Arrays.fill(alpha, Double.parseDouble(value));
				break;
			case beta:
				beta = Double.parseDouble(value);
				break;
			case gamma:
				gamma = Double.parseDouble(value);
				break;
			case numIterations:
				numIterations = Integer.parseInt(value);
				break;
			case burninPeriod:
				burninPeriod = Integer.parseInt(value);
				break;
			case printLogLikelihood:
				printLogLikelihood = Boolean.parseBoolean(value);
				break;
			default:
				break;
			}
		}
	}

	public void addCorpus(Corpus corpus) {
		this.corpus = corpus;
		numDocs = Corpus.docNameAlphabet.size();
		numUniqueWords = Corpus.wordAlphabet.size();
		numUniqueCitations = Corpus.citationAlphabet.size();
		wordAlphabet = Corpus.wordAlphabet;
		citationAlphabet = Corpus.citationAlphabet;
		docNameAlphabet = Corpus.docNameAlphabet;
	}

	public void InitializeParameters(String paraFile, int numTopics) {
		if (corpus == null)
			throw new RuntimeException(
					"addCorpus() should be called first, since the corpus is empty now!");

		this.numTopics = numTopics;

		alpha = new double[numTopics];
		setModelPara(paraFile);
		alphaSum = 0;
		for (int i = 0; i < numTopics; i++) {
			alphaSum += alpha[i];
		}
		betaSum = beta * numUniqueWords;
		gammaSum = gamma * numUniqueCitations;

		wordTopicCounts = new TIntIntHashMap[numUniqueWords];
		for (int w = 0; w < numUniqueWords; w++)
			wordTopicCounts[w] = new TIntIntHashMap();

		citationTopicCounts = new TIntIntHashMap[numUniqueCitations];
		for (int c = 0; c < numUniqueCitations; c++)
			citationTopicCounts[c] = new TIntIntHashMap();

		docTopicCounts = new TIntIntHashMap[numDocs];
		for (int d = 0; d < numDocs; d++)
			docTopicCounts[d] = new TIntIntHashMap();

		wordsPerTopic = new int[numTopics];
		citationsPerTopic = new int[numTopics];

		wordTopicAssignments = new int[numDocs][];
		for (int d = 0; d < numDocs; d++) {
			Document doc = (Document) corpus.getDoc(d);
			wordTopicAssignments[d] = new int[doc.numWords];
		}

		citationTopicAssignments = new int[numDocs][];
		for (int d = 0; d < numDocs; d++) {
			Document doc = (Document) corpus.getDoc(d);
			citationTopicAssignments[d] = new int[doc.numCitations];
		}

		theta_train = new double[numDocs][numTopics]; // doc topic vector
		phi_train = new double[numTopics][numUniqueWords]; // topic-word
															// distribution
		psi_train = new double[numTopics][numUniqueCitations]; // topic-citation
																// distribution
	}

	public void InitializeAssignments() {
		this.random = new Randoms();
		for (int d = 0; d < numDocs; d++) {
			Document doc = (Document) corpus.getDoc(d);

			if (doc.numWords != 0) {
				int[] words = doc.wordSequence;
				for (int wPos = 0; wPos < words.length; wPos++) {
					int topic = r.nextInt(numTopics);

					wordTopicAssignments[d][wPos] = topic;

					docTopicCounts[d].adjustOrPutValue(topic, 1, 1);
					wordTopicCounts[words[wPos]].adjustOrPutValue(topic, 1, 1);
					wordsPerTopic[topic]++;
				}
			}

			if (doc.numCitations != 0) {
				int[] citations = doc.citationSequence;
				for (int cPos = 0; cPos < citations.length; cPos++) {
					int topic = r.nextInt(numTopics);
					citationTopicAssignments[d][cPos] = topic;

					docTopicCounts[d].adjustOrPutValue(topic, 1, 1);
					citationTopicCounts[citations[cPos]].adjustOrPutValue(
							topic, 1, 1);
					citationsPerTopic[topic]++;
				}
			}
		}
	}

	public void sampleOneDocument(Document doc, int dPos) {
		if (doc.numWords != 0) {
			int[] words = doc.wordSequence;
			int[] topics = wordTopicAssignments[dPos];
			for (int wPos = 0; wPos < words.length; wPos++) {
				docTopicCounts[dPos].adjustOrPutValue(topics[wPos], -1, 0);
				wordTopicCounts[words[wPos]].adjustOrPutValue(topics[wPos], -1,
						0);
				wordsPerTopic[topics[wPos]]--;

				TIntIntHashMap currentWordTopicCounts = wordTopicCounts[words[wPos]];
				double[] topicDistribution = new double[numTopics];
				double topicDistributionSum = 0, weight = 0;
				for (int k = 0; k < numTopics; k++) {
					/*
					 * weight = ((currentWordTopicCounts.get(k) + beta) /
					 * (wordsPerTopic[k] + betaSum))
					 * ((docTopicCounts[dPos].get(k) + alpha[k]) / (doc.numWords
					 * + alphaSum));
					 */
					weight = ((currentWordTopicCounts.get(k) + beta) / (wordsPerTopic[k] + betaSum))
							* ((docTopicCounts[dPos].get(k) + alpha[k]));
					topicDistributionSum += weight;
					topicDistribution[k] = weight;
				}

				int newTopic = random.nextDiscrete(topicDistribution,
						topicDistributionSum);

				wordTopicAssignments[dPos][wPos] = newTopic;
				docTopicCounts[dPos].adjustOrPutValue(newTopic, 1, 1);
				wordTopicCounts[words[wPos]].adjustOrPutValue(newTopic, 1, 1);
				wordsPerTopic[newTopic]++;
			}
		}

		if (doc.numCitations != 0) {
			int[] citations = doc.citationSequence;
			int[] topics = citationTopicAssignments[dPos];
			for (int cPos = 0; cPos < citations.length; cPos++) {
				docTopicCounts[dPos].adjustOrPutValue(topics[cPos], -1, 0);
				citationTopicCounts[citations[cPos]].adjustOrPutValue(
						topics[cPos], -1, 0);
				citationsPerTopic[topics[cPos]]--;

				TIntIntHashMap currentCitationTopicCounts = citationTopicCounts[citations[cPos]];
				double[] topicDistribution = new double[numTopics];
				double topicDistributionSum = 0, weight = 0;
				for (int t = 0; t < numTopics; t++) {
					weight = ((currentCitationTopicCounts.get(t) + gamma) / (citationsPerTopic[t] + gammaSum))
							* ((docTopicCounts[dPos].get(t) + alpha[t]));
					topicDistributionSum += weight;
					topicDistribution[t] = weight;
				}

				int newTopic = random.nextDiscrete(topicDistribution,
						topicDistributionSum);

				citationTopicAssignments[dPos][cPos] = newTopic;
				docTopicCounts[dPos].adjustOrPutValue(newTopic, 1, 1);
				citationTopicCounts[citations[cPos]].adjustOrPutValue(newTopic,
						1, 1);
				citationsPerTopic[newTopic]++;
			}
		}
	}

	public void estimate() {
		long startTime = System.currentTimeMillis();

		for (int iteration = 1; iteration <= numIterations; iteration++) {
			System.out.println("Iteration " + iteration + "start...");
			long iterationStart = System.currentTimeMillis();

			for (int dPos = 0; dPos < numDocs; dPos++) {
				sampleOneDocument((Document) corpus.getDoc(dPos), dPos);
			}

			long elapsedMillis = System.currentTimeMillis() - iterationStart;
			if (elapsedMillis < 1000) {
				System.out.println("Iteration finished: " + elapsedMillis
						+ "ms ");
			} else {
				System.out.println("Iteration finished: "
						+ (elapsedMillis / 1000) + "s ");
			}

			if (iteration % 10 == 0) {
				if (printLogLikelihood) {
					System.out.println("<" + iteration + "> LL/token: "
							+ modelLogLikelihood());
				} else {
					System.out.println("<" + iteration + ">");
				}
			}

			if (iteration == numIterations || iteration >= burninPeriod
					&& iteration % 10 == 0) {
				estimateParameters();
			}
		}

		long seconds = Math
				.round((System.currentTimeMillis() - startTime) / 1000.0);
		long minutes = seconds / 60;
		seconds %= 60;
		long hours = minutes / 60;
		minutes %= 60;
		long days = hours / 24;
		hours %= 24;

		StringBuilder timeReport = new StringBuilder();
		timeReport.append("\nTotal time: ");
		if (days != 0) {
			timeReport.append(days);
			timeReport.append(" days ");
		}
		if (hours != 0) {
			timeReport.append(hours);
			timeReport.append(" hours ");
		}
		if (minutes != 0) {
			timeReport.append(minutes);
			timeReport.append(" minutes ");
		}
		timeReport.append(seconds);
		timeReport.append(" seconds");

		System.out.println(timeReport.toString());
	}

	public void learnParameters(Corpus corpus) {
		docLengthCounts = new int[corpus.maxDocLen + 1];
		topicDocCounts = new int[numTopics][corpus.maxDocLen + 1];
		for (int d = 0; d < numDocs; d++) {
			Document doc = (Document) corpus.getDoc(d);

			docLengthCounts[doc.numWords]++;

			int topics[] = docTopicCounts[d].keys();
			for (int topic : topics) {
				topicDocCounts[topic][docTopicCounts[d].get(topic)]++;
			}
		}

		alphaSum = Dirichlet.learnParameters(alpha, topicDocCounts,
				docLengthCounts);
	}

	// estimate parameters after every k iterations after burn-in
	public void estimateParameters() {
		numSamples++;

		for (int dPos = 0; dPos < numDocs; dPos++) {
			Document doc = (Document) corpus.getDoc(dPos);

			for (int k = 0; k < numTopics; k++) {
				if (numSamples > 1)
					theta_train[dPos][k] *= (numSamples - 1);
				theta_train[dPos][k] += (alpha[k] + docTopicCounts[dPos].get(k)
						/ (doc.numWords + doc.numCitations + alphaSum));

				if (numSamples > 1)
					theta_train[dPos][k] /= numSamples;
			}
		}

		for (int k = 0; k < numTopics; k++) {
			for (int wPos = 0; wPos < numUniqueWords; wPos++) {
				if (numSamples > 1)
					phi_train[k][wPos] *= (numSamples - 1);
				phi_train[k][wPos] += ((wordTopicCounts[wPos].get(k) + beta) / (wordsPerTopic[k] + betaSum));

				if (numSamples > 1)
					phi_train[k][wPos] /= numSamples;
			}
		}

		for (int k = 0; k < numTopics; k++) {
			for (int cPos = 0; cPos < numUniqueCitations; cPos++) {
				if (numSamples > 1)
					psi_train[k][cPos] *= (numSamples - 1);
				psi_train[k][cPos] += ((citationTopicCounts[cPos].get(k) + gamma) / (citationsPerTopic[k] + gammaSum));

				if (numSamples > 1)
					psi_train[k][cPos] /= (numSamples);
			}
		}
	}

	public double modelLogLikelihood() {
		double logLikelihood = 0.0;

		// The likelihood of the model is a combination of a
		// Dirichlet-multinomial for the words in each topic
		// and a Dirichlet-multinomial for the topics in each
		// document.

		// The likelihood function of a dirichlet multinomial is
		// Gamma( sum_i alpha_i ) prod_i Gamma( alpha_i + N_i )
		// prod_i Gamma( alpha_i ) Gamma( sum_i (alpha_i + N_i) )

		// So the log likelihood is
		// logGamma ( sum_i alpha_i ) - logGamma ( sum_i (alpha_i + N_i) ) +
		// sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i ) ]

		// Do the documents first

		double[] topicLogGammas = new double[numTopics];
		int sampleSize = 0;

		for (int topic = 0; topic < numTopics; topic++) {
			topicLogGammas[topic] = Dirichlet.logGammaStirling(alpha[topic]);
		}

		for (int dPos = 0; dPos < numDocs; dPos++) {
			Document doc = (Document) corpus.getDoc(dPos);
			sampleSize += doc.numWords + doc.numCitations;

			for (int topic : docTopicCounts[dPos].keys()) {
				int count = docTopicCounts[dPos].get(topic);
				if (count > 0)
					logLikelihood += (Dirichlet.logGammaStirling(alpha[topic]
							+ count) - topicLogGammas[topic]);
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet
					.logGammaStirling(alphaSum + doc.numWords);
		}

		// add the parameter sum term
		logLikelihood += numDocs * Dirichlet.logGammaStirling(alphaSum);

		// And the topics
		// Count the number of type-topic pairs
		double logGammaBeta = Dirichlet.logGammaStirling(beta);
		for (int wPos = 0; wPos < numUniqueWords; wPos++) {
			for (int topic : wordTopicCounts[wPos].keys()) {
				int count = wordTopicCounts[wPos].get(topic);
				if (count > 0) {
					logLikelihood += Dirichlet.logGammaStirling(beta + count)
							- logGammaBeta;
				}
			}
		}

		for (int topic = 0; topic < numTopics; topic++) {
			logLikelihood -= Dirichlet.logGammaStirling(betaSum
					+ wordsPerTopic[topic]);
		}

		logLikelihood += numTopics * Dirichlet.logGammaStirling(betaSum);

		double logGammaR = Dirichlet.logGammaStirling(gamma);
		for (int cPos = 0; cPos < numUniqueCitations; cPos++) {
			for (int topic : citationTopicCounts[cPos].keys()) {
				int count = citationTopicCounts[cPos].get(topic);
				if (count > 0) {
					logLikelihood += Dirichlet.logGammaStirling(gamma + count)
							- logGammaR;
				}
			}
		}

		for (int topic = 0; topic < numTopics; topic++) {
			logLikelihood -= Dirichlet.logGammaStirling(gammaSum
					+ citationsPerTopic[topic]);
		}

		logLikelihood += numTopics * Dirichlet.logGammaStirling(gammaSum);

		return Math.exp(-1 * logLikelihood / sampleSize);
	}

	/**
	 * Return an array of sorted sets (one set per topic). Each set contains
	 * IDSorter objects with integer keys into the alphabet. To get direct
	 * access to the Strings, use getTopWords().
	 */
	public ArrayList<TreeSet<IDSorter>> getSortedWords(int topN) {
		ArrayList<TreeSet<IDSorter>> topicSortedWords = new ArrayList<TreeSet<IDSorter>>(
				numTopics);

		// Initialize the tree sets
		for (int topic = 0; topic < numTopics; topic++) {
			topicSortedWords.add(new TreeSet<IDSorter>());
		}

		// Collect counts
		int[] wordCount = new int[numTopics];
		for (int word = 0; word < numUniqueWords; word++) {
			for (int k = 0; k < numTopics; k++) {
				if (wordCount[k] < topN) {
					topicSortedWords.get(k).add(
							new IDSorter(word, phi_train[k][word]));

					wordCount[k]++;
				} else {
					IDSorter curMin = topicSortedWords.get(k).first();
					if (curMin.getWeight() < phi_train[k][word]) {
						topicSortedWords.get(k).pollFirst();
						topicSortedWords.get(k).add(
								new IDSorter(word, phi_train[k][word]));
					}
				}
			}
		}

		return topicSortedWords;
	}

	public ArrayList<TreeSet<IDSorter>> getSortedCitations(int topN) {
		ArrayList<TreeSet<IDSorter>> topicSortedCitation = new ArrayList<TreeSet<IDSorter>>(
				numTopics);

		// Initialize the tree sets
		for (int topic = 0; topic < numTopics; topic++) {
			topicSortedCitation.add(new TreeSet<IDSorter>());
		}

		// Collect counts
		int[] citationCount = new int[numTopics];
		for (int citation = 0; citation < numUniqueCitations; citation++) {
			for (int k = 0; k < numTopics; k++) {
				if (citationCount[k] < topN) {
					topicSortedCitation.get(k).add(
							new IDSorter(citation, psi_train[k][citation]));

					citationCount[k]++;
				} else {
					IDSorter curMin = topicSortedCitation.get(k).first();
					if (curMin.getWeight() < psi_train[k][citation]) {
						topicSortedCitation.get(k).pollFirst();
						topicSortedCitation.get(k).add(
								new IDSorter(citation, psi_train[k][citation]));
					}
				}
			}
		}

		return topicSortedCitation;
	}
	
	/**
	 * Return an array (one element for each topic) of arrays of words, which
	 * are the most probable words for that topic in descending order. These are
	 * returned as Objects, but will probably be Strings.
	 * 
	 * @param numWords
	 *            The maximum length of each topic's array of words (may be
	 *            less).
	 */

	public Object[][] getTopWords(int topN) {
		ArrayList<TreeSet<IDSorter>> topicSortedWords = getSortedWords(topN);
		Object[][] result = new Object[numTopics][];

		for (int topic = 0; topic < numTopics; topic++) {
			TreeSet<IDSorter> sortedWords = topicSortedWords.get(topic);

			// How many words should we report? Some topics may have fewer than
			// the default number of words with non-zero weight.
			int limit = topN;
			if (sortedWords.size() < topN) {
				limit = sortedWords.size();
			}

			result[topic] = new Object[limit];

			Iterator<IDSorter> iterator = sortedWords.iterator();
			for (int i = 0; i < limit; i++) {
				IDSorter info = iterator.next();
				result[topic][i] = wordAlphabet.lookupObject(info.getID());
			}
		}

		return result;
	}

	public void printTopWords(PrintWriter out, int topN, boolean usingNewLines) {
		out.print(displayTopWords(topN, usingNewLines));
	}
	
	public String displayTopWords(int topN, boolean usingNewLines) {
		ArrayList<TreeSet<IDSorter>> topicSortedWords = getSortedWords(topN);

		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int topic = 0; topic < numTopics; topic++) {
			TreeSet<IDSorter> sortedWords = topicSortedWords.get(topic);

			if (usingNewLines) {
				out.format("#%d:\n", topic);
				for(IDSorter info: sortedWords) {
					out.format("\t%s (%.5f)\n", wordAlphabet.lookupObject(info.getID()), info.getWeight());
				}
			} else {
				out.format("#%d:\t", topic);

				for(IDSorter info: sortedWords) {
					out.format("%s (%.5f) ", wordAlphabet.lookupObject(info.getID()),
							info.getWeight());
				}
				out.format("\n");
			}
		}
		
		String res = out.toString();
		out.close();
		return res;
	}
	
	public Object[][] getTopCitations(int topN) {
		ArrayList<TreeSet<IDSorter>> topicSortedCitations = getSortedCitations(topN);
		Object[][] result = new Object[numTopics][];

		for (int topic = 0; topic < numTopics; topic++) {
			TreeSet<IDSorter> sortedCitations = topicSortedCitations.get(topic);

			// How many words should we report? Some topics may have fewer than
			// the default number of words with non-zero weight.
			int limit = topN;
			if (sortedCitations.size() < topN) {
				limit = sortedCitations.size();
			}

			result[topic] = new Object[limit];

			Iterator<IDSorter> iterator = sortedCitations.iterator();
			for (int i = 0; i < limit; i++) {
				IDSorter info = iterator.next();
				result[topic][i] = citationAlphabet.lookupObject(info.getID());
			}
		}

		return result;
	}

	public void printTopCitations(PrintWriter out, int topN, boolean usingNewLines) {
		out.print(displayTopCitations(topN, usingNewLines));
	}
	
	public String displayTopCitations(int topN, boolean usingNewLines) {
		ArrayList<TreeSet<IDSorter>> topicSortedCitations = getSortedCitations(topN);

		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int topic = 0; topic < numTopics; topic++) {
			TreeSet<IDSorter> sortedCitations = topicSortedCitations.get(topic);

			if (usingNewLines) {
				out.format("#%d:\n", topic);
				for(IDSorter info: sortedCitations) {
					out.format("\t%s (%.5f)\n", citationAlphabet.lookupObject(info.getID()), info.getWeight());
				}
			} else {
				out.format("#%d:\t", topic);

				for(IDSorter info: sortedCitations) {
					out.format("%s (%.5f) ", citationAlphabet.lookupObject(info.getID()),
							info.getWeight());
				}
				out.format("\n");
			}
		}
		
		String res = out.toString();
		out.close();
		return res;
	}
	
	
	public double testPerplexity() {
		double logSum = 0;
		int sampleSize = 0;

		for (int dPos = 0; dPos < numDocs; dPos++) {
			Document doc = (Document) corpus.getDoc(dPos);
			sampleSize += doc.numWords;

			TIntIntHashMap wordCounts = doc.wordCounts;
			int[] keys = wordCounts.keys();
			for (int w : keys) {
				double probWord = 0.0;
				for (int t = 0; t < numTopics; t++) {
					probWord += theta_train[dPos][t] * phi_train[t][w];
				}
				logSum += (Math.log(probWord) * wordCounts.get(w));
			}

		}

		return Math.exp(-1 * logSum / sampleSize);
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeLong(serialVersionUID);

		out.writeObject(wordAlphabet);
		out.writeObject(citationAlphabet);
		out.writeObject(docNameAlphabet);

		out.writeInt(numTopics);
		out.writeInt(numUniqueWords);
		out.writeInt(numUniqueCitations);

		out.writeObject(alpha);
		out.writeDouble(alphaSum);
		out.writeDouble(beta);
		out.writeDouble(betaSum);
		out.writeDouble(gamma);
		out.writeDouble(gammaSum);

		out.writeObject(theta_train);
		out.writeObject(phi_train);
		out.writeObject(psi_train);

		out.writeInt(numSamples);
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		Long version = in.readLong();
		if (version < serialVersionUID)
			throw new RuntimeException("Serial version is out of date");

		wordAlphabet = (Alphabet) in.readObject();
		citationAlphabet = (Alphabet) in.readObject();
		docNameAlphabet = (Alphabet) in.readObject();

		numTopics = in.readInt();
		numUniqueWords = in.readInt();
		numUniqueCitations = in.readInt();

		alpha = (double[]) in.readObject();
		alphaSum = in.readDouble();
		beta = in.readDouble();
		betaSum = in.readDouble();
		gamma = in.readDouble();
		gammaSum = in.readDouble();

		theta_train = (double[][]) in.readObject();
		phi_train = (double[][]) in.readObject();
		psi_train = (double[][]) in.readObject();

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

	public static TwitterModel read(File f) throws Exception {

		TwitterModel topicModel = null;

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
		topicModel = (TwitterModel) ois.readObject();
		ois.close();

		return topicModel;
	}
}
