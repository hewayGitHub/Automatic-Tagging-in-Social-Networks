package hadoop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class GetFollow {
	public static class GetFollowMapper extends Mapper<LongWritable, Text, Text, Text> {	
		static Set<String> IDSet = new HashSet<String>();
		
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
								IDSet.add(items[1]);
							}
						}
					} finally {
						inName2ID.close();
					}
				} else {
					System.err.println("cannot read from DistributedCache. cacheFiles:" + (cacheFiles == null?"null":cacheFiles.length));
					throw new RuntimeException();
				}
				
				System.out.println("Num of name2ID:" + IDSet.size());
			} catch (IOException e) {
				System.err.println("Exception reading DistributedCache: " + e);
			}
		}
		
		@Override
		protected void map(LongWritable key, Text value, 
	            Context context) throws IOException, InterruptedException {
			String[] items = value.toString().trim().split("\t");
			
			if(items.length == 2) {
				String followID = items[0];
				String fanID = items[1];
				
				if(IDSet.contains(fanID)) {
					context.write(new Text(fanID), new Text(followID));//fan和follow
				}
			}
		}
	}
	
	public static class GetFollowReducer extends Reducer<Text, Text, Text, Text> {		
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context
	            ) throws IOException, InterruptedException {
			Iterator<Text> it = values.iterator();
			StringBuilder sb = new StringBuilder(it.next().toString());
			while(it.hasNext()) {
				sb.append(" " + it.next().toString());
			}
			
			context.write(key, new Text(sb.toString()));//fan和follow
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
		job.setJobName("GetFollow");
		job.setJarByClass(GetFollow.class);
		
		job.setMapperClass(GetFollowMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		job.setCombinerClass(GetFollowReducer.class);
		
		job.setReducerClass(GetFollowReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		//设置压缩
		FileOutputFormat.setCompressOutput(job, true);
		FileOutputFormat.setOutputCompressorClass(job, org.apache.hadoop.io.compress.GzipCodec.class);
		
		job.waitForCompletion(true);
	}

}
