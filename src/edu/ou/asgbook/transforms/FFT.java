/**
 * 
 */
package edu.ou.asgbook.transforms;

import java.text.DecimalFormat;


/**
 * FFT based on Sedgewick and Wayne
 * 
 * @author v.lakshmanan
 * 
 */
public class FFT {
	public static class Complex {
		public final double real;
		public final double imag;
		public Complex(double real, double imag) {
			super();
			this.real = real;
			this.imag = imag;
		}
		public Complex multiply(Complex o){
	        return new Complex(real * o.real - imag * o.imag, real * o.imag + imag * o.real);
		}
		public Complex add(Complex o){
			return new Complex(real + o.real, imag + o.imag);
		}
		public Complex subtract(Complex o){
			return new Complex(real - o.real, imag - o.imag);
		}
		public Complex conjugate(){
			return new Complex(real, -imag);
		}
		public Complex divide(int N){
			return new Complex(real/N, imag/N);
		}
		public double normsq(){
			return real*real + imag*imag;
		}
		public double norm(){
			return Math.sqrt(normsq());
		}
		public double phase(){
			return Math.atan2(imag,real);
		}
		public Complex multiply(double d) {
			return new Complex(real*d, imag*d);
		}
	}
	
	/** Computes FFT of array whose length is a power of 2 */
	public static Complex[] fft(Complex[] x) {
		int N = x.length;

		if (N == 1){
			return new Complex[] { x[0] };
		} else if (N % 2 != 0) {
			throw new IllegalArgumentException("N is not a power of 2");
		}

		// Break the array down into two parts and perform FFT of each piece
		Complex[] part = new Complex[N / 2];
		for (int k = 0; k < N / 2; k++) {
			part[k] = x[2 * k]; // even terms
		}
		Complex[] evenfft = fft(part);
		for (int k = 0; k < N / 2; k++) {
			part[k] = x[2 * k + 1]; // odd terms
		}
		Complex[] oddfft = fft(part);

		// combine
		Complex[] y = new Complex[N];
		for (int k = 0; k < N / 2; k++) {
			double kth = -2 * k * Math.PI / N;
			Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
			y[k] = evenfft[k].add(wk.multiply(oddfft[k]));
			y[k + N / 2] = evenfft[k].subtract(wk.multiply(oddfft[k]));
		}
		return y;
	}

	/** compute inverse FFT of array whose length is a power of 2 */
	public static Complex[] ifft(Complex[] x) {
		int N = x.length;

		// Conjugate x
		Complex[] y = new Complex[N];
		for (int i = 0; i < N; i++) {
			y[i] = x[i].conjugate();
		}

		// compute forward FFT
		y = fft(y);

		// Conjugate result and divide by N
		for (int i = 0; i < N; i++) {
			y[i] = y[i].conjugate().divide(N);
		}

		return y;

	}

	public static void main(String[] args){
		DecimalFormat df = new DecimalFormat("0.0");
		// FFT( rect ) should be a sinc function
		FFT.Complex[] input = new FFT.Complex[32];
		for (int i=0; i < input.length; ++i){
			input[i] = new FFT.Complex(0,0);
		}
		for (int i=input.length/3; i < 2*input.length/3; ++i){
			input[i] = new FFT.Complex(1, 0);
		}
		FFT.Complex[] output = fft(input);
		for (int i=0; i < output.length; ++i){
			System.out.print(df.format(output[i].norm()) + " ");
		}
		System.out.println();
		FFT.Complex[] reverse = ifft(output);
		for (int i=0; i < reverse.length; ++i){
			System.out.print(df.format(reverse[i].norm()) + " ");
		}
		System.out.println();
	}
	
}
