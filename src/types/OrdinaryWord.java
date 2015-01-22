package types;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class OrdinaryWord {
	private static OrdinaryWord instance = new OrdinaryWord();
	
	private Set<String> wordSet = new HashSet<String>();
	private OrdinaryWord() {
	}
	
	public static void init(String wordFile) {
		if(wordFile == null || wordFile.trim().length() == 0) {
			return;
		}
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(wordFile));
			
			String line = null;
			while(null != (line=in.readLine())) {
				line = line.trim().toLowerCase();
				if(line.length() != 0) {
					instance.wordSet.add(line);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public static boolean isOrdinaryWord(String word) {
		return instance.wordSet.contains(word);
	}
}
