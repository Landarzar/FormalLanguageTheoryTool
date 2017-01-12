package edu.udo.cs.ls1.fltl;

/***
 * An implemtation of immutable tuples
 * 
 * @author Kai Sauerwald
 *
 */
public class Tuple<A, B> {

	private A f;
	private B s;

	public Tuple(A first, B second) {
		this.f = first;
		this.s = second;
	}

	/***
	 * Gets the first element of this tuple
	 * 
	 * @return
	 */
	public A first() {
		return f;
	}

	public A getFirst() {
		return f;
	}

	public B second() {
		return s;
	}

	public B getSecond() {
		return s;
	}
	
	@Override
	public int hashCode() {
		return f.hashCode() ^ s.hashCode();
	}

	/***
	 * @param obj
	 *            the object to compare with
	 * @return true, if obj is a Tuple and both elements a equal. false
	 *         otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Tuple<?, ?>) {
			Tuple<?, ?> t = (Tuple<?, ?>) obj;
			return this.f.equals(t.f) && this.s.equals(t.s);
		}
		return false;
	}

	/***
	 * Give a String representation of this Tuple, in the common way (a,b).
	 */
	@Override
	public String toString() {
		return "(" + f.toString() + "," + s.toString() + ")";
	}
}
