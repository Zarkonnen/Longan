package com.metalbeetle.longan;

import java.awt.Rectangle;
import java.util.HashMap;

public class Letter {
	public final Rectangle location;
	public final HashMap<String, Double> possibleLetters;

	public Letter(Rectangle location, HashMap<String, Double> possibleLetters) {
		this.location = location;
		this.possibleLetters = possibleLetters;
	}
}
