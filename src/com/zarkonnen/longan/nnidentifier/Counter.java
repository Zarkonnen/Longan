package com.zarkonnen.longan.nnidentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Counter<T> implements Comparator<Map.Entry<T, Integer>> {
	public final HashMap<T, Integer> counts = new HashMap<T, Integer>();
	
	public void increment(T t) {
		if (counts.containsKey(t)) {
			counts.put(t, counts.get(t) + 1);
		} else {
			counts.put(t, 1);
		}
	}
	
	public ArrayList<Map.Entry<T, Integer>> sortedCountsHighestFirst() {
		ArrayList<Map.Entry<T, Integer>> cs = new ArrayList<Map.Entry<T, Integer>>(counts.entrySet());
		Collections.sort(cs, this);
		return cs;
	}

	public int compare(Entry<T, Integer> t, Entry<T, Integer> t1) {
		return t1.getValue() - t.getValue();
	}
}
