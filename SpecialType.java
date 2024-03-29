/**
	Special ability types.
	- Names should be nouns or noun phrases.

	@author Daniel R. Collins
	@since 2022-12-10
*/

public enum SpecialType {

	//----------------------------------------------------------------------
	//  Enumeration
	//----------------------------------------------------------------------

	Pikes, Shields, Mounts, SplitMove, MoraleBonus,
	WoodsCover, LightWeakness, GiantClass, GiantDodging,
	ShotBonus, MeleeShot, NoRainShot, BigStones, DamageBonus, 
	Invisibility, Detection, Teleport, Regeneration, 
	Flight, Swimming, SweepAttack, SilverToHit, MagicToHit, 
	Spells, Wand, MagicResistance, WeatherControl, Whirlwind, 
	FireBreath, VoltBreath, ColdBreath, AcidBreath, PoisonBreath, 
	MultiBreath, FireImmunity, VoltImmunity, ColdImmunity, AcidImmunity,
	PoisonImmunity, FireVulnerability, MissileWard, Fear, 
	Fearless, Animated, Conjured;
	
	//----------------------------------------------------------------------
	//  Methods
	//----------------------------------------------------------------------

	/**
		Find special type matching a string.
		@param s Name of special ability type.
		@return Value of special type (or null).
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
		Is this a type of breath weapon?
		@return true if this is a breath weapon.
	*/
	public boolean isBreathWeapon() {
		switch (this) {
			case FireBreath: case ColdBreath: case AcidBreath:
			case VoltBreath: case PoisonBreath: case MultiBreath:
				return true;
			default: 
				return false;
		}	
	}
	
	/**
		Is this a type of magic spell casting?
		@return true if this is a magic spell ability.
	*/
	public boolean isSpellCasting() {
		switch (this) {
			case Spells: case Wand: case WeatherControl:
				return true;
			default:
				return false;
		}	
	}
	
	/**
		Is this a type that requires a controlling leader?
		@return true if this requires a controller.
	*/
	public boolean isControlRequired() {
		switch (this) {
			case Animated: case Conjured: 
				return true;
			default: 
				return false;
		}	
	}
}
