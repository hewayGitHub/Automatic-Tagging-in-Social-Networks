package experiments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import topic.Model;
import types.IDSorter;

public class TagRecommendation {
	public static Model readModel(String modelPath) throws IOException, ClassNotFoundException {
		Model model = new Model();
		model.readObject(new ObjectInputStream(new FileInputStream(new File(modelPath))));
		
		return model;
	}
	
	public static ArrayList<TreeSet<IDSorter>> recommend(Model model, int topN) throws IOException, ClassNotFoundException {
		ArrayList<TreeSet<IDSorter>> recDocWords = model.recommendWord(topN);
		
		return recDocWords;
	}
	
	public static void printRecommend(Model model, ArrayList<TreeSet<IDSorter>> recommend, String resPath) throws IOException{
		PrintWriter resOut = new PrintWriter(new FileWriter(resPath));

		for (int m = 0; m < recommend.size(); m++) {
			Iterator<IDSorter> iterator = recommend.get(m).iterator();

			resOut.format("%s\t", model.docNameAlphabet.lookupObject(m));
			while (iterator.hasNext()) {
				IDSorter idCountPair = iterator.next();
				resOut.format("%s (%.4f) ",
						model.vocabulary.lookupObject(idCountPair.getID()),
						idCountPair.getWeight());
			}
			
			resOut.println();
		}
		
		resOut.close();
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		/*Model model = readModel("D:\\twitter\\Twitter network\\tweetLDA\\tweetLDA.model.200");
		ArrayList<TreeSet<IDSorter>> recWords = recommend(model, 40);
		printRecommend(model, recWords, "D:\\twitter\\Twitter network\\tweetLDA\\tweetLDA.model.200"+".rec.word.max");
		*/
		Model model = readModel("D:\\twitter\\Twitter network\\tagLDA\\tagDesLDA.model.1000");
		ArrayList<TreeSet<IDSorter>> recWords = recommend(model, 40);
		printRecommend(model, recWords, "D:\\twitter\\Twitter network\\tagLDA\\tagLDA.model.1000"+".rec.word.IG");
	}

}
