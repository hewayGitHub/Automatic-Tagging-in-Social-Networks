package topic;

import java.io.Serializable;

import types.Document;
import types.IDocument;
import utils.Randoms;
import gnu.trove.TIntIntHashMap;

public class Inferencer extends Model implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Model trainModel = null;
	
	public TIntIntHashMap[] docTopicCounts;
	public int[][] wordTopicAssignments;
	public int[][] citationTopicAssignments;
	
	public void setModel(Model trainModel) {
		this.trainModel = trainModel;
	}
	
	public void InitializeParameters() {
		super.InitializeParameters();

		docTopicCounts = new TIntIntHashMap[numDocs];
		for (int d = 0; d < numDocs; d++)
			docTopicCounts[d] = new TIntIntHashMap();

		wordTopicAssignments = new int[numDocs][];
		for (int d = 0; d < numDocs; d++) {
			IDocument doc = corpus.getDoc(d);
			wordTopicAssignments[d] = new int[doc.numWords];
		}

		citationTopicAssignments = new int[numDocs][];
		for (int d = 0; d < numDocs; d++) {
			IDocument doc = corpus.getDoc(d);
			citationTopicAssignments[d] = new int[doc.numCitations];
		}
		
		if(trainModel == null) {
			throw new RuntimeException(
					"setModel() should be called first, since the model is empty now!");
		}
	}

	public void InitializeAssignments() {
		this.random = new Randoms();
		for (int d = 0; d < numDocs; d++) {
			Document doc = (Document) corpus.getDoc(d);

			if (doc.numWords != 0) {
				int[] words = doc.wordSequence;
				for (int wPos = 0; wPos < words.length; wPos++) {
					int topic = r.nextInt(numTopics);

					wordTopicAssignments[d][wPos] = topic;

					docTopicCounts[d].adjustOrPutValue(topic, 1, 1);
				}
			}

			if (doc.numCitations != 0) {
				int[] citations = doc.citationSequence;
				for (int cPos = 0; cPos < citations.length; cPos++) {
					int topic = r.nextInt(numTopics);
					citationTopicAssignments[d][cPos] = topic;

					docTopicCounts[d].adjustOrPutValue(topic, 1, 1);
				}
			}
		}
	}

	public void sampleOneDocument(Document doc, int dPos) {
		if (doc.numWords != 0) {
			int[] words = doc.wordSequence;
			int[] topics = wordTopicAssignments[dPos];
			for (int wPos = 0; wPos < words.length; wPos++) {
				docTopicCounts[dPos].adjustOrPutValue(topics[wPos], -1, 0);

				double[] topicDistribution = new double[trainModel.numTopics];
				double topicDistributionSum = 0, weight = 0;
				for (int k = 0; k < trainModel.numTopics; k++) {
					weight = trainModel.phi[k][words[wPos]]
							* ((docTopicCounts[dPos].get(k) + trainModel.alpha[k]));
					topicDistributionSum += weight;
					topicDistribution[k] = weight;
				}

				int newTopic = random.nextDiscrete(topicDistribution,
						topicDistributionSum);

				wordTopicAssignments[dPos][wPos] = newTopic;
				docTopicCounts[dPos].adjustOrPutValue(newTopic, 1, 1);
			}
		}

		if (doc.numCitations != 0) {
			int[] citations = doc.citationSequence;
			int[] topics = citationTopicAssignments[dPos];
			for (int cPos = 0; cPos < citations.length; cPos++) {
				docTopicCounts[dPos].adjustOrPutValue(topics[cPos], -1, 0);

				double[] topicDistribution = new double[trainModel.numTopics];
				double topicDistributionSum = 0, weight = 0;
				for (int k = 0; k < trainModel.numTopics; k++) {
					weight = trainModel.psi[k][citations[cPos]]
							* ((docTopicCounts[dPos].get(k) + trainModel.alpha[k]));
					topicDistributionSum += weight;
					topicDistribution[k] = weight;
				}

				int newTopic = random.nextDiscrete(topicDistribution,
						topicDistributionSum);

				citationTopicAssignments[dPos][cPos] = newTopic;
				docTopicCounts[dPos].adjustOrPutValue(newTopic, 1, 1);
			}
		}
	}

	public void estimate() {
		long startTime = System.currentTimeMillis();

		for (int iteration = 1; iteration <= numIterations; iteration++) {
			System.out.println("Iteration " + iteration + " start...");
			long iterationStart = System.currentTimeMillis();

			for (int dPos = 0; dPos < numDocs; dPos++) {
				sampleOneDocument((Document) corpus.getDoc(dPos), dPos);
			}

			long elapsedMillis = System.currentTimeMillis() - iterationStart;
			if (elapsedMillis < 1000) {
				System.out.println("Iteration finished: " + elapsedMillis
						+ "ms ");
			} else {
				System.out.println("Iteration finished: "
						+ (elapsedMillis / 1000) + "s ");
			}

			if (iteration % 10 == 0) {
				if (printLogLikelihood) {
					System.out.println("<" + iteration + "> perplexity: "
							+ computePerplexity());
				} else {
					System.out.println("<" + iteration + ">");
				}
			}

			if (iteration == numIterations || iteration >= burninPeriod
					&& iteration % 10 == 0) {
				estimateParameters();
			}
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
	}

	// estimate parameters after every k iterations after burn-in
	public void estimateParameters() {
		numSamples++;

		for (int dPos = 0; dPos < numDocs; dPos++) {
			IDocument doc = corpus.getDoc(dPos);

			for (int k = 0; k < trainModel.numTopics; k++) {
				if (numSamples > 1)
					theta[dPos][k] *= (numSamples - 1);
				theta[dPos][k] += (trainModel.alpha[k] + docTopicCounts[dPos].get(k))
						/ (doc.docLen + trainModel.alphaSum);

				if (numSamples > 1)
					theta[dPos][k] /= numSamples;
			}
		}
	}
}
