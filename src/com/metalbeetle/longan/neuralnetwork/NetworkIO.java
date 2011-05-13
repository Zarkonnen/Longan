package com.metalbeetle.longan.neuralnetwork;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class NetworkIO {
	public static void output(Network nw, OutputStream os) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		for (Layer l : nw.layers) {
			for (Weight w : l.weights) {
				oos.writeDouble(w.value);
			}
		} 
		oos.flush();
	}
	
	public static void input(Network nw, InputStream is) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(is);
		for (Layer l : nw.layers) {
			for (Weight w : l.weights) {
				w.value = ois.readDouble();
			}
		}
	}
}
