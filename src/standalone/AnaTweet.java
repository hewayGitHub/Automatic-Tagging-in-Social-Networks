package standalone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AnaTweet {
	static Map<String, String> getNameMap(String userPath, int userNum) throws IOException {	
		Map<String, String> nameMap = new HashMap<String, String>(userNum);
		
		BufferedReader inUser = new BufferedReader(new FileReader(userPath));

		String line = null, userName;
		int tapIndex = -1;
		while (null != (line = inUser.readLine())) {
			tapIndex = line.indexOf('\t');
			if (tapIndex != -1) {
				userName = line.substring(0, tapIndex);
				nameMap.put(userName, line);
			}
		}
		
		inUser.close();
		
		return nameMap;
	}
	
	static void anaTweet() throws NumberFormatException, IOException {
		int MAX_LEN = 1000;
		
		int[] tweetsAna = new int[MAX_LEN + 1];
		
		Arrays.fill(tweetsAna, 0);
		
		int tweetsCount, index;
		BufferedReader inTweetCount = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\tweet_count"));
		String line, items[];
		while(null != (line=inTweetCount.readLine())) {
			items = line.split("\t");
			
			if(items.length != 2) continue;
			
			tweetsCount = Integer.parseInt(items[1]);
			
			index = tweetsCount / 100;
			if(index >= MAX_LEN) {
				index = MAX_LEN;
			}
			tweetsAna[index] += 1;
		}
		inTweetCount.close();
		
		PrintWriter outTweetsAna = new PrintWriter(
				"D:\\twitter\\Twitter network\\tweets_ana");
		for (int i = 0; i <= MAX_LEN; i++) {
			outTweetsAna.println(i * 100 + "\t" + tweetsAna[i]);
		}
		outTweetsAna.close();
	}
	
	static void getTweet(int tweetThreshold) throws NumberFormatException, IOException {
		//Map<String, String> normalNameMap = testSetup();
		Map<String, String> normalNameMap = getNameMap("D:\\twitter\\Twitter network\\normal_user_name", 883206);
		Map<String, String> famousNameMap = getNameMap("D:\\twitter\\Twitter network\\famous_user_name", 55950);
		
		int tweetsCount;
		BufferedReader inTweetCount = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\tweet_count"));
		PrintWriter outNormalInfo = new PrintWriter("D:\\twitter\\Twitter network\\normal_user_info");
		PrintWriter outFamousInfo = new PrintWriter("D:\\twitter\\Twitter network\\famous_user_info");

		String line, items[], name;
		while(null != (line=inTweetCount.readLine())) {
			items = line.split("\t");
			
			if(items.length != 2) continue;
			
			name = items[0];
			tweetsCount = Integer.parseInt(items[1]);
			
			if(tweetsCount < tweetThreshold) continue;
			
			if(normalNameMap.containsKey(name)) {
				outNormalInfo.println(normalNameMap.get(name) + "\t" + tweetsCount);
			} else if(famousNameMap.containsKey(name)) {
				outFamousInfo.println(famousNameMap.get(name) + "\t" + tweetsCount);
			}
		}
		inTweetCount.close();
		outNormalInfo.close();
		outFamousInfo.close();
	}
	
	static void anaTweetWord() throws NumberFormatException, IOException {
		int MAX_LEN = 1000;
		
		int[] tweetsAna = new int[MAX_LEN + 1];
		
		Arrays.fill(tweetsAna, 0);
		
		int wordCount, index;
		BufferedReader inTweetCount = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\word_count"));
		PrintWriter outTweets1000 = new PrintWriter("D:\\twitter\\Twitter network\\word_1000");
		String line, items[];
		while(null != (line=inTweetCount.readLine())) {
			items = line.split("\t");
			
			if(items.length != 2) continue;
			
			wordCount = Integer.parseInt(items[1]);
			if(wordCount >= 1000) {
				outTweets1000.println(line);
			}
			index = wordCount / 100;
			if(index >= MAX_LEN) {
				index = MAX_LEN;
			}
			tweetsAna[index] += 1;
		}
		inTweetCount.close();
		outTweets1000.close();
		
		PrintWriter outTweetsAna = new PrintWriter(
				"D:\\twitter\\Twitter network\\word_ana");
		for (int i = 0; i <= MAX_LEN; i++) {
			outTweetsAna.println(i * 100 + "\t" + tweetsAna[i]);
		}
		outTweetsAna.close();
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
		//anaTweet();
		//getTweet(100);
		anaTweetWord();
	}

}
