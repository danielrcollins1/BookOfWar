/******************************************************************************
*  Special ability types.
*  - Names should be nouns or noun phrases.
******************************************************************************/

public enum SpecialType {

	//--------------------------------------------------------------------------
	//  Enumeration
	//--------------------------------------------------------------------------

	Pikes, Shields, Mounts, MoraleBonus, 
	WoodsCover, GiantDodging, LightWeakness, GiantClass,
	ShotBonus, MeleeShot, NoRainShot, LargeStones, DamageInc, 
	Invisibility, Detection, Teleport, Regeneration, 
	FireImmunity, ColdImmunity, VoltImmunity, FireVulnerability,
	Flight, Swimming, BreathWeapon, Whirlwind, Spells,
	Solo, SweepAttacks, SilverToHit, MagicToHit, MagicResistance;
	
	//--------------------------------------------------------------------------
	//  Methods
	//--------------------------------------------------------------------------

	/**
	*  Find special type matching a string.
	*/
	static public SpecialType findByName (String s) {
		for (SpecialType t: SpecialType.values()) {
			if (s.equals(t.name())) {
				return t;
			}
		}
		return null;
	}
}
