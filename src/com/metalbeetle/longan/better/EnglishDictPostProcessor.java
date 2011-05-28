package com.metalbeetle.longan.better;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.PostProcessor;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class EnglishDictPostProcessor implements PostProcessor {
	static final HashMap<Integer, ArrayList<String>> L_TO_WS = new HashMap<Integer, ArrayList<String>>();
	
	static final double YES = 1.0;
	
	static {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(EnglishDictPostProcessor.class.getResourceAsStream("wordsEn.txt")));
			String w = null;
			while ((w = br.readLine()) != null) {
				if (!L_TO_WS.containsKey(w.length())) {
					ArrayList<String> l = new ArrayList<String>();
					L_TO_WS.put(w.length(), l);
				}
				ArrayList<String> l = L_TO_WS.get(w.length());
				l.add(w);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			try { br.close(); } catch (Exception e) {}
		}
	}
	
	final double maxCoercion = 0.25;
	final double baseMC = 0.6;
	
	public void process(ArrayList<ArrayList<ArrayList<Letter>>> lines, BufferedImage img) {
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				process(word);
			}
		}
	}
	
	void process(ArrayList<Letter> word) {
		if (word.isEmpty()) { return; }
		if (word.get(word.size() - 1).bestLetter().matches("[.,:!?;)+='-]")) {
			ArrayList<Letter> w2 = new ArrayList<Letter>();
			w2.addAll(word.subList(0, word.size() - 1));
			process(w2);
			return;
		}
		if (word.get(0).bestLetter().matches("[.,:!?;(+='-]")) {
			ArrayList<Letter> w2 = new ArrayList<Letter>();
			w2.addAll(word.subList(1, word.size()));
			process(w2);
			return;
		}
		if (!L_TO_WS.containsKey(word.size())) { return; }
		String wordS = "";
		for (Letter l : word) {
			wordS += l.bestLetter();
		}
		ArrayList<String> candidates = L_TO_WS.get(word.size());
		if (candidates.contains(wordS.toLowerCase())) { return; }
		
		String bestW = null;
		double bestDelta = 0.0;
		
		for (String candidate : candidates) {
			double delta = 0.0;
			for (int i = 0; i < word.size(); i++) {
				delta += 1 - word.get(i).possibleLetters.get(candidate.substring(i, i + 1));
			}
			if (bestW == null || delta < bestDelta) {
				bestW = candidate;
				bestDelta = delta;
			}
		}
		
		if (bestW != null && bestDelta <= baseMC + maxCoercion * word.size()) {
			for (int i = 0; i < word.size(); i++) {
				word.get(i).possibleLetters.put(bestW.substring(i, i + 1), YES);
			} 
		}
	}
}
