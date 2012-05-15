/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.util.ArrayList;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pixel;

/**
 * Common object-identification utility.
 * 
 * @author v.lakshmanan
 *
 */
public class RegionGrowing {
	public static void growRegion(int x, int y, LatLonGrid data, int thresh, LatLonGrid label, int currLabel){
		final int junk = 0; // data value not needed for region growing
		final int UNSET = 0;
		List<Pixel> stack = new ArrayList<Pixel>();
		stack.add(new Pixel(x,y,junk));
		while (stack.size() > 0){
			Pixel p = stack.remove(stack.size()-1);
			label.setValue(p.getX(), p.getY(), currLabel);
			for (int i=p.getX()-1; i <= p.getX()+1; ++i){
				for (int j=p.getY()-1; j <= p.getY()+1; ++j){
					if (data.isValid(i, j) && data.getValue(i,j) > thresh && label.getValue(i,j) == UNSET){
						stack.add(new Pixel(i,j,junk));
					}
				}
			}
		}
	}
}
