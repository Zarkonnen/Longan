package com.metalbeetle.longan;

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

import com.metalbeetle.longan.data.Letter;
import com.metalbeetle.longan.better.AggressiveLetterSplittingPostProcessor;
import com.metalbeetle.longan.better.BetterChunker2;
import com.metalbeetle.longan.better.BetterLetterFinder;
import com.metalbeetle.longan.better.HeuristicPostProcessor;
import com.metalbeetle.longan.better.IntensityHistogramPreProcessor;
import com.metalbeetle.longan.better.LetterSplittingPostProcessor;
import com.metalbeetle.longan.better.RotationFixingPreProcessor;
import com.metalbeetle.longan.data.Column;
import com.metalbeetle.longan.data.Line;
import com.metalbeetle.longan.data.Result;
import com.metalbeetle.longan.data.Word;
import com.metalbeetle.longan.neuralnetwork.NNLI3PostProcessor;
import com.metalbeetle.longan.neuralnetwork.NNLetterIdentifier3;
import com.metalbeetle.longan.simple.SimpleWordPlaintextConverter;
import com.metalbeetle.longan.stage.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Longan {
	public final List<PreProcessor> preProcessors;
	public final LetterFinder letterFinder;
	public final LetterIdentifier letterIdentifier;
	public final Chunker chunker;
	public final List<PostProcessor> postProcessors;
	public final PlaintextConverter plaintextConverter;

	public static Longan getDefaultImplementation() {
		ArrayList<PreProcessor> preps = new ArrayList<PreProcessor>();
		preps.add(new IntensityHistogramPreProcessor());
		preps.add(new RotationFixingPreProcessor());
		ArrayList<PostProcessor> pps = new ArrayList<PostProcessor>();
		pps.add(new LetterSplittingPostProcessor());
		pps.add(new AggressiveLetterSplittingPostProcessor());
		pps.add(new NNLI3PostProcessor());
		pps.add(new HeuristicPostProcessor());
		return new Longan(
			preps,
			new BetterLetterFinder(),
			new BetterChunker2(),
			new NNLetterIdentifier3(),
			pps,
			new SimpleWordPlaintextConverter()
		);
	}

	public Longan(List<PreProcessor> preProcessors, LetterFinder letterFinder, Chunker chunker, LetterIdentifier letterIdentifier, List<PostProcessor> postProcessors, PlaintextConverter plaintextConverter) {
		this.preProcessors = preProcessors;
		this.letterFinder = letterFinder;
		this.letterIdentifier = letterIdentifier;
		this.chunker = chunker;
		this.postProcessors = postProcessors;
		this.plaintextConverter = plaintextConverter;
	}

	public String recognize(BufferedImage img) {
		return plaintextConverter.convert(process(img, new HashMap<String, String>()));
	}

	public BufferedImage visualize(BufferedImage img) {
		Result result = process(img, new HashMap<String, String>());
		Visualizer.visualize(result);
		return result.img;
	}

	public Result process(BufferedImage img, HashMap<String, String> md) {
		for (PreProcessor pp : preProcessors) {
			img = pp.process(img, md);
		}
		ArrayList<Letter> Letters = letterFinder.find(img, md);
		Result result = chunker.chunk(Letters, img, md);
		
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
