package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.nnidentifier.network.Network;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	
	public static class NNIdentifier extends Identifier {
		public ArrayList<Network> networks = new ArrayList<Network>();
		public ArrayList<FastLoadingNetwork> fastNetworks = new ArrayList<FastLoadingNetwork>();
		public ArrayList<HashMap<LetterClass, ArrayList<float[]>>> targets = new ArrayList<HashMap<LetterClass, ArrayList<float[]>>>();
		public int numberOfNetworks;
		public boolean proportionalInput = true;
		
		public NNIdentifier(JSONObject json) throws JSONException {
			super(json);
			numberOfNetworks = json.optInt("numberOfNetworks", 1);
			proportionalInput = json.optBoolean("proportionalInput", true);
		}
		
		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject json = super.toJSON();
			json.put("type", "nnIdentifier");
			json.put("numberOfNetworks", numberOfNetworks);
			json.put("proportionalInput", proportionalInput);
			return json;
		}
		
		@Override
		public String toString() { return "Neural Network " + super.toString(); }
	}
	
	public static class TreeIdentifier extends Identifier {
		public TreePredict.TreeNode<LetterClass> tree;
		
		public TreeIdentifier(JSONObject json) throws JSONException {
			super(json);
		}
		
		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject json = super.toJSON();
			json.put("type", "treeIdentifier");
			return json;
		}
		
		@Override
		public String toString() { return "Tree " + super.toString(); }
	}
	
	public static class NearestNeighbourIdentifier extends Identifier {
		public NearestNeighbour.Comparisons comparisons;
		
		public NearestNeighbourIdentifier(JSONObject json) throws JSONException {
			super(json);
		}
		
		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject json = super.toJSON();
			json.put("type", "nearestNeighbourIdentifier");
			return json;
		}
		
		@Override
		public String toString() { return "Nearest Neighbour " + super.toString(); }
	}
	
	public static class NumberOfPartsIdentifier extends Identifier {
		int numberOfPartsBoundary;
		boolean firstIsAboveBoundary;
		boolean enabled = false;
		
		public NumberOfPartsIdentifier(JSONObject json) throws JSONException {
			super(json);
		}
		
		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject json = super.toJSON();
			json.put("type", "numberOfPartsIdentifier");
			return json;
		}
		
		@Override
		public String toString() { return "Number of Parts " + super.toString(); }
	}
	
	public static abstract class Identifier {
		static String DEFAULT_SAMPLE_SENTENCE = "The quick brown fox jumped over the lazy dog.";
		public ArrayList<FontType> fonts;
		public ArrayList<LetterClass> classes = new ArrayList<LetterClass>();
		public ArrayList<String> allLetters = new ArrayList<String>();
		public boolean root = false;
		public HashMap<String, Double> expectedRelativeSizes;
		public HashMap<String, Double> expectedAspectRatios;
		public String sampleSentence = DEFAULT_SAMPLE_SENTENCE;
		public long seed = System.currentTimeMillis();
		
		@Override
		public String toString() { return "Identifier for " + Arrays.toString(fonts.toArray()) +
				" and " + Arrays.toString(classes.toArray()); }
		
		public Identifier(JSONObject json) throws JSONException {
			if (!json.optString("version", "1.0").equals("1.0")) {
				throw new JSONException("Unknown config file version.");
			}
			seed = json.optLong("seed", seed);
			root = json.optBoolean("root", false);
			sampleSentence = json.optString("sampleSentence", DEFAULT_SAMPLE_SENTENCE);
			fonts = new ArrayList<FontType>();
			JSONArray fontsA = json.getJSONArray("fonts");
			for (int i = 0; i < fontsA.length(); i++) {
				fonts.add(new FontType(fontsA.getJSONObject(i).getString("font"),
						fontsA.getJSONObject(i).optBoolean("italic", false)));
			}
			
			JSONArray classesA = json.getJSONArray("classes");
			for (int i = 0; i < classesA.length(); i++) {
				LetterClass lc = new LetterClass();
				classes.add(lc);
				String letters = classesA.getString(i);
				for (int j = 0; j < letters.length(); j++) {
					lc.members.add(letters.substring(j, j + 1));
				}
				allLetters.addAll(lc.members);
			}
		}
		
		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();
			json.put("version", "1.0");
			json.put("root", root);
			json.put("seed", seed);
			json.put("sampleSentence", sampleSentence);
			JSONArray fontsA = new JSONArray();
			json.put("fonts", fontsA);
			for (FontType f : fonts) {
				JSONObject fO = new JSONObject();
				fO.put("font", f.font);
				fO.put("italic", f.italic);
				fontsA.put(fO);
			}
			
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
		public boolean contains(LetterClass lc2) {
			return members.containsAll(lc2.members);
		}
	}
	
	public Config() {}
	
	public ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
	
	public Config(JSONObject json) throws IOException{
		try {
			if (!json.optString("version", "1.0").equals("1.0")) {
				throw new IOException("Unknown config file version.");
			}
			JSONArray contentsA = json.getJSONArray("contents");
			for (int i = 0; i < contentsA.length(); i++) {
				JSONObject item = contentsA.getJSONObject(i);
				if (item.get("type").equals("nnIdentifier")) {
					identifiers.add(new NNIdentifier(item));
				}
				if (item.get("type").equals("numberOfPartsIdentifier")) {
					identifiers.add(new NumberOfPartsIdentifier(item));
				}
				if (item.get("type").equals("nearestNeighbourIdentifier")) {
					identifiers.add(new NearestNeighbourIdentifier(item));
				}
				if (item.get("type").equals("treeIdentifier")) {
					identifiers.add(new TreeIdentifier(item));
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
		return json;
	}
}
