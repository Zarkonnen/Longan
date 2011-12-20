package com.zarkonnen.longan.profilegen;

import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Config {	
	public static final class FontType {
		public final String font;
		public final boolean italic;

		public FontType(String font, boolean italic) {
			this.font = font;
			this.italic = italic;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof FontType)) { return false; }
			FontType ft2 = (FontType) o;
			return ft2.italic == italic && ft2.font.equals(font);
		}
		
		@Override
		public int hashCode() {
			return (italic ? 7 : 77) + font.hashCode();
		}
		
		@Override
		public String toString() { return font + (italic ? " Italic" : ""); }
	}
	
	public static class Identifier {
		public final FontType font;
		public ArrayList<LetterClass> classes = new ArrayList<LetterClass>();
		public IdentifierNet network;

		public Identifier(FontType font) {
			this.font = font;
		}
		
		@Override
		public String toString() { return "Identifier for " + font; }
		
		public Identifier(JSONObject json) throws JSONException {
			if (!json.optString("version", "1.0").equals("1.0")) {
				throw new JSONException("Unknown config file version.");
			}
			font = new FontType(json.getString("font"), json.optBoolean("italic", false));
			JSONArray classesA = json.getJSONArray("classes");
			for (int i = 0; i < classesA.length(); i++) {
				LetterClass lc = new LetterClass();
				classes.add(lc);
				String letters = classesA.getString(i);
				for (int j = 0; j < letters.length(); j++) {
					lc.members.add(letters.substring(j, j + 1));
				}
			}
		}
		
		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();
			json.put("type", "identifier");
			json.put("version", "1.0");
			json.put("font", font.font);
			json.put("italic", font.italic);
			JSONArray classesA = new JSONArray();
			json.put("classes", classesA);
			for (LetterClass lc : classes) {
				StringBuilder sb = new StringBuilder();
				for (String s : lc.members) { sb.append(s); }
				classesA.put(sb.toString());
			}
			return json;
		}
	}
	
	public static class LetterClass {
		public ArrayList<String> members = new ArrayList<String>();
		public float[] target;
		@Override
		public boolean equals(Object o) {
			return o instanceof LetterClass && ((LetterClass) o).members.equals(members);
		}
		@Override
		public int hashCode() {
			return 19 + members.hashCode();
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (String l : members) { sb.append(l); }
			return sb.toString();
		}
	}

	public static abstract class Discriminator {
		public FontType font;
		public final String trigger;
		public final String alternative;
		
		abstract String getTypeName();

		public Discriminator(FontType font, String triggerLetter, String alternateLetter) {
			this.font = font;
			this.trigger = triggerLetter;
			this.alternative = alternateLetter;
		}
		
		public Discriminator(JSONObject json) throws JSONException {
			if (!json.optString("version", "1.0").equals("1.0")) {
				throw new JSONException("Unknown config file version.");
			}
			font = new FontType(json.getString("font"), json.optBoolean("italic", false));
			trigger = json.getString("trigger");
			alternative = json.getString("alternative");
		}
		
		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();
			json.put("type", getTypeName());
			json.put("version", "1.0");
			json.put("font", font.font);
			json.put("italic", font.italic);
			json.put("trigger", trigger);
			json.put("alternative", alternative);
			return json;
		}
		
		@Override
		public String toString() {
			return getTypeName() + ": " + font + ": " + trigger + " vs " + alternative;
		}
	}
	
	public static class NNDiscriminator extends Discriminator {
		public DiscriminatorNet network;

		public NNDiscriminator(FontType font, String triggerLetter, String alternateLetter) {
			super(font, triggerLetter, alternateLetter);
		}
		
		public NNDiscriminator(JSONObject json) throws JSONException {
			super(json);
		}

		@Override
		String getTypeName() { return "discriminator"; }
	}
	
	public static class AspectRatioDiscriminator extends Discriminator {
		double boundaryRatio;
		boolean triggerIsAboveBoundary;
		
		public AspectRatioDiscriminator(FontType font, String triggerLetter, String alternateLetter) {
			super(font, triggerLetter, alternateLetter);
		}
		
		public AspectRatioDiscriminator(JSONObject json) throws JSONException {
			super(json);
		}
		
		@Override
		String getTypeName() { return "aspectRatioDiscriminator"; }
	}
	
	public static class RelativeSizeDiscriminator extends Discriminator {
		static String DEFAULT_SAMPLE_SENTENCE = "The quick brown fox jumped over the lazy dog.";
		double boundarySize;
		boolean triggerIsAboveBoundary;
		String sampleSentence = DEFAULT_SAMPLE_SENTENCE;
		
		public RelativeSizeDiscriminator(FontType font, String triggerLetter, String alternateLetter) {
			super(font, triggerLetter, alternateLetter);
		}
		
		public RelativeSizeDiscriminator(JSONObject json) throws JSONException {
			super(json);
			sampleSentence = json.optString("sampleSentence", DEFAULT_SAMPLE_SENTENCE);
		}
		
		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject o = super.toJSON();
			o.put("sampleSentence", sampleSentence);
			return o;
		}
		
		@Override
		String getTypeName() { return "relativeSizeDiscriminator"; }
	}
	
	public static class NumberOfPartsDiscriminator extends Discriminator {
		int numberOfPartsBoundary;
		boolean triggerIsAboveBoundary;
		boolean enabled = false;
		
		public NumberOfPartsDiscriminator(FontType font, String triggerLetter, String alternateLetter) {
			super(font, triggerLetter, alternateLetter);
		}
		
		public NumberOfPartsDiscriminator(JSONObject json) throws JSONException {
			super(json);
		}
		
		@Override
		String getTypeName() { return "numberOfPartsDiscriminator"; }
	}
	
	public Config() {}
	
	public ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
	public ArrayList<Discriminator> discriminators = new ArrayList<Discriminator>();
	
	public Config(JSONObject json) throws IOException{
		try {
			if (!json.optString("version", "1.0").equals("1.0")) {
				throw new IOException("Unknown config file version.");
			}
			JSONArray contentsA = json.getJSONArray("contents");
			for (int i = 0; i < contentsA.length(); i++) {
				JSONObject item = contentsA.getJSONObject(i);
				if (item.get("type").equals("identifier")) {
					identifiers.add(new Identifier(item));
				}
				if (item.get("type").equals("discriminator")) {
					discriminators.add(new NNDiscriminator(item));
				}
				if (item.get("type").equals("aspectRatioDiscriminator")) {
					discriminators.add(new AspectRatioDiscriminator(item));
				}
				if (item.get("type").equals("relativeSizeDiscriminator")) {
					discriminators.add(new RelativeSizeDiscriminator(item));
				}
				if (item.get("type").equals("numberOfPartsDiscriminator")) {
					discriminators.add(new NumberOfPartsDiscriminator(item));
				}
			}
		} catch (JSONException je) {
			throw new IOException("Could not parse config file.", je);
		}
	}
	
	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("version", "1.0");
		json.put("type", "config");
		JSONArray contentsA = new JSONArray();
		json.put("contents", contentsA);
		for (Identifier i : identifiers) {
			contentsA.put(i.toJSON());
		}
		for (Discriminator d : discriminators) {
			contentsA.put(d.toJSON());
		}
		return json;
	}
}
