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
	enum SimMode { ZoomInGame, TableAssess, AutoBalance, 
						FullBalance, EmbedBalance };
	enum EnergyType { Fire, Volt, Acid, Cold, Poison, Multi };

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
	private static final int BUDGET_MIN_DEFAULT = 50;

	/** Budget maximum (basis 100). */
	private static final int BUDGET_MAX_DEFAULT = 100;

	/** Ceiling for any unit cost in auto-balancer. */
	private static final int COST_LIMIT = 999;

	/** Balances swords vs. pikes & cavalry (basis 1.00). */
	private static final double TERRAIN_MULTIPLIER = 1.00;

	/** Balances pikes vs. swords & cavalry (basis 0.20). */
	private static final double PIKE_FLANK_CHANCE = 0.30;

	/** Limit per-hit damage by target's health? */
	private static final boolean CAP_DAMAGE_BY_HEALTH = false;

	/** Buy silver weapons for all troop types? */
	private static final boolean USE_SILVER_WEAPONS = false;
	
	/** Target for morale check success (per Vol-1, p. 12). */
	private static final int MORALE_TARGET = 9;

	/** Range for magic wand missile attacks. */
	private static final int WAND_RANGE = 24;

	/** Number of steps allowed for Control Weather spell. */
	private static final int CONTROL_WEATHER_STEPS = 1;

	/** Index for solo added to animated units. */
	private static final int DEFAULT_CONTROLLER = 1;
	
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

	/** Budget minimum and maximum. */
	private int budgetMin, budgetMax;

	/** Assess to this unit number (unit types 1 to n). */
	private int assessUnitNum;

	/** Base unit set for comparisons (unit types 1 to n). */
	private int baseUnitNum;

	/** Chief unit set for embed balancing (solo types 1 to n). */
	private int chiefUnitNum;

	/** Number of trials per matchup. */
	private int trialsPerMatchup;

	/** Units for zoom-in game (1-based index into Units list). */
	private int zoomGameUnit1, zoomGameUnit2;

	/** Solo to embed with zoom-in game (1-based index into Solo list). */
	private int zoomGameChief;

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
		budgetMin = BUDGET_MIN_DEFAULT;
		budgetMax = BUDGET_MAX_DEFAULT;
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
		budgetMin = src.budgetMin;
		budgetMax = src.budgetMax;
		unitList = src.unitList;
		soloList = src.soloList;
		assessUnitNum = src.assessUnitNum;
		baseUnitNum = src.baseUnitNum;
		chiefUnitNum = src.chiefUnitNum;
		trialsPerMatchup = src.trialsPerMatchup;
		zoomGameUnit1 = src.zoomGameUnit1;
		zoomGameUnit2 = src.zoomGameUnit2;
		zoomGameChief = src.zoomGameChief;
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
		System.out.println("\t-m sim mode (0 = zoom-in game, 1 = table-asses,\n"
									+ "\t\t 2 = base auto-balance, 3 = full auto-balance,\n"
									+ "\t\t 4 = balance solo embeds");
		System.out.println("\t-p use preferred values in full auto-balancer");
		System.out.println("\t-s balance the solo vs. basic unit types");
		System.out.println("\t-t trials per matchup (default=" + DEFAULT_TRIALS_PER_MATCHUP + ")");
		System.out.println("\t-v print assessment table in CSV format");
		System.out.println("\t-x zoom-in game chief solo index (1-based)");
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
					case 'c': chiefUnitNum = getParamInt(s); break;
					case 'm': parseSimMode(s); break;
					case 'p': usePreferredValues = true; break;					
					case 's': soloBalancing = true; break;
					case 't': trialsPerMatchup = getParamInt(s); break;
					case 'v': printFormatCSV = true; break;
					case 'x': zoomGameChief = getParamInt(s); break;
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
			case 0: simMode = SimMode.ZoomInGame; break;
			case 1: simMode = SimMode.TableAssess; break;
			case 2: simMode = SimMode.AutoBalance; break;
			case 3: simMode = SimMode.FullBalance; break;
			case 4: simMode = SimMode.EmbedBalance; break;
			default: System.err.println("Error: Unknown sim mode.");
				exitAfterStartup = true; 
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
		if (!soloBalancing 
			&& simMode != SimMode.EmbedBalance
			&& baseUnitNum >= assessUnitNum) 
		{
			postStartupFailMsg("Error: Assessed unit set must be more than base units (fix -a or -b switch).");
		}
		
		// Check chief set size
		if (chiefUnitNum < 0) {
			postStartupFailMsg("Error: Chief unit set must be nonnegative (fix -c switch).");
		}
		else if (chiefUnitNum > soloList.size()) {
			postStartupFailMsg("Error: Chief unit set must be no more than database size (fix -c switch).");
		}
	}
	
	/**
	*  Check base unit set positive (for certain cases).
	*/
	boolean checkBaseUnitsPositive() {
	
		// Set default if needed
		if ((soloBalancing || simMode == SimMode.EmbedBalance)
			&& baseUnitNum == 0) 
		{
			baseUnitNum = unitList.size();
		}
	
		// Check base set size
		if (baseUnitNum <= 0) {
			System.err.println("Error: Base unit set must be positive (fix -b switch).");
			return false;
		}
		return true;
	}	

	/**
	*  Check chief unit set positive (for certain cases).
	*/
	void checkChiefUnitsPositive() {
	
		// Set default if needed
		if (simMode == SimMode.EmbedBalance && chiefUnitNum == 0) {
			chiefUnitNum = soloList.size();
		}
	}	

	/**
	*  Run the simulator in selected mode.
	*/
	void run() {
 		switch (simMode) {
 			case ZoomInGame: zoomInGame(); break;
 			case TableAssess: assessmentTable(); break;
 			case AutoBalance: autoBalancer(); break;
			case FullBalance: fullAutoBalancer(); break;
			case EmbedBalance: embedBalancer(); break;
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
		if (simMode == SimMode.ZoomInGame) {
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
	int getMaxNameLength(List<? extends Unit> pUnitList) {
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
	*  Battle two specified unit types with detailed in-game reports.
	*/
	void zoomInGame() {
		int maxFirstIndex = soloBalancing
			? soloList.size() : unitList.size();

		// Check selected unit indeces.
		if (zoomGameUnit1 <= 0 || zoomGameUnit2 <= 0) {
			System.err.println("Error: Zoom-in game requires two unit indexes (use -y and -z switches).");
			return;
		}
		if (zoomGameUnit1 > maxFirstIndex || zoomGameUnit2 > unitList.size()) {
			System.err.println("Error: Zoom-in game has unit out of range for database (fix -y or -z.");
			return;
		}
		if (zoomGameChief > soloList.size()) {
			System.err.println("Error: Zoom-in game has chief out of range for database (fix -x).");
			return;
		}
		
		// Do the game
		Unit unit1 = soloBalancing
			? new Solo(soloList.get(zoomGameUnit1 - 1))
			: new Unit(unitList.get(zoomGameUnit1 - 1));
		if (zoomGameChief < 1 && unit1.isControlRequired()) {
			System.err.println("Error: First unit requires leader control (use -x).");
			return;		
		}
		if (zoomGameChief > 0) {
			budgetMin *= 2;
			budgetMax *= 2;
			Solo chief = new Solo(soloList.get(zoomGameChief - 1));
			unit1.setLeader(chief);
		}
		Unit unit2 = new Unit(unitList.get(zoomGameUnit2 - 1));
		playGame(unit1, unit2);
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
		printWideField("Unit", nameColSize);
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
			if (highCostWinPctErr > 0 && highCost > COST_LIMIT) {
				newUnit.setCost(COST_LIMIT);
				return;
			}
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
		if (usePreferredValues) {
			return up ? PreferredValues.inc(oldCost) : PreferredValues.dec(oldCost);
		}
		else {
			return up ? oldCost + 1 : oldCost - 1;
		}
	}

	/**
	*  Balance costs for embedded Solo units.
	*/
	void embedBalancer() {

		// Check prerequisites
		checkChiefUnitsPositive();
		if (!checkBaseUnitsPositive()) {
			return;		
		}

		// Double the standard budgt values.
		budgetMin *= 2;
		budgetMax *= 2;

		// Get list of solo units to embed.
		List<Solo> chiefUnits = soloList.subList(0, chiefUnitNum);
		int nameColSize = getMaxNameLength(chiefUnits);

		// Print header
		printf("Auto-balanced embeded Solos best cost "
			+ "(nominal budget " + budgetMin + "-" + budgetMax + "):\n\n");
		printWideField("Solo", nameColSize);
		printf(getSepChar() + "Cost\n");

		// Make the table
		for (Solo solo: chiefUnits) {
			setEmbedBalancedCost(solo);
			printWideField(solo.getName(), nameColSize);
			printf(getSepChar() + "" + solo.getCost() + "\n");
		}
	}

	/**
	*  Set the best cost for an embedded Solo type.
	*  Searches for sumWinPctErr closest to zero (0). 
	*/
	void setEmbedBalancedCost(Solo solo) {
		int lowCost = 1, highCost = solo.getCost();

		// Check lower bound for cost
		solo.setCost(lowCost);
		double lowCostWinPctErr = scoreSoloAllHosts(solo);
		if (lowCostWinPctErr < 0) {
			return;
		}

		// Find upper bound for cost
		solo.setCost(highCost);
		double highCostWinPctErr = scoreSoloAllHosts(solo);
		while (highCostWinPctErr > 0) {
			highCost *= 2;
			solo.setCost(highCost);
			highCostWinPctErr = scoreSoloAllHosts(solo);
			if (highCostWinPctErr > 0 && highCost > COST_LIMIT) {
				solo.setCost(COST_LIMIT);
				return;
			}
		}
			
		// Binary search for best cost
		while (highCost - lowCost > 1) {
			int midCost = (highCost + lowCost) / 2;
			solo.setCost(midCost);
			double midWinPctErr = scoreSoloAllHosts(solo);
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
		solo.setCost(!usePreferredValues ? bestCost : PreferredValues.getClosest(bestCost));
	}

	/**
	*  Score error for one Solo embedded in all Hosts.
	*  @return the total sumErr across hosts vs. base units.
	*/
	double scoreSoloAllHosts(Solo solo) {
		double totalSumErr = 0;
		List<Unit> hostUnits;
		if (!soloBalancing) {
			hostUnits = unitList.subList(0, assessUnitNum);		
		}
		else {
			// Balancing solos leading other solos:
			// get the _end_ of the solo list for host units.
			int maxSolo = soloList.size();
			hostUnits = new ArrayList<Unit>(
				soloList.subList(maxSolo - assessUnitNum, maxSolo));
		}
		for (Unit host: hostUnits) {
			double error = scoreSoloOneHost(solo, host);
			totalSumErr += error;
		}
		return totalSumErr;
	}

	/**
	*  Score error for one Solo embedded in one Host.
	*  @return the sumErr for this solo + host across the base unit set.
	*/
	double scoreSoloOneHost(Solo solo, Unit host) {
		Unit newHost = new Unit(host);
		Solo newSolo = new Solo(solo);
		newHost.setLeader(newSolo);
		List<Unit> baseUnits = unitList.subList(0, baseUnitNum);
		return playDocket(newHost, baseUnits);
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
		return !unit1.isTotallyBeaten()
			&& !unit2.isTotallyBeaten();
	}

	/**
	*  Determine victorious unit.
	*/
	Unit getWinner(Unit unit1, Unit unit2) {
		if (unit1.isTotallyBeaten()) {
			return unit2;
		}
		else if (unit2.isTotallyBeaten()) {
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

		// Check if we need to add required controllers
		checkControllerReq(unit1);
		checkControllerReq(unit2);

		// Handle units pricier than our nominal budget:
		// if so, set budget to their cost plus a margin to not advantage them
		int maxCost = Math.max(getMaxCost(unit1), getMaxCost(unit2));
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
	*  Add a controller if this unit needs it.
	*/
	void checkControllerReq(Unit unit) {
		if (unit.isControlRequired() && !unit.hasLeader()) {
			Solo controller = new Solo(soloList.get(DEFAULT_CONTROLLER - 1));		
			unit.setLeader(controller);
		}
	}

	/**
	*  Get maximum figure cost for a unit.
	*/
	int getMaxCost(Unit unit) {
		if (unit.hasLeader()) {
			return Math.max(unit.getCost(), unit.getLeader().getCost());
		}	
		else {
			return unit.getCost();
		}
	}

	/**
	*  Initialize one unit by budget.
	*/
	void initUnit(Unit unit, int budget) {
		assert budget >= 0;
		assert getMaxCost(unit) <= budget;

		// Buy any attached leader
		if (unit.hasLeader()) {
			Solo leader = unit.getLeader();
			if (budget >= leader.getCost()) {
				leader.setFigures(1);
				budget -= leader.getCost();
			}
			else {
				leader.setFigures(0);
			}
			finishInitUnit(leader);
		}

		// Here we round the number of purchased figures to closest integer
		// If this goes over budget, we assume it balances with some other
		// unit on the imagined table that was under-budget to compensate
		int figures = (int) ((double) budget / unit.getCost() + 0.5);

		// Buy & set up normal figures
		unit.setFigures(figures);
		finishInitUnit(unit);
	}

	/**
	*  Finish initializing a unit before a game.
	*/
	void finishInitUnit(Unit unit) {

		// Set the ranks and files
		setRanksAndFiles(unit);

		// Set visibility
		boolean invisible = unit.hasSpecial(SpecialType.Invisibility)
				|| (unit.hasSpecial(SpecialType.WoodsCover) && terrain == Terrain.Woods);
		unit.setVisible(!invisible);

		// Prepare any special abilities
		unit.refreshCharges();
		unit.setSavedVsFear(false);
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
		assert !attacker.hasHost();

		// Initialize
		defender.clearFigsLostInTurn();

		// Take one turn of action
		takeOneTurnAction(attacker, defender);

		// Check morale
		checkMoraleEndTurn(defender);

		// Check regeneration
		if (defender.hasSpecial(SpecialType.Regeneration)) {
			defender.regenerate();
		}
		
		// Check animated with no leader
		if (defender.isControlRequired()
			&& !defender.hasActiveLeader())
		{
			defender.setRouted(true);		
		}
	}

	/**
	*  Take one turn of action by type of unit.
	*/
	void takeOneTurnAction(Unit attacker, Unit defender) {
		assert !attacker.hasHost();
		boolean wantsToMove = true;

		// Leader caster actions
		if (attacker.hasCasterLeader()) {
			if (tryOneTurnCaster(attacker.getLeader(), defender)) {
				wantsToMove = false;
			}
		}

		// Main unit caster actions
		if (attacker.isCaster()) {
			if (tryOneTurnCaster(attacker, defender)) {
				return;
			}
		}

		// Normal unit actions
		if (tryOneTurnRanged(attacker, defender, wantsToMove)) {
			return;
		}
		else {
			oneTurnMelee(attacker, defender, wantsToMove);
		}
	}

	/**
	*  Check if we should act as a ranged attacker ths turn.
	*  @return true if we took an action.
	*/
	boolean tryOneTurnRanged(Unit attacker, Unit defender, boolean wantsToMove) {
		if (distance > 0
			&& attacker.hasMissiles()
			&& minDistanceToShoot(attacker, defender) > 0
			&& !attacker.hasSpecial(SpecialType.MeleeShot))
		{
			oneTurnRanged(attacker, defender, wantsToMove);
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
		assert distance > 0;
		assert 0 <= goalDist && goalDist < distance;

		// Compute distance to move
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
			checkInitialContact(attacker, defender);
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
	void oneTurnMelee(Unit attacker, Unit defender, boolean wantsToMove) {
		int distMoved = 0;
	
		// Charge to contact
		if (distance > 0 && wantsToMove) {
			distMoved = moveSeekRange(attacker, defender, 0, true);
		}

		// Expand frontage if useful
		if (distance == 0 && distMoved == 0) {
			if (attacker.getRanks() > 1 
					&& attacker.getTotalWidth() < defender.getPerimeter()) {
				int newFiles = Math.min(attacker.getFiles() + 6, attacker.getFigures());
				attacker.setFiles(newFiles);
			}		
		}

 		// Attack if in contact
 		if (distance == 0) {
			checkMeleeSpecials(attacker, defender);
 			meleeAttack(attacker, defender);
			checkMeleeShot(attacker, defender, distMoved);
			priorContact = true;
 		}
	}

	/**
	*  Try to make shot in melee, if possible.
	*/
	void checkMeleeShot(Unit attacker, Unit defender, int distMoved) {
		assert distance == 0;
		if (bothUnitsLive(attacker, defender)
			&& attacker.hasSpecial(SpecialType.MeleeShot) 
			&& minDistanceToShoot(attacker, defender) > 0
			&& distMoved <= getMove(attacker) / 2)
		{				
			rangedAttack(attacker, defender, false);
		}
	}

	/**
	*  Make special checks on initial contact.
	*/
	void checkInitialContact(Unit attacker, Unit defender) {
		assert distance == 0;

		// Check for defender pikes
		if (defender.hasSpecial(SpecialType.Pikes)) {
			checkPikeInterrupt(attacker, defender);
		}
	}

	/**
	*  Check for pikes interrupt attack on defense.
	*/
	void checkPikeInterrupt(Unit attacker, Unit defender) {
		if (bothUnitsLive(attacker, defender)
				&& isPikeAvailable(defender) 
				&& !(random.nextDouble() < PIKE_FLANK_CHANCE)
				&& !(getsRearAttack(attacker, defender))) 
		{
			reportDetail("** PIKES INTERRUPT ATTACK **");
			pikesInterrupt = true;
			attacker.clearFigsLostInTurn();
			meleeAttack(defender, attacker);
			checkMoraleEndTurn(attacker);
			pikesInterrupt = false;
		}
	}

	/**
	*  Play out one turn for ranged troops (AI-flavored).
	*/
	void oneTurnRanged(Unit attacker, Unit defender, boolean wantsToMove) {
		int distMoved = 0;

		// Check relative firing ranges
		int minShotDist = minDistanceToShoot(attacker, defender);
		int minShotDistEnemy = minDistanceToShoot(defender, attacker);
		boolean outranged = (minShotDist < minShotDistEnemy);

		// Move to shooting distance
		if (distance > minShotDist && wantsToMove) {

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
		assert !attacker.isTotallyBeaten();
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

		// Check for auto-hit
		if (attacker.autoHits()) {
			return attacker.getRange();
		}

		// Get base to-hit target
		int baseToHit = getArmorForShot(defender)
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
	*  Get the effective armor for shooting at a target.
	*/
	int getArmorForShot(Unit target) {
		if (target.isLoneLeader()) {
			return target.getLeader().getArmor();
		}	
		else {
			return target.getArmor();		
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
		if (unit.hasSpecial(SpecialType.Teleport)
			&& unit.getCharges() > 0) 
		{
			int teleportRange = unit.getSpecialParam(SpecialType.Teleport);
			if (distance < teleportRange) {
				reportDetail(unit + " * TELEPORTS * into combat");
				unit.decrementCharges();
				return distance;
			}
			else {
				return 1;
			}
		}

		// Return move (at least 1 inch)
		int move = Math.max(unit.getMove(), unit.getFlyMove()) / moveCost;
		return Math.max(move, 1);
	}

	/**
	*  Play out one melee attack.
	*/
	void meleeAttack(Unit attacker, Unit defender) {

		// Jump out if either side dead
		if (!bothUnitsLive(attacker, defender)) {
			return;
		}

		// Give an attack to a leader figure
		if (attacker.hasActiveLeader()) {
			meleeAttack(attacker.getLeader(), defender);
		}

		// Jump out if we have no attacks
		if (attacker.getAttacks() == 0 || attacker.isNormalBeaten()) {
			return;
		}

		// Compute number of attackers (possibly one vs. leader)
		int figsAtk = countFiguresInContact(attacker, defender);
		if (defender.hasActiveLeader() && figsAtk > 0) {
			meleeAttack(attacker, defender.getLeader());
			figsAtk--;
		}

		// Check that we have attacks to make
		if (figsAtk == 0) {
			return;		
		}

		// Check for defender immune
		if (isAttackImmune(attacker, defender)) {
			reportDetail(attacker + " barred from attacking " + defender);
			return;
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
		if (CAP_DAMAGE_BY_HEALTH) {
			damagePerHit = Math.min(damagePerHit, defender.getHealth());
		}
		int damageTotal = numHits * damagePerHit;
		if (attacker.hasSpecial(SpecialType.DamageBonus)) {
			damageTotal += damageTotal / 2;
		}
		applyDamage(attacker, defender, false, damageTotal);
		
		// Make the attacker visible
		makeVisible(attacker);
	}

	/**
	*  Compute number of attacking figures in contact.
	*  Assumes wrapping after initial contact.
	*/
	int countFiguresInContact(Unit attacker, Unit defender) {
		assert distance == 0;
		double atkWidth = attacker.getTotalWidth();
		double defWidth = priorContact 
			? defender.getPerimeter() : defender.getTotalWidth();
		int figsAtk = (atkWidth <= defWidth) ? attacker.getFiles()
			: (int) Math.ceil(defWidth / attacker.getFigWidth());
		if (defender.isSmallTarget()) {
			int figsDef = defender.getFigures()
				+ (defender.hasActiveLeader() ? 1 : 0);
			figsAtk = Math.min(figsAtk, figsDef);
		}
		assert figsAtk <= attacker.getFigures();
		return figsAtk;
	}

	/**
	*  Play out one ranged attack.
	*  Note that hosted leaders are not touched here.
	*/
	void rangedAttack(Unit attacker, Unit defender, boolean fullRate) {

		// Check preconditions
		assert !attacker.isTotallyBeaten();
		assert attacker.hasMissiles();
		assert distance <= attacker.getRange();
		assert distance > 0 || attacker.hasSpecial(SpecialType.MeleeShot);
		assert !defender.hasActiveHost();

		// Check for lone leader target
		if (defender.isLoneLeader()) {
			rangedAttack(attacker, defender.getLeader(), fullRate);
			return;
		}

		// Give shot to attacker leader
		if (attacker.hasActiveLeader()) {
			Solo leader = attacker.getLeader();
			if (leader.hasMissiles()
				&& distance <= leader.getRange())
			{
				rangedAttack(attacker.getLeader(), defender, fullRate);
			}
		}

		// Check for defender immune
		if (isAttackImmune(attacker, defender)) {
			reportDetail(attacker + " barred from shooting " + defender);
			return;
		}

		// Measure range & get modifier
		int rangeMod = (distance <= attacker.getRange() / 2)
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
			if (attacker.autoHits()
				|| rollToHit(atkBonus, defender.getArmor())) 
			{
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
		if (CAP_DAMAGE_BY_HEALTH) {
			damagePerHit = Math.min(damagePerHit, defender.getHealth());
		}
		int damageTotal = numHits * damagePerHit;
		applyDamage(attacker, defender, true, damageTotal);

		// Make the attacker visible
		makeVisible(attacker);
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
		if (defender.hasActiveHost()) {
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

		// Check for lone leader target
		if (defender.isLoneLeader()) {
			return isAttackImmune(attacker, defender.getLeader());		
		}

		// Invisible units can't be attacked
		if (!defender.isVisible()) {
			return true;
		}

		// Units that auto-hit (solos) assumed to bypass protections
		if (attacker.autoHits()) {
			return false;
		}

		// Check silver-to-hit (AD&D rule: 4HD+ bypasses)
		if (defender.hasSpecial(SpecialType.SilverToHit) && !USE_SILVER_WEAPONS
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
			&& !unit.isNormalBeaten()
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
		return total >= armor;
	}

	/**
	*  Check morale at end of turn.
	*/
	void checkMoraleEndTurn(Unit unit) {
		if (unit.getFigsLostInTurn() > 0) {
			int rateOfLoss = unit.getFigures() / unit.getFigsLostInTurn();
			checkMorale(unit, rateOfLoss);
		}
	}

	/**
	*  Make a morale check given the rate of loss.
	*  Sets routed field if failed.
	*/
	void checkMorale(Unit unit, int rateOfLoss) {

		// Check waivers
		if (unit.isFearless() || unit.isNormalBeaten()) {
			return;
		}

		// Compute bonus
		int health = unit.getHealth();
		int miscBonus = miscMoraleBonus(unit);
		int bonus = rateOfLoss + health + miscBonus;

		// Make the roll
		int roll = roll2d6();
		int total = roll + bonus;

		// Check and report
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
		if (unit.hasActiveLeader()) {
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
		int roll = (int) (random.nextDouble() / TERRAIN_MULTIPLIER * 100);

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
	*  Roll two 6-sided dice.
	*/
	int roll2d6() {
		return d6() + d6();	
	}

	/**
	*  Roll an arbitrary-sided die.
	*/
	int rollDie(int sides) {
		return random.nextInt(sides) + 1;
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
	*  Check for special abilities joint with melee atacks.
	*/
	void checkMeleeSpecials(Unit attacker, Unit defender) {

		// Jump out if no attacks to make
		if (attacker.isNormalBeaten()) {
			return;
		}

		// Check for fear ability
		if (bothUnitsLive(attacker, defender)
			&& attacker.hasSpecial(SpecialType.Fear)
			&& !defender.hasSavedVsFear())
		{
			checkFearAbility(attacker, defender);
		}

		// Check for breath weapon
		if (bothUnitsLive(attacker, defender)
			&& attacker.hasBreathWeapon() 
			&& attacker.getCharges() > 0)
		{
			useBreathWeapon(attacker, defender);
		}
	}

	/**
	*  Check the fear effect of dragons as they make contact.
	*/
	void checkFearAbility(Unit attacker, Unit defender) {
		assert distance == 0;
		assert attacker.hasSpecial(SpecialType.Fear);
		if (!attacker.isNormalBeaten()
			&& !defender.isFearless()) 
		{
			reportDetail(defender + " confronts * FEAR * ability");
			checkMorale(defender, 0);
			defender.setSavedVsFear(true);
		}
	}

	/**
	*  Check if a unit should act as a caster this turn.
	*  @return true if we took an action.
	*/
	boolean tryOneTurnCaster(Unit attacker, Unit defender) {

		// Jump out if the attacker is beaten
		if (attacker.isNormalBeaten()) {
			return false;		
		}

		// Jump out if we are controlling an Elemental
		if (attacker.hasActiveHost()
			&& attacker.getHost().hasSpecial(SpecialType.Conjured))
		{
			return false;		
		}

		// Storm giants control weather
		if (attacker.hasSpecial(SpecialType.WeatherControl)
			&& attacker.getCharges() > 0
			&& weather != getTargetWeather(attacker, defender))
		{
			castControlWeather(attacker, defender);
			attacker.decrementCharges();
			return true;
		}
		
		// Air Elementals Whirlwind attack
		if (attacker.hasSpecial(SpecialType.Whirlwind)) {
			doWhirlwindTurn(attacker, defender);
			return true;
		}			

		// Wizard top-level spell casting
		if (attacker.hasSpecial(SpecialType.Spells)
			&& attacker.getCharges() > 0)
		{
			if (doSpellTurn(attacker, defender)) {
				return true;
			}		
		}
		
		// Wizards with magic wands
		if (attacker.hasSpecial(SpecialType.Wand)) {
			doMagicWandTurn(attacker, defender);		
			return true;
		}
	
		return false;	
	}

	/**
	*  Try to use a wizard spell ability.
	*  @return true if we cast a spell.
	*/
	boolean doSpellTurn(Unit attacker, Unit defender) {
		assert attacker.hasSpecial(SpecialType.Spells);
		assert attacker.getCharges() > 0;
		
		// Cast Control Weather if it benefits us
		if (weather != getTargetWeather(attacker, defender)
			&& attacker.getCharges() 
				== attacker.getSpecialParam(SpecialType.Spells))
		{
			castControlWeather(attacker, defender);
			attacker.decrementCharges();
			return true;
		}

		// Cast Move Earth if it benefits us
		if (terrain == Terrain.Open) {
			castMoveEarth(attacker);
			attacker.decrementCharges();
			return true;		
		}

		// Cast Death Spell otherwise
		if (distance <= 24 
			&& defender.getHealth() <= 8
			&& !defender.getsSaves())
		{
			castDeathSpell(attacker, defender);		
			attacker.decrementCharges();
			return true;
		}
		
		// Else nothing
		return false;		
	}

	/**
	*  Take a turn as a magic wand wielder.
	*/
	void doMagicWandTurn(Unit attacker, Unit defender) {
		assert attacker.hasSpecial(SpecialType.Wand);

		// If out of range, move forward a bit.
		if (distance > WAND_RANGE) {
			distance--;
			return;		
		}
		
		// Shoot two lightning bolts per turn at target
		reportDetail(attacker + " shoots two * LIGHTNING *");
		int numShots = attacker.getFigures() * 2;
		for (int shot = 0; shot < numShots; shot++) {
			if (checkWandHit(defender)) {
				castEnergy(defender, 1, 6, EnergyType.Volt);
			}		
		}
	}

	/**
	*  Check if we can hit a target unit with a magic wand.
	*  Target center of unit & check variation.
	*/
	boolean checkWandHit(Unit target) {
		assert distance <= WAND_RANGE;
	
		// Get the shot error
		double shotError;
		if (distance > WAND_RANGE / 2) {
			shotError = Math.abs(roll2d6() - 7);
		}
		else if (distance > WAND_RANGE / 4) {
			shotError = Math.abs(rollDie(3) + rollDie(3) - 4);
		}
		else {
			shotError = 0;
		}
		
		// See if error is within length of target
		return shotError <= target.getTotalLength() / 2;
	}

	/**
	*  Cast a Move Earth spell.
	*  Assume this can move a Hill into a protective position for caster.
	*/
	void castMoveEarth(Unit attacker) {
		assert terrain == Terrain.Open;
		terrain = Terrain.Hill;
		reportDetail(attacker + " casts * MOVE EARTH * to get " + terrain);
	}

	/**
	*  Cast a Death Spell on the defending unit.
	*  (Damage amount here roughly averages OD&D and AD&D.)
	*/
	void castDeathSpell(Unit attacker, Unit defender) {
		assert distance <= 24;
		assert defender.getHealth() <= 8;
		assert !defender.getsSaves();
		int deathSpellDamage = 4;
		int numCasters = attacker.getFigures();
		int damage = deathSpellDamage * numCasters;
		defender.takeDamage(damage);
		reportDetail(attacker + " casts * DEATH SPELL * on " + defender);
	}

	/**
	*  Cast a Control Weather spell to our benefit.
	*/
	void castControlWeather(Unit attacker, Unit defender) {
		assert attacker.hasSpecial(SpecialType.Spells)
			|| attacker.hasSpecial(SpecialType.WeatherControl);
		assert weather != getTargetWeather(attacker, defender);
		int castings = 0;
		while (castings < attacker.getFigures()
			&& weather != getTargetWeather(attacker, defender)) 
		{
			weather = getTargetWeather(attacker, defender);
			reportDetail(attacker + " casts * CONTROL WEATHER * for " + weather);
			castings++;
		}
	}

	/**
	*  Determine the target weather value for a control spell.
	*/
	Weather getTargetWeather(Unit attacker, Unit defender) {
		int thirst = getThirst(attacker) - getThirst(defender);
		if (thirst > 0) {
			return CONTROL_WEATHER_STEPS == 1
				? incWeather() : Weather.Rainy;
		}
		else if (thirst < 0) {
			return CONTROL_WEATHER_STEPS == 1
				? decWeather() : Weather.Sunny;
		}
		else {
			return Weather.Cloudy;
		}
	}
	
	/**
	*  Get weather one step wetter than current.
	*/
	Weather incWeather() {
		switch (weather) {
			case Sunny: return Weather.Cloudy;
			default: return Weather.Rainy;
		}
	}
	
	/**
	*  Get weather one step dryer than current.
	*/
	Weather decWeather() {
		switch (weather) {
			case Rainy: return Weather.Cloudy;
			default: return Weather.Sunny;
		}
	}

	/**
	*  Check how thirsty (rain-desiring) a given unit is.
	*  @return positive if we desire rain, negative if want to avoid it
	*/
	int getThirst(Unit unit) {
		assert unit != null;
		int thirst = 0;
		if (unit.hasSpecial(SpecialType.Mounts)) {
			thirst--;
		}			
		if (unit.hasSpecial(SpecialType.LightWeakness)) {
			thirst++;
		}	
		if (unit.hasSpecial(SpecialType.GiantClass)) {
			thirst++;
		}	
		if (unit.hasSpecial(SpecialType.WeatherControl)) {
			thirst++;
		}
		if (unit.hasSpecial(SpecialType.Wand)) {
			thirst++;
		}	
		return thirst;	
	}

	/**
	*  Use a breath weapon.
	*/
	void useBreathWeapon(Unit attacker, Unit defender) {

		// Check preconditions
		assert distance == 0;
		assert attacker.hasBreathWeapon();
		assert attacker.getCharges() > 0;	
	
		// Determine how many figures hit (2" length)
		final double breathLength = 2.0;
		int hitPerAtkr = (int)(breathLength / defender.getFigLength());
		hitPerAtkr = Math.min(hitPerAtkr, defender.getRanks());
		hitPerAtkr = Math.max(hitPerAtkr, 1);
		int numHit = hitPerAtkr * countFiguresInContact(attacker, defender);

		// Cast the energy attack
		SpecialAbility breath = attacker.getBreathWeapon();
		reportDetail(attacker + " uses " + breath);
		castEnergy(defender, numHit, breath.getParam(),
			getBreathEnergy(breath.getType()));
		attacker.decrementCharges();
	}

	/**
	*  Cast energy damage on a number of figures in a unit.
	*/
	void castEnergy(Unit unit, int numFigs, int dmgPerFig, EnergyType energy) {

		// Check for lone leader
		if (unit.isLoneLeader()) {
			castEnergy(unit.getLeader(), 1, dmgPerFig, energy);
			return;
		}

		// Check for immunity or magic resistance
		if (isEnergyImmune(unit, energy)
			|| resistsMagic(unit))
		{
			return;
		}

		// Check for saving throw
		if (unit.getsSaves()) {
			int roll = roll2d6();
			if (roll >= dmgPerFig) {
				reportDetail(unit + " saves versus " + energy);
				return;
			}	
		}

		// Magic area damage is capped by figure health
		dmgPerFig = Math.min(dmgPerFig, unit.getHealth());
		int damage = dmgPerFig * numFigs;
		int figsLost = unit.takeDamage(damage);
		reportDetail(unit + " lost " + figsLost + " figures from " + energy);
	}

	/**
	*  Convert a breath weapon to an energy type.
	*/
	EnergyType getBreathEnergy(SpecialType breathType) {
		assert breathType.isBreathWeapon();	
		switch (breathType) {
			case FireBreath: return EnergyType.Fire;
			case VoltBreath: return EnergyType.Volt;
			case ColdBreath: return EnergyType.Cold;
			case AcidBreath: return EnergyType.Acid;
			case PoisonBreath: return EnergyType.Poison;
			case MultiBreath: return EnergyType.Multi;
			default: System.err.println("Unknown breath weapon type.");
				return null;
		}
	}

	/**
	*  Is this unit immune to this energy type?
	*/
	boolean isEnergyImmune(Unit unit, EnergyType energy) {
		switch (energy) {
			case Fire: return unit.hasSpecial(SpecialType.FireImmunity);
			case Volt: return unit.hasSpecial(SpecialType.VoltImmunity);
			case Cold: return unit.hasSpecial(SpecialType.ColdImmunity);
			case Acid: return unit.hasSpecial(SpecialType.AcidImmunity);
			case Poison: return unit.hasSpecial(SpecialType.PoisonImmunity);
			case Multi: return unit.hasSpecial(SpecialType.FireImmunity)
								&& unit.hasSpecial(SpecialType.PoisonImmunity);
			default: System.err.println("Unknown energy type.");
						return false;
		}
	}

	/**
	*  Set up for a Whirlwind pass-through attack on a target unit.
	*/
	void doWhirlwindTurn(Unit attacker, Unit defender) {

		// Concede if opponent is not a 1-health type.
		if (!defender.isSweepable()) {
			reportDetail(attacker + " * RETREATS *");
			attacker.setRouted(true);
			return;
		}

		// If out of range, move up half.
		if (distance >= getMove(attacker)) {
			distance -= getMove(attacker) / 2;
			return;
		}

		// If not alternate turn, move back a bit.
		if (d6() <= 3) {
			distance += getMove(attacker) / 4;
			return;
		}	
	
		// Do the attack: goes through defender in straight line
		// Sweeps away half of the figures it touches
		int numKilled = 0;
		int numFigsTouched = defender.getRanks();
		for (int i = 0; i < numFigsTouched; i++) {
			if (d6() <= 3) {
				numKilled++;
			}
		}
		reportDetail(attacker + " whirlwind sweeps away " + numKilled + " figures");
		defender.takeDamage(numKilled);
		
		// Move as much as possible to other side of target
		distance = getMove(attacker) - distance;
		return;
	}

	/**
	*  Check if a unit successfully resists magic.
	*  Balrog per OD&D Vol-2 1st print (etc.) has 75% resistance,
	*  roughly 6+ on 2d6 (could be 7+ for Wiz 14-16, ignored here).
	*  @return true if the unit resists magic.
	*/
	boolean resistsMagic(Unit unit) {
		return unit.hasSpecial(SpecialType.MagicResistance)
			&& roll2d6() >= 6;
	}

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
