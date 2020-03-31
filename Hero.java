/******************************************************************************
*  One hero figure.
*
*  @author   Daniel R. Collins (dcollins@superdan.net)
*  @since    2010-10-11
******************************************************************************/

public class Hero extends Unit {
	private int wizardRank;    // Wizard rank = level - 10

	//--------------------------------------------------------------------------
	//  Constructors
	//--------------------------------------------------------------------------

// 	/**
// 	*  Constructor (general).
// 	*/
// 	public Hero(String name, int cost, int MV, int AH, int HD, int atk, int dam) {
// 		// TODO: Heroes currently not implemented.
// 		//super(name, cost, MV, AH, HD, atk, dam, 0, 0, 0.75, 'N', "");
// 		this.wizardRank = 0;
// 	}
// 
// 	/**
// 	*  Constructor (wizards).
// 	*/
// 	public Hero(String name, int cost, int MV, int AH, int HD, int atk, int dam, 
// 			int wizardRank) {
// 		//this(name, cost, MV, AH, HD, atk, dam);
// 		this.wizardRank = wizardRank;
// 	}

	public Hero (String[] s) {
		super(s);
		this.wizardRank = 0;
	}

	//--------------------------------------------------------------------------
	//  Methods
	//--------------------------------------------------------------------------
	public boolean isWizard() { return wizardRank > 0; }
	public int getWizardRank() { return wizardRank; }

	/**
	*  Is this unit fearless (immune to morale)?
	*/
	public boolean isFearless () {
		return true;
	}	

	/**
	*  Refresh special ability charges (overrides Unit).
	*/
	public void refreshCharges() { 
		if (isWizard()) // 6th-level spells
			specialCharges = wizardRank - 1;
//		else if (nameStarts("Dragon"))
//			specialCharges = 3;
	}
}

