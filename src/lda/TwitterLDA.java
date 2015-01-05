package lda;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;

public class TwitterLDA {
	
	static void importData(String dataPath, String outDir) throws UnsupportedEncodingException, FileNotFoundException {
		// Begin by importing documents from text to feature sequences
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Pipes: lowercase, tokenize, remove stopwords, map to features
		// pipeList.add( new CharSequenceLowercase() );
		pipeList.add(new CharSequence2TokenSequence(Pattern
				.compile("[\\p{L}\\p{N}-]+")));
		// pipeList.add( new TokenSequenceRemoveStopwords(new
		// File("stoplists/en.txt"), "UTF-8", false, false, false) );
		pipeList.add(new TokenSequence2FeatureSequence());

		InstanceList instances = new InstanceList(new SerialPipes(pipeList));

		Reader fileReader = new InputStreamReader(new FileInputStream(new File(dataPath)), "UTF-8");
		instances.addThruPipe(new CsvIterator(fileReader, Pattern
				.compile("^(\\S*)[\\s]*(.*)$"), 2, -1, 1)); // data, // label, // name
		
		instances.save(new File(outDir + "/instances"));
		
		Alphabet dataAlphabet = instances.getDataAlphabet();
		Alphabet nameAlpabet = instances.getAlphabet();
		
		dataAlphabet.dump(new PrintWriter(outDir + "/data_alphabet"));
		nameAlpabet.dump(new PrintWriter(outDir + "/name_alphabet"));
	}
	
	static void predictTag(ParallelTopicModel model, int minTagNum) {
		double[][] phi = model.computePhi();//[numTopics][numTypes]
		
		int instanceNum = model.getData().size();
		instanceNum = 10;
		for(int instanceID = 0; instanceID < instanceNum; instanceID++) {
			double[] topicProbs = model.getTopicProbabilities(instanceID);
			
			Set<IDSorter> tagSet = new TreeSet<IDSorter>();
			for(int type = 0; type < model.numTypes; type++) {
				double prob = 0.0;
				
				for(int topic = 0; topic < model.numTopics; topic++) {
					prob += topicProbs[topic] * phi[topic][type];
				}
				
				tagSet.add(new IDSorter(type, prob));
			}
			
			int limit = minTagNum;
			if (tagSet.size() < minTagNum) { limit = tagSet.size(); }
			
			Iterator<IDSorter> iterator = tagSet.iterator();
			StringBuilder sb = new StringBuilder();
			for (int i=0; i < limit; i++) {
				IDSorter info = iterator.next();
				sb.append(model.alphabet.lookupObject(info.getID()) + " ");
			}
			System.out.println(instanceID + "\t" 
					+ model.getData().get(instanceID).instance.getName() + "\t" + sb.toString());
		}
	}
	
	static void runLDA(String instancesPath, int numTopics, int nIters) throws IOException {
		InstanceList instances = InstanceList.load(new File(instancesPath));

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		//  Note that the first parameter is passed as the sum over topics, while
		//  the second is 
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 50.0, 0.01);

		model.addInstances(instances);

		// Use two parallel samplers, which each look at one half the corpus and combine
		//  statistics after every iteration.
		model.setNumThreads(6);

		// Run the model for 50 iterations and stop (this is for testing only, 
		//  for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(nIters);
		model.estimate();
		
		/*PrintWriter outPerp = new PrintWriter(new FileWriter("tweets/perp", true));
		outPerp.println(numTopics + "\t" + model.modelPerplexity());
		outPerp.close();*/
		System.out.println("perplixity: " + model.modelPerplexity());
		
		model.printDocumentTopics(new File("tweet-lda/doc_topic"));
		model.printTopWords(new File("tweet-lda/top_words"), 10, false);
		model.printTopicWordWeights(new File("tweet-lda/topic_word"));
		model.write(new File("tweet-lda/model"));

		// The data alphabet maps word IDs to strings
		Alphabet dataAlphabet = instances.getDataAlphabet();
		Formatter out = new Formatter(new StringBuilder(), Locale.US);

		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
		
		// Show top 5 words in topics with proportions for the first document
		for (int topic = 0; topic < numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
			
			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t", topic);
			int rank = 0;
			while (iterator.hasNext() && rank < 20) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
				rank++;
			}
			System.out.println(out);
		}
	}
	
	public static void main(String[] args) throws Exception {
		//importData("D:/twitter/Twitter network/test_follows", "follows");
		
		/*for(int numTopics = 10; numTopics < 200; numTopics += 10) {
			runLDA("tweets/instances", numTopics, 500);
		}*/
		
		runLDA("tweets/instances", 100, 100);
		
		/*ParallelTopicModel model = ParallelTopicModel.read(new File("tweet-lda/model"));
		//predictTag(model, 20);
		
		model.printTopicWordWeights(new File("tweet-lda/topic_word"));*/
	}

}
