package com.metalbeetle.longan.neuralnetwork;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class MicroNet {
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
		ArrayList<File> bExFolders = new ArrayList<File>();
		
		/*for (int i = 0; i < args.length; i++) {
			bExFolders.add(new File(args[i]));
		}*/
		for (String s : LETTERS) {
			bExFolders.add(new File(new File(args[1]), letterToFilename(s)));
		}
		
		HashMap<String, ArrayList<BufferedImage>> lToEx = new HashMap<String, ArrayList<BufferedImage>>();
		
		for (File fol : bExFolders) {
			if (fol.listFiles() == null) {
				System.out.println(fol + " is not a folder");
				continue;
			}
			if (fol.listFiles().length < 10) {
				System.out.println(fol + " doesn't have enough data");
				continue;
			}
			ArrayList<BufferedImage> exs = new ArrayList<BufferedImage>();
			lToEx.put(fol.getName(), exs);
			for (File f : fol.listFiles()) {
				if (f.getName().endsWith(".png")) {
					try {
						exs.add(ImageIO.read(f));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (exs.size() == 100) { break; } // qqDPS
			}
			Collections.shuffle(exs);
		}
		
		System.out.println("Loaded images");
		
		HashMap<String, ConvolvedData> lToCData = new HashMap<String, ConvolvedData>();
		for (Map.Entry<String, ArrayList<BufferedImage>> w : lToEx.entrySet()) {
			ArrayList<BufferedImage> exs = w.getValue();
			double[][] data = new double[exs.size()][0];
			for (int i = 0; i < data.length; i++) {
				data[i] = convolve2(exs.get(i));
			}
			lToCData.put(w.getKey(), new ConvolvedData(data));
		}
		
		System.out.println("Convolved data");
		
		HashMap<String, MicroNetwork> networks = new HashMap<String, MicroNetwork>();
		
		if (args[0].equals("train") || args[0].equals("trainAndTest")) {
			String[][] problems = {
				{ "o", "c" },
				{ "c", "o" },
				{ "h", "b" },
				{ "b", "h" }
			};
			
			for (String lName : lToCData.keySet()) {
				int positiveTrainingSize = lToCData.get(lName).data.length / 2;
				int negativeTrainingSize = -positiveTrainingSize;
				for (ConvolvedData cd : lToCData.values()) {
					negativeTrainingSize += cd.data.length / 2;
				}
				double[][] trainingPos = new double[positiveTrainingSize][0];
				System.arraycopy(lToCData.get(lName).data, 0, trainingPos, 0, trainingPos.length);
				double[][] trainingNeg = new double[negativeTrainingSize][0];
				int offset = 0;
				for (String lName2 : lToCData.keySet()) {
					if (lName2.equals(lName)) { continue; }
					System.arraycopy(lToCData.get(lName2).data, 0, trainingNeg, offset,
							lToCData.get(lName2).data.length / 2);
					offset += lToCData.get(lName2).data.length / 2;
				}

				MicroNetwork mn = new MicroNetwork();
				System.out.println("Created MN for " + lName);
				for (int i = 0; i < 3; i++) {
					mn.train(trainingPos, trainingNeg, 0.001, 0.0002);
					System.out.println("pass " + (i + 1) + " complete");
				}

				/*
				for (String[] p : problems) {
					if (lName.equals(p[0])) {
						trainingNeg = new double[lToCData.get(p[1]).data.length / 2][0];
						System.arraycopy(lToCData.get(p[1]).data, 0, trainingNeg, 0, trainingNeg.length);
						for (int i = 0; i < 10; i++) {
							mn.train(trainingPos, trainingNeg, 0.001, 0.0002);
							System.out.println("special pass " + (i + 1) + " complete");
						}
					}
				}*/

				System.out.println("Trained MN for " + lName);
				networks.put(lName, mn);
			}
		} else {
			for (String lName : lToCData.keySet()) {
				FileInputStream fis = new FileInputStream(new File(new File(args[2]), lName));
				MicroNetwork mn = new MicroNetwork();
				NetworkIO.input(mn.nw, fis);
				fis.close();
				System.out.println("Loaded MN for " + lName);
				networks.put(lName, mn);
			}
		}
		
		if (args[0].equals("train")) {
			for (String lName : networks.keySet()) {
				FileOutputStream fos = new FileOutputStream(new File(new File(args[2]), lName));
				NetworkIO.output(networks.get(lName).nw, fos);
				fos.close();
			}
			return;
		}
		
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
				for (Map.Entry<String, MicroNetwork> e : networks.entrySet()) {
					double score = e.getValue().run(cd.data[i]);
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
					/*System.out.println("Mis-identified " + lName + " " + i + " as " +
							bestScoringLetter + " with a score of " + bestScore + " vs " +
							scoreForCorrectLetter + ".");*/
					misses++;
				}
			}
		}
		System.out.println("Hits: " + hits);
		System.out.println("Misses: " + misses);
		/*
		double[][] trainingAs = new double[as.length / 2][0];
		for (int i = 0; i < trainingAs.length; i++) {
			trainingAs[i] = as[i];
		}
		
		double[][] trainingBs = new double[bs.length / 2][0];
		for (int i = 0; i < trainingBs.length; i++) {
			trainingBs[i] = bs[i];
		}
		
		double[][] testAs = new double[as.length / 2][0];
		for (int i = 0; i < testAs.length; i++) {
			testAs[i] = as[as.length / 2 + i];
		}
		
		double[][] testBs = new double[bs.length / 2][0];
		for (int i = 0; i < testBs.length; i++) {
			testBs[i] = bs[bs.length / 2 + i];
		}		
		
		double totalPosError = 0.0;
		double totalNegError = 0.0;
		
		for (int j = 0; j < 1; j++) {
			MicroNetwork mn = new MicroNetwork();
			System.out.println("Created MN");
			for (int i = 0; i < 4; i++) {
				mn.train(trainingAs, trainingBs, 0.001, 0.0002);
				System.out.println("pass " + (i + 1) + " complete");
			}

			System.out.println("Trained MN");

			double err = 0.0;
			for (double[] a : testAs) {
				double result = mn.run(a);
				//System.out.println(result);
				err += Math.abs(1.0 - result);
			}
			System.out.println("Positives' average error: " + (err / testAs.length));
			totalPosError += err /  testAs.length;

			err = 0.0;
			for (double[] b : testBs) {
				double result = mn.run(b);
				//System.out.println(result);
				err += Math.abs(result);
			}
			System.out.println("Negatives' average error: " + (err / testBs.length));
			totalNegError += err / testBs.length;
		}
		
		System.out.println("Positives' average error: " + (totalPosError / 1));
		System.out.println("Negatives' average error: " + (totalNegError / 1));
		 * 
		 */
	}
	
	static double[] convolve2(BufferedImage src) {
		BufferedImage scaledSrc = new BufferedImage(14, 14, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaledSrc.getGraphics();
		g.setColor(Color.WHITE);
		g.drawImage(src, 2, 2, 12, 12, 0, 0, src.getWidth(), src.getHeight(), null);
		src = scaledSrc;
		double[] result = new double[kernels.length * 12 * 12];
		for (int y = 0; y < 12; y++) { for (int x = 0; x < 12; x++) {
			for (int kdy = 0; kdy < 3; kdy++) { for (int kdx = 0; kdx < 3; kdx++) {
				Color c = new Color(src.getRGB(x + kdx, y + kdy));
				double intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 255.0 / 3.0;
				for (int k = 0; k < kernels.length; k++) {
					result[k * 144 + y * 12 + x] += intensity * kernels[k][kdy][kdx];
				}
			} }
		} }
		return result;
	}
}
