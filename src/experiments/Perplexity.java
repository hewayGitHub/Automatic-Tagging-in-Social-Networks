package experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;

import topic.Estimator;
import topic.LDA;
import topic.Model;
import types.Corpus;
import utils.FileUtil;


public class Perplexity {
	public HashMap<Integer, ArrayList<Integer>> getNFolds(int numFolds, int sum) {
		HashMap<Integer, ArrayList<Integer>> nFolds = new HashMap<Integer, ArrayList<Integer>>();
		
		int[] entries = new int[sum];
		for (int i = 0; i < sum; i++) {
			entries[i] = i;
		}
		
		// shuffle array
		Random r = new Random(21);
		for (int i = 0; i < sum; i++) {
			int rand = r.nextInt(entries.length - i) + i;
			int temp = entries[i];
			entries[i] = entries[rand];
			entries[rand] = temp;
		}
		
		for (int i = 0; i < entries.length; i++) {
			int whichSplit = i % numFolds;
			
			if(nFolds.containsKey(whichSplit)) {
				nFolds.get(whichSplit).add(entries[i]);
			} else {
				ArrayList<Integer> tempList = new ArrayList<Integer>();
				tempList.add(entries[i]);
				
				nFolds.put(whichSplit, tempList);
			}
		}
		
		return nFolds;
	}
	
	public static void sampleNumTopics(String dataDir, String modelName, String documentClassName, 
			String modelParasFile, String ordinaryWordFile) throws ClassNotFoundException, IOException {
		Corpus corpus = LDA.readCorpus(dataDir, modelName, documentClassName, ordinaryWordFile);
		
		String outputDir = dataDir + "/" + modelName + "/";
		FileUtil.mkdir(new File(outputDir));
		String perpFile = outputDir + modelName + ".sampleNumTopics";
		
		int maxNumTopics = 200, step = 10, workerNum = 0;
		Runnable workers[] = new Runnable[maxNumTopics/step + 1];
		for(int numTopics = 10; numTopics <= maxNumTopics; numTopics += step) {
			System.out.println("Init worker for numTopics: " + numTopics);
			workers[workerNum++] = new PerplexityRunner(modelName, modelParasFile, numTopics, corpus, perpFile);
		}
		
		ExecutorService exec = Executors.newCachedThreadPool();
		for(int i = 0; i < workerNum; i++) {
			exec.execute(workers[i]);
		}
		
		exec.shutdown();
	}
	
	static class PerplexityRunner implements Runnable{
		Model model = null;
		String perpFile = null;
		String modelParasFile = null;
		public PerplexityRunner(String modelName, String modelParasFile, int numTopics, 
				Corpus corpus, String perpFile) {
			Model model = new Estimator();
			model.setModelName(modelName);
			model.setCorpus(corpus);
			model.numTopics = numTopics;
			this.modelParasFile = modelParasFile;
			
			this.model = model;
			this.perpFile = perpFile;
		}
		
		@Override
		public void run() {
			model.InitializeParameters(modelParasFile);
			model.InitializeAssignments();
			model.estimate();
			
			PrintWriter outPerp = null;
			try {
				outPerp = new PrintWriter(new FileWriter(perpFile, true));
				outPerp.println(model.numTopics + "\t" + model.computePerplexity());
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(outPerp != null) {
					outPerp.close();
				}
			}
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
//		args = new String[]{System.getProperty("user.dir") + "/data/", "lda", 
//				"Document", "sampleNumTopicsModelParameters"};
		
		String dataDir = args[0];
		String modelName = args[1];
		String documentClassName = "types." + args[2];
		String modelParasFile = dataDir + args[3];
		
		String ordinaryWordFile = null;
		if(args.length > 4) ordinaryWordFile = dataDir + args[4];
		
		Perplexity.sampleNumTopics(dataDir, modelName, documentClassName, modelParasFile, ordinaryWordFile);
	}

}
