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
	
	public static ArrayList<TreeSet<IDSorter>> recTokenByIG(Model model, int topN, String tokenType) {
		long startTime = System.currentTimeMillis();
		
		int numTokens = 0;
		double[][] topicProb = null;
		if(tokenType.equalsIgnoreCase("word")) {
			topicProb = model.phi;
			numTokens = model.numUniqueWords;
		} else {
			topicProb = model.psi;
			numTokens = model.numUniqueCitations;
		}
		
		double[] tokenProbs = new double[numTokens];
		for(int m = 0; m < model.numDocs; m++) {
			for(int k = 0; k < model.numTopics; k++) {
				for(int w = 0; w < numTokens; w++) {
					tokenProbs[w] += model.theta[m][k] * topicProb[k][w];
				}
			}
		}
		for(int i = 0; i < tokenProbs.length; i++)
			tokenProbs[i] /= model.numDocs;
		
		ArrayList<TreeSet<IDSorter>> recSortedTokens = new ArrayList<TreeSet<IDSorter>>();

		for(int m = 0; m < model.numDocs; m++) {
			if(m % 100 == 0)
				System.out.println("Rec word for doc #" + m);
			
			recSortedTokens.add(recTokenByIGForOne(model, m, topN, tokenProbs, tokenType));
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
		
		return recSortedTokens;
	}
	
	private static TreeSet<IDSorter> recTokenByIGForOne(Model model, int dPos, int topN, double[] wordProbs, String tokenType) {
		double[] topicVector = model.theta[dPos];
		
		TreeSet<IDSorter> docSortedWords = new TreeSet<IDSorter>();
		
		int numTokens = 0;
		double[][] topicProb = null;
		if(tokenType.equalsIgnoreCase("word")) {
			topicProb = model.phi;
			numTokens = model.numUniqueWords;
		} else {
			topicProb = model.psi;
			numTokens = model.numUniqueCitations;
		}
		
		// Collect counts
		int wordCount = 0;
		//Set<Integer> wordSet = model.getTopWordSet(topN * 5);
		for(int word = 0; word < numTokens; word++) {
			double pDocWord = 0;
			for (int k = 0; k < model.numTopics; k++) {
				pDocWord += topicVector[k] * topicProb[k][word];
			}
			pDocWord /= model.numDocs;
			pDocWord /= wordProbs[word];
			
			double pDocWordNeg = 0;
			for (int k = 0; k < model.numTopics; k++) {
				pDocWordNeg += topicVector[k] * (1 - topicProb[k][word]);
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

	public static ArrayList<TreeSet<IDSorter>> recTokenByProb(Model model, int topN, boolean isSumNotMax, String tokenType) {
		long startTime = System.currentTimeMillis();
		
		ArrayList<TreeSet<IDSorter>> recSortedTokens = new ArrayList<TreeSet<IDSorter>>(model.numTopics);

		for(int m = 0; m < model.numDocs; m++) {
			recSortedTokens.add(recTokenByProbForOne(model, m, topN, isSumNotMax, tokenType));
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
		return recSortedTokens;
	}
	
	private static TreeSet<IDSorter> recTokenByProbForOne(Model model, int docIndex, int topN, boolean isSumNotMax, String tokenType) {
		double[] topicVector = model.theta[docIndex];
		
		int numTokens = 0;
		double[][] topicProb = null;
		if(tokenType.equalsIgnoreCase("word")) {
			topicProb = model.phi;
			numTokens = model.numUniqueWords;
		} else {
			topicProb = model.psi;
			numTokens = model.numUniqueCitations;
		}
		
		TreeSet<IDSorter> docSortedWords = new TreeSet<IDSorter>();

		// Collect counts
		int wordCount = 0;
		//Set<Integer> wordSet = model.getTopWordSet(topN * 5);
		//for(int word: wordSet) {
		for(int word = 0; word < numTokens; word++) {
			double prob = 0;
			for (int k = 0; k < model.numTopics; k++) {
				if(isSumNotMax)
					prob += topicVector[k] * topicProb[k][word];
				else {
					if(prob < topicVector[k] * topicProb[k][word]) {
						prob = topicVector[k] * topicProb[k][word];
					}
				}
			}
			
			if(isSumNotMax) prob /= model.numTopics;
			
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
	
	public static void printRecResult(Alphabet docNameAlphabet, Alphabet alphabet, 
			ArrayList<TreeSet<IDSorter>> recResult, String resPath) throws IOException{
		PrintWriter resOut = new PrintWriter(new FileWriter(resPath));
		
		for (int m = 0; m < recResult.size(); m++) {
			Iterator<IDSorter> iterator = recResult.get(m).iterator();

			resOut.format("%s\t", docNameAlphabet.lookupObject(m));
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
		args = new String[]{ "data/lda/lda.model.1"};
		
		String modelFile = args[0];
		Model model = readModel(modelFile);
		
		System.out.println("# rec_word");
		ArrayList<TreeSet<IDSorter>> recWords = TagRecommendation.recTokenByIG(model, 30, "word");
		TagRecommendation.printRecResult(model.docNameAlphabet, model.wordAlphabet, recWords, modelFile + ".rec.word.ig");
		
		recWords = TagRecommendation.recTokenByProb(model, 30, false, "word");
		TagRecommendation.printRecResult(model.docNameAlphabet, model.wordAlphabet, recWords, modelFile + ".rec.word.sum");
		
		recWords = TagRecommendation.recTokenByProb(model, 30, true, "word");
		TagRecommendation.printRecResult(model.docNameAlphabet, model.wordAlphabet, recWords, modelFile + ".rec.word.max");
		
		System.out.println("done");
		
		System.out.println("# rec_citation");
		ArrayList<TreeSet<IDSorter>> recCitations = TagRecommendation.recTokenByIG(model, 30, "citation");
		TagRecommendation.printRecResult(model.docNameAlphabet, model.citationAlphabet, recCitations, modelFile + ".rec.citation.ig");
		
		recCitations = TagRecommendation.recTokenByProb(model, 30, false, "citation");
		TagRecommendation.printRecResult(model.docNameAlphabet, model.citationAlphabet, recCitations, modelFile + ".rec.citation.sum");
		
		recCitations = TagRecommendation.recTokenByProb(model, 30, true, "citation");
		TagRecommendation.printRecResult(model.docNameAlphabet, model.citationAlphabet, recCitations, modelFile + ".rec.citation.max");
		
		System.out.println("done");
	}

}
