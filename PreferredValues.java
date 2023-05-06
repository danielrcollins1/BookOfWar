/**
	Preferred values sequence class.

	@author Daniel R. Collins
	@since 2022-12-18
*/

public class PreferredValues {

	/**
		Base array for preferred values (note mostly divisors of 60).
	*/
	private static final int[] PREFER_VALS =
		{1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20};

	/**
		Increment for each step beyond the base array.
	*/
	private static final int HIGHER_INC = 5;

	/**
		Get the nth preferred value (possibly outside array).
		@param idx index in the values sequence.
		@return value of the entry.
	*/
	private static int get(int idx) {
		assert idx >= 0;
		int maxArrayIdx = PREFER_VALS.length - 1;
		if (idx <= maxArrayIdx) {
			return PREFER_VALS[idx];
		}
		else {
			return PREFER_VALS[maxArrayIdx]
				+ HIGHER_INC * (idx - maxArrayIdx);
		}
	}

	/**
		Find index of first preferred value greater than or equal to target.
		@param num number to find nearby index.
		@return first index with value at least num.
	*/
	private static int getIdxAtLeast(int num) {
		int idx = 0;
		while (get(idx) < num) {
			idx++;
		}
		return idx;
	}

	/**
		Find index of closest preferred value.
		@param num number to find nearby index.
		@return index with value closest to num.
	*/
	private static int getClosestIdx(int num) {

		// Get upper-bound index & value
		int highIdx = getIdxAtLeast(num);
		int highIdxVal = get(highIdx);
		if (highIdxVal == num) {
			return highIdx;
		}

		// Get lower-bound index & value
		int lowIdx = highIdx - 1;
		int lowIdxVal = get(lowIdx);
		int valRange = highIdxVal - lowIdxVal;
		
		// Check which is closer
		return (2 * (num - lowIdxVal) < valRange) 
			? lowIdx : highIdx;
	}

	/**
		Get value of closest preferred number.
		@param num number to find nearby sequence value.
		@return sequence value closest to num.
	*/
	public static int getClosest(int num) {
		return get(getClosestIdx(num));	
	}

	/**
		Increment to next higher preferred value.
		@param num number to increment in sequence.
		@return next higher sequence value.
	*/
	public static int inc(int num) {
		int closestIdx = getClosestIdx(num);
		int closestVal = get(closestIdx);
		return num < closestVal ? closestVal : get(closestIdx + 1);
	}

	/**
		Decrement to next lower preferred value.
		@param num number to decrement in sequence.
		@return next lower sequence value.
	*/
	public static int dec(int num) {
		int closestIdx = getClosestIdx(num);
		int closestVal = get(closestIdx);
		return num > closestVal ? closestVal : get(closestIdx - 1);
	}

	/**
		Main test driver.
		@param args command-line arguments.
	*/
	public static void main(String[] args) {

		// Test preferred values getter
		System.out.println("Preferred values:");
		for (int i = 0; i < 20; i++) {
			System.out.print(get(i) + " ");
		}
		System.out.print("\n\n");
		
		// Test some find-closest values
		System.out.println("Closest to 16: " + getClosest(16));
	}
}
