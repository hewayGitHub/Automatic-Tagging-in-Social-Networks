package topic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

import types.AlphabetFactory;
import types.Corpus;
import types.Document;
import types.IDSorter;


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
				doc.addContent(uid, -1, words.split("\\s+"));

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
					corpus.getDoc(docID).addCitations(uid, -1, citations.split(" "));
				}
			}
			
			corpus.numUniqueCitations = Corpus.citationAlphabet.size();
		} finally {
			citationBR.close();
		}

		System.out.println("Total Documents:" + corpus.numDocs);
		System.out.println("Total Word Size:" + corpus.numUniqueWords);
		System.out.println("Total Citation Size:" + corpus.numUniqueCitations);
		return corpus;
	}
	
	public static void main(String[] args) throws IOException {
		//String rootDir = "D:\\twitter\\Twitter network\\tagLDA\\";
		String rootDir = "/home/hewei/tag/";
		
		String contentDir = rootDir + "normal_user_tweets_filter";
		String citationDir = rootDir + "normal_user_celebrities";

		int iterations = 1000;
		String output = rootDir + "tagLDA_result_iter_" + iterations +".txt";
		String modelDir = rootDir + "tagLDA.model." + iterations;
		
		TagLDA tagLDA = new TagLDA();
		System.out.println("Reading Data.....");
		Corpus corpus = tagLDA.readData(contentDir, citationDir, 5000);
		System.out.println("Done");
		
		tagLDA.numTopics = 100;
		tagLDA.InitializeParameters(corpus, tagLDA.numTopics);
		tagLDA.InitializeAssignments(corpus, AlphabetFactory.labelAlphabetOfSize(tagLDA.numTopics));
		tagLDA.sampleCorpus(corpus, iterations);
		tagLDA.estimateParameters(corpus);
		
		System.out.println("save model");
		tagLDA.writeObject(new ObjectOutputStream(new FileOutputStream(new File(modelDir))));
		int topN = 40;
		
		ArrayList<TreeSet<IDSorter>> topicSortedWords = tagLDA.getSortedWords(corpus, topN);
		System.out.println("================================");
		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(output))));
		
		System.out.println("# Topic_word");
		bw1.write("# Topic_word");
		bw1.newLine();
		for (int topic = 0; topic < tagLDA.numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic)
					.iterator();

			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t", topic);
			int rank = 0;
			while (iterator.hasNext() && rank < 20) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.4f) ",
						Corpus.vocabulary.lookupObject(idCountPair.getID()),
						idCountPair.getWeight());
				rank++;
			}
			//System.out.println(out);
			String line = out.toString();
			bw1.write(line);
			bw1.newLine();
		}
		
		System.out.println("# Topic_citation");
		ArrayList<TreeSet<IDSorter>> topicSortedCitation = tagLDA.getSortedCitations(corpus, topN);
		bw1.write("# Topic_citation");
		bw1.newLine();
		for (int topic = 0; topic < tagLDA.numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedCitation.get(topic)
					.iterator();

			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t", topic);
			int rank = 0;
			while (iterator.hasNext() && rank < 20) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.4f) ",
						Corpus.citationAlphabet.lookupObject(idCountPair.getID()),
						idCountPair.getWeight());
				rank++;
			}
			//System.out.println(out);
			String line = out.toString();
			bw1.write(line);
			bw1.newLine();
		}
		
		System.out.println("# rec_word");
		ArrayList<TreeSet<IDSorter>> recDocWords = tagLDA.recommendWordIG(topN);
		bw1.write("# rec_word");
		bw1.newLine();
		for (int m = 0; m < tagLDA.numDocs; m++) {
			Iterator<IDSorter> iterator = recDocWords.get(m).iterator();

			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%s\t", Corpus.docNameAlphabet.lookupObject(m));
			while (iterator.hasNext()) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.4f) ",
						Corpus.vocabulary.lookupObject(idCountPair.getID()),
						idCountPair.getWeight());
			}
			String line = out.toString();
			bw1.write(line);
			bw1.newLine();
		}
		
		/*System.out.println("# Doc_Topic");
		bw1.write("# Doc_Topic");
		bw1.newLine();
		for (int i = 0; i < tagLDA.numDocs; i++) {
			out = new Formatter(new StringBuilder(), Locale.US);

			out.format("%s\t", Corpus.docNameAlphabet.lookupObject(i));
			double max = 0.0;
			int assign = -1;
			for (int topic = 0; topic < tagLDA.numTopics; topic++) {
				if (tagLDA.theta_train[i][topic] > max) {
					max = tagLDA.theta_train[i][topic];
					assign = topic;
				}
				out.format("%.4f\t", tagLDA.theta_train[i][topic]);
			}
			out.format("%d\t%.4f", assign, max);

			String line = out.toString();
			//System.out.println(line);
			bw1.write(line);
			bw1.newLine();
		}*/
		
		bw1.close();
	}
}
