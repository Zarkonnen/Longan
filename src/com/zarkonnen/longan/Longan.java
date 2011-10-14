package com.zarkonnen.longan;

/*
 * Copyright 2011 David Stark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.better.AggressiveLetterSplittingPostProcessor;
import com.zarkonnen.longan.better.BetterChunker2;
import com.zarkonnen.longan.better.BetterLetterFinder;
import com.zarkonnen.longan.better.HeuristicPostProcessor;
import com.zarkonnen.longan.better.IntensityHistogramPreProcessor;
import com.zarkonnen.longan.better.LetterSplittingPostProcessor;
import com.zarkonnen.longan.better.RotationFixingPreProcessor;
import com.zarkonnen.longan.better.SpeckleEliminator;
import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.neuralnetwork.NNLI3PostProcessor;
import com.zarkonnen.longan.neuralnetwork.NNLetterIdentifier3;
import com.zarkonnen.longan.simple.SimpleWordPlaintextConverter;
import com.zarkonnen.longan.stage.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Longan {
	public static final String VERSION = "0.9";
	
	public final List<PreProcessor> preProcessors;
	public final LetterFinder letterFinder;
	public final LetterIdentifier letterIdentifier;
	public final Chunker chunker;
	public final List<PostProcessor> postProcessors;

	public static Longan getDefaultImplementation() {
		ArrayList<PreProcessor> preps = new ArrayList<PreProcessor>();
		preps.add(new IntensityHistogramPreProcessor());
		preps.add(new RotationFixingPreProcessor());
		ArrayList<PostProcessor> pps = new ArrayList<PostProcessor>();
		pps.add(new LetterSplittingPostProcessor());
		pps.add(new AggressiveLetterSplittingPostProcessor());
		pps.add(new NNLI3PostProcessor());
		pps.add(new HeuristicPostProcessor());
		pps.add(new SpeckleEliminator());
		return new Longan(
			preps,
			new BetterLetterFinder(),
			new BetterChunker2(),
			new NNLetterIdentifier3(),
			pps
		);
	}
	
	public static String recognize(BufferedImage img) {
		return new SimpleWordPlaintextConverter().convert(getDefaultImplementation().process(img));
	}

	public Longan(
			List<PreProcessor> preProcessors,
			LetterFinder letterFinder,
			Chunker chunker,
			LetterIdentifier letterIdentifier,
			List<PostProcessor> postProcessors)
	{
		this.preProcessors = preProcessors;
		this.letterFinder = letterFinder;
		this.letterIdentifier = letterIdentifier;
		this.chunker = chunker;
		this.postProcessors = postProcessors;
	}

	public Result process(BufferedImage img) {
		HashMap<String, String> metadata = new HashMap<String, String>();
		for (PreProcessor pp : preProcessors) {
			img = pp.process(img, metadata);
		}
		ArrayList<Letter> Letters = letterFinder.find(img, metadata);
		Result result = chunker.chunk(Letters, img, metadata);
		
		for (Column c : result.columns) {
			for (Line l : c.lines) {
				for (Word w : l.words) {
					for (Letter letter : w.letters) {
						letterIdentifier.identify(letter, result);
					}
				}
			}
		}
		
		for (PostProcessor pp : postProcessors) {
			pp.process(result, this);
		}
		return result;
	}
}
