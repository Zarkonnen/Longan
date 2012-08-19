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
	static final int IMG_SZ = 800;
	static final int DIMS = 81;
	
	public static class Img<T> {
		float[] data;
		T tag;

		public Img(float[] data, T tag) {
			this.data = data;
			this.tag = tag;
		}
	}
	
	static float[] expand(float[] original) {
		float[] expanded = new float[6];
		expanded[0] = original[0];
		expanded[1] = original[1];
		
		// order 2
		expanded[2] = original[0] * original[0];
		expanded[3] = original[1] * original[1];
		expanded[4] = original[0] * original[1];
		
		expanded[5] = original[0] * original[0] + original[1] * original[1];
		
		/*
		// order 2-1
		expanded[5] = original[0] * original[1] + original[0] * original[0];
		expanded[6] = original[0] * original[1] + original[1] * original[1];
		expanded[7] = original[1] * original[0] + original[0] * original[0];
		expanded[8] = original[1] * original[0] + original[1] * original[1];
		
		// order 3
		expanded[9] = original[0] * original[0] * original[0];
		expanded[10] = original[0] * original[0] * original[1];
		expanded[11] = original[0] * original[1] * original[0];
		expanded[12] = original[0] * original[1] * original[1];
		expanded[13] = original[1] * original[0] * original[0];
		expanded[14]= original[1] * original[0] * original[1];
		expanded[15] = original[1] * original[1] * original[0];
		expanded[16] = original[1] * original[1] * original[1];
		
		expanded[17] = original[0] - original[1];
		
		expanded[18] = (1 - original[0]) * (1 - original[0]);
		expanded[19] = (1 - original[1]) * (1 - original[1]);
		expanded[20] = (1 - original[0]) * (1 - original[1]);
		
		expanded[21] = original[0] * (1 - original[1]);
		expanded[22] = (1 - original[0]) * original[1];*/
		return expanded;
	}
	
	static float realDot(float[] a, float[] b) {
		a = expand(a);
		b = expand(b);
		float d = 0.0f;
		for (int i = 0; i < a.length; i++) {
			d += a[i] * b[i];
		}
		return d;
	}
	
	static float dotGaussian(float[] a, float[] b) {
		double normSq = 0;
		for (int i = 0; i < a.length; i++) {
			normSq += (a[i] - b[i]) * (a[i] - b[i]);
		}
		return (float) Math.exp(-0.5 * normSq);
	}
	
	static float dotSigmoid(float[] a, float[] b) {
		return (float) Math.tanh(0.7 * realDot(a, b) + 0.3);
	}
	
	static float dot(float[] a, float[] b) {
		return dotGaussian(a, b);
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
	
	static SV findBestSV(ArrayList<Img<Color>> allEls, ArrayList<Img<Color>> testEls) {
		int bestScore = 0;
		float[] bestV = null;
		float bestD = 0;
		Color aTag = null;
		Color bTag = null;
		float bestNormSquare = 0;
		for (Img<Color> a : allEls) {
			for (Img<Color> b : allEls) {
				if (a.tag.equals(b.tag)) { continue; }
				float[] vector = new float[a.data.length];
				float[] avg = new float[a.data.length];
				for (int i = 0; i < vector.length; i++) {
					vector[i] = a.data[i] - b.data[i];
					avg[i] = a.data[i] / 2 + b.data[i] / 2;
				}
				float d = dot(vector, avg);
				
				//for (float d = -4; d < 4; d += 0.05f) {
					// How good is this?
					int score = 0;
					for (Img<Color> e : testEls) {
						float dist = d - dot(vector, e.data);
						Color tag = dist < 0 ? a.tag : b.tag;
						if (tag.equals(e.tag)) { score++; }
					}
					float normSquare = 0;
					for (float f : vector) { normSquare += f * f; }
					if (score > bestScore || (score == bestScore && normSquare < bestNormSquare)) {
						bestScore = score;
						bestV = vector;
						bestD = d;
						aTag = a.tag;
						bTag = b.tag;
						bestNormSquare = normSquare;
					}
				//}
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
	
	static SVNode createSVTree(ArrayList<Img<Color>> allEls, ArrayList<Img<Color>> testEls, int maxD, int minImprovement,
			Color currentTag)
	{
		if (maxD == 0) { return null; }
		SVNode node = new SVNode();
		node.sv = findBestSV(allEls, testEls);
		if (node.sv == null) {
			return null;
		}
		ArrayList<Img<Color>> aList = new ArrayList<Img<Color>>();
		ArrayList<Img<Color>> bList = new ArrayList<Img<Color>>();
		int aError = 0;
		int bError = 0;
		for (Img<Color> e : testEls) {
			if (node.sv.classify(e).equals(node.sv.aTag)) {
				if (!e.tag.equals(node.sv.aTag)) {
					aError++;
				}
				aList.add(e);
			} else {
				if (!e.tag.equals(node.sv.bTag)) {
					bError++;
				}
				bList.add(e);
			}
		}
		
		if (aError > minImprovement) {
			node.aBranch = createSVTree(allEls, aList, maxD - 1, minImprovement, node.sv.aTag);
		}
		if (bError > minImprovement) {
			node.bBranch = createSVTree(allEls, bList, maxD - 1, minImprovement, node.sv.bTag);
		}
		return node;
	}
	
	static float distSq(float[] a, float[] b) {
		float ds = 0;
		for (int i = 0; i < a.length; i++) {
			ds += (a[i] - b[i]) * (a[i] - b[i]);
		}
		return ds;
	}
	
	/**
	 * The Fast Fourier Transform (generic version, with NO optimizations).
	 * 
	 * @param inputReal
	 *            an array of length n, the real part
	 * @param inputImag
	 *            an array of length n, the imaginary part
	 * @param DIRECT
	 *            TRUE = direct transform, FALSE = inverse transform
	 * @return a new array of length 2n
	 */
	public static float[] fft(final float[] inputReal, float[] inputImag,
			boolean DIRECT) {
		// - n is the dimension of the problem
		// - nu is its logarithm in base e
		int n = inputReal.length;

		// If n is a power of 2, then ld is an integer (_without_ decimals)
		double ld = Math.log(n) / Math.log(2.0);

		// Here I check if n is a power of 2. If exist decimals in ld, I quit
		// from the function returning null.
		if (((int) ld) - ld != 0) {
			System.out.println("The number of elements is not a power of 2.");
			return null;
		}

		// Declaration and initialization of the variables
		// ld should be an integer, actually, so I don't lose any information in
		// the cast
		int nu = (int) ld;
		int n2 = n / 2;
		int nu1 = nu - 1;
		float[] xReal = new float[n];
		float[] xImag = new float[n];
		float tReal, tImag, p, arg, c, s;

		// Here I check if I'm going to do the direct transform or the inverse
		// transform.
		float constant;
		if (DIRECT) {
			constant = (float) (-2 * Math.PI);
		} else {
			constant = (float) (2 * Math.PI);
		}

		// I don't want to overwrite the input arrays, so here I copy them. This
		// choice adds \Theta(2n) to the complexity.
		for (int i = 0; i < n; i++) {
			xReal[i] = inputReal[i];
			xImag[i] = inputImag[i];
		}

		// First phase - calculation
		int k = 0;
		for (int l = 1; l <= nu; l++) {
			while (k < n) {
				for (int i = 1; i <= n2; i++) {
					p = bitreverseReference(k >> nu1, nu);
					// direct FFT or inverse FFT
					arg = constant * p / n;
					c = (float) Math.cos(arg);
					s = (float) Math.sin(arg);
					tReal = xReal[k + n2] * c + xImag[k + n2] * s;
					tImag = xImag[k + n2] * c - xReal[k + n2] * s;
					xReal[k + n2] = xReal[k] - tReal;
					xImag[k + n2] = xImag[k] - tImag;
					xReal[k] += tReal;
					xImag[k] += tImag;
					k++;
				}
				k += n2;
			}
			k = 0;
			nu1--;
			n2 /= 2;
		}

		// Second phase - recombination
		k = 0;
		int r;
		while (k < n) {
			r = bitreverseReference(k, nu);
			if (r > k) {
				tReal = xReal[k];
				tImag = xImag[k];
				xReal[k] = xReal[r];
				xImag[k] = xImag[r];
				xReal[r] = tReal;
				xImag[r] = tImag;
			}
			k++;
		}

		// Here I have to mix xReal and xImag to have an array (yes, it should
		// be possible to do this stuff in the earlier parts of the code, but
		// it's here to readibility).
		float[] newArray = new float[xReal.length * 2];
		float radice = (float) (1 / Math.sqrt(n));
		for (int i = 0; i < newArray.length; i += 2) {
			int i2 = i / 2;
			// I used Stephen Wolfram's Mathematica as a reference so I'm going
			// to normalize the output while I'm copying the elements.
			newArray[i] = xReal[i2] * radice;
			newArray[i + 1] = xImag[i2] * radice;
		}
		return newArray;
	}

	/**
	 * The reference bitreverse function.
	 */
	private static int bitreverseReference(int j, int nu) {
		int j2;
		int j1 = j;
		int k = 0;
		for (int i = 1; i <= nu; i++) {
			j2 = j1 / 2;
			k = 2 * k + j1 - 2 * j2;
			j1 = j2;
		}
		return k;
	}
	
	public static void main(String[] args) throws IOException {
		ArrayList<Img<Color>> els = new ArrayList<Img<Color>>();
		Random r = new Random(/*2413*/);
		String[] FONTS = { "Times", "Georgia", "Optima", "Times New Roman" };
		for (int i = 0; i < 200; i++) {
			TreePredict.Img<Color> timg = TreePredict.getImg("a", new Config.FontType(FONTS[i % FONTS.length], false), Color.RED, r);
			float[] fftdata = fft(timg.data, new float[timg.data.length], true);
			els.add(new Img<Color>(fftdata, Color.RED));
		}
		for (int i = 0; i < 200; i++) {
			TreePredict.Img<Color> timg = TreePredict.getImg("e", new Config.FontType(FONTS[i % FONTS.length], false), Color.BLUE, r);
			float[] fftdata = fft(timg.data, new float[timg.data.length], true);
			els.add(new Img<Color>(fftdata, Color.BLUE));
		}
		
		SVNode tree = createSVTree(els, els, 5, 2, null);
		int failures = 0;
		int tries = 0;
		for (File f : new File("/Users/zar/Desktop/DesktopContents/ltestdata3/a/").listFiles()) {
			if (!f.getName().endsWith(".png")) { continue; }
			float[] data = TreePredict.getImg(ImageIO.read(f), Color.RED).data;
			float[] fftdata = fft(data, new float[data.length], true);
			Color tag = tree.classify(new Img(fftdata, Color.RED));
			if (!tag.equals(Color.RED)) { failures++; }
			tries++;
		}
		
		for (File f : new File("/Users/zar/Desktop/DesktopContents/ltestdata3/e/").listFiles()) {
			if (!f.getName().endsWith(".png")) { continue; }
			float[] data = TreePredict.getImg(ImageIO.read(f), Color.BLUE).data;
			float[] fftdata = fft(data, new float[data.length], true);
			Color tag = tree.classify(new Img(fftdata, Color.BLUE));
			if (!tag.equals(Color.BLUE)) { failures++; }
			tries++;
		}
		
		System.out.println(failures + "/" + tries);
		System.out.println((tries - failures) * 100.0 / tries);
		
		/*// Find SV Tree
		for (int i = 1; i < 8; i += 2) {
			BufferedImage img = new BufferedImage(IMG_SZ, IMG_SZ, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = img.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, IMG_SZ, IMG_SZ);
			SVNode tree = createSVTree(els, els, i, 2, null);

			// Draw support vector by checking each pixel of the display.
			drawVector(tree, g, 0);

			// Draw classes.
			int score = 0;
			for (Img<Color> e : els) {
				Color c = tree.classify(e);
				if (c == e.tag) { score++; }
				g.setColor(c);
				g.drawRect((int) (IMG_SZ / 2 * (e.data[0] + 1)) - 2, (int) (IMG_SZ / 2 * (e.data[1] + 1)) - 2, 5, 5);
			}

			// Draw points with correct classes.
			for (Img<Color> e : els) {
				g.setColor(e.tag);
				g.fillRect((int) (IMG_SZ / 2 * (e.data[0] + 1)) - 1, (int) (IMG_SZ / 2 * (e.data[1] + 1)) - 1, 4, 4);
			}

			g.setColor(Color.BLACK);
			g.drawString(score + "/" + els.size(), 10, 20);

			ImageIO.write(img, "png", new File("/Users/zar/Desktop/" + i + ".png"));
		}*/
	}
	
	static int drawC(SVNode n, float[] coords, int level) {
		double dist = n.sv.d - dot(n.sv.v, coords);
		//System.out.println(dist);
		if (Math.abs(dist) < 2.28E-11) {
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
			float[] coords = new float[DIMS];
			coords[0] = x * 2.0f / IMG_SZ - 1;
			coords[1] = y * 2.0f / IMG_SZ - 1;
			int intensity = drawC(n, coords, 0);
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
