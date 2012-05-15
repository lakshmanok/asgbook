/**
 * 
 */
package edu.ou.asgbook.motion;

import java.io.File;
import java.util.Date;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.dataset.SeviriInfraredTemperature;
import edu.ou.asgbook.filters.SaturateFilter;
import edu.ou.asgbook.filters.SobelEdgeFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * 
 * Estimates motion based on the displacement of edges.
 * 
 * @author v.lakshmanan
 *
 */
public class EdgeBased implements MotionEstimator {
	private SobelEdgeFilter edgeFilter = new SobelEdgeFilter();
	private MotionEstimator hornSchunk = new HornSchunk();

	@Override
	public Pair<LatLonGrid, LatLonGrid> compute(LatLonGrid data0, LatLonGrid data1, File outdir){
		// do an edge filter on the pair of images
		LatLonGrid edge0 = edgeFilter.edgeFilter(data0);
		LatLonGrid edge1 = edgeFilter.edgeFilter(data1);
		
		if (outdir != null){
			try {
				KmlWriter.write(edge0, outdir, "edge0", PngWriter.createCoolToWarmColormap());
				KmlWriter.write(edge1, outdir, "edge1", PngWriter.createCoolToWarmColormap());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return hornSchunk.compute(edge0, edge1, outdir);
	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("edgemotion");
		
		// read
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		
		// do alg
		MotionEstimator alg = new EdgeBased();
		Pair<LatLonGrid,LatLonGrid> motion = alg.compute(grids[0].first, grids[1].first, out);
		
		// write
		SaturateFilter filter = new SaturateFilter(-150, 150);
		LatLonGrid u = filter.filter(motion.first);
		LatLonGrid v = filter.filter(motion.second);
		KmlWriter.write(u, out, "opticflow_u", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(v, out, "opticflow_v", PngWriter.createCoolToWarmColormap());
	}
}
