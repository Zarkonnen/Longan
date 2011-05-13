package com.metalbeetle.longan;

import com.metalbeetle.longan.better.BetterChunker;
import com.metalbeetle.longan.dummy.DummyLetterFinder;
import com.metalbeetle.longan.dummy.DummyLetterIdentifier;
import com.metalbeetle.longan.dummy.DummyChunker;
import com.metalbeetle.longan.neuralnetwork.NeuralNetworkLetterIdentifier;
import com.metalbeetle.longan.simple.SimpleLetterFinder;
import com.metalbeetle.longan.simple.SimpleLetterIdentifier;
import com.metalbeetle.longan.simple.SimpleWordPlaintextConverter;
import com.metalbeetle.longan.stage.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Longan {
	final LetterFinder letterFinder;
	final LetterIdentifier letterIdentifier;
	final Chunker chunker;
	final List<PostProcessor> postProcessors;
	final PlaintextConverter plaintextConverter;

	public static Longan getSimpleImplementation() {
		return new Longan(
			new SimpleLetterFinder(),
			new BetterChunker(),
			new NeuralNetworkLetterIdentifier(),
			new ArrayList<PostProcessor>(),
			new SimpleWordPlaintextConverter()
		);
	}

	public static Longan getDummyImplementation() {
		return new Longan(
			new DummyLetterFinder(),
			new DummyChunker(),
			new DummyLetterIdentifier(),
			new ArrayList<PostProcessor>(),
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
		return plaintextConverter.convert(process(img), img);
	}

	public void visualize(BufferedImage img) {
		Visualizer.visualize(process(img), img);
	}

	public ArrayList<ArrayList<ArrayList<Letter>>> process(BufferedImage img) {
		ArrayList<Rectangle> letterRects = letterFinder.find(img);
		ArrayList<ArrayList<ArrayList<Rectangle>>> rectLines = chunker.chunk(letterRects, img);
		ArrayList<ArrayList<ArrayList<Letter>>> lines = new ArrayList<ArrayList<ArrayList<Letter>>>();
		for (ArrayList<ArrayList<Rectangle>> rectLine : rectLines) {
			ArrayList<ArrayList<Letter>> line = new ArrayList<ArrayList<Letter>>();
			lines.add(line);
			for (ArrayList<Rectangle> rectWord : rectLine) {
				ArrayList<Letter> word = new ArrayList<Letter>();
				line.add(word);
				for (Rectangle r : rectWord) {
					word.add(letterIdentifier.identify(r, img));
				}
			}
		}
		
		for (PostProcessor pp : postProcessors) {
			pp.process(lines, img);
		}
		return lines;
	}
}
