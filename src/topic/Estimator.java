package topic;

import java.io.Serializable;
import types.Dirichlet;
import types.Document;
import types.IDocument;
import utils.Randoms;
import gnu.trove.TIntIntHashMap;

public class Estimator extends Model implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public TIntIntHashMap[] wordTopicCounts;
	public TIntIntHashMap[] citationTopicCounts;
	public TIntIntHashMap[] docTopicCounts;
	public int[] wordsPerTopic;
	public int[] citationsPerTopic;
	public int[][] wordTopicAssignments;
	public int[][] citationTopicAssignments;

	public void InitializeParameters(String modelParasFile) {
		super.InitializeParameters(modelParasFile);
		
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
			IDocument doc = corpus.getDoc(d);
			wordTopicAssignments[d] = new int[doc.numWords];
		}

		citationTopicAssignments = new int[numDocs][];
		for (int d = 0; d < numDocs; d++) {
			IDocument doc = corpus.getDoc(d);
			citationTopicAssignments[d] = new int[doc.numCitations];
		}
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
			System.out.println("Iteration " + iteration + " start...");
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

			if (iteration % 10 == 0 || iteration == 1) {
				if (printLogLikelihood) {
					System.out.println("<" + iteration + "> LL: "
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

	public void learnParameters() {
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
			IDocument doc = corpus.getDoc(dPos);

			for (int k = 0; k < numTopics; k++) {
				if (numSamples > 1)
					theta[dPos][k] *= (numSamples - 1);
				theta[dPos][k] += (alpha[k] + docTopicCounts[dPos].get(k))
						/ (doc.docLen + alphaSum);

				if (numSamples > 1)
					theta[dPos][k] /= numSamples;
			}
		}

		for (int k = 0; k < numTopics; k++) {
			for (int wPos = 0; wPos < numUniqueWords; wPos++) {
				if (numSamples > 1)
					phi[k][wPos] *= (numSamples - 1);
				phi[k][wPos] += (wordTopicCounts[wPos].get(k) + beta) / (wordsPerTopic[k] + betaSum);

				if (numSamples > 1)
					phi[k][wPos] /= numSamples;
			}
		}

		for (int k = 0; k < numTopics; k++) {
			for (int cPos = 0; cPos < numUniqueCitations; cPos++) {
				if (numSamples > 1)
					psi[k][cPos] *= (numSamples - 1);
				psi[k][cPos] += (citationTopicCounts[cPos].get(k) + gamma) / (citationsPerTopic[k] + gammaSum);

				if (numSamples > 1)
					psi[k][cPos] /= numSamples;
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
		if(numUniqueWords > 0) {
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
		}
		
		if(numUniqueCitations > 0) {
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
		}
		

		return Math.exp(-1 * logLikelihood / sampleSize);
	}
	
	public Inferencer getInferencer(String paraFile) {
		Inferencer inf = new Inferencer();
		
		inf.setModel(this);
		inf.InitializeParameters();
		
		return inf;
	}
}
