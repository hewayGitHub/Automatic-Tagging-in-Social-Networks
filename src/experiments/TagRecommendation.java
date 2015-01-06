package experiments;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import topic.Model;
import types.Alphabet;
import types.IDSorter;

public class TagRecommendation {
	public static Model readModel(String modelPath) throws Exception {
		Model model = Model.read(new File(modelPath));

		return model;
	}
	
	public static ArrayList<TreeSet<IDSorter>> recWordByIG(Model model, int topN) {
		double[] wordProbs = new double[model.numUniqueWords];
		for(int m = 0; m < model.numDocs; m++) {
			for(int k = 0; k < model.numTopics; k++) {
				for(int w = 0; w < model.numUniqueWords; w++) {
					wordProbs[w] += model.theta_train[m][k] * model.phi_train[k][w];
				}
			}
		}
		for(int i = 0; i < wordProbs.length; i++)
			wordProbs[i] /= model.numDocs;
		
		ArrayList<TreeSet<IDSorter>> recSortedWords = new ArrayList<TreeSet<IDSorter>>();

		for(int m = 0; m < model.numDocs; m++) {
			recSortedWords.add(recWordByIGForOne(model, m, topN, wordProbs));
		}
		
		return recSortedWords;
	}
	
	public static TreeSet<IDSorter> recWordByIGForOne(Model model, int dPos, int topN, double[] wordProbs) {
		double[] topicVector = model.theta_train[dPos];
		
		TreeSet<IDSorter> docSortedWords = new TreeSet<IDSorter>();

		// Collect counts
		int wordCount = 0;
		for (int word = 0; word < model.numUniqueWords; word++) {
			double pDocWord = 0;
			for (int k = 0; k < model.numTopics; k++) {
				pDocWord += topicVector[k] * model.phi_train[k][word];
			}
			pDocWord /= model.numDocs;
			pDocWord /= wordProbs[word];
			
			double pDocWordNeg = 0;
			for (int k = 0; k < model.numTopics; k++) {
				pDocWordNeg += topicVector[k] * (1 - model.phi_train[k][word]);
			}
			pDocWordNeg /= model.numDocs;
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

	public static ArrayList<TreeSet<IDSorter>> recWordByProb(Model model, int topN, boolean isSumNotMax) {
		ArrayList<TreeSet<IDSorter>> recSortedWords = new ArrayList<TreeSet<IDSorter>>(model.numTopics);

		for(int m = 0; m < model.numDocs; m++) {
			recSortedWords.add(recWordByProbForOne(model, m, topN, isSumNotMax));
		}
		
		return recSortedWords;
	}
	
	public static TreeSet<IDSorter> recWordByProbForOne(Model model, int docIndex, int topN, boolean isSumNotMax) {
		double[] topicVector = model.theta_train[docIndex];
		
		TreeSet<IDSorter> docSortedWords = new TreeSet<IDSorter>();

		// Collect counts
		int wordCount = 0;
		for (int word = 0; word < model.numUniqueWords; word++) {
			double prob = 0;
			for (int k = 0; k < model.numTopics; k++) {
				if(isSumNotMax)
					prob += topicVector[k] * model.phi_train[k][word];
				else {
					if(prob < topicVector[k] * model.phi_train[k][word]) {
						prob = topicVector[k] * model.phi_train[k][word];
					}
				}
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
	
	public static ArrayList<TreeSet<IDSorter>> recCitationByIG(Model model, int topN) {
		double[] citationProbs = new double[model.numUniqueCitations];
		for(int m = 0; m < model.numDocs; m++) {
			for(int k = 0; k < model.numTopics; k++) {
				for(int c = 0; c < model.numUniqueCitations; c++) {
					citationProbs[c] += model.theta_train[m][k] * model.psi_train[k][c];
				}
			}
		}
		for(int i = 0; i < citationProbs.length; i++)
			citationProbs[i] /= model.numDocs;
		
		ArrayList<TreeSet<IDSorter>> recSortedWords = new ArrayList<TreeSet<IDSorter>>(model.numTopics);

		for(int m = 0; m < model.numDocs; m++) {
			recSortedWords.add(recCitationByIGForOne(model, m, topN, citationProbs));
		}
		
		return recSortedWords;
	}
	
	public static TreeSet<IDSorter> recCitationByIGForOne(Model model, int docIndex, int topN, double[] citationProbs) {
		double[] topicVector = model.theta_train[docIndex];
		
		TreeSet<IDSorter> docSortedCitations = new TreeSet<IDSorter>();

		// Collect counts
		int citationCount = 0;
		for (int citation = 0; citation < model.numUniqueCitations; citation++) {
			double pDocCitation = 0;
			for (int k = 0; k < model.numTopics; k++) {
				pDocCitation += topicVector[k] * model.psi_train[k][citation];
			}
			pDocCitation /= model.numDocs;
			pDocCitation /= citationProbs[citation];
			
			double pDocCitationNeg = 0;
			for (int k = 0; k < model.numTopics; k++) {
				pDocCitationNeg += topicVector[k] * (1 - model.psi_train[k][citation]);
			}
			pDocCitationNeg /= model.numDocs;
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

	public static ArrayList<TreeSet<IDSorter>> recCitationByProb(Model model, int topN, boolean isSumNotMax) {
		ArrayList<TreeSet<IDSorter>> recSortedCitation = new ArrayList<TreeSet<IDSorter>>(model.numTopics);

		for(int m = 0; m < model.numDocs; m++) {
			recSortedCitation.add(recCitationByProbForOne(model, m, topN, isSumNotMax));
		}
		
		return recSortedCitation;
	}
	
	public static TreeSet<IDSorter> recCitationByProbForOne(Model model, int docIndex, int topN, boolean isSumNotMax) {
		double[] topicVector = model.theta_train[docIndex];
		
		TreeSet<IDSorter> docSortedCitaions = new TreeSet<IDSorter>();

		// Collect counts
		int citationCount = 0;
		for (int citation = 0; citation < model.numUniqueCitations; citation++) {
			double prob = 0;
			for (int k = 0; k < model.numTopics; k++) {
				if(isSumNotMax)
					prob += topicVector[k] * model.psi_train[k][citation];
				else {
					if(prob < topicVector[k] * model.psi_train[k][citation]) {
						prob = topicVector[k] * model.psi_train[k][citation];
					}
				}
			}
			
			if(citationCount < topN) {
				docSortedCitaions.add(new IDSorter(citation, prob));
				
				citationCount++;
			} else {
				IDSorter curMin = docSortedCitaions.first();
				if(curMin.getWeight() < prob) {
					docSortedCitaions.pollFirst();
					docSortedCitaions.add(new IDSorter(citation, prob));
				}
			}
			docSortedCitaions.add(new IDSorter(citation, prob));
		}
		
		return docSortedCitaions;
	}

	
	public static void printRecResult(Model model, Alphabet alphabet, ArrayList<TreeSet<IDSorter>> recResult, String resPath) throws IOException{
		PrintWriter resOut = new PrintWriter(new FileWriter(resPath));
		
		for (int m = 0; m < recResult.size(); m++) {
			Iterator<IDSorter> iterator = recResult.get(m).iterator();

			resOut.format("%s\t", model.docNameAlphabet.lookupObject(m));
			while (iterator.hasNext()) {
				IDSorter idCountPair = iterator.next();
				resOut.format("%s (%f) ",
						alphabet.lookupObject(idCountPair.getID()),
						idCountPair.getWeight());
			}
			
			resOut.println();
		}
		
		resOut.close();
	}
	
	public static void main(String[] args) throws Exception {
		String modelDir = "D:\\twitter\\Twitter network\\tagLDA\\tagDesLDA.model.1000";
		Model model = readModel(modelDir);
		ArrayList<TreeSet<IDSorter>> recWords = recWordByIG(model, 40);
		printRecResult(model, model.wordAlphabet, recWords, modelDir + ".rec.word");
	}

}
