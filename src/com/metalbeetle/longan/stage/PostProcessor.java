package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.Letter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public interface PostProcessor {
	public void process(ArrayList<ArrayList<ArrayList<Letter>>> lines, BufferedImage img, HashMap<String, String> metadata);
}
