package com.metalbeetle.longan;

import com.metalbeetle.longan.better.BetterChunker;
import com.metalbeetle.longan.better.BetterLetterFinder;
import com.metalbeetle.longan.better.EnglishDictPostProcessor;
import com.metalbeetle.longan.better.HeuristicPostProcessor;
import com.metalbeetle.longan.neuralnetwork.NNLI3PostProcessor;
import com.metalbeetle.longan.neuralnetwork.NNLetterIdentifier3;
import com.metalbeetle.longan.simple.SimpleWordPlaintextConverter;
import com.metalbeetle.longan.stage.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Longan {
	final LetterFinder letterFinder;
	final LetterIdentifier letterIdentifier;
	final Chunker chunker;
	final List<PostProcessor> postProcessors;
	final PlaintextConverter plaintextConverter;

	public static Longan getDefaultImplementation() {
		ArrayList<PostProcessor> pps = new ArrayList<PostProcessor>();
		pps.add(new NNLI3PostProcessor());
		pps.add(new HeuristicPostProcessor());
		//pps.add(new EnglishDictPostProcessor());
		return new Longan(
			new BetterLetterFinder(),
			new BetterChunker(),
			new NNLetterIdentifier3(),
			pps,
			new SimpleWordPlaintextConverter()
		);
	}

	public Longan(LetterFinder letterFinder, Chunker chunker, LetterIdentifier letterIdentifier, List<PostProcessor> postProcessors, PlaintextConverter plaintextConverter) {
		this.letterFinder = letterFinder;
		this.letterIdentifier = letterIdentifier;
		this.chunker = chunker;
		this.postProcessors = postProcessors;
		this.plaintextConverter = plaintextConverter;
	}

	public String recognize(BufferedImage img) {
		HashMap<String, String> md = new HashMap<String, String>();
		return plaintextConverter.convert(process(img, md), img, md);
	}

	public void visualize(BufferedImage img) {
		HashMap<String, String> md = new HashMap<String, String>();
		Visualizer.visualize(process(img, md), img);
	}

	public ArrayList<ArrayList<ArrayList<Letter>>> process(BufferedImage img, HashMap<String, String> md) {
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
			pp.process(lines, img, md);
		}
		return lines;
	}
}
