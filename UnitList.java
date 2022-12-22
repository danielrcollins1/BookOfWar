import java.util.*;
import java.io.IOException; 

/******************************************************************************
*  List of available unit types (singleton pattern).
*
*  @author   Daniel R. Collins
*  @since    2014-09-05
******************************************************************************/

public class UnitList implements Iterable<Unit> {
	enum Type {Unit, Solo};

	//--------------------------------------------------------------------------
	//  Fields
	//--------------------------------------------------------------------------

	/** List of Unit records. */
	List<Unit> unitList;

	//--------------------------------------------------------------------------
	//  Constructors
	//--------------------------------------------------------------------------

	/**
	*  Constructor (read from dedicated file).
	*/
	protected UnitList (String filename, Type type) throws IOException {
		String[][] table = CSVReader.readFile(filename);
		unitList = new ArrayList<Unit>(table.length - 1);
		for (int i = 1; i < table.length; i++) {
			Unit newUnit = (type == Type.Unit) ?
				new Unit(table[i]) : new Solo(table[i]);
			unitList.add(newUnit);
		}
	}

	//--------------------------------------------------------------------------
	//  Methods
	//--------------------------------------------------------------------------

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
		return unitList.get(index);
	}

	/**
	*  Get references for given range of the unit list.
	*/
	public List<Unit> getSublist (int start, int end) {
		return unitList.subList(start, end);
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
		UnitList list = null;
		try {
			list = new UnitList("UnitTypes.csv", Type.Unit);
		}
		catch (IOException e) {
			System.err.println("Failed to read test units file");
		}
		for (Unit unit: list) {
			System.out.println(unit);
		}
		System.out.println();
	}
}
