package topic;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import types.Dirichlet;
import types.Document;
import types.IDocument;
import types.User;
import utils.FileUtil;
import utils.Randoms;
import gnu.trove.TIntIntHashMap;

public class TweetEstimator extends TweetModel implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public TIntIntHashMap[] wordTopicCounts;
	public TIntIntHashMap[] citationTopicCounts;
	public TIntIntHashMap[] docTopicCounts;
	public int[] wordsPerTopic;
	public int[] citationsPerTopic;
	public int[][] tweetTopicAssignments;
	public int[][] citationTopicAssignments;
	
	public boolean[][][] isWordBackground; //doc sentence word
	public double[] backgroundCount; //0: background 1: not background
	
	static public boolean hasBackGroundTopic = true;
	
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

		wordsPerTopic = new int[numTopics + 1];
		citationsPerTopic = new int[numTopics + 1];

		tweetTopicAssignments = new int[numDocs][];
		isWordBackground = new boolean[numDocs][][];
		for (int d = 0; d < numDocs; d++) {
			User doc = (User) corpus.getDoc(d);
			tweetTopicAssignments[d] = new int[doc.numTweets];
			isWordBackground[d] = new boolean[doc.numTweets][];
			for(int t = 0; t < doc.numTweets; t++)
				isWordBackground[d][t] = new boolean[doc.wordSequence[t].length];
		}

		citationTopicAssignments = new int[numDocs][];
		for (int d = 0; d < numDocs; d++) {
			IDocument doc = corpus.getDoc(d);
			citationTopicAssignments[d] = new int[doc.numCitations];
		}
		
		backgroundCount = new double[2];
		Arrays.fill(backgroundCount, 0);
	}

	public void InitializeAssignments() {
		this.random = new Randoms();
		for (int d = 0; d < numDocs; d++) {
			User doc = (User) corpus.getDoc(d);

			if (doc.numWords != 0) {
				for(int t = 0; t < doc.numTweets; t++){
					int topic = r.nextInt(numTopics);
					tweetTopicAssignments[d][t] = topic;
					docTopicCounts[d].adjustOrPutValue(topic, 1, 1);
					
					int[] words = doc.wordSequence[t];
					for (int wPos = 0; wPos < words.length; wPos++) {
						if(!hasBackGroundTopic) {
							isWordBackground[d][t][wPos] = false;
						} else {
							isWordBackground[d][t][wPos] = r.nextDouble() > 0.5;
						}
						
						if(isWordBackground[d][t][wPos]) {
							backgroundCount[0]++;
							wordTopicCounts[words[wPos]].adjustOrPutValue(numTopics, 1, 1);
							wordsPerTopic[numTopics]++;
						} else {
							backgroundCount[1]++;
							wordTopicCounts[words[wPos]].adjustOrPutValue(topic, 1, 1);
							wordsPerTopic[topic]++;
						}
					}
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
	private void reComputeProbs(double[] p_topic, int[] pCount) {
		int max = pCount[0];
		// System.out.print(max + " ");
		for (int i = 1; i < pCount.length; i++) {
			if (pCount[i] > max)
				max = pCount[i];
			// System.out.print(pCount[i] + " ");
		}
		if (max > 0)
			FileUtil.print(p_topic, "previous: ", " ", "\n");
		for (int i = 0; i < pCount.length; i++) {
			p_topic[i] = p_topic[i] * Math.pow(1e150, pCount[i] - max);
		}
		if (max > 0) {
			System.out.print(pCount[0] + " ");
			for (int i = 1; i < pCount.length; i++) {
				System.out.print(pCount[i] + " ");
			}
			System.out.println();
			FileUtil.print(p_topic, "current: ", " ", "\n");
			// System.exit(0);
		}
	}

	private double isOverFlow(double buffer_P, int[] pCount, int a2) {
		if (buffer_P > 1e150) {
			pCount[a2]++;
			return buffer_P / 1e150;
		}
		if (buffer_P < 1e-150) {
			pCount[a2]--;
			return buffer_P * 1e150;
		}
		return buffer_P;
	}
	
	private short sampleTweetBak(int dPos, int t, int[] words) {
		int word;
		int w;

		double[] P_topic;
		int[] pCount;
		P_topic = new double[numTopics];
		pCount = new int[numTopics];

		HashMap<Integer, Integer> wordcnt = new HashMap<Integer, Integer>(); // store
		// the
		// topic
		// words
		// with
		// frequency
		int totalWords = 0; // total number of topic words

		for (w = 0; w < words.length; w++) {
			if (isWordBackground[dPos][t][w] == true) {
				totalWords++;
				word = words[w];
				if (!wordcnt.containsKey(word)) {
					wordcnt.put(word, 1);
				} else {
					int buffer_word_cnt = wordcnt.get(word) + 1;
					wordcnt.put(word, buffer_word_cnt);
				}
			}
		}
		
		User buffer_user = (User) corpus.getDoc(dPos);
		for (int a = 0; a < numTopics; a++) {
			P_topic[a] = (docTopicCounts[dPos].get(a) + alpha[a]) / (buffer_user.numTweets - 1 + alphaSum);

			double buffer_P = 1;

			// Set s = wordcnt.entrySet();
			// Iterator it = s.iterator();
			// while (it.hasNext()) {
			// Map.Entry m = (Map.Entry) it.next();
			// word = (Integer) m.getKey();
			// int buffer_cnt = (Integer) m.getValue();
			// for (int j = 0; j < buffer_cnt; j++) {
			// buffer_P *= (C_word[a][word] + beta_word[word] + j);
			// }
			// }
			//
			// for (int i = 0; i < totalWords; i++) {
			// buffer_P /= (countAllWord[a] + beta_word_sum + i);
			// }

			int i = 0;
			Set s = wordcnt.entrySet();
			Iterator it = s.iterator();
			while (it.hasNext()) {
				Map.Entry m = (Map.Entry) it.next();
				word = (Integer) m.getKey();
				int buffer_cnt = (Integer) m.getValue();
				for (int j = 0; j < buffer_cnt; j++) {
					double value = (double) (wordTopicCounts[word].get(a) + beta + j) / (wordsPerTopic[a] + betaSum + i);
					i++;
					buffer_P *= value;
					buffer_P = isOverFlow(buffer_P, pCount, a); // in case that
																// buffer_P is
																// too small
				}
			}

			P_topic[a] *= Math.pow(buffer_P, (double) 1);
		}

		reComputeProbs(P_topic, pCount);

		double randz = Math.random();

		double sum = 0;

		for (int a = 0; a < numTopics; a++) {
			sum += P_topic[a];
		}

		double thred = 0;

		short chosena = -1;

		for (short a = 0; a < numTopics; a++) {
			thred += P_topic[a] / sum;
			if (thred >= randz) {
				chosena = a;
				break;
			}
		}
		if (chosena == -1) {
			System.out.println("chosena equals -1, error!");
		}

		wordcnt.clear();
		return chosena;
	}
	
	public int sampleTweet(int dPos, int t, int[] words) {
		int oldTopic = tweetTopicAssignments[dPos][t];
		docTopicCounts[dPos].adjustOrPutValue(oldTopic, -1, 0);
		
		for (int wPos = 0; wPos < words.length; wPos++) {
			if(!isWordBackground[dPos][t][wPos]) {
				wordTopicCounts[words[wPos]].adjustOrPutValue(oldTopic, -1, 0);
				wordsPerTopic[oldTopic]--;
			}
		}	
		
		double[] topicDistribution = new double[numTopics];
		double topicDistributionSum = 0;
		for (int k = 0; k < numTopics; k++) {
			double weight = ((docTopicCounts[dPos].get(k) + alpha[k]));
			for (int wPos = 0; wPos < words.length; wPos++) {
				TIntIntHashMap currentWordTopicCounts = wordTopicCounts[words[wPos]];
				
				weight *= ((currentWordTopicCounts.get(k) + beta) / (wordsPerTopic[k] + betaSum + wPos));
			}
			topicDistributionSum += weight;
			topicDistribution[k] = weight;
		}

		int newTopic = random.nextDiscrete(topicDistribution,
				topicDistributionSum);
		
		tweetTopicAssignments[dPos][t] = newTopic;
		docTopicCounts[dPos].adjustOrPutValue(newTopic, 1, 1);
		for (int wPos = 0; wPos < words.length; wPos++) {
			if(!isWordBackground[dPos][t][wPos]) {
				wordTopicCounts[words[wPos]].adjustOrPutValue(newTopic, 1, 1);
				wordsPerTopic[newTopic]++;
			}
		}
		
		return newTopic;
	}
	
	public void sampleWord(int dPos, int t, int wPos, int word, int tweetTopic) {
		if(isWordBackground[dPos][t][wPos]) {
			backgroundCount[0]--;
			wordTopicCounts[word].adjustOrPutValue(numTopics, -1, 0);
			wordsPerTopic[numTopics]--;
		} else {
			backgroundCount[1]--;
			wordTopicCounts[word].adjustOrPutValue(tweetTopic, -1, 0);
			wordsPerTopic[tweetTopic]--;
		}
		
		double[] P_lv;
		P_lv = new double[2];
		double Pb = 1;
		double Ptopic = 1;

		P_lv[0] = (backgroundCount[0] + delta)
				/ (backgroundCount[0] + backgroundCount[1] + deltaSum); 

		P_lv[1] = (backgroundCount[1] + delta)
				/ (backgroundCount[0] + backgroundCount[1] + deltaSum);
		
		Pb = (wordTopicCounts[word].get(numTopics) + betaB)
				/ (wordsPerTopic[numTopics] + betaBSum); // word in background part(2)
		Ptopic = (wordTopicCounts[word].get(tweetTopic) + beta)
				/ (wordsPerTopic[tweetTopic] + betaSum);

		double p0 = Pb * P_lv[0];
		double p1 = Ptopic * P_lv[1];

		double sum = p0 + p1;
		double randPick = Math.random();

		if (randPick <= p0 / sum) {
			isWordBackground[dPos][t][wPos] = false;
		} else {
			isWordBackground[dPos][t][wPos] = true;
		}
		
		if(isWordBackground[dPos][t][wPos]) {
			backgroundCount[0]++;
			wordTopicCounts[word].adjustOrPutValue(numTopics, 1, 1);
			wordsPerTopic[numTopics]++;
		} else {
			backgroundCount[1]++;
			wordTopicCounts[word].adjustOrPutValue(tweetTopic, 1, 1);
			wordsPerTopic[tweetTopic]++;
		}
	}
	
	public void sampleOneDocument(User doc, int dPos) {
		if (doc.numWords != 0) {
			for(int t = 0; t < doc.numTweets; t++){
				int[] words = doc.wordSequence[t];
				int newTopic = sampleTweet(dPos, t, words);
				
				if(hasBackGroundTopic) {
					for(int wPos = 0; wPos < words.length; wPos++) {
						int word = words[wPos];
						sampleWord(dPos, t, wPos, word, newTopic);
					}
				}
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
				sampleOneDocument((User) corpus.getDoc(dPos), dPos);
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

		for (int k = 0; k <= numTopics; k++) {
			for (int wPos = 0; wPos < numUniqueWords; wPos++) {
				if (numSamples > 1)
					phi[k][wPos] *= (numSamples - 1);
				phi[k][wPos] += (wordTopicCounts[wPos].get(k) + beta) / (wordsPerTopic[k] + betaSum);

				if (numSamples > 1)
					phi[k][wPos] /= numSamples;
			}
		}

		for (int k = 0; k <= numTopics; k++) {
			for (int cPos = 0; cPos < numUniqueCitations; cPos++) {
				if (numSamples > 1)
					psi[k][cPos] *= (numSamples - 1);
				psi[k][cPos] += (citationTopicCounts[cPos].get(k) + gamma) / (citationsPerTopic[k] + gammaSum);

				if (numSamples > 1)
					psi[k][cPos] /= numSamples;
			}
		}
		
		for (int y = 0; y < 2; y++) {
			if (numSamples > 1)
				rho[y] *= (numSamples - 1);
			rho[y] += (backgroundCount[y] + delta)
					/ (backgroundCount[0] + backgroundCount[1] + deltaSum);
			if (numSamples > 1)
				rho[y] /= numSamples;
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
