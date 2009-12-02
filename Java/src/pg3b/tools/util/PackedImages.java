
package pg3b.ui.util;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Comparator;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;

public class PackedImages {
	public final PackedImage[] images;

	public PackedImages (String packPath) throws IOException {
		PriorityQueue<PackedImage> sortedImages = new PriorityQueue(16, indexComparator);

		String imagePath = new File(packPath).getParent();
		imagePath = imagePath == null ? "" : imagePath.replace('\\', '/');

		InputStream input = getClass().getResourceAsStream(packPath);
		if (input == null) throw new IOException("Pack file not found: " + packPath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input), 64);
		try {
			BufferedImage pageImage = null;
			while (true) {
				String line = reader.readLine();
				if (line == null) break;
				if (line.trim().length() == 0)
					pageImage = null;
				else if (pageImage == null) {
					URL url = getClass().getResource(imagePath + line);
					if (url == null) throw new IOException("Packed image not found: " + imagePath + line);
					pageImage = ImageIO.read(url);
				} else {
					int left = Integer.parseInt(reader.readLine());
					int top = Integer.parseInt(reader.readLine());
					int right = Integer.parseInt(reader.readLine());
					int bottom = Integer.parseInt(reader.readLine());
					int offsetX = Integer.parseInt(reader.readLine());
					int offsetY = Integer.parseInt(reader.readLine());
					PackedImage image = new PackedImage(pageImage.getSubimage(left, top, right - left, bottom - top), offsetX, offsetY);
					image.name = line;
					line = reader.readLine();
					image.index = line.length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(line);
					sortedImages.add(image);
				}
			}
		} catch (IOException ex) {
			throw new IOException("Error reading pack file: " + packPath, ex);
		} finally {
			try {
				reader.close();
			} catch (IOException ignored) {
			}
		}

		int n = sortedImages.size();
		images = new PackedImage[n];
		for (int i = 0; i < n; i++)
			images[i] = sortedImages.poll();
	}

	public PackedImage get (String name) {
		for (int i = 0, n = images.length; i < n; i++)
			if (images[i].name.equals(name)) return images[i];
		return null;
	}

	static private final Comparator<PackedImage> indexComparator = new Comparator<PackedImage>() {
		public int compare (PackedImage image1, PackedImage image2) {
			return image1.index - image2.index;
		}
	};

	static public class PackedImage {
		public int index;
		public String name;
		public final BufferedImage image;
		public final int offsetX;
		public final int offsetY;

		PackedImage (BufferedImage image, int offsetX, int offsetY) {
			this.image = image;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}

		public void draw (Graphics g, int x, int y) {
			g.drawImage(image, x + offsetX, y + offsetY, null);
		}
	}
}
