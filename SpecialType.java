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
	Flight, Swimming, Whirlwind, Spells,
	SweepAttack, SilverToHit, MagicToHit, MagicResistance, 
	WeatherControl, Fear, Wand,
	FireBreath, VoltBreath, ColdBreath, AcidBreath, PoisonBreath, 
	MultiBreath, FireImmunity, VoltImmunity, ColdImmunity, AcidImmunity,
	PoisonImmunity, FireVulnerability;
	
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
	
	/**
	*  Is this a type of breath weapon?
	*  @return true if this is a breath weapon.
	*/
	public boolean isBreathWeapon() {
		switch (this) {
			case FireBreath: case ColdBreath: case AcidBreath:
			case VoltBreath: case PoisonBreath: case MultiBreath:
				return true;
			default: return false;
		}	
	}
}
