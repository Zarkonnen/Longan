package com.metalbeetle.longan.neuralnetwork2;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

public class DemoNet {
	static final int NUM_NETWORKS = 7;
	
	static final double[][][] kernels = {
		// Identity
		{
			{ 0,  0,  0 },
			{ 0,  1,  0 },
			{ 0,  0,  0 }
		},
		// c/o detector (vgap)
		{
			{-1,  4, -1 },
			{-1, -2, -1 },
			{-1,  4, -1 }
		},
		// weird-ass kernel
		{
			{-2,  4,  2 },
			{-4,  0,  4 },
			{ 2, -4, -2 }
		},
	};
	
	static class ConvolvedData {
		public double[][] data;

		public ConvolvedData(double[][] data) {
			this.data = data;
		}
	}
	
	static final String[] LETTERS = {
		"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
		"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
		"!", "@", "£", "$", "%", "&", "(", ")", "'", ".", ",", ":", ";", "/", "?", "+", "-",
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	static class LetterRecord {
		BufferedImage img;
		double size = 1.0;
		double offset = 0.0;

		public LetterRecord(BufferedImage img) {
			this.img = img;
		}
	}
	
	static String letterToFilename(String l) {
		return
				l.equals(".")
				? "period"
				: l.equals(":")
				? "colon"
				: l.equals("/")
				? "slash"
				: l.toLowerCase().equals(l)
				? l
				: l.toLowerCase() + "-uc";
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Random rnd = new Random();
		PrintStream ps = new PrintStream(new File("/Users/zar/Desktop/out.txt"));
		System.setOut(ps);
		System.out.println(System.currentTimeMillis());
		ArrayList<File> bExFolders = new ArrayList<File>();
		HashMap<String, HashMap<String, Double>> offsets = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, ArrayList<Double>> offsetLists = new HashMap<String, ArrayList<Double>>();
		HashMap<String, HashMap<String, Double>> sizes = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, ArrayList<Double>> sizeLists = new HashMap<String, ArrayList<Double>>();
		
		for (String s : LETTERS) {
			HashMap<String, Double> os = new HashMap<String, Double>();
			ArrayList<Double> osL = new ArrayList<Double>();
			ATRReader r = new ATRReader(new BufferedInputStream(new FileInputStream(
					new File(new File(args[1]), letterToFilename(s) + "-offset.atr"))));
			List<String> rec = null;
			while ((rec = r.readRecord()) != null) {
				os.put(rec.get(0), Double.parseDouble(rec.get(1)));
				osL.add(Double.parseDouble(rec.get(1)));
			}
			offsets.put(letterToFilename(s), os);
			offsetLists.put(letterToFilename(s), osL);
			r.close();
			
			HashMap<String, Double> ss = new HashMap<String, Double>();
			ArrayList<Double> sL = new ArrayList<Double>();
			r = new ATRReader(new BufferedInputStream(new FileInputStream(
					new File(new File(args[1]), letterToFilename(s) + "-size.atr"))));
			rec = null;
			while ((rec = r.readRecord()) != null) {
				ss.put(rec.get(0), Double.parseDouble(rec.get(1)));
				sL.add(Double.parseDouble(rec.get(1)));
			}
			sizes.put(letterToFilename(s), ss);
			sizeLists.put(letterToFilename(s), sL);
			r.close();
		}
		System.out.println("Loaded offsets and sizes.");
		System.out.println(System.currentTimeMillis());
		
		/*for (int i = 0; i < args.length; i++) {
			bExFolders.add(new File(args[i]));
		}*/
		for (String s : LETTERS) {
			bExFolders.add(new File(new File(args[1]), letterToFilename(s)));
		}
		
		HashMap<String, ArrayList<LetterRecord>> lToEx =
				new HashMap<String, ArrayList<LetterRecord>>();
		
		for (File fol : bExFolders) {
			if (fol.listFiles() == null) {
				System.out.println(fol + " is not a folder");
				continue;
			}
			if (fol.listFiles().length < 10) {
				System.out.println(fol + " doesn't have enough data");
				continue;
			}
			ArrayList<LetterRecord> exs = new ArrayList<LetterRecord>();
			lToEx.put(fol.getName(), exs);
			for (File f : fol.listFiles()) {
				if (exs.size() >= 200) { break; }
				if (f.getName().endsWith(".png")) {
					try {
						LetterRecord lr = new LetterRecord(ImageIO.read(f));
						String num = f.getName().substring(0, f.getName().length() - 4);
						if (offsets.get(fol.getName()).containsKey(num)) {
							lr.offset = offsets.get(fol.getName()).get(num);
						} else {
							lr.offset = offsetLists.get(fol.getName()).get(
									rnd.nextInt(offsetLists.get(fol.getName()).size()));
						}
						if (sizes.get(fol.getName()).containsKey(num)) {
							lr.size = sizes.get(fol.getName()).get(num);
						} else {
							lr.size = sizeLists.get(fol.getName()).get(
									rnd.nextInt(sizeLists.get(fol.getName()).size()));
						}
						exs.add(lr);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			Collections.shuffle(exs);
		}
		
		System.out.println("Loaded images");
		System.out.println(System.currentTimeMillis());
		
		HashMap<String, ConvolvedData> lToCData = new HashMap<String, ConvolvedData>();
		for (Map.Entry<String, ArrayList<LetterRecord>> w : lToEx.entrySet()) {
			ArrayList<LetterRecord> exs = w.getValue();
			double[][] data = new double[exs.size()][0];
			for (int i = 0; i < data.length; i++) {
				data[i] = getInputForNN(exs.get(i));
			}
			lToCData.put(w.getKey(), new ConvolvedData(data));
			w.setValue(new ArrayList<LetterRecord>(0)); // Free up memory
		}
		
		lToEx = null; // For GC
		
		System.out.println("Convolved data");
		System.out.println(System.currentTimeMillis());
				
		HashMap<String, ArrayList<WeightSharingNanoNetwork>> networks = new HashMap<String, ArrayList<WeightSharingNanoNetwork>>();
		
		int total = 0;
		
		if (args[0].equals("train") || args[0].equals("trainAndTest")) {
			for (String lName : lToCData.keySet()) {
				int positiveTrainingSize = args[0].equals("train")
						? lToCData.get(lName).data.length / 2
						: lToCData.get(lName).data.length / 2;
				int negativeTrainingSize = -positiveTrainingSize;
				for (ConvolvedData cd : lToCData.values()) {
					negativeTrainingSize += args[0].equals("train")
							? cd.data.length / 2
							: cd.data.length / 2;
				}
				double[][] trainingPos = new double[positiveTrainingSize][0];
				System.arraycopy(lToCData.get(lName).data, 0, trainingPos, 0, trainingPos.length);
				double[][] trainingNeg = new double[negativeTrainingSize][0];
				int offset = 0;
				for (String lName2 : lToCData.keySet()) {
					if (lName2.equals(lName)) { continue; }
					System.arraycopy(lToCData.get(lName2).data, 0, trainingNeg, offset,
							args[0].equals("train")
							? lToCData.get(lName2).data.length / 2
							: lToCData.get(lName2).data.length / 2);
					offset += args[0].equals("train")
							? lToCData.get(lName2).data.length / 2
							: lToCData.get(lName2).data.length / 2;
				}

				ArrayList<WeightSharingNanoNetwork> mns = new ArrayList<WeightSharingNanoNetwork>();
				for (int n = 0; n < NUM_NETWORKS; n++) {
					WeightSharingNanoNetwork mn = new WeightSharingNanoNetwork(n);
					System.out.println("Created MN for " + lName);
					// Shuffle?
					for (int i = 0; i < 3; i++) {
						mn.train(trainingPos, trainingNeg, 0.001, 0.0002);
						System.out.println("pass " + (i + 1) + " complete");
					}

					System.out.println("Trained MN for " + lName);
					System.out.println(mn.nw.numNodes() + " nodes, " + mn.nw.numWeights() + " weights");
					System.out.println(++total + " of " + LETTERS.length * NUM_NETWORKS);
					mns.add(mn);
				}
				networks.put(lName, mns);
			}
		} else {
			for (String lName : lToCData.keySet()) {
				ArrayList<WeightSharingNanoNetwork> mns = new ArrayList<WeightSharingNanoNetwork>();
				for (int n = 0; n < NUM_NETWORKS; n++) {
					FileInputStream fis = new FileInputStream(new File(new File(args[2]), lName + "-" + n));
					WeightSharingNanoNetwork mn = new WeightSharingNanoNetwork(n);
					NetworkIO.input(mn.nw, fis);
					fis.close();
					mn.nw.freeze();
					System.out.println("Loaded MN for " + lName);
					mns.add(mn);
				}
				networks.put(lName, mns);
			}
		}
		
		if (args[0].equals("train")) {
			for (String lName : networks.keySet()) {
				for (int n = 0; n < NUM_NETWORKS; n++) {
					FileOutputStream fos = new FileOutputStream(new File(new File(args[2]), lName + "-" + n));
					NetworkIO.output(networks.get(lName).get(n).nw, fos);
					fos.close();
				}
			}
			return;
		}
		System.out.println(System.currentTimeMillis());
		
		System.out.println("Testing");
		int hits = 0;
		int misses = 0;
		for (String lName : lToCData.keySet()) {
			ConvolvedData cd = lToCData.get(lName);
			//System.out.println(lName + cd.data.length);
			for (int i = cd.data.length / 2 + 1; i < cd.data.length; i++) {
				//System.out.println(i);
				String bestScoringLetter = null;
				double bestScore = -100;
				double scoreForCorrectLetter = 0;
				for (Map.Entry<String, ArrayList<WeightSharingNanoNetwork>> e : networks.entrySet()) {
					double[] results = new double[NUM_NETWORKS];
					for (int n = 0; n < NUM_NETWORKS; n++) {
						results[n] = e.getValue().get(n).run(cd.data[i]);
					}
					Arrays.sort(results);
					double score = results[NUM_NETWORKS / 2];
					if (bestScoringLetter == null || score > bestScore) {
						bestScoringLetter = e.getKey();
						bestScore = score;
					}
					if (e.getKey().equals(lName)) {
						scoreForCorrectLetter = score;
					}
				}
				
				if (bestScoringLetter.equals(lName) ||
					(bestScoringLetter + "-uc").equals(lName) ||
					bestScoringLetter.equals(lName + "-uc"))
				{
					hits++;
				} else {
					System.out.println("Mis-identified " + lName + " " + i + " as " +
							bestScoringLetter + " with a score of " + bestScore + " vs " +
							scoreForCorrectLetter + ".");
					misses++;
				}
			}
		}
		System.out.println("Hits: " + hits);
		System.out.println("Misses: " + misses);
		System.out.println(System.currentTimeMillis());
		ps.close();
	}
	
	static double[] getInputForNN(LetterRecord lr) {
		BufferedImage src = lr.img;
		BufferedImage scaledSrc = new BufferedImage(14, 14, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaledSrc.getGraphics();
		g.setColor(Color.WHITE);
		g.drawImage(src, 2, 2, 12, 12, 0, 0, src.getWidth(), src.getHeight(), null);
		src = scaledSrc;
		double[] result = new double[kernels.length * 12 * 12 + 3];
		for (int y = 0; y < 12; y++) { for (int x = 0; x < 12; x++) {
			for (int kdy = 0; kdy < 3; kdy++) { for (int kdx = 0; kdx < 3; kdx++) {
				Color c = new Color(src.getRGB(x + kdx, y + kdy));
				double intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 255.0 / 3.0;
				for (int k = 0; k < kernels.length; k++) {
					result[k * 144 + y * 12 + x] += intensity * kernels[k][kdy][kdx];
				}
			} }
		} }
		result[result.length - 3] = Math.log(src.getWidth() / ((double) src.getHeight())) * 2;
		result[result.length - 2] = Math.log(lr.size) * 2;
		result[result.length - 1] = lr.offset * 5;
		return result;
	}
}
