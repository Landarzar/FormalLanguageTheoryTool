package edu.udo.cs.ls1.fltl.reg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.udo.cs.ls1.fltl.Tuple;

/***
 * Represents a (deterministic) finite state automata.
 * 
 * @author Kai Sauerwald
 *
 * @param <S>
 *            The
 * @param <A>
 */
public class FiniteStateAutomata<S, A> {
	private Set<S> states = new HashSet<>(3);
	private Set<S> accepting = new HashSet<>(3);
	private Map<Tuple<S, A>, S> transitions = new HashMap<>(0);
	private S initial = null;

	/***
	 * Performce one step of the Automata
	 * 
	 * @param q
	 * @param symbol
	 * @return
	 */
	public S step(S q, A symbol) {
		return transitions.get(new Tuple<S, A>(q, symbol));
	}
	
	/***
	 * Creates a new Finite State Automata
	 * 
	 * @param initial
	 *            the initial state of the automata
	 */
	public FiniteStateAutomata() {
	}

	/***
	 * Creates a new Finite State Automata
	 * 
	 * @param initial
	 *            the initial state of the automata
	 */
	public FiniteStateAutomata(S initial) {
		setInitialState(initial);
	}

	/***
	 * Copys the old fsa
	 * 
	 * @param fsa
	 */
	public FiniteStateAutomata(FiniteStateAutomata<S, A> fsa) {
		states = new HashSet<>(fsa.states);
		accepting = new HashSet<>(fsa.accepting);
		transitions = new HashMap<>(fsa.transitions);
		initial = fsa.initial;
	}

	public void setInitialState(S state) {
		// make sure the state is present\
		assert (state != null);
		states.add(state);
		this.initial = state;
	}

	public S getInitialState() {
		return initial;
	}

	public void addState(S state) {
		assert (state != null);
		states.add(state);
	}

	public Set<S> getStates() {
		return states;
	}

	/***
	 * Adds a new transition;
	 * 
	 * @param src
	 *            the source node
	 * @param sign
	 *            the sign
	 * @param tgt
	 *            the target node
	 */
	public void addTransition(S src, A sign, S tgt) {
		// make sure the state are present
		assert (src != null);
		assert (tgt != null);
		assert (sign != null);
		states.add(src);
		states.add(tgt);
		transitions.put(new Tuple<S, A>(src, sign), tgt);
	}

	/***
	 * Removes a Transition
	 * 
	 * @param src
	 *            The target transition
	 * @param sign
	 * @throws Exception
	 */
	public void RemoveTransition(S src, A sign) {
		throw new UnsupportedOperationException();
	}

	/***
	 * 
	 * @param state
	 * @return
	 */
	public Set<Tuple<A, S>> getTransitions(S state) {
		return transitions.entrySet().stream().filter(e -> e.getKey().first() == state)
				.map(e -> new Tuple<>(e.getKey().getSecond(), e.getValue())).collect(Collectors.toSet());
	}

	/***
	 * 
	 * @param state
	 * @return
	 */
	public Set<Map.Entry<Tuple<S, A>, S>> getTransitions() {
		return transitions.entrySet();
	}

	public Set<S> getAcceptingStates() {
		return accepting;
	}

	/***
	 * 
	 * @param state
	 * @return false if state is not part of the automata. true otherwise.
	 */
	public boolean setAccepting(S state, boolean accepting) {
		if (!states.contains(state))
			return false;

		if (accepting)
			this.accepting.add(state);
		else
			this.accepting.remove(state);

		return true;
	}

	/***
	 * 
	 * @param state
	 * @return true if state is an final state. false otherwise.
	 */
	public boolean isAccepting(S state) {
		return accepting.contains(state);
	}

	/***
	 * removes all states and transitions that cant be reached
	 */
	public void removeUnreachable() {
		HashSet<S> reached = new HashSet<>();
		LinkedList<S> queue = new LinkedList<>();
		queue.add(getInitialState());

		while (!queue.isEmpty()) {
			S q = queue.remove();
			assert (q != null);
			reached.add(q);

			for (Tuple<A, S> t : getTransitions(q)) {
				if (!reached.contains(t.second())) {
					queue.add(t.second());
				}
			}
		}

		states = reached;

		// remove transitions that are not up to date
		for (Iterator<Map.Entry<Tuple<S, A>, S>> iterator = getTransitions().iterator(); iterator.hasNext();) {
			Map.Entry<Tuple<S, A>, S> t = (Map.Entry<Tuple<S, A>, S>) iterator.next();
			if (!states.contains(t.getKey().first()) || !states.contains(t.getValue()))
				iterator.remove();
		}
	}
}
