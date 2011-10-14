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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import static com.zarkonnen.fruitbat.atrio.ATRWriter.*;

/** Reader for the ATR format. (See ATRWriter.) */
public class ATRReader {
	static final int EOS = -1;

	private final InputStream in;
	private char[] buffer = new char[128];
	private int bOffset = 0;
	private boolean endOfRecord = true;
	private boolean cleanEndOfRecord = true;
	private boolean endOfStream = false;
	private Expecting expecting;

	public ATRReader(InputStream in) { this.in = in; }
	public void close() throws IOException { in.close(); }

	/** @return If we've reached the end of the stream. */
	public boolean endOfStream() { return endOfStream; }

	/** @return If we've reached the end of the record. */
	public boolean endOfRecord() { return endOfRecord; }

	/** @return If the last record read ended cleanly. */
	public boolean cleanEndOfRecord() { return cleanEndOfRecord; }

	static enum Expecting {
		START_OF_FIELD,
		FIELD_CONTENTS,
		ESCAPE_CODE,
		UNICODE_DIGIT;
	}

	void resetField() {
		expecting = Expecting.START_OF_FIELD;
		bOffset = 0;
	}

	/** @return The next valid record, or null if at the end of stream. */
	public List<String> readRecord() throws IOException {
		ArrayList<String> fs = new ArrayList<String>();
		do {
			fs.clear();
			String f;
			while ((f = read()) != null) {
				fs.add(f);
			}
			if (endOfStream()) { return null; }
		} while (!cleanEndOfRecord());
		return fs;
	}

	/**
	 * Reads record, storing the first max fields into into, discarding the others.
	 * @return The number of fields actually read, or -1 if at the end of stream.
	 */
	public int readRecord(String[] into, int offset, int max) throws IOException {
		while (true) {
			int fieldsRead = 0;
			String f;
			while ((f = read()) != null) {
				if (fieldsRead < max) {
					into[offset + fieldsRead++] = f;
				}
			}
			if (endOfStream()) { return -1; }
			if (cleanEndOfRecord()) { return fieldsRead; }
		}
	}

	/**
	 * Read a field - note that the field's record may be invalid, so check cleanEndOfRecord when
	 * you get back null, or you'll read in garbage data!
	 * @return The next field, or null if at the end of record or end of stream.
	 */
	public String read() throws IOException {
		int codePoint = 0;
		int codePointShift = 24;

		resetField();
		int code;
		while (true) {
			// Read in new code and ensure buffer size.
			code = in.read();
			if (bOffset > buffer.length - 2) {
				char[] b2 = new char[buffer.length * 2];
				System.arraycopy(buffer, 0, b2, 0, bOffset);
				buffer = b2;
			}
			switch (code) {
				// If an end of stream comes unexpectedly, the user can tell because
				// cleanEndOfRecord is false.
				case EOS: {
					if (!endOfRecord) {
						endOfRecord = true;
						cleanEndOfRecord = false;
					}
					endOfStream = true;
					return null;
				}
				// If a start of record comes unexpectedly, we return null to indicate a record end,
				// discarding the field, but set cleanEndOfRecord to false. Otherwise, we can just
				// ignore this code.
				case R_START: {
					if (!endOfRecord) {
						endOfRecord = true;
						cleanEndOfRecord = false;
						return null;
					} else {
						endOfRecord = false;
					}
					break;
				}
				// A clean end of record. If there was a half-built field, we just get rid of it.
				case R_END: {
					endOfRecord = true;
					cleanEndOfRecord = true;
					return null;
				}
				// At a start-of-field we discard whatever we may have had in an aborted previous
				// field and start accepting field contents.
				case F_START: {
					bOffset = 0;
					expecting = Expecting.FIELD_CONTENTS;
					break;
				}
				// At an unexpected end of field, discard its contents, and start afresh. This may
				// happen if writing fails halfway through an escape sequence, for example.
				// At an expected end of field, we can proudly return the field's contents.
				case F_END: {
					if (expecting != Expecting.FIELD_CONTENTS) {
						resetField();
					} else {
						return new String(buffer, 0, bOffset);
					}
					break;
				}
				// If we find any unescaped carriage returns, we assume someone has been using
				// Windows again, and we just discard them.
				case CR: {
					break;
				}
				// Same thing with an unexpected escape character.
				case ESCAPE: {
					if (expecting == Expecting.FIELD_CONTENTS) {
						expecting = Expecting.ESCAPE_CODE;
						break;
					}
					if (expecting != Expecting.ESCAPE_CODE) {
						resetField();
						break;
					}
					// Otherwise WE FALL THROUGH TO DEFAULT!!!
				}
				default: {
					// It's not a control character. What we do with it depends on what we're
					// expecting at this point in the stream.
					switch (expecting) {
						// If we wanted a start-of-field and got gibberish instead, ignore it.
						case START_OF_FIELD: {
							break;
						}
						// If we're just looking for normal field contents, check if they are within
						// the unescaped ASCII range, and if yes, add them. Otherwise, we have some
						// gibberish on our hands, so reset the field and wait for the next start of
						// field.
						case FIELD_CONTENTS: {
							if (code >= RAW_MIN && code <= RAW_MAX) {
								buffer[bOffset++] = (char) code;
							} else {
								resetField();
							}
							break;
						}
						// We are expecting an escape code.
						case ESCAPE_CODE: {
							// Usually, we'll next want normal field contents again, unless this is
							// the start of an unicode escape sequence.
							expecting = Expecting.FIELD_CONTENTS;
							switch (code) {
								case ESCAPE:    buffer[bOffset++] = (char) ESCAPE;  break;
								case R_START_E: buffer[bOffset++] = (char) R_START; break;
								case R_END_E:   buffer[bOffset++] = (char) R_END;   break;
								case F_START_E: buffer[bOffset++] = (char) F_START; break;
								case F_END_E:   buffer[bOffset++] = (char) F_END;   break;
								case CR_E:      buffer[bOffset++] = (char) CR;      break;
								case UNICODE_E: {
									expecting = Expecting.UNICODE_DIGIT;
									codePoint = 0;
									codePointShift = 24;
									break;
								}
								default: resetField();
							}
							break;
						}
						case UNICODE_DIGIT: {
							codePointShift -= 4;
							codePoint = codePoint | (Character.getNumericValue(code) << codePointShift);
							if (codePointShift == 0) {
								bOffset += Character.toChars(codePoint, buffer, bOffset);
								expecting = Expecting.FIELD_CONTENTS;
							}
							break;
						}
					}
				}
			}
		}
	}
}
