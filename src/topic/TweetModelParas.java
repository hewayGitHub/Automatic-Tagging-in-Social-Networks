package topic;

import java.util.ArrayList;
import java.util.Arrays;

import utils.FileUtil;

public enum TweetModelParas {
	numTopics, alpha, beta, betaB, delta, gamma, numIterations, burninPeriod, 
	printLogLikelihood, numTopicWord;
	
	public static void setModelPara(String paraFile, TweetModel model) {
		ArrayList<String> inputlines = new ArrayList<String>();
		FileUtil.readLines(paraFile, inputlines);
		for (int i = 0; i < inputlines.size(); i++) {
			int index = inputlines.get(i).indexOf(":");
			String para = inputlines.get(i).substring(0, index).trim();
			String value = inputlines.get(i)
					.substring(index + 1, inputlines.get(i).length()).trim();
			switch (TweetModelParas.valueOf(para)) {
			case numTopics:
				model.numTopics = Integer.parseInt(value);
				break;
			case alpha:
				if(model.numTopics != 0) {
					model.alpha = new double[model.numTopics];
				} else {
					throw new RuntimeException("In modelParameters: numTopics should be the first parameters");
				}
				
				Arrays.fill(model.alpha, Double.parseDouble(value));
				break;
			case beta:
				model.beta = Double.parseDouble(value);
				break;
			case betaB:
				model.betaB = Double.parseDouble(value);
				break;
			case gamma:
				model.gamma = Double.parseDouble(value);
				break;
			case delta:
				model.delta = Double.parseDouble(value);
				break;
			case numIterations:
				model.numIterations = Integer.parseInt(value);
				break;
			case burninPeriod:
				model.burninPeriod = Integer.parseInt(value);
				break;
			case printLogLikelihood:
				model.printLogLikelihood = Boolean.parseBoolean(value);
				break;
			case numTopicWord:
				model.numTopicWord = Integer.parseInt(value);
				break;
			default:
				break;
			}
		}
	}

	public static void setDefaultValue(TweetModel model) {
		model.numTopics = 100;
		model.alpha = new double[model.numTopics];
		Arrays.fill(model.alpha, 50.0 / model.numTopics);
		model.beta = 0.01;
		model.betaB = 0.01;
		model.gamma = 0.01;
		model.delta = 20;
		model.numIterations = 1000;
		model.burninPeriod = model.numIterations * 3 / 4;
		model.printLogLikelihood = false;
		model.numTopicWord = 20;
	}
}
