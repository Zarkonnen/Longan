package com.metalbeetle.longan;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage:\n" +
					"java -jar longan.jar recognize path-to-image-file\n" +
					"java -jar longan.jar visualize path-to-image-file path-to-output-file\n");
			return;
		}

		if (args[0].equals("recognize")) {
			System.out.print(Longan.getSimpleImplementation().recognize(ImageIO.read(new File(args[1]))));
		}
		if (args[0].equals("visualize")) {
			BufferedImage img = ImageIO.read(new File(args[1]));
			Longan.getSimpleImplementation().visualize(img);
			ImageIO.write(img, "jpg", new File(args[2]));
		}
	}
}
