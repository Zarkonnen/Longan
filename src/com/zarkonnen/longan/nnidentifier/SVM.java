package com.zarkonnen.longan.nnidentifier;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

public class SVM {
	static final int IMG_SZ = 1000;
	
	public static class Img<T> {
		float[] data;
		T tag;

		public Img(float[] data, T tag) {
			this.data = data;
			this.tag = tag;
		}
	}
	
	static float[] expand(float[] original) {
		float[] expanded = new float[5];
		expanded[0] = original[0];
		expanded[1] = original[1];
		
		// order 2
		expanded[2] = original[0] * original[0];
		expanded[3] = original[1] * original[1];
		expanded[4] = original[0] * original[1];
		
		// order 3
		/*expanded[5] = original[0] * original[0] * original[0];
		expanded[6] = original[0] * original[0] * original[1];
		expanded[7] = original[0] * original[1] * original[0];
		expanded[8] = original[0] * original[1] * original[1];
		expanded[9] = original[1] * original[0] * original[0];
		expanded[10]= original[1] * original[0] * original[1];
		expanded[11] = original[1] * original[1] * original[0];
		expanded[12] = original[1] * original[1] * original[1];*/
		return expanded;
	}
	
	static float dot(float[] a, float[] b) {
		a = expand(a);
		b = expand(b);
		float d = 0.0f;
		for (int i = 0; i < a.length; i++) {
			d += a[i] * b[i];
		}
		return d;
	}
	
	static class SV {
		float[] v;
		float d;
		Color aTag;
		Color bTag;

		public SV(float[] v, float d, Color aTag, Color bTag) {
			this.v = v;
			this.d = d;
			this.aTag = aTag;
			this.bTag = bTag;
		}
		
		public Color classify(Img<Color> e) {
			float dist = d - dot(v, e.data);
			return dist < 0 ? aTag : bTag;
		}
	}
	
	static SV findBestSV(ArrayList<Img<Color>> els) {
		int bestScore = -1;
		float[] bestV = null;
		float bestD = 0;
		Color aTag = null;
		Color bTag = null;
		for (Img<Color> a : els) {
			for (Img<Color> b : els) {
				if (a.tag.equals(b.tag)) { continue; }
				float[] vector = new float[a.data.length];
				float[] avg = new float[a.data.length];
				for (int i = 0; i < vector.length; i++) {
					vector[i] = a.data[i] - b.data[i];
					avg[i] = a.data[i] / 2 + b.data[i] / 2;
				}
				float d = dot(vector, avg);
				
				// How good is this?
				int score = 0;
				for (Img<Color> e : els) {
					float dist = d - dot(vector, e.data);
					Color tag = dist < 0 ? a.tag : b.tag;
					if (tag.equals(e.tag)) { score++; }
				}
				if (score > bestScore) {
					bestScore = score;
					bestV = vector;
					bestD = d;
					aTag = a.tag;
					bTag = b.tag;
				}
			}
		}
		
		return bestV == null ? null : new SV(bestV, bestD, aTag, bTag);
	}
	
	static class SVNode {
		SV sv;
		SVNode aBranch;
		SVNode bBranch;
		
		public Color classify(Img<Color> e) {
			Color c = sv.classify(e);
			if (c == sv.aTag && aBranch != null) {
				return aBranch.classify(e);
			}
			if (c == sv.bTag && bBranch != null) {
				return bBranch.classify(e);
			}
			return c;
		}
	}
	
	static SVNode createSVTree(ArrayList<Img<Color>> els, int maxD, int minImprovement) {
		if (maxD == 0) { return null; }
		SVNode node = new SVNode();
		node.sv = findBestSV(els);
		if (node.sv == null) {
			return null;
		}
		ArrayList<Img<Color>> aList = new ArrayList<Img<Color>>();
		ArrayList<Img<Color>> bList = new ArrayList<Img<Color>>();
		for (Img<Color> e : els) {
			if (node.sv.classify(e) == node.sv.aTag) {
				aList.add(e);
			} else {
				bList.add(e);
			}
		}
		if (aList.size() < minImprovement || bList.size() < minImprovement) {
			return node;
		}
		node.aBranch = createSVTree(aList, maxD - 1, minImprovement);
		node.bBranch = createSVTree(bList, maxD - 1, minImprovement);
		return node;
	}
	
	public static void main(String[] args) throws IOException {
		ArrayList<Img<Color>> els = new ArrayList<Img<Color>>();
		Random r = new Random();
		ArrayList<Img<Color>> attractors = new ArrayList<Img<Color>>();
		for (int i = 0; i < 10; i++) {
			float[] data = new float[] { r.nextFloat(), r.nextFloat() };
			Color tag = i < 5 ? Color.RED : Color.BLUE;
			attractors.add(new Img<Color>(data, tag));
		}
		for (int i = 0; i < 1000; i++) {
			float[] data = new float[] { r.nextFloat(), r.nextFloat() };
			// Make everything within 0.3 of { 0.8, 0.5 } RED.
			/*Color tag = (data[0] - 0.8) * (data[0] - 0.8) + (data[1] - 0.5) * (data[1] - 0.5) < 0.3 * 0.3
					? Color.RED : Color.BLUE;*/
			Img<Color> bestA = null;
			float bestD = 0;
			for (Img<Color> a : attractors) {
				float dist = (a.data[0] - data[0]) * (a.data[0] - data[0]) + (a.data[1] - data[1]) * (a.data[1] - data[1]);
				if (bestA == null || dist < bestD) {
					bestA = a;
					bestD = dist;
				}
			}
			els.add(new Img<Color>(data, bestA.tag));
		}
		
		BufferedImage img = new BufferedImage(IMG_SZ, IMG_SZ, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, IMG_SZ, IMG_SZ);
		
		// Find SV Tree
		SVNode tree = createSVTree(els, 20, 3);
		
		// Draw support vector by checking each pixel of the display.
		drawVector(tree, g, 0);

		// Draw classes.
		int score = 0;
		for (Img<Color> e : els) {
			Color c = tree.classify(e);
			if (c == e.tag) { score++; }
			g.setColor(c);
			g.drawRect((int) (IMG_SZ * e.data[0]) - 2, (int) (IMG_SZ * e.data[1]) - 2, 5, 5);
		}
		
		// Draw points with correct classes.
		for (Img<Color> e : els) {
			g.setColor(e.tag);
			g.fillRect((int) (IMG_SZ * e.data[0]) - 1, (int) (IMG_SZ * e.data[1]) - 1, 4, 4);
		}
		
		g.setColor(Color.BLACK);
		g.drawString(score + "/" + els.size(), 10, 20);
		
		ImageIO.write(img, "png", new File("/Users/zar/Desktop/out.png"));
	}
	
	static int drawC(SVNode n, float[] coords, int level) {
		float dist = n.sv.d - dot(n.sv.v, coords);
		if (Math.abs(dist) < 0.0001) {
			return Math.min(220, 120 + level * 30);
		}
		if (dist < 0 && n.aBranch != null) {
			return drawC(n.aBranch, coords, level + 1);
		}
		if (dist > 0 && n.bBranch != null) {
			return drawC(n.bBranch, coords, level + 1);
		}
		return 255;
	}
	
	static void drawVector(SVNode n, Graphics2D g, int level) {
		for (int y = 0; y < IMG_SZ; y++) { for (int x = 0; x < IMG_SZ; x++) {
			int intensity = drawC(n, new float[] { x * 1.0f / IMG_SZ, y * 1.0f / IMG_SZ}, 0);
			if (intensity == 255) { continue; }
			g.setColor(new Color(intensity, intensity, intensity));
			g.fillRect(x, y, 1, 1);
		}}
		/*if (n.aBranch != null) { drawVector(n.aBranch, g, level + 1); }
		if (n.bBranch != null) { drawVector(n.bBranch, g, level + 1); }
		int intensity = Math.min(220, 120 + level * 30);
		g.setColor(new Color(intensity, intensity, intensity));
		for (int y = 0; y < IMG_SZ; y++) { for (int x = 0; x < IMG_SZ; x++) {
			float dist = n.sv.d - dot(n.sv.v, new float[] { x * 1.0f / IMG_SZ, y * 1.0f / IMG_SZ});
			if (Math.abs(dist) < 0.0005) {
				g.fillRect(x, y, 1, 1);
			}
		}}*/
	}
}
