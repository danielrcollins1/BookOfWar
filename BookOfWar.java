import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.io.IOException; 

/******************************************************************************
*  Book of War simulation for cost-balancing purposes.
*
*  @author   Daniel R. Collins
*  @since    2009-11-27
******************************************************************************/

public class BookOfWar {
	enum Weather { Sunny, Cloudy, Rainy };
	enum Terrain { Open, Gulley, Rough, Hill, Woods, Marsh, Stream, Pond };
	enum SimMode { TableAssess, AutoBalance, FullBalance, ZoomGame };

	//-----------------------------------------------------------------
	//  Filename constants
	//-----------------------------------------------------------------

	/** Basic unit list filename. */
	private static final String BASIC_UNIT_FILE = "UnitTypes.csv";
	
	/** Solo unit list filename. */
	private static final String SOLO_UNIT_FILE = "SoloTypes.csv";
	
	//-----------------------------------------------------------------
	//  Parameter defaults
	//-----------------------------------------------------------------

	/** Default trials per matchup. */
	private static final int DEFAULT_TRIALS_PER_MATCHUP = 10000;

	/** Default simulation mode. */
	private static final SimMode DEFAULT_SIM_MODE = SimMode.TableAssess;

	//-----------------------------------------------------------------
	//  Constant fields
	//-----------------------------------------------------------------

	/** Budget minimum (basis 50). */
	private final int budgetMin = 50;

	/** Budget maximum (basis 100). */
	private final int budgetMax = 100;

	/** Balances swords vs. pikes & cavalry (basis 1.00). */
	private final double terrainMultiplier = 1.00;

	/** Balances pikes vs. swords & cavalry (basis 0.20). */
	private final double pikeFlankingChance = 0.30;

	/** Limit per-hit damage by target's health? */
	private final boolean capDamageByHealth = false;

	/** Buy silver weapons for all troop types? */
	private final boolean useSilverWeapons = false;
	
	/** Target for morale check success (per Vol-1, p. 12). */
	private static final int MORALE_TARGET = 9;
	
	//-----------------------------------------------------------------
	//  Out-of-game settings
	//-----------------------------------------------------------------

	/** Random number generator. */
	private Random random;

	/** Mode of action for simulator. */
	private SimMode simMode;

	/** List of basic unit types. */
	private List<Unit> unitList;

	/** List of solo unit types. */
	private List<Solo> soloList;

	/** Assess to this unit number (unit types 1 to n). */
	private int assessUnitNum;

	/** Base unit set for comparisons (unit types 1 to n). */
	private int baseUnitNum;

	/** Number of trials per matchup. */
	private int trialsPerMatchup;

	/** Units for zoom-in game (1-based index into Units list). */
	private int zoomGameUnit1, zoomGameUnit2;

	/** Balance the Solo unit types? */
	private boolean soloBalancing;

	/** Use round number prices in autobalancer? */
	private boolean usePreferredValues;
	
	/** Print results table in CSV format? */
	private boolean printFormatCSV;

	/** Flag to escape after startup procedures. */
	private boolean exitAfterStartup;
	
	//-----------------------------------------------------------------
	//  In-game variables
	//-----------------------------------------------------------------

	/** Distance between opposing units. */
	private int distance;

	/** Weather category for battle. */
	private Weather weather;

	/** Uniform terrain for battle. */
	private Terrain terrain;
	
	/** Are the units already in contact? */
	private boolean priorContact;

	/** Are pikes making an interrupting defense now? */
	private boolean pikesInterrupt;

	//-----------------------------------------------------------------
	//  Constructor(s)
	//-----------------------------------------------------------------

	/**
	*  Construct the simulator.
	*/
	public BookOfWar() {
		random = new Random();
		simMode = DEFAULT_SIM_MODE;
		trialsPerMatchup = DEFAULT_TRIALS_PER_MATCHUP;
		loadBasicUnits();
		loadSoloUnits();
	}

	/**
	*  Copy constructor.
	*  Copies only out-of-game setings.
	*/
	public BookOfWar(BookOfWar src) {
		random = new Random();			
		simMode = src.simMode;
		unitList = src.unitList;
		soloList = src.soloList;
		assessUnitNum = src.assessUnitNum;
		baseUnitNum = src.baseUnitNum;
		trialsPerMatchup = src.trialsPerMatchup;
		zoomGameUnit1 = src.zoomGameUnit1;
		zoomGameUnit2 = src.zoomGameUnit2;
		usePreferredValues = src.usePreferredValues;
		printFormatCSV = src.printFormatCSV;
		exitAfterStartup = src.exitAfterStartup;
	}

	//-----------------------------------------------------------------
	//  Methods
	//-----------------------------------------------------------------

	/**
	*  Run the main method.
	*/
	public static void main(String[] args) {
		BookOfWar book = new BookOfWar();
		if (!book.exitAfterStartup) {
			book.parseArgs(args);
			book.checkArgUnitNums();
			if (!book.exitAfterStartup) {
				book.run();
			}
			else {
				book.printUsage();
			}		
		}
	}

	/**
	*  Print usage.
	*/
	public void printUsage() {
		System.out.println();
		System.out.println("Usage: BookOfWar [options]");
		System.out.println("  Options include:");
		System.out.println("\t-a assess up to the nth unit in database");
		System.out.println("\t-b use first n units as fixed base for comparisons");
		System.out.println("\t-m sim mode (1 = table-assess, 2 = auto-balance,\n"
									+ "\t\t 3 = full auto-balance, 4 = zoom-in game)");
		System.out.println("\t-p use preferred values in full auto-balancer");
		System.out.println("\t-s balance the solo vs. basic unit types");
		System.out.println("\t-t trials per matchup (default=" + DEFAULT_TRIALS_PER_MATCHUP + ")");
		System.out.println("\t-v print assessment table in CSV format");
		System.out.println("\t-y zoom-in game 1st unit index (1-based)");
		System.out.println("\t-z zoom-in game 2nd unit index (1-based)");
		System.out.println();
	}

	/**
	*  Parse arguments.
	*/
	public void parseArgs(String[] args) {
		for (String s: args) {
			if (s.charAt(0) == '-') {
				switch (s.charAt(1)) {
					case 'a': assessUnitNum = getParamInt(s); break;
					case 'b': baseUnitNum = getParamInt(s); break;
					case 'm': parseSimMode(s); break;
					case 'p': usePreferredValues = true; break;					
					case 's': soloBalancing = true; break;
					case 't': trialsPerMatchup = getParamInt(s); break;
					case 'v': printFormatCSV = true; break;
					case 'y': zoomGameUnit1 = getParamInt(s); break;
					case 'z': zoomGameUnit2 = getParamInt(s); break;
					default: exitAfterStartup = true; break;
				}
			}
			else {
				exitAfterStartup = true;
			}
		}
	}

	/**
	*  Get integer following equals sign in command parameter.
	*/
	int getParamInt(String s) {
		if (s.charAt(2) == '=') {
			try {
				return Integer.parseInt(s.substring(3));
			}
			catch (NumberFormatException e) {
				System.err.println("Error: Could not read integer argument: " + s);
			}
		}
		exitAfterStartup = true;
		return -1;
	}

	/**
	*  Parse the simulation mode.
	*/
	void parseSimMode(String s) {
		int num = getParamInt(s);
		switch (num) {
			case 1: simMode = SimMode.TableAssess; return;
			case 2: simMode = SimMode.AutoBalance; return;
			case 3: simMode = SimMode.FullBalance; return;
			case 4: simMode = SimMode.ZoomGame; return;
			default: exitAfterStartup = true; return;
		}
	}

	/**
	*  Read the basic unit data.
	*/
	void loadBasicUnits() {
		try {
			String[][] table = CSVReader.readFile(BASIC_UNIT_FILE);
			unitList = new ArrayList<Unit>(table.length - 1);
			for (int i = 1; i < table.length; i++) {
				unitList.add(new Unit(table[i]));
			}
		}
		catch (IOException e) {
			System.err.println("Could not read basic unit type list.");
			exitAfterStartup = true;		
		}
	}

	/**
	*  Read the solo unit data.
	*/
	void loadSoloUnits() {
		try {
			String[][] table = CSVReader.readFile(SOLO_UNIT_FILE);
			soloList = new ArrayList<Solo>(table.length - 1);
			for (int i = 1; i < table.length; i++) {
				soloList.add(new Solo(table[i]));
			}
		}
		catch (IOException e) {
			System.err.println("Could not read solo unit type list.");
			exitAfterStartup = true;		
		}
	}

	/**
	*  Post a startup failure message.
	*/
	void postStartupFailMsg(String msg) {
		System.err.println(msg);
		exitAfterStartup = true;	
	}

	/**
	*  Validate values of assessed and base unit numbers.
	*/
	void checkArgUnitNums() {
	
		// Set default if needed
		int assessMaxSize = soloBalancing 
			? soloList.size() : unitList.size();
		if (assessUnitNum == 0) {
			assessUnitNum = assessMaxSize;
		}
		
		// Check assessment set size
		if (assessUnitNum < 0) {
			postStartupFailMsg("Error: Assessed unit set must be positive (fix -a switch).");
		}
		else if (assessUnitNum > assessMaxSize) {
			postStartupFailMsg("Error: Assessed unit set must be no more than database size (fix -a switch).");
		}

		// Check base set size
		if (baseUnitNum < 0) {
			postStartupFailMsg("Error: Base unit set must be nonnegative (fix -b switch).");
		}
		else if (baseUnitNum > unitList.size()) {
			postStartupFailMsg("Error: Base unit set must be no more than database size (fix -b switch).");
		}
		
		// Check relation between sets
		if (!soloBalancing && baseUnitNum >= assessUnitNum) {
			postStartupFailMsg("Error: Assessed unit set must be more than base units (fix -a or -b switch).");
		}
	}
	
	/**
	*  Check base unit set positive (for certain cases).
	*/
	boolean checkBaseUnitsPositive() {
		if (baseUnitNum <= 0) {
			System.err.println("Error: Base unit set must be positive (fix -b switch).");
			return false;
		}
		return true;
	}	

	/**
	*  Run the simulator in selected mode.
	*/
	void run() {
 		switch (simMode) {
 			case TableAssess: assessmentTable(); break;
 			case AutoBalance: autoBalancer(); break;
			case FullBalance: fullAutoBalancer(); break;
 			case ZoomGame: zoomInGame(); break;
			default: System.err.println("Unknown simulation mode"); break;
 		}
	}

	/**
	*  Create table of assessed win percents.
	*/
	void assessmentTable() {
		if (!soloBalancing) {
			List<Unit> assessUnits = unitList.subList(0, assessUnitNum);
			makeAssessmentTable(assessUnits, assessUnits);
		}
		else {
			if (checkBaseUnitsPositive()) {
				List<Unit> assessUnits = 
					new ArrayList<Unit>(soloList.subList(0, assessUnitNum));
				List<Unit> baseUnits = unitList.subList(0, baseUnitNum);
				makeAssessmentTable(assessUnits, baseUnits);
			}
		}
	}

	/**
	*  Auto-balance unit costs.
	*/
	void autoBalancer() {
		if (checkBaseUnitsPositive()) {
			if (!soloBalancing) {
				List<Unit> assessUnits = unitList.subList(baseUnitNum, assessUnitNum);
				List<Unit> baseUnits = unitList.subList(0, baseUnitNum);
				makeAutoBalancedTable(assessUnits, baseUnits);
			}
			else {
				List<Unit> assessUnits = 
					new ArrayList<Unit>(soloList.subList(0, assessUnitNum));
				List<Unit> baseUnits = unitList.subList(0, baseUnitNum);
				makeAutoBalancedTable(assessUnits, baseUnits);
			}
		}
	}

	/**
	*  Report detail for a zoom-in game.
	*/
	void reportDetail(String s) {
		if (simMode == SimMode.ZoomGame) {
			System.out.println(s);
		}
	}
	
	/**
	*  Print to output (printf recreation for copied code).
	*/
	void printf(String s) {
		System.out.print(s);	
	}

	/**
	*  Get the preferred field separator character.
	*/
	char getSepChar() {
		return printFormatCSV ? ',' : '\t';
	}

	/**
	*  Print formatted entry in a wide table column (left-justified).
	*/
	void printWideField(String text, int fieldWidth) {
		if (printFormatCSV) {
			printf(text);
		}			
		else {
			String fieldCode = "%1$-" + fieldWidth + "s";
			printf(String.format(fieldCode, text));
		}
	}

	/**
	*  Get maximum name length in a list of units.
	*/
	int getMaxNameLength(List<Unit> pUnitList) {
		int maxLength = 0;
		for (Unit u: pUnitList) {
			int nameLength = u.getName().length();
			if (nameLength > maxLength) {
				maxLength = nameLength;
			}
		}	
		return maxLength;
	}

	/**
	*  Make general assessment table.
	*/
	void makeAssessmentTable(List<Unit> unitList1, List<Unit> unitList2) {
		assert !unitList1.isEmpty() && !unitList2.isEmpty();
  
		// Get formatting info
		String sepChar = "" + getSepChar();
		int nameColSize = getMaxNameLength(unitList1);

  		// Header
		printf("Assessed win percents "
			+ "(nominal budget " + budgetMin + "-" + budgetMax + "):\n\n");
		printWideField("", nameColSize);
		for (Unit unit: unitList2) {
			printf(sepChar + unit.getAbbreviation());
		}
		printf(sepChar + "Wins" + sepChar + "SumErr\n");

  		// Body
		double sumNormError = 0.0;
		double maxNormError = 0.0;
		Unit maxNormErrUnit = unitList1.get(0);
		for (Unit unit1: unitList1) {

			// Run simulation docket
			double[] winRates = playDocketThreads(unit1, unitList2);
			double sumErr = sumErrArray(winRates);

			// Print row name
			printWideField(unit1.getName(), nameColSize);
			for (int i = 0; i < unitList2.size(); i++) {
				printf(sepChar + (winRates[i] <= 0.5 ? "-" : "" + toPercent(winRates[i])));
			}
			printf(sepChar + countHighRates(winRates));
			printf(sepChar + toPercent(sumErr) + "\n");

			// Update global stats
			sumNormError += normalError(sumErr);
			if (normalError(sumErr) > normalError(maxNormError)) {
				maxNormError = sumErr;
				maxNormErrUnit = unit1;
			}
		}
		printf("\n");

		// Tail
		printf("Sum Normal Error: " + toPercent(sumNormError * 100) + "\n");
		printf("Maximum error unit: " + maxNormErrUnit.getName() 
			+ " (" + toPercent(maxNormError) + ")\n");
	}

	/**
	*  Make auto-balanced table of estimated best costs.
	*/
	void makeAutoBalancedTable(List<Unit> newUnits, List<Unit> baseUnits) {
		assert baseUnits != newUnits;

		// Get name field size
		int nameColSize = getMaxNameLength(newUnits);

		// Print header
		printf("Auto-balanced best cost "
			+ "(nominal budget " + budgetMin + "-" + budgetMax + "):\n\n");
		printWideField("Unit", nameColSize);
		printf(getSepChar() + "Cost\n");

		// Make the table
		for (Unit newUnit: newUnits) {
			setAutoBalancedCost(newUnit, baseUnits);		
			printWideField(newUnit.getName(), nameColSize);
			printf(getSepChar() + "" + newUnit.getCost() + "\n");
		}
	} 

	/**
	*  Set auto-balanced cost for a new unit.
	*  Searches for sumWinPctErr closest to zero (0). 
	*/
	void setAutoBalancedCost(Unit newUnit, List<Unit> baseUnits) {
		int lowCost = 1, highCost = newUnit.getCost();

		// Check lower bound for cost
		newUnit.setCost(lowCost);
		double lowCostWinPctErr = playDocket(newUnit, baseUnits);
		if (lowCostWinPctErr < 0) {
			return;
		}

		// Find upper bound for cost
		newUnit.setCost(highCost);
		double highCostWinPctErr = playDocket(newUnit, baseUnits);
		while (highCostWinPctErr > 0) {
			highCost *= 2;
			newUnit.setCost(highCost);
			highCostWinPctErr = playDocket(newUnit, baseUnits);
		}
			
		// Binary search for best cost
		while (highCost - lowCost > 1) {
			int midCost = (highCost + lowCost) / 2;			
			newUnit.setCost(midCost);
			double midWinPctErr = playDocket(newUnit, baseUnits);
			if (midWinPctErr < 0) {
				highCost = midCost;
				highCostWinPctErr = midWinPctErr;
			}
			else {
				lowCost = midCost;
				lowCostWinPctErr = midWinPctErr;
			}
		}

		// Final check for which is better
		assert lowCostWinPctErr >= 0 && highCostWinPctErr <= 0;
		int bestCost = lowCostWinPctErr < -highCostWinPctErr ? lowCost : highCost;
		newUnit.setCost(!usePreferredValues ? bestCost : PreferredValues.getClosest(bestCost));
	}

	/**
	*  Fully automatic unit cost-balancer.
	*
	*  Caution: This feature is not a complete silver bullet.
	*    - Random nature may make different suggestions on different passes.
	*    - For a very small unit list, may cyclically push costs in one direction.
	*
	*  Use tastefully; recommend estimating prices with mode-2 first.
	*/
	void fullAutoBalancer() {

		// Initialize list to balance
		printf("Initializing full auto-balancer...\n");
		List<Unit> assessUnits = unitList.subList(0, assessUnitNum);
		double oldSumNormError = playAllDockets(assessUnits);
		
		// Iterate attempts at improving some unit
		printf("Searching for improved costs...\n");
		boolean adjustedAnyUnit;
		do {
			adjustedAnyUnit = false;

			// Make shuffled list of units to test
			List<Unit> unitsToTest = new ArrayList<Unit>(
				unitList.subList(baseUnitNum, assessUnitNum));
			Collections.shuffle(unitsToTest);
				
			// Try to improve price of any unit in list
			for (Unit modUnit: unitsToTest) {
				int startCost = modUnit.getCost();
		
				// Keep trying to adjust this unit while we see improvement
				boolean adjustGain;
				do {
					adjustGain = false;

					// One step cost change in needed direction
					int oldCost = modUnit.getCost();
					double oldUnitSumErr = playDocket(modUnit, assessUnits);
					int newCost = getNewCost(oldCost, oldUnitSumErr > 0);
					modUnit.setCost(newCost);
					double newSumNormError = playAllDockets(assessUnits);

					// If this reduced error from parity, keep new cost
					if (newSumNormError < oldSumNormError) {
						printf(modUnit.getName() 
							+ (newCost > oldCost ? " raised to " : " lowered to ")
							+ modUnit.getCost() + "\n");
						oldSumNormError = newSumNormError;
						adjustGain = true;
					}
					else {
						modUnit.setCost(oldCost);
					}
				} while (adjustGain);
				
				// If we really adjusted this unit, go back to shuffle & restart
				if (modUnit.getCost() != startCost) {
					adjustedAnyUnit = true;
					break;				
				}
			}	
		} while (adjustedAnyUnit);
		
		// Print table of new values
		printf("\nFinal suggested costs:\n\n");
		List<Unit> testedUnits = 
			unitList.subList(baseUnitNum, assessUnitNum);
		int nameColSize = getMaxNameLength(testedUnits);
		printWideField("Unit", nameColSize);
		printf(getSepChar() + "Cost\n");
		for (Unit unit: testedUnits) {
			printWideField(unit.getName(), nameColSize);
			printf(getSepChar() + "" + unit.getCost() + "\n");
		}
	}

	/**
	*  Get new cost for full auto-balancer.
	*/
	int getNewCost(int oldCost, boolean up) {
		if (!usePreferredValues) {
			return up ? oldCost + 1 : oldCost - 1;
		}
		else {
			return up ? PreferredValues.inc(oldCost) : PreferredValues.dec(oldCost);
		}
	}

	/**
	*  Battle two specified unit types with detailed in-game reports.
	*/
	void zoomInGame() {
		int maxFirstIndex = soloBalancing
			? soloList.size() : unitList.size();
		if (zoomGameUnit1 <= 0 || zoomGameUnit2 <= 0) {
			System.err.println("Error: Zoom-in game requires two unit indexes (use -y and -z switches).");
		}
		else if (zoomGameUnit1 > maxFirstIndex || zoomGameUnit2 > unitList.size()) {
			System.err.println("Error: Zoom-in game has unit out of range for database.");
		}		
		else {
			Unit unit1 = soloBalancing
				? new Solo(soloList.get(zoomGameUnit1 - 1))
				: new Unit(unitList.get(zoomGameUnit1 - 1));
			Unit unit2 = new Unit(unitList.get(zoomGameUnit2 - 1));
			playGame(unit1, unit2);
		}
	}

	/**
	*  Get sum error for a double array.
	*  (Sum of differences from 0.5.)
	*/
	double sumErrArray(double[] dblArray) {
		double sumErr = 0.0;
		for (double d: dblArray) {
			sumErr += d - 0.5;
		}
		return sumErr;
	}

	/**
	*  Count ratios above 0.5 in a double array.
	*/
	int countHighRates(double[] dblArray) {
		int countHigh = 0;
		for (double d: dblArray) {
			if (d > 0.5) {
				countHigh++;
			}
		}
		return countHigh;
	}

	/**
	*  Play repeated series of every unit in a list against every other unit.
	*  Returns grand total of normalized win percent error.
	*/
	double playAllDockets(List<Unit> pUnitList) {
		double sumNormError = 0.0;
		for (Unit unit: pUnitList) {
			double error = playDocket(unit, pUnitList);
			sumNormError += normalError(error);
		}
		return sumNormError;
	}

	/**
	*  Play repeated series of one unit against a list of other units.
	*  Returns sum win percentage error for the test unit.
	*/
	double playDocket(Unit unit, List<Unit> enemies) {
		double[] results = playDocketThreads(unit, enemies);
		return sumErrArray(results);
	}

	/**
	*  Play repeated series of one unit against a list of other units.
	*  (Think of a "docket" as one season for a pro sports team.)
	*  Multithreaded: Spawns separate thread per series matchup.
	*  Returns array of win ratios for test unit vs. each enemy in list.
	*/
	double[] playDocketThreads(Unit unit, List<Unit> enemies) {

		// Run series threads
		int numOpp = enemies.size();
		Thread[] threads = new Thread[numOpp];
		SeriesRunner[] runners = new SeriesRunner[numOpp];
		for (int i = 0; i < numOpp; i++) {
			runners[i] = new SeriesRunner(this, unit, enemies.get(i));
			threads[i] = new Thread(runners[i]);
			threads[i].start();		
		}
		waitForThreads(threads);

		// Compile results array
		double[] results = new double[numOpp];
		for (int i = 0; i < numOpp; i++) {
			results[i] = runners[i].getTestUnitWinRatio();
		}		
		return results;
	}

	/**
	*  Play series of games between a pair of units.
	*  Return ratio of wins by first unit.
	*/
	double playSeries(Unit unit1, Unit unit2) {
		int unitOneWins = 0;
		for (int i = 0; i < trialsPerMatchup; i++) {
			boolean win1 = playGame(unit1, unit2);
			if (win1) {
				unitOneWins++;
			}
		}
		return (double) unitOneWins / trialsPerMatchup;
	}

	/**
	*  Play out one game.
	*  Return true if first unit wins.
	*/
	boolean playGame(Unit unit1, Unit unit2) {

		// Set up game
		initBattlefield();
		initUnitsByBudget(unit1, unit2);

		// Initiative for unit2 to start
		if (d6() > 3) {
			oneTurn(unit2, unit1);
		}
		
		// Battle until one side wins
		while (bothUnitsLive(unit1, unit2)) {
			oneTurn(unit1, unit2);
			if (bothUnitsLive(unit1, unit2)) {
				oneTurn(unit2, unit1);
			}
		}

		// Report on winner
		Unit winner = getWinner(unit1, unit2);		
		reportDetail("* WINNER *: " + winner);
		return winner == unit1;
	}

	/**
	*  Check if both units in game still live.
	*/
	boolean bothUnitsLive(Unit unit1, Unit unit2) {
		return getWinner(unit1, unit2) == null;
	}

	/**
	*  Determine victorious unit.
	*/
	Unit getWinner(Unit unit1, Unit unit2) {
		if (unit1.isBeaten()) {
			return unit2;
		}
		else if (unit2.isBeaten()) {
			return unit1;
		}
		else {
			return null;
		}
	}

	/**
	*  Initialize battlefield (terrain, weather, distance, etc.).
	*/
	void initBattlefield() {
		randomizeTerrain();
		randomizeWeather();
		distance = 25 + random.nextInt(25);
		reportDetail("Terrain: " + terrain);
		reportDetail("Weather: " + weather);
		reportDetail("Distance: " + distance);
		priorContact = false;
	}

	/**
	*  Randomize budget & initialize opposing units.
	*/
	void initUnitsByBudget(Unit unit1, Unit unit2) {

		// Get random budget
		int range = budgetMax - budgetMin;
		int budget = budgetMin + random.nextInt(range);

		// Handle units pricier than our nominal budget:
		// if so, set budget to their cost plus a margin to not advantage them
		int maxCost = Math.max(unit1.getCost(), unit2.getCost());
		if (maxCost > budget) {
			int minCost = Math.min(unit1.getCost(), unit2.getCost());
			budget = maxCost + minCost / 2;		
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
	void initUnit(Unit unit, int budget) {
		assert unit.getCost() <= budget;

// 		// Buy any attached hero
// 		if (unit.hasHero()) {
// 			Hero hero = unit.getHero();
// 			if (budget >= hero.getCost()) {
// 				initUnit(hero, hero.getCost());
// 				budget -= hero.getCost();
// 			}
// 			else {
// 				initUnit(hero, 0);			
// 			}
// 		}

// 		// Buy silver weapons if needed
// 		int cost = unit.getCost();
// 		if (useSilverWeapons && !(unit instanceof Hero)
// 				&& !unit.hasSpecial(SpecialType.SilverToHit) && unit.getHealth() < 4) {
// 			cost += (unit.hasMissiles() ? 2 : 1);
// 		}

		// Here we round the number of purchased figures to closest integer
		// If this goes over budget, we assume it balances with some other
		// unit on the imagined table that was under-budget to compensate
		int figures = (int) ((double) budget / unit.getCost() + 0.5);

		// Buy & set up normal figures
		unit.setFigures(figures);
		setRanksAndFiles(unit);

		// Set visibility
		boolean invisible = unit.hasSpecial(SpecialType.Invisibility)
				|| (unit.hasSpecial(SpecialType.WoodsCover) && terrain == Terrain.Woods);
		unit.setVisible(!invisible);

		// Prepare any special abilities
		unit.refreshCharges();
	}

	/**
	*  Set ranks and files for a unit.
	*/
	void setRanksAndFiles(Unit unit) {

		// Based on experience at table & need for maneuvarability,
		// we fill out 5 files up to 3 ranks (up to 15 figures).
		if (unit.getFigures() <= 15) {
			int files = Math.min(unit.getFigures(), 5);
			unit.setFiles(files);
		}
		
		// For more figures (not seen in practice, but may happen here
		// when the autobalancer starts at low cost, or if budget increased)
		// then we aim for a files:ranks proportion of 2:1.
		else {
			int files = (int) Math.sqrt(2 * unit.getFigures());
			unit.setFiles(files);
		}
	}

	/**
	*  Play out one turn of action for one attacking unit.
	*/
	void oneTurn(Unit attacker, Unit defender) {

		// Initialize
		defender.clearFigsLostInTurn();

		// Take one turn of action
		takeOneTurnAction(attacker, defender);

		// Check morale
		checkMorale(defender);

		// Check regeneration
		if (defender.hasSpecial(SpecialType.Regeneration)) {
			defender.regenerate();
		}
	}

	/**
	*  Take one turn of action by type of unit.
	*/
	void takeOneTurnAction(Unit attacker, Unit defender) {
		if (tryOneTurnCaster(attacker, defender)) {
			return;
		}
		else if (tryOneTurnRanged(attacker, defender)) {
			return;
		}
		else {
			oneTurnMelee(attacker, defender);
		}
	}

	/**
	*  Check if we should act as a ranged attacker ths turn.
	*  @return true if we took an action.
	*/
	boolean tryOneTurnRanged(Unit attacker, Unit defender) {
		if (distance > 0
			&& attacker.hasMissiles()
			&& minDistanceToShoot(attacker, defender) > 0
			&& !attacker.hasSpecial(SpecialType.MeleeShot))
		{
			oneTurnRanged(attacker, defender);
			return true;		
		}
		return false;
	}

	/**
	*  Move attacker forward, seeking goal distance
	*  Either full-speed or half-speed only
	*  Returns distance actually moved
	*/
	int moveSeekRange(Unit attacker, Unit defender, int goalDist, boolean fullSpeed) {

		// Compute distance to move
		assert 0 <= goalDist && goalDist < distance;
		int maxMove = getMove(attacker);
		if (!fullSpeed && maxMove > 1) {
			maxMove /= 2;
		}
		int moveDist = Math.min(distance - goalDist, maxMove);

		// Make the move
		assert 0 < moveDist && moveDist <= distance;
		distance -= moveDist;
		reportDetail(attacker + " move to distance " + distance);
		checkVisibility(attacker, defender);
		if (distance == 0) {
			checkPikeInterrupt(attacker, defender);
		}
		return moveDist;
	}

	/**
	*  Move attacker backward, as much as possible
	*  For horse archers with split-move-and-fire ability
	*  Assumes this uses half movement after fire
	*    & requires some wheeling to make happen
	*  So: Only get one-quarter of full movement
	*/
	int moveBackward(Unit attacker) {
		assert distance > 0;
		assert attacker.hasSpecial(SpecialType.SplitMove);
		int moveDist = getMove(attacker) / 4;
		distance += moveDist;
		reportDetail(attacker + " move back to distance " + distance);
		return moveDist;			
	}	

	/**
	*  Apply damage from attack & report.
	*/
	void applyDamage(Unit attacker, Unit defender, boolean ranged, int hits) {
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
	void oneTurnMelee(Unit attacker, Unit defender) {
		int distMoved = 0;
	
		// Charge to contact
		if (distance > 0) {
			distMoved = moveSeekRange(attacker, defender, 0, true);
		}

		// Expand frontage if useful
		if (distMoved == 0) {
			if (attacker.getRanks() > 1 
					&& attacker.getTotalWidth() < defender.getPerimeter()) {
				int newFiles = Math.min(attacker.getFiles() + 6, attacker.getFigures());
				attacker.setFiles(newFiles);
			}		
		}

 		// Attack if in contact
 		if (distance == 0) {
			if (!attacker.isBeaten()) {
	 			meleeAttack(attacker, defender);
				checkMeleeShot(attacker, defender, distMoved);
			}
			priorContact = true;
 		}
	}

	/**
	*  Try to make shot in melee, if possible.
	*/
	void checkMeleeShot(Unit attacker, Unit defender, int distMoved) {
		assert distance == 0;
		if (attacker.hasSpecial(SpecialType.MeleeShot) 
			&& minDistanceToShoot(attacker, defender) > 0
			&& distMoved <= getMove(attacker) / 2)
		{				
			rangedAttack(attacker, defender, false);
		}
	}

	/**
	*  Check for pikes interrupt attack on defense.
	*/
	void checkPikeInterrupt(Unit attacker, Unit defender) {
		if (isPikeAvailable(defender) 
				&& !(random.nextDouble() < pikeFlankingChance)
				&& !(getsRearAttack(attacker, defender))) 
		{
			reportDetail("** PIKES INTERRUPT ATTACK **");
			pikesInterrupt = true;
			attacker.clearFigsLostInTurn();
			meleeAttack(defender, attacker);
			checkMorale(attacker);
			pikesInterrupt = false;
		}
	}

	/**
	*  Play out one turn for ranged troops (AI-flavored).
	*/
	void oneTurnRanged(Unit attacker, Unit defender) {
		int distMoved = 0;

		// Check relative firing ranges
		int minShotDist = minDistanceToShoot(attacker, defender);
		int minShotDistEnemy = minDistanceToShoot(defender, attacker);
		boolean outranged = (minShotDist < minShotDistEnemy);

		// Move to shooting distance
		if (distance > minShotDist) {

			// If enemy outranges us, better to go full speed to our range
			// Otherwise we step forward half-speed to get first shot
			// (Note testing shows it a tiny bit better to stand and wait for full shots;
			// but that's rare at the table, often pivot required, so we assume movement.)
			distMoved = moveSeekRange(attacker, defender, minShotDist, outranged);
		}

		// Fire if permitted
		if (distance <= minShotDist) {
			if (distMoved == 0) {
				rangedAttack(attacker, defender, true);
			}
			else if (distMoved <= getMove(attacker) / 2) {
				rangedAttack(attacker, defender, false);
				checkSplitMove(attacker, outranged);
			}
		}
	}

	/**
	*  Try to use split-move-and-fire, if possible.
	*/
	void checkSplitMove(Unit attacker, boolean outranged) {
		if (attacker.hasSpecial(SpecialType.SplitMove) && !outranged) {
			moveBackward(attacker);
		}
	}

	/**
	*  Find minimum distance to have any chance of shooting enemy.
	*/
	int minDistanceToShoot(Unit attacker, Unit defender) {

		// Check factors prohibiting fire
		if (!attacker.hasMissiles() 
			|| !terrainPermitShots()
			|| isAttackImmune(attacker, defender)
			|| (weather == Weather.Rainy && attacker.hasSpecial(SpecialType.NoRainShot)))
		{
      	return 0;
      }

		// Get base to-hit target
		int baseToHit = defender.getArmor() 
			- baseAtkBonus(attacker)
			- miscAtkBonus(attacker, defender, true);

		// Determine minimum distance
		if (baseToHit > 6) {
			return 0;                     	   // Melee only
		}
		else if (baseToHit == 6) {
			return attacker.getRange() / 2;     // Short only
		}
		else {
			return attacker.getRange();         // Full range
		}
	}

	/**
	*  Does terrain permit shooting?
	*/
	boolean terrainPermitShots() {
		return !(terrain == Terrain.Woods || terrain == Terrain.Gulley);
	}

	/**
	*  Find maximum distance to move, including terrain & weather.
	*/
	int getMove(Unit unit) {
		int moveCost;

		// Terrain mods
		switch (terrain) {
			default: moveCost = 1; break;
			case Hill: case Gulley: // Assume uphill; +1 step per 2"
			case Rough: case Woods: moveCost = 2; break;
			case Marsh: moveCost = 3; break;
			case Stream: moveCost = 4; break;
		}
			
		// Swimmers ignore streams
		if (unit.hasSpecial(SpecialType.Swimming) && terrain == Terrain.Stream) {
			moveCost = 1;
		}

		// Flyers ignore everything
		if (unit.hasSpecial(SpecialType.Flight)) {
			moveCost = 1;
		}

		// Weather mods
		if (weather == Weather.Rainy) {
			moveCost *= 2;
		}

		// Mounted penalties double
		if (unit.hasSpecial(SpecialType.Mounts) && moveCost > 1) {
			moveCost *= 2;
		}

		// Teleporters wait to pounce
		if (unit.hasSpecial(SpecialType.Teleport)) {
			int teleportRange = unit.getSpecialParam(SpecialType.Teleport);
			return distance < teleportRange ? distance : 1;
		}

		// Return move (at least 1 inch)
		int move = Math.max(unit.getMove(), unit.getFlyMove()) / moveCost;
		return Math.max(move, 1);
	}

	/**
	*  Play out one melee attack.
	*/
	void meleeAttack(Unit attacker, Unit defender) {
		makeVisible(attacker);

		// Check for defender immune
		if (isAttackImmune(attacker, defender)) {
			return;
		}

		// Compute number of figures attacking
		// (normally assumes wrap, but no rear bonus)
		double atkWidth = attacker.getTotalWidth();
		double defWidth = priorContact 
			? defender.getPerimeter() : defender.getTotalWidth();
		int figsAtk = atkWidth <= defWidth ? attacker.getFiles() 
			: (int) Math.ceil(defWidth / attacker.getFigWidth());
		if (defender.isSmallTarget()) {
			figsAtk = Math.min(figsAtk, defender.getFigures());
		}

		// Compute number of dice & attack bonus
		int numAtkDice = meleeAttackDice(attacker, defender, figsAtk);
		int atkBonus = baseAtkBonus(attacker) 
			+ miscAtkBonus(attacker, defender, false);

		// Roll the attack dice (if needed)
		int numHits = 0;
		for (int i = 0; i < numAtkDice; i++) {
			if (attacker.autoHits()
				|| rollToHit(atkBonus, defender.getArmor())) 
			{
				numHits++;
			}
		}

		// Apply damage
		int damagePerHit = attacker.getDamage();
		if (pikesInterrupt) {
			damagePerHit *= 2;
		}
		if (capDamageByHealth) {
			damagePerHit = Math.min(damagePerHit, defender.getHealth());
		}
		int damageTotal = numHits * damagePerHit;
		if (attacker.hasSpecial(SpecialType.DamageInc)) {
			damageTotal += damageTotal / 2;
		}
		applyDamage(attacker, defender, false, damageTotal);
	}

	/**
	*  Play out one ranged attack.
	*  Note that embedded leaders are not touched here.
	*/
	void rangedAttack(Unit attacker, Unit defender, boolean fullRate) {

		// Check preconditions
		assert attacker.hasMissiles();
		assert distance <= attacker.getRange();
		assert distance > 0 || attacker.hasSpecial(SpecialType.MeleeShot);
		assert !attacker.autoHits(); // solos not handled
		assert !defender.isEmbedded();

		// Make attacker visible
		makeVisible(attacker);

		// Check for defender immune
		if (isAttackImmune(attacker, defender)) {
			return;
		}

		// Measure range & get modifier
		int rangeMod = distance <= attacker.getRange() / 2
			? 0 : -1;

		// Compute number of dice to roll
		int figsAtk = attacker.getFigures();
		int atkDice = figsAtk * attacker.getRate();
		if (!fullRate) {
			atkDice /= 2;
		}

		// Compute attack bonus
		int atkBonus = baseAtkBonus(attacker)
			+ miscAtkBonus(attacker, defender, true) 
			+ rangeMod;

		// Roll attack dice
		int numHits = 0;
		for (int i = 0; i < atkDice; i++) {
			if (rollToHit(atkBonus, defender.getArmor())) {
				numHits++;
			}
		}
		
		// Confirm hits if needed
		if (defender.isSmallTarget()) {
			numHits = confirmRangedHits(defender, numHits);
		}
		
		// Apply damage
		int damagePerHit = attacker.getDamage();
		if (attacker.hasSpecial(SpecialType.Mounts)) {
			damagePerHit = 1; // elephant archers
		}
		if (attacker.hasSpecial(SpecialType.BigStones)) {
			damagePerHit += 1; // stone giants
		}
		if (capDamageByHealth) {
			damagePerHit = Math.min(damagePerHit, defender.getHealth());
		}
		int damageTotal = numHits * damagePerHit;
		applyDamage(attacker, defender, true, damageTotal);
	}

	/**
	*  Confirm a ranged hits for small (solo) targets.
	*  Use the size parameter as chance in 6 to score hit.
	*  @return the number of confirmed hits
	*/
	int confirmRangedHits(Unit defender, int numHits) {
		assert defender.isSmallTarget();
		int confirmed = 0;
		for (int i = 0; i < numHits; i++) {
			if (d6() <= defender.getFigWidthPips()) {
				confirmed++;
			}
		}
		return confirmed;
	}

	/**
	*  Count melee attack dice (with special modifiers).
	*/
	int meleeAttackDice(Unit attacker, Unit defender, int figsAtk) {

		// Initialize
		int atkDice = figsAtk * attacker.getAttacks();

		// Sweep attacks replace normal attacks
		if (defender.isSweepable()
			&& attacker.hasSpecial(SpecialType.SweepAttack))
		{
			atkDice = figsAtk 
				* attacker.getSpecialParam(SpecialType.SweepAttack);
		}

		// Mounts & pikes get half dice in bad terrain
		if ((attacker.hasSpecial(SpecialType.Mounts) 
				|| attacker.hasSpecial(SpecialType.Pikes))
			&& (terrain != Terrain.Open || weather == Weather.Rainy)) 
		{
			atkDice /= 2;
		}

		// Return at least 1 die
		return Math.max(atkDice, 1);
	}

	/**
	*  Get base attack bonus (one-third of health).
	*/
	int baseAtkBonus(Unit attacker) {
		return attacker.getHealth() / 3;
	}

	/**
	*  Find miscellaneous to-hit bonuses.
	*/
	int miscAtkBonus(Unit attacker, Unit defender, boolean ranged) {
		int bonus = 0;

		// Rainy day weather missile penalty
		if (ranged && weather == Weather.Rainy)  {
			bonus -= 1;
		}

		// Orcs & goblins penalty in sunlight
		if (attacker.hasSpecial(SpecialType.LightWeakness) && weather == Weather.Sunny) {
			bonus -= 1;
		}

 		// Halfling ranged attack bonus
 		if (attacker.hasSpecial(SpecialType.ShotBonus) && ranged) {
			bonus += 1;
		}
		
 		// Dwarf dodge giants
 		if (attacker.hasSpecial(SpecialType.GiantClass) 
				&& defender.hasSpecial(SpecialType.GiantDodging)) {
 			bonus -= 1;
		}

		// Mounted archers assumed to hit as normal men (e.g.: elephants)
		if (ranged && attacker.hasSpecial(SpecialType.Mounts)) {
			bonus -= baseAtkBonus(attacker);
		}

		// Melee rear attack bonus
		if (!ranged && getsRearAttack(attacker, defender)) {
			bonus += 1;
		}
		
		return bonus;
	}

	/**
	*  Is this a case where we get the melee rear attack bonus?
	*/
	boolean getsRearAttack (Unit attacker, Unit defender) {

		// Embedded targets are not susceptible
		if (defender.isEmbedded()) {
			return false;
		}

		// Small non-embedded targets always suffer it
		if (defender.isSmallTarget()) {
			return true;
		}	

		// Fast flyers always get it
		if (attacker.getFlyMove() >= 30) {
			return true;
		}
		
		// Slow flyers & teleporters get it on first contact only
		if ((attacker.hasSpecial(SpecialType.Flight)
				|| attacker.hasSpecial(SpecialType.Teleport))
			&& !priorContact)
		{
			return true;
		}
			
		return false;
	}
	
	/**
	*  Is the defender immune to this attack?
	*/
	boolean isAttackImmune(Unit attacker, Unit defender) {

		// Invisible units can't be attacked
		if (!defender.isVisible()) {
			return true;
		}

		// Units that auto-hit (solos) assumed to bypass protections
		if (attacker.autoHits()) {
			return false;
		}

		// Check silver-to-hit (AD&D rule: 4HD+ bypasses)
		if (defender.hasSpecial(SpecialType.SilverToHit) && !useSilverWeapons
				&& attacker.getHealth() < 4 && !attacker.hasSpecial(SpecialType.SilverToHit)) {
			return true;
		}
		
		// Check magic-to-hit (AD&D rule: 6HD+ bypasses +2 to hit)
		if (defender.hasSpecial(SpecialType.MagicToHit)
				&& attacker.getHealth() < 6 && !attacker.hasSpecial(SpecialType.MagicToHit)) {
			return true;
		}

		return false;
	}

	/**
	*  Is this unit able to make the special pike attack?
	*/
	boolean isPikeAvailable(Unit unit) {
		return unit.hasSpecial(SpecialType.Pikes)
			&& terrain == Terrain.Open
			&& weather != Weather.Rainy
			&& !priorContact;
	}

	/**
	*  Roll to hit for one attack.
	*/
	boolean rollToHit(int bonus, int armor) {
		int die = d6();
		int total = die + bonus;
		//reportDetail("Attack roll: die " + die + " + " + bonus
		//	+ " = " + total + " vs. armor " + armor);
		return total >= armor;
	}

	/**
	*  Check morale (sets field if failed).
	*/
	void checkMorale(Unit unit) {
		if (unit.getFigures() == 0 
			|| unit.getFigsLostInTurn() == 0
			|| unit.isFearless()) 
		{
			return;
		}

		// Compute bonus
		int hitDice = unit.getHealth();
		int rateOfLoss = unit.getFigures() / unit.getFigsLostInTurn();
		int miscBonus = miscMoraleBonus(unit);
		int bonus = hitDice + rateOfLoss + miscBonus;

		// Make the roll
		int roll = d6() + d6();
		int total = roll + bonus;

		// Report and assessment
		reportDetail("Morale check (" + unit + "): " 
			+ roll + " + " + bonus + " = " + total);
		if (total < MORALE_TARGET) {
			unit.setRouted(true);
			reportDetail(unit + " are * ROUTED *");
		}
	}

	/**
	*  Find miscellaneous morale bonuses.
	*/
	int miscMoraleBonus(Unit unit) {
		int bonus = 0;

		// Alignment
		switch (unit.getAlignment()) {
			case Lawful: bonus += 1; break;
			case Chaotic: bonus -= 1; break;
			default: break;
		}

		// Leadership
		if (unit.hasLeader()) {
			bonus += 1;
		}

		// Light weakness (orcs & goblins)
		if (unit.hasSpecial(SpecialType.LightWeakness) 
			&& weather == Weather.Sunny) 
		{
			bonus -= 1;
		}

		// Special ability bonuses
		if (unit.hasSpecial(SpecialType.MoraleBonus)) {
			bonus += unit.getSpecialParam(SpecialType.MoraleBonus);
		}

		return bonus;		
	}

	/**
	*  Check for visibility in both directions
	*/
	void checkVisibility(Unit attacker, Unit defender) {
		checkVision(attacker, defender);
		checkVision(defender, attacker);
	}

	/**
	*  Check for one side spotting the other
	*/
	void checkVision(Unit spotter, Unit target) {
		if (!target.isVisible() 
			&& spotter.getSpecialParam(SpecialType.Detection) >= distance)
		{
			target.setVisible(true);		
		}
	}

	/**
	*  Randomize terrain.
	*    Assume just one terrain type across entire field.
	*    Percents match coverage of entire table.
	*/
	void randomizeTerrain() {
		int roll = (int) (random.nextDouble() / terrainMultiplier * 100);

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
	void randomizeWeather() {
		switch (d6()) {
			case 1: case 2: 
				weather = Weather.Sunny; break;
			case 3: case 4: case 5: 
				weather = Weather.Cloudy; break;
			default: 
				weather = Weather.Rainy; break;
		}
	}

	/**
	*  Roll a 6-sided die.
	*/
	int d6() {
		return random.nextInt(6) + 1;
	}

	/**
	*  Find greatest common denominator of two integers.
	*/
	int gcd(int a, int b) {
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
	int lcm(int a, int b) {
		return a * (b / gcd(a, b));
	}

	/**
	*  Find the closest multiple of two integers to some target value.
	*/
	int closestCommonMultiple(int a, int b, int target) {
		return closestMultiple(lcm(a, b), target);
	}

	/**
	*  Find the closest multiple of one integer to some target value.
	*/
	int closestMultiple(int num, int target) {
		int q = target / num;
		if (q == 0) {
			return num;
		}
		int lowerBound = q * num;
		int upperBound = (q + 1) * num;
		int lowerError = target - lowerBound;
		int upperError = upperBound - target;
		return lowerError <= upperError ? lowerBound : upperBound;
	}

	/**
	*  Convert double to integer percent.
	*/
	int toPercent(double d) {
		return (int) Math.round(d * 100); 	
	}

	/**
	*  Normalize an error value to positive.
	*  (We may try either squared-error or absolute-error here.)
	*/
	double normalError(double error) {
		return Math.pow(error, 2);
	}

	//-----------------------------------------------------------------
	//  Methods for magic caster abilities.
	//-----------------------------------------------------------------

	/**
	*  Make an invisible attacker become visible.
	*/
	void makeVisible(Unit atk) {
		if (!atk.isVisible()) {
			atk.setVisible(true);		
			reportDetail(atk + " become visible!");
		}
	}

	/**
	*  Check if a unit should act as a caster this turn.
	*  @return true if we took an action.
	*/
	boolean tryOneTurnCaster(Unit attacker, Unit defender) {

		// Storm giants make rain
		if (attacker.hasSpecial(SpecialType.WeatherControl)
				&& weather != Weather.Rainy) 
		{
			weather = Weather.Rainy;
			reportDetail(attacker + " use Weather Control to make weather Rainy");
			return true;
		}
	
		return false;	
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
// 			if (getSave) save = random.nextInt(3) + 1; // d3
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

	//-----------------------------------------------------------------
	//  Methods for thread management
	//-----------------------------------------------------------------

	/**
	*  Check if any thread in an array is live
	*/
	boolean isAnyThreadLive(Thread[] threads) {
		for (Thread t: threads) {
			if (t.isAlive()) {
				return true;
			}
		}
		return false;
	}

	/**
	*  Wait for all threads in an array to finish
	*/
	void waitForThreads(Thread[] threads) {
		while (isAnyThreadLive(threads)) {
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				System.err.println("Exception in waitForThreads: " + e);
			}
		}	
	}
}

//-----------------------------------------------------------------
//  Class to run multi-threaded game series.
//-----------------------------------------------------------------

class	SeriesRunner implements Runnable {

	// Member records
	private BookOfWar bowSim;
	private Unit testUnit, oppUnit;
	private double testUnitWinRatio;

	// Constructor
	SeriesRunner(BookOfWar pBowSim, Unit pTestUnit, Unit pOppUnit) {
		bowSim = new BookOfWar(pBowSim);
		testUnit = pTestUnit instanceof Solo
			? new Solo((Solo) pTestUnit) : new Unit(pTestUnit);
		oppUnit = new Unit(pOppUnit);
	}

	// Interface run function
	@Override
	public void run() {
		testUnitWinRatio = testUnit.equals(oppUnit) 
			? 0.5 : bowSim.playSeries(testUnit, oppUnit);
	}
	
	// Get win ratio result
	public double getTestUnitWinRatio() {
		return testUnitWinRatio;
	}
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
