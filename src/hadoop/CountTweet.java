package hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class CountTweet {
	public static class GetTweetMapper extends Mapper<Text, Text, Text, IntWritable> {
		static IntWritable ONE = new IntWritable(1);
		
		@Override
		protected void map(Text key, Text value, 
	            Context context) throws IOException, InterruptedException {
			context.write(key, ONE); //用户名和tweet
		}
	}
	
	public static class GetTweetReducer extends Reducer<Text, IntWritable, Text, IntWritable> {		
		@Override
		protected void reduce(Text key, Iterable<IntWritable> values, Context context
	            ) throws IOException, InterruptedException {
			int sum = 0;
			for(IntWritable v: values) {
				sum += v.get();
			}
			
			context.write(key, new IntWritable(sum));
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		Configuration jobConf = new Configuration();
		
		Job job = new Job(jobConf);
		job.setJobName("CountTweet");
		job.setJarByClass(CountTweet.class);
		
		job.setMapperClass(GetTweetMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setCombinerClass(GetTweetReducer.class);
		
		job.setReducerClass(GetTweetReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		job.setInputFormatClass(TweetInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.waitForCompletion(true);
	}

}
