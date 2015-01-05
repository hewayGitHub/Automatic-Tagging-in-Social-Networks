package hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;


public class CountFollow {
	public static class CountFollowMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
		static final IntWritable FAN = new IntWritable(1);
		static final IntWritable FOLLOW = new IntWritable(-1);
		
		@Override
		protected void map(LongWritable key, Text value, 
	            Context context) throws IOException, InterruptedException {
			String[] items = value.toString().trim().split("\t");
			
			if(items.length == 2) {
				context.write(new Text(items[0]), FAN);
				context.write(new Text(items[1]), FOLLOW);
			}
		}
		
	}
	
	public static class CountFollowCombiner extends Reducer<Text, IntWritable, Text, IntWritable> {
		static final IntWritable FAN = new IntWritable(1);
		static final IntWritable FOLLOW = new IntWritable(0);
		
		@Override
		protected void reduce(Text key, Iterable<IntWritable> values, Context context
	            ) throws IOException, InterruptedException {
			int fanCount = 0, followCount = 0, count;
			for(IntWritable value: values) {
				count = value.get();
				if(count > 0) fanCount += count;
				else followCount += count;
			}
			
			if(fanCount != 0) context.write(key, new IntWritable(fanCount));
			if(followCount != 0) context.write(key, new IntWritable(followCount));
		}
	}
	
	public static class CountFollowReducer extends Reducer<Text, IntWritable, Text, Text> {
		static final IntWritable FAN = new IntWritable(1);
		static final IntWritable FOLLOW = new IntWritable(0);
		
		@Override
		protected void reduce(Text key, Iterable<IntWritable> values, Context context
	            ) throws IOException, InterruptedException {
			int fanCount = 0, followCount = 0, count;
			for(IntWritable value: values) {
				count = value.get();
				
				if(count > 0) fanCount += count;
				else followCount += count;
			}
			
			context.write(key, new Text(fanCount + "\t" + followCount));
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		Configuration jobConf = new Configuration();
		
		Job job = new Job(jobConf);
		job.setJobName("CountFollow");
		job.setJarByClass(CountFollow.class);
		
		job.setMapperClass(CountFollowMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setCombinerClass(CountFollowCombiner.class);
		
		job.setReducerClass(CountFollowReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.waitForCompletion(true);
	}
}
