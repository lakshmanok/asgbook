/**
 * 
 */
package edu.ou.asgbook.filters;

/**
 * 
 * From Numerical Recipes, a fast way to find the kth smallest item in a list
 * Useful to implement rank filters
 * 
 * @author v.lakshmanan
 * 
 */
public class QuickSelect {
	private static void SWAP(int[] arr, int x, int y) {
		int temp = arr[x];
		arr[x] = arr[y];
		arr[y] = temp;
	}

	public static int kth_element(int[] arr, int k) {
		return kth_element(arr, arr.length, k);
	}

	/**
	 * Finds the kth smallest item in the list
	 * @param arr list
	 * @param n   number of elements in list, in case the last elements of list are unfilled
	 * @param k   finds kth smallest
	 * @return
	 */
	public static int kth_element(int[] arr, int n, int k) {
		if (k > n){
			throw new IllegalArgumentException("k should be less than n!");
		}
		int i, ir, j, low, mid;
		int a;

		low = 0;
		ir = n - 1;
		for (;;) {
			if (ir <= low + 1) {
				if (ir == low + 1 && arr[ir] < arr[low]) {
					SWAP(arr, low, ir);
				}
				return arr[k];
			} else {
				mid = (low + ir) >> 1;
				SWAP(arr, mid, low + 1);
				if (arr[low] > arr[ir]) {
					SWAP(arr, low, ir);
				}
				if (arr[low + 1] > arr[ir]) {
					SWAP(arr, low + 1, ir);
				}
				if (arr[low] > arr[low + 1]) {
					SWAP(arr, low, low + 1);
				}
				i = low + 1;
				j = ir;
				a = arr[low + 1];
				for (;;) {
					do
						i++;
					while (arr[i] < a);
					do
						j--;
					while (arr[j] > a);
					if (j < i)
						break;
					SWAP(arr, i, j);
				}
				arr[low + 1] = arr[j];
				arr[j] = a;
				if (j >= k)
					ir = j - 1;
				if (j <= k)
					low = i;
			}
		}
	}
}
