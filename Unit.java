import java.util.Set;
import java.util.HashSet;

/**
	One unit of figures.

	@author Daniel R. Collins
	@since 2009-11-27
*/

public class Unit {

	/** Alignment types. */
	public enum Alignment { Lawful, Neutral, Chaotic };

	//----------------------------------------------------------------------
	//  Constants
	//----------------------------------------------------------------------

	/** Conversion from internal width units to inches. */
	private static final int WIDTH_UNITS_PER_INCH = 4;

	//----------------------------------------------------------------------
	//  Fixed unit type fields
	//----------------------------------------------------------------------

	/** Name of the unit type. */
	private String name;

	/** Cost per figure. */
	private int cost;
	
	/** Movement rate in inches. */
	private int move;
	
	/** Armor value (6-point scale). */
	private int armor;
	
	/** Health (as D&D hit dice). */
	private int health;
	
	/** Number of melee attacks per turn. */
	private int attacks;
	
	/** Damage inflicted per hit. */
	private int damage;
	
	/** Rate of missile attacks per turn. */
	private int rate;
	
	/** Range of missile attacks in inches. */
	private int range;
	
	/** Width of figure in quarter-inch units. */
	private int width;
	
	/** Alignment of the creature type. */
	private Alignment alignment;

	/** List of special abilities. */
	private Set<SpecialAbility> specials;

	//----------------------------------------------------------------------
	//  Dynamic in-game play records
	//----------------------------------------------------------------------

	/** Number of figures. */
	private int figures;
	
	/** Number of figures across front. */
	private int frontFiles;
	
	/** Damage taken up to a figure loss. */
	private int damageTaken;
	
	/** Number of figures lost this turn. */	
	private int figsLostInTurn;

	/** Is this unit routed? */
	private boolean routed;

	/** Can this unit be seen normally? */
	private boolean visible;
	
	/** Has this unit made a save vs. magic fear? */
	private boolean savedVsFear;

	/** Charges available for a special ability. */
	protected int specialCharges;

	/** Solo leader embedded with this unit. */
	private Solo leader;

	//----------------------------------------------------------------------
	//  Constructors
	//----------------------------------------------------------------------

	/**
		Constructor (from string array).
		@param s descriptor string array.
	*/
	public Unit(String[] s) {
		name = s[0];
		cost = Integer.parseInt(s[1]);
		move = Integer.parseInt(s[2]);
		armor = Integer.parseInt(s[3]);
		health = Integer.parseInt(s[4]);
		attacks = Integer.parseInt(s[5]);
		damage = Integer.parseInt(s[6]);
		rate = Integer.parseInt(s[7]);
		range = Integer.parseInt(s[8]);		
		width = Integer.parseInt(s[9]);
		alignment = parseAlignment(s[10]);		
		parseSpecials(s[11]);
	}

	/**
		Constructor (copy).
		@param src source unit to copy.
	*/
	public Unit(Unit src) {
		name = src.name;
		cost = src.cost;
		move = src.move;
		armor = src.armor;
		health = src.health;
		attacks = src.attacks;
		damage = src.damage;
		rate = src.rate;
		range = src.range;
		width = src.width;
		alignment = src.alignment;
		specials = new HashSet<SpecialAbility>(src.specials);
		if (src.leader != null) {
			setLeader(new Solo(src.leader));
		}
		// Other in-play records not copied
	}

	//----------------------------------------------------------------------
	//  Methods
	//----------------------------------------------------------------------

	// Unit type statistics
	public String getName() { return name; };
	public int getCost() { return cost; };
	public int getArmor() { return armor; };
	public int getHealth() { return health; };
	public int getAttacks() { return attacks; };
	public int getDamage() { return damage; };
	public int getRate() { return rate; };
	public int getRange() { return range; };
	public Alignment getAlignment() { return alignment; };

	// Unit in-play records
	public int getFigures() { return figures; };
	public int getFiles() { return frontFiles; }
	public int getFigsLostInTurn() { return figsLostInTurn; };
	public int getCharges() { return specialCharges; }
	public boolean hasMissiles() { return range > 0; };
	public boolean isVisible() { return visible; };
	public Solo getLeader() { return leader; };

	// Methods to be overridden by sublass
	public boolean isSmallTarget() { return false; }
	public boolean isSweepable() { return health <= 1; }
	public boolean autoHits() { return false; }
	public boolean getsSaves() { return false; }
	public boolean hasHost() { return false; }
	public boolean hasActiveHost() { return false; }
	public Unit getHost() { return null; }
	
	/**
		Parse alignment code.
		@param s alignment descriptor.
		@return alignment enumeration.
	*/
	private Alignment parseAlignment(String s) {
		if (s.length() > 0) {
			switch (s.charAt(0)) {
				case 'L': return Alignment.Lawful;
				case 'C': return Alignment.Chaotic;
				default: return Alignment.Neutral;
			}
		}
		return Alignment.Neutral;
	}

	/**
		Parse specials list.
		@param specialString descriptor of special abilities.
	*/
	private void parseSpecials(String specialString) {
		specials = new HashSet<SpecialAbility>();
		if (!specialString.equals("-")) {
			String[] splits = specialString.split(", ");
			for (String s: splits) {
				SpecialAbility ability 
					= SpecialAbility.createFromString(s);
				if (ability != null) {
					specials.add(ability);
				}
			}
		}
	}
	
	/**
		Get the movement rate.
		@return the current move rate.
	*/
	public int getMove() { 
		return hasActiveLeader()
			? Math.min(move, leader.getMove()) : move;
	};

	/**
		Set the leader for this unit.
		@param newLeader the new leader for this unit.
	*/
	public void setLeader(Solo newLeader) { 
		leader = newLeader;
		leader.setHost(this);
	}

	/**
		Set a new cost.
		@param newCost the new cost.
	*/
	public void setCost(int newCost) {
		assert newCost >= 1;
		cost = newCost;
	}

	/**
		Initialize figures.
		@param numFigs number of figures for unit.
	*/
	public void setFigures(int numFigs) {
		assert numFigs >= 0;
		figures = numFigs;
		routed = false;
		damageTaken = 0;
	}

	/**
		Initialize frontage.
		@param files number of files for unit.
	*/
	public void setFiles(int files) {
		assert files >= 0;
		assert files <= figures;
		frontFiles = files;	
	}

	/**
		Get one figure's raw width.
		@return one figure's width in database pips.
	*/
	public int getFigWidthPips() { 
		return width;
	};
	
	/**
		Get one figure's width.
		@return one figure's width in inches.
	*/
	public double getFigWidth() { 
		return (double) width / WIDTH_UNITS_PER_INCH; 
	};

	/**
		Get one figure's length.
		@return one figure's length in inches.
	*/
	public double getFigLength() {
		return hasSpecial(SpecialType.Mounts) 
			? 2 * getFigWidth() : getFigWidth();
	}

	/**
		Compute how many effective ranks we have.
		@return number of ranks in unit.
	*/
	public int getRanks() {
		int files = getFiles();
		if (files < 1) {
			return 0;
		}
		int ranks = figures / files;
		int backrow = figures % files;
		if (backrow * 2 >= files) {
			ranks++;
		}
		return ranks;	
	}

	/**
		Compute width of unit.
		@return width of unit in inches.
	*/
	public double getTotalWidth() {
		return getFiles() * getFigWidth();
	}

	/**
		Compute length of unit.
		@return length of unit in inches.
	*/
	public double getTotalLength() {
		return getRanks() * getFigLength();
	}

	/**
		Compute perimeter around entire unit.
		@return perimeter around unit in inches.
	*/
	public double getPerimeter() {
		if (figures == 0 && hasActiveLeader()) {
			return leader.getPerimeter();
		}
		else {
			return 2 * (getTotalWidth() + getTotalLength());
		}
	}

	/**
		Remove a certain number of figures.
		@param lost number requested to be removed.
		@return number actually lost (capped by number in unit).
	*/
	private int removeFigures(int lost) {
		lost = Math.min(lost, figures);
		figures -= lost;
		figsLostInTurn += lost;
 		if (figures < frontFiles) {
 			frontFiles = figures;
 		}
		return lost;
	}

	/**
		Take damage; returns figures killed.
		@param points damage inflicted on unit.
		@return number of figures lost.
	*/
	public int takeDamage(int points) {
		assert points >= 0;
		damageTaken += points;
		int figsKilled = damageTaken / health;
		damageTaken = damageTaken % health;
		return removeFigures(figsKilled);
	}

	/**
		Clear out figures lost in a turn.
	*/
	public void clearFigsLostInTurn() {
		figsLostInTurn = 0;	
	}

	/**
		Are the base-level troops in this unit beaten?
		(May still have active leader.)
		Think of this as "shallow beaten".
		@return true if the normals in this unit are nonfunctional.
	*/
	public boolean isNormalBeaten() {
		return figures == 0 || routed;	
	}

	/**
		Is this unit totally decimated?
		(Includes any attached leader.)
		Think of this as "deeply beaten".
		@return true if this unit is totally nonfunctional.
	*/
	public boolean isTotallyBeaten() {
		return isNormalBeaten() && !hasActiveLeader();	
	}

	/**
		Get a special ability matching a given type.
		@param type special ability type.
		@return special ability object (or null)
	*/
	public SpecialAbility getAbilityByType(SpecialType type) {
		for (SpecialAbility a: specials) {
			if (a.getType() == type) {
				return a;
			}
		}
		return null;
	}

	/**
		Find if unit has a special of a given type.
		@param type special ability type.
		@return true if unit has that type of ability.
	*/
	public boolean hasSpecial(SpecialType type) {
		SpecialAbility ability = getAbilityByType(type);
		return ability != null;	
	}

	/**
		Get the parameter for a given special type.
		@param type special ability type.
		@return parameter value of special ability.
	*/
	public int getSpecialParam(SpecialType type) {
		SpecialAbility ability = getAbilityByType(type);
		return ability != null ? ability.getParam() : 0;
	}

	/**
		Get a breath weapon, if any.
		@return this unit's breath weapon (or null).
	*/
	public SpecialAbility getBreathWeapon() {
		for (SpecialAbility ability: specials) {
			if (ability.getType().isBreathWeapon()) {
				return ability;
			}
		}
		return null;
	}

	/**
		Does this unit have any type of breath weapon?
		@return true f we have a breath weapon.
	*/
	public boolean hasBreathWeapon() {
		return getBreathWeapon() != null;
	}

	/**
		Decrement special ability charges.
	*/
	public void decrementCharges() {
		specialCharges--;	
	}

	/**
		Refresh special ability charges pre-game.
	*/
	public void refreshCharges() {

		// Unicorn teleport 1 time/game.
		if (hasSpecial(SpecialType.Teleport)) {
			specialCharges = 1;
		}

		// Storm Giants control weather 1 time/game.
		else if (hasSpecial(SpecialType.WeatherControl)) {
			specialCharges = 1;
		}

		// Dragon breath usable 3 times/game.
		else if (hasBreathWeapon()) {
			specialCharges = 3;
		}

		// Wizards with top spell slots by level.
		else if (hasSpecial(SpecialType.Spells)) {
			specialCharges = getSpecialParam(SpecialType.Spells);
		}
	
		// Otherwise nothing.
		else {	
			specialCharges = 0;
		}
	}
	
	/**
		Regenerate hits.

		For higher budget-scaling purposes, we assume here that
		each figure in a unit regenerates 1 pip (i.e., any hits were
		distributed to all figures).

		In practice, players don't assemble more than one figure of trolls
		in a unit, so this is a bit academic. In the written book
		for simplicity we'll probably just say "regenerate 1 hit".

		Note if we regenerate only one pip total here, then trolls drop
		value precipitously at higher budgets, compared to other types.
	*/
	public void regenerate() {
		damageTaken -= Math.min(getFigures(), damageTaken);
	}

	/**
		Set unit visibility.
		@param newVisible true if unit becomes visible.
	*/
	public void setVisible(boolean newVisible) {
		visible = newVisible;
	}

	/**
		Set routed status.
		@param newRouted true if unit becomes routed.
	*/
	public void setRouted(boolean newRouted) {
		routed = newRouted;
		if (routed) {
			removeFigures(figures);
		}
	}

	/**
		Get routed status.
		@return true if this unit is routed.
	*/
	public boolean isRouted() {
		return routed;
	}

	/**
		Get the unit's flying movement.
		@return flying speed in inches.
	*/
	public int getFlyMove() {
		return getSpecialParam(SpecialType.Flight);
	}

	/**
		Can this unit teleport?
		@return true if unit can teleport at this time.
	*/
	public boolean canTeleport() {
		return hasSpecial(SpecialType.Teleport)
			&& getCharges() > 0;
	}

	/**
		Does this unit have a leader set?
		@return true if unit has a leader object.
	*/
	public boolean hasLeader() { 
		return leader != null; 
	};

	/**
		Does this unit have an active leader?
		@return true if unit has an active leader.
	*/
	public boolean hasActiveLeader() {
		return leader != null && !leader.isNormalBeaten(); 
	}

	/**
		Is this unit reduced down to a lone leader?
		@return true if only a leader remains.
	*/
	public boolean isLoneLeader() {
		return isNormalBeaten() && hasActiveLeader();	
	}

	/**
		Is this unit a caster-type?
		@return true if we can cast magic spells.
	*/
	public boolean isCaster() {
		for (SpecialAbility a: specials) {
			if (a.getType().isSpellCasting()) {
				return true;
			}		
		}
		return false;
	}

	/**
		Does this unit have a caster leader?
		@return true if we have a casting leader.
	*/
	public boolean hasCasterLeader() {
		return hasActiveLeader() && leader.isCaster();
	}

	/**
		Count how many spells this unit has cast.
		@return number of spells cast so far.
	*/
	public int numSpellsCast() {
		if (hasSpecial(SpecialType.Spells)) {
			int maxSpells = getSpecialParam(SpecialType.Spells);
			return maxSpells - specialCharges;
		}
		return 0;
	}

	/**
		Is this unit type fearless?
		@return true if we are immune to morale checks.
	*/
	public boolean isFearless() { 
		return hasSpecial(SpecialType.Fearless); 
	}

	/**
		Does this unit type require a controlling leader?
		@return true if we require a controller.
	*/
	public boolean isControlRequired() {
		for (SpecialAbility a: specials) {
			if (a.getType().isControlRequired()) {
				return true;
			}		
		}
		return false;
	}

	/**
		Have we already saved against fear?
		@return true if we have saved against fear.
	*/
	public boolean hasSavedVsFear() {
		return savedVsFear;	
	}

	/**
		Set whether we have saved against fear.
		@param saved true if we have saved versus fear.
	*/
	public void setSavedVsFear(boolean saved) {
		savedVsFear = saved;	
	}

	/**
		Return a plural suffix if needed.
		@param n a number.
		@return "s" if n is not one.
	*/
	public String plural(int n) {
		return n == 1 ? "" : "s";
	}

	/**
		Get a string representation of this object.
		@return string representation of this unit.
	*/
	public String toString() {
		String s = name + " (" + figures + " fig" + plural(figures) 
			+ ", " + getRanks() + " rank" + plural(getRanks());
		if (hasActiveLeader()) {
			s += ", " + leader.getName();
		}
		s += ")";
		return s;
	}
	
	/**
		Get a short abbreviation of this unit's name.
		@return short abbreviation for unit.
	*/
	public String getAbbreviation() {
		String s = "" + name.charAt(0);
		for (int i = 1; i < name.length() - 1; i++) {
			if (name.charAt(i) == ' ' && s.length() < 2) {
				s = s + name.charAt(i + 1);
			}
		}
		return s;
	}
	
	/**
		Main test method.
		@param args command-line arguments.
	*/
	public static void main(String[] args) {
		String[] desc = {"Light Infantry", "4", "12", "4", 
			"1", "1", "1", "0", "0", "3", "N", "-"};
		Unit unit = new Unit(desc);
		System.out.println(unit);
	}
}
