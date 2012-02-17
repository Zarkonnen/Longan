package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.nnidentifier.Config.LetterClass;
import com.zarkonnen.longan.nnidentifier.Config.NearestNeighbourIdentifier;
import com.zarkonnen.longan.nnidentifier.network.Util;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NearestNeighbour {
	public static class Comparisons {
		HashMap<String, ArrayList<float[]>> cmps = new HashMap<String, ArrayList<float[]>>();
		
		public double leastError(ArrayList<String> classMembers, float[] input) {
			double leastErr = -1;
			for (String l : classMembers) {
				for (float[] cmp : cmps.get(l)) {
					double error = 0.0;
					for (int y = 6; y < 22; y++) { for (int x = 6; x < 22; x++) {
						error += (input[y * 28 + x] - cmp[y * 28 + x]) * (input[y * 28 + x] - cmp[y * 28 + x]);
					}}
					leastErr = leastErr == -1 ? error : Math.min(error, leastErr);
				}
			}
			return leastErr;
		}
	}
	
	public static void write(Comparisons c, ObjectOutputStream oos) throws IOException {
		oos.writeInt(0); // Version
		oos.writeInt(c.cmps.size());
		for (Map.Entry<String, ArrayList<float[]>> e : c.cmps.entrySet()) {
			oos.writeUTF(e.getKey());
			oos.writeInt(e.getValue().size());
			for (float[] cmp : e.getValue()) {
				for (int i = 0; i < 28 * 28; i++) { oos.writeFloat(cmp[i]); }
			}
		}
	}
	
	public static Comparisons read(ObjectInputStream ois) throws IOException {
		if (ois.readInt() > 0) {
			throw new IOException("Comparisons format too modern: I only support up to version 0.");
		}
		Comparisons c = new Comparisons();
		int n = ois.readInt();
		for (int i = 0; i < n; i++) {
			ArrayList<float[]> lCmps = new ArrayList<float[]>();
			c.cmps.put(ois.readUTF(), lCmps);
			int m = ois.readInt();
			for (int j = 0; j < m; j++) {
				float[] cmp = new float[28 * 28];
				lCmps.add(cmp);
				for (int k = 0; k < 28 * 28; k++) {
					cmp[k] = ois.readFloat();
				}
			}
		}
		return c;
	}
	
	public static Comparisons createComparisons(NearestNeighbourIdentifier nni) {
		Comparisons c = new Comparisons();
		for (LetterClass lc : nni.classes) {
			for (String letter : lc.members) {
				ArrayList<float[]> l = new ArrayList<float[]>();
				c.cmps.put(letter, l);
				for (Config.FontType ft : nni.fonts) {
					l.add(Util.getInputForNN(
							ExampleGenerator2.getSimpleLetter(letter, ft.font, ft.italic),
							false));
				}
			}
		}
		
		return c;
	}
}
