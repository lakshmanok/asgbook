/**
 * 
 */
package edu.ou.asgbook.io;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;

/**
 * Writes a spatial grid out as PNG file
 * 
 * @author Valliappa.Lakshmanan
 *
 */
public class PngWriter {	
	static final byte DEFAULT_TRANSPARENCY = (byte) 200; // 255 is opaque
	static public void writeAutoScaled(LatLonGrid grid, File outputFile, ColorModel colormap) throws Exception {
		// find min, max in data
		int[][] data = grid.getData();
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int numvalid = 0;
		for (int i=0; i < data.length; ++i){
			for (int j=0; j < data[0].length; ++j){
				if ( data[i][j] != grid.getMissing() ){
					++numvalid;
					if ( data[i][j] < min ){
						min = data[i][j];
					}
					if ( data[i][j] > max ){
						max = data[i][j];
					}
				}
			}
		}
		System.out.println("Autoscaling " + numvalid + " valid pixels between " + min + " and " + max);
		writeScaled(grid, outputFile, min, max, colormap);
	}
	
	/**
	 * black-to-white colormap
	 * 
	 */
	static public IndexColorModel createGrayScaleColormap(){
		byte[] red = new byte[256];
		byte[] alpha = new byte[red.length];
		for (int i=0; i < red.length; ++i){
			red[i] = (byte) i;
		}
		alpha[0] = 0;
		for (int i=1; i < alpha.length; ++i){
			alpha[i] = DEFAULT_TRANSPARENCY;
		}
		IndexColorModel colormap = new IndexColorModel(16, red.length, red, red, red, alpha);
		return colormap;
	}
	
	/**
	 * a colormap that goes from blue to red through purple
	 */
	static public IndexColorModel createHotColormap(){
		byte[] red = new byte[256];
		byte[] green = new byte[red.length];
		byte[] blue = new byte[red.length];
		byte[] alpha = new byte[red.length];
		for (int i=0; i < red.length; ++i){
			red[i] = (byte) i;
			blue[i] = (byte)( 255 - red[i]);
			green[i] = (byte)( (red[i] + blue[i])/2 );
		}
		alpha[0] = 0;
		for (int i=1; i < alpha.length; ++i){
			alpha[i] = DEFAULT_TRANSPARENCY;
		}
		IndexColorModel colormap = new IndexColorModel(16, red.length, red, green, blue, alpha);
		return colormap;
	}
	
	static private void interpolate(byte[] red, byte[] blue, byte[] green, int start, int end, double r1, double g1, double b1, double r2, double g2, double b2){
		for (int i=start; i < end; ++i){
			double frac = (i - start) / ((double)(end-start));
			long r = Math.round(255*(r1 + frac*(r2-r1)));
			long g = Math.round(255*(g1 + frac*(g2-g1)));
			long b = Math.round(255*(b1 + frac*(b2-b1)));
			if ( r < 0 ) r = 0;
			if ( g < 0 ) g = 0;
			if ( b < 0 ) b = 0;
			if ( r > 255 ) r = 255;
			if ( g > 255 ) g = 255;
			if ( b > 255 ) b = 255;
			red[i] = (byte) r;
			blue[i] = (byte) b;
			green[i] = (byte) g;
			// System.out.println( i + " " + red[i] + " " + green[i] + " " + blue[i]);
		}
	}
	
	/**
	 * a colormap that goes from green to red through white.
	 * See Candidate2 in http://www.paraview.org/ParaView3/index.php/Default_Color_Map
	 * Adapted from the work of Cindy Brewer for use in ParaView
	 */
	static public IndexColorModel createCoolToWarmColormap(){
		byte[] red = new byte[256];
		byte[] green = new byte[red.length];
		byte[] blue = new byte[red.length];
		byte[] alpha = new byte[red.length];
		
		interpolate(red, green, blue, 0,  25, 0.0196078, 0.188235, 0.380392, 0.129412, 0.4, 0.67451);
		interpolate(red, green, blue, 25, 51, 0.129412, 0.4, 0.67451, 0.262745, 0.576471, 0.764706);
		interpolate(red, green, blue, 51, 76, 0.262745, 0.576471, 0.764706, 0.572549, 0.772549, 0.870588);
		interpolate(red, green, blue, 76, 102, 0.572549, 0.772549, 0.870588, 0.819608, 0.898039, 0.941176);
		interpolate(red, green, blue, 102, 127, 0.819608, 0.898039, 0.941176, 0.968627, 0.968627, 0.968627);
		interpolate(red, green, blue, 127, 153, 0.968627, 0.968627, 0.968627, 0.992157, 0.858824, 0.780392);
		interpolate(red, green, blue, 153, 178, 0.992157, 0.858824, 0.780392, 0.956863, 0.647059, 0.509804);
		interpolate(red, green, blue, 178, 204, 0.956863, 0.647059, 0.509804, 0.839216, 0.376471, 0.301961);
		interpolate(red, green, blue, 204, 229, 0.839216, 0.376471, 0.301961, 0.698039, 0.0941176, 0.168627);
		interpolate(red, green, blue, 229, 256, 0.698039, 0.0941176, 0.168627, 0.403922, 0, 0.121569);
		
		alpha[0] = 0;
		for (int i=1; i < alpha.length; ++i){
			alpha[i] = DEFAULT_TRANSPARENCY;
		}
		
		IndexColorModel colormap = new IndexColorModel(16, red.length, red, green, blue, alpha);
		return colormap;
	}
	
	/**
	 * Randomized colormap, useful for displaying object labels, for example where
	 * the datavalues themselves do not mean anything beyond being a means to distinguish
	 * between objects
	 */
	static public IndexColorModel createRandomColormap(){
		byte[] red = new byte[256];
		byte[] green = new byte[red.length];
		byte[] blue = new byte[red.length];
		byte[] alpha = new byte[red.length];
		
		Random rnd = new Random();
		
		// random colors for the three channels
		for (int i=0; i < red.length; ++i){
			red[i] = (byte) rnd.nextInt(255);
			green[i] = (byte) rnd.nextInt(255);
			blue[i] = (byte) rnd.nextInt(255);
		}
		
		// 0 is transparent; everything else is opaque
		alpha[0] = 0;
		for (int i=1; i < alpha.length; ++i){
			alpha[i] = DEFAULT_TRANSPARENCY;
		}
		
		IndexColorModel colormap = new IndexColorModel(16, red.length, red, green, blue, alpha);
		return colormap;
	}
	
	static public void writeScaled(LatLonGrid grid, File outputFile, int min, int max, ColorModel colormap) throws Exception {	
		// scale the data and lookup the color
		int[][] data = grid.getData();
		double scale = 255.0 / (max - min + 1); // first is for 'missing'
		BufferedImage result = new BufferedImage(grid.getNumLon(), grid.getNumLat(), BufferedImage.TYPE_INT_ARGB);
		for (int i=0; i < data.length; ++i){
			for (int j=0; j < data[0].length; ++j){
				int scaled = 0;
				if ( data[i][j] == grid.getMissing() ){
					scaled = 0;
				} else if ( data[i][j] < min ){
					// System.out.println(data[i][j] + " " + scaled);
					scaled = 0;
				} else if ( data[i][j] >= max ){
					scaled = 255;
				} else {
					scaled = (int) ( (data[i][j]-min) * scale + 1.5);
				}
				
				result.setRGB(j, i, colormap.getRGB(scaled));
			}
		}
	
		// write it out
		ImageIO.write(result, "png", outputFile);
		System.out.println("Wrote " + outputFile + " by scaling data between " + min + " and " + max);
	}
	
	public static void main(String[] args) throws Exception {
		LatLonGrid grid = new LatLonGrid(100, 200, -1, new LatLon(35,-97), 0.1, 0.1);
		for (int i=0; i < grid.getNumLat(); ++i){
			for (int j=0; j < grid.getNumLon(); ++j){
				grid.getData()[i][j] = i + j;
				if ( i%10 == 0 || j%20 == 0){
					grid.getData()[i][j] = grid.getMissing();
				}
			}
		}
		
		File outdir = OutputDirectory.getDefault("pngwriter");
		File out = new File(outdir.getAbsoluteFile() + "/autoscaled_cooltowarm.png");
		PngWriter.writeAutoScaled(grid, out, PngWriter.createCoolToWarmColormap());
		
		out = new File(outdir.getAbsoluteFile() + "/autoscaled_hot.png");
		PngWriter.writeAutoScaled(grid, out, PngWriter.createHotColormap());
		
		out = new File(outdir.getAbsoluteFile() + "/autoscaled_gray.png");
		PngWriter.writeAutoScaled(grid, out, PngWriter.createGrayScaleColormap());
		
		out = new File(outdir.getAbsoluteFile() + "/scaled_0_10.png");
		PngWriter.writeScaled(grid, out, 10, 100, PngWriter.createCoolToWarmColormap());
	}
}
