package standalone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class GetName {
	static Map<String, String> getIDMap(String userPath, int userNum) throws IOException {
		Map<String, String> idMap = new HashMap<String, String>(userNum);
		
		BufferedReader inUser = new BufferedReader(new FileReader(userPath));

		String line = null, userID;
		int tapIndex = -1;
		while (null != (line = inUser.readLine())) {
			tapIndex = line.indexOf('\t');
			if (tapIndex != -1) {
				userID = line.substring(0, tapIndex);
				idMap.put(userID, line);
			}
		}
		
		inUser.close();
		
		return idMap;
	}
	 
	public static void main(String[] args) throws IOException {
		/*if(args.length < 2) {
			System.out.println("Pls run as *.jar namePath userPath");
		}
		
		String namePath = args[0];
		String userPath = args[1];*/
		
		String namePath = "D:\\twitter\\Twitter network\\numeric2screen";
		String normalUserPath = "D:\\twitter\\Twitter network\\normal_user";
		String famousUserPath = "D:\\twitter\\Twitter network\\famous_user";
		
		Map<String, String> normalIdMap = getIDMap(normalUserPath, 903437);
		Map<String, String> famousIdMap = getIDMap(famousUserPath, 57004);
		
		BufferedReader inUser = new BufferedReader(new FileReader(namePath));
		PrintWriter outNormalName = new PrintWriter("D:\\twitter\\Twitter network\\normal_user_name");
		PrintWriter outFamousName = new PrintWriter("D:\\twitter\\Twitter network\\famous_user_name");
		
		String line = null, userID;
		int tapIndex = -1;
		while (null != (line = inUser.readLine())) {
			tapIndex = line.indexOf(' ');
			
			if (tapIndex != -1) {
				userID = line.substring(0, tapIndex);
				
				if(normalIdMap.containsKey(userID)) {
					outNormalName.println(line.substring(tapIndex+1) + '\t' + normalIdMap.get(userID));
				} else if(famousIdMap.containsKey(userID)) {
					outFamousName.println(line.substring(tapIndex+1) + '\t' + famousIdMap.get(userID));
				}
				
				
			}
		}
		
		outNormalName.close();
		outNormalName.close();
		inUser.close();
	}
}
