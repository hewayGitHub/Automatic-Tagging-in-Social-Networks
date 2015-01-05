/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CodecPool;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.io.compress.SplitCompressionInputStream;
import org.apache.hadoop.io.compress.SplittableCompressionCodec;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;

/**
 * Treats keys as offset in file and value as line.
 */
public class TweetRecordReader extends RecordReader<Text, Text> {
	private CompressionCodecFactory compressionCodecs = null;
	private long start;
	private long pos;
	private long end;
	private LineReader in;
	private Text key = null;
	private Text value = null;
	private Seekable filePosition;
	private CompressionCodec codec;
	private Decompressor decompressor;

	public void initialize(InputSplit genericSplit, TaskAttemptContext context)
			throws IOException {
		FileSplit split = (FileSplit) genericSplit;
		Configuration job = context.getConfiguration();

		start = split.getStart();
		end = start + split.getLength();
		final Path file = split.getPath();
		compressionCodecs = new CompressionCodecFactory(job);
		codec = compressionCodecs.getCodec(file);

		// open the file and seek to the start of the split
		FileSystem fs = file.getFileSystem(job);
		FSDataInputStream fileIn = fs.open(split.getPath());

		if (isCompressedInput()) {
			decompressor = CodecPool.getDecompressor(codec);
			if (codec instanceof SplittableCompressionCodec) {
				final SplitCompressionInputStream cIn = ((SplittableCompressionCodec) codec)
						.createInputStream(fileIn, decompressor, start, end,
								SplittableCompressionCodec.READ_MODE.BYBLOCK);
				in = new LineReader(cIn, job);
				start = cIn.getAdjustedStart();
				end = cIn.getAdjustedEnd();
				filePosition = cIn;
			} else {
				in = new LineReader(codec.createInputStream(fileIn,
						decompressor), job);
				filePosition = fileIn;
			}
		} else {
			fileIn.seek(start);
			in = new LineReader(fileIn, job);
			filePosition = fileIn;
		}

		this.pos = start;
	}

	private boolean isCompressedInput() {
		return (codec != null);
	}

	private long getFilePosition() throws IOException {
		long retVal;
		if (isCompressedInput() && null != filePosition) {
			retVal = filePosition.getPos();
		} else {
			retVal = pos;
		}
		return retVal;
	}

	public boolean nextKeyValue() throws IOException {
		if (key == null) {
			key = new Text();
		}

		if (value == null) {
			value = new Text();
		}

		int newSize = 0;

		String tempStr;
		int tempIndex;
		boolean getKey = false;
		while (getFilePosition() <= end || getKey) {
			Text tempValue = new Text();
			newSize = in.readLine(tempValue);
			if (newSize == 0) {
				break;
			}
			pos += newSize;

			tempStr = tempValue.toString();
			if (tempStr.length() == 0)
				continue;

			if (tempStr.charAt(0) == 'U') {
				tempIndex = tempStr.lastIndexOf('/');
				key.set(tempStr.substring(tempIndex + 1));

				getKey = true;
			} else if (tempStr.charAt(0) == 'W') {
				if (tempStr.equalsIgnoreCase("W	No Post Title")) {
					newSize = 0;
					continue;
				} else {
					value.set(tempStr.substring(1).trim());
					break;
				}
			}
		}
		if (newSize == 0) {
			key = null;
			value = null;
			return false;
		} else {
			return true;
		}
	}

	@Override
	public Text getCurrentKey() {
		return key;
	}

	@Override
	public Text getCurrentValue() {
		return value;
	}

	/**
	 * Get the progress within the split
	 */
	public float getProgress() throws IOException {
		if (start == end) {
			return 0.0f;
		} else {
			return Math.min(1.0f, (getFilePosition() - start)
					/ (float) (end - start));
		}
	}

	public synchronized void close() throws IOException {
		try {
			if (in != null) {
				in.close();
			}
		} finally {
			if (decompressor != null) {
				CodecPool.returnDecompressor(decompressor);
			}
		}
	}
}
