import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
	Parameterized special ability for a unit.

	@author Daniel R. Collins
	@since 2022-12-10
*/

public class SpecialAbility {

	//----------------------------------------------------------------------
	//  Fields
	//----------------------------------------------------------------------

	/** SpecialType of special ability. */
	private SpecialType type;

	/** Parameter for level of ability. */
	private int param;

	//----------------------------------------------------------------------
	//  Constructors
	//----------------------------------------------------------------------

	/**	
		Constructor (type, param).
		@param pType Special type value.
		@param pParam Special parameter value.
	*/
	SpecialAbility(SpecialType pType, int pParam) {
		type = pType;
		param = pParam;
	}

	/**	
		Constructor (type only).
		@param pType Special type value.
	*/
	SpecialAbility(SpecialType pType) {
		this(pType, 0);
	}
	
	/**	
		Constructor (copy).
		@param src Special source to copy.
	*/
	SpecialAbility(SpecialAbility src) {
		this(src.type, src.param);
	}
	
	//----------------------------------------------------------------------
	//  Methods
	//----------------------------------------------------------------------

	/**
		Create new special ability from a string.
		@param s Special ability descriptor.
		@return Special ability object.
	*/
	public static SpecialAbility createFromString(String s) {
		Pattern p = Pattern.compile("(\\w+)( \\(([-]?\\d+))?\\)?");
		Matcher m = p.matcher(s);
		if (m.matches()) {
			SpecialType type = SpecialType.findByName(m.group(1));
			if (type != null) {
				return m.group(3) == null 
					? new SpecialAbility(type) 
					: new SpecialAbility(type, 
						Integer.parseInt(m.group(3)));
			}
			else {
				System.err.println(
					"Error: Unknown special type: " + s);
				return null;
			}
		}
		else {
			System.err.println(
				"Error: Invalid special ability format: " + s);
			return null;		
		}
	}

	/**
		Get the type of this special ability.
		@return Special type value.
	*/
	public SpecialType getType() { 
		return type; 
	}

	/**
		Get the parameter of this special ability.
		@return Special parameter value.
	*/
	public int getParam() { 
		return param; 
	}

	/**
		Identify this object as a string.
		@return String descriptor.
	*/
	public String toString() {
		String s = type.name();
		if (param != 0) {
			s += " (" + param + ")";		
		}
		return s;
	}
	
	/**
		Main test function.
		@param args Command-line arguments.
	*/
	public static void main(String[] args) {
		System.out.println(createFromString("MoraleBonus"));
		System.out.println(createFromString("MoraleBonus (1)"));
		System.out.println(createFromString("MoraleBonus (2)"));
		System.out.println(createFromString("Flight (12)"));
		System.out.println(createFromString("Flight (24)"));
		System.out.println(createFromString("Swimming (18)"));
	}
}
