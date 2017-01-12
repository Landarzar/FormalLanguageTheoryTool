package edu.udo.cs.ls1.fltl.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import edu.udo.cs.ls1.fltl.reg.FiniteStateAutomata;

/***
 * Provides methods for dealing with graphs
 * @author Kai Sauerwald
 *
 * @param <S> The Type of the vertecies
 * @param <A> The type of the edge labes
 */
public class GraphHelper<S, A> {

	public static void main(String[] args) {
		FiniteStateAutomata<String, Character> fsa = new FiniteStateAutomata<>("0");

		fsa.addTransition("0", 'b', "0");
		fsa.addTransition("0", 'a', "1");
//		fsa.addTransition("1", 'b', "0");
//		fsa.addTransition("1", 'a', "1");

		GraphHelper<String, Character> helper = new GraphHelper<>();
		System.out.println(helper.tarjanSCC(fsa));
		;
	}

	Integer index = 0;
	HashSet<Set<S>> result = null;
	LinkedList<Node> S = null;

	/***
	 * A Helperclass for The Tarjan algorithmn
	 * 
	 * @author Kai Sauerwald
	 *
	 */
	private class Node {
		public Node(S s, int i) {
			this.state = s;
			this.index = i;
		}

		/***
		 * a index for the tarjan SCC algorithmn.
		 */
		int index;

		/***
		 * a index for the tarjan SCC algorithmn.
		 */
		int lowlink;

		/***
		 * a index for the tarjan SCC algorithmn.
		 */
		boolean onStack = false;

		List<Node> neighbors = new ArrayList<>();

		/***
		 * The corresponding state
		 */
		S state;

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "{" + "\nindex=" + index + "\nlowlink=" + lowlink + "\nonStack=" + onStack + "\nstate=" + state + "}";
		}
	}

	/***
	 * This is an implementation of Tarjans strongly connected components
	 * alogrithm.
	 * 
	 * @param A
	 * 
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Wikipedia</a>
	 * 
	 * @param A
	 * @return
	 */
	public Set<Set<S>> tarjanSCC(FiniteStateAutomata<S, A> A) {
		result = new HashSet<>();
		S = new LinkedList<>();

		List<Node> V = A.getStates().stream().map(s -> new Node(s, -1)).collect(Collectors.toList());
		computeNeighbors(V, A);

		index = 0;

		for (Node v : V) {
			if (v.index == -1)
				strongconnect(v);
		}
		return result;
	}

	private void computeNeighbors(List<Node> V, FiniteStateAutomata<S, A> A) {
		for (Node v : V) {
			V.stream().filter(w -> A.getTransitions(v.state).stream().anyMatch(t -> t.getSecond() == w.state))
					.forEach(w -> v.neighbors.add(w));
		}
	}

	private void strongconnect(Node v) {
		v.index = index;
		v.lowlink = index;
		index = index + 1;
		S.push(v);
		v.onStack = true;

		v.neighbors.forEach(w -> {
			if (w.index == -1) {
				strongconnect(w);
				v.lowlink = Math.min(v.lowlink, w.lowlink);
			} else if (w.onStack) {
				v.lowlink = Math.min(v.lowlink, w.lowlink);
			}
		});

		if (v.lowlink == v.index) {
			// a scc is found
			HashSet<S> scc = new HashSet<>();
			Node w = null;
			do{
				w = S.pop();
				w.onStack=false;
				scc.add(w.state);
			}
			while(w != v);
			result.add(scc);
		}
	}
}
