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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Writes records of UTF-8 fields into an ASCII-based format called Atomic Text Records. When used
 * append-only, ATRReader (and SimpleATRReader) will ignore all fields and records that failed to be
 * written in their entirety, making appending fields and records atomic in practice.
 *
 * The format only uses visible ASCII characters in the range of " " (32) to "~" (126). The record
 * start marker is line feed, and the field end marker is tab, making the format readable as TSV.
 * However note that records not terminated with "%" and fields not started with ":" should be
 * ignored.
 *
 * Backslash, colon, tab, line feed, percent and carriage return are all escaped using two-character
 * escape codes, while all other characters not in the range of " " to "~" are represented as
 * six-digit hexadecimal representations of their unicode code points.
 */
public class ATRWriter {
	// The range in which characters (with the exception of backslash, %, lf, :, tab, and cr) can be
	// put in directly.
	static final int RAW_MIN   =  32;
	static final int RAW_MAX   = 126;

	// Backslash used for all escape sequences, u for unicode hex codes.
	static final int ESCAPE    = (int) '\\'; static final int UNICODE_E = (int) 'u';

	// : starts fields, \t ends them.
	static final int F_START   = (int)  ':'; static final int F_START_E = (int) 'c';
	static final int F_END     = (int) '\t'; static final int F_END_E   = (int) 't';

	// \n starts records, % ends them.
	static final int R_START   = (int) '\n'; static final int R_START_E = (int) 'n';
	static final int R_END     = (int)  '%'; static final int R_END_E   = (int) 'p';
	
	// Also escaping carriage return for sanity purposes.
	static final int CR        = (int) '\r'; static final int CR_E      = (int) 'r';

	// Base numbers for quickly encoding hexadecimal.
	static final int ZERO      = (int)  '0'; static final int A         = (int) 'a';

	private final OutputStream out;

	/** Note: ATRWriter is unbuffered. */
	public ATRWriter(OutputStream out) { this.out = out; }
	public void close() throws IOException { out.close(); }
	public void flush() throws IOException { out.flush(); }

	public void startRecord() throws IOException {
		out.write(R_START);
	}

	public void endRecord() throws IOException {
		out.write(R_END);
	}

	/**
	 * Writes a record containing the fields. If the previous record wasn't ended, it will be
	 * ignored.
	 */
	public void writeRecord(Collection<String> fields) throws IOException {
		startRecord();
		for (String f : fields) { write(f); }
		endRecord();
	}

	public void write(String field) throws IOException {
		out.write(F_START);
		int charIndex = 0;
		while (charIndex < field.length()) {
			int codePoint = field.codePointAt(charIndex);
			charIndex += Character.charCount(codePoint);
			switch (codePoint) {
				case ESCAPE:
					out.write(ESCAPE); out.write(ESCAPE);    break;
				case F_START:
					out.write(ESCAPE); out.write(F_START_E); break;
				case F_END:
					out.write(ESCAPE); out.write(F_END_E);   break;
				case R_START:
					out.write(ESCAPE); out.write(R_START_E); break;
				case R_END:
					out.write(ESCAPE); out.write(R_END_E);   break;
				case CR:
					out.write(ESCAPE); out.write(CR_E);      break;
				default: {
					if (codePoint >= RAW_MIN && codePoint <= RAW_MAX) {
						out.write(codePoint);
					} else {
						// Write the codepoint in its full glory. To do this, we write the escape
						// char, then the unicode escape char, then six hexadecimal digits.
						out.write(ESCAPE);
						out.write(UNICODE_E);
						for (int shift = 5 * 4; shift >= 0; shift -= 4) {
							int nyb = (codePoint >>> shift) % 16;
							out.write(nyb < 10 ? ZERO + nyb : A - 10 + nyb);
						}
					}
				}
			}
		}

		out.write(F_END);
	}
}
