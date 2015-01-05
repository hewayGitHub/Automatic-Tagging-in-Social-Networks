package standalone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class AnaFollow {
	static void anaFollow() throws NumberFormatException, IOException {
		int MAX_LEN = 1000;
		
		int[] fansAna = new int[MAX_LEN + 1];
		int[] followersAna = new int[MAX_LEN + 1];
		
		Arrays.fill(fansAna, 0);
		Arrays.fill(followersAna, 0);
		
		int fansCount, followsCount, index;
		//BufferedReader inFollowCount = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\follow_count"));
		
		BufferedReader inFollowCount = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\normal_user_info"));
		
		String line, items[];
		while(null != (line=inFollowCount.readLine())) {
			items = line.split("\t");
			
			if(items.length < 3) continue;
			
			
			fansCount = Integer.parseInt(items[2]);
			followsCount = Integer.parseInt(items[3]);
			
			index = fansCount / 100;
			if(index >= MAX_LEN) {
				index = MAX_LEN;
			}
			fansAna[index] += 1;
			
			 index = followsCount / 100;
			if(index >= MAX_LEN) {
				index = MAX_LEN;
			}
			followersAna[index] += 1;
		}
		inFollowCount.close();
		
		PrintWriter outFansAna = new PrintWriter(
				"D:\\twitter\\Twitter network\\fans_ana");
		for (int i = 0; i <= MAX_LEN; i++) {
			outFansAna.println(i * 100 + "\t" + fansAna[i]);
		}
		outFansAna.close();

		PrintWriter outFollowsAna = new PrintWriter(
				"D:\\twitter\\Twitter network\\followers_ana");
		for (int i = 0; i <= MAX_LEN; i++) {
			outFollowsAna.println(i * 100 + "\t" + followersAna[i]);
		}
		outFollowsAna.close();
	}
	
	static void getTargetUser() throws NumberFormatException, IOException {
		BufferedReader inFollowCount = new BufferedReader(new FileReader("D:\\twitter\\Twitter network\\analyze\\follow_count"));
		PrintWriter outFamousUser = new PrintWriter("D:\\twitter\\Twitter network\\famous_user");
		PrintWriter outNormalUser = new PrintWriter("D:\\twitter\\Twitter network\\normal_user");
		
		String line, items[];
		int fansCount, followsCount;
		while(null != (line=inFollowCount.readLine())) {
			items = line.split("\t");
			
			if(items.length != 3) continue;
			
			fansCount = Integer.parseInt(items[1]);
			followsCount = Integer.parseInt(items[2]);
			
			if(fansCount < 10000 && fansCount > 5000) {
				outFamousUser.println(line);
			} else if(fansCount >= 100 && fansCount <= 2000 && followsCount >= 100 && followsCount <= 2000) {
				outNormalUser.println(line);
			}
		}
		inFollowCount.close();
		outFamousUser.close();
		outNormalUser.close();
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
		//anaFollow();
		getTargetUser();
	}

}
