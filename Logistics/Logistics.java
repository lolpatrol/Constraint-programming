package Logistics;

import org.jacop.core.*;
import org.jacop.constraints.*;
import org.jacop.constraints.netflow.NetworkBuilder;
import org.jacop.constraints.netflow.NetworkFlow;
import org.jacop.constraints.netflow.simplex.Node;
import org.jacop.search.*;

public class Logistics {
	static int n_edges;
	static int n_dests;
	static int graph_size;
	static int start;
	static int[] dest;
	static int[] from;
	static int[] to;
	static int[] cost;

	public static void main(String[] args) {

		Store store = new Store();

		/**
		 * Run the examples by inputing a number in the method below. Valid
		 * choices are: 1, 2, or 3.
		 */
		chooseExampleProblem(3);

		/**
		 * Examples with start nodes other than 1, and more than 2 destinations.
		 * But still using the same paths.
		 * 
		 * The first is for problem 3 and starts in 5 and has dest={1,3,6}. The
		 * second is for problem 2 and starts in 3 and has dest={1,4,5,6}. The
		 * third is a weird graph we sketched, and is included as a totally
		 * different example, expected cost is 8, and included nodes are
		 * {1,2,3,5,6,7,8}.
		 */
		// --------- //
		// threeDestinations();
		// fourDestinations();
		// weirdGraph();
		// --------- //

		// This is the network vector.
		// Its size is determined by [source, edges in both direction, sinks].
		IntVar[] x = new IntVar[1 + n_dests + (n_edges * 2)];

		// Initiate the network builder.
		NetworkBuilder net = new NetworkBuilder();

		// Define source and sink.
		Node source = net.addNode("source", n_dests);
		Node sink = net.addNode("sink", -n_dests);

		x[0] = new IntVar(store, "source", 0, n_dests);

		// Define node vector.
		Node[] vertices = new Node[graph_size];

		// Create and add nodes to list, and add arc from source to start.
		for (int i = 0; i < graph_size; i++) {
			vertices[i] = net.addNode("" + (i + 1), 0);
			if (start == (i + 1)) {
				net.addArc(source, vertices[i], 0, x[0]);
			}
		}

		// Add arcs from->to and to->from and store corresponding IntVar in x.
		for (int i = 1; i <= n_edges; i++) {
			x[i] = new IntVar(store, from[i - 1] + "->" + to[i - 1], 0, n_dests);
			net.addArc(vertices[from[i - 1] - 1], vertices[to[i - 1] - 1],
					cost[i - 1], x[i]);

			x[i + n_edges] = new IntVar(store, to[i - 1] + "->" + from[i - 1],
					0, n_dests);
			net.addArc(vertices[to[i - 1] - 1], vertices[from[i - 1] - 1],
					cost[i - 1], x[i + n_edges]);
		}

		// Add arcs destinations->sinks.
		for (int i = 0; i < dest.length; i++) {
			int index = (x.length - n_dests) + i;
			x[index] = new IntVar(store, "sink " + (i + 1), 0, n_dests);
			net.addArc(vertices[dest[i] - 1], sink, 0, x[index]);

			// Flow should not be 0 at any sink.
			new XgtC(x[index], 0).impose(store);
		}

		// Array for collecting traversed edges, saving a "traversed state", 1,
		// regardless of the flow, and 0 otherwise. Only used for cost.
		IntVar[] myX = new IntVar[n_edges * 2];
		for (int i = 0; i < myX.length; i++) {
			myX[i] = new IntVar(store, 0, 1);

			// For any capacity (and flow) larger than 1, any two intersecting
			// "paths" (moving towards different sinks) will be larger than 1 in
			// the result vector. NetworkFlow
			// seems to do a weighted summation against the costs, and this is a
			// way of getting around that, and imposing my own weighted cost
			// on the network cost, while still maintaining the integrity of the
			// solution vector.
			// if flow > 1 (p) then set this vector to 1 (q), else
			// set it to its actual value (r).
			// (p -> q) ^ (not(p) -> r)
			// (not(p) v q) ^ (p v r)
			new And(new Or(new Not(new XgtC(x[i + 1], 1)), new XeqY(myX[i],
					new IntVar(store, 1, 1))), new Or(new XgtC(x[i + 1], 1),
					new XeqY(myX[i], x[i + 1]))).impose(store);
		}

		// Cost for traversing the network.
		IntVar myCost = new IntVar(store, "True cost", 0, maxCost(cost));

		// Vector for costs, used to sum the cost in the solution.
		int[] costs = new int[n_edges * 2];
		for (int i = 0; i < n_edges; i++) {
			costs[i] = cost[i];
			costs[i + n_edges] = cost[i];
		}

		// Impose a weighted summation.
		new SumWeight(myX, costs, myCost).impose(store);

		IntVar flowCost = new IntVar(store, "Flow cost", 0, maxCost(cost));
		net.setCostVariable(flowCost);

		store.impose(new NetworkFlow(net));

		// Search.
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		SelectChoicePoint<IntVar> select = new InputOrderSelect<IntVar>(store,
				x, new IndomainMin<IntVar>());
		boolean result = search.labeling(store, select, myCost);
		if (result) {
			System.out.println("Yay, look at this nice path!");
			System.out.println("The cost is: " + search.getCostValue());
			// System.out.println(myCost.toString());
			printPath(x);
		}
	}

	private static void chooseExampleProblem(int problem) {
		if (problem == 1) {
			setup1();
		}
		if (problem == 2) {
			setup2();
		}
		if (problem == 3) {
			setup3();
		}
	}

	private static void printPath(IntVar[] x) {
		String thePath = "------- This is the path -------\n";
		for (IntVar iv : x) {
			if (iv.value() != 0 && !iv.id().contains("source")
					&& !iv.id().contains("sink")) {
				thePath += " (" + iv.id() + ") ";
			}
		}
		System.out.println(thePath + "\n------- This is the path -------");
	}

	private static int maxCost(int[] costs) {
		int sum = 0;
		for (int cost : costs) {
			sum += cost;
		}
		return sum;
	}

	private static void setup1() {
		graph_size = 6;
		start = 1;
		n_dests = 1;
		dest = new int[] { 6 };
		n_edges = 7;
		from = new int[] { 1, 1, 2, 2, 3, 4, 4 };
		to = new int[] { 2, 3, 3, 4, 5, 5, 6 };
		cost = new int[] { 4, 2, 5, 10, 3, 4, 11 };
	}

	private static void setup2() {
		graph_size = 6;
		start = 1;
		n_dests = 2;
		dest = new int[] { 5, 6 };
		n_edges = 7;
		from = new int[] { 1, 1, 2, 2, 3, 4, 4 };
		to = new int[] { 2, 3, 3, 4, 5, 5, 6 };
		cost = new int[] { 4, 2, 5, 10, 3, 4, 11 };
	}

	private static void setup3() {
		graph_size = 6;
		start = 1;
		n_dests = 2;
		dest = new int[] { 5, 6 };
		n_edges = 9;
		from = new int[] { 1, 1, 1, 2, 2, 3, 3, 3, 4 };
		to = new int[] { 2, 3, 4, 3, 5, 4, 5, 6, 6 };
		cost = new int[] { 6, 1, 5, 5, 3, 5, 6, 4, 2 };
	}

	private static void threeDestinations() {
		graph_size = 6;
		start = 5;
		n_dests = 3;
		dest = new int[] { 1, 3, 6 };
		n_edges = 9;
		from = new int[] { 1, 1, 1, 2, 2, 3, 3, 3, 4 };
		to = new int[] { 2, 3, 4, 3, 5, 4, 5, 6, 6 };
		cost = new int[] { 6, 1, 5, 5, 3, 5, 6, 4, 2 };
	}

	private static void fourDestinations() {
		graph_size = 6;
		start = 3;
		n_dests = 4;
		dest = new int[] { 1, 4, 5, 6 };
		n_edges = 7;
		from = new int[] { 1, 1, 2, 2, 3, 4, 4 };
		to = new int[] { 2, 3, 3, 4, 5, 5, 6 };
		cost = new int[] { 4, 2, 5, 10, 3, 4, 11 };
	}

	private static void weirdGraph() {
		graph_size = 8;
		start = 1;
		n_dests = 3;
		dest = new int[] { 5, 7, 8 };
		n_edges = 9;
		from = new int[] { 1, 1, 2, 2, 3, 3, 4, 4, 6 };
		to = new int[] { 2, 4, 5, 6, 5, 7, 6, 7, 8 };
		cost = new int[] { 1, 2, 2, 1, 2, 1, 2, 2, 1 };
	}
}