package com.metalbeetle.longan.better;

import com.metalbeetle.longan.data.Word;
import java.util.Comparator;

class WordXComparator implements Comparator<Word> {
	public int compare(Word w0, Word w1) {
		return w0.boundingRect.x - w1.boundingRect.x;
	}
}
