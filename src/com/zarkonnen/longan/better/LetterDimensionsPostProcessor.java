package com.zarkonnen.longan.better;

import com.zarkonnen.longan.Longan;
import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.nnidentifier.Identifier;
import com.zarkonnen.longan.stage.PostProcessor;
import java.util.HashMap;
import java.util.Map;

public class LetterDimensionsPostProcessor implements PostProcessor {
	static final double MAX_MAJOR_SZ_DEVIATION     = 2.0;
	static final double MAJOR_SZ_DEVIATION_PENALTY = 0.4;
	
	static final double MAX_ASPECT_DEVIATION       = 1.6;
	static final double ASPECT_DEVIATION_PENALTY   = 0.4;
	
	public void process(Result result, Longan longan) {
		for (Column c : result.columns) {
			if (!c.metadata.has(Identifier.IDENTIFIER_USED)) {
				continue;
			}
			HashMap<String, Double> sizes = c.metadata.get(Identifier.IDENTIFIER_USED).expectedRelativeSizes;
			HashMap<String, Double> aspects = c.metadata.get(Identifier.IDENTIFIER_USED).expectedAspectRatios;
			for (Line line : c.lines) {
				for (Word word : line.words) {
					for (Letter l : word.letters) {
						for (Map.Entry<String, Double> e : l.possibleLetters.entrySet()) {
							if (!e.getKey().equals(l.bestLetter())) { continue; }
							if (sizes.containsKey(e.getKey())) {
								double sz = sizes.get(e.getKey());
								if (l.relativeSize > sz * MAX_MAJOR_SZ_DEVIATION ||
									l.relativeSize < sz / MAX_MAJOR_SZ_DEVIATION)
								{
									e.setValue(e.getValue() - MAJOR_SZ_DEVIATION_PENALTY);
								}
							}
							if (aspects.containsKey(e.getKey())) {
								double as = aspects.get(e.getKey());
								double letterAspect = l.width * 1.0 / l.height;
								if (letterAspect > as * MAX_ASPECT_DEVIATION ||
									letterAspect < as / MAX_ASPECT_DEVIATION)
								{
									e.setValue(e.getValue() - ASPECT_DEVIATION_PENALTY);
								}
							}
						}
					}
				}
			}
		}
	}
}
