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

package twitter4j.examples.user;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.PagableResponseList;
import twitter4j.RateLimitStatusEvent;
import twitter4j.RateLimitStatusListener;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UserList;

/**
 * Shows one single user.
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public final class GetUserListSubscriptions {
	static List<String> userIDs = new ArrayList<String>();
	static  List<String> userNames = new ArrayList<String>();
	static List<String> getUserIDs(int maxLine) throws IOException {
		String citationDir = "D:\\twitter\\Twitter network\\normal_user_json";
		
		BufferedReader citationBR = null;
		try {
			citationBR = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(citationDir)), "UTF-8"));

			String line = null, uid, name;
			int lineCount = 0;
			Pattern p = Pattern.compile("id=(\\d+), .*?screenName='([^']+)'");
			Matcher m;
			while ((line = citationBR.readLine()) != null) {
				if(lineCount++ > maxLine) break;
				
				m = p.matcher(line);

				if (m.find()) {
					uid = m.group(1);
					name = m.group(2);
					
					userIDs.add(uid);
					userNames.add(name);
				}
				
				
			}			
		} catch(IOException e){
			e.printStackTrace();
			System.out.println("Failed to delete status: " + e.getMessage());
            System.exit(-1);
		} finally {
			citationBR.close();
		}
		
		return userNames;
	}
	
	static boolean needRedo = false;
	static int curCount = 0;
	static String showUserListSubscriptionsBak(String uid, Twitter twitter) throws IOException {
		needRedo = false;
		
		long cursor = -1;
        PagableResponseList<UserList> lists = null;
        
        StringBuilder sb = new StringBuilder();
        do {
            try {
				lists = twitter.getUserListSubscriptions(uid, cursor);
				
				double prob = random.nextDouble();
				if(prob < 0.3) { //have a slap
					int limit = lists.getRateLimitStatus().getRemaining();
					if(limit != 0) {
						int seconds = lists.getRateLimitStatus().getSecondsUntilReset();
						try {
							Thread.sleep(seconds / limit * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (TwitterException e) {
				if(e.getErrorCode() == 34) { //The user does not exist
					System.out.println("The user does not exist, uid:" + uid);
				} else if(e.getErrorCode() == 88) { // out of rate limit
					needRedo = true;
					
					int seconds = e.getRateLimitStatus().getSecondsUntilReset();
			    	System.out.println("Sleeping for " + seconds + "s");
			    	try {
						Thread.sleep(seconds * 1000);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				} else if(e.getErrorCode() == -1) {
					needRedo = true;
					
					System.out.println("Connect timed out or refused");		
					saveCurProcess();
					System.exit(-1);
				} else {
					needRedo = true;
					
					e.printStackTrace();
					System.out.println("\nMessage: " + e.getMessage() + " uid:" + uid);
					
					saveCurProcess();
					System.exit(-1);
				}
			}
            
            if(lists == null) break;
            
            for (UserList list : lists) {
                sb.append("id:" + list.getId() + ", name:" + list.getName() + ", description:" + list.getDescription()
                		+ ", slug:" + list.getSlug() + ", memeber_count:" + list.getMemberCount() 
                        + ", subscriber_count:" + list.getSubscriberCount() + ", created_at:" + list.getCreatedAt() + "\n");
            }
        } while ((cursor = lists.getNextCursor()) != 0); 
		
		return sb.toString();
	}
	
	static String showUserListSubscriptions(String uid, Twitter twitter) throws IOException {
		needRedo = false;
		
		ResponseList<UserList> lists = null;
        
		StringBuilder sb = new StringBuilder();
		try {
			lists = twitter.getUserLists(uid);
			
			for (UserList list : lists) {
				sb.append("id:" + list.getId() + ", name:" + list.getName() + ", description:" + list.getDescription() + ", slug:" + list.getSlug()
						+ ", memeber_count:" + list.getMemberCount() + ", subscriber_count:" + list.getSubscriberCount() + ", created_at:" + list.getCreatedAt()
						+ "\n");
			}
		} catch (TwitterException e) {
			if (e.getErrorCode() == 34) { // The user does not exist
				System.out.println("The user does not exist, uid:" + uid);
			} else if (e.getErrorCode() == 88) { // out of rate limit
				needRedo = true;

				int seconds = e.getRateLimitStatus().getSecondsUntilReset();
				System.out.println("Sleeping for " + seconds + "s");
				try {
					Thread.sleep(seconds * 1000);
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
			} else if (e.getErrorCode() == -1) {
				needRedo = true;

				System.out.println("Connect timed out or refused, sleep 10min");
				saveCurProcess();
				
				try {
					Thread.sleep(10 * 60 * 1000);
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
				//System.exit(-1);
			} else if(e.getErrorCode() == 130){ 
				needRedo = true;
				
				System.out.println("The Twitter servers are up, but overloaded with requests. Try again later.sleep 15min");
				saveCurProcess();
				
				try {
					Thread.sleep(15 * 60 * 1000);
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
			}else {
				needRedo = true;

				e.printStackTrace();
				System.out.println("\nMessage: " + e.getMessage() + " uid:" + uid);

				saveCurProcess();
				System.exit(-1);
			}
		}

		if(!needRedo && lists != null && lists.getRateLimitStatus() != null) {
			double prob = random.nextDouble();
			if (prob < 0.3) { // have a slap
				int limit = lists.getRateLimitStatus().getRemaining();
				if (limit != 0) {
					int seconds = lists.getRateLimitStatus().getSecondsUntilReset();
					try {
						System.out.println("sleep " + seconds/limit + "s");
						Thread.sleep(seconds / limit * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return sb.toString();
	}
	
	
	static public void saveCurProcess() throws IOException{
		BufferedWriter outCurCount = new BufferedWriter(new FileWriter("data/cur_count_list"));
    	outCurCount.write(curCount + "");
    	outCurCount.close();
	}
	
	static Random random = new Random(System.currentTimeMillis());
	public static class MyRateLimitStatusListener implements RateLimitStatusListener {
		static private MyRateLimitStatusListener instance = new MyRateLimitStatusListener();
		private MyRateLimitStatusListener(){
			
		}
		
		static MyRateLimitStatusListener getInstance(){
			return instance;
		}
		
		public void onRateLimitStatus(RateLimitStatusEvent event){
			double prob = random.nextDouble();
			if(prob < 0.3) { //have a slap
				int limit = event.getRateLimitStatus().getRemaining();
				if(limit == 0) return;
				
				int seconds = event.getRateLimitStatus().getSecondsUntilReset();
				try {
					Thread.sleep(seconds / limit * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	    /**
	     * Called when the account or IP address is hitting the rate limit.<br>
	     * onRateLimitStatus will be also called before this event.
	     *
	     * @param event rate limit status event.
	     */
	    public void onRateLimitReached(RateLimitStatusEvent event){
	    	if(event.isIPRateLimitStatus()) {
	    		System.out.println("Ip is out of rate limit");
	    		System.exit(-1);
	    	}
	    	
	    	int seconds = event.getRateLimitStatus().getSecondsUntilReset();
	    	System.out.println("Sleeping for " + seconds + "s");
	    	try {
				Thread.sleep(seconds * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }
	}
	
    /**
     * Usage: java twitter4j.examples.user.ShowUser [screen name]
     *
     * @param args message
     * @throws InterruptedException 
     * @throws IOException 
     */
    public static void main(String[] args) throws InterruptedException, IOException {
    	try {
        	//read previous count of user we process
        	BufferedReader inCurCount = new BufferedReader(new FileReader("data/cur_count_list"));
        	String line = null;
        	while(null != (line=inCurCount.readLine())) {
        		curCount = Integer.parseInt(line);
        	}
        	inCurCount.close();
        	System.out.println("Start from line: " + curCount);
        } catch(IOException e) {
        	e.printStackTrace();
        }
        
        try{
        	PrintWriter outUserInfo = new PrintWriter(new FileWriter("data/normal_user_list", true));
        	List<String> userIDs = getUserIDs(5000);
        	
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.addRateLimitStatusListener(MyRateLimitStatusListener.getInstance());
            
            String uid = null;
            for(;curCount < userIDs.size(); ) {
            	uid =  userIDs.get(curCount);
            	
            	String lists = showUserListSubscriptions(uid, twitter);
                
            	if(lists.length() != 0) {
            		outUserInfo.println("uid:" + uid);
            		outUserInfo.print(lists);
            		outUserInfo.flush();
            	}
            	
            	curCount++;
            	if(needRedo) curCount--;
            	if(curCount % 5 == 0) outUserInfo.flush();
            	
            	System.out.println(curCount);
            }
            
            outUserInfo.close();
        } catch (IOException e) {
			e.printStackTrace();
		} finally{
			saveCurProcess();
		}
    }
}
