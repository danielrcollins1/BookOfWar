/******************************************************************************
*  One solo figure.
*
*  @author   Daniel R. Collins
*  @since    2022-12-22
******************************************************************************/

public class Solo extends Unit {

	//----------------------------------------------------------------------
	//  Field(s)
	//----------------------------------------------------------------------

	/** 
	*  The unit in which this figure is embedded (if any).
	*/
	private Unit hostUnit;

	//----------------------------------------------------------------------
	//  Constructor(s)
	//----------------------------------------------------------------------

	/**
	*  Constructor (from string descriptor).
	*  @param s Descriptor string array.
	*/
	public Solo(String[] s) {
		super(s);
	}

	//----------------------------------------------------------------------
	//  Methods
	//----------------------------------------------------------------------

	/** Solos cannot recursively have their own solos. */
	@Override public boolean hasSolo() { return false; };
	@Override public Solo getSolo() { return null; }
	
	/** Solos never check morale. */
	@Override public boolean isFearless() { return true; }
	
}
