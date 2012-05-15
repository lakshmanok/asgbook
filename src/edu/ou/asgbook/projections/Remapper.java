/**
 * 
 */
package edu.ou.asgbook.projections;

/**
 * 
 * Utilities to remap one map projection to another.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class Remapper {
	public static int nearestNeighbor(double rowno, double colno, int[][] input, int missing){
		final int row = (int) Math.round( rowno );
		final int col = (int) Math.round( colno );
		final int nrows = input.length;
		final int ncols = (nrows > 0)? input[0].length : 0;
		if ( row >= 0 && col >= 0 && row < nrows && col < ncols ){
			return input[row][col];
		} else {
			return missing;
		}
	}
	
	public static int bilinearInterpolation(double rowno, double colno, int[][] input, int missing){
		final int row0 = (int) Math.floor( rowno );
		final int col0 = (int) Math.floor( colno );
		final int row1 = (int) Math.ceil( rowno );
		final int col1 = (int) Math.ceil( colno );
		final int nrows = input.length;
		final int ncols = (nrows > 0)? input[0].length : 0;
		
		int npts = 0;
		double totwt = 0;
		double totval = 0;
		for (int row = row0; row <= row1; ++row){
			for (int col = col0; col <= col1; ++col){
				if ( row >= 0 && col >= 0 && row < nrows && col < ncols && input[row][col] != missing ){
					double rowwt = 1 - Math.abs(rowno-row);
					double colwt = 1 - Math.abs(colno-col);
					double wt = rowwt * colwt;
					npts++;
					totwt += wt;
					totval += wt * input[row][col];
				}
			}
		}
		
		// weighted average
		if (npts == 0){
			return missing;
		} else {
			return (int) Math.round(totval / totwt);
		}
	}
}
