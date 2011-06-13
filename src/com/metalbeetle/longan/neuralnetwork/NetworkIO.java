package com.metalbeetle.longan.neuralnetwork;

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
