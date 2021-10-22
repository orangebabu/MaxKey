package com.google.code.kaptcha.text.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;
import com.google.code.kaptcha.text.WordRenderer;
import com.google.code.kaptcha.util.ConfigHelper;
import com.google.code.kaptcha.util.Configurable;

/**
 * The default implementation of {@link WordRenderer}, creates an image with a
 * word rendered on it.
 */
public class RandomColorWordRenderer extends Configurable implements WordRenderer
{
	ConfigHelper configHelper = new ConfigHelper();
	/**
	 * Renders a word to an image.
	 * 
	 * @param word
	 *            The word to be rendered.
	 * @param width
	 *            The width of the image to be created.
	 * @param height
	 *            The height of the image to be created.
	 * @return The BufferedImage created from the word.
	 */
	public BufferedImage renderWord(String word, int width, int height)
	{
		int fontSize = getConfig().getTextProducerFontSize();
		Font[] fonts = getConfig().getTextProducerFonts(fontSize);
		Color color = getConfig().getTextProducerFontColor();
		int charSpace = getConfig().getTextProducerCharSpace();
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2D = image.createGraphics();
		g2D.setColor(color);

		RenderingHints hints = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY));
		g2D.setRenderingHints(hints);

		FontRenderContext frc = g2D.getFontRenderContext();
		Random random = new SecureRandom();

		int startPosY = (height - fontSize) / 5 + fontSize;

		char[] wordChars = word.toCharArray();
		Font[] chosenFonts = new Font[wordChars.length];
		int [] charWidths = new int[wordChars.length];
		int widthNeeded = 0;
		for (int i = 0; i < wordChars.length; i++)
		{
			
			//chosenFonts[i] = new Font("Arial", Font.BOLD, fontSize);
			//random
			chosenFonts[i] = fonts[random.nextInt(fonts.length)];

			char[] charToDraw = new char[]{
				wordChars[i]
			};
			GlyphVector gv = chosenFonts[i].createGlyphVector(frc, charToDraw);
			charWidths[i] = (int)gv.getVisualBounds().getWidth();
			if (i > 0)
			{
				widthNeeded = widthNeeded + 2;
			}
			widthNeeded = widthNeeded + charWidths[i];
		}
		
		HashMap<String,String> selectedColor =new HashMap<String,String>();
		int startPosX = (width - widthNeeded) / 2;
		
		for (int i = 0; i < wordChars.length; i++)
		{
			String randomcolor="";
			do {
				randomcolor=COLOR_LIST[random.nextInt(COLOR_LIST.length)].replaceAll(" ", "");
			}while(selectedColor.containsKey(randomcolor));
			
			selectedColor.put(randomcolor, randomcolor);
			
			color = configHelper.getColor(randomcolor, randomcolor, Color.LIGHT_GRAY);
			g2D.setColor(color);
			
			g2D.setFont(chosenFonts[i]);
			
			char[] charToDraw = new char[] {
				wordChars[i]
			};
			
			//System.out.println(charToDraw[0] +" - "+chosenFonts[i]);
			g2D.drawChars(charToDraw, 0, charToDraw.length, startPosX, startPosY);
			startPosX = startPosX + (int) charWidths[i] + charSpace;
		}
		
		return image;
	}
	
	static String [] COLOR_LIST = {
			//"255, 255, 255",//white
			//"192, 192, 192",//silver
			//"128, 128, 128",//gray
			"0, 0, 0",//black
			"255, 0, 0",//red
			"128, 0, 0",//maroon
			"255, 255, 0",//yellow
			"128, 128, 0",//olive
			"0, 255, 0",//lime
			"0, 128, 0",//green
			"0, 255, 255",//aqua
			"0, 128, 128",//teal
			"0, 0, 255",//blue
			"0, 0, 128",//navy
			"255, 0, 255",//fuchsia
			"128, 0, 128"//purple
	};
}
