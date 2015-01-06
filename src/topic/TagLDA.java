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


public class TagLDA extends Model{
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
		String name = "tagLDA";
		
		String outputDir = base + "/" + name + "/";
		String modelParas = base + "/modelParameters";
		
		// create output folder
		FileUtil.mkdir(new File(outputDir));
		
		String contentDataFile = base + "/train_user_tweets";
		String citationDataFile = base + "/train_user_follows";

		int iterations = 1000;
		String output = outputDir + "res." + name + "." + iterations +".txt";
		String modelDir = outputDir + name + "." + iterations + ".model";
		
		TagLDA tagLDA = new TagLDA();
		System.out.println("Reading Data.....");
		Corpus corpus = tagLDA.readData(contentDataFile, citationDataFile, 5000);
		System.out.println("Done");
		
		tagLDA.numTopics = 100;
		tagLDA.addCorpus(corpus);
		tagLDA.InitializeParameters(modelParas, tagLDA.numTopics);
		tagLDA.InitializeAssignments();
		
		tagLDA.numIterations = iterations;
		tagLDA.estimate();
		
		System.out.println("save model");
		tagLDA.write(new File(modelDir));
		
		PrintWriter out = new PrintWriter(new FileWriter(output, true));
		int topN = 40;
		
		System.out.println("# Topic_word");
		out.println("# Topic_word");
		tagLDA.printTopWords(out, topN, false);
		
		System.out.println("# Topic_citation");
		out.println("# Topic_word");
		tagLDA.printTopCitations(out, topN, false);
		
		System.out.println("# rec_word by IG, prob sum, prob max");
		ArrayList<TreeSet<IDSorter>> recWordsIG = TagRecommendation.recWordByIG(tagLDA, 40);
		TagRecommendation.printRecResult(tagLDA, tagLDA.wordAlphabet, recWordsIG, modelDir + ".rec.word.IG");
		ArrayList<TreeSet<IDSorter>> recWordsProbSum = TagRecommendation.recWordByProb(tagLDA, 40, true);
		TagRecommendation.printRecResult(tagLDA, tagLDA.wordAlphabet, recWordsProbSum, modelDir + ".rec.word.prob.sum");
		ArrayList<TreeSet<IDSorter>> recWordsProbMax = TagRecommendation.recWordByProb(tagLDA, 40, false);
		TagRecommendation.printRecResult(tagLDA, tagLDA.wordAlphabet, recWordsProbMax, modelDir + ".rec.word.prob.max");
		
		System.out.println("# rec_citation by IG, prob sum, prob max");
		ArrayList<TreeSet<IDSorter>> recCitationsIG = TagRecommendation.recCitationByIG(tagLDA, 40);
		TagRecommendation.printRecResult(tagLDA, tagLDA.citationAlphabet, recCitationsIG, modelDir + ".rec.citation.IG");
		ArrayList<TreeSet<IDSorter>> recCitationssProbSum = TagRecommendation.recWordByProb(tagLDA, 40, true);
		TagRecommendation.printRecResult(tagLDA, tagLDA.citationAlphabet, recCitationssProbSum, modelDir + ".rec.citation.prob.sum");
		ArrayList<TreeSet<IDSorter>> recCitationsProbMax = TagRecommendation.recWordByProb(tagLDA, 40, false);
		TagRecommendation.printRecResult(tagLDA, tagLDA.citationAlphabet, recCitationsProbMax, modelDir + ".rec.citation.prob.max");
		
		out.close();
	}
}
