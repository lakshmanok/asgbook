/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLon;
import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.core.ScalarStatistic;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.geocode.UsaZipcode;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * 
 * Properties of a region such as geometric (centroid, area, etc) and
 * physical (based on other grid values).
 * 
 * @author v.lakshmanan
 *
 */
public class RegionProperty {
	private ScalarStatistic xstat = new ScalarStatistic();
	private ScalarStatistic ystat = new ScalarStatistic();
	private ScalarStatistic valstat = new ScalarStatistic();
	private ScalarStatistic xystat = new ScalarStatistic();
	private void update(int x, int y, int val){
		xstat.update(x);
		ystat.update(y);
		valstat.update(val);
		xystat.update(x*y);
	}
	
	public double getCx() {
		return xstat.getMean();
	}
	public double getCy() {
		return ystat.getMean();
	}
	public double getCval() {
		return valstat.getMean();
	}
	public int getSize() {
		return xstat.getNumSamples();
	}
	
	public static class Ellipse {
		public final double a, b, cx, cy, phi;
		private final double sinphi, cosphi;
		  
		public Ellipse(double cx, double cy, double a, double b, double phi) {
			this.a = a;
			this.b = b;
			this.cx = cx;
			this.cy = cy;
			this.phi = phi;
			this.sinphi = Math.sin(Math.toRadians(phi));
			this.cosphi = Math.cos(Math.toRadians(phi));
		}
		public double getAspectRatio(){
			if (a != 0) return b/a;
			else return 1;
		}
		public boolean contains(double x, double y) {
			double ox = x - cx;
			double oy = y - cy;
			double rotx = ox * cosphi + oy * sinphi;
			double roty = -ox * sinphi + oy * cosphi;
			double dist_x = rotx / a;
			double dist_y = roty / b;
			double dist = dist_x * dist_x + dist_y * dist_y;
			return (dist < 1);
		}
		@Override
		public String toString(){
			return "EllipseFit: a=" + a + " b=" + b + " cx=" + cx + " cy=" + cy + " phi=" + phi;
		}
	}
	
	public Ellipse getEllipseFit() {
		final double cx = xstat.getMean();
		final double cy = ystat.getMean();
		final double s11 = xstat.getVariance();
		final double s22 = ystat.getVariance();
		final double s12 = xystat.getMean() - cx*cy;
		double tmp = (s11 - s22) * (s11 - s22) + 4 * s12 * s12;
		if (tmp >= 0.00001) {
			tmp = Math.sqrt(tmp);
		} else {
			tmp = 0;
		}
		double eigen1 = (s11 + s22 + tmp) / 2;
		double eigen2 = (s11 + s22 - tmp) / 2;
		
		double v1 = s12
				/ Math.sqrt((eigen1 - s11) * (eigen1 - s11) + s12 * s12);
		double v2 = (eigen1 - s11)
				/ Math.sqrt((eigen1 - s11) * (eigen1 - s11) + s12 * s12);

		double a = 2 * Math.sqrt(eigen1);
		double b = 2 * Math.sqrt(eigen2);
		double phi = Math.toDegrees(Math.atan2(v2, v1));
		return new Ellipse(cx, cy, a, b, phi);

	}
	
	public static RegionProperty[] compute(LabelResult label, LatLonGrid data){
		RegionProperty[] props = new RegionProperty[label.maxlabel+1];
		for (int i=1; i < props.length; ++i){
			props[i] = new RegionProperty();
		}
		int nrows = label.label.getNumLat();
		int ncols = label.label.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			if (label.label.getValue(i,j) > 0){
				props[ label.label.getValue(i,j) ].update(i, j, data.getValue(i,j));
			}
		}
		return props;
	}
	
	/**
	 * All the pixels for each region. The array is organized as
	 * pixels[regno][pixelno]
	 * @param data1
	 * @param objects1
	 * @return
	 */
	public static Pixel[][] getPixelsInRegions(LatLonGrid data1, LabelResult objects1) {
		@SuppressWarnings("unchecked")
		List<Pixel>[] regions = new List[objects1.maxlabel+1];
		for (int reg=1; reg < regions.length; ++reg){
			regions[reg] = new ArrayList<Pixel>();
		}
		for (int i=0; i < data1.getNumLat(); ++i) for (int j=0; j < data1.getNumLon(); ++j){
			int reg = objects1.label.getValue(i,j);
			if (reg > 0){
				regions[reg].add(new Pixel(i,j,data1.getValue(i,j)));
			}
		}
		Pixel[][] result = new Pixel[regions.length][];
		for (int i=1; i < result.length; ++i){
			result[i] = regions[i].toArray(new Pixel[0]);
		}
		return result;
	}

	/**
	 * Regions for which keep=false are removed.
	 */
	public static LabelResult prune(LabelResult input, boolean[] keep){
		// find mapping
		int[] newRegionNo = new int[keep.length]; // init to zero
		int numRegions = 0;
		for (int i=1; i < keep.length; ++i){
			if ( keep[i] ){
				++numRegions;
				newRegionNo[i] = numRegions;
			}
		}
		
		// replace old label by new label
		LatLonGrid newLabel = LatLonGrid.copyOf(input.label);
		int nrows = newLabel.getNumLat();
		int ncols = newLabel.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int oldno = input.label.getValue(i,j);
			newLabel.setValue(i,j, newRegionNo[oldno]);
		}
		return new LabelResult(newLabel, numRegions);
	}
	
	/**
	 * Regions smaller than sizethresh are removed.
	 */
	public static LabelResult pruneBySize(LabelResult input, LatLonGrid grid, int sizethresh){
		RegionProperty[] prop = RegionProperty.compute(input, grid);
		
		// find mapping
		int[] newRegionNo = new int[prop.length]; // init to zero
		int numRegions = 0;
		for (int i=1; i < prop.length; ++i){
			if ( prop[i].getSize() >= sizethresh ){
				++numRegions;
				newRegionNo[i] = numRegions;
			}
		}
		
		// replace old label by new label
		LatLonGrid newLabel = LatLonGrid.copyOf(input.label);
		int nrows = newLabel.getNumLat();
		int ncols = newLabel.getNumLon();
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			int oldno = input.label.getValue(i,j);
			newLabel.setValue(i,j, newRegionNo[oldno]);
		}
		return new LabelResult(newLabel, numRegions);
	}
	
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("regionproperty");
		
		// data
		LatLonGrid grid = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LinearScaling()).crop(1000, 2100, 100, 200);
		KmlWriter.write(grid, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// global thresh
		int thresh = 20;
		ThresholdSegmenter seg = new ThresholdSegmenter(thresh);
		LabelResult labelResult = seg.label(grid);
		labelResult.label.setMissing(-1); // to get background color
		KmlWriter.write(labelResult.label, out, "allcities_"+thresh, PngWriter.createRandomColormap());
		
		// prune cities less than 5 pixels in size
		for (int sizethresh = 2; sizethresh <= 5; ++sizethresh){
			LabelResult pruned = pruneBySize(labelResult, grid, sizethresh);
			pruned.label.setMissing(-1); // to get background color
			KmlWriter.write(pruned.label, out, "sizepruned_"+sizethresh, PngWriter.createRandomColormap());
			// get geocode
			RegionProperty[] prop = RegionProperty.compute(pruned, grid);
			for (int i=1; i < prop.length; ++i){
				LatLon loc = grid.getLocation(prop[i].getCx(), prop[i].getCy());
				UsaZipcode.Entry entry = UsaZipcode.getInstance().getEntryClosestTo(loc);
				System.out.println(entry + " " + prop[i].getEllipseFit());
			}
			
			// there are more efficient ways to paint an ellipse, but this will do
			Ellipse[] ellipses = new Ellipse[prop.length];
			for (int i=1; i < prop.length; ++i){
				ellipses[i] = prop[i].getEllipseFit();
			}
			LatLonGrid ellipse = LatLonGrid.copyOf(pruned.label);
			ellipse.fill(0);
			int nrows = ellipse.getNumLat();
			int ncols = ellipse.getNumLon();
			for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
				for (int k=1; k < prop.length; ++k){
					if (ellipses[k].contains(i, j)){
						ellipse.setValue(i, j, k); // paint pixel
					}
				}
			}
			KmlWriter.write(ellipse, out, "ellipse_"+sizethresh, PngWriter.createRandomColormap());
		}
	}
}
