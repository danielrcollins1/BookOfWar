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
	private Unit host;

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

	/**
	*  Constructor (copy).
	*  @param src source solo to copy.
	*/
	public Solo(Solo src) {
		super(src);
		host = src.host;
	}

	//----------------------------------------------------------------------
	//  Methods
	//----------------------------------------------------------------------

	/** Solos don't have embedded leaders. */
	@Override public Solo getLeader() { return null; }
	@Override public boolean hasLeader() { return false; };

	/** Solos may be embedded in another unit. */
	@Override public Unit getHost() { return host; }
	@Override public boolean hasHost() { return host != null; }
	
	/** Solos never check morale. */
	@Override public boolean isFearless() { return true; }

	/** Solos always hit on an attack. */
	@Override public boolean autoHits() { return true; }
	
	/** Solos present small targets against attacks. */
	@Override public boolean isSmallTarget() { return true; }
	
	/** Solos are never subject to sweep attacks. */
	@Override public boolean isSweepable() { return false; }

	/** Solos get saving throws versus magic damage. */
	@Override public boolean getsSaves() { return true; }
}
