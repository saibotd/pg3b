
package pg3b.ui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class ImagePacker {
	static Pattern numberedImagePattern = Pattern.compile(".*?(\\d+)");

	private ArrayList<Image> images = new ArrayList();
	FileWriter writer;
	final File inputDir;
	private int uncompressedSize, compressedSize;
	private int alphaThreshold = 0;
	private boolean debug = false;
	int canvasWidth, canvasHeight;

	public ImagePacker (File inputDir, int maxWidth, int maxHeight, String prefix) throws IOException {
		this.inputDir = inputDir;

		processImage(inputDir);

		// File outputDir = new File(inputDir.getParent(), inputDir.getName() + "-packed");
		File outputDir = new File("resources");
		outputDir.mkdirs();

		Collections.sort(images, new Comparator<Image>() {
			public int compare (Image image1, Image image2) {
				return image1.getWidth() * image1.getHeight() - image2.getWidth() * image2.getHeight();
			}
		});

		writer = new FileWriter(new File(outputDir, prefix + ".pack"));
		try {
			int imageNumber = 1;
			while (!images.isEmpty()) {
				writer.write("\n" + prefix + imageNumber + ".png\n");
				int width = 1;
				int height = 1;
				outer: while (width <= maxWidth) {
					height = 1;
					while (height <= maxHeight) {
						if (process(null, new ArrayList(images), width, height)) {
							width = canvasWidth;
							height = canvasHeight;
							break outer;
						}
						if (height == maxHeight) break;
						height *= 2;
					}
					if (width == maxWidth) break;
					width *= 2;
				}
				BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				process(canvas, images, width, height);
				File outputFile = new File(outputDir, prefix + imageNumber + ".png");
				System.out.println("Writing " + canvas.getWidth() + "x" + canvas.getHeight() + ": " + outputFile);
				ImageIO.write(canvas, "png", outputFile);
				compressedSize += canvas.getWidth() * canvas.getHeight();
				imageNumber++;
			}
			if (writer != null)
				System.out.println("Pixels eliminated: " + (1 - compressedSize / (float)uncompressedSize) * 100 + "%");
		} finally {
			writer.close();
		}
	}

	private boolean process (BufferedImage canvas, ArrayList<Image> images, int width, int height) throws IOException {
		canvasWidth = canvasHeight = 0;
		Node root = new Node(0, 0, width, height);
		for (int i = images.size() - 1; i >= 0; i--) {
			Image image = images.get(i);
			Node node = root.insert(image, canvas);
			if (node == null) continue;
			images.remove(i);
			if (canvas != null) {
				Graphics g = canvas.getGraphics();
				g.drawImage(image, node.left, node.top, null);
				if (debug) {
					g.setColor(Color.black);
					g.drawRect(node.left, node.top, node.width, node.height);
				}
			}
		}
		return images.isEmpty();
	}

	private void processImage (File file) throws IOException {
		if (file.isDirectory()) {
			for (File f : file.listFiles())
				processImage(f);
			return;
		}
		if (!file.getName().endsWith(".png")) return;
		Image image = squeeze(file);
		if (image != null) images.add(image);
	}

	private Image squeeze (File file) throws IOException {
		BufferedImage source = ImageIO.read(file);
		uncompressedSize += source.getWidth() * source.getHeight();
		WritableRaster alphaRaster = source.getAlphaRaster();
		if (alphaRaster == null) return new Image(file, source, 0, 0, source.getWidth(), source.getHeight());
		final byte[] a = new byte[1];
		int top = 0;
		outer: for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				alphaRaster.getDataElements(x, y, a);
				int alpha = a[0];
				if (alpha < 0) alpha += 256;
				if (alpha > alphaThreshold) break outer;
			}
			top++;
		}
		int bottom = source.getHeight() - 1;
		outer: for (int y = source.getHeight(); --y >= top;) {
			for (int x = 0; x < source.getWidth(); x++) {
				alphaRaster.getDataElements(x, y, a);
				int alpha = a[0];
				if (alpha < 0) alpha += 256;
				if (alpha > alphaThreshold) break outer;
			}
			bottom--;
		}
		int left = 0;
		outer: for (int x = 0; x < source.getWidth(); x++) {
			for (int y = top; y <= bottom; y++) {
				alphaRaster.getDataElements(x, y, a);
				int alpha = a[0];
				if (alpha < 0) alpha += 256;
				if (alpha > alphaThreshold) break outer;
			}
			left++;
		}
		int right = source.getWidth() - 1;
		outer: for (int x = source.getWidth(); --x >= left;) {
			for (int y = top; y <= bottom; y++) {
				alphaRaster.getDataElements(x, y, a);
				int alpha = a[0];
				if (alpha < 0) alpha += 256;
				if (alpha > alphaThreshold) break outer;
			}
			right--;
		}
		int newWidth = 1 + right - left;
		int newHeight = 1 + bottom - top;
		if (newWidth <= 0 || newHeight <= 0) {
			System.out.println("Ignoring blank input image: " + file.getAbsolutePath());
			return null;
		}
		return new Image(file, source, left, top, newWidth, newHeight);
	}

	private class Node {
		final int left, top, width, height;
		Node child1, child2;
		Image image;

		public Node (int left, int top, int width, int height) {
			this.left = left;
			this.top = top;
			this.width = width;
			this.height = height;
		}

		public Node insert (Image image, BufferedImage canvas) throws IOException {
			if (this.image != null) return null;
			if (child1 != null) {
				Node newNode = child1.insert(image, canvas);
				if (newNode != null) return newNode;
				return child2.insert(image, canvas);
			}
			int imageWidth = image.getWidth() + 1;
			int imageHeight = image.getHeight() + 1;
			if (imageWidth > width || imageHeight > height) return null;
			if (imageWidth == width && imageHeight == height) {
				this.image = image;
				canvasWidth = Math.max(canvasWidth, left + width);
				canvasHeight = Math.max(canvasHeight, top + height);
				write(canvas);
				return this;
			}
			int dw = width - imageWidth;
			int dh = height - imageHeight;
			if (dw > dh) {
				child1 = new Node(left, top, imageWidth, height);
				child2 = new Node(left + imageWidth, top, width - imageWidth, height);
			} else {
				child1 = new Node(left, top, width, imageHeight);
				child2 = new Node(left, top + imageHeight, width, height - imageHeight);
			}
			return child1.insert(image, canvas);
		}

		private void write (BufferedImage canvas) throws IOException {
			if (canvas == null) return;

			String imageName = image.file.getAbsolutePath().substring(inputDir.getAbsolutePath().length()) + "\n";
			if (imageName.startsWith("/") || imageName.startsWith("\\")) imageName = imageName.substring(1);
			imageName = withoutExtension(imageName);

			writer.write(imageName.replace("\\", "/").replaceAll(".*_", "") + "\n");
			writer.write(left + "\n");
			writer.write(top + "\n");
			writer.write(left + width + "\n");
			writer.write(top + height + "\n");
			writer.write(image.offsetX + "\n");
			writer.write(image.offsetY + "\n");

			Matcher matcher = numberedImagePattern.matcher(imageName);
			if (matcher.matches())
				writer.write(Integer.parseInt(matcher.group(1)) + "\n");
			else
				writer.write("\n");
		}
	}

	static public String withoutExtension (String fileName) {
		if (fileName == null) throw new IllegalArgumentException("fileName cannot be null.");
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) return fileName;
		return fileName.substring(0, dotIndex);
	}

	static private class Image extends BufferedImage {
		final File file;
		final int offsetX, offsetY;

		public Image (File file, BufferedImage src, int left, int top, int newWidth, int newHeight) {
			super(src.getColorModel(), src.getRaster().createWritableChild(left, top, newWidth, newHeight, 0, 0, null), src
				.getColorModel().isAlphaPremultiplied(), null);
			this.file = file;
			this.offsetX = left;
			this.offsetY = top;
		}

		public String toString () {
			return file.toString();
		}
	}

	public static void main (String[] args) throws IOException {
		// new ImagePacker(new File(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
		new ImagePacker(new File("misc/raw"), 1024, 1024, "controller");
	}
}
