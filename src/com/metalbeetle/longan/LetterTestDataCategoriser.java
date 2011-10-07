package com.metalbeetle.longan;

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

import com.metalbeetle.fruitbat.atrio.ATRWriter;
import com.metalbeetle.longan.stage.Chunker;
import com.metalbeetle.longan.stage.LetterFinder;
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
	LetterFinder lf;
	Chunker chunker;
	Canvas c;
	String letter = null;
	BufferedImage img;
	BufferedImage letterImg;
	Rectangle letterR;

	static final int REFERENCE_INTENSITY_BOUNDARY = 165;
	
	public LetterTestDataCategoriser(LetterFinder lf, Chunker chunker) {
		this.lf = lf;
		this.chunker = chunker;
	}
	
	public void run(File sourceFile, File targetFolder) {
		HashMap<String, ATRWriter> offsetWriters = new HashMap<String, ATRWriter>();
		HashMap<String, ATRWriter> sizeWriters = new HashMap<String, ATRWriter>();
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
				/*g.drawImage(
						img,
						100,
						100,
						100 + letterR.width,
						100 + letterR.height,
						letterR.x,
						letterR.y,
						letterR.x + letterR.width,
						letterR.y + letterR.height,
						null,
						null
				);*/
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
		ArrayList<LetterRect> rects = lf.find(img, md);
		ArrayList<ArrayList<ArrayList<LetterRect>>> rs = chunker.chunk(rects, img, md);
		rects.clear();
		for (ArrayList<ArrayList<LetterRect>> line : rs) {
			for (ArrayList<LetterRect> word : line) {
				rects.addAll(word);
			}
		}
		
		int blackWhiteBoundary = 0;
		if (md.containsKey("blackWhiteBoundary")) {
			int intensityBoundary = Integer.parseInt(md.get("blackWhiteBoundary"));
			blackWhiteBoundary = (REFERENCE_INTENSITY_BOUNDARY - intensityBoundary) * 3 / 4;
		}
		
		for (LetterRect r : rects) {
			letter = null;
			letterR = r;
			letterImg = Util.cropMaskAndAdjust(img, r, blackWhiteBoundary);
			c.repaint();
			while (letter == null) {
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
					letter.equals("/")
					? "slash"
					: letter.equals(".")
					? "period"
					: letter.equals(":")
					? "colon"
					: letter;
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
				
				if (!offsetWriters.containsKey(charS)) {
					File offF = new File(targetFolder, charS + "-offset.atr");
					offsetWriters.put(charS, new ATRWriter(new FileOutputStream(offF, /*append*/true)));
				}
				ATRWriter w = offsetWriters.get(charS);
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
		}
		
		for (ATRWriter w : offsetWriters.values()) {
			try { w.close(); } catch (Exception e) { e.printStackTrace(); }
		}
		for (ATRWriter w : sizeWriters.values()) {
			try { w.close(); } catch (Exception e) { e.printStackTrace(); }
		}
		
		fr.dispose();
	}

	public void keyTyped(KeyEvent ke) {
		letter = new String(new char[] {ke.getKeyChar()});
	}

	public void keyPressed(KeyEvent ke) {}

	public void keyReleased(KeyEvent ke) {}
}
