import java.util.*;

/******************************************************************************
*  One unit of figures.
*
*  @author   Daniel R. Collins (dcollins@superdan.net)
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
	int cost, move, armor, health, attacks, damage, range, rate, width;
	Alignment alignment;
	List<Keyword> keyList;

	// Unit in-play records
	int figures, frontFiles, damageTaken, figsLostInTurn, specialCharges;
	boolean routed, visible;
	Hero hero;

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
		range = Integer.parseInt(s[7]);		
		rate = Integer.parseInt(s[8]);
		width = Integer.parseInt(s[9]);
		alignment = parseAlignment(s[10]);		
		parseKeywords(s[11]);
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
		range = src.range;
		rate = src.rate;
		width = src.width;
		alignment = src.alignment;
		keyList = new ArrayList<Keyword>(src.keyList);
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
	public int getRange() { return range; };
	public int getFireRate() { return rate; };
	public Alignment getAlignment() { return alignment; };
	public boolean hasKeyword(Keyword key) { return keyList.contains(key); };

	// Unit in-play records
	public int getFigures() { return figures; };
	public int getFiles() { return frontFiles; }
	public int getFigsLostInTurn() { return figsLostInTurn; };
	public int getCharges() { return specialCharges; }
	public boolean hasMissiles() { return range > 0; };
	public boolean hasHero() { return hero != null && !hero.isBeaten(); };
	public boolean isBeaten() { return figures == 0 || routed; };
	public boolean isVisible() { return visible; };
	public Hero getHero() { return hero; };

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
	*  Parse keyword list.
	*/
	private void parseKeywords (String keyString) {
		keyList = new ArrayList<Keyword>();
		String[] splits = keyString.split(", ");
		for (String s: splits) {
			for (Keyword key: Keyword.values()) {
				if (key.name().equals(s)) {
					keyList.add(key);
					break;				
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
		return hasKeyword(Keyword.Mounted) ? 
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
	*  Regenerate 1 hit (formerly per figure in front/contact).
	*/
	public void regenerate () {
		//damageTaken -= getFiles(); 
		if (damageTaken > 0) {
			damageTaken -= 1;
		}
	}

	/**
	*  Set visibility (after invisible attack).
	*/
	public void setVisible (boolean visible) {
		this.visible = visible;
	}

	/**
	*  Is this unit a wizard?
	*/
	public boolean isWizard () {
		return (this instanceof Hero && ((Hero)this).isWizard());
	}

	/**
	*  Is this unit wizard(s), or have an attached wizard?
	*/
	public boolean hasAnyWizard () {
		return (isWizard() || hasHero() && getHero().isWizard());
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
		if (hasKeyword(Keyword.Fly30))
			return 30;
		else if (hasKeyword(Keyword.Fly36))
			return 36;
		else if (hasKeyword(Keyword.Fly48))
			return 48;
		else
			return 0;
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
			"1", "1", "1", "0", "0", "0.75", "N", "-"};
		Unit unit = new Unit(desc);
		System.out.println(unit);
	}
}

