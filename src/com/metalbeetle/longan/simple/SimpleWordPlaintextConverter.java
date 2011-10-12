package com.metalbeetle.longan.simple;

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

import com.metalbeetle.longan.data.Column;
import com.metalbeetle.longan.data.Letter;
import com.metalbeetle.longan.data.Line;
import com.metalbeetle.longan.data.Result;
import com.metalbeetle.longan.data.Word;
import com.metalbeetle.longan.stage.PlaintextConverter;

public class SimpleWordPlaintextConverter implements PlaintextConverter {
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
}
