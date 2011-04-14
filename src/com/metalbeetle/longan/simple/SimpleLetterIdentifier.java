package com.metalbeetle.longan.simple;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.LetterIdentifier;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class SimpleLetterIdentifier implements LetterIdentifier {
	static final HashMap<String, BufferedImage> EXAMPLES;
	static final int IMG_SIZE = 20;
	static {
		HashMap<String, BufferedImage> exs = new HashMap<String, BufferedImage>();
		String[] letters = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
				"o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E",
				"F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
				"W", "X", "Y", "Z"
		};
		for (String l : letters) {
			BufferedImage img = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_BYTE_GRAY);
			Graphics g = img.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, IMG_SIZE, IMG_SIZE);
			g.setColor(Color.BLACK);
			g.setFont(g.getFont().deriveFont(IMG_SIZE / 2f));
			g.drawString(l, IMG_SIZE / 4, IMG_SIZE / 2);

			int minY = 0;
			outer: while (minY < IMG_SIZE) {
				for (int x = 0; x < IMG_SIZE; x++) {
					if (!new Color(img.getRGB(x, minY)).equals(Color.WHITE)) {
						break outer;
					}
				}
				minY++;
			}

			int maxY = IMG_SIZE - 1;
			outer: while (maxY >= 0) {
				for (int x = 0; x < IMG_SIZE; x++) {
					if (!new Color(img.getRGB(x, maxY)).equals(Color.WHITE)) {
						break outer;
					}
				}
				maxY--;
			}

			int minX = 0;
			outer: while (minX < IMG_SIZE) {
				for (int y = 0; y < IMG_SIZE; y++) {
					if (!new Color(img.getRGB(minX, y)).equals(Color.WHITE)) {
						break outer;
					}
				}
				minX++;
			}

			int maxX = IMG_SIZE - 1;
			outer: while (maxX >= 0) {
				for (int y = 0; y < IMG_SIZE; y++) {
					if (!new Color(img.getRGB(maxX, y)).equals(Color.WHITE)) {
						break outer;
					}
				}
				maxX--;
			}

			BufferedImage img2 = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_BYTE_GRAY);
			img2.getGraphics().drawImage(img,
					0, 0, IMG_SIZE, IMG_SIZE, minX, minY, maxX + 1, maxY + 1, null);

			/*try {
				ImageIO.write(img2, "jpg", new File("/Users/zar/Desktop/exs/" + l + ".jpg"));
			} catch (Exception e) {
				e.printStackTrace();
			}*/

			exs.put(l, img2);
		}
		EXAMPLES = exs;
	}

	public ArrayList<Letter> identify(ArrayList<Rectangle> possibleLetters, BufferedImage img) {
		ArrayList<Letter> ls = new ArrayList<Letter>();
		for (Rectangle r : possibleLetters) {
			ls.add(new Letter(r, identifyLetter(r, img)));
		}
		return ls;
	}

	static int lid = 0;

	HashMap<String, Double> identifyLetter(Rectangle r, BufferedImage img) {
		HashMap<String, Double> hm = new HashMap<String, Double>();
		BufferedImage letterImg = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_INT_RGB);
		letterImg.getGraphics().drawImage(
				img,
				0, 0, IMG_SIZE, IMG_SIZE,
				r.x, r.y, r.x + r.width, r.y + r.height,
				null
		);

		/*try {
			ImageIO.write(letterImg, "jpg", new File("/Users/zar/Desktop/letters/" + lid++ + ".jpg"));
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		for (Map.Entry<String, BufferedImage> ex : EXAMPLES.entrySet()) {
			int totalDiff = 1;
			for (int y = 0; y < IMG_SIZE; y++) { for (int x = 0; x < IMG_SIZE; x++) {
				Color c1 = new Color(letterImg.getRGB(x, y));
				/*if (c1.getRed() + c1.getGreen() + c1.getBlue() < 133 * 3) {
					c1 = Color.BLACK;
				} else {
					c1 = Color.WHITE;
				}*/
				Color c2 = new Color(ex.getValue().getRGB(x, y));
				int diff = Math.abs(c1.getRed() - c2.getRed()) +
						Math.abs(c1.getGreen() - c2.getGreen()) +
						Math.abs(c1.getBlue() - c2.getBlue());
				totalDiff += diff;
			}}
			double maxDiff = 3 * 255 * IMG_SIZE * IMG_SIZE;
			double match = (maxDiff - totalDiff) / maxDiff;
			hm.put(ex.getKey(), match);
		}

		return hm;
	}
}
