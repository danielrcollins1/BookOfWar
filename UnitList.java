import java.util.*;
import java.io.IOException; 

/******************************************************************************
*  List of available unit types (singleton pattern).
*
*  @author   Daniel R. Collins (dcollins@superdan.net)
*  @since    2014-09-05
******************************************************************************/

public class UnitList implements Iterable<Unit> {
	//--------------------------------------------------------------------------
	//  Constants
	//--------------------------------------------------------------------------

	/** Name of the data file. */
	final String UNIT_TYPES_FILE = "UnitTypes.csv";

	//--------------------------------------------------------------------------
	//  Fields
	//--------------------------------------------------------------------------

	/** The singleton class instance. */
	static UnitList instance = null;
	
	/** Array of Unit records. */
	List<Unit> unitList;

	//--------------------------------------------------------------------------
	//  Constructors
	//--------------------------------------------------------------------------

	/**
	*  Constructor (read from dedicated file).
	*/
	protected UnitList () throws IOException {
		String[][] table = CSVReader.readFile(UNIT_TYPES_FILE);
		unitList = new ArrayList<Unit>(table.length - 1);
		for (int i = 1; i < table.length; i++) {
			unitList.add(new Unit(table[i]));
		}
	}

	//--------------------------------------------------------------------------
	//  Methods
	//--------------------------------------------------------------------------

	/**
	*  Access the singleton class instance.
	*/
	public static UnitList getInstance() {
		if (instance == null) {
			try {
				instance = new UnitList();
			}
			catch (IOException e) {
				System.out.println("Failed to read the UnitTypes file.");
			}
		}
		return instance;
	}

	/**
	*	Return iterator for the iterable interface.
	*/
	public Iterator<Unit> iterator() {
		return unitList.iterator();
	}
	
	/**
	*	Return the size of the unit list.
	*/
	public int size () {
		return unitList.size();	
	}

	/**
	*	Return unit in a specified index of the list.
	*/
	public Unit get (int index) {
		if (0 <= index && index < size()) {
			return unitList.get(index);
		}	
		else {
			System.err.println("UnitList index out-of-bounds.");
			return null;
		}
	}

	/**
	*  Get a copy of a given range of the unit list.
	*/
	public Unit[] copyOfRange (int from, int to) { 
		// TODO: switch this to List implementation.
		int size = to - from;
		Unit[] copy = new Unit[size];
		for (int i = 0; i < size; i++) {
			copy[i] = unitList.get(from + i);
		}
		return copy;
	}

	/**
	*  Get a unit with a particular name.
	*/
	public Unit getByName (String s) {
		for (Unit unit: unitList) {
			if (unit.getName().equals(s))
				return unit;
		}
		return null;
	}

	/**
	*  Main test method.
	*/
	public static void main (String[] args) {
		UnitList list = UnitList.getInstance();
		for (Unit unit: list) {
			System.out.println(unit);
		}
		System.out.println();
	}
}

