package hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.SplittableCompressionCodec;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

public class TweetInputFormat extends FileInputFormat<Text, Text>{

	@Override
	public RecordReader<Text, Text> createRecordReader(InputSplit split,
			TaskAttemptContext context) throws IOException,
			InterruptedException {
		// TODO Auto-generated method stub
		return new TweetRecordReader();
	}

	  @Override
	  protected boolean isSplitable(JobContext context, Path file) {
	    CompressionCodec codec = 
	      new CompressionCodecFactory(context.getConfiguration()).getCodec(file);
	    if (null == codec) {
	      return true;
	    }
	    return codec instanceof SplittableCompressionCodec;
	  }
}
