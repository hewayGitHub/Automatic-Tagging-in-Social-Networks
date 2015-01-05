/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package twitter4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

/**
 * Shows one single user.
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public final class ShowUser {
	static List<String> getUserIDs(int maxLine) throws IOException {
		String citationDir = "D:\\twitter\\Twitter network\\normal_user_celebrities";
		List<String> userIDs = new ArrayList<String>();
		
		BufferedReader citationBR = null;
		try {
			citationBR = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(citationDir)), "UTF-8"));

			String line = null, items[], uid;
			int lineCount = 0;
			while ((line = citationBR.readLine()) != null) {
				if(lineCount++ > maxLine) break;
				items = line.split("\\t");

				if (items.length != 2)
					continue;

				uid = items[0];
				
				userIDs.add(uid);
			}			
		} catch(IOException e){
			e.printStackTrace();
			System.out.println("Failed to delete status: " + e.getMessage());
            System.exit(-1);
		} finally {
			citationBR.close();
		}
		
		return userIDs;
	}
	
	static User showUser(String userID, Twitter twitter) {
		int uid = Integer.parseInt(userID);
		
		User user = null;
		try {
			user = twitter.showUser(uid);
		} catch (TwitterException e) {
			e.printStackTrace();
			System.out.println("\nFailed to delete status: " + e.getMessage() + " uid:" + userID);
		}
		
		return user;
	}
	
    /**
     * Usage: java twitter4j.examples.user.ShowUser [screen name]
     *
     * @param args message
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException {
    	
        try {
        	PrintWriter outUserInfo = new PrintWriter(new FileWriter("D:\\twitter\\Twitter network\\test_user_json", true));
        	
            Twitter twitter = new TwitterFactory().getInstance();
            List<String> userIDs = getUserIDs(10);
            
            int count  = 1, remainCount = 90;        	
            long start = System.currentTimeMillis();
            long end = start;
            for(int i = 0; i < userIDs.size(); i++) {
            	String uid =  userIDs.get(i);
            	
            	if(count % 180 == 0 || count > remainCount) {
            		end = System.currentTimeMillis();
            		System.out.println(count);
            		outUserInfo.flush();
            		
            		Thread.sleep(15*60*1000 + end - start);
            		start = end;
            		remainCount = count+180;
            	}
            	
            	User user = showUser(uid, twitter);
            	outUserInfo.println(user.toString().replace('\n', ' '));
            	
            	System.out.println(count++);
            }
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
