package edu.ou.asgbook.transforms;

import java.text.DecimalFormat;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.transforms.FFT.Complex;

/**
 * Two-dimensional FFT.
 * 
 * @author valliappa.lakshmanan
 *
 */
public class FFT2D {

	public static Complex[][] ifft(Complex[][] input){
		// compute ifft row-wise, then column-wise
		int nrows = input.length;
		int ncols = input[0].length;
		Complex[][] rowwise = new Complex[nrows][];
		for (int i=0; i < nrows; ++i){
			rowwise[i] = FFT.ifft(input[i]);
		}
		Complex[][] result = new Complex[nrows][ncols];
		Complex[] tmp = new Complex[nrows];
		for (int j=0; j < ncols; ++j){
			for (int i=0; i < nrows; ++i){
				tmp[i] = rowwise[i][j];
			}
			Complex[] tmp2 = FFT.ifft(tmp);
			for (int i=0; i < nrows; ++i){
				result[i][j] = tmp2[i];
			}
		}
		return result;
	}
	
	public static Complex[][] zeropad(LatLonGrid data){
		int nrows = getNextPowerOf2(data.getNumLat());
		int ncols = getNextPowerOf2(data.getNumLon());
		Complex[][] result = new Complex[nrows][ncols];
		Complex ZERO = new Complex(0,0);
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			result[i][j] = ZERO;
		}
		for (int i=0; i < data.getNumLat(); ++i){
			for (int j=0; j < data.getNumLon(); ++j){
				result[i][j] = new Complex(data.getValue(i,j), 0);
			}
		}
		return result;
	}
	
	public static Complex[][] zeropad(double[][] data){
		int nrows = getNextPowerOf2(data.length);
		int ncols = getNextPowerOf2(data[0].length);
		return zeropad(data, nrows, ncols);
	}

	public static Complex[][] zeropad(double[][] data, int nrows, int ncols) {
		Complex[][] result = new Complex[nrows][ncols];
		Complex ZERO = new Complex(0,0);
		for (int i=0; i < nrows; ++i) for (int j=0; j < ncols; ++j){
			result[i][j] = ZERO;
		}
		for (int i=0; i < data.length; ++i){
			for (int j=0; j < data[0].length; ++j){
				result[i][j] = new Complex(data[i][j], 0);
			}
		}
		return result;
	}
	
	public static Complex[][] fft(Complex[][] data){
		int nrows = data.length;
		int ncols = data[0].length;
		
		// compute fft row-by-row
		Complex[][] rowwise = new Complex[nrows][];
		for (int i=0; i < nrows; ++i){
			rowwise[i] = FFT.fft(data[i]);
		}
		
		// on the result, compute fft column by column
		Complex[][] result = new Complex[nrows][ncols];
		{
			Complex[] tmp = new Complex[nrows];
			for (int j=0; j < ncols; ++j){
				for (int i=0; i < nrows; ++i){
					tmp[i] = rowwise[i][j];
				}
				Complex[] tmp2 = FFT.fft(tmp);
				for (int i=0; i < nrows; ++i){
					result[i][j] = tmp2[i];
				}
			}
		}
		
		return result;
	}

	private static int getNextPowerOf2(int n) {
		return (int) Math.round( Math.pow(2, Math.ceil(Math.log(n) / Math.log(2))) );
	}
	
	public static void main(String[] args) throws Exception {
		DecimalFormat df = new DecimalFormat("0.0");
		// FFT( rect ) should be a sinc function
		FFT.Complex[][] input = new FFT.Complex[8][8];
		for (int i=0; i < input.length; ++i){
			for (int j=0; j < input[i].length; ++j){
				input[i][j] = new FFT.Complex(0,0);
			}
		}
		for (int i=input.length/3; i < 2*input.length/3; ++i){
			for (int j=input[i].length/3; j < 2*input[i].length/3; ++j){
				input[i][j] = new FFT.Complex(1, 0);
			}
		}
		FFT.Complex[][] output = fft(input);
		for (int i=0; i < output.length; ++i){
			for (int j=0; j < input[i].length; ++j){
				System.out.print(df.format(output[i][j].norm()) + " ");
			}
			System.out.println();
		}
		System.out.println();
		FFT.Complex[][] reverse = ifft(output);
		for (int i=0; i < reverse.length; ++i){
			for (int j=0; j < reverse[i].length; ++j){
				System.out.print(df.format(reverse[i][j].norm()) + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
}
