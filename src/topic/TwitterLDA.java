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
import types.IDSorter;
import types.User;
import utils.FileUtil;


public class TwitterLDA extends TwitterModel{
	private static final long serialVersionUID = 1L;
	
	public Corpus readData(String contentDir, int maxLine)
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

				User doc = new User();
				doc.setDocName(uid);
				
				items = words.split("  ");
				String[][] tweets = new String[items.length][];
				int index = 0;
				for(String tweet: items) {
					tweets[index++] = tweet.split(" ");
				}

				doc.addWords(tweets, Corpus.wordAlphabet);

				corpus.addDoc(doc);
			}
		} finally {
			contentBR.close();
		}

		System.out.println("Total Documents:" + corpus.numDocs);
		System.out.println("Total Word Size:" + Corpus.wordAlphabet.size());
		System.out.println("Total Citation Size:" + Corpus.citationAlphabet.size());
		return corpus;
	}
	
	public static void main(String[] args) throws IOException {
		//args = new String[]{System.getProperty("user.dir") + "/data/", "true"};
		String base = args[0];
		
		String name;
		if(args[1] == "true") {
			name = "TwitterLDA";
		} else {
			name = "TwitterLDANoBG";
		}
		
		String outputDir = base + "/" + name + "/";
		String modelParas = base + "/modelParameters";
		
		// create output folder
		FileUtil.mkdir(new File(outputDir));
		
		String contentDataFile = base + "/train_user_tweets";
		
		TwitterLDA lda = new TwitterLDA();
		System.out.println("Reading Data.....");
		Corpus corpus = lda.readData(contentDataFile, 5000);
		System.out.println("Done");
		
		lda.numTopics = 100;
		lda.addCorpus(corpus);
		lda.InitializeParameters(modelParas, lda.numTopics);
		lda.InitializeAssignments();
		
		lda.estimate();
		
		String output = outputDir + "res." + name + "." + lda.numIterations +".txt";
		String modelDir = outputDir + name + ".model." + lda.numIterations;
		System.out.println("save model");
		lda.write(new File(modelDir));
		
		PrintWriter out = new PrintWriter(new FileWriter(output));
		int topN = 40;
		
		System.out.println("# Topic_word");
		out.println("# Topic_word");
		lda.printTopWords(out, topN, false);
		
		System.out.println("# Topic_citation");
		out.println("# Topic_citation");
		lda.printTopCitations(out, topN, false);
		
		out.close();
		
		System.out.println("# Text with label");
		lda.outputTweetWithLabel(outputDir);
		
		System.out.println("# rec_word by IG, prob sum, prob max");
		ArrayList<TreeSet<IDSorter>> recWordsIG = TagRecommendation.recWordByIG(lda, 40);
		TagRecommendation.printRecResult(lda, lda.wordAlphabet, recWordsIG, modelDir + ".rec.word.IG");
		ArrayList<TreeSet<IDSorter>> recWordsProbSum = TagRecommendation.recWordByProb(lda, 40, true);
		TagRecommendation.printRecResult(lda, lda.wordAlphabet, recWordsProbSum, modelDir + ".rec.word.prob.sum");
		ArrayList<TreeSet<IDSorter>> recWordsProbMax = TagRecommendation.recWordByProb(lda, 40, false);
		TagRecommendation.printRecResult(lda, lda.wordAlphabet, recWordsProbMax, modelDir + ".rec.word.prob.max");
	}
}
