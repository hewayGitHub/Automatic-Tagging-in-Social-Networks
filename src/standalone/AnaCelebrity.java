package standalone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import preprocess.TweetTokenize;

public class AnaCelebrity {
	public static void extractFeatures() throws IOException {
		PrintWriter outCelebrityFeatures = new PrintWriter("D:\\twitter\\Twitter network\\celebrity\\celebrities_features");
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\celebrity\\celebrities_profiles.txt"));
		String line, items[], uid, description, screenName, name;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 20) continue;
			
			uid = items[0];
			description = items[10];
			screenName = items[16];
			name = items[19];
			
			outCelebrityFeatures.println(uid + "\t" + name + "\t" + screenName + "\t" + description);
		}
		inCelebrity.close();
		outCelebrityFeatures.close();
	}
	
	public static void handleFeatures() throws IOException {
		PrintWriter outCelebrityFeatures = new PrintWriter("D:\\twitter\\Twitter network\\celebrity\\celebritis_tokens");
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\celebrity\\celebrities_features"));
		String line, items[], uid, description;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 4) continue;
			
			uid = items[0];
			description = items[3];
			
			List<String> features = TweetTokenize.tokenizeRawTweetText(description);
			if(features.size() != 0) {
				outCelebrityFeatures.print(uid + "\t");
				for(int i = 0; i< features.size(); i++) {
					if(i != features.size()-1)
						outCelebrityFeatures.print(features.get(i) + " ");
					else
						outCelebrityFeatures.print(features.get(i));
				}
				outCelebrityFeatures.println();
			}
		}
		inCelebrity.close();
		
		outCelebrityFeatures.close();
	}
	
	private static Set<String> getCelebrityIDs() throws IOException{
		Set<String> idSet = new HashSet<String>();
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\celebrity\\celebrities_features"));
		String line, items[], uid;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 4) continue;
			
			uid = items[0];
			idSet.add(uid);
		}
		inCelebrity.close();
		
		return idSet;
	}

	public static void extractFamous() throws IOException {
		PrintWriter outCelebrityFeatures = new PrintWriter(
				new FileWriter("D:\\twitter\\Twitter network\\celebrity\\celebrities_features", true));
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\famous_user_json"));
		String line, uid, description, screenName, name;
		//id=(\\d+), name='(.+?)', screenName='(.+?)', location='.+?', description='(.+?)',
		Pattern pattern = Pattern.compile("id=(\\d+), name='(.+?)', screenName='(.+?)', location='.*?', description='(.+?)', isContributorsEnabled");
		int count = 0, missCount = 0;
		while(null != (line=inCelebrity.readLine())) {
			if(!line.startsWith("UserJSONImpl")) {
				System.out.println(line);
				continue;
			}
			
			line = line.substring("UserJSONImpl".length());
			Matcher m = pattern.matcher(line);
			if(m.find() && m.groupCount() >= 4) {
				uid = m.group(1);
				name = m.group(2);
				screenName = m.group(3);
				description = m.group(4);
				
				count++;
				outCelebrityFeatures.println(uid + "\t" + name + "\t" + screenName + "\t" + description);
			} else {
				System.out.println(line);
				missCount++;
			}
		}
		inCelebrity.close();
		outCelebrityFeatures.close();
		
		System.out.println(count + " " + missCount);
	}

	public static void countCelebrities() throws IOException {
		PrintWriter outNormalCelebrityCount = new PrintWriter("D:\\twitter\\Twitter network\\normal_user_celebrities_count");
		
		Set<String> idSet = getCelebrityIDs();
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_follows"));
		String line, items[], uid, follows[];
		int count = 0;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];
			follows = items[1].split(" ");
			
			int followCount = 0, sum = follows.length;
			for(String follow: follows) {
				if(idSet.contains(follow)) {					
					followCount++;
				}
			}
			if(followCount != 0) {				
				outNormalCelebrityCount.println(uid + "\t" + followCount + "\t" + sum + "\t" + followCount * 1.0 /sum);
				count++;
			}
		}
		inCelebrity.close();
		outNormalCelebrityCount.close();
		
		System.out.println(count);
	}
	
	static Map<String, String> getIDMap(String userPath, int userNum) throws IOException {	
		Map<String, String> nameMap = new HashMap<String, String>(userNum);
		
		BufferedReader inUser = new BufferedReader(new FileReader(userPath));

		String line = null, uid;
		int firstTapIndex = -1, secondTapIndex;
		while (null != (line = inUser.readLine())) {
			firstTapIndex = line.indexOf('\t');
			if (firstTapIndex != -1) {
				secondTapIndex = line.indexOf('\t', firstTapIndex + 1);
				uid = line.substring(firstTapIndex + 1, secondTapIndex);
				nameMap.put(uid, line);
			}
		}
		
		inUser.close();
		
		return nameMap;
	}
	
	private static Set<String> getTargetIDs() throws IOException{
		Set<String> idSet = new HashSet<String>();
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_celebrities_count"));
		String line, items[], uid;
		int celebrityCount;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];
			celebrityCount = Integer.parseInt(items[1]);
			
			if(celebrityCount >= 50) {
				idSet.add(uid);
			}
		}
		inCelebrity.close();
		
		return idSet;
	}
	
	private static Map<String, String> getTargetIDMap() throws IOException{
		Map<String, String> idSet = new HashMap<String, String>();
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_celebrities_count"));
		String line, items[], uid;
		int celebrityCount;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];
			celebrityCount = Integer.parseInt(items[1]);
			
			if(celebrityCount >= 50) {
				idSet.put(uid, ""+celebrityCount);
			}
		}
		inCelebrity.close();
		
		return idSet;
	}
	
	public static void filterFollows() throws IOException {
		PrintWriter outNormalCelebrity = new PrintWriter("D:\\twitter\\Twitter network\\normal_user_celebrities");
		
		Set<String> targetIdSet = getTargetIDs();
		Set<String> celebrityIdSet = getCelebrityIDs();
		System.out.println("target user:" + targetIdSet.size() + " celebrity: " + celebrityIdSet.size());
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_follows"));
		String line, items[], uid, follows[];
		int count = 0;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];
			
			if(targetIdSet.contains(uid)) {
				count++;
				
				follows = items[1].split(" ");
				
				StringBuilder sb = new StringBuilder();
				for(String follow: follows) {
					if(celebrityIdSet.contains(follow)) {					
						sb.append(follow + " ");
					}
				}
				sb.deleteCharAt(sb.length() - 1);
			
				outNormalCelebrity.println(uid + "\t" + sb.toString());
			}
		}
		inCelebrity.close();
		outNormalCelebrity.close();
		
		System.out.println(count);
	}
	
	public static void filterTweets() throws IOException {
		PrintWriter outNormalCelebrity = new PrintWriter("D:\\twitter\\Twitter network\\normal_user_tweets_filter");
		
		Set<String> idSet = getTargetIDs();
		System.out.println(idSet.size());
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_tweets"));
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
	
	public static void anaFollows() throws IOException {
		HashMap<String, int[]> celebrityIdSet = new HashMap<String, int[]>();
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_celebrities"));
		String line, items[], follows[];
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			follows = items[1].split(" ");
				
			for(String follow: follows) {
				if(celebrityIdSet.containsKey(follow)) {
					celebrityIdSet.get(follow)[0]++; 
				} else {
					celebrityIdSet.put(follow, new int[]{1});
				}
			}
		}
		inCelebrity.close();
		
		PrintWriter outNormalCelebrity = new PrintWriter("D:\\twitter\\Twitter network\\celebrity\\celebrities_occur");
		for(Entry<String, int[]> e: celebrityIdSet.entrySet()) {
			outNormalCelebrity.println(e.getKey() + "\t" + e.getValue()[0]);
		}
		outNormalCelebrity.close();
	}
	
	private static Map<String, String> getCelebrityDescription() throws IOException{
		Map<String, String> idMap = new HashMap<String, String>();
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\celebrity\\celebritis_tokens"));
		String line, items[], uid, tokens;
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];
			tokens = items[1];
			idMap.put(uid, tokens);
		}
		inCelebrity.close();
		
		return idMap;
	}
	
	public static void getFollowsDescription() throws IOException {
		PrintWriter outNormalCelebrity = new PrintWriter("D:\\twitter\\Twitter network\\normal_user_celebrities_tokens");
		
		Map<String, String> celebrityDescr = getCelebrityDescription();
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_celebrities"));
		String line, items[], uid, follows[];
		while(null != (line=inCelebrity.readLine())) {
			items = line.split("\t");
			
			if(items.length < 2) continue;
			
			uid = items[0];
			follows = items[1].split(" ");
				
			outNormalCelebrity.print(uid + "\t");
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < follows.length; i++) {
				String follow = follows[i];
				
				if(i == follows.length - 1)
					sb.append(celebrityDescr.get(follow));
				else
					sb.append(celebrityDescr.get(follow) + " ");
			}
			outNormalCelebrity.println(sb.toString());
		}
		inCelebrity.close();
		outNormalCelebrity.close();
	}
	
	static Map<String, String> getNameMap(String userPath, int userNum) throws IOException {	
		Map<String, String> nameMap = new HashMap<String, String>(userNum);
		
		BufferedReader inUser = new BufferedReader(new FileReader(userPath));

		String line = null, userName;
		int tapIndex = -1;
		while (null != (line = inUser.readLine())) {
			tapIndex = line.indexOf('\t');
			if (tapIndex != -1) {
				userName = line.substring(0, tapIndex);
				nameMap.put(userName, line.substring(tapIndex+1));
			}
		}
		
		inUser.close();
		
		return nameMap;
	}
	
	public static void extractNormal() throws IOException {
		Map<String, String> normalNameMap = getNameMap("D:\\twitter\\Twitter network\\target_normal_user_info", 72623);
		
		PrintWriter outCelebrityFeatures = new PrintWriter(
				new FileWriter("D:\\twitter\\Twitter network\\normal_user_features"));
		
		BufferedReader inCelebrity = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_json"));
		String preLine, curline, uid, description, screenName, name, location;
		//id=(\\d+), name='(.+?)', screenName='(.+?)', location='.+?', description='(.+?)',
		Pattern pattern = Pattern.compile("id=(\\d+), name='(.+?)', screenName='(.+?)', location='(.*?)', description='(.*?)', isContributorsEnabled");
		int count = 0, missCount = 0;
		
		preLine = inCelebrity.readLine();
		while(null != (curline=inCelebrity.readLine()) || preLine != null) {
			if(curline != null && !curline.startsWith("UserJSONImpl")) {
				preLine += curline;
				continue;
			}
			
			preLine = preLine.substring("UserJSONImpl".length());
			Matcher m = pattern.matcher(preLine);
			if(m.find() && m.groupCount() >= 5) {
				uid = m.group(1);
				name = m.group(2);
				screenName = m.group(3);
				location = m.group(4);
				description = m.group(5);
				
				count++;
				outCelebrityFeatures.println(uid + "\t" + name + "\t" + screenName + "\t" + location + "\t" + description);
			} else {
				System.out.println(preLine);
				missCount++;
			}
			
			preLine = curline;
		}
		inCelebrity.close();
		outCelebrityFeatures.close();
		
		System.out.println(count + " " + missCount);
	}
	
	public static void main(String[] args) throws IOException {
		//extractFeatures();
		//extractFamous();
		//countCelebrities();
		//filterFollows();
		//filterTweets();
		//anaFollows();
		
		//handleFeatures();
		//getFollowsDescription();
		
		//extractNormal();
		
		Map<String, String> normalIDMap = getIDMap("D:\\twitter\\Twitter network\\normal_user_info", 146685);
		Map<String, String> celebrityCountMap = getNameMap("D:\\twitter\\Twitter network\\normal_user_celebrities_count", 146685);
		Map<String, String> featureMap = getNameMap("D:\\twitter\\Twitter network\\normal_user_features", 10000);

		PrintWriter outTargetID = new PrintWriter("D:\\twitter\\Twitter network\\train_normal_user");
		for(String id: featureMap.keySet()) {
			outTargetID.println(normalIDMap.get(id) + "\t" + celebrityCountMap.get(id) + "\t" + featureMap.get(id));
		}
		outTargetID.close();
	}

}
