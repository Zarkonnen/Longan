package com.zarkonnen.longan.better;

import com.zarkonnen.longan.Longan;
import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.stage.PostProcessor;
import java.util.Iterator;

public class SpeckleEliminator implements PostProcessor {

	public void process(Result result, Longan longan) {
		/*if (!result.metadata.containsKey("letterToWordSpacingBoundary")) { return; }
		int letterToWordSpacingBoundary = Integer.parseInt(result.metadata.get("letterToWordSpacingBoundary"));*/
		for (Column c : result.columns) {
			for (Line line : c.lines) {
				for (Word word : line.words) {
					for (int i = 0; i < word.letters.size(); i++) {
						Letter l = word.letters.get(i);
						if (l.fragment && !l.bestLetter().matches("[.',-]")) {
							word.letters.remove(i);
							word.regenBoundingRect();
							i--;
						}
					}
				}
			}
		}
		
		for (Iterator<Column> cit = result.columns.iterator(); cit.hasNext();) {
			Column c = cit.next();
			boolean lRem = false;
			for (Iterator<Line> lit = c.lines.iterator(); lit.hasNext();) {
				Line l = lit.next();
				boolean wRem = false;
				for (Iterator<Word> wit = l.words.iterator(); wit.hasNext();) {
					if (wit.next().letters.isEmpty()) {
						wRem = true;
						wit.remove();
					}
				}
				if (l.words.isEmpty()) {
					lRem = true;
					lit.remove();
				} else {
					if (wRem) { l.regenBoundingRect(); }
				}
			}
			if (c.lines.isEmpty()) {
				cit.remove();
			} else {
				if (lRem) { c.regenBoundingRect(); }
 			}
		}
	}
}
