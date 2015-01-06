package types;
import java.util.ArrayList;
import java.util.HashMap;
import utils.FileUtil;

//import edu.mit.jwi.item.Word;

public class user {

	protected String userID;
	
	protected int tweetCnt;
	
	ArrayList<tweet> tweets = new ArrayList<tweet>();
	
	
	public user(String Dir, String id, HashMap<String, Integer> wordMap, 
			ArrayList<String> uniWordMap) {
		
		this.userID = id;
		ArrayList<String> datalines = new ArrayList<String>();
		FileUtil.readLines(Dir, datalines);		
		
		this.tweetCnt = datalines.size();
		
		for(int lineNo = 0; lineNo < datalines.size(); lineNo++) {
			String line = datalines.get(lineNo);
			tweet tw = new tweet(line, wordMap, uniWordMap);
			tweets.add(tw);						
		}
		
		datalines.clear();
	}
	
	public user(String id, HashMap<String, Integer> wordMap, 
			ArrayList<String> uniWordMap, String content) {
		
		this.userID = id;
		String lines[] = content.split("  ");
		
		this.tweetCnt = lines.length;
		
		for(int lineNo = 0; lineNo < lines.length; lineNo++) {
			tweet tw = new tweet(lines[lineNo], wordMap, uniWordMap);
			tweets.add(tw);						
		}
	}
}
