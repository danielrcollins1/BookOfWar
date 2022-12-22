/******************************************************************************
*  One solo figure.
*
*  @author   Daniel R. Collins
*  @since    2022-12-22
******************************************************************************/

public class Solo extends Unit {

	//--------------------------------------------------------------------------
	//  Constructor(s)
	//--------------------------------------------------------------------------

	/**
	*  Constructor (from string descriptor)
	*/
	public Solo (String[] s) {
		super(s);
	}

	//--------------------------------------------------------------------------
	//  Methods
	//--------------------------------------------------------------------------

	/** Solos cannot recursively have their own solos. */
	@Override public boolean hasSolo() { return false; };
	@Override public Solo getSolo() { return null; }
}
