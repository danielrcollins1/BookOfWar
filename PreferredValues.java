/******************************************************************************
*  Preferred values manager class.
******************************************************************************/

public class PreferredValues {
	static final int PREFER_VALS[] = {1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20};
	static final int HIGHER_INC = 5;

	/**
	*  Get the nth preferred value (possibly outside array).
	*/
	static private int get (int idx) {
		assert(idx >= 0);
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
	*  Find index of first preferred value greater than or equal to target.
	*/
	static private int getIdxAtLeast (int num) {
		int idx = 0;
		while (get(idx) < num) idx++;
		return idx;
	}

	/**
	*  Find index of closest preferred value.
	*/
	static private int getClosestIdx (int num) {

		// Get upper-bound index & value
		int highIdx = getIdxAtLeast(num);
		int highIdxVal = get(highIdx);
		if (highIdxVal == num) return highIdx;

		// Get lower-bound index & value
		int lowIdx = highIdx - 1;
		int lowIdxVal = get(lowIdx);
		int valRange = highIdxVal - lowIdxVal;
		
		// Check which is closer
		return (2 * (num - lowIdxVal) < valRange) ?
			lowIdx : highIdx;
	}

	/**
	*  Get value of closest preferred number.
	*/
	static public int getClosest (int num) {
		return get(getClosestIdx(num));	
	}

	/**
	*  Increment to next higher preferred value.
	*/
	static public int inc (int num) {
		int closestIdx = getClosestIdx(num);
		int closestVal = get(closestIdx);
		return num < closestVal ? closestVal : get(closestIdx + 1);
	}

	/**
	*  Decrement to next lower preferred value.
	*/
	static public int dec (int num) {
		int closestIdx = getClosestIdx(num);
		int closestVal = get(closestIdx);
		return num > closestVal ? closestVal : get(closestIdx - 1);
	}

	/**
	*  Main test driver.
	*/
	public static void main (String[] args) {

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