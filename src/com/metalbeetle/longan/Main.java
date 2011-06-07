package com.metalbeetle.longan;

import com.metalbeetle.longan.better.BetterChunker;
import com.metalbeetle.longan.better.BetterLetterFinder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage:\n" +
					"java -jar longan.jar recognize path-to-image-file\n" +
					"java -jar longan.jar visualize path-to-image-file path-to-output-file\n" +
					"java -jar longan.jar categorize path-to-image-file path-to-output-folder\n");
			return;
		}

		if (args[0].equals("recognize")) {
			System.out.print(Longan.getDefaultImplementation().recognize(ImageIO.read(new File(args[1]))));
		}
		if (args[0].equals("visualize")) {
			BufferedImage img = ImageIO.read(new File(args[1]));
			if (img.getType() != BufferedImage.TYPE_INT_RGB &&
				img.getType() != BufferedImage.TYPE_INT_ARGB)
			{
				BufferedImage img2 = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
				img2.getGraphics().drawImage(img, 0, 0, null);
				img = img2;
			}
			Longan.getDefaultImplementation().visualize(img);
			ImageIO.write(img, "jpg", new File(args[2]));
		}
		if (args[0].equals("categorize")) {
			LetterTestDataCategoriser ltdc = new LetterTestDataCategoriser(new BetterLetterFinder(),
					new BetterChunker());
			ltdc.run(new File(args[1]), new File(args[2]));
		}
	}
}
