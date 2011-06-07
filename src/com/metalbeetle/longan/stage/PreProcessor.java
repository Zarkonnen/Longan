package com.metalbeetle.longan.stage;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public interface PreProcessor {
	public BufferedImage process(BufferedImage img, HashMap<String, String> metadata);
}
