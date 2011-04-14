package com.metalbeetle.longan;

import com.metalbeetle.longan.dummy.DummyLetterFinder;
import com.metalbeetle.longan.dummy.DummyLetterIdentifier;
import com.metalbeetle.longan.dummy.DummyLineChunker;
import com.metalbeetle.longan.dummy.DummyWordChunker;
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
	final LineChunker lineChunker;
	final WordChunker wordChunker;
	final List<PostProcessor> postProcessors;
	final PlaintextConverter plaintextConverter;

	public static Longan getSimpleImplementation() {
		return new Longan(
			new SimpleLetterFinder(),
			new SimpleLetterIdentifier(),
			new DummyLineChunker(),
			new DummyWordChunker(),
			new ArrayList<PostProcessor>(),
			new SimpleWordPlaintextConverter()
		);
	}

	public static Longan getDummyImplementation() {
		return new Longan(
			new DummyLetterFinder(),
			new DummyLetterIdentifier(),
			new DummyLineChunker(),
			new DummyWordChunker(),
			new ArrayList<PostProcessor>(),
			new SimpleWordPlaintextConverter()
		);
	}

	public Longan(LetterFinder letterFinder, LetterIdentifier letterIdentifier, LineChunker lineChunker, WordChunker wordChunker, List<PostProcessor> postProcessors, PlaintextConverter plaintextConverter) {
		this.letterFinder = letterFinder;
		this.letterIdentifier = letterIdentifier;
		this.lineChunker = lineChunker;
		this.wordChunker = wordChunker;
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
		ArrayList<Letter> letters = letterIdentifier.identify(letterRects, img);
		ArrayList<ArrayList<Letter>> lines = lineChunker.chunk(letters, img);
		ArrayList<ArrayList<ArrayList<Letter>>> wordLines = wordChunker.chunk(lines, img);
		for (PostProcessor pp : postProcessors) {
			pp.process(wordLines, img);
		}
		return wordLines;
	}
}
