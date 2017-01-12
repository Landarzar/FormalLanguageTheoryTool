package edu.udo.cs.ls1.fltl.reg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import edu.udo.cs.ls1.fltl.Tuple;
import edu.udo.cs.ls1.fltl.reg.bkw.BKW;

/**
 * Hello world!
 *
 */
public class Anzeige {
	private static String s2i(Automaton aut, State s) {
		return "q" + (s.compareTo(aut.getInitialState()) + aut.getNumberOfStates()) % (aut.getNumberOfStates() + 1);
	}

	private static String c2sym(char c, boolean isEnabled) {
		if (c == '|')
			return "+";
		if (isEnabled) {
			if (c >= 'a' && c <= 'z') {
				return "a<SUB>" + (c - 'a' + 1) + "</SUB>";
			} else if (c >= 'A' && c <= 'Z') {
				return "b<SUB>" + (c - 'A' + 1) + "</SUB>";
			} else if (c >= '0' && c <= '9') {
				return "c<SUB>" + (c - '0' + 1) + "</SUB>";
			}
		}
		return "" + c;
	}

	private static String s2t(Automaton aut, State s, boolean isEnabled) {
		return s.getTransitions().stream()
				.map(t -> s2i(aut, s) + " -> " + s2i(aut, t.getDest()) + " [ label = <" + c2sym(t.getMin(), isEnabled)
						+ (t.getMin() != t.getMax() ? " - " + c2sym(t.getMax(), isEnabled) : "") + "> ];\n")
				.reduce("", String::concat);
	}

	public static String automataToGraphViz(Automaton aut, String caption, boolean isEnabled) {
		String text = "digraph finite_state_machine { graph [label=<" + caption + ">];\n" + "	rankdir=LR;\n"
				+ "	size=\"8,5\"\n" + "  node [shape = point ]; qi \n" + "node [shape = doublecircle]; "
				+ aut.getAcceptStates().stream().map(s -> s2i(aut, s) + " ").reduce("", String::concat) + ";\n"
				+ "	node [shape = circle];\n" + " qi -> " + s2i(aut, aut.getInitialState()) + ";\n"
				+ aut.getStates().stream().map(s -> s2t(aut, s, isEnabled)).reduce("", String::concat) + "}";

		return text;
	}

	static class Stats {
		String fn;
		int states;
		int transitions;
	}

	private static Automaton showMinA2REXP(String regexp, List<Fragment> fn, List<Stats> stats,
			boolean charResetisEnabled) throws IOException, InterruptedException {
		return showMinA2REXP(regexp, fn, stats, false, false, charResetisEnabled);
	}

	private static Automaton showMinA2REXP(String regexp, List<Fragment> fn, List<Stats> stats,
			boolean charResetisEnabled, boolean show, boolean write) throws IOException, InterruptedException {

		RegExp rep = new RegExp(regexp);
		Automaton aut1 = rep.toAutomaton(true);

		Stats stat = new Stats();

		stat.fn = fn.toString();
		stat.states = aut1.getNumberOfStates();
		stat.transitions = aut1.getNumberOfTransitions();
		stats.add(stat);

		if (write) {
			// File file = File.createTempFile("graph", ".dot");
			File file = new File("/tmp/graph" + fn.size()
					+ fn.stream().map(f -> " " + f.toString()).reduce("", String::concat).substring(1) + ".dot");
			File filePDF = new File(file.getAbsolutePath() + ".pdf");
			PrintWriter writer = new PrintWriter(file);

			ArrayList<String> aList = new ArrayList<>(regexp.length());

			for (int i = 0; i < regexp.toCharArray().length; i++) {
				aList.add(c2sym(regexp.charAt(i), charResetisEnabled));
			}

			writer.write(automataToGraphViz(aut1, "Graph zu " + aList.stream().reduce("", String::concat),
					charResetisEnabled));
			writer.flush();
			writer.close();

			ProcessBuilder dot = new ProcessBuilder("/usr/bin/dot", "-Tpdf");
			dot.redirectOutput(filePDF);
			dot.redirectInput(Redirect.from(file));
			Process pdot = dot.start();
			pdot.waitFor();

			if (show) {
				ProcessBuilder evince = new ProcessBuilder("/usr/bin/evince", filePDF.getAbsolutePath());
				evince.start();
			}
		}

		return aut1;
	}

	enum Fragment {
		SIGN, SIGN_STARF, SIGN_STARB, SIGN_PLUSF, SIGN_PLUSB, SIGN_Q
	}

	private static String createPattern(char[] signs, Fragment fragment) {
		ArrayList<Character> sig = new ArrayList<>(signs.length);
		for (int i = 0; i < signs.length; i++) {
			sig.add(signs[i]);
		}
		switch (fragment) {
		case SIGN:
			return "(" + sig.stream().map(c -> "|" + c).reduce("", String::concat).substring(1) + ")";
		case SIGN_STARF:
			return "(" + sig.stream().map(c -> "|" + c).reduce("", String::concat).substring(1) + ")*";
		case SIGN_STARB:
			return "(" + sig.stream().map(c -> "|" + c + "*").reduce("", String::concat).substring(1) + ")";
		case SIGN_PLUSF:
			return "(" + sig.stream().map(c -> "|" + c).reduce("", String::concat).substring(1) + ")+";
		case SIGN_PLUSB:
			return "(" + sig.stream().map(c -> "|" + c + "+").reduce("", String::concat).substring(1) + ")";
		case SIGN_Q:
			return "(" + sig.stream().map(c -> "|" + c).reduce("", String::concat).substring(1) + ")?";
		}

		return "";
	}

	public static void computeFourFragments() throws IOException, InterruptedException {
		List<Stats> stats = new LinkedList<>();

		Fragment[] l1 = new Fragment[2];
		Fragment[] l2 = new Fragment[3];
		Fragment[] l3 = new Fragment[4];
		for (Fragment f1 : Fragment.values()) {
			l1[0] = f1;
			l2[0] = f1;
			l3[0] = f1;
			for (Fragment f2 : Fragment.values()) {
				l1[1] = f2;
				l2[1] = f2;
				l3[1] = f2;
				String s11 = createPattern(new char[] { 'a', 'b', 'c', 'd' }, f1);
				String s12 = createPattern(new char[] { 'a', 'b', 'A', 'B' }, f2);
				showMinA2REXP(s11 + s12, Arrays.asList(l1), stats, true);
				for (Fragment f3 : Fragment.values()) {
					l2[2] = f3;
					l3[2] = f3;
					String s21 = createPattern(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h' }, f1);
					String s22 = createPattern(new char[] { 'a', 'b', 'c', 'd', 'A', 'B', 'C', 'D' }, f2);
					String s23 = createPattern(new char[] { 'a', 'b', 'e', 'f', 'A', 'B', '0', '1' }, f3);
					showMinA2REXP(s21 + s22 + s23, Arrays.asList(l2), stats, true);
					for (Fragment f4 : Fragment.values()) {
						l3[3] = f4;
						String s31 = createPattern(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' }, f1);
						String s32 = createPattern(
								new char[] { 'a', 'b', 'c', 'd', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H' }, f2);
						String s33 = createPattern(
								new char[] { 'a', 'b', 'e', 'f', 'A', 'B', 'C', 'D', '0', '1', '2', '3' }, f3);
						String s34 = createPattern(
								new char[] { 'a', 'b', 'g', 'h', 'A', 'B', 'E', 'F', '0', '1', 'x', 'y' }, f4);
						showMinA2REXP(s31 + s32 + s33 + s34, Arrays.asList(l3), stats, true);
					}
				}
			}
		}
		showStats(stats);
	}

	private static void showStats(List<Stats> stats) {
		System.out.println("Now sorting...");
		stats.sort(new Comparator<Stats>() {

			@Override
			public int compare(Stats o1, Stats o2) {

				if (o1.states > o2.states)
					return 1;
				else if (o1.states < o2.states)
					return -1;

				if (o1.transitions > o2.transitions)
					return 1;
				else if (o1.transitions < o2.transitions)
					return -1;

				return 0;
			}
		});
		System.out.println("finish sort sorting...");

		for (Stats s : stats) {

			System.out.println("Stats for" + s.fn + ":");
			System.out.println("States: " + s.states + ", Transitions: " + s.transitions);
		}
	}

	private <E> E getRandomObject(Collection<E> from) {
		Random rnd = new Random();
		int i = rnd.nextInt(from.size());
		return from.toArray((E[]) null)[i];
	}

	/***
	 * Berechnet
	 * 
	 * @param minFragments
	 * @param maxFragments
	 * @param minElements
	 * @param maxElements
	 */
	private static void computeFragmentsRnd(int minFragments, int maxFragments, int minElements, int maxElements)
			throws IOException, InterruptedException {
		Random rnd = new Random();

		List<Stats> stats = new LinkedList<>();

		for (int i = minFragments; i <= maxFragments; i++) {

			Fragment[] fgm = new Fragment[i];
			for (int j = 0; j < fgm.length; j++) {
				fgm[j] = Fragment.values()[rnd.nextInt(Fragment.values().length)];
			}

			Supplier<char[]> mkchar = () -> {
				char[] result = new char[minElements + rnd.nextInt(maxElements - minElements + 1)];

				for (int j = 0; j < result.length; j++) {
					result[j] = (char) ('a' + rnd.nextInt('z' - 'a' + 1));
				}

				return result;
			};

			showMinA2REXP(Arrays.stream(fgm).map(f -> createPattern(mkchar.get(), f)).reduce("", String::concat),
					Arrays.asList(fgm), stats, true);

		}
		showStats(stats);
	}

	public static Fragment[] produce(int n, Fragment f) {
		Fragment[] res = new Fragment[n];

		for (int i = 0; i < res.length; i++) {
			res[i] = f;
		}

		return res;
	}

	private static Automaton doTheStuff(boolean showStats, boolean statsSort, boolean showAutomata, int notover,
			String outputFile, String input, List<Stats> stats, int n, boolean charRepaceIsEnabled)
			throws IOException, FileNotFoundException, InterruptedException {
		String in = input.replace("^*", "*").replace("^{n^2}", "{" + n * n + "}").replace("^{2n}", "{" + 2 * n + "}").replace("^{n}", "{" + n + "}")
				.replace("^{1,n}", "{1," + n + "}").replace("^{0,n}", "{0," + n + "}").replace("^{1,2n}", "{1," + 2*n + "}").replace("^{0,2n}", "{0," +2* n + "}").replace("^n", "{" + n + "}").replace("n", "{" + n + "}").replace(" ", "");

		RegExp rep = new RegExp(in.replace("^+", "ß").replace('+', '|').replace("ß", "+"));

		Instant time = Instant.now();
		Automaton aut1 = rep.toAutomaton(true);
		Duration timediff = Duration.between(time, Instant.now());

		if (showStats) {

			Stats stat = new Stats();
			if (statsSort) {
				stat.fn = in;
				stat.states = aut1.getNumberOfStates();
				stat.transitions = aut1.getNumberOfTransitions();
				stats.add(stat);
			} else {
				System.out.println("\n--------- n=" +n +" ---------------");
				System.out.println("Regex: " + in + " (Zeit: " + timediff.toString() + ")\nStates: " + aut1.getNumberOfStates()
						+ ", Trans.: " + aut1.getNumberOfTransitions());
			}
		}

		if ((showAutomata && (notover == -1 || notover >= aut1.getNumberOfStates())) || outputFile != null) {
			File file;
			if (outputFile != null)
				file = new File(outputFile.replace("{n}", "" + n));
			else
				file = File.createTempFile("dfa", ".dot");
			File filePDF = new File(file.getAbsolutePath() + ".pdf");
			PrintWriter writer = new PrintWriter(file);

			writer.write(automataToGraphViz(aut1, "Graph zu " + in, charRepaceIsEnabled));
			writer.flush();
			writer.close();

			if (showAutomata && (notover == -1 || notover >= aut1.getNumberOfStates())) {
				ProcessBuilder dot = new ProcessBuilder("/usr/bin/dot", "-Tpdf");
				dot.redirectOutput(filePDF);
				dot.redirectInput(Redirect.from(file));
				Process pdot = dot.start();
				pdot.waitFor();

				ProcessBuilder evince = new ProcessBuilder("/usr/bin/evince", filePDF.getAbsolutePath());
				evince.start();
			}
		}

		return aut1;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		int getStatesOfDeepN = -1;
		boolean showStats = false;
		boolean statsSort = false;
		boolean showAutomata = false;
		boolean computeDRE = false;
		boolean showHelp = false;
		boolean charRepaceIsEnabled = false;
		int nmin = -1;
		int nmax = -1;
		int notover = -1;
		String outputFile = null;
		String input = null;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			System.out.print(arg);
			if (arg.startsWith("--")) {
				switch (arg) {
				case "--help":
					showHelp = true;
					break;
				case "--stats":
					showStats = true;
					break;
				case "--sorted":
					statsSort = true;
					break;
				case "--show":
					showAutomata = true;
					break;
				case "--dre":
					computeDRE = true;
					break;
				case "--notover":
					if (i == args.length - 1)
						throw new InvalidParameterException("Fehlendes Argument für Parameter: " + arg);
					notover = Integer.parseInt(args[i + 1]);
					System.out.print(" " + args[i + 1]);
					++i;
					break;
				case "--min":
					if (i == args.length - 1)
						throw new InvalidParameterException("Fehlendes Argument für Parameter: " + arg);
					nmin = Integer.parseInt(args[i + 1]);
					System.out.print(" " + args[i + 1]);
					++i;
					break;
				case "--max":
					if (i == args.length - 1)
						throw new InvalidParameterException("Fehlendes Argument für Parameter: " + arg);
					nmax = Integer.parseInt(args[i + 1]);
					System.out.print(" " + args[i + 1]);
					++i;
					break;
				case "--states-of-deep":
					if (i == args.length - 1)
						throw new InvalidParameterException("Fehlendes Argument für Parameter: " + arg);
					if (args[i + 1].equals("n"))
						getStatesOfDeepN = -2;
					else if (args[i + 1].equals("all"))
						getStatesOfDeepN = -3;
					else
						getStatesOfDeepN = Integer.parseInt(args[i + 1]);
					System.out.print(" " + args[i + 1]);
					++i;
					break;
				case "--output":
					if (i == args.length - 1)
						throw new InvalidParameterException("Fehlendes Argument für Parameter: " + arg);
					outputFile = args[i + 1];
					System.out.print(" " + args[i + 1]);
					++i;
					break;

				default:
					throw new InvalidParameterException("Unbekannter Parameter: " + arg);
				}
			} else if (i == args.length - 1) {
				input = arg;
			} else
				throw new InvalidParameterException("Kann Paramter " + arg + " nicht interpretieren");
			System.out.println();
		}

		if (showHelp) {
			System.out.println("java -jar dfaTool.jar <parameter> Input");

			System.out
					.println("Input ist ein Regulärer Ausdruck, eventuelle \"n\" werden durch die Laufnummer ersetzt.");
			System.out.println("---------------------------------------");
			System.out.println("Liste der Parameter:");
			System.out.println("--help\t\tZeit die Hilfe");
			System.out.println("--stats\t\tGibt die Größe der Automaten aus.");
			System.out.println("--sorted\t\tGibt die Größe der Automaten sortiert nach Größe aus.");
			System.out.println("--show\t\tZeit den Automaten mit evince an.");
			System.out.println("--notover o\tZeit Automaten mit mehr als o Zuständen nicht an");
			System.out.println(
					"--dre\t\tWenn gesetzt wird auch ein deterministischer Regulärer Ausdruck für den Automaten berechnet (sofern einer existiert)");
			System.out.println("--min nmin\tMinimalzahl für n");
			System.out.println("--max nmax\tMaximalzahl für n");
			System.out.println("--states-of-deep n \tGibt die Anzahl an Zuständen der Tiefe n an");
			System.out.println(
					"--output\tDatei in die das Aussgabe geschrieben werden soll. Der Teilstring {n}  wird im Zweifel durch die durchlaufnummer ersetzt.");
			return;
		}

		if (input == null)
			throw new InvalidParameterException("Eingabe Fehlt");
		if (nmin > nmax)
			throw new InvalidParameterException("Min muss kleiner gleich Max sein");

		if (nmin < 0)
			nmin = 1;
		if (nmax < 0)
			nmax = nmin;

		List<Stats> stats = new LinkedList<>();
		for (int n = nmin; n < nmax + 1; n++) {
			Automaton aut = doTheStuff(showStats, statsSort, showAutomata, notover, outputFile, input, stats, n,
					charRepaceIsEnabled);

			if (getStatesOfDeepN > -1) {
				HashSet<State> hs = new HashSet<>();
				hs.add(aut.getInitialState());
				hs = computeDeepN(aut, hs, 0, getStatesOfDeepN);
				System.out.println("Zustände der Tiefe " + getStatesOfDeepN + ": " + hs.size());
			}
			if (getStatesOfDeepN == -2) {
				HashSet<State> hs = new HashSet<>();
				hs.add(aut.getInitialState());
				hs = computeDeepN(aut, hs, 0, 2 * n);
				System.out.println("Zustände der Tiefe " + 2 * n + ": " + hs.size());
			}
			if (getStatesOfDeepN == -3) {
				HashSet<State> hs = new HashSet<>();
				hs.add(aut.getInitialState());
				if (input.indexOf("^{n^2}") != -1)
					hs = computeDeepN(aut, hs, 0, n * n, true);
				else
					hs = computeDeepN(aut, hs, 0, 2 * n, true);
			}

			if (computeDRE) {
				System.out.println("DRE: " + BKW.computeBKW(aut));
			}
			
			if(!input.contains("n"))
				break;
		}

		if (showStats && statsSort)
			showStats(stats);

	}

	private static HashSet<State> computeDeepN(Automaton aut, HashSet<State> hs, int deep, int n) {

		return computeDeepN(aut, hs, deep, n, false);
	}

	private static HashSet<State> computeDeepN(Automaton aut, HashSet<State> hs, int deep, int n, boolean show) {
		if (show) {
			System.out.println("Zustände der Tiefe " + deep + ": " + hs.size());
		}
		if (deep == n)
			return hs;
		HashSet<State> nextlevel = new HashSet<>();
		for (State state : hs) {
			for (Transition t : state.getTransitions()) {
				nextlevel.add(t.getDest());
			}
		}

		return computeDeepN(aut, nextlevel, deep + 1, n, show);
	}

}
