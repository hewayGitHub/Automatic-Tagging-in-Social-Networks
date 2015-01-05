package hadoop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import preprocess.TweetTokenize;

public class CountTweetWord {
	public static class CountWordMapper extends Mapper<Text, Text, Text, IntWritable> {	
		static Map<String, String> name2ID = new HashMap<String, String>();
		IntWritable ONE = new IntWritable(1);
		
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			try {
				Path[] cacheFiles = DistributedCache.getLocalCacheFiles(context.getConfiguration());
				
				if (cacheFiles != null && cacheFiles.length > 0) {
					BufferedReader inName2ID = new BufferedReader(new FileReader(cacheFiles[0].toString()));
					
					try {
						String line, items[];
						while (null != (line = inName2ID.readLine())) {
							items = line.split("\t");
							
							if(items.length >= 2) {
								name2ID.put(items[0], items[1]);
							}
						}
					} finally {
						inName2ID.close();
					}
				} else {
					System.err.println("cannot read from DistributedCache. cacheFiles:" + (cacheFiles == null?"null":cacheFiles.length));
					throw new RuntimeException();
				}
				
				System.out.println("Num of name2ID:" + name2ID.size());
			} catch (IOException e) {
				System.err.println("Exception reading DistributedCache: " + e);
			}
		}
		
		@Override
		protected void map(Text key, Text value, 
	            Context context) throws IOException, InterruptedException {
			String name = key.toString().trim();//key为昵称 value为tweet
			//context.write(key, value);
			
			if(name2ID.containsKey(name)) {
				String tweet = value.toString();
				List<String> toks = TweetTokenize.tokenizeRawTweetText(tweet);
				
				for(int i = 0; i < toks.size() - 1; i++) {
					context.write(new Text(toks.get(i)), ONE);
				}
			}
		}
	}
	
	public static class CountWordReducer extends Reducer<Text, IntWritable, Text, IntWritable> {		
		@Override
		protected void reduce(Text key, Iterable<IntWritable> values, Context context
	            ) throws IOException, InterruptedException {
			int sum = 0;
			for(IntWritable v: values) {
				sum += v.get();
			}
			
			context.write(key, new IntWritable(sum));//word和num
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 * @throws URISyntaxException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, URISyntaxException {
		Configuration jobConf = new Configuration();
		jobConf.setBoolean("mapreduce.map.output.compress", true);
		jobConf.setClass("mapreduce.map.output.compress.codec", org.apache.hadoop.io.compress.GzipCodec.class, org.apache.hadoop.io.compress.CompressionCodec.class);
		DistributedCache.addCacheFile(new Path("hdfs://t1:9000/heway/normal_user_info").toUri(), jobConf); //添加分布式缓存文件
		
		Job job = new Job(jobConf);
		job.setJobName("GetTweet");
		job.setJarByClass(CountTweetWord.class);
		
		job.setMapperClass(CountWordMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setCombinerClass(CountWordReducer.class);
		
		job.setReducerClass(CountWordReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		job.setInputFormatClass(TweetInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		//设置压缩
		FileOutputFormat.setCompressOutput(job, true);
		FileOutputFormat.setOutputCompressorClass(job, org.apache.hadoop.io.compress.GzipCodec.class);
		
		job.waitForCompletion(true);
	}

}
