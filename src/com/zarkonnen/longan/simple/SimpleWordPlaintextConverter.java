package com.zarkonnen.longan.simple;

/*
 * Copyright 2011 David Stark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.stage.ResultConverter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SimpleWordPlaintextConverter implements ResultConverter<String> {
	public String convert(Result result) {
		StringBuilder sb = new StringBuilder();
		for (Column c : result.columns) {
			for (Line l : c.lines) {
				for (Word w : l.words) {
					for (Letter letter : w.letters) {
						sb.append(letter.bestLetter());
					}
					sb.append(" ");
				}
				sb.append("\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public void write(String output, OutputStream stream) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(stream);
		osw.write(output);
		osw.flush();
	}
}
