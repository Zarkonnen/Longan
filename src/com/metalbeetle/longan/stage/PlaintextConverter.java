package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.Letter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface PlaintextConverter {
	public String convert(ArrayList<ArrayList<ArrayList<Letter>>> lines, BufferedImage img);
}
