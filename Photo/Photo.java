package Photo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.jacop.core.*;
import org.jacop.constraints.*;
import org.jacop.search.*;

public class Photo_sent {

	// Variables required from the example files.
	private static int n, n_prefs;
	private static int[][] prefs;

	public static void main(String[] args) {

		/**
		 * Choose which problem set to run. Valid choices are: 1, 2, 3 (the
		 * example files, with or without file ending), or any other file that
		 * uses the same structure as the example files. If the files are not in
		 * the project directory, set the ChosenProblem variable to the full
		 * file path.
		 * */

		String ChosenProblem = "1.java";
		parseProblemSet(ChosenProblem);

		// -------------------------------------------//
		// Choose to run standard or extended version //
		// Valid choices are: 1, or 2 ----------------//
		// -------------------------------------------//
		int ChosenVersion = 1;

		// ------------------------------------//
		// Variables for the program and setup //
		// ------------------------------------//

		// Store
		Store store = new Store();

		// Position vector
		IntVar[] pos = new IntVar[n];
		for (int i = 0; i < n; i++) {
			pos[i] = new IntVar(store, "" + (i + 1), 1, n);
		}

		// Cost variable
		IntVar cost = new IntVar(store, "Cost", -n_prefs, 0);

		// All individual costs
		IntVar[] costs = new IntVar[n_prefs];
		for (int i = 0; i < n_prefs; i++) {
			costs[i] = new IntVar(store, "costs" + i, -1, 0);
		}

		// --------------//
		// Define Search //
		// --------------//
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		SelectChoicePoint<IntVar> select = new InputOrderSelect<IntVar>(store,
				pos, new IndomainMin<IntVar>());

		// -------------------------------------------//
		// Run the program and try to find a solution //
		// -------------------------------------------//
		chosenVersion(ChosenVersion, n, n_prefs, prefs, store, pos, cost,
				costs, search, select);

	}

	/**
	 * Parses a problem file containing the variables used. Files must conform
	 * to the structure of the example files.
	 * 
	 * @param problem
	 *            The name of file if located in the project directory, or full
	 *            file path.
	 */
	private static void parseProblemSet(String problem) {
		if (!problem.contains(".java")) {
			problem += ".java";
		}

		String FILENAME = problem;
		BufferedReader br = null;
		FileReader fr = null;

		try {
			fr = new FileReader(FILENAME);
			br = new BufferedReader(fr);

			String sCurrentLine;
			String allLines = "";

			while ((sCurrentLine = br.readLine()) != null) {
				allLines += sCurrentLine;
			}
			String[] separated = allLines.split(";");
			n = Integer.parseInt(separated[0].split(" = ")[1]);
			n_prefs = Integer.parseInt(separated[1].split(" = ")[1]);
			prefs = new int[n_prefs][2];
			String relations = separated[2].substring(1,
					separated[2].length() - 1);
			int pref_index = 0;
			int rel_counter = 0;
			int digit_counter = 0;
			for (int i = 0; i < relations.length(); i++) {
				if (Character.isDigit(relations.charAt(i))) {
					digit_counter = i;
					while (digit_counter < relations.length()
							&& Character.isDigit(relations
									.charAt(digit_counter))) {
						digit_counter++;
					}
					prefs[pref_index][rel_counter] = Integer.parseInt(relations
							.substring(i, digit_counter));
					i = digit_counter;
					rel_counter++;
				}
				if (rel_counter == 2) {
					rel_counter = 0;
					pref_index++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				if (fr != null)
					fr.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Imposes constraints specific to the chosen version and searches for the
	 * solution.
	 * 
	 * @param version
	 *            Standard (1) or extended (2).
	 */
	private static void chosenVersion(int version, int n, int n_prefs,
			int[][] prefs, Store store, IntVar[] pos, IntVar cost,
			IntVar[] costs, Search<IntVar> search,
			SelectChoicePoint<IntVar> select) {

		parseInput(version, prefs, n, n_prefs, store, pos, costs);

		store.impose(new SumInt(store, costs, "==", cost));
		boolean result = search.labeling(store, select, cost);
		if (result && store.consistency()) {
			System.out.println("Solution: " + "Cost: " + (-cost.value()));
			// for (IntVar iv : pos) {
			// System.out.println(iv.toString());
			// }
			// for (IntVar iv : costs) {
			// System.out.println(iv.toString());
			// }
		}

	}

	/**
	 * Where the actual imposing takes place.
	 */
	private static void parseInput(int version, int[][] input, int n,
			int n_prefs, Store store, IntVar[] pos, IntVar[] costs) {
		IntVar[] results = new IntVar[n_prefs];
		store.impose(new Alldifferent(pos));
		for (int i = 0; i < n_prefs; i++) {
			results[i] = new IntVar(store, "i" + i, 0, n);
			store.impose(new Distance(pos[input[i][0] - 1],
					pos[input[i][1] - 1], results[i]));

			// Version decides what distance we allow: 1, or 2.
			store.impose(new Eq(new XlteqC(results[i], version), new XeqC(
					costs[i], -1)));
			store.impose(new Eq(new XgtC(results[i], version), new XeqC(
					costs[i], 0)));

		}
		for (int i = 0; i < n; i++) {
			new XneqC(pos[i], i + 1).impose(store);
		}
	}

}
