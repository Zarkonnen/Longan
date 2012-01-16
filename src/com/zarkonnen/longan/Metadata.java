package com.zarkonnen.longan;

import java.util.HashMap;

public class Metadata {
	private final HashMap<Key, Object> data = new HashMap<Key, Object>();
	
	public <T> boolean has(Key<T> key) {
		return data.containsKey(key);
	}
	
	public <T> T get(Key<T> key, T fallback) {
		return (T) (data.containsKey(key) ? data.get(key) : fallback);
	}
	
	public <T> T get(Key<T> key) {
		if (!data.containsKey(key)) { throw new RuntimeException("Metadata not found for " + key.name + "."); }
		return (T) data.get(key);
	}
	
	public <T> void put(Key<T> key, T value) {
		data.put(key, value);
	}
	
	public static <T> Key<T> key(String name, Class<T> cl) {
		return new Key<T>(name);
	}
	
	public static class Key<T> {
		public final String name;

		public Key(String name) {
			this.name = name;
		}
		
		@Override
		public boolean equals(Object o2) {
			return o2 instanceof Key && ((Key) o2).name.equals(name);
		}
		
		@Override
		public int hashCode() { return name.hashCode(); }
	}
}
