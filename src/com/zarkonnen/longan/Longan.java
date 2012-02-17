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
import com.zarkonnen.longan.better.IntensityHistogramPreProcessor;
import com.zarkonnen.longan.better.LetterSplittingPostProcessor;
import com.zarkonnen.longan.better.RotationFixingPreProcessor;
import com.zarkonnen.longan.better.SpeckleEliminator;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.nnidentifier.Identifier;
import com.zarkonnen.longan.simple.SimpleWordPlaintextConverter;
import com.zarkonnen.longan.stage.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Longan {
	public static final String VERSION = "0.9";
	
	public final boolean enableOpenCL;
	public final List<PreProcessor> preProcessors;
	public final LetterFinder letterFinder;
	public final LetterIdentifier letterIdentifier;
	public final Chunker chunker;
	public final List<PostProcessor> postProcessors;

	public static Longan getDefaultImplementation() {
		return getDefaultImplementation(true);
	}
	
	public static Longan getDefaultImplementation(boolean enableOpenCL) {
		ArrayList<PreProcessor> preps = new ArrayList<PreProcessor>();
		preps.add(new IntensityHistogramPreProcessor());
		preps.add(new RotationFixingPreProcessor());
		ArrayList<PostProcessor> pps = new ArrayList<PostProcessor>();
		pps.add(new LetterSplittingPostProcessor());
		//pps.add(new AggressiveLetterSplittingPostProcessor());
		pps.add(new SpeckleEliminator());
		return new Longan(
			preps,
			new BetterLetterFinder(),
			new BetterChunker2(),
			new Identifier(),
			pps,
			enableOpenCL
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
			List<PostProcessor> postProcessors,
			boolean enableOpenCL)
	{
		this.preProcessors = preProcessors;
		this.letterFinder = letterFinder;
		this.letterIdentifier = letterIdentifier;
		this.chunker = chunker;
		this.postProcessors = postProcessors;
		this.enableOpenCL = enableOpenCL;
	}

	public Result process(BufferedImage img) {
		HashMap<String, String> metadata = new HashMap<String, String>();
		metadata.put("enableOpenCL", "" + enableOpenCL);
		for (PreProcessor pp : preProcessors) {
			img = pp.process(img, metadata);
		}
		ArrayList<Letter> Letters = letterFinder.find(img, metadata);
		Result result = chunker.chunk(Letters, img, metadata);
		letterIdentifier.identify(result);
		
		for (PostProcessor pp : postProcessors) {
			pp.process(result, this);
		}
		letterIdentifier.finish();
		return result;
	}
}
