package com.metalbeetle.longan.better;

import com.metalbeetle.longan.data.Letter;
import java.util.Comparator;

class LetterXComparator implements Comparator<Letter> {
	public int compare(Letter r0, Letter r1) {
		return r0.x - r1.x;
	}
}
