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
	
	
	
	public static void main(String[] args) throws IOException {
		//args = new String[]{"TagLDA", System.getProperty("user.dir") + "/data/"};
		String base = args[1];
		String name = "tagLDA";
		
		String outputDir = base + "/" + name + "/";
		String modelParas = base + "/modelParameters";
		
		// create output folder
		FileUtil.mkdir(new File(outputDir));
		
		String contentDataFile = base + "/train_user_tweets";
		String citationDataFile = base + "/train_user_celebrities";

		TagLDA tagLDA = new TagLDA();
		System.out.println("Reading Data.....");
		Corpus corpus = tagLDA.readData(contentDataFile, citationDataFile, 5000);
		System.out.println("Done");
		
		tagLDA.numTopics = 100;
		tagLDA.addCorpus(corpus);
		tagLDA.InitializeParameters(modelParas, tagLDA.numTopics);
		tagLDA.InitializeAssignments();

		tagLDA.estimate();
		
		String output = outputDir + "res." + name + "." + tagLDA.numIterations +".txt";
		String modelDir = outputDir + name + ".model." + tagLDA.numIterations;
		System.out.println("save model");
		tagLDA.write(new File(modelDir));
		
		PrintWriter out = new PrintWriter(new FileWriter(output, true));
		int topN = 40;
		
		System.out.println("# Topic_word");
		out.println("# Topic_word");
		tagLDA.printTopWords(out, topN, false);
		
		System.out.println("# Topic_citation");
		out.println("# Topic_citation");
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
