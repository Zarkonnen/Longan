package com.zarkonnen.fruitbat.atrio;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shorter, more-commented, low-performing reference implementation. */
public class SimpleATRReader {
	private final BufferedReader in;
	private boolean waitingForRecordStart = true;
	public SimpleATRReader(InputStream in) {
		this.in = new BufferedReader(new InputStreamReader(in));
	}
	public void close() throws IOException { in.close(); }

	public List<String> readRecord() throws IOException {
		// Since records start with a newline, we want to throw away the first one.
		if (waitingForRecordStart) {
			waitingForRecordStart = false;
			if (in.readLine() == null) { return null; }
		}
		while (true) {
			String line = in.readLine();
			// If there are no more lines, we've reached the end of the stream.
			if (line == null) { return null; }
			// There may be multiple aborted records on this line, so let's find the first end
			// marker and ignore everything else.
			int recordEndMarker = line.indexOf("%");
			// If there is no end marker, the line's defective, so try again.
			if (recordEndMarker == -1) { continue; }
			line = line.substring(0, recordEndMarker);
			// Now we can split along field start markers.
			String[] rawFields = line.split("[:]", -1);
			// Of course the record may just be empty.
			if (rawFields.length == 0) { return Collections.emptyList(); }
			// Note that the first field of the split must be empty, otherwise the second one didn't
			// actually start itself.
			int firstFieldIndex = rawFields[0].length() == 0 ? 1 : 2;
			// Now we go through the fields processing the escape codes, and ignoring those that do
			// not end with a field end marker.
			ArrayList<String> fields = new ArrayList<String>(rawFields.length);
			for (int i = firstFieldIndex; i < rawFields.length; i++) {
				String field = rawFields[i];
				if (!field.endsWith("\t")) { continue; }
				// First, let's deal with the hard bits: Unicode replacements.
				int backslashUIndex = -1;
				try {
					while ((backslashUIndex = field.indexOf("\\u", backslashUIndex + 1)) != -1) {
						// First, make sure there's enough space for the hexadecimal codepoint.
						if (backslashUIndex > field.length() - 7) {
							// No? Field's defective,
							continue;
						}
						// Use the next 6 letters after the backslash-u as a hexadecimal number.
						String hexNum = field.substring(backslashUIndex + 2, backslashUIndex + 8);
						String chars = new String(Character.toChars(Integer.parseInt(hexNum, 16)));
						// Now slot those chars in, replacing the original sequence. We can be sure
						// that the second substring won't fail us because the final character is a
						// field end marker (a tab).
						field = field.substring(0, backslashUIndex) + chars +
								field.substring(backslashUIndex + 8);
					}
				} catch (NumberFormatException e) {
					// Broken field!
					continue;
				}
				// With that over, we now replace the other escape sequences.
				field = field.replace("\\r", "\r");
				field = field.replace("\\n", "\n");
				field = field.replace("\\p",  "%");
				field = field.replace("\\c",  ":");
				field = field.replace("\\t", "\t");
				field = field.replace("\\\\", "\\");
				// Finally, get rid of the field end marker and put the field in.
				fields.add(field.substring(0, field.length() - 1));
			}

			return fields;
		}
	}
}