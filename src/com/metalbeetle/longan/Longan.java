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

import com.metalbeetle.longan.better.BetterChunker;
import com.metalbeetle.longan.better.BetterLetterFinder;
import com.metalbeetle.longan.better.HeuristicPostProcessor;
import com.metalbeetle.longan.better.LetterSplittingPostProcessor;
import com.metalbeetle.longan.better.RotationFixingPreProcessor;
import com.metalbeetle.longan.neuralnetwork.NeuralNetworkLetterIdentifier;
import com.metalbeetle.longan.neuralnetwork.NeuralNetworkLetterIdentifierPostProcessor;
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
		preps.add(new RotationFixingPreProcessor());
		ArrayList<PostProcessor> pps = new ArrayList<PostProcessor>();
		pps.add(new LetterSplittingPostProcessor());
		pps.add(new NeuralNetworkLetterIdentifierPostProcessor());
		pps.add(new HeuristicPostProcessor());
		return new Longan(
			preps,
			new BetterLetterFinder(),
			new BetterChunker(),
			new NeuralNetworkLetterIdentifier(),
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
		HashMap<String, String> md = new HashMap<String, String>();
		ProcessResult pr = process(img, md);
		return plaintextConverter.convert(pr.lines, pr.img, md);
	}

	public void visualize(BufferedImage img) {
		HashMap<String, String> md = new HashMap<String, String>();
		ProcessResult pr = process(img, md);
		Visualizer.visualize(pr.lines, pr.img);
	}
	
	public static class ProcessResult {
		ArrayList<ArrayList<ArrayList<Letter>>> lines;
		BufferedImage img;

		public ProcessResult(ArrayList<ArrayList<ArrayList<Letter>>> lines, BufferedImage img) {
			this.lines = lines;
			this.img = img;
		}
	}

	public ProcessResult process(BufferedImage img, HashMap<String, String> md) {
		for (PreProcessor pp : preProcessors) {
			img = pp.process(img, md);
		}
		ArrayList<LetterRect> letterRects = letterFinder.find(img, md);
		ArrayList<ArrayList<ArrayList<LetterRect>>> rectLines = chunker.chunk(letterRects, img, md);
		ArrayList<ArrayList<ArrayList<Letter>>> lines = new ArrayList<ArrayList<ArrayList<Letter>>>();
		for (ArrayList<ArrayList<LetterRect>> rectLine : rectLines) {
			ArrayList<ArrayList<Letter>> line = new ArrayList<ArrayList<Letter>>();
			lines.add(line);
			for (ArrayList<LetterRect> rectWord : rectLine) {
				ArrayList<Letter> word = new ArrayList<Letter>();
				line.add(word);
				for (LetterRect r : rectWord) {
					word.add(letterIdentifier.identify(r, img, md));
				}
			}
		}
		
		for (PostProcessor pp : postProcessors) {
			pp.process(lines, img, md, this);
		}
		return new ProcessResult(lines, img);
	}
}
