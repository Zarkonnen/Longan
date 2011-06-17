package com.metalbeetle.longan.better;

import com.metalbeetle.longan.LetterRect;
import java.util.Comparator;

class XComparator implements Comparator<LetterRect> {

	public int compare(LetterRect r0, LetterRect r1) {
		return r0.x - r1.x;
	}
	
}
