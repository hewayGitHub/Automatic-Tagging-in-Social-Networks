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


public class StandardLDA extends Model{
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

				Document doc = new Document();
				doc.setDocName(uid);
				doc.addWords(words.split("\\s+"), Corpus.wordAlphabet);

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
		//String rootDir = "D:\\twitter\\Twitter network\\tagLDA\\";
		String base = System.getProperty("user.dir") + "/data/";
		String name = "StandardLDA";
		
		String outputDir = base + "/" + name + "/";
		String modelParas = base + "/modelParameters";
		
		// create output folder
		FileUtil.mkdir(new File(outputDir));
		
		String contentDataFile = base + "/train_user_tweets";

		int iterations = 1000;
		String output = outputDir + "res." + name + "." + iterations +".txt";
		String modelDir = outputDir + name + "." + iterations + ".model";
		
		StandardLDA lda = new StandardLDA();
		System.out.println("Reading Data.....");
		Corpus corpus = lda.readData(contentDataFile, 5000);
		System.out.println("Done");
		
		lda.numTopics = 100;
		lda.addCorpus(corpus);
		lda.InitializeParameters(modelParas, lda.numTopics);
		lda.InitializeAssignments();
		
		lda.numIterations = iterations;
		lda.estimate();
		
		System.out.println("save model");
		lda.write(new File(modelDir));
		
		PrintWriter out = new PrintWriter(new FileWriter(output, true));
		int topN = 40;
		
		System.out.println("# Topic_word");
		out.println("# Topic_word");
		lda.printTopWords(out, topN, false);
		
		System.out.println("# Topic_citation");
		out.println("# Topic_word");
		lda.printTopCitations(out, topN, false);
		
		System.out.println("# rec_word by IG, prob sum, prob max");
		ArrayList<TreeSet<IDSorter>> recWordsIG = TagRecommendation.recWordByIG(lda, 40);
		TagRecommendation.printRecResult(lda, lda.wordAlphabet, recWordsIG, modelDir + ".rec.word.IG");
		ArrayList<TreeSet<IDSorter>> recWordsProbSum = TagRecommendation.recWordByProb(lda, 40, true);
		TagRecommendation.printRecResult(lda, lda.wordAlphabet, recWordsProbSum, modelDir + ".rec.word.prob.sum");
		ArrayList<TreeSet<IDSorter>> recWordsProbMax = TagRecommendation.recWordByProb(lda, 40, false);
		TagRecommendation.printRecResult(lda, lda.wordAlphabet, recWordsProbMax, modelDir + ".rec.word.prob.max");
		
		out.close();
	}
}
