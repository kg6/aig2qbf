package at.jku.aig2qbf.formatter;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.False;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Latch;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.component.True;

public class AAG implements Formatter {
	@Override
	public String format(Tree tree) {
		StringBuilder s = new StringBuilder();

		HashMap<Component, Component> seen = new HashMap<>();
		Stack<Component> stack = new Stack<>();

		seen.put(tree.cFalse, null);
		seen.put(tree.cTrue, null);

		SortedSet<Integer> inputs = new TreeSet<>();
		SortedSet<Integer> latches = new TreeSet<>();
		SortedSet<Integer> outputs = new TreeSet<>();
		SortedSet<Integer> ands = new TreeSet<>();

		// go through the whole tree
		for (Component i : tree.outputs) {
			stack.add(i);
			seen.put(i, null);

			outputs.add(i.getId());

			if (i.inputs.size() != 1) {
				throw new RuntimeException("Output has " + i.inputs.size() + " inputs instead of exactly 1");
			}
		}

		while (!stack.isEmpty()) {
			Component n = stack.pop();

			for (Component i : n.inputs) {
				if (!seen.containsKey(i)) {
					if (i instanceof Not) {
						// ignore it

						if (i.inputs.size() != 1) {
							throw new RuntimeException("Not has " + i.inputs.size() + " inputs instead of exactly 1");
						}
					}
					else if (i instanceof Input) {
						inputs.add(i.getId());

						if (i.inputs.size() != 0) {
							throw new RuntimeException("Input has " + i.inputs.size() + " inputs instead of exactly 0");
						}
					}
					else if (i instanceof Latch) {
						latches.add(i.getId());

						if (i.inputs.size() != 1) {
							throw new RuntimeException("Latch has " + i.inputs.size() + " inputs instead of exactly 1");
						}
					}
					else if (i instanceof And) {
						ands.add(i.getId());

						if (i.inputs.size() != 2) {
							throw new RuntimeException("And has " + i.inputs.size() + " inputs instead of exactly 2");
						}
					}
					else {
						throw new RuntimeException("Cannot handle component " + i.getClass() + " here");
					}

					stack.add(i);
					seen.put(i, null);
				}
			}
		}

		// reassign indices
		int maxIndex = 2;
		HashMap<Integer, Integer> indizes = new HashMap<>();

		for (Integer i : inputs) {
			indizes.put(i, maxIndex);

			maxIndex += 2;
		}
		for (Integer i : latches) {
			indizes.put(i, maxIndex);

			maxIndex += 2;
		}
		for (Integer i : ands) {
			indizes.put(i, maxIndex);

			maxIndex += 2;
		}

		// Write all to the file
		int HEADER_M = inputs.size() + latches.size() + ands.size();
		int HEADER_I = inputs.size();
		int HEADER_L = latches.size();
		int HEADER_O = outputs.size();
		int HEADER_A = ands.size();

		s.append(String.format("aag %d %d %d %d %d\n", HEADER_M, HEADER_I, HEADER_L, HEADER_O, HEADER_A));

		for (Integer i : inputs) {
			Component n = Component.ComponentArray[i];

			s.append(String.format("%d\n", indizes.get(n.getId())));
		}
		for (Integer i : latches) {
			Component n = Component.ComponentArray[i];

			s.append(String.format("%d %d\n", indizes.get(n.getId()), this.aigerInput(indizes, n.inputs.get(0))));
		}
		for (Integer i : outputs) {
			Component n = Component.ComponentArray[i];

			s.append(String.format("%d\n", this.aigerInput(indizes, n.inputs.get(0))));
		}
		for (Integer i : ands) {
			Component n = Component.ComponentArray[i];

			s.append(String.format("%d %d %d\n", indizes.get(n.getId()), this.aigerInput(indizes, n.inputs.get(0)), this.aigerInput(indizes, n.inputs.get(1))));
		}

		return s.toString();
	}

	private int aigerInput(HashMap<Integer, Integer> indizes, Component n) {
		if (n instanceof False) {
			return 0;
		}
		else if (n instanceof True) {
			return 1;
		}
		else if (n instanceof Not) {
			int r = this.aigerInput(indizes, n.inputs.get(0));

			if (r < 2) {
				return (r + 1) % 2;
			}
			else if (r % 2 == 0) {
				return r + 1;
			}
			else {
				return r - 1;
			}
		}
		else {
			return indizes.get(n.getId());
		}
	}
}
