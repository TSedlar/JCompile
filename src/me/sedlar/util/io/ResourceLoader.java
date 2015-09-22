package me.sedlar.util.io;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tyler Sedlar
 * @since 11/1/14
 */
public class ResourceLoader {

	private final String dir;
	private final Map<String, Font> fontCache = new HashMap<>();

	public ResourceLoader(String dir) {
		if (!dir.endsWith("/"))
			dir += "/";
		this.dir = dir;
	}

	/**
	 * Gets the InputStream from the given path.
	 *
	 * @param path The path to get from.
	 * @return The InputStream from the given path.
	 */
	public InputStream get(String path) {
		return ResourceLoader.class.getResourceAsStream(dir + path);
	}

	/**
	 * Gets the raw content from the given path.
	 *
	 * @param path The path to get from.
	 * @return The raw content from the given path.
	 */
	public byte[] getBinary(String path) {
		return Streams.getBinary(get(path));
	}

	/**
	 * Gets the image from the given path.
	 *
	 * @param img The image to get.
	 * @return The image from the given path.
	 */
	public BufferedImage getImage(String img) {
		try (InputStream in = get(img)) {
			return ImageIO.read(in);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Gets the font from the given path.
	 *
	 * @param fontName The font to get.
	 * @return The font from the given path.
	 */
	public Font getFont(String fontName) {
		return getFont(fontName, Font.TRUETYPE_FONT, 12F);
	}

	/**
	 * Gets the font from the given path.
	 *
	 * @param fontName The font to get.
	 * @param fontType The type of font.
	 * @return The font from the given path.
	 */
	public Font getFont(String fontName, int fontType) {
		return getFont(fontName, fontType, 12F);
	}

	/**
	 * Gets the font from the given path.
	 *
	 * @param fontName The font to get.
	 * @param fontType The type of font.
	 * @param size The size of the font.
	 * @return The font from the given path.
	 */
	public Font getFont(String fontName, int fontType, float size) {
		if (!fontCache.containsKey(fontName)) {
			try (InputStream in = get(fontName)) {
				Font font = Font.createFont(fontType, in).deriveFont(size);
				fontCache.put(fontName, font);
			} catch (IOException | FontFormatException e) {
				return null;
			}
		}
		return fontCache.get(fontName);
	}
}