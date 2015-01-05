package topic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import types.Alphabet;
import types.Corpus;
import types.Dirichlet;
import types.Document;
import types.IDSorter;
import types.LabelAlphabet;
import utils.Randoms;

import gnu.trove.TIntIntHashMap;

public class Model implements Serializable{
	private static final long serialVersionUID = 1L;

	public int numTopics;
	
	public int numDocs = 0;
	public int numUniqueWords;
	public int numUniqueCitations;
	
	public double[] alpha;
	public double alphaSum;
	public double beta;
	public double betaSum;
	public double gamma;
	public double gammaSum;
	
	public TIntIntHashMap[] wordTopicCounts;
	public TIntIntHashMap[] citationTopicCounts;
	public int[] wordsPerTopic;
	public int[] citationsPerTopic;
	
	public double[][] theta_train;
	public double[][] phi_train;
	public double[][] psi_train;
	
	public int numSamples = 0;
	public Random r = new Random();
	public Randoms random;
	// for dirichlet estimation
	public int[] docLengthCounts; // histogram of document sizes
	public int[][] topicDocCounts;
	
	public double curLoglikelihood;
	
	public double[] wordProbs;
	public double[] citationProbs;
	
	public void InitializeParameters(Corpus corpus, int numTopics) {
		this.numTopics = numTopics;
		numDocs = corpus.numDocs = Corpus.docNameAlphabet.size();
		numUniqueWords = corpus.numUniqueWords = Corpus.vocabulary.size();
		numUniqueCitations = corpus.numUniqueCitations = Corpus.citationAlphabet.size();
		
		alpha = new double[numTopics];
		alphaSum = 0;
		for (int i = 0; i < numTopics; i++) {
			alpha[i] = 50.0 / (double) numTopics;
			alphaSum += alpha[i];
		}
		
		beta = 0.01;
		betaSum = beta * numUniqueWords;
		gamma = 0.01;
		gammaSum = gamma * numUniqueCitations;
		
		wordTopicCounts = new TIntIntHashMap[numUniqueWords];
		for (int i = 0; i < numUniqueWords; i++)
			wordTopicCounts[i] = new TIntIntHashMap();
		
		citationTopicCounts = new TIntIntHashMap[numUniqueCitations];
		for (int i = 0; i < numUniqueCitations; i++)
			citationTopicCounts[i] = new TIntIntHashMap();
		
		wordsPerTopic = new int[numTopics];
		citationsPerTopic  = new int[numTopics];
		
		theta_train = new double[numDocs][numTopics]; // doc topic vector
		phi_train = new double[numUniqueWords][numTopics]; // topic-word distribution
		psi_train = new double[numUniqueCitations][numTopics]; //topic-citation distribution
	}

	public void InitializeAssignments(Corpus corpus, LabelAlphabet topicAlphabet) {
		// initilize word, and citation factors
		this.random = new Randoms();
		for (int doc_index = 0; doc_index < numDocs; doc_index++) {
			Document doc = corpus.getDoc(doc_index);
			doc.topicCounts = new TIntIntHashMap();
			
			if(doc.numWords != 0) {
				doc.wordTopicAssignment = new int[doc.numWords];
				int[] words = doc.wordSequence.getFeatures();
				for (int i = 0; i < words.length; i++) {
					int topic = r.nextInt(numTopics);
					if(i >= doc.numWords) {
						System.out.println();
					}
					doc.wordTopicAssignment[i] = topic;
					doc.topicCounts.adjustOrPutValue(topic, 1, 1);
					
					wordTopicCounts[words[i]].adjustOrPutValue(topic, 1, 1);
					wordsPerTopic[topic]++;
				}
			}
			
			if(doc.numCitations != 0) {
				doc.citationTopicAssignment = new int[doc.numCitations];
				int[] citations = doc.citationSequence.getFeatures();
				for (int t = 0; t < citations.length; t++) {
					int topic = r.nextInt(numTopics);
					doc.citationTopicAssignment[t] = topic;
					doc.topicCounts.adjustOrPutValue(topic, 1, 1);
					
					citationTopicCounts[citations[t]].adjustOrPutValue(topic, 1, 1);
					citationsPerTopic[topic]++;
				}	
			}
		}
	}

	public void sampleOneDocument(Corpus corpus, Document doc) {
		// decrement current sampling word
		if(doc.numWords != 0) {
			int[] words = doc.wordSequence.getFeatures();
			int[] topics = doc.wordTopicAssignment;
			for (int j = 0; j < words.length; j++) {
				doc.topicCounts.adjustOrPutValue(topics[j], -1, 0);
				wordTopicCounts[words[j]].adjustOrPutValue(topics[j], -1, 0);
				wordsPerTopic[topics[j]]--;
				
				TIntIntHashMap currentTypeTopicCounts = wordTopicCounts[words[j]];
				double[] topicDistribution = new double[numTopics];
				double topicDistributionSum = 0, weight = 0;
				for (int t = 0; t < numTopics; t++) {
					weight = ((currentTypeTopicCounts.get(t) + beta) / (wordsPerTopic[t] + betaSum))
							* ((doc.topicCounts.get(t) + alpha[t]));
					topicDistributionSum += weight;
					topicDistribution[t] = weight;
				}
	
				topics[j] = random.nextDiscrete(topicDistribution, topicDistributionSum);
	
				doc.wordTopicAssignment[j] = topics[j];
				wordTopicCounts[words[j]].adjustOrPutValue(topics[j], 1, 1);
				doc.topicCounts.adjustOrPutValue(topics[j], 1, 1);
				wordsPerTopic[topics[j]]++;
			}
		}
		
		if(doc.numCitations != 0) {
			int[] citations = doc.citationSequence.getFeatures();
			int[] topics = doc.citationTopicAssignment;
			for (int j = 0; j < citations.length; j++) {
				doc.topicCounts.adjustOrPutValue(topics[j], -1, 0);
				citationTopicCounts[citations[j]].adjustOrPutValue(topics[j], -1, 0);
				citationsPerTopic[topics[j]]--;
				
				TIntIntHashMap currentTypeTopicCounts = citationTopicCounts[citations[j]];
				double[] topicDistribution = new double[numTopics];
				double topicDistributionSum = 0, weight = 0;
				for (int t = 0; t < numTopics; t++) {
					weight = ((currentTypeTopicCounts.get(t) + gamma) / (citationsPerTopic[t] + gammaSum))
							* ((doc.topicCounts.get(t) + alpha[t]));
					topicDistributionSum += weight;
					topicDistribution[t] = weight;
				}
	
				topics[j] = random.nextDiscrete(topicDistribution, topicDistributionSum);
	
				doc.citationTopicAssignment[j] = topics[j];
				citationTopicCounts[citations[j]].adjustOrPutValue(topics[j], 1, 1);
				doc.topicCounts.adjustOrPutValue(topics[j], 1, 1);
				citationsPerTopic[topics[j]]++;
			}
		}
	}
	
	public void sampleCorpus(Corpus corpus, int iterations){
		for (int ite = 0; ite < iterations; ite++) {
			for (int i = 0; i < numDocs; i++) {
				sampleOneDocument(corpus, corpus.getDoc(i));
			}
			
			if(ite > 0 && ite % 10 == 0) {
				System.out.println("iteration: " + ite);
				
				/*curLoglikelihood = modelLogLikelihood(corpus);
				System.out.println("loglikelihood:" + curLoglikelihood);*/
			}
			
			if(ite > 200 && ite % 50 == 0) {
				estimateParameters(corpus);
			}
		}
		
		curLoglikelihood = modelLogLikelihood(corpus);
		System.out.println("loglikelihood:" + curLoglikelihood);
	}

	public void learnParameters(Corpus corpus) {
		docLengthCounts = new int[corpus.maxDocLen + 1];
		topicDocCounts = new int[numTopics][corpus.maxDocLen + 1];
		for (int d = 0; d < numDocs; d++) {
			Document doc = corpus.getDoc(d);

			docLengthCounts[doc.numWords]++;
			
			int topics[] = doc.topicCounts.keys();
			for (int topic: topics) {
				topicDocCounts[topic][doc.topicCounts.get(topic)]++;
			}
		}
		
		alphaSum = Dirichlet.learnParameters(alpha, topicDocCounts, docLengthCounts);
	}

	public void estimateParameters(Corpus corpus) {
		// estimate parameters after every k iterations after burn-in
		numSamples++;

		for (int m = 0; m < numDocs; m++) {
			Document doc = corpus.getDoc(m);
			
			for (int k = 0; k < numTopics; k++) {
				if (numSamples > 1) theta_train[m][k] *= (numSamples - 1);
				theta_train[m][k] += (alpha[k] / (doc.numWords + doc.numCitations + alphaSum));
			}
			
			if(doc.numWords != 0) {
				int[] topics = doc.wordTopicAssignment;
				for (int i = 0; i < topics.length; i++) {
					theta_train[m][topics[i]] += (1.0 / (doc.numWords + doc.numCitations + alphaSum));
				}
			}
			
			if(doc.numCitations != 0) {
				int[] topics = doc.citationTopicAssignment;
				for (int i = 0; i < topics.length; i++) {
					theta_train[m][topics[i]] += (1.0 / (doc.numWords + doc.numCitations + alphaSum));
				}
			}
			
			if (numSamples > 1) {
				for (int k = 0; k < numTopics; k++) {
					theta_train[m][k] /= numSamples;
				}
			}
		}
		
		for (int k = 0; k < numUniqueWords; k++) {
			for (int i = 0; i < numTopics; i++) {
				if (numSamples > 1)
					phi_train[k][i] *= (numSamples - 1);
				phi_train[k][i] += ((wordTopicCounts[k].get(i) + beta) / (wordsPerTopic[i] + betaSum));
				if (numSamples > 1)
					phi_train[k][i] /= numSamples;
			}
		}
		for (int k = 0; k < numUniqueCitations; k++) {
			for (int i = 0; i < numTopics; i++) {
				if (numSamples > 1)
					psi_train[k][i] *= (numSamples - 1);
				psi_train[k][i] += ((citationTopicCounts[k].get(i) + gamma) / (citationsPerTopic[i] + gammaSum));
				if (numSamples > 1)
					psi_train[k][i] /= (numSamples);
			}
		}
	}

	public void estimateParameters_single(Corpus corpus) {
		// estimate parameters after every k iterations after burn-in
		numSamples++;
		for (int m = 0; m < numDocs; m++) {
			Document doc = corpus.getDoc(m);
			
			for (int k = 0; k < numTopics; k++) {
				theta_train[m][k] = (alpha[k] / (doc.numWords + doc.numCitations + alphaSum));
			}
			
			if(doc.numWords != 0) {
				int[] topics = doc.wordTopicAssignment;
				for (int i = 0; i < topics.length; i++) {
					theta_train[m][topics[i]] += (1.0 / (doc.numWords + doc.numCitations + alphaSum));
				}
			}
			
			if(doc.numCitations != 0) {
				int[] topics = doc.citationTopicAssignment;
				for (int i = 0; i < topics.length; i++) {
					theta_train[m][topics[i]] += (1.0 / (doc.numWords + doc.numCitations + alphaSum));
				}
			}
		}
		
		for (int k = 0; k < numUniqueWords; k++) {
			for (int i = 0; i < numTopics; i++) {
				phi_train[k][i] = ((wordTopicCounts[k].get(i) + beta) / (wordsPerTopic[i] + betaSum));
			}
		}
		for (int k = 0; k < numUniqueCitations; k++) {
			for (int i = 0; i < numTopics; i++) {
				psi_train[k][i] += ((citationTopicCounts[k].get(i) + gamma) / (citationsPerTopic[i] + gammaSum));
			}
		}
	}

	/**
	 * Return an array of sorted sets (one set per topic). Each set contains
	 * IDSorter objects with integer keys into the alphabet. To get direct
	 * access to the Strings, use getTopWords().
	 */
	public ArrayList<TreeSet<IDSorter>> getSortedWords(Corpus corpus, int topN) {
		ArrayList<TreeSet<IDSorter>> topicSortedWords = new ArrayList<TreeSet<IDSorter>>(numTopics);

		// Initialize the tree sets
		for (int topic = 0; topic < numTopics; topic++) {
			topicSortedWords.add(new TreeSet<IDSorter>());
		}

		// Collect counts
		int[] wordCount = new int[numTopics];
		for (int word = 0; word < numUniqueWords; word++) {
			for (int k = 0; k < numTopics; k++) {
				if(wordCount[k] < topN) {
					topicSortedWords.get(k).add(new IDSorter(word, phi_train[word][k]));
					
					wordCount[k]++;
				} else {
					IDSorter curMin = topicSortedWords.get(k).first();
					if(curMin.getWeight() < phi_train[word][k]) {
						topicSortedWords.get(k).pollFirst();
						topicSortedWords.get(k).add(new IDSorter(word, phi_train[word][k]));
					}
				}
			}
		}

		return topicSortedWords;
	}

	public ArrayList<TreeSet<IDSorter>> getSortedCitations(Corpus corpus, int topN) {
		ArrayList<TreeSet<IDSorter>> topicSortedCitation = new ArrayList<TreeSet<IDSorter>>(numTopics);

		// Initialize the tree sets
		for (int topic = 0; topic < numTopics; topic++) {
			topicSortedCitation.add(new TreeSet<IDSorter>());
		}

		// Collect counts
		int[] citationCount = new int[numTopics];
		for (int citation = 0; citation < numUniqueCitations; citation++) {
			for (int k = 0; k < numTopics; k++) {
				if(citationCount[k] < topN) {
					topicSortedCitation.get(k).add(new IDSorter(citation, psi_train[citation][k]));
					
					citationCount[k]++;
				} else {
					IDSorter curMin = topicSortedCitation.get(k).first();
					if(curMin.getWeight() < phi_train[citation][k]) {
						topicSortedCitation.get(k).pollFirst();
						topicSortedCitation.get(k).add(new IDSorter(citation, psi_train[citation][k]));
					}
				}
			}
		}

		return topicSortedCitation;
	}

	public double testPerplexity(int numSamples, Corpus corpus) {
		double logSum = 0;
		int sampleSize = 0;

		for (int m = 0; m < numDocs; m++) {
			Document doc = corpus.getDoc(m);
			sampleSize += doc.numWords;
			
			TIntIntHashMap wordCounts = doc.wordCounts;
			int[] keys = wordCounts.keys();
			for (int i = 0; i < keys.length; i++) {
				double probWord = 0.0;
				for (int t = 0; t < numTopics; t++) {
					probWord += theta_train[m][t] * phi_train[keys[i]][t];
				}
				logSum += (Math.log(probWord) * wordCounts.get(keys[i]));
			}

		}
		
		return Math.exp(-1 * logSum / sampleSize);
	}

	public double modelLogLikelihood(Corpus corpus) {
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

		for (int m = 0; m < numDocs; m++) {
			Document doc = corpus.getDoc(m);
			sampleSize += doc.numWords;

			for(int topic: doc.topicCounts.keys()) {
				int count = doc.topicCounts.get(topic);
				if(count > 0)
					logLikelihood += (Dirichlet.logGammaStirling(alpha[topic] + count) - topicLogGammas[topic]);
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGammaStirling(alphaSum + doc.numWords);
		}

		// add the parameter sum term
		logLikelihood += numDocs * Dirichlet.logGammaStirling(alphaSum);

		// And the topics
		// Count the number of type-topic pairs
		int nonZeroTypeTopics = 0;

		for (int word = 0; word < numUniqueWords; word++) {
			for (int topic : wordTopicCounts[word].keys()) {
				int count = wordTopicCounts[word].get(topic);
				if (count > 0) {
					nonZeroTypeTopics++;
					logLikelihood += Dirichlet.logGammaStirling(beta + count);
				}
			}
		}

		for (int topic = 0; topic < numTopics; topic++) {
			logLikelihood -= Dirichlet.logGammaStirling((beta * numTopics) + wordsPerTopic[topic]);
		}

		logLikelihood += (Dirichlet.logGammaStirling(beta * numTopics)) - (Dirichlet.logGammaStirling(beta) * nonZeroTypeTopics);

		return Math.exp(-1 * logLikelihood / sampleSize);
		//return logLikelihood;
	}
	
	public TreeSet<IDSorter> recommendWordIG(int docIndex, int topN) {
		double[] topicVector = theta_train[docIndex];
		
		TreeSet<IDSorter> docSortedWords = new TreeSet<IDSorter>();

		// Collect counts
		int wordCount = 0;
		for (int word = 0; word < numUniqueWords; word++) {
			double pDocWord = 0;
			for (int k = 0; k < numTopics; k++) {
				pDocWord += topicVector[k] * phi_train[word][k];
			}
			pDocWord /= numDocs;
			pDocWord /= wordProbs[word];
			
			double pDocWordNeg = 0;
			for (int k = 0; k < numTopics; k++) {
				pDocWordNeg += topicVector[k] * (1 - phi_train[word][k]);
			}
			pDocWordNeg /= numDocs;
			pDocWordNeg /= (1 - wordProbs[word]);
			
			double ig = wordProbs[word] * (pDocWord*Math.log(pDocWord) + (1-pDocWord)*Math.log(1-pDocWord))
					+ (1-wordProbs[word]) * (pDocWordNeg*Math.log(pDocWordNeg) + (1-pDocWordNeg)*Math.log(1-pDocWordNeg));
			if(wordCount < topN) {
				docSortedWords.add(new IDSorter(word, ig));
				
				wordCount++;
			} else {
				IDSorter curMin = docSortedWords.first();
				if(curMin.getWeight() < ig) {
					docSortedWords.pollFirst();
					docSortedWords.add(new IDSorter(word, ig));
				}
			}
		}
		
		return docSortedWords;
	}
	
	public ArrayList<TreeSet<IDSorter>> recommendWordIG(int topN) {
		wordProbs = new double[numUniqueWords];
		for(int m = 0; m < numDocs; m++) {
			for(int k = 0; k < numTopics; k++) {
				for(int w = 0; w < numUniqueWords; w++) {
					wordProbs[w] += theta_train[m][k] * phi_train[w][k];
				}
			}
		}
		for(int i = 0; i < wordProbs.length; i++)
			wordProbs[i] /= numDocs;
		
		ArrayList<TreeSet<IDSorter>> recSortedWords = new ArrayList<TreeSet<IDSorter>>(numTopics);

		for(int m = 0; m < numDocs; m++) {
			recSortedWords.add(recommendWordIG(m, topN));
		}
		
		return recSortedWords;
	}
	
	public TreeSet<IDSorter> recommendWord(int docIndex, int topN) {
		double[] topicVector = theta_train[docIndex];
		
		TreeSet<IDSorter> docSortedWords = new TreeSet<IDSorter>();

		// Collect counts
		int wordCount = 0;
		for (int word = 0; word < numUniqueWords; word++) {
			double prob = 0;
			for (int k = 0; k < numTopics; k++) {
				prob += topicVector[k] * phi_train[word][k];
				/*if(prob < topicVector[k] * phi_train[word][k]) {
					prob = topicVector[k] * phi_train[word][k];
				}*/
			}
			
			if(wordCount < topN) {
				docSortedWords.add(new IDSorter(word, prob));
				
				wordCount++;
			} else {
				IDSorter curMin = docSortedWords.first();
				if(curMin.getWeight() < prob) {
					docSortedWords.pollFirst();
					docSortedWords.add(new IDSorter(word, prob));
				}
			}
		}
		
		return docSortedWords;
	}
	
	public ArrayList<TreeSet<IDSorter>> recommendWord(int topN) {
		ArrayList<TreeSet<IDSorter>> recSortedWords = new ArrayList<TreeSet<IDSorter>>(numTopics);

		for(int m = 0; m < numDocs; m++) {
			recSortedWords.add(recommendWordIG(m, topN));
		}
		
		return recSortedWords;
	}
	
	public TreeSet<IDSorter> recommendCitationIG(int docIndex, int topN) {
		double[] topicVector = theta_train[docIndex];
		
		TreeSet<IDSorter> docSortedCitations = new TreeSet<IDSorter>();

		// Collect counts
		int citationCount = 0;
		for (int citation = 0; citation < numUniqueCitations; citation++) {
			double pDocCitation = 0;
			for (int k = 0; k < numTopics; k++) {
				pDocCitation += topicVector[k] * psi_train[citation][k];
			}
			pDocCitation /= numDocs;
			pDocCitation /= citationProbs[citation];
			
			double pDocCitationNeg = 0;
			for (int k = 0; k < numTopics; k++) {
				pDocCitationNeg += topicVector[k] * (1 - psi_train[citation][k]);
			}
			pDocCitationNeg /= numDocs;
			pDocCitationNeg /= (1 - citationProbs[citation]);
			
			double ig = citationProbs[citation] * (pDocCitation*Math.log(pDocCitation) + (1-pDocCitation)*Math.log(1-pDocCitation))
					+ (1-citationProbs[citation]) * (pDocCitationNeg*Math.log(pDocCitationNeg) + (1-pDocCitationNeg)*Math.log(1-pDocCitationNeg));
			if(citationCount < topN) {
				docSortedCitations.add(new IDSorter(citation, ig));
				
				citationCount++;
			} else {
				IDSorter curMin = docSortedCitations.first();
				if(curMin.getWeight() < ig) {
					docSortedCitations.pollFirst();
					docSortedCitations.add(new IDSorter(citation, ig));
				}
			}
		}
		
		return docSortedCitations;
	}
	
	public ArrayList<TreeSet<IDSorter>> recommendCitationIG(int topN) {
		citationProbs = new double[numUniqueCitations];
		for(int m = 0; m < numDocs; m++) {
			for(int k = 0; k < numTopics; k++) {
				for(int c = 0; c < numUniqueCitations; c++) {
					citationProbs[c] += theta_train[m][k] * phi_train[c][k];
				}
			}
		}
		for(int i = 0; i < citationProbs.length; i++)
			citationProbs[i] /= numDocs;
		
		ArrayList<TreeSet<IDSorter>> recSortedWords = new ArrayList<TreeSet<IDSorter>>(numTopics);

		for(int m = 0; m < numDocs; m++) {
			recSortedWords.add(recommendCitationIG(m, topN));
		}
		
		return recSortedWords;
	}
	
	public TreeSet<IDSorter> recommendCitation(int docIndex, int topN) {
		double[] topicVector = theta_train[docIndex];
		
		TreeSet<IDSorter> docSortedCitaions = new TreeSet<IDSorter>();

		// Collect counts
		int citationCount = 0;
		for (int citaion = 0; citaion < numUniqueCitations; citaion++) {
			double prob = 0;
			for (int k = 0; k < numTopics; k++) {
				prob += topicVector[k] * psi_train[citaion][k];
			}
			
			if(citationCount < topN) {
				docSortedCitaions.add(new IDSorter(citaion, prob));
				
				citationCount++;
			} else {
				IDSorter curMin = docSortedCitaions.first();
				if(curMin.getWeight() < prob) {
					docSortedCitaions.pollFirst();
					docSortedCitaions.add(new IDSorter(citaion, prob));
				}
			}
			docSortedCitaions.add(new IDSorter(citaion, prob));
		}
		
		return docSortedCitaions;
	}
	
	public ArrayList<TreeSet<IDSorter>> recommendCitation(int topN) {
		ArrayList<TreeSet<IDSorter>> recSortedCitation = new ArrayList<TreeSet<IDSorter>>(numTopics);

		for(int m = 0; m < numDocs; m++) {
			recSortedCitation.add(recommendCitationIG(m, topN));
		}
		
		return recSortedCitation;
	}
	
	public Alphabet vocabulary = Corpus.vocabulary;
	public Alphabet citationAlphabet = Corpus.citationAlphabet;
	public Alphabet docNameAlphabet = Corpus.docNameAlphabet; // save doc name
	public void writeObject(ObjectOutputStream out) throws IOException {
		out.writeLong(serialVersionUID);
		
		out.writeObject(vocabulary);
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
		out.writeObject(wordTopicCounts);
		out.writeObject(citationTopicCounts);
		out.writeObject(wordsPerTopic);
		out.writeObject(citationsPerTopic);
		out.writeObject(theta_train);
		out.writeObject(phi_train);
		out.writeObject(psi_train);
		out.writeInt(numSamples);
		out.writeObject(r);
		out.writeObject(random);
		out.writeObject(docLengthCounts);
		out.writeObject(topicDocCounts);
		out.writeDouble(curLoglikelihood);
   }

   public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
	   Long version = in.readLong();
	   if(version < serialVersionUID) throw new RuntimeException("Serial version is out of date");
	   
	   vocabulary = (Alphabet) in.readObject();
	   citationAlphabet = (Alphabet) in.readObject();
	   docNameAlphabet = (Alphabet) in.readObject();
	   
	   numTopics = in.readInt();
	   numDocs = in.readInt();
	   numUniqueWords = in.readInt();
	   numUniqueCitations = in.readInt();
	   alpha = (double[])in.readObject();
	   alphaSum = in.readDouble();
	   beta = in.readDouble();
	   betaSum = in.readDouble();
	   gamma = in.readDouble();
	   gammaSum = in.readDouble();
	   wordTopicCounts = (TIntIntHashMap[])in.readObject();
	   citationTopicCounts = (TIntIntHashMap[])in.readObject();
	   wordsPerTopic = (int[])in.readObject();
	   citationsPerTopic = (int[])in.readObject();
	   theta_train = (double[][])in.readObject();
	   phi_train = (double[][])in.readObject();
	   psi_train = (double[][])in.readObject();
	   numSamples = in.readInt();
	   r = (Random)in.readObject();
	   random = (Randoms)in.readObject();
	   docLengthCounts = (int[])in.readObject();
	   topicDocCounts = (int[][])in.readObject();
	   //curLoglikelihood = in.readDouble();
   }
}
