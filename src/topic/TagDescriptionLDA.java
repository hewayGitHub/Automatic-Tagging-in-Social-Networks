package topic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeSet;

import experiments.TagRecommendation;
import types.Corpus;
import types.Document;
import types.IDSorter;
import utils.FileUtil;


public class TagDescriptionLDA extends Model{
	private static final long serialVersionUID = 1L;
	
	public Corpus readData(String contentDir, String citationDir, int maxLine)
			throws IOException, FileNotFoundException {
		Corpus corpus = new Corpus();

		BufferedReader contentBR = null;
		try {
			contentBR = new BufferedReader(new InputStreamReader(new FileInputStream(new File(contentDir)), "UTF-8"));

			String line = null, items[], uid, words;
			int lineCount = 0;
			while ((line = contentBR.readLine()) != null) {
				if(lineCount++ > maxLine) break;
				items = line.split("\\t");

				if (items.length != 2)
					continue;

				uid = items[0];
				words = items[1];

				Document doc = new Document();
				doc.setDocName(uid);
				doc.addWords(words.split("\\s+"), Corpus.wordAlphabet);

				corpus.addDoc(doc);
			}
		} finally {
			contentBR.close();
		}

		BufferedReader citationBR = null;
		try {
			citationBR = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(citationDir)), "UTF-8"));

			String line = null, items[], uid, citations;
			int docID;
			int lineCount = 0;
			while ((line = citationBR.readLine()) != null) {
				if(lineCount++ > maxLine) break;
				items = line.split("\\t");

				if (items.length != 2)
					continue;

				uid = items[0];
				citations = items[1];

				if (Corpus.docNameAlphabet.contains(uid)) {
					docID = Corpus.docNameAlphabet.lookupIndex(uid);
					((Document)corpus.getDoc(docID)).addCitations(citations.split("\\s"), Corpus.citationAlphabet);
				}
			}
			
		} finally {
			citationBR.close();
		}

		System.out.println("Total Documents:" + corpus.numDocs);
		System.out.println("Total Word Size:" + Corpus.wordAlphabet.size());
		System.out.println("Total Citation Size:" + Corpus.citationAlphabet.size());
		return corpus;
	}
	
	public static void main(String[] args) throws IOException {
		//String rootDir = "D:\\twitter\\Twitter network\\tagLDA\\";
		String base = System.getProperty("user.dir") + "/data/";
		String name = "tagDesLDA";
		
		String outputDir = base + "/" + name + "/";
		String modelParas = base + "/modelParameters";
		
		// create output folder
		FileUtil.mkdir(new File(outputDir));
		
		String contentDataFile = base + "/train_user_tweets";
		String citationDataFile = base + "/train_user_follows_tokens";

		int iterations = 1000;
		String output = outputDir + "res." + name + "." + iterations +".txt";
		String modelDir = outputDir + name + "." + iterations + ".model";
		
		TagDescriptionLDA tagDesLDA = new TagDescriptionLDA();
		System.out.println("Reading Data.....");
		Corpus corpus = tagDesLDA.readData(contentDataFile, citationDataFile, 5000);
		System.out.println("Done");
		
		tagDesLDA.numTopics = 100;
		tagDesLDA.addCorpus(corpus);
		tagDesLDA.InitializeParameters(modelParas, tagDesLDA.numTopics);
		tagDesLDA.InitializeAssignments();
		
		tagDesLDA.numIterations = iterations;
		tagDesLDA.estimate();
		
		System.out.println("save model");
		tagDesLDA.write(new File(modelDir));
		
		PrintWriter out = new PrintWriter(new FileWriter(output, true));
		int topN = 40;
		
		System.out.println("# Topic_word");
		out.println("# Topic_word");
		tagDesLDA.printTopWords(out, topN, false);
		
		System.out.println("# Topic_citation");
		out.println("# Topic_word");
		tagDesLDA.printTopCitations(out, topN, false);
		
		System.out.println("# rec_word by IG, prob sum, prob max");
		ArrayList<TreeSet<IDSorter>> recWordsIG = TagRecommendation.recWordByIG(tagDesLDA, 40);
		TagRecommendation.printRecResult(tagDesLDA, tagDesLDA.wordAlphabet, recWordsIG, modelDir + ".rec.word.IG");
		ArrayList<TreeSet<IDSorter>> recWordsProbSum = TagRecommendation.recWordByProb(tagDesLDA, 40, true);
		TagRecommendation.printRecResult(tagDesLDA, tagDesLDA.wordAlphabet, recWordsProbSum, modelDir + ".rec.word.prob.sum");
		ArrayList<TreeSet<IDSorter>> recWordsProbMax = TagRecommendation.recWordByProb(tagDesLDA, 40, false);
		TagRecommendation.printRecResult(tagDesLDA, tagDesLDA.wordAlphabet, recWordsProbMax, modelDir + ".rec.word.prob.max");
		
		System.out.println("# rec_citation by IG, prob sum, prob max");
		ArrayList<TreeSet<IDSorter>> recCitationsIG = TagRecommendation.recCitationByIG(tagDesLDA, 40);
		TagRecommendation.printRecResult(tagDesLDA, tagDesLDA.citationAlphabet, recCitationsIG, modelDir + ".rec.citation.IG");
		ArrayList<TreeSet<IDSorter>> recCitationssProbSum = TagRecommendation.recWordByProb(tagDesLDA, 40, true);
		TagRecommendation.printRecResult(tagDesLDA, tagDesLDA.citationAlphabet, recCitationssProbSum, modelDir + ".rec.citation.prob.sum");
		ArrayList<TreeSet<IDSorter>> recCitationsProbMax = TagRecommendation.recWordByProb(tagDesLDA, 40, false);
		TagRecommendation.printRecResult(tagDesLDA, tagDesLDA.citationAlphabet, recCitationsProbMax, modelDir + ".rec.citation.prob.max");
		
		out.close();
	}
}
