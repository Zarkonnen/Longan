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
	
	static void readRelativeSizeInfo(Config.RelativeSizeDiscriminator d, InputStream is) throws JSONException {
		JSONObject o = new JSONObject(new JSONTokener(new InputStreamReader(is)));
		d.boundarySize = o.getDouble("boundarySize");
		d.triggerIsAboveBoundary = o.getBoolean("triggerIsAboveBoundary");
	}
	
	static void writeRelativeSizeInfo(Config.RelativeSizeDiscriminator d, OutputStream os) throws JSONException, UnsupportedEncodingException, IOException {
		JSONObject o = new JSONObject();
		o.put("boundarySize", d.boundarySize);
		o.put("triggerIsAboveBoundary", d.triggerIsAboveBoundary);
		os.write(o.toString(4).getBytes("UTF-8"));
	}
	
	static void readRelativeSizeInfo(Config.Identifier id, InputStream is) throws JSONException {
		JSONObject o = new JSONObject(new JSONTokener(new InputStreamReader(is)));
		id.expectedRelativeSizes = new HashMap<String, Double>();
		JSONArray keys = o.names();
		for (int i = 0; i < keys.length(); i++) {
			id.expectedRelativeSizes.put(keys.getString(i), o.getDouble(keys.getString(i)));
		}
	}
	
	static void writeRelativeSizeInfo(Config.Identifier id, OutputStream os) throws JSONException, UnsupportedEncodingException, IOException {
		JSONObject o = new JSONObject();
		for (Map.Entry<String, Double> e : id.expectedRelativeSizes.entrySet()) {
			o.put(e.getKey(), e.getValue());
		}
		os.write(o.toString(4).getBytes("UTF-8"));
	}
	
	static void readAspectRatioInfo(Config.Identifier id, InputStream is) throws JSONException {
		JSONObject o = new JSONObject(new JSONTokener(new InputStreamReader(is)));
		id.expectedAspectRatios = new HashMap<String, Double>();
		JSONArray keys = o.names();
		for (int i = 0; i < keys.length(); i++) {
			id.expectedAspectRatios.put(keys.getString(i), o.getDouble(keys.getString(i)));
		}
	}
	
	static void writeAspectRatioInfo(Config.Identifier id, OutputStream os) throws JSONException, UnsupportedEncodingException, IOException {
		JSONObject o = new JSONObject();
		for (Map.Entry<String, Double> e : id.expectedAspectRatios.entrySet()) {
			o.put(e.getKey(), e.getValue());
		}
		os.write(o.toString(4).getBytes("UTF-8"));
	}
	
	static void readAspectRatioInfo(Config.AspectRatioDiscriminator d, InputStream is) throws JSONException {
		JSONObject o = new JSONObject(new JSONTokener(new InputStreamReader(is)));
		d.boundaryRatio = o.getDouble("boundaryRatio");
		d.triggerIsAboveBoundary = o.getBoolean("triggerIsAboveBoundary");
	}
	
	static void writeAspectRatioInfo(Config.AspectRatioDiscriminator d, OutputStream os) throws JSONException, UnsupportedEncodingException, IOException {
		JSONObject o = new JSONObject();
		o.put("boundaryRatio", d.boundaryRatio);
		o.put("triggerIsAboveBoundary", d.triggerIsAboveBoundary);
		os.write(o.toString(4).getBytes("UTF-8"));
	}
	
	static void readNumberOfPartsInfo(Config.NumberOfPartsDiscriminator d, InputStream is) throws JSONException {
		JSONObject o = new JSONObject(new JSONTokener(new InputStreamReader(is)));
		d.numberOfPartsBoundary = o.getInt("numberOfPartsBoundary");
		d.triggerIsAboveBoundary = o.getBoolean("triggerIsAboveBoundary");
		d.enabled = o.getBoolean("enabled");
	}
	
	static void writeNumberOfPartsInfo(Config.NumberOfPartsDiscriminator d, OutputStream os) throws JSONException, UnsupportedEncodingException, IOException {
		JSONObject o = new JSONObject();
		o.put("numberOfPartsBoundary", d.numberOfPartsBoundary);
		o.put("triggerIsAboveBoundary", d.triggerIsAboveBoundary);
		o.put("enabled", d.enabled);
		os.write(o.toString(4).getBytes("UTF-8"));
	}
	
	public static Config readDefaultArchive() throws ZipException, IOException, JSONException {
		FastLoadingNetwork identifierTemplate = new FastLoadingNetwork();
		identifierTemplate.loadShape(NetworkIO.class.getResourceAsStream("identifier.lns"));
		FastLoadingNetwork discriminatorTemplate = new FastLoadingNetwork();
		discriminatorTemplate.loadShape(NetworkIO.class.getResourceAsStream("discriminator.lns"));
		
		Config c = new Config(new JSONObject(new JSONTokener(new InputStreamReader(NetworkIO.class.getResourceAsStream("data/source.json"), "UTF-8"))));
		HashSet<String> taken = new HashSet<String>();
		for (Config.Identifier identifier : c.identifiers) {
			String name = getName(identifier, taken);
			/*
			identifier.network = new IdentifierNet();
			input(identifier.network.nw, NetworkIO.class.getResourceAsStream("data/" + name + ".lin"));
			*/
			identifier.fastNetwork = identifierTemplate.cloneWithSameShape();
			identifier.fastNetwork.loadWeights(NetworkIO.class.getResourceAsStream("data/" + name + ".lfn"));
			readRelativeSizeInfo(identifier, NetworkIO.class.getResourceAsStream("data/" + name + ".lls"));
			readAspectRatioInfo(identifier, NetworkIO.class.getResourceAsStream("data/" + name + ".lla"));
		}
		for (Config.Discriminator discriminator : c.discriminators) {
			if (discriminator instanceof Config.NNDiscriminator) {
				String name = getName(discriminator, taken);
				Config.NNDiscriminator d = (Config.NNDiscriminator) discriminator;
				d.fastNetwork = discriminatorTemplate.cloneWithSameShape();
				d.fastNetwork.loadWeights(NetworkIO.class.getResourceAsStream("data/" + name + ".ldf")); 
				/*
				((Config.NNDiscriminator) discriminator).network = new DiscriminatorNet();
				input(((Config.NNDiscriminator) discriminator).network.nw, NetworkIO.class.getResourceAsStream("data/" + name + ".ldn"));
				*/
			}
			if (discriminator instanceof Config.AspectRatioDiscriminator) {
				readAspectRatioInfo((Config.AspectRatioDiscriminator) discriminator, NetworkIO.class.getResourceAsStream("data/" + getName(discriminator, taken) + ".lda"));
			}
			if (discriminator instanceof Config.NumberOfPartsDiscriminator) {
				readNumberOfPartsInfo((Config.NumberOfPartsDiscriminator) discriminator, NetworkIO.class.getResourceAsStream("data/" + getName(discriminator, taken) + ".ldp"));
			}
			if (discriminator instanceof Config.RelativeSizeDiscriminator) {
				readRelativeSizeInfo((Config.RelativeSizeDiscriminator) discriminator, NetworkIO.class.getResourceAsStream("data/" + getName(discriminator, taken) + ".lds"));
			}
		}
		
		return c;
	}
	
	public static Config readArchive(File archiveF) throws ZipException, IOException, JSONException {
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
				if (json.getString("type").equals("identifier")) {
					Config.Identifier identifier = new Config.Identifier(json);
					identifier.network = new IdentifierNet();
					input(identifier.network.nw,
							zf.getInputStream(new ZipEntry(ze.getName().substring(0, ze.getName().length() - 5) + ".lin")));
					c.identifiers.add(identifier);
					readRelativeSizeInfo(identifier,
							zf.getInputStream(new ZipEntry(ze.getName().substring(0, ze.getName().length() - 5) + ".lls")));
					readAspectRatioInfo(identifier,
							zf.getInputStream(new ZipEntry(ze.getName().substring(0, ze.getName().length() - 5) + ".lla")));
				}
				if (json.getString("type").equals("discriminator")) {
					Config.NNDiscriminator discriminator = new Config.NNDiscriminator(json);
					discriminator.network = new DiscriminatorNet();
					input(discriminator.network.nw,
							zf.getInputStream(new ZipEntry(ze.getName().substring(0, ze.getName().length() - 5) + ".ldn")));
					c.discriminators.add(discriminator);
				}
				if (json.getString("type").equals("aspectRatioDiscriminator")) {
					Config.AspectRatioDiscriminator discriminator = new Config.AspectRatioDiscriminator(json);
					readAspectRatioInfo(discriminator, zf.getInputStream(new ZipEntry(ze.getName().substring(0, ze.getName().length() - 5) + ".lda")));
					c.discriminators.add(discriminator);
				}
				if (json.getString("type").equals("numberOfPartsDiscriminator")) {
					Config.NumberOfPartsDiscriminator discriminator = new Config.NumberOfPartsDiscriminator(json);
					readNumberOfPartsInfo(discriminator, zf.getInputStream(new ZipEntry(ze.getName().substring(0, ze.getName().length() - 5) + ".ldp")));
					c.discriminators.add(discriminator);
				}
				if (json.getString("type").equals("relativeSizeDiscriminator")) {
					Config.RelativeSizeDiscriminator discriminator = new Config.RelativeSizeDiscriminator(json);
					readRelativeSizeInfo(discriminator, zf.getInputStream(new ZipEntry(ze.getName().substring(0, ze.getName().length() - 5) + ".lds")));
					c.discriminators.add(discriminator);
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
			zos.putNextEntry(new ZipEntry(name + ".lin"));
			NetworkIO.output(identifier.network.nw, zos);
			zos.putNextEntry(new ZipEntry(name + ".lls"));
			writeRelativeSizeInfo(identifier, zos);
			zos.putNextEntry(new ZipEntry(name + ".lla"));
			writeAspectRatioInfo(identifier, zos);
		}
		
		for (Config.Discriminator discriminator : config.discriminators) {
			String name = getName(discriminator, taken);
			zos.putNextEntry(new ZipEntry(name + ".json"));
			zos.write(discriminator.toJSON().toString(4).getBytes("UTF-8"));
			if (discriminator instanceof Config.NNDiscriminator) {
				zos.putNextEntry(new ZipEntry(name + ".ldn"));
				NetworkIO.output(((Config.NNDiscriminator) discriminator).network.nw, zos);
			}
			if (discriminator instanceof Config.AspectRatioDiscriminator) {
				zos.putNextEntry(new ZipEntry(name + ".lda"));
				writeAspectRatioInfo((Config.AspectRatioDiscriminator) discriminator, zos);
			}
			if (discriminator instanceof Config.NumberOfPartsDiscriminator) {
				zos.putNextEntry(new ZipEntry(name + ".ldp"));
				writeNumberOfPartsInfo((Config.NumberOfPartsDiscriminator) discriminator, zos);
			}
			if (discriminator instanceof Config.RelativeSizeDiscriminator) {
				zos.putNextEntry(new ZipEntry(name + ".lds"));
				writeRelativeSizeInfo((Config.RelativeSizeDiscriminator) discriminator, zos);
			}
		}
		
		zos.closeEntry();
		zos.flush(); // Bug in older versions of Java where close() would swallow flush() errors.
		zos.close();
	}
	
	static String getName(Config.Identifier i, HashSet<String> taken) {
		String base = i.font.font.replaceAll("[^a-zA-Z]", "").toLowerCase();
		if (i.font.italic) { base += "_italic"; }
		if (base.isEmpty()) { base = "identifier"; }
		if (!taken.contains(base)) { taken.add(base); return base; }
		int num = 2;
		while (true) {
			String name = base + "_" + num;
			if (!taken.contains(name)) { taken.add(name); return name; }
			num++;
		}
	}
	
	static String getName(Config.Discriminator d, HashSet<String> taken) {
		String base = d.font.font.replaceAll("[^a-zA-Z]", "").toLowerCase();
		if (d.font.italic) { base += "_italic"; }
		base += ("_" + d.trigger + "_vs_" + d.alternative).replaceAll("[^a-zA-Z_]", "").toLowerCase();
		if (base.isEmpty()) { base = "discriminator"; }
		if (!taken.contains(base)) { taken.add(base); return base; }
		int num = 2;
		while (true) {
			String name = base + "_" + num;
			if (!taken.contains(name)) { taken.add(name); return name; }
			num++;
		}
	}
}
