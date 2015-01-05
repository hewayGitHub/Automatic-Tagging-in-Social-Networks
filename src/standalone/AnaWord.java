package standalone;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class AnaWord {
	static void filterFreq(int freqThreshold) throws IOException {
		BufferedReader inData = new BufferedReader(new FileReader("D:/twitter/Twitter network/analyze/tf-idf"));
		PrintWriter outData = new PrintWriter("D:/twitter/Twitter network/analyze/tf-idf-filter");
		
		String line = null, items[];
		int tweet_sum, freq_sum, count = 0;
		while(null != (line=inData.readLine())) {
			items = line.split("\t");
			if(items.length < 3) continue;
			
			tweet_sum = Integer.parseInt(items[1]);
			freq_sum = Integer.parseInt(items[2]);
			
			if(freq_sum >= freqThreshold) {
				count++;
				
				outData.println(line);
			}
		}
		
		inData.close();
		outData.close();
		
		System.out.println(count);
	}
	
	public static void main(String[] args) throws IOException {
		filterFreq(1000);
	}

}
