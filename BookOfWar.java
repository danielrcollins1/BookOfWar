/******************************************************************************
*  Book of War simulation for cost-balancing purposes.
*
*  @author   Daniel R. Collins (dcollins@superdan.net)
*  @since    2009-11-27
******************************************************************************/

public class BookOfWar {
	enum Weather {Sunny, Cloudy, Rainy, Stormy};
	enum Terrain {Open, Gulley, Rough, Hill, Woods, Marsh, Stream, Pond};
	enum SimMode {TableAssess, AutoBalance, ZoomGame};

	//-----------------------------------------------------------------
	//  Constant defaults
	//-----------------------------------------------------------------

	/** Default trials per matchup. */
	final int DEFAULT_TRIALS_PER_MATCHUP = 10000;

	/** Default simulation mode. */
	final SimMode DEFAULT_SIM_MODE = SimMode.TableAssess;

	//-----------------------------------------------------------------
	//  Constant fields
	//-----------------------------------------------------------------

	/** Budget minimum: suggest base 20. */
	final int budgetMin = 50;

	/** Budget maximum: suggest base 100. */
	final int budgetMax = 100;

	/** Switch to set budget as multiple of pricier unit. */
	final boolean packBudgetToMax = true;

	/** How many units from start of list count as base for comparisons. */
	final int baseUnitsForAssessment = 3;

	/** Mode for table assessments (base-to-base, else base-to-extras). */
	final boolean tableAssessBaseToBase = true;

	/** Balances pikes vs. swords & cavalry. (basis 0.20) */
	final double pikeFlankingChance = 0.20;

	/** Balances swords vs. pikes & cavalry. (basis 1.00) */
	final double terrainMultiplier = 1.00;

	/** Cap per-hit damage by target's health? */
	final boolean useDamageCeilingByHealth = true;

	/** Switch for optional morale modifiers. */
	final boolean useOptionalMoraleMods = false;

	/** Switch to buy silver weapons for any troop types. */
	final boolean useSilverWeapons = false;
	
	/** Target for morale check success (per Vol-1, p. 12). */
	final int MORALE_TARGET = 9;

	//-----------------------------------------------------------------
	//  Out-of-game settings
	//-----------------------------------------------------------------

	/** Flag to escape after parsing arguments. */
	boolean exitAfterArgs;

	/** Mode of action for simulator. */
	SimMode simMode;

	/** Number of trials per matchup. */
	int trialsPerMatchup;

	/** Switch for long-range penalty. */
	boolean useRangePenalty;
	
	/** Switch for cavalry charge bonus. */
	boolean useChargeBonus;

	/** Switch for shield bonus vs. pikes & archers. */
	boolean useShieldBonus;

	/** Units for zoom-in game (1-based index into Units list). */
	int zoomGameUnitIdx1, zoomGameUnitIdx2;

	//-----------------------------------------------------------------
	//  In-game variables
	//-----------------------------------------------------------------

	/** Distance between opposing units. */
	int distance;

	/** Weather category for battle. */
	Weather weather;

	/** Uniform terrain for battle. */
	Terrain terrain;
	
	/** Are the units already in contact? */
	boolean inContact;

	/** Are pikes making an interrupting defense now? */
	boolean pikesInterrupt;
	
	/** Victorious unit for the battle. */
	Unit winner;

	//-----------------------------------------------------------------
	//  Constructor(s)
	//-----------------------------------------------------------------

	/**
	*  Construct the simulator.
	*/
	public BookOfWar () {
		simMode = DEFAULT_SIM_MODE;
		trialsPerMatchup = DEFAULT_TRIALS_PER_MATCHUP;
	}

	//-----------------------------------------------------------------
	//  Methods
	//-----------------------------------------------------------------

	/**
	*  Run the main method.
	*/
	public static void main (String[] args) {
		BookOfWar book = new BookOfWar();
		book.parseArgs(args);
		if (book.exitAfterArgs) {
			book.printUsage();
		}
		else {
			book.run();
		}
	}

	/**
	*  Print usage.
	*/
	public void printUsage () {
		System.out.println();
		System.out.println("Usage: BookOfWar [options]");
		System.out.println("  Options include:");
		System.out.println("\t-s use shield bonus");
		System.out.println("\t-c use charge bonus");
		System.out.println("\t-r use range penalty");
		System.out.println("\t-t trials per matchup (default=" + DEFAULT_TRIALS_PER_MATCHUP + ")");
		System.out.println("\t-m sim mode (0 = table-assess, 1 = auto-balance, 2 = zoom-in game");
		System.out.println("\t-y zoom-in game 1st unit index (1-based)");
		System.out.println("\t-z zoom-in game 2nd unit index (1-based)");
		System.out.println();
	}

	/**
	*  Parse arguments.
	*/
	public void parseArgs (String[] args) {
		for (String s: args) {
			if (s.charAt(0) == '-') {
				switch (s.charAt(1)) {
					case 's': useShieldBonus = true; break;
					case 'c': useChargeBonus = true; break;
					case 'r': useRangePenalty = true; break;
					case 'm': parseSimMode(s); break;
					case 't': trialsPerMatchup = getParamInt(s); break;
					case 'y': zoomGameUnitIdx1 = getParamInt(s); break;
					case 'z': zoomGameUnitIdx2 = getParamInt(s); break;
					default: exitAfterArgs = true; break;
				}
			}
			else {
				exitAfterArgs = true;
			}
		}
	}

	/**
	*  Get integer following equals sign in command parameter.
	*/
	int getParamInt (String s) {
		if (s.charAt(2) == '=') {
			try {
				return Integer.parseInt(s.substring(3));
			}
			catch (NumberFormatException e) {
				System.err.println("Error: Could not read integer argument: " + s);
			}
		}
		exitAfterArgs = true;
		return -1;
	}

	/**
	*  Parse the simulation mode.
	*/
	void parseSimMode (String s) {
		int num = getParamInt(s);
		switch (num) {
			case 0: simMode = SimMode.TableAssess; return;
			case 1: simMode = SimMode.AutoBalance; return;
			case 2: simMode = SimMode.ZoomGame; return;
		}
		exitAfterArgs = true;
	}

	/**
	*  Run the simulator in selected mode.
	*/
	void run () {
 		switch (simMode) {
 			case TableAssess: assessmentTable(); break;
 			case AutoBalance: autoBalancer(); break;
 			case ZoomGame: zoomInGame(); break;
 		}
	}

	/**
	*  Battle two specified unit types with detailed reports.
	*/
	void zoomInGame () {
		UnitList unitList = UnitList.getInstance();
		Unit unit1 = unitList.get(zoomGameUnitIdx1 - 1);
		Unit unit2 = unitList.get(zoomGameUnitIdx2 - 1);
		if (unit1 != null && unit2 != null) {
			oneGame(unit1, unit2);
		}
	}

	/**
	*  Create table of assessed win percents.
	*/
	void assessmentTable () {
		UnitList unitList = UnitList.getInstance();
		if (tableAssessBaseToBase) {
			Unit[] set1 = unitList.copyOfRange(0, baseUnitsForAssessment);
			makeAssessmentTable(set1, set1);
		}
		else {
	 		Unit[] set1 = unitList.copyOfRange(0, baseUnitsForAssessment);
 			Unit[] set2 = unitList.copyOfRange(baseUnitsForAssessment, unitList.size());
 			makeAssessmentTable(set2, set1);
		}
	}

	/**
	*  Auto-balance unit costs.
	*/
	void autoBalancer() {
		UnitList unitList = UnitList.getInstance();
		Unit[] baseSet = unitList.copyOfRange(0, baseUnitsForAssessment);
		Unit[] newSet = unitList.copyOfRange(baseUnitsForAssessment, unitList.size());
		makeAutoBalancedTable(baseSet, newSet);
	}

	/**
	*  Report detail for a zoom-in game.
	*/
	void reportDetail (String s) {
		if (simMode == SimMode.ZoomGame) 
			System.out.println(s);
	}
	
	/**
	*  Print to output (printf recreation for copied code).
	*/
	void printf(String s) {
		System.out.print(s);	
	}

	/**
	*  Make general assessment table.
	*/
	void makeAssessmentTable (Unit[] array1, Unit[] array2) {
  
		// Title
		printf("Assessed win percents "
			+ "(budget " + budgetMin + "-" + budgetMax + "):\n\n");

	 	// Header
		for (Unit unit: array2) {
			printf("\t" + unit.getAbbreviation());
		}
		printf("\tWins\tSumErr\n");
		for (int i = 0; i < array2.length + 3; i++) {
			printf("----");
		}
		printf("\n");

  		// Body
		int absTotalError = 0;
		int expectSumPct = array2.length * 50;
		for (Unit unit1: array1) {
			int sumPct = 0;
			int winCount = 0;
			printf(unit1.getAbbreviation() + "\t");
			for (Unit unit2: array2) {
				if (unit1 == unit2) {
					printf("-\t");
					sumPct += 50;
				}					
				else {
					double ratioWon = assessGames(unit1, unit2);
					int pctWon = (int)(ratioWon * 100 + 0.5);
					printf(pctWon >= 50 ? pctWon + "\t" : "-\t");
					if (pctWon >= 50) winCount++;
					sumPct += pctWon;
				}
			}
			printf(winCount + "\t");
			int sumError = sumPct - expectSumPct;
			printf(sumError + "\t");
			absTotalError += Math.abs(sumError);
			printf("\n");
		}

		// Tail
		printf("\nAbsolute Total Error: " + absTotalError);
		printf("\n");
	}

	/**
	*  Estimate best cost for a set of units.
	*/
	void makeAutoBalancedTable (Unit[] baseUnits, Unit[] newUnits) {
		assert(baseUnits != newUnits);

		// Title
		printf("Auto-balanced best cost "
			+ "(budget " + budgetMin + "-" + budgetMax + "):\n\n");

	 	// Header
		printf("Unit\tCost\n");
		for (int i = 0; i < 2; i++) {
			printf("----");
		}
		printf("\n");
				
  		// Body
		for (Unit newUnit: newUnits) {
			setAutoBalancedCost(newUnit, baseUnits);
			printf(newUnit.getAbbreviation() + "\t" + newUnit.getCost() + "\n");
		}
	
		// Tail
		printf("\n");
	}

	/**
	*  Set auto-balanced cost for a new unit.
	*/
	void setAutoBalancedCost (Unit newUnit, Unit[] baseUnits) {
		int expectWins = baseUnits.length/2;
		int lowCost = 1, highCost = newUnit.getCost();

		// Check lower bound for cost
		newUnit.setCost(lowCost);
		int lowWins = assessGameSeries(newUnit, baseUnits);
		if (lowWins < expectWins)
			return;

		// Find upper bound for cost
		newUnit.setCost(highCost);
		int highWins = assessGameSeries(newUnit, baseUnits);
		while (highWins > expectWins) {
			highCost *= 2;
			newUnit.setCost(highCost);
			highWins = assessGameSeries(newUnit, baseUnits);
		}
			
		// Binary search for best cost
		while (highCost - lowCost > 1) {
			int midCost = (highCost + lowCost)/2;			
			newUnit.setCost(midCost);
			int midWins = assessGameSeries(newUnit, baseUnits);
			if (midWins < expectWins) {
				highCost = midCost;
				highWins = midWins;
			}
			else {
				lowCost = midCost;
				lowWins = midWins;
			}
		}

		// Final check for which is better
		int lowErr = lowWins - expectWins;
		int highErr = expectWins - highWins;
		newUnit.setCost(lowErr < highErr ? lowCost : highCost);
	}

	/**
	*  Run game trials versus array of enemy units.
	*  Return number of enemies bested.
	*/
	int assessGameSeries(Unit unit, Unit enemies[]) {
		int wins = 0;
		for (Unit enemy: enemies) {
			if (assessGames(unit, enemy) > 0.5)
				wins++;		
		}
		return wins;
	}

	/**
	*  Run many game trials.
	*  Return ratio of wins by first unit.
	*/
	double assessGames(Unit unit1, Unit unit2) {
		int wins = 0;
		for (int i = 0; i < trialsPerMatchup; i++) {
			oneGame(unit1, unit2);
			if (unit1 == winner) wins++;
		}
		return (double) wins/trialsPerMatchup;
	}

	/**
	*  Play out one game.
	*/
	void oneGame (Unit unit1, Unit unit2) {

		// Set up game
		initBattlefield();
		initUnitsByBudget(unit1, unit2);

		// Initiative for unit2 to start
		if (d6() > 3)
			oneTurn(unit2, unit1);
		
		// Battle until one side wins
		while (winner == null) {
			oneTurn(unit1, unit2);
			if (winner != null) break;
			oneTurn(unit2, unit1);
		}

		// Report on winner
		reportDetail("* WINNER *: " + winner);
	}

	/**
	*  Initialize battlefield (terrain, weather, distance, etc.).
	*/
	void initBattlefield () {
		randomizeTerrain();
		randomizeWeather();
		distance = 25 + (int) (Math.random() * 25);
		reportDetail("Terrain: " + terrain);
		reportDetail("Weather: " + weather);
		reportDetail("Distance: " + distance);
		inContact = false;
		winner = null;
	}

	/**
	*  Randomize budget & initialize opposing units.
	*/
	void initUnitsByBudget (Unit unit1, Unit unit2) {

		// Get random budget
		int range = budgetMax - budgetMin;
		int budget = budgetMin + (int)(Math.random() * range);

		// Set budget to a multiple of the pricier unit
		if (packBudgetToMax) {
			int maxCost = Math.max(unit1.getCost(), unit2.getCost());
			budget = closestMultiple(maxCost, budget);
		}

		// Set units
		initUnit(unit1, budget);
		initUnit(unit2, budget);
		
		// Report
		reportDetail("Budget: " + budget);
		reportDetail("Units: " + unit1 + " vs. " + unit2);
	}

	/**
	*  Initialize one unit by budget.
	*/
	void initUnit (Unit unit, int budget) {

		// Buy any attached hero
		if (unit.hasHero()) {
			Hero hero = unit.getHero();
			if (budget >= hero.getCost()) {
				initUnit(hero, hero.getCost());
				budget -= hero.getCost();
			}
			else {
				initUnit(hero, 0);			
			}
		}

		// Buy silver weapons if needed
		int cost = unit.getCost();
		if (useSilverWeapons && !(unit instanceof Hero)
				&& !unit.hasKeyword(Keyword.SilverToHit) && unit.getHealth() < 4) {
			cost += (unit.hasMissiles() ? 2 : 1);
		}

		// Buy & set up normal figures
		unit.setFigures(budget/cost);
		setRanksAndFiles(unit);

		// Set visibility
		boolean invisible = unit.hasKeyword(Keyword.Invisibility)
				|| (unit.hasKeyword(Keyword.HideInWoods) && terrain == Terrain.Woods);
		unit.setVisible(!invisible);

		// Prepare any special abilities
		unit.refreshCharges();
	}

	/**
	*  Set ranks and files for a unit.
	*/
	void setRanksAndFiles (Unit unit) {
		
		// For maneuverability, set all files to max 5
		int files = Math.min(5, unit.getFigures());
		unit.setFiles(files);
	}

	/**
	*  Play out one turn of action.
	*/
	void oneTurn (Unit unit1, Unit unit2) {

		// Initialize
		unit2.clearFigsLostInTurn();

		// Take action by type
//		if (unit1.hasAnyWizard())
//			oneTurnWizard(unit1, unit2); else

		if (unit1.hasMissiles())
			oneTurnRanged(unit1, unit2);
		else
			oneTurnMelee(unit1, unit2);

		// Check morale
		checkMorale(unit2);

		// Check regeneration
		if (unit2.hasKeyword(Keyword.Regeneration))
			unit2.regenerate();
			
		// Check for victory
		if (unit1.isBeaten()) winner = unit2;
		if (unit2.isBeaten()) winner = unit1;
	}

	/**
	*  Move unit forward given distance.
	*/
	void moveForward (Unit unit, int move) {
		assert(move > 0);
		distance -= move;
		if (distance < 0) distance = 0;
		reportDetail(unit + " move to dist. " + distance);
	}

	/**
	*  Apply damage from attack & report.
	*/
	void applyDamage (Unit attacker, Unit defender, boolean ranged, int hits) {
		String startReport = attacker 
			+ (ranged ? " shoot " : " attack ") + defender + ": ";

		int figsKilled = defender.takeDamage(hits);

		String endReport = figsKilled + " fig" 
			+ (figsKilled == 1 ? "" : "s") + " killed";
		reportDetail(startReport + endReport);
	}

	/**
	*  Play out one turn for melee troops.
	*/
	void oneTurnMelee (Unit unit1, Unit unit2) {
	
		// Move towards contact
		if (distance > 0) {
			moveForward(unit1, Math.min(getMove(unit1), distance));
		}

		// Expand frontage if useful
		else {
			if (unit1.getRanks() > 1 && unit1.getWidth() < unit2.getPerimeter()) {
				int newFiles = Math.min(unit1.getFiles() + 6, unit1.getFigures());
				unit1.setFiles(newFiles);
			}		
		}

 		// Attack if in contact
 		if (distance == 0) {
			checkPikeInterrupt(unit1, unit2);
			if (!unit1.isBeaten()) {
	 			meleeAttack(unit1, unit2);
			}
			inContact = true;
 		}
	}

	/**
	*  Check for pikes interrupt attack on defense.
	*/
	void checkPikeInterrupt (Unit unit1, Unit unit2) {
		if (isPikeAvailable(unit2) 
				&& !(Math.random() < pikeFlankingChance)) {
			reportDetail("** PIKES INTERRUPT ATTACK **");
			pikesInterrupt = true;
			unit1.clearFigsLostInTurn();
			meleeAttack(unit2, unit1);
			checkMorale(unit1);
			pikesInterrupt = false;
		}
	}

	/**
	*  Play out one turn for ranged troops (AI-flavored).
	*/
	void oneTurnRanged (Unit unit1, Unit unit2) {
		int minDist = minDistanceToShoot(unit1, unit2);

		// If in contact or shooting impossible, melee attack
		if (distance == 0 || minDist == 0) {
			oneTurnMelee(unit1, unit2);
		}
		
		// If enemy has no missiles, stay motionless & just shoot
		else if (!unit2.hasMissiles() && !unit2.hasAnyWizard()) {
			if (distance <= minDist)
				rangedAttack(unit1, unit2, true);
		}
		
		// Possible charge to melee to reduce enemy missile fire
		else if (attackAdvantageRatio(unit1, unit2) > 1) {
			oneTurnMelee(unit1, unit2);
		}
		
		// If in range, full attack
		else if (distance <= minDist) {
			rangedAttack(unit1, unit2, true);
		}
		
		// If enemy outranges us, get in range asap
		else if (minDist < minDistanceToShoot(unit2, unit1)
				|| (unit2.hasAnyWizard() && minDist < 24)) {
			int gap = distance - minDist;
			moveForward(unit1, Math.min(getMove(unit1), gap));
			if (distance <= minDist && gap <= getMove(unit1)/2 /*&& unit1.getROF() > 1*/) 
				rangedAttack(unit1, unit2, false);
		}
		
		// Half-move to range if we can shoot
		else if (getMove(unit1)/2 > 0 /*&& unit1.getROF() > 1*/) {
			int gap = distance - minDist;
			moveForward(unit1, Math.min(getMove(unit1)/2, gap));
			if (distance <= minDist)
				rangedAttack(unit1, unit2, false);
		}
		
		// Just get in range
		else if (getMove(unit1) > 0) {
			int gap = distance - minDist;
			moveForward(unit1, Math.min(getMove(unit1), gap));
		}
		
		// Else force move 1"
		else {
			assert(getMove(unit1) == 1);
			moveForward(unit1, 1);
		}		
	}

	/**
	*  Compute attack advantage ratio.
	*    Like an odds ratio for melee/range attack rate.
	*    Over 1 prefer melee; under 1 prefer ranged.
	*    Both units must have missiles (else divide by zero).
	*/
	double attackAdvantageRatio (Unit unit1, Unit unit2) {
		int rateOfMelee1 = meleeAttackDice(unit1, unit2, 1);
		int rateOfMelee2 = meleeAttackDice(unit2, unit1, 1);
		double atkAdvantage1 = (double) rateOfMelee1 / unit1.getRate();
		double atkAdvantage2 = (double) rateOfMelee2 / unit2.getRate();
		return atkAdvantage1 / atkAdvantage2;
	}

	/**
	*  Find minimum distance to have any chance of shooting enemy.
	*/
	int minDistanceToShoot (Unit unit1, Unit unit2) {
		int baseToHit = unit2.getArmor() - unit1.getHealth() / 3 
			- miscAtkBonus(unit1, unit2, true);
		if (baseToHit > 6 || !terrainPermitShots())
			return 0;                     	// Melee only
		else if (baseToHit == 6 && useRangePenalty)
			return unit1.getRange() / 2;     // Short only
		else
			return unit1.getRange();         // Full range
	}

	/**
	*  Does terrain and weather permit shooting?
	*/
	boolean terrainPermitShots () {
		return (!(weather == Weather.Stormy
			|| terrain == Terrain.Woods || terrain == Terrain.Gulley));	
	}

	/**
	*  Find maximum distance to move, including terrain & weather.
	*/
	int getMove (Unit unit) {
		int moveCost;

		// Terrain mods
		switch (terrain) {
			default: moveCost = 1; break;
			case Hill: case Gulley: // Assume uphill; +1 step per 2"
			case Rough: case Woods: moveCost = 2; break;
			case Marsh: moveCost = 3; break;
			case Stream: moveCost = 4; break;
		}
			
		// Weather mods
		if (weather == Weather.Stormy)
			moveCost *= 2;		

		// Mounted penalties double
		if (moveCost > 1 && unit.hasKeyword(Keyword.Mounted))
			moveCost *= 2;

		// Flyers ignore everything
		if (unit.hasKeyword(Keyword.Flying))
			moveCost = 1;
				
		// Swim in stream at half-speed
		if (unit.hasKeyword(Keyword.Swimming) && terrain == Terrain.Stream)
			moveCost = 2;

		// Return move (at least 1 inch)
		int move = unit.getMove() / moveCost;
		if (move < 1) move = 1;
		return move;
	}

	/**
	*  Play out one melee attack.
	*/
	void meleeAttack (Unit attacker, Unit defender) {
		makeVisible(attacker);

// 		// Check for heroes attacking
// 		if (attacker.hasHero()) {
// 			heroMeleeAttack(attacker.getHero(), defender);
// 		}
// 		else if (attacker instanceof Hero) {
// 			heroMeleeAttack((Hero) attacker, defender);
// 			return;
// 		}

		// Check for defender immune
		if (isAttackImmune(attacker, defender))
			return;

		// Shoot in melee special ability (e.g., elephant archers)
		if (attacker.hasKeyword(Keyword.ShootInMelee)) {
			if (minDistanceToShoot(attacker, defender) > 0) {
				rangedAttack(attacker, defender, false);
			}
		}

		// Compute figures & dice in attack
		// Normally assumes wrap, but no rear bonus
		double atkWidth = attacker.getWidth();
		double defWidth = inContact ? defender.getPerimeter() : defender.getWidth();
		int figsAtk = (atkWidth <= defWidth ? attacker.getFiles() :
			(int) Math.ceil(defWidth / attacker.getFigWidth()));
		if ((defender instanceof Hero) && (figsAtk > defender.getFigures()))
			figsAtk = defender.getFigures();
		int atkDice = meleeAttackDice(attacker, defender, figsAtk);

// 		// Compute figures & dice in attack
// 		double atkWidth = attacker.getFiles() * attacker.getFigWidth();
// 		double defWidth = defender.getFiles() * defender.getFigWidth();
// 		int figsAtk = (atkWidth <= defWidth ? attacker.getFiles() :
// 			(int) Math.ceil(defWidth / attacker.getFigWidth()));
// 		if ((defender instanceof Hero) && (figsAtk > defender.getFiles()))
// 			figsAtk = defender.getFiles();
// 		int atkDice = meleeAttackDice(attacker, defender, figsAtk);

// 		// Check dragon breath attack
// 		if (attacker.nameStarts("Dragon")
// 				&& attacker.getCharges() > 0)
// 			dragonBreathAttack(attacker, defender, figsAtk);

// 		// Check hell hound breath attack
// 		if (attacker.nameStarts("Hell Hound"))
// 			hellHoundBreathAttack(attacker, defender, figsAtk);

		// Roll attack dice
		int numHits = 0;
		int atkBonus = miscAtkBonus(attacker, defender, false);
		for (int i = 0; i < atkDice; i++) {
			if (rollToHit(defender.getArmor(), attacker.getHealth(), atkBonus))
				numHits++;
		}

		// Apply damage
		int damagePerHit = attacker.getDamage();
		if (pikesInterrupt) {
			damagePerHit *= 2;		
		}
		if (useDamageCeilingByHealth)
			damagePerHit = Math.min(damagePerHit, defender.getHealth());
		int damageTotal = numHits * damagePerHit;
		applyDamage(attacker, defender, false, damageTotal);
	}

	/**
	*  Play out one ranged attack.
	*/
	void rangedAttack (Unit attacker, Unit defender, boolean fullROF) {
		makeVisible(attacker);

		// Check for defender immune
		if (isAttackImmune(attacker, defender))
			return;

		// Compute figures & dice in attack
		int figsAtk = attacker.getFigures();
		//int figsAtk = (attacker.getRanks() <= 3 ?
		//	attacker.getFigures() : attacker.getFiles() * 3);
		if (distance == 0) { // shoot in melee ability
			figsAtk = attacker.getFiles();
		}			
		int atkDice = figsAtk * attacker.getRate();
		if (!fullROF) atkDice /= 2;

		// Determine range modifier (if any)
		int rangeMod = 0;
		if (distance > attacker.getRange())
			return; // out-of-range
		if (useRangePenalty && distance > attacker.getRange()/2)
			rangeMod = -1;

		// Roll attack dice
		int numHits = 0;
		int atkBonus = miscAtkBonus(attacker, defender, true) + rangeMod;
		for (int i = 0; i < atkDice; i++) {
			if (rollToHit(defender.getArmor(), attacker.getHealth(), atkBonus))
				numHits++;
		}
		
		// Apply damage
		int bonus = attacker.hasKeyword(Keyword.StoneBonus) ? 1 : 0;
		int damagePerHit = attacker.getDamage() + bonus;
		if (useDamageCeilingByHealth)
			damagePerHit = Math.min(damagePerHit, defender.getHealth());
		int damageTotal = numHits * damagePerHit;
		applyDamage(attacker, defender, true, damageTotal);
	}

	/**
	*  Count melee attack dice (with special modifiers).
	*/
	int meleeAttackDice (Unit attacker, Unit defender, int figsAtk) {

		// Initialize
		int atkDice = figsAtk * attacker.getAttacks();

		// Mounted gets half dice in bad terrain
		if (attacker.hasKeyword(Keyword.Mounted) &&
				(terrain != Terrain.Open || weather == Weather.Stormy)) {
			atkDice /= 2;
		}

		// Mounted gets 3 dice (+50%) on first attack in good terrain
		if (useChargeBonus
				&& !inContact
				&& attacker.hasKeyword(Keyword.Mounted)
				&& (terrain == Terrain.Open && weather != Weather.Stormy)) {
			atkDice += atkDice/2;
		}

// 		// Pikes first attack gets multiplied dice
// 		if (isPikeAvailable(attacker)) {
// 			atkDice *= (attacker.getRanks() == 1 ? 2 : 4);
//			reportDetail("** PIKE SPECIAL ATTACK **");
// 		}
				
// 		// Ghoul paralysis effectively 3 dice vs. 1HD heroes
// 		if (attacker.nameStarts("Ghouls") && (defender instanceof Hero)
// 				&& (!defender.nameContains("Elf")) && (defender.getHD() == 1)) {
// 			atkDice *= 3;
// 		}

		// Return (at least 1 die)
		return Math.max(atkDice, 1);
	}

	/**
	*  Find miscellaneous to-hit bonuses.
	*/
	int miscAtkBonus (Unit attacker, Unit defender, boolean ranged) {
		int bonus = 0;

// 		// Charging mounts bonus
// 		if (useChargeBonus
// 				&& !ranged
// 				&& attacker.hasKeyword(Keyword.Mounted)
// 				&& (terrain == Terrain.Open && weather != Weather.Stormy)) {
// 			bonus += 1;
// 		}

 		// Pike to-hit bonus vs. large targets
 		if (isPikeAvailable(attacker)
				&& !ranged
				&& (defender.hasKeyword(Keyword.Mounted)
					|| defender.getFigWidth() >= 1.5)) {
			bonus += 1;
 		}

		// Orcs & goblins penalty in sunlight
		if (attacker.hasKeyword(Keyword.LightWeakness) && weather == Weather.Sunny)
			bonus -= 1;

		// Solo heroes attacked in normal melee
		if (!ranged && (defender instanceof Hero) && !(attacker instanceof Hero))
			bonus += 1;

		// Rainy day weather missile penalty
		if (weather == Weather.Rainy && ranged) 
			bonus -= 1;

		// Mounted units mostly attack at half listed HD
		// TODO: This is super hacky!
		if (attacker.hasKeyword(Keyword.Mounted)) {
			if (attacker.getAttacks() >= 4) { // elephants
				if (ranged)
					bonus -= 2;
			}
			else {
				bonus -= attacker.getHealth()/3 - attacker.getHealth()/2/3;
			}		
		}

 		// Halfling ranged bonus
 		if (attacker.hasKeyword(Keyword.SlingBonus) && ranged) {
			bonus += 1;
		}
		
 		// Dwarf dodge giants
 		if (attacker.hasKeyword(Keyword.GiantClass) 
				&& defender.hasKeyword(Keyword.DodgeGiants)) {
 			bonus -= 1;
		}

		// Extra shield bonus vs. pikes & missiles
		if (useShieldBonus && defender.hasKeyword(Keyword.Shields)) {
			if (ranged || attacker.hasKeyword(Keyword.Pikes)) {
				bonus -= 1;
			}		
		}

// 		// Undead (mid-level) in sunlight
// 		if (weather == Weather.Sunny && (attacker.nameStarts("Ghoul")
// 				|| attacker.nameStarts("Wight") || attacker.nameStarts("Wraith")))
// 			bonus -= 1;
			
// 		// Giant rats with low 1d3 damage
// 		if (attacker.nameStarts("Rats")) bonus -= 1;

		return bonus;
	}
	
	/**
	*  Is the defender immune to this attack?
	*/
	boolean isAttackImmune (Unit attacker, Unit defender) {
		assert(!(attacker instanceof Hero));
		if (!defender.isVisible())
			return true; // Invisible can't be attacked
		if (defender.hasKeyword(Keyword.SilverToHit) && !useSilverWeapons 
				&& attacker.getHealth() < 4 && !attacker.hasKeyword(Keyword.SilverToHit))
			return true; // AD&D rule vs. silver
		if (defender.hasKeyword(Keyword.MagicToHit)
				&& attacker.getHealth() < 6 && !attacker.hasKeyword(Keyword.MagicToHit))
			return true; // AD&D rule vs. +2 to hit
		return false;
	}

	/**
	*  Is this unit able to make the special pike attack?
	*/
	boolean isPikeAvailable (Unit unit) {
		return unit.hasKeyword(Keyword.Pikes)
			&& terrain == Terrain.Open
			&& !inContact;
	}

	/**
	*  Roll to hit for one attack.
	*/
	boolean rollToHit (int AH, int HD, int mods) {
		int roll = d6();
		int target = AH - HD/3 - mods;
		return roll >= target;
	}

	/**
	*  Check morale (sets field if failed).
	*/
	void checkMorale (Unit unit) {
		if (unit.getFigures() == 0 
			|| unit.getFigsLostInTurn() == 0) return;
		if (unit.isFearless()) return;

		// Get terms
		int roll = d6() + d6();
		int hitDice = unit.getHealth();
		int rateOfLoss = unit.getFigures() / unit.getFigsLostInTurn();
		int miscBonus = miscMoraleBonus(unit);

		// Total, report, and assessment
		int check = roll + hitDice + rateOfLoss + miscBonus;
		reportDetail("Morale check (" + unit + "): " + check);
		if (check < MORALE_TARGET) {
			unit.setRouted(true);
			reportDetail(unit + " are *ROUTED*");
		}
	}

	/**
	*  Find miscellaneous morale bonuses.
	*/
	int miscMoraleBonus (Unit unit) {
		int bonus = 0;

		// Fixed bonuses
		if (unit.hasKeyword(Keyword.MoralePlus1))
			bonus += 1;
		if (unit.hasKeyword(Keyword.MoralePlus2))
			bonus += 2;

		// Light weakness (orcs & goblins)
		if (weather == Weather.Sunny && unit.hasKeyword(Keyword.LightWeakness))
			bonus -= 1;

		// Optional stuff
		if (useOptionalMoraleMods) {

			// Leadership
			bonus += unit.hasHero() ? 1 : 0;

			// Extra Ranks
			bonus += Math.min(unit.getRanks(), unit.getFiles()) - 1;

			// Alignment
			switch (unit.getAlignment()) {
				case Lawful: bonus += 1; break;
				case Chaotic: bonus -= 1; break;
			}

		}
		return bonus;		
	}
	
	/**
	*  Randomize terrain.
	*    Assume just one terrain type across entire field.
	*    Percents match coverage of entire table.
	*/
	void randomizeTerrain () {
		int roll = (int) (Math.random() / terrainMultiplier * 100);

		// These percents are for entire table
		if (roll < 1) terrain = Terrain.Gulley;
		else if (roll < 3) terrain = Terrain.Rough;
		else if (roll < 9) terrain = Terrain.Hill;
		else if (roll < 17) terrain = Terrain.Woods;
		else if (roll < 20) terrain = Terrain.Marsh;
		else if (roll < 22) terrain = Terrain.Stream;
		else terrain = Terrain.Open;		
		
		// Note: Pond type is not used (impassable)
	}

	/**
	*  Randomize weather.
	*/
	void randomizeWeather () {
		int roll = d6() + d6();
		if (roll <= 7) weather = Weather.Sunny;
		else if (roll <= 9) weather = Weather.Cloudy;
		else if (roll <= 11) weather = Weather.Rainy;
		else weather = Weather.Stormy;
	}

	/**
	*  Roll a 6-sided die.
	*/
	int d6 () {
		return (int) (Math.random() * 6) + 1;
	}

	/**
	*  Find greatest common denominator of two integers.
	*/
	int gcd (int a, int b) {
		while (b > 0) {
			int temp = b;
			b = a % b;
			a = temp;		
		}	
		return a;
	}
	
	/**
	*  Find least common multiple of two integers.
	*/
	int lcm (int a, int b) {
		return a * (b / gcd(a, b));
	}

	/**
	*  Find the closest multiple of two integers to some target value.
	*/
	int closestCommonMultiple (int a, int b, int target) {
		return closestMultiple(lcm(a, b), target);
	}

	/**
	*  Find the closest multiple of one integer to some target value.
	*/
	int closestMultiple (int num, int target) {
		int q = target/num;
		if (q == 0) return num;
		int lowerBound = q * num;
		int upperBound = (q+1) * num;
		int lowerError = target - lowerBound;
		int upperError = upperBound - target;
		return lowerError <= upperError ? lowerBound : upperBound;
	}

	//-----------------------------------------------------------------
	//  Methods for special abilities
	//  (Hero attacks, wizard spells, dragon breath, etc.)
	//-----------------------------------------------------------------

	/**
	*  Make an invisible attacker become visible.
	*/
	void makeVisible (Unit atk) {
		if (!atk.isVisible()) {
			atk.setVisible(true);		
			reportDetail(atk + " become visible!");
		}
	}

// 	/**
// 	*  Play out one hero melee attack.
// 	*/
// 	void heroMeleeAttack (Hero attacker, Unit defender) {
// 		if (attacker.getFigures() < 1) return;
// 
// 		// Skip wizards in melee
// 		if (attacker.isWizard())
// 			return;
// 
// 		// Compute figures in attack
// 		double atkWidth = attacker.getFiles() * attacker.getFigWidth();
// 		double defWidth = defender.getFiles() * defender.getFigWidth();
// 		int figsAtk = (atkWidth <= defWidth ? attacker.getFiles() :
// 			(int) Math.ceil(defWidth / attacker.getFigWidth()));
// 
// // 		// Check dragon breath attack
// // 		if (attacker.nameStarts("Dragon")
// // 				&& attacker.getCharges() > 0)
// // 			dragonBreathAttack(attacker, defender, figsAtk);
// 
// 		// Roll attack dice
// 		int numHits = 0;
// 		for (int i = 0; i < figsAtk; i++) {
// 			if (d6() >= attacker.getAttacks()) {
// 				numHits += Math.min(attacker.getDamage(), defender.getHD());
// 			}
// 		}
// 
// 		// Apply damage
// 		applyDamage(attacker, defender, false, numHits);
// 	}
// 
// 	/**
// 	*  Play out one turn with a wizard figure.
// 	*    No move so wizard can shoot full; ranged or melee as capable.
// 	*    Note: Currently cannot handle wizard-vs-wizard engagements.
// 	*/
// 	void oneTurnWizard (Unit unit1, Unit unit2) {
// 
// 		// All-wizard unit
// 		if (unit1.isWizard()) {
// 			wizardSpellAttack((Hero)unit1, unit2);
// 		}
// 
// 		// Troops with attached wizard
// 		else {
// 			if (unit1.hasMissiles() && distance > 0) {
// 				rangedAttack(unit1, unit2, true);
// 			}
// //			else if (distance <= meleeRange(unit1)) {
// 			else if (distance == 0) {
// 				meleeAttack(unit1, unit2);
// 			}	
// 			oneTurnWizard(unit1.getHero(), unit2);
// 		}
// 	}
// 
// 	/**
// 	*  Play out one magic area attack.
// 	*    Adjudicates one hit on one basic figure.
// 	*    (If target is attached hero, identify that as target.)
// 	*    @return Damage taken.
// 	*/
// 	int magicAreaAttack (Unit target, int damage, boolean getSave) {
// 		if (!(target instanceof Hero)) {
// 			int save = 0;
// 			if (getSave) save = (int) (Math.random() * 3) + 1; // d3
// 			int damageTaken = Math.min(damage - save, target.getHD());
// 			if (target.nameStarts("Dragon Flock")) damageTaken /= 4;
// 			return (damageTaken > 0 ? damageTaken : 0);
// 		}
// 		else {
// 			int save = d6();
// 			return (save < damage/2 ? 1 : 0);
// 		}
// 	}
// 
// 	/**
// 	*  Play out one dragon breath attack.
// 	*/
// 	void dragonBreathAttack (Unit dragon, Unit target, int figsAtk) {
// 		assert(dragon.getCharges() > 0);
// 		if (dragon.getFigures() < 1) return;
// 		reportDetail("* DRAGON BREATH ATTACK *");
// 		dragon.decrementCharges();
// 
// 		// Get damage by type (adults) & check immunities
// 		boolean getSave = true;
// 		if (dragonTargetImmune(dragon, target)) return;
// 		int damage = dragonBreathDamage(dragon);
// 		if ((dragon.nameContains("Red") || dragon.nameContains("Gold"))
// 				&& (target.nameStarts("Treant") || target.nameStarts("Tree")))
// 			getSave = false;			
// 
// 		// If dragon flock, determine figures hit in 2x3"
// 		int figsHit = 1;
// 		if (dragon.nameStarts("Dragon Flock")) {
// 			int figHitWidth = Math.min(
// 				(int)(3.0/target.getFigWidth()), target.getFiles());
// 			int figHitLength = Math.min(
// 				(int)(2.0/target.getFigLength()), target.getRanks());
// 			figsHit = figHitWidth * figHitLength;
// 		}
// 		
// 		// Assess hits
// 		int hits = 0;
// 		for (int j = 0; j < figsHit; j++) {
// 			for (int i = 0; i < figsAtk; i++) {
// 				hits += magicAreaAttack(target, damage, getSave);
// 			}
// 		}
// 		applyDamage(dragon, target, true, hits);
// 	}
// 
// 	/**
// 	*  Find dragon breath attack damage.
// 	*/
// 	int dragonBreathDamage (Unit dragon) {
// 		int damage = 0;
// 
// 		if (dragon.nameContains("Adult")) {
// 			if (dragon.nameContains("White")) damage = 7;
// 			else if (dragon.nameContains("Black")) damage = 8;
// 			else if (dragon.nameContains("Green")) damage = 9;
// 			else if (dragon.nameContains("Blue")) damage = 10;
// 			else if (dragon.nameContains("Red")) damage = 11;
// 			else if (dragon.nameContains("Gold")) damage = 12;
// 			// Note book used 9, 10, 11 for latter
// 		}
// 
// 		else if (dragon.nameContains("Very Old")) {
// 			if (dragon.nameContains("White")) damage = 10;
// 			else if (dragon.nameContains("Black")) damage = 12;
// 			else if (dragon.nameContains("Green")) damage = 14;
// 			else if (dragon.nameContains("Blue")) damage = 15;
// 			else if (dragon.nameContains("Red")) damage = 17;
// 			else if (dragon.nameContains("Gold")) damage = 19;
// 			if (dragon.nameContains("Gold Large")) damage = 20;
// 		}
// 		
// 		else if (dragon.nameContains("Old")) {
// 			if (dragon.nameContains("White")) damage = 9;
// 			else if (dragon.nameContains("Black")) damage = 10;
// 			else if (dragon.nameContains("Green")) damage = 11;
// 			else if (dragon.nameContains("Blue")) damage = 13;
// 			else if (dragon.nameContains("Red")) damage = 14;
// 			else if (dragon.nameContains("Gold")) damage = 16;
// 		}
// 		
// 		return damage;
// 	}
// 
// 	/**
// 	*  Is the target immune to this dragon breath attack?
// 	*/
// 	boolean dragonTargetImmune (Unit dragon, Unit target) {
// 		boolean retval = false;
// 		
// 		if (dragon.nameContains("White")) {
// 			if (target.nameStarts("Giant, Frost")) retval = true;
// 		}
// 		else if (dragon.nameContains("Black")) {
// 		}
// 		else if (dragon.nameContains("Green")) {
// 		}
// 		else if (dragon.nameContains("Blue")) {
// 			if (target.nameStarts("Giant, Storm")) retval = true;
// 		}
// 		else if (dragon.nameContains("Red")) {
// 			if (target.nameStarts("Giant, Fire")) retval = true;
// 		}
// 		else if (dragon.nameContains("Gold")) {
// 		}
// 		
// 		return retval;
// 	}
// 
// 	/**
// 	*  Play out one hell hound breath attack.
// 	*/
// 	void hellHoundBreathAttack (Unit hounds, Unit target, int figsAtk) {
// 		if (hounds.getFigures() < 1) return;
// 		reportDetail("* HELL HOUND BREATH ATTACK *");
// 
// 		// Get damage & check immunities
// 		int damage = hounds.getHD();
// 		if (target.nameStarts("Giant, Fire")) return;
// 
// 		// Check save ability
// 		boolean getSave = true;
// 		if (target.nameStarts("Treant") 
// 				|| target.nameStarts("Tree"))
// 			getSave = false;				
// 		
// 		// Assess hits
// 		int hits = 0;
// 		for (int i = 0; i < figsAtk; i++) {
// 			hits += magicAreaAttack(target, damage, getSave);
// 		}
// 		applyDamage(hounds, target, true, hits);
// 	}
// 
// 	/**
// 	*  Did this missile spell hit the target?
//		*  Used for fireballs & lightning bolts.
// 	*  Assumes variation of +/-1 inch for missile.
// 	*/
// 	boolean missileSpellHit (Unit target) {
// 		double targetDepth = target.getFigLength() * target.getRanks();
// 		if (targetDepth <= 0.0)
// 			return false;
// 		else if (targetDepth <= 1.0) 
// 			return (d6() <= 2);	// 2-in-6
// 		else if (targetDepth <= 2.0)
// 			return (d6() <= 4);  // 4-in-6
// 		else
// 			return true;
// 	}
// 
// 	/**
// 	*  Play out one wizard spell attack.
//		*  Assumes wizard motionless, so full fire.
// 	*/
// 	void wizardSpellAttack (Hero wizard, Unit target) {
// 		assert(wizard.isWizard());
// 		if (wizard.getFigures() < 1) return;
// 
// 		// Assume each greater-spell charge used for "Death Spell"
// 		if (wizard.getCharges() > 0 && distance <= 12
// 				&& target.getHD() <= 8 && !(target instanceof Hero)) {
// 			reportDetail("* WIZARD DEATH SPELLS *");
// 			int hits = 0;
// 			for (int i = 0; i < wizard.getFigures(); i++)
// 				hits += 4 + wizard.getRanks() / 3;
// 			applyDamage(wizard, target, true, hits);
// 			wizard.decrementCharges();
// 		}
// 
// 		// Otherwise shoot two wand-based "Fireballs"
// 		else if (distance <= 24) {
// 			reportDetail("* WIZARD FIREBALLS *");
// 
// 			// Check damage, immunity, saves
// 			int damage = 6;
// 			boolean getSave = true;
// 			if (target.nameStarts("Giant, Fire")) 
// 				damage = 0;
// 			if (target.nameStarts("Treant") 
// 					|| target.nameStarts("Tree"))
// 				getSave = false;				
// 			
// 			// Generate total hits
// 			int hits = 0;
// 			for (int i = 0; i < wizard.getFigures() * 2; i++) {
// 				if (missileSpellHit(target))
// 					hits += magicAreaAttack(target, damage, getSave);
// 			}
// 			applyDamage(wizard, target, true, hits);		
// 		}
// 	}
}


/*
=====================================================================
LICENSING INFORMATION

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

The author may be contacted by email at: delta@superdan.net
=====================================================================
*/

