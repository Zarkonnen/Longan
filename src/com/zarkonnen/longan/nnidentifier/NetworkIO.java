package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.nnidentifier.network.Network;
import com.zarkonnen.longan.nnidentifier.network.Weight;
import com.zarkonnen.longan.nnidentifier.network.Layer;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class NetworkIO {
	static boolean networksLoaded = false;
	static FastLoadingNetwork identifierTemplate = new FastLoadingNetwork();
		
	static void loadNetworkShapes() throws IOException {
		if (!networksLoaded) {
			identifierTemplate.loadShape(NetworkIO.class.getResourceAsStream("identifier.lns"));
			networksLoaded = true;
		}
	}
	
	public static void output(Network nw, OutputStream os) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		for (Layer l : nw.layers) {
			for (Weight w : l.weights) {
				oos.writeFloat((float) w.value);
			}
		} 
		oos.flush();
	}
	
	public static void input(Network nw, InputStream is) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(is);
		for (Layer l : nw.layers) {
			for (Weight w : l.weights) {
				w.value = ois.readFloat();
			}
		}
	}
	
	static void readRelativeSizeInfo(Config.NNIdentifier id, InputStream is) throws JSONException, UnsupportedEncodingException {
		JSONObject o = new JSONObject(new JSONTokener(new InputStreamReader(is, "UTF-8")));
		id.expectedRelativeSizes = new HashMap<String, Double>();
		JSONArray keys = o.names();
		for (int i = 0; i < keys.length(); i++) {
			id.expectedRelativeSizes.put(keys.getString(i), o.getDouble(keys.getString(i)));
		}
	}
	
	static void writeRelativeSizeInfo(Config.NNIdentifier id, OutputStream os) throws JSONException, UnsupportedEncodingException, IOException {
		JSONObject o = new JSONObject();
		for (Map.Entry<String, Double> e : id.expectedRelativeSizes.entrySet()) {
			o.put(e.getKey(), e.getValue());
		}
		os.write(o.toString(4).getBytes("UTF-8"));
	}
	
	static void readAspectRatioInfo(Config.NNIdentifier id, InputStream is) throws JSONException, UnsupportedEncodingException {
		JSONObject o = new JSONObject(new JSONTokener(new InputStreamReader(is, "UTF-8")));
		id.expectedAspectRatios = new HashMap<String, Double>();
		JSONArray keys = o.names();
		for (int i = 0; i < keys.length(); i++) {
			id.expectedAspectRatios.put(keys.getString(i), o.getDouble(keys.getString(i)));
		}
	}
	
	static void writeAspectRatioInfo(Config.NNIdentifier id, OutputStream os) throws JSONException, UnsupportedEncodingException, IOException {
		JSONObject o = new JSONObject();
		for (Map.Entry<String, Double> e : id.expectedAspectRatios.entrySet()) {
			o.put(e.getKey(), e.getValue());
		}
		os.write(o.toString(4).getBytes("UTF-8"));
	}
	
	static void readNumberOfPartsInfo(Config.NumberOfPartsIdentifier d, InputStream is) throws JSONException {
		JSONObject o = new JSONObject(new JSONTokener(new InputStreamReader(is)));
		d.numberOfPartsBoundary = o.getInt("numberOfPartsBoundary");
		d.firstIsAboveBoundary = o.getBoolean("firstIsAboveBoundary");
		d.enabled = o.getBoolean("enabled");
	}
	
	static void writeNumberOfPartsInfo(Config.NumberOfPartsIdentifier d, OutputStream os) throws JSONException, UnsupportedEncodingException, IOException {
		JSONObject o = new JSONObject();
		o.put("numberOfPartsBoundary", d.numberOfPartsBoundary);
		o.put("firstIsAboveBoundary", d.firstIsAboveBoundary);
		o.put("enabled", d.enabled);
		os.write(o.toString(4).getBytes("UTF-8"));
	}
	
	static void writeTargets(Config.NNIdentifier id, int n, OutputStream os) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeInt(1); // version
		oos.writeInt(id.classes.size());
		for (Config.LetterClass lc : id.classes) {
			ArrayList<float[]> targets = id.targets.get(n).get(lc);
			oos.writeInt(targets.size());
			for (float[] t : targets) {
				for (int i = 0; i < t.length; i++) {
					oos.writeFloat(t[i]);
				}
			}
		}
		oos.flush();
	}
	
	static void readTargets(Config.NNIdentifier id, InputStream is) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(is);
		if (ois.readInt() != 1) {
			throw new IOException("Unknown network targets version!");
		}
		int nClasses = ois.readInt();
		if (nClasses != id.classes.size()) {
			throw new IOException("Network targets file is incompatible with its identifier: wrong number of classes.");
		}
		HashMap<Config.LetterClass, ArrayList<float[]>> nnTargets = new HashMap<Config.LetterClass, ArrayList<float[]>>();
		id.targets.add(nnTargets);
		for (Config.LetterClass lc : id.classes) {
			ArrayList<float[]> targets = new ArrayList<float[]>();
			nnTargets.put(lc, targets);
			int nTargets = ois.readInt();
			for (int t = 0; t < nTargets; t++) {
				float[] target = new float[ProfileGen.OUTPUT_SIZE];
				for (int i = 0; i < target.length; i++) {
					target[i] = ois.readFloat();
				}
				targets.add(target);
			}
		}
	}
	
	public static Config readDefaultArchive() throws ZipException, IOException, JSONException {
		loadNetworkShapes();
		Config c = new Config(new JSONObject(new JSONTokener(new InputStreamReader(NetworkIO.class.getResourceAsStream("data/source.json"), "UTF-8"))));
		HashSet<String> taken = new HashSet<String>();
		for (Config.Identifier identifier : c.identifiers) {
			if (identifier instanceof Config.NNIdentifier) {
				String name = getName(identifier, taken);
				Config.NNIdentifier id = (Config.NNIdentifier) identifier;
				for (int i = 0; i < id.numberOfNetworks; i++) {
					// qqDPS
					//FastLoadingNetwork fln = (id.classes.size() == 2 ? discriminatorTemplate : identifierTemplate).cloneWithSameShape();
					FastLoadingNetwork fln = identifierTemplate.cloneWithSameShape();
					fln.loadWeights(NetworkIO.class.getResourceAsStream("data/" + name + "_weights_" + i));
					id.fastNetworks.add(fln);
					readTargets(id, NetworkIO.class.getResourceAsStream("data/" + name + "_targets_" + i));
				}
				readRelativeSizeInfo(id, NetworkIO.class.getResourceAsStream("data/" + name + "_sizes.json"));
				readAspectRatioInfo(id, NetworkIO.class.getResourceAsStream("data/" + name + "_aspectRatios.json"));
			}
			if (identifier instanceof Config.NumberOfPartsIdentifier) {
				String name = getName(identifier, taken);
				Config.NumberOfPartsIdentifier id = (Config.NumberOfPartsIdentifier) identifier;
				readNumberOfPartsInfo(id, NetworkIO.class.getResourceAsStream("data/" + name + "_numberOfParts.json"));
			}
			if (identifier instanceof Config.TreeIdentifier) {
				String name = getName(identifier, taken);
				Config.TreeIdentifier id = (Config.TreeIdentifier) identifier;
				id.tree = TreePredict.readNode(new ObjectInputStream(NetworkIO.class.getResourceAsStream("data/" + name + "_tree")));
			}
			if (identifier instanceof Config.NearestNeighbourIdentifier) {
				String name = getName(identifier, taken);
				Config.NearestNeighbourIdentifier id = (Config.NearestNeighbourIdentifier) identifier;
				id.comparisons = NearestNeighbour.read(new ObjectInputStream(NetworkIO.class.getResourceAsStream("data/" + name + "_comparisons")));
			}
		}
		
		return c;
	}
	
	public static Config readArchive(File archiveF) throws ZipException, IOException, JSONException {
		loadNetworkShapes();
		ZipFile zf = new ZipFile(archiveF);
		Config c = new Config();
		Enumeration<? extends ZipEntry> zfi = zf.entries();
		while (zfi.hasMoreElements()) {
			ZipEntry ze = zfi.nextElement();
			if (ze.getName().endsWith(".json") && !ze.getName().equals("source.json")) {
				JSONObject json = null;
				try {
					json = new JSONObject(new JSONTokener(new InputStreamReader(zf.getInputStream(ze), "UTF-8")));
				} catch (Exception e) {
					continue;
				}
				String baseName = ze.getName().substring(0, ze.getName().length() - 5);
				if (!json.has("type")) { continue; } // qqDPS Solve this more cleanly!
				if (json.getString("type").equals("nnIdentifier")) {
					Config.NNIdentifier identifier = new Config.NNIdentifier(json);
					for (int i = 0; i < identifier.numberOfNetworks; i++) {
						// qqDPS
						//FastLoadingNetwork fln = (identifier.classes.size() == 2 ? discriminatorTemplate : identifierTemplate).cloneWithSameShape();
						FastLoadingNetwork fln = identifierTemplate.cloneWithSameShape();
						fln.loadWeights(
								zf.getInputStream(new ZipEntry(baseName + "_weights_" + i)));
						identifier.fastNetworks.add(fln);
						readTargets(identifier,
								zf.getInputStream(new ZipEntry(baseName + "_targets_" + i)));
					}
					c.identifiers.add(identifier);
					readRelativeSizeInfo(identifier,
							zf.getInputStream(new ZipEntry(baseName + "_sizes.json")));
					readAspectRatioInfo(identifier,
							zf.getInputStream(new ZipEntry(baseName + "_aspectRatios.json")));
				}
				if (json.getString("type").equals("numberOfPartsIdentifier")) {
					Config.NumberOfPartsIdentifier identifier = new Config.NumberOfPartsIdentifier(json);
					readNumberOfPartsInfo(identifier, zf.getInputStream(
							new ZipEntry(baseName + "_numberOfParts.json")));
					c.identifiers.add(identifier);
				}
				if (json.getString("type").equals("treeIdentifier")) {
					Config.TreeIdentifier identifier = new Config.TreeIdentifier(json);
					identifier.tree = TreePredict.readNode(new ObjectInputStream(zf.getInputStream(
							new ZipEntry(baseName + "_tree"))));
					c.identifiers.add(identifier);
				}
				if (json.getString("type").equals("nearestNeighbourIdentifier")) {
					Config.NearestNeighbourIdentifier identifier = new Config.NearestNeighbourIdentifier(json);
					identifier.comparisons = NearestNeighbour.read(new ObjectInputStream(zf.getInputStream(
							new ZipEntry(baseName + "_comparisons"))));
					c.identifiers.add(identifier);
				}
			}
		}
		
		return c;
	}
	
	public static void writeNetworks(Config config, File archiveF) throws IOException, JSONException {
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archiveF)));
		zos.putNextEntry(new ZipEntry("source.json"));
		zos.write(config.toJSON().toString(4).getBytes("UTF-8"));
		
		HashSet<String> taken = new HashSet<String>();
		taken.add("source");
		
		for (Config.Identifier identifier : config.identifiers) {
			String name = getName(identifier, taken);
			zos.putNextEntry(new ZipEntry(name + ".json"));
			zos.write(identifier.toJSON().toString(4).getBytes("UTF-8"));
			if (identifier instanceof Config.NNIdentifier) {
				Config.NNIdentifier id = (Config.NNIdentifier) identifier;
				for (int n = 0; n < id.numberOfNetworks; n++) {
					zos.putNextEntry(new ZipEntry(name + "_weights_" + n));
					new FastLoadingNetwork().initFromNetwork(id.networks.get(n)).saveWeights(zos);
					zos.putNextEntry(new ZipEntry(name + "_targets_" + n));
					writeTargets(id, n, zos);
				}
				zos.putNextEntry(new ZipEntry(name + "_sizes.json"));
				writeRelativeSizeInfo(id, zos);
				zos.putNextEntry(new ZipEntry(name + "_aspectRatios.json"));
				writeAspectRatioInfo(id, zos);
			}
			if (identifier instanceof Config.NumberOfPartsIdentifier) {
				zos.putNextEntry(new ZipEntry(name + "_numberOfParts.json"));
				writeNumberOfPartsInfo((Config.NumberOfPartsIdentifier) identifier, zos);
			}
			if (identifier instanceof Config.TreeIdentifier) {
				zos.putNextEntry(new ZipEntry(name + "_tree"));
				ObjectOutputStream oos = new ObjectOutputStream(zos);
				TreePredict.writeNode(((Config.TreeIdentifier) identifier).tree, oos);
				oos.flush();
			}
			if (identifier instanceof Config.NearestNeighbourIdentifier) {
				zos.putNextEntry(new ZipEntry(name + "_comparisons"));
				ObjectOutputStream oos = new ObjectOutputStream(zos);
				NearestNeighbour.write(((Config.NearestNeighbourIdentifier) identifier).comparisons, oos);
				oos.flush();
			}
		}
		
		zos.closeEntry();
		zos.flush(); // Bug in older versions of Java where close() would swallow flush() errors.
		zos.close();
	}
	
	static String getName(Config.Identifier i, HashSet<String> taken) {
		String base = "";
		for (Config.FontType ft : i.fonts) {
			String fb = ft.font.replaceAll("[^a-zA-Z]", "").toLowerCase();
			if (ft.italic) { fb += "_italic"; }
			base += fb + "_";
		}
		
		if (base.isEmpty()) { base = "identifier"; }
		String all = "";
		for (Config.LetterClass lc : i.classes) { for (String m : lc.members) {
			all += ProfileGen.letterToFilename(m);
		}}
		if (all.length() < 20) {
			base += "_" + all;
		} else {
			base += "_" + all.substring(0, 20);
		}
		if (!taken.contains(base)) { taken.add(base); return base; }
		int num = 2;
		while (true) {
			String name = base + "_" + num;
			if (!taken.contains(name)) { taken.add(name); return name; }
			num++;
		}
	}
}
