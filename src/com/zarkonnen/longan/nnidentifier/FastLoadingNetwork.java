package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.nnidentifier.network.Layer;
import com.zarkonnen.longan.nnidentifier.network.Network;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class FastLoadingNetwork {
	public ArrayList<FastLayer> layers = new ArrayList<FastLayer>();
	public int inputSize;
	
	public FastLoadingNetwork initFromNetwork(Network n) {
		inputSize = n.layers.get(0).nodes.size();
		for (int i = 1; i < n.layers.size(); i++) {
			layers.add(new FastLayer(n.layers.get(i), n.layers.get(i - 1)));
		}
		return this;
	}
	
	public FastLoadingNetwork loadShape(InputStream in) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(in);
		inputSize = ois.readInt();
		int numLayers = ois.readInt();
		for (int layerN = 0; layerN < numLayers; layerN++) {
			FastLayer l = new FastLayer();
			layers.add(l);
			l.numNodes            = ois.readInt();
			l.numWeights          = ois.readInt();
			l.totalNumConnections = ois.readInt();
			l.hasBias             = ois.readBoolean();
			l.connectionOffsets = new int[l.numNodes];
			l.numConnections    = new int[l.numNodes];
			l.connections       = new int[l.totalNumConnections];
			l.weightConnections = new int[l.totalNumConnections];
			l.weights           = new float[l.numWeights];
			l.biases            = new float[l.numNodes];
			l.output         = new float[l.numNodes];
			for (int i = 0; i < l.connectionOffsets.length; i++) {
				l.connectionOffsets[i] = ois.readInt();
			}
			for (int i = 0; i < l.numConnections.length; i++) {
				l.numConnections[i] = ois.readInt();
			}
			for (int i = 0; i < l.connections.length; i++) {
				l.connections[i] = ois.readInt();
			}
			for (int i = 0; i < l.weightConnections.length; i++) {
				l.weightConnections[i] = ois.readInt();
			}
		}
		return this;
	}
	
	public void saveShape(OutputStream out) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeInt(inputSize);
		oos.writeInt(layers.size());
		for (FastLayer l : layers) {
			oos.writeInt(l.numNodes);
			oos.writeInt(l.numWeights);
			oos.writeInt(l.totalNumConnections);
			oos.writeBoolean(l.hasBias);
			for (int i = 0; i < l.connectionOffsets.length; i++) {
				oos.writeInt(l.connectionOffsets[i]);
			}
			for (int i = 0; i < l.numConnections.length; i++) {
				oos.writeInt(l.numConnections[i]);
			}
			for (int i = 0; i < l.connections.length; i++) {
				oos.writeInt(l.connections[i]);
			}
			for (int i = 0; i < l.weightConnections.length; i++) {
				oos.writeInt(l.weightConnections[i]);
			}
		}
		oos.flush();
	}
	
	public FastLoadingNetwork loadWeights(InputStream in) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(in);
		for (FastLayer l : layers) {
			for (int i = 0; i < l.weights.length; i++) {
				l.weights[i] = ois.readFloat();
			}
			for (int i = 0; i < l.biases.length; i++) {
				l.biases[i] = ois.readFloat();
			}
		}
		return this;
	}
	
	public void saveWeights(OutputStream out) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(out);
		for (FastLayer l : layers) {
			for (int i = 0; i < l.weights.length; i++) {
				oos.writeFloat(l.weights[i]);
			}
			for (int i = 0; i < l.biases.length; i++) {
				oos.writeFloat(l.biases[i]);
			}
		}
		oos.flush();
	}
	
	public FastLoadingNetwork cloneWithSameShape() {
		FastLoadingNetwork n2 = new FastLoadingNetwork();
		n2.inputSize = inputSize;
		for (FastLayer l : layers) { n2.layers.add(l.cloneWithSameShape()); }
		return n2;
	}
	
	public float[] run(float[] input) {
		for (FastLayer l : layers) {
			l.run(input);
			input = l.output;
		}
		return input;
	}
	
	public static class FastLayer {
		int numNodes;
		int numWeights;
		int totalNumConnections;
		boolean hasBias;
		int[]   connectionOffsets;
		int[]   numConnections;
		int[]   connections;
		int[]   weightConnections;
		float[] weights;
		float[] biases;
		float[] output;
		
		public FastLayer() {}
		
		public FastLayer(Layer layer, Layer prevLayer) {
			// Analyse the layer.
			numNodes = layer.nodes.size();
			numWeights = prevLayer.weights.size();
			hasBias = hasBias(layer);
			connectionOffsets = new int[numNodes];
			numConnections    = new int[numNodes];
			totalNumConnections = 0;
			for (int node = 0; node < numNodes; node++) {
				connectionOffsets[node] = totalNumConnections;
				numConnections[node]    = layer.nodes.get(node).incoming.size() - (hasBias ? 1 : 0);
				totalNumConnections += numConnections[node];
			}
			connections       = new int  [totalNumConnections];
			weightConnections = new int[totalNumConnections];
			weights           = new float[numWeights];
			biases            = new float[numNodes];
			for (int w = 0; w < numWeights; w++) {
				weights[w] = prevLayer.weights.get(w).value;
			}
			for (int node = 0; node < numNodes; node++) {
				for (int input = 0; input < numConnections[node]; input++) {
					connections[connectionOffsets[node] + input] = prevLayer.nodes.indexOf(
							layer.nodes.get(node).incoming.get(input).input);
					weightConnections[connectionOffsets[node] + input] = prevLayer.weights.indexOf(
							layer.nodes.get(node).incoming.get(input).weight);
				}
				if (hasBias) {
					biases[node] = (float) layer.nodes.get(node).incoming.get(numConnections[node]).weight.value;
				}
			}
			
			output = new float[numNodes];
		}
				
		public void run(float[] prevLayer) {
			for (int n = 0; n < numNodes; n++) {
				float x = biases[n];
				for (int i = 0; i < numConnections[n]; i += 1) {
					x += weights[weightConnections[connectionOffsets[n] + i]]
							* prevLayer[connections[connectionOffsets[n] + i]];
				}
				output[n] = (float) Math.tanh(x);
			}
		}
		
		public FastLayer cloneWithSameShape() {
			FastLayer l2 = new FastLayer();
			l2.numNodes = numNodes;
			l2.numWeights = numWeights;
			l2.totalNumConnections = totalNumConnections;
			l2.hasBias = hasBias;
			l2.connectionOffsets = connectionOffsets;
			l2.numConnections = numConnections;
			l2.connections = connections;
			l2.weightConnections = weightConnections;
			l2.weights = new float[weights.length];
			l2.biases  = new float[biases.length];
			l2.output  = new float[output.length];
			return l2;
		}
	}
	
	// NB: Horrible.
	public static boolean hasBias(Layer layer) {
		return layer.nodes.get(0).incoming.get(layer.nodes.get(0).incoming.size() - 1).input.name.toLowerCase().contains("bias");
	}
}
