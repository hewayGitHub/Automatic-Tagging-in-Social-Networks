package standalone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GetTrain {
	private static Set<String> getTargetIDs() throws IOException{
		Set<String> idSet = new HashSet<String>();
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_features"));
		String line, items[], uid;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];			
			idSet.add(uid);
		}
		inCelebrity.close();
		
		return idSet;
	}
	
	public static void filterFollows() throws IOException {
		PrintWriter outNormalCelebrity = new PrintWriter("D:\\twitter\\Twitter network\\train_user_celebrities_tokens");
		
		Set<String> targetIdSet = getTargetIDs();
		System.out.println("target user:" + targetIdSet.size());
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_celebrities_tokens"));
		String line, items[], uid;
		int count = 0;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];
			
			if(targetIdSet.contains(uid)) {
				count++;
			
				outNormalCelebrity.println(line);
			}
		}
		inCelebrity.close();
		outNormalCelebrity.close();
		
		System.out.println(count);
	}
	
	public static void filterTweets() throws IOException {
		PrintWriter outNormalCelebrity = new PrintWriter("D:\\twitter\\Twitter network\\train_user_tweets");
		
		Set<String> idSet = getTargetIDs();
		System.out.println(idSet.size());
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_tweets_filter"));
		String line, items[], uid;
		int count = 0;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];
			
			if(idSet.contains(uid)) {
				outNormalCelebrity.println(line);
				count++;
			}
		}
		inCelebrity.close();
		outNormalCelebrity.close();
		
		System.out.println(count);
	}
	public static void main(String[] args) throws IOException {
		//filterTweets();
		filterFollows();
	}

}
