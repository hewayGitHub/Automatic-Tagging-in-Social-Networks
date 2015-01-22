package topic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeSet;

import experiments.TagRecommendation;
import types.Corpus;
import types.IDSorter;
import types.IDocument;
import types.OrdinaryWord;
import utils.FileUtil;

public class LDA {
	enum ModelType{
		lda, taglda, tagdeslda
	}
	
	@SuppressWarnings("unchecked")
	public static Corpus readCorpus(String dataDir, String modelName, 
			String documentClassName, String ordinaryWordFile) throws ClassNotFoundException {
		OrdinaryWord.init(ordinaryWordFile);
		
		Corpus corpus = null;
		int maxLine = 5000;
		String firstDelimeter = "\\t";
		String secondDelimeter = "\\s+";
		if(documentClassName == "User") secondDelimeter = "  ";
		
		System.out.println("Reading Data.....");
		if(modelName.startsWith("lda_follow")) {
			corpus = Corpus.readData((Class<? extends IDocument>) Class.forName(documentClassName), 
					dataDir + "train_user_celebrities_tokens", maxLine, firstDelimeter, secondDelimeter, true);
		} else if(modelName.startsWith("lda")) {
			corpus = Corpus.readData((Class<? extends IDocument>) Class.forName(documentClassName), 
					dataDir + "train_user_tweets", maxLine, firstDelimeter, secondDelimeter, true);
		} else if(modelName.startsWith("tagdeslda")) {
			corpus = Corpus.readData((Class<? extends IDocument>) Class.forName(documentClassName), 
					dataDir + "train_user_tweets", dataDir + "train_user_celebrities_tokens", 
					maxLine, firstDelimeter, secondDelimeter, true);
		} else if(modelName.startsWith("taglda")) {
			corpus = Corpus.readData((Class<? extends IDocument>) Class.forName(documentClassName), 
					dataDir + "train_user_tweets", dataDir + "train_user_celebrities", 
					maxLine, firstDelimeter, secondDelimeter, true);
		} else {
			System.out.println("wrong model name");
			System.exit(-1);
		}
		
		System.out.println("Done");
		
		return corpus;
	}
	
	public static Model run(String modelName, String modelParasFile, Corpus corpus) throws ClassNotFoundException {
		Model model = new Estimator();
		model.setModelName(modelName);
		
		model.setCorpus(corpus);
		
		model.InitializeParameters(modelParasFile);
		model.InitializeAssignments();
		model.estimate();
		
		return model;
	}
		
	public static void printModelResult(Model model, String dataDir) {
		String outputDir = dataDir + "/" + model.modelName + "/";
		FileUtil.mkdir(new File(outputDir));
		
		String modelFile = outputDir + model.modelName + ".model." + model.numIterations;
		System.out.println("save model");
		model.write(new File(modelFile));
		System.out.println("done");
		
		String outputFile = modelFile + ".res";
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(outputFile));
			
			if(model.numUniqueWords > 0) {
				System.out.println("# Topic_word");
				out.println("# Topic_word");
				model.printTopTokens(out, model.numTopicWord, false, "word");
				System.out.println("done");
				
				System.out.println("# Topic_word_distribution");
				out.println("# Topic_word_distribution");
				model.printTopicTokenVector(out, "word");
				System.out.println("done");
			}
			
			if(model.numUniqueCitations > 0) {
				System.out.println("# Topic_citation");
				out.println("# Topic_citation");
				model.printTopTokens(out, model.numTopicWord, false, "citation");
				System.out.println("done");
				
				System.out.println("# Topic_citation_distribution");
				out.println("# Topic_citation_distribution");
				model.printTopicTokenVector(out, "citation");
				System.out.println("done");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(out != null) {
				out.close();
			}
		}
		
		String docTopicFile = modelFile + ".doc.topic";
		System.out.println("# Doc_topic");
		model.printDocTopicVector(docTopicFile);
		System.out.println("done");
	}
	
	public static void printRecResult(Model model, String dataDir) throws IOException {
		String outputDir = dataDir + "/" + model.modelName + "/";
		FileUtil.mkdir(new File(outputDir));
		
		String modelFile = outputDir + model.modelName + ".model." + model.numIterations;
		
		if(model.numUniqueWords > 0) {
	 		System.out.println("# rec_word");
			ArrayList<TreeSet<IDSorter>> recWords = TagRecommendation.recTokenByIG(model, 30, "word");
			TagRecommendation.printRecResult(model.docNameAlphabet, model.wordAlphabet, recWords, modelFile + ".rec.word.ig");
			
			recWords = TagRecommendation.recTokenByProb(model, 30, false, "word");
			TagRecommendation.printRecResult(model.docNameAlphabet, model.wordAlphabet, recWords, modelFile + ".rec.word.sum");
			
			recWords = TagRecommendation.recTokenByProb(model, 30, true, "word");
			TagRecommendation.printRecResult(model.docNameAlphabet, model.wordAlphabet, recWords, modelFile + ".rec.word.max");
			
			System.out.println("done");
		}
		
		if(model.numUniqueCitations > 0) {
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
	
	public static void main(String args[]) throws ClassNotFoundException, IOException {
		args = new String[]{System.getProperty("user.dir") + "/data/", "lda_follow", "Document", "trainModelParameters"};
		
		String dataDir = args[0];
		String modelName = args[1];
		String documentClassName = "types." + args[2];
		String modelParasFile = dataDir + args[3];
		
		String ordinaryWordFile = null;
		if(args.length > 4) ordinaryWordFile = dataDir + args[4];
		
		Corpus corpus = LDA.readCorpus(dataDir, modelName, documentClassName, ordinaryWordFile);
		Model model = LDA.run(modelName, modelParasFile, corpus);
		
		printModelResult(model, dataDir);
		printRecResult(model, dataDir);
	}
}
