package edu.udo.cs.ls1.fltl.reg.bkw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import edu.udo.cs.ls1.fltl.Tuple;
import edu.udo.cs.ls1.fltl.graph.GraphHelper;
import edu.udo.cs.ls1.fltl.reg.FiniteStateAutomata;

/**
 * This class provides an implementation of the Algorithm from Brüggemann-Klein
 * and Wood [1]. It computes for a given deterministic finite automata a
 * deterministic regular expression. Its based on an optimised version from Bex,
 * Gelade, Martens and Neven [2].
 * 
 * [1] One-Unambiguous Regular Languages, Brüggemann-Klein and Wood [2]
 * Simplifying XML Schema: Effortless Handling of Nondeterministic Regular
 * Expressions, Bex and Gelade and Martens and Neven
 * 
 * @author Kai Sauerwald
 *
 */
public class BKW<S> {

	public static void main(String[] args) {

		// RegExp exp = new RegExp("(0|1)*0");
		// Automaton a = exp.toAutomaton();
		//
		// System.out.println(computeBKW(a));

		FiniteStateAutomata<String, Character> fsa = new FiniteStateAutomata<>("0");

		fsa.addState("1");
		fsa.addTransition("0", 'b', "0");
		fsa.addTransition("0", 'a', "1");
		fsa.addTransition("1", 'b', "0");
		fsa.addTransition("1", 'a', "1");
		fsa.setAccepting("1", true);

		HashSet<Character> sigma = new HashSet<>();
		sigma.add('a');
		sigma.add('b');
		System.out.println(new BKW<String>().computeBKW(fsa, sigma));
	}

	/***
	 * translates from one representation to another
	 * 
	 * @param aut
	 * @return
	 */
	private static FiniteStateAutomata<State, Character> minimalDKAutomata2FSA(Automaton aut) {
		FiniteStateAutomata<State, Character> result = new FiniteStateAutomata<State, Character>(aut.getInitialState());

		aut.getStates().forEach(s -> {
			result.addState(s);
			s.getTransitions().forEach(t -> {
				for (char c = t.getMin(); c <= t.getMax(); c++) {
					result.addTransition(s, c, t.getDest());
				}
			});
		});

		aut.getAcceptStates().forEach(s -> {
			result.setAccepting(s, true);
		});

		return result;
	}

	/***
	 * This method computes for a given deterministic finite automata a
	 * deterministic regular expression.
	 * 
	 * @param automata
	 * @return a deterministic regular expression (as String) with the same
	 *         language as the automata. If there is no equivalent deterministic
	 *         regular expression, it returns null.
	 */
	public static String computeBKW(Automaton automata) {
		// check if input is deterministic and minimal
		if (!automata.isDeterministic())
			automata.determinize();
		automata.minimize();

		FiniteStateAutomata<State, Character> fsa = minimalDKAutomata2FSA(automata);

		Set<Character> alphabet = fsa.getTransitions().stream().map(t -> t.getKey().second())
				.collect(Collectors.toSet());

		return new BKW<State>().computeBKW(fsa, alphabet);
	}

	/***
	 * This method computes for a given deterministic finite automata a
	 * deterministic regular expression.
	 * 
	 * @param automata
	 * @return a deterministic regular expression (as String) with the same
	 *         language as the automata. If there is no equivalent deterministic
	 *         regular expression, it returns null.
	 */
	public String computeBKW(FiniteStateAutomata<S, Character> fsa, Set<Character> alphabet) {

		// If the Automata has only one state q and no transitions then return
		// epsilon if q is final or emptyset.
		if (fsa.getStates().size() == 1 && fsa.getTransitions().isEmpty()) {
			if (fsa.isAccepting(fsa.getInitialState()))
				return "λ"; // () is the empty word in
							// dk.brics.automata
			else
				return "#"; // # is the empty String in
							// dk.brics.automata
		}

		List<Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>>> orbits = computeOrbits(fsa);

		if (orbits.size() == 1) {
			Set<Character> S = computeConsistentSymbols(fsa, alphabet);
			// If we have exactly one orbit and no consistent symbols, we fail
			if (S.isEmpty())
				throw new RuntimeException("Fails the Consistency");

			// Now we compute bkw(A_s)\\union(\bigunion_{a\in S} bkw(A_S^{w(a)})
			// ),
			// where w(a) is the unique state that is reached from the accepting
			// states (scince a is consistent, this exist)
			FiniteStateAutomata<S, Character> scut = computeScut(fsa, S);

			String s1 = computeBKW(scut, alphabet);
			String s2 = S.stream().map(a -> "+" + a + "("
					+ computeBKW(computeQAutomata(computeScut(fsa, S), computeWitnessStateForSymbol(fsa, a)), alphabet)
					+ ")").reduce("", String::concat).substring(1);

			return "(" + s1 + ")(" + s2 + ")*";
		} else {

			// if the automaton has not the orbitproperty we fail
			if (!testOrbitProperty(fsa, orbits))
				throw new RuntimeException("Fails the Orbitproperty");

			// compute unique states for startorbit
			HashMap<Character, S> us = new HashMap<>(); // State q_a
			for (Character a : alphabet) {
				S sym = computeStateForSymbol(fsa, orbits, a);
				if (sym != null)
					us.put(a, sym);
			}

			FiniteStateAutomata<S, Character> q0 = computeOrbitAutomata(fsa, orbits, fsa.getInitialState()); // Orbitautomaton
			// q_0

			String s1 = computeBKW(q0, alphabet);
			String s2 = alphabet.stream().map(a -> {
				if (us.get(a) == null)
					return "";
				return "+" + a + "(" + computeBKW(computeQAutomata(fsa, us.get(a)), alphabet) + ")";
			}).reduce("", String::concat).substring(1);

			if (q0.getAcceptingStates().stream().anyMatch(q -> fsa.getAcceptingStates().contains(q))) {
				// Now return bkw(A_{q_0})\\union(\bigunion_{a\in \Sigma}
				// bkw(A^{q_a)})?)
				return "(" + s1 + ")(" + s2 + ")?";
			} else // Now return bkw(A_{q_0})\\union(\bigunion_{a\in \Sigma}
					// bkw(A^{q_a)}))
				return "(" + s1 + ")(" + s2 + ")";
		}
	}

	private class Gate {

		public Gate(S src) {
			this.src = src;
		}

		S src;
		List<Tuple<Character, S>> outgoing = new ArrayList<>();
	}

	/***
	 * Computes for the given Automata all the Oribits
	 * 
	 * @param automata
	 * @return
	 */
	public List<Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>>> computeOrbits(
			FiniteStateAutomata<S, Character> automata) {
		List<Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>>> orbits = new LinkedList<>();

		GraphHelper<S, Character> help = new GraphHelper<S, Character>();
		// Calculates the strongly conntected components of the automata
		Set<Set<S>> scc = help.tarjanSCC(automata);

		for (Set<S> orbit : scc) {
			// Achtung, keinen Startzustand zu setzen könnte probleme machen
			FiniteStateAutomata<S, Character> fsa = new FiniteStateAutomata<S, Character>();
			HashMap<S, Gate> gateList = new HashMap<>();
			
			for (S s : orbit) {
				fsa.addState(s);
			}

			// Get gates and
			for (Entry<Tuple<S, Character>, S> transition : automata.getTransitions()) {
				S q = transition.getKey().first();
				Character c = transition.getKey().second();
				S p = transition.getValue();
				// if transition begins from a state q in the orbit
				if (orbit.contains(q)) {
					// and it stays in the orbit, then add the transition
					if (orbit.contains(p))
						fsa.addTransition(q, c, p);
					else // otherwise q is a gate, and should be added to the
							// gatelist
					{
						if (!gateList.containsKey(q))
							gateList.put(q, new Gate(q));
						gateList.get(q).outgoing.add(new Tuple<Character, S>(c, p));
					}
				}
			}
			// add the gates that are final states
			for (S q : orbit) {
				automata.isAccepting(q);
				fsa.setAccepting(q, true);
				if (!gateList.containsKey(q))
					gateList.put(q, new Gate(q));
			}

			orbits.add(new Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>>(fsa, gateList.values()));
		}

		return orbits;
	}

	/***
	 * Tests if a Automata fullfills the orbit propertie. This is for every two
	 * gates q_1 and q_2 of a orbit the following holds: 1. q_1 is finial iff
	 * q_2 is final, and 2. for all states q outside of the orbit there is a
	 * transition (q_1,a,q) iff (q_2,a,q).
	 * 
	 * @param fsa
	 * @return
	 */
	public boolean testOrbitProperty(FiniteStateAutomata<S, Character> fsa,
			List<Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>>> orbits) {
		for (Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>> tuple : orbits) {
			FiniteStateAutomata<S, Character> orbit = tuple.first();
			Collection<Gate> gates = tuple.getSecond();

			for (Gate q1 : gates) {
				for (Gate q2 : gates) {
					if (q1 == q2)
						continue;
					// q1 have to be finial iff q2
					if (fsa.isAccepting(q1.src) ^ fsa.isAccepting(q2.src))
						return false;
					for (Tuple<Character, S> t : q1.outgoing) {
						// for all outgoing transitions (w.r.t. orbit of q1 and
						// q2, the have to be behave like eachother
						if (!q2.outgoing.contains(t))
							return false;
					}
				}
			}
		}

		return true;
	}

	private S computeWitnessStateForSymbol(FiniteStateAutomata<S, Character> fsa, Character a) {
		for (S q : fsa.getAcceptingStates())
			return fsa.step(q, a);
		throw new UnsupportedOperationException(); // this should never ever
													// happen!
	}

	/***
	 * Computes for a automaten, the automaton with q as initial state, and the
	 * set of states restricted to the reachable states from q
	 * 
	 * @param automata
	 * @param q
	 * @return
	 */
	private FiniteStateAutomata<S, Character> computeQAutomata(FiniteStateAutomata<S, Character> fsa, S q) {
		FiniteStateAutomata<S, Character> result = new FiniteStateAutomata<S, Character>(fsa);

		result.setInitialState(q);
		result.removeUnreachable();

		return result;
	}

	/***
	 * Computes the oribit automata. That is the automata that is obtained by
	 * setting q as the initial, and then restriciting to the stats of the Orbit
	 * of q and making alle the gates of orbit(q) final.
	 * 
	 * @param fsa
	 * @param q
	 * @return
	 */
	private FiniteStateAutomata<S, Character> computeOrbitAutomata(FiniteStateAutomata<S, Character> fsa,
			List<Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>>> orbits, S state) {
		// finite the orbit from state;
		Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>> orbit = findOrbitofState(orbits, state);
		FiniteStateAutomata<S, Character> result = new FiniteStateAutomata<>(orbit.first());

		result.getAcceptingStates().clear();
		result.setInitialState(state);
		result.removeUnreachable();
		for (Gate g : orbit.getSecond()) {
			result.setAccepting(g.src, true);
		}

		return result;
	}

	/***
	 * Computes the unique state that is reached by leaving the oribit of the
	 * initial state under the symbol a.
	 * 
	 * @param fsa
	 * @param orbits
	 * @param a
	 * @return
	 */
	private S computeStateForSymbol(FiniteStateAutomata<S, Character> fsa,
			List<Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>>> orbits, Character a) {

		// finite the orbit from initial state;
		Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>> orbit = findOrbitofState(orbits,
				fsa.getInitialState());

		for (Gate gate : orbit.getSecond()) {
			for (Tuple<Character, S> og : gate.outgoing) {
				if (og.getFirst() == a)
					return og.getSecond();
			}
		}

		return null;
		// throw new UnsupportedOperationException(); // this should never ever
		// happen
	}

	private Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>> findOrbitofState(
			List<Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>>> orbits, S state) {
		for (Tuple<FiniteStateAutomata<S, Character>, Collection<Gate>> o : orbits) {
			if (o.first().getStates().contains(state)) {
				return o;
			}
		}
		throw new UnsupportedOperationException(); // this should never ever
		// happen
	}

	/***
	 * The S-cut is obtained by removing any transition (q,a,p) from the
	 * automata, where q is a finite state and a in S. *
	 * 
	 * @param automata
	 * @param s
	 *            The set S
	 * @return
	 */
	private FiniteStateAutomata<S, Character> computeScut(FiniteStateAutomata<S, Character> fsa, Set<Character> s) {
		FiniteStateAutomata<S, Character> result = new FiniteStateAutomata<S, Character>(fsa);

		result.getAcceptingStates().forEach(q -> {
			for (Iterator<Map.Entry<Tuple<S, Character>, S>> iterator = result.getTransitions().iterator(); iterator
					.hasNext();) {
				// Remove transition if sign is in S
				Map.Entry<Tuple<S, Character>, S> entry = iterator.next();
				if (entry.getKey().first() == q && s.contains(entry.getKey().getSecond())) {
					iterator.remove();
				}
			}
		});

		return result;
	}

	/***
	 * Computes all consistent symbols. That are the symbols for that a state q
	 * exists, such that all accepting state have a transition to q for that
	 * symbol.
	 * 
	 * @param automata
	 * @return The set of consistent symbols.
	 */
	private Set<Character> computeConsistentSymbols(FiniteStateAutomata<S, Character> fsa, Set<Character> alpabet) {

		if (fsa.getAcceptingStates().size() == 0)
			return alpabet;

		Set<Character> result = new HashSet<>();

		alpabet.forEach(c -> {
			S q = null;
			for (S a : fsa.getAcceptingStates()) {
				S r = fsa.step(a, c);
				if (r == null) // sollte nicht passieren
					continue;
				if (q == null) {
					q = r;
					continue;
				}
				// Symbol is not consistent
				if (q != r) {
					q = null;
					break;
				}
			}
			if (q != null)
				result.add(c);
		});

		return result;
	}
}
