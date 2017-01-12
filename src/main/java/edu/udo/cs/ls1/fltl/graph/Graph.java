package edu.udo.cs.ls1.fltl.graph;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import edu.udo.cs.ls1.fltl.Tuple;

public interface Graph<V> {

	/***
	 * 
	 * @return the Set of vertices
	 */
	public Set<V> getVertices();

	/***
	 * 
	 * @return the edges
	 */
	public Set<Tuple<V, V>> getEdges();

	/***
	 * 
	 * @param vertex
	 * @return
	 */
	public default Set<V> getNeighbors(V vertex) {
		return getEdges().stream().filter(t -> t.first() == vertex).map(t -> t.second()).collect(Collectors.toSet());
	}
}
