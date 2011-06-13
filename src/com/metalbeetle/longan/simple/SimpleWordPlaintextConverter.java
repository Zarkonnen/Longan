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

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.PlaintextConverter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class SimpleWordPlaintextConverter implements PlaintextConverter {
	public String convert(
			ArrayList<ArrayList<ArrayList<Letter>>> lines,
			BufferedImage img,
			HashMap<String, String> metadata)
	{
		StringBuilder sb = new StringBuilder();
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				for (Letter letter : word) {
					sb.append(letter.bestLetter());
				}
				sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
