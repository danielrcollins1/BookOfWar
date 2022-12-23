/******************************************************************************
*  Special ability types.
*  - Names should be nouns or noun phrases.
*
*  @author   Daniel R. Collins
*  @since    2022-12-10
******************************************************************************/

public enum SpecialType {

	//----------------------------------------------------------------------
	//  Enumeration
	//----------------------------------------------------------------------

	Pikes, Shields, Mounts, SplitMove, MoraleBonus,
	WoodsCover, GiantDodging, LightWeakness, GiantClass,
	ShotBonus, MeleeShot, NoRainShot, BigStones, DamageInc, 
	Invisibility, Detection, Teleport, Regeneration, 
	FireImmunity, ColdImmunity, VoltImmunity, FireVulnerability,
	Flight, Swimming, BreathWeapon, Whirlwind, Spells,
	SweepAttacks, SilverToHit, MagicToHit, MagicResistance, WeatherControl;
	
	//----------------------------------------------------------------------
	//  Methods
	//----------------------------------------------------------------------

	/**
	*  Find special type matching a string.
	*  @param s Name of special ability type.
	*  @return Value of special type (or null).
	*/
	public static SpecialType findByName(String s) {
		for (SpecialType t: SpecialType.values()) {
			if (s.equals(t.name())) {
				return t;
			}
		}
		return null;
	}
}
