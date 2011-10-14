package com.zarkonnen.longan.data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class Result {
	public HashMap<String, String> metadata;
	public BufferedImage img;
	public ArrayList<Column> columns = new ArrayList<Column>();
	public ArrayList<Picture> pictures = new ArrayList<Picture>();
}
