package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.nnidentifier.Config.FontType;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TreePredict {
	public static <T> T vote(HashMap<T, Integer> result) {
		T v = null;
		int amt = 0;
		for (Map.Entry<T, Integer> e : result.entrySet()) {
			if (v == null || amt < e.getValue()) {
				v = e.getKey();
				amt = e.getValue();
			}
		}
		return v;
	}
	
	static class DimCmp implements Comparator<Img> {
		final int dim;

		public DimCmp(int dim) {
			this.dim = dim;
		}

		public int compare(Img t, Img t1) {
			return Float.compare(t.data[dim], t1.data[dim]);
		}
	}
	
	public static TreeNode<Config.LetterClass> buildTree(Config.TreeIdentifier ti) {
		Random r = new Random(ti.seed);
		ArrayList<Img<Config.LetterClass>> imgs = new ArrayList<Img<Config.LetterClass>>();
		for (int i = 0; i < 300; i++) {
			for (Config.FontType ft : ti.fonts) {
				for (Config.LetterClass lc : ti.classes) {
					for (String l : lc.members) {
						imgs.add(getImg(l, ft, lc, r));
					}
				}
			}
		}
		return buildTree(imgs);
	}
	
	public static <T> TreeNode<T> buildTree(List<Img<T>> imgs) {
		if (imgs.isEmpty()) {
			return new TreeNode();
		}
		
		float currentScore = entropy(imgs);
		float bestGain = 0.0f;
		int bestDimension = 0;
		float bestTestValue = 0.0f;
		ArrayList<Img<T>>[] bestSets = null;
		
		for (int dim = 0; dim < imgs.get(0).data.length; dim++) {
			HashSet<Float> columnValues = new HashSet<Float>();
			for (Img img : imgs) {
				columnValues.add(img.data[dim]);
			}
			for (float value : columnValues) {
				ArrayList<Img<T>>[] sets = divideset(imgs, dim, value);
				float p = sets[0].size() * 1.0f / imgs.size();
				float gain = currentScore - p * entropy(sets[0]) - (1 - p) * entropy(sets[1]);
				if (gain > bestGain && !sets[0].isEmpty() && !sets[1].isEmpty()) {
					bestGain = gain;
					bestDimension = dim;
					bestTestValue = value;
					bestSets = sets;
				}
			}
		}
		if (bestGain > 0) {
			return new TreeNode<T>(bestDimension, bestTestValue, buildTree(bestSets[0]), buildTree(bestSets[1]));
		} else {
			return new TreeNode<T>(uniquecounts(imgs));
		}
	}
	
	public static <T> ArrayList<Img<T>>[] divideset(List<Img<T>> imgs, int dimension, float testValue) {
		ArrayList<Img<T>>[] sets = new ArrayList[2];
		sets[0] = new ArrayList<Img<T>>();
		sets[1] = new ArrayList<Img<T>>();
		for (Img img : imgs) {
			if (img.data[dimension] < testValue) {
				sets[0].add(img);
			} else {
				sets[1].add(img);
			}
		}
		return sets;
	}
	
	public static <T> HashMap<T, Integer> uniquecounts(List<Img<T>> imgs) {
		HashMap<T, Integer> counts = new HashMap<T, Integer>();
		for (Img<T> img : imgs) {
			if (!counts.containsKey(img.tag)) {
				counts.put(img.tag, 1);
			} else {
				counts.put(img.tag, counts.get(img.tag) + 1);
			}
		}
		return counts;
	}
	
	public static <T> float entropy(List<Img<T>> imgs) {
		return entropy(uniquecounts(imgs));
	}
	
	public static <T> float entropy(HashMap<T, Integer> counts) {
		float ent = 0.0f;
		int size = 0;
		for (int c : counts.values()) { size += c; }
		for (T tag : counts.keySet()) {
			float p = counts.get(tag) * 1.0f / size;
			ent -= p * Math.log(p) / Math.log(2);
		}
		return ent;
	}
	
	// IO
	public static void writeNode(TreeNode<Config.LetterClass> node, ObjectOutputStream oos) throws IOException {
		oos.writeInt(0); // Version
		oos.writeBoolean(node.results == null);
		if (node.results == null) {
			oos.writeInt(node.dimension);
			oos.writeFloat(node.testValue);
			writeNode(node.falseBranch, oos);
			writeNode(node.trueBranch, oos);
		} else {
			oos.writeInt(node.results.size());
			for (Map.Entry<Config.LetterClass, Integer> result : node.results.entrySet()) {
				oos.writeInt(result.getKey().members.size());
				for (String l : result.getKey().members) {
					oos.writeUTF(l);
				}
				oos.writeInt(result.getValue());
			}
		}
	}
	
	public static TreeNode<Config.LetterClass> readNode(ObjectInputStream ois) throws IOException {
		if (ois.readInt() > 0) {
			throw new IOException("TreeNode format too modern: I only support up to version 0.");
		}
		TreeNode<Config.LetterClass> node = new TreeNode<Config.LetterClass>();
		if (ois.readBoolean()) {
			node.results = null;
			node.dimension = ois.readInt();
			node.testValue = ois.readFloat();
			node.falseBranch = readNode(ois);
			node.trueBranch = readNode(ois);
		} else {
			int n = ois.readInt();
			for (int i = 0; i < n; i++) {
				Config.LetterClass lc = new Config.LetterClass();
				int ls = ois.readInt();
				for (int j = 0; j < ls; j++) {
					lc.members.add(ois.readUTF());
				}
				node.results.put(lc, ois.readInt());
			}
		}
		return node;
	}
	
	public static class TreeNode<T> {
		int dimension;
		float testValue;
		HashMap<T, Integer> results;
		TreeNode<T> falseBranch;
		TreeNode<T> trueBranch;
		
		public void prune(float minGain) {
			if (results != null) { return; }
			if (falseBranch.results != null) {
				falseBranch.prune(minGain);
			}
			if (trueBranch.results != null) {
				trueBranch.prune(minGain);
			}
			if (falseBranch.results != null && trueBranch.results != null) {
				HashMap<T, Integer> combined = new HashMap<T, Integer>(falseBranch.results);
				for (Map.Entry<T, Integer> e : trueBranch.results.entrySet()) {
					if (combined.containsKey(e.getKey())) {
						combined.put(e.getKey(), combined.get(e.getKey()) + e.getValue());
					} else {
						combined.put(e.getKey(), e.getValue());
					}
				}
				float delta = entropy(combined) - (entropy(falseBranch.results) + entropy(trueBranch.results)) / 2;
				if (delta < minGain) {
					results = combined;
					falseBranch = null;
					trueBranch = null;
				}
			}
		}
		
		public HashMap<T, Integer> classify(Img img) {
			if (results == null) {
				if (img.data[dimension] < testValue) {
					return falseBranch.classify(img);
				} else {
					return trueBranch.classify(img);
				}
			} else {
				return results;
			}
		}

		public TreeNode() { results = new HashMap<T, Integer>(); }

		public TreeNode(int dimension, float testValue, TreeNode falseBranch, TreeNode trueBranch) {
			this.dimension = dimension;
			this.testValue = testValue;
			this.falseBranch = falseBranch;
			this.trueBranch = trueBranch;
		}

		public TreeNode(HashMap<T, Integer> results) {
			this.results = results;
		}
		
		@Override
		public String toString() {
			return toString("  ");
		}

		private String toString(String indent) {
			StringBuilder sb = new StringBuilder();
			if (results == null) {
				sb.append("data[");
				sb.append(dimension);
				sb.append("] > ");
				sb.append(testValue);
				sb.append("\n");
				sb.append(indent);
				sb.append("? ");
				sb.append(trueBranch.toString(indent + "  "));
				sb.append("\n");
				sb.append(indent);
				sb.append(": ");
				sb.append(falseBranch.toString(indent + "  "));
				//sb.append("\n");
			} else {
				sb.append(str(results));
			}
			return sb.toString();
		}
	}
	
	static <T> String str(HashMap<T, Integer> results) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Map.Entry<T, Integer> e : results.entrySet()) {
			sb.append(" ");
			sb.append(e.getKey());
			sb.append(": ");
			sb.append(e.getValue());
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	public static class Img<T> {
		float[] data;
		T tag;
		int width = SZ;
		int height = SZ;

		public Img(float[] data, T tag) {
			this.data = data;
			this.tag = tag;
		}
	}
		
	public static <T> Img<T> getImg(String l, FontType ft, T tag, Random r) {
		return getImg(ExampleGenerator2.makeCorrectlyVariableLetterImage(l, ft, r), tag);
	}
		
	public static <T> Img<T> getImg(BufferedImage src, T tag) {
		return getImg(src, tag, 0);
	}
	
	static final int SZ = 9;
	
	public static <T> Img<T> getImg(BufferedImage src, T tag, int intensityAdjustment) {
		BufferedImage scaledSrc = new BufferedImage(SZ, SZ, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaledSrc.getGraphics();
		g.drawImage(src, 0, 0, SZ, SZ, 0, 0, src.getWidth(), src.getHeight(), null);
		src = scaledSrc;
		float[] result = new float[SZ * SZ];
		for (int y = 0; y < SZ; y++) { for (int x = 0; x < SZ; x++) {
			Color c = new Color(src.getRGB(x, y));
			result[y * SZ + x] = (c.getRed() + c.getGreen() + c.getBlue() + intensityAdjustment * 3) / 255.0f / 1.5f - 1;
		} }
		return new Img(result, tag);
	}
	
	public static Img getImg(Letter r, BufferedImage src, int intensityAdjustment) {
		// Masking
		BufferedImage maskedSrc = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
		Graphics g = maskedSrc.getGraphics();
		g.drawImage(
				src,
				0, 0,
				r.width, r.height,
				r.x, r.y,
				r.x + r.width, r.y + r.height,
				null);
		int white = Color.WHITE.getRGB();
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				boolean hasMask = false;
				for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
					int ny = y + dy;
					int nx = x + dx;
					if (ny >= 0 && ny < r.height && nx >= 0 && nx < r.width) {
						hasMask |= r.mask[ny][nx];
					}
				}}
				if (!hasMask) {
					maskedSrc.setRGB(x, y, white);
				}
			}
		}
		return getImg(maskedSrc, null, intensityAdjustment);
	}
}
