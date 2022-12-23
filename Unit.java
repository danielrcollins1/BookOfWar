import java.util.*;

/******************************************************************************
*  One unit of figures.
*
*  @author   Daniel R. Collins
*  @since    2009-11-27
******************************************************************************/

public class Unit {
	enum Alignment {Lawful, Neutral, Chaotic};

	//--------------------------------------------------------------------------
	//  Constants
	//--------------------------------------------------------------------------

	/**
	*  Conversion from internal width units to inches.
	*/
	final int WIDTH_UNITS_PER_INCH = 4;

	//--------------------------------------------------------------------------
	//  Fields
	//--------------------------------------------------------------------------

	// Unit type statistics
	String name;
	int cost, move, armor, health, attacks, damage, rate, range, width;
	Alignment alignment;
	Set<SpecialAbility> specialSet;

	// Unit in-play records
	int figures, frontFiles, damageTaken, figsLostInTurn, specialCharges;
	boolean routed, visible;
	Solo solo;

	//--------------------------------------------------------------------------
	//  Constructors
	//--------------------------------------------------------------------------

	/**
	* Constructor (from String array).
	* @param s String array.
	*/
	public Unit (String[] s) {
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
	* Constructor (copy).
	*/
	public Unit (Unit src) {
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
		specialSet = new HashSet<SpecialAbility>(src.specialSet);
		// In-play records not copied
	}

	//--------------------------------------------------------------------------
	//  Methods
	//--------------------------------------------------------------------------

	// Unit type statistics
	public String getName() { return name; };
	public int getCost() { return cost; };
	public int getMove() { return move; };
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
	public boolean isBeaten() { return figures == 0 || routed; };
	public boolean hasSolo() { return solo != null && !solo.isBeaten(); };
	public Solo getSolo() { return solo; };

	// Methods to be overridden by sublass
	public boolean isFearless () { return false; }
	
	/**
	*  Parse alignment code.
	*/
	private Alignment parseAlignment (String s) {
		if (s.length() > 0) {
			switch (s.charAt(0)) {
				case 'L': return Alignment.Lawful;
				case 'C': return Alignment.Chaotic;
			}
		}
		return Alignment.Neutral;
	}

	/**
	*  Parse specials list.
	*/
	private void parseSpecials (String specialString) {
		specialSet = new HashSet<SpecialAbility>();
		if (!specialString.equals("-")) {
			String[] splits = specialString.split(", ");
			for (String s: splits) {
				SpecialAbility ability = SpecialAbility.createFromString(s);
				if (ability != null) {
					specialSet.add(ability);
				}
			}
		}
	}

	/**
	*  Set a new cost (for auto-balancer).
	*/
	public void setCost (int cost) {
		this.cost = cost;
	}

	/**
	*  Initialize figures.
	*/
	public void setFigures (int figures) {
		this.figures = figures;
		this.routed = false;
		this.damageTaken = 0;
	}

	/**
	*  Initialize frontage.
	*/
	public void setFiles (int files) {
		assert(files > 0);
		assert(files <= figures);
		frontFiles = files;	
	}
	
	/**
	*  Get the figure width in inches.
	*/
	public double getFigWidth() { 
		return (double) width / WIDTH_UNITS_PER_INCH; 
	};

	/**
	*  Get the figure length in inches (double width for mounts).
	*/
	public double getFigLength() {
		return hasSpecial(SpecialType.Mounts) ? 
			2 * getFigWidth() : getFigWidth();
	}

	/**
	*  Compute how many effective ranks we have.
	*/
	public int getRanks() {
		int files = getFiles();
		if (files < 1) return 0;
		int ranks = figures / files;
		int backrow = figures % files;
		if (backrow * 2 >= files && files > 1) ranks++;
		return ranks;	
	}

	/**
	*  Compute width of unit in inches.
	*/
	public double getTotalWidth () {
		return getFiles() * getFigWidth();
	}

	/**
	*  Compute length of unit in inches.
	*/
	public double getTotalLength () {
		return getRanks() * getFigLength();
	}

	/**
	*  Compute perimeter around entire unit.
	*/
	public double getPerimeter () {
		return 2 * (getTotalWidth() + getTotalLength());
	}

	/**
	*  Remove a certain number of figures.
	*  @return Number lost (capped by number in unit).
	*/
	private int removeFigures (int lost) {
		lost = Math.min(lost, figures);
		figures -= lost;
		figsLostInTurn += lost;
 		if (figures < frontFiles) {
 			frontFiles = figures;
 		}
		return lost;
	}

	/**
	*  Take damage; returns figures killed.
	*/
	public int takeDamage (int points) {
		assert(points >= 0);		
		damageTaken += points;
		int figsKilled = damageTaken / health;
		damageTaken = damageTaken % health;
		return removeFigures(figsKilled);
	}

	/**
	*  Clear out figures lost in a turn.
	*/
	public void clearFigsLostInTurn () {
		figsLostInTurn = 0;	
		}

	/**
	*  Get a special ability matching a given type.
	*/
	public SpecialAbility getAbilityByType (SpecialType type) {
		for (SpecialAbility a: specialSet) {
			if (a.getType() == type) {
				return a;
			}
		}
		return null;
	}

	/**
	*  Find if unit has a special of a given type.
	*/
	public boolean hasSpecial (SpecialType type) {
		SpecialAbility ability = getAbilityByType(type);
		return ability != null;	
	}

	/**
	*  Get the parameter for a given special type.
	*/
	public int getSpecialParam (SpecialType type) {
		SpecialAbility ability = getAbilityByType(type);
		return ability == null ? 0 : ability.getParam();
	}

	/**
	*  Decrement special ability charges.
	*/
	public void decrementCharges() {
		specialCharges--;	
	}

	/**
	*  Refresh special ability charges.
	*/
	public void refreshCharges() { 
		specialCharges = 0;
	}
	
	/**
	*  Regenerate 1 hit.
	*/
	public void regenerate () {
		if (damageTaken > 0) {
			damageTaken -= 1;
		}
	}

	/**
	*  Set unit visibility.
	*/
	public void setVisible (boolean visible) {
		this.visible = visible;
	}

	/**
	*  Set routed status.
	*/
	public void setRouted (boolean routed) {
		this.routed = routed;
	}

	/**
	*  Get the unit's flying movement.
	*/
	public int getFlyMove () {
		return getSpecialParam(SpecialType.Flight);
	}

	/**
	*  Check if this unit has same name as another unit.
	*/
	public boolean isSameType (Unit other) {
		return name.equals(other.name);	
	}

	/**
	*  Return an "s" for a number more than 1.
	*/
	public String plural (int n) {
		return (n == 1 ? "" : "s");
	}

	/**
	*  Returns a string representation of this object.
	*/
	public String toString() {
		return name + " (" + figures + " fig" + plural(figures) + ", "
			+ getRanks() + " rank" + plural(getRanks()) + ")";
	}
	
	/**
	*  Returns a short abbreviation for this unit.
	*/
	public String getAbbreviation() {
		String s = "" + name.charAt(0);
		for (int i = 1; i < name.length() - 1; i++) {
			if (name.charAt(i) == ' ' && s.length() < 2)
				s = s + name.charAt(i + 1);
		}
		return s;
	}
	
	/**
	* Main test method.
	*/
	public static void main (String[] args) {
		String[] desc = {"Light Infantry", "4", "12", "4", 
			"1", "1", "1", "0", "0", "3", "N", "-"};
		Unit unit = new Unit(desc);
		System.out.println(unit);
	}
}
