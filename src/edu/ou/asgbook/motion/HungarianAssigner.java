/**
 * 
 */
package edu.ou.asgbook.motion;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.core.Pair;
import edu.ou.asgbook.core.Pixel;
import edu.ou.asgbook.dataset.SeviriInfraredTemperature;
import edu.ou.asgbook.filters.MedianFilter;
import edu.ou.asgbook.filters.SaturateFilter;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;
import edu.ou.asgbook.segmentation.Segmenter;

/**
 * Optimal assignment algorithm.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class HungarianAssigner implements ObjectTracker.Assigner {

	@Override
	public int[] getAssignments(int[][] cost, int maxcost) {
		// intialize result to be all unassigned
		int[] result = new int[cost.length];
		for (int i=0; i < result.length; ++i){
			result[i] = -1;
		}
		
		// if number of objects is zero, then can't do any assignment
		if (cost.length == 0 || cost[0].length == 0){
			return result;
		}
		
		if (cost[0].length < cost.length){
			// rotate so that we have more columns than rows
			int[][] rot = new int[ cost[0].length ][ cost.length ];
			for (int i=0; i < cost.length; ++i){
				for (int j=0; j < cost[i].length; ++j){
					rot[j][i] = cost[i][j];
				}
			}
			// do the assignment process on rotated cost function
			int[] col_to_row = getAssignments(rot, maxcost);
			// fix result: we need row_to_col
			for (int col = 0; col < col_to_row.length; ++col){
				int row = col_to_row[col];
				if (row >= 0){
					result[row] = col;
				}
			}
			return result;
		}

		// threshold just in case some cost > maxcost
		for (int i=0; i < cost.length; ++i){
			for (int j=0; j < cost[i].length; ++j){
				if (cost[i][j] > maxcost){
					cost[i][j] = maxcost;
				}
			}
		}
		HungarianMatch match = new HungarianMatch(cost);
		match.do_step1();
		match.do_step2();
		match.do_step3();
		for (int i=0; i < cost.length; ++i){
			for (int j=0; j < cost[i].length; ++j){
				if (match.starred_zero[i][j] && cost[i][j] < maxcost ){
					result[i] = j;
				}
			}
		}
		return result;
	}

	private static class HungarianMatch {
		private int[][] cost;
		private final int nrows, ncols;
		private boolean[] covered_cols, covered_rows;
		private boolean[][] starred_zero, primed_zero;

		HungarianMatch(int[][] cost){
			this.nrows = cost.length;
			this.ncols = cost[0].length;
			this.cost = new int[nrows][ncols];
			for (int i=0; i < nrows; ++i){
				for (int j=0; j < ncols; ++j){
					this.cost[i][j] = cost[i][j];
				}
			}
			this.covered_cols = new boolean[ ncols ];
			this.covered_rows = new boolean[ nrows ];
			this.starred_zero = new boolean[ nrows ][ ncols ];
			this.primed_zero = new boolean[ nrows ][ ncols ];
		}

		private void do_step1(){
			// Step 1: For each row of the matrix, find the smallest element and subtract it from every element in its row
			for (int i=0; i < nrows; ++i){
				int minval = cost[i][0]; // ok: more cols than rows
				for (int j=1; j < ncols; ++j){
					if ( cost[i][j] < minval ) minval = cost[i][j];
				}
				for (int j=0; j < ncols; ++j){
					cost[i][j] -= minval;
				}
			}
		}

		void do_step2(){
			// Step 2: Find a zero (Z) in the resulting matrix.  If there is no
			// starred zero in its row or column, star Z.
			// Repeat for each element in the matrix.
			for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
				if ( cost[i][j] == 0 && !covered_rows[i] && !covered_cols[j] ){
					starred_zero[i][j] = true;
					covered_rows[i] = true;
					covered_cols[j] = true;
				}
			}

			// unset covered_rows, covered_cols as we shouldn't have used it!
			for (int i=0; i < nrows; ++i)
				covered_rows[i] = false;
			for (int j=0; j < ncols; ++j)
				covered_cols[j] = false;
		}

		void do_step3(){
			// Step 3: Cover each column containing a starred zero.
			// If num_rows columns are covered, the starred zeros
			// describe a complete set of unique assignments.  In this case, we're done.
			for (int j=0; j < ncols; ++j){
				covered_cols[j] = col_has(starred_zero,j).first;
			}
			// what is the next step?
			if ( count_of(covered_cols) >= nrows ) return; // DONE
			else do_step4();
		}

		void do_step4(){
			// Step 4: Find a noncovered zero and prime it.  If there is no
			// starred zero in the row containing this primed zero, Go to Step 5.
			// Otherwise, cover this row and uncover the column containing the
			// starred zero. Continue in this manner until there are no
			// uncovered zeros left. Save the smallest uncovered value and Go to Step 6.
			while (true ){
				int num_uncovered_zero = 0;
				for (int j=0; j < ncols; ++j){
					if ( !covered_cols[j] ){ // uncovered column
						for (int i=0; i < nrows; ++i){
							if ( !covered_rows[i] && cost[i][j] == 0 ){ // uncovered zero
								primed_zero[i][j] = true;
								++ num_uncovered_zero;
								Pair<Boolean,Integer> has_starred_zero = row_has(starred_zero,i);
								int col_with_starred_zero = has_starred_zero.second;
								if ( !has_starred_zero.first ){
									do_step5(i,j);
									return;
								} else {
									covered_rows[i] = true;
									covered_cols[col_with_starred_zero] = false;
								}
							}
						}
					}
				}
				if ( num_uncovered_zero == 0 ){
					do_step6(); // will find smallest uncovered value in step 6
					// will come back here and continue ...
				}
			} // until there are no uncovered zeros
		}

		void do_step5(int sx, int sy){
			// Step 5: Construct a series of alternating primed and starred zeros
			// as follows.  Let Z0 represent the uncovered primed zero found in Step 4.
			// Let Z1 denote the starred zero in the column of Z0 (if any). Let Z2
			// denote the primed zero in the row of Z1 (there will always be one).
			// Continue until the series terminates at a primed zero that has no starred
			// zero in its column.  Unstar each starred zero of the series, star each
			// primed zero of the series, erase all primes and uncover every line
			// in the matrix.  Return to Step 3.
			List<Pixel> primed = new ArrayList<Pixel>();
			List<Pixel> starred = new ArrayList<Pixel>();
			Pixel zp = new Pixel( sx, sy, 0 );
			primed.add( zp ); 

			while (true){
				// find starred zero in the column of Z0
				int z0col = primed.get(primed.size()-1).getY();
				boolean z1found = false;
				for (int i=0; i < nrows; ++i){
					if ( starred_zero[i][z0col] ){
						Pixel z1 = new Pixel(i, z0col, 0);
						starred.add( z1 );
						for (int j=0; j < ncols; ++j){
							if ( primed_zero[i][j] ){
								Pixel z2 = new Pixel(i,j,0);
								primed.add( z2 );
								break;
							}
						}
						z1found = true;
						break;
					}
				}
				if ( !z1found ) break;
			}

			// unstar starred zeroes of series
			for (int k=0; k < starred.size(); ++k){
				starred_zero[ starred.get(k).getX() ][ starred.get(k).getY() ] = false;
			}
			// star each primed zero of series
			for (int k=0; k < primed.size(); ++k){
				starred_zero[ primed.get(k).getX() ][ primed.get(k).getY() ] = true;
			}
			// erase all primes
			for (int i=0; i < primed_zero.length; ++i){
				for (int j=0; j < primed_zero[i].length; ++j){
					primed_zero[i][j] = false;
				}
			}
			// uncover all lines
			for (int i=0; i < covered_rows.length; ++i){
				covered_rows[i] = false;
			}
			for (int i=0; i < covered_cols.length; ++i){
				covered_cols[i] = false;
			}

			do_step3();
		}

		void do_step6(){
			// Step 6: 
			// Find the smallest uncovered value in the matrix
			// Add this value to every element of each covered row,
			// and subtract it from every element of each uncovered column.
			// Return to Step 4 without altering any stars, primes, or covered lines.
			
			int smallest_uncovered_value = Integer.MAX_VALUE;
			for (int i=0; i < cost.length; ++i){
				for (int j=0; j < cost[0].length && !covered_rows[i]; ++j){ // XXX: C++
					if ( !covered_cols[j] ){
						if ( cost[i][j] < smallest_uncovered_value ){
							smallest_uncovered_value = cost[i][j];
						}
					}
				}
			} 

			// do the correction
			for (int i=0; i < nrows; ++i){
				if ( covered_rows[i] ){
					for (int j=0; j < ncols; ++j){
						cost[i][j] += smallest_uncovered_value;
					}
				}
			}
			for (int j=0; j < ncols; ++j){
				if ( !covered_cols[j] ){
					for (int i=0; i < nrows; ++i){
						cost[i][j] -= smallest_uncovered_value;
					}
				}
			}
		}


		private Pair<Boolean,Integer> row_has(boolean[][] img, int i){
			int col = 0;
			for (int j=0; j < ncols; ++j){
				if ( img[i][j] ){
					col = j;
					return new Pair<Boolean,Integer>(true,col);
				}
			}
			return new Pair<Boolean,Integer>(false,col);
		}

		private Pair<Boolean,Integer> col_has(boolean[][] img, int j){
			int row = 0;
			for (int i=0; i < nrows; ++i){
				if ( img[i][j] ){
					row = i;
					return new Pair<Boolean,Integer>(true,row);
				}
			}
			return new Pair<Boolean,Integer>(true,row);
		}

		private int count_of(boolean[] flags){
			int count = 0;
			for (int i=0; i < flags.length; ++i){
				if ( flags[i] ) ++count;
			}
			return count;
		}

	}
	
	public static void main(String[] args) throws Exception {
		// create output directory
		File out = OutputDirectory.getDefault("hungarian");
		
		// read
		File f = new File("data/seviri");
		Pair<LatLonGrid,Date>[] grids = SeviriInfraredTemperature.readAll(f);
		
		// do alg
		Segmenter seg = new ObjectTracker.SimpleSegmenter(100, 110, 1000);
		ObjectTracker alg = new ObjectTracker(seg, new ObjectTracker.CentroidDistance(), new HungarianAssigner());
		MedianFilter smoother = new MedianFilter(10);
		LatLonGrid grid0 = smoother.filter(grids[0].first);
		LatLonGrid grid1 = smoother.filter(grids[1].first);
		Pair<LatLonGrid,LatLonGrid> motion = alg.compute(grid0, grid1, out);
		
		// write
		SaturateFilter filter = new SaturateFilter(-150, 150);
		LatLonGrid u = filter.filter(motion.first);
		LatLonGrid v = filter.filter(motion.second);
		KmlWriter.write(u, out, "hungarian_u", PngWriter.createCoolToWarmColormap());
		KmlWriter.write(v, out, "hungarian_v", PngWriter.createCoolToWarmColormap());
	}
}
