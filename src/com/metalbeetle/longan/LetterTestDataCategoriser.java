package com.metalbeetle.longan;

import com.metalbeetle.longan.stage.LetterFinder;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class LetterTestDataCategoriser implements KeyListener {
	LetterFinder lf;
	Canvas c;
	String letter = null;
	BufferedImage img;
	Rectangle letterR;

	public LetterTestDataCategoriser(LetterFinder lf) {
		this.lf = lf;
	}
	
	public void run(File sourceFile, File targetFolder) {
		JFrame fr = new JFrame("Character identification");
		fr.add(c = new Canvas() {
			@Override
			public void paint(Graphics g) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, c.getWidth(), c.getHeight());
				/*if (letterImg != null) {
					g.drawImage(letterImg, c.getWidth() / 2 - letterImg.getWidth() / 2, c.getHeight() / 2 - letterImg.getHeight() / 2, c);
				}*/
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
				);
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
		ArrayList<Rectangle> rects = lf.find(img);
		
		for (Rectangle r : rects) {
			letter = null;
			letterR = r;
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
			String charS = letter.equals("/") ? "slash" : letter;
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
				BufferedImage letterImg = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
				letterImg.getGraphics().drawImage(img, 0, 0, r.width, r.height, r.x, r.y,
					r.x + r.width, r.y + r.height, null, null);
				ImageIO.write(letterImg, "png", new File(charF, n + ".png"));
			} catch (Exception e) {
				JOptionPane.showMessageDialog(fr, "Can't read file " + sourceFile + ":\n" + e.getMessage());
				fr.dispose();
				return;
			}
		}
		
		fr.dispose();
	}

	public void keyTyped(KeyEvent ke) {
		letter = new String(new char[] {ke.getKeyChar()});
	}

	public void keyPressed(KeyEvent ke) {}

	public void keyReleased(KeyEvent ke) {}
}
