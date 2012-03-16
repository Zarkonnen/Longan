package com.zarkonnen.longan;

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

import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.fruitbat.atrio.ATRWriter;
import com.zarkonnen.longan.better.BetterChunker2;
import com.zarkonnen.longan.better.BetterLetterFinder;
import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.stage.Chunker;
import com.zarkonnen.longan.stage.LetterFinder;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class LetterTestDataCategoriser implements KeyListener {
	Longan longan;
	Canvas c;
	String letterIdentification = null;
	BufferedImage img;
	BufferedImage letterImg;
	Rectangle letterR;
	Letter prevLetter = null;
	String prevLetterIdentification = null;

	static final int REFERENCE_INTENSITY_BOUNDARY = 165;
	
	public LetterTestDataCategoriser(Longan l) {
		this.longan = l;
	}
	
	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("Usage: sourceFile, targetFolder, fontName, sourceName");
			return;
		}
		
		new LetterTestDataCategoriser(Longan.getDefaultImplementation()).run(new File(args[0]), new File(args[1]), args[2], args[3]);
	}
	
	public void run(File sourceFile, File targetFolder, String fontName, String sourceName) {
		HashMap<String, ATRWriter> offsetWriters = new HashMap<String, ATRWriter>();
		HashMap<String, ATRWriter> sizeWriters = new HashMap<String, ATRWriter>();
		HashMap<String, ATRWriter> fontWriters = new HashMap<String, ATRWriter>();
		HashMap<String, ATRWriter> sourceWriters = new HashMap<String, ATRWriter>();
		HashMap<String, ATRWriter> prevLetterWriters = new HashMap<String, ATRWriter>();
		JFrame fr = new JFrame("Character identification");
		fr.add(c = new Canvas() {
			@Override
			public void paint(Graphics g) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, c.getWidth(), c.getHeight());
				if (img == null || letterR == null) { return; }
				g.drawImage(img, 0, 0, 200, 200,
						letterR.x - 100,
						letterR.y - 100,
						letterR.x + 100,
						letterR.y + 100,
						null,
						null);
				g.setColor(new Color(0, 0, 0, 63));
				g.fillRect(0, 0, c.getWidth(), c.getHeight());
				g.drawImage(
						letterImg,
						100,
						100,
						null
				);
				if (prevLetter != null) {
					g.setColor(Color.RED);
					g.drawRect(prevLetter.x - letterR.x + 100, prevLetter.y - letterR.y + 100, prevLetter.width, prevLetter.height);
					g.drawString(prevLetterIdentification, prevLetter.x - letterR.x + 100, prevLetter.y - letterR.y + 100);
				}
			}
		});
		fr.addKeyListener(this);
		c.addKeyListener(this);
		fr.setSize(200, 200);
		fr.setVisible(true);
		try {
			img = ImageIO.read(sourceFile);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(fr, "Can't read file " + sourceFile + ":\n" + e.getMessage());
			fr.dispose();
			return;
		}
		HashMap<String, String> md = new HashMap<String, String>();
		Result result = longan.process(img);
		img = result.img;
		
		File srcF = new File(targetFolder, "sources");
		if (!srcF.exists()) { srcF.mkdirs(); }
		int sourceFileID = 0;
		while (new File(srcF, sourceFileID + ".jpg").exists()) {
			sourceFileID++;
		}
		try {
			ImageIO.write(result.img, "jpg", new File(srcF, sourceFileID + ".jpg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int blackWhiteBoundary = 0;
		if (md.containsKey("blackWhiteBoundary")) {
			int intensityBoundary = Integer.parseInt(md.get("blackWhiteBoundary"));
			blackWhiteBoundary = (REFERENCE_INTENSITY_BOUNDARY - intensityBoundary) * 3 / 4;
		}
		
		for (Column column : result.columns) {
			for (Line line : column.lines) {
				for (Word word : line.words) {
					prevLetter = null;
					prevLetterIdentification = null;
					for (Letter r : word.letters) {
						letterIdentification = null;
						letterR = r;
						letterImg = Util.cropMaskAndAdjust(img, r, blackWhiteBoundary);
						c.repaint();
						while (letterIdentification == null) {
							synchronized (this) {
								try { this.wait(1000); } catch (InterruptedException e) {
									JOptionPane.showMessageDialog(fr, e.getMessage());
									fr.dispose();
									return;
								}
							}
						}

						if (!targetFolder.exists()) {
							targetFolder.mkdirs();
						}
						String charS =
								letterIdentification.equals("/")
								? "slash"
								: letterIdentification.equals(".")
								? "period"
								: letterIdentification.equals(":")
								? "colon"
								: letterIdentification;
						if (!charS.toLowerCase().equals(charS)) {
							charS = charS.toLowerCase() + "-uc";
						}
						File charF = new File(targetFolder, charS);
						if (!charF.exists()) {
							charF.mkdirs();
						}

						int n = 0;
						while (new File(charF, n + ".png").exists()) {
							n++;
						}

						try {
							ImageIO.write(letterImg, "png", new File(charF, n + ".png"));

							if (prevLetter != null) {
								if (!prevLetterWriters.containsKey(charS)) {
									File prevLetterF = new File(targetFolder, charS + "-prevletter.atr");
									prevLetterWriters.put(charS, new ATRWriter(new FileOutputStream(prevLetterF, /*append*/true)));
								}
								ATRWriter w = prevLetterWriters.get(charS);
								w.startRecord();
								w.write("" + n);
								w.write(prevLetterIdentification);
								w.write("" + (r.x - prevLetter.x - prevLetter.width) * 1.0 / r.width);
								w.endRecord();
								w.flush();
							}

							if (!fontWriters.containsKey(charS)) {
								File fontF = new File(targetFolder, charS + "-font.atr");
								fontWriters.put(charS, new ATRWriter(new FileOutputStream(fontF, /*append*/true)));
							}
							ATRWriter w = fontWriters.get(charS);
							w.startRecord();
							w.write("" + n);
							w.write(fontName);
							w.endRecord();
							w.flush();

							if (!sourceWriters.containsKey(charS)) {
								File sourceF = new File(targetFolder, charS + "-source.atr");
								sourceWriters.put(charS, new ATRWriter(new FileOutputStream(sourceF, /*append*/true)));
							}
							w = sourceWriters.get(charS);
							w.startRecord();
							w.write("" + n);
							w.write(sourceName);
							w.write(sourceFileID + ".jpg");
							w.write("" + r.x);
							w.write("" + r.y);
							w.write("" + r.width);
							w.write("" + r.height);
							w.endRecord();
							w.flush();

							if (!offsetWriters.containsKey(charS)) {
								File offF = new File(targetFolder, charS + "-offset.atr");
								offsetWriters.put(charS, new ATRWriter(new FileOutputStream(offF, /*append*/true)));
							}
							w = offsetWriters.get(charS);
							w.startRecord();
							w.write("" + n);
							w.write("" + r.relativeLineOffset);
							w.endRecord();
							w.flush();

							if (!sizeWriters.containsKey(charS)) {
								File sizeF = new File(targetFolder, charS + "-size.atr");
								sizeWriters.put(charS, new ATRWriter(new FileOutputStream(sizeF, /*append*/true)));
							}
							w = sizeWriters.get(charS);
							w.startRecord();
							w.write("" + n);
							w.write("" + r.relativeSize);
							w.endRecord();
							w.flush();
						} catch (Exception e) {
							JOptionPane.showMessageDialog(fr, "Can't read file " + sourceFile + ":\n" + e.getMessage());
							fr.dispose();
							return;
						}
						prevLetter = r;
						prevLetterIdentification = letterIdentification;
					}
				}
			}
		}
		
		for (ATRWriter w : offsetWriters.values()) {
			try { w.close(); } catch (Exception e) { e.printStackTrace(); }
		}
		for (ATRWriter w : sizeWriters.values()) {
			try { w.close(); } catch (Exception e) { e.printStackTrace(); }
		}
		for (ATRWriter w : fontWriters.values()) {
			try { w.close(); } catch (Exception e) { e.printStackTrace(); }
		}
		for (ATRWriter w : prevLetterWriters.values()) {
			try { w.close(); } catch (Exception e) { e.printStackTrace(); }
		}
		for (ATRWriter w : sourceWriters.values()) {
			try { w.close(); } catch (Exception e) { e.printStackTrace(); }
		}
		
		fr.dispose();
	}

	public void keyTyped(KeyEvent ke) {
		letterIdentification = new String(new char[] {ke.getKeyChar()});
	}

	public void keyPressed(KeyEvent ke) {}

	public void keyReleased(KeyEvent ke) {
		if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
			prevLetter = null;
			prevLetterIdentification = null;
			c.repaint();
			return;
		}
	}
}
