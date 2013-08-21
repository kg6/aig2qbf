package at.jku.aig2qbf.formatter;

import java.util.HashMap;
import java.util.Stack;

import at.jku.aig2qbf.Configuration;
import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.component.quantifier.Quantifier;
import at.jku.aig2qbf.component.quantifier.QuantifierSet;

public class QDIMACS implements Formatter {

	@Override
	public String format(Tree cnfTree) {
		if (Configuration.SANTIY && !cnfTree.isCNF()) {
			throw new RuntimeException("Tree not in CNF!");
		}

		Output rootNode = null;

		if (cnfTree.outputs.size() > 0 && cnfTree.outputs.get(0).inputs.size() > 0) {
			rootNode = cnfTree.outputs.get(0);
		}

		if (rootNode == null) {
			return "p cnf 0 0\n";
		}

		Component globalAnd = rootNode.inputs.get(0);

		StringBuilder builder = new StringBuilder();

		// determine the number of variables
		final int[] minMaxVariableIndex = (cnfTree.minMaxTseitinVariables != null) ? cnfTree.minMaxTseitinVariables : getMinMaxVariableIndex(globalAnd);
		final int variableOffset = minMaxVariableIndex[0] - 1;
		final int numberOfClauses = (globalAnd instanceof And) ? globalAnd.inputs.size() : 1;

		// define problem line
		builder.append(String.format("p cnf %s %s\n", minMaxVariableIndex[1] - (minMaxVariableIndex[0] - 1), numberOfClauses));

		// define all quantifiers
		for (QuantifierSet quantifier : cnfTree.quantifier) {
			builder.append(quantifier.quantifier == Quantifier.EXISTENTIAL ? "e " : "a ");

			for (Input input : quantifier.literals) {
				builder.append(input.getId() - variableOffset);
				builder.append(" ");
			}

			builder.append("0\n");
		}

		// define CNF clauses
		for (Component c : globalAnd.inputs) {
			if (c instanceof Or) {
				if (Configuration.SANTIY && c.inputs.size() < 2) {
					throw new RuntimeException("OR should have more than two Inputs");
				}

				for (Component i : c.inputs) {
					if (i instanceof Not) {
						builder.append(-(i.inputs.get(0).getId() - variableOffset));
						builder.append(" ");
					}
					else { // Input, True, False
						if (Configuration.SANTIY && !i.isLiteral()) {
							throw new RuntimeException(i + " not allowed here");
						}

						builder.append(i.getId() - variableOffset);
						builder.append(" ");
					}
				}
			}
			else if (c instanceof Not) {
				builder.append(-(c.inputs.get(0).getId() - variableOffset));
				builder.append(" ");
			}
			else { // Input, True, False
				if (Configuration.SANTIY && !c.isLiteral()) {
					throw new RuntimeException(c + " not allowed here");
				}

				builder.append(c.getId() - variableOffset);
				builder.append(" ");
			}

			builder.append("0\n");
		}

		return builder.toString();
	}

	private int[] getMinMaxVariableIndex(Component root) {
		int minVariableIndex = Integer.MAX_VALUE;
		int maxVariableIndex = 0;

		HashMap<Component, Component> seen = new HashMap<>();
		Stack<Component> stack = new Stack<>();

		stack.add(root);
		seen.put(root, null);

		while (!stack.isEmpty()) {
			Component n = stack.pop();

			if (n instanceof Input) {
				if (n.getId() < minVariableIndex) {
					minVariableIndex = n.getId();
				}

				if (n.getId() > maxVariableIndex) {
					maxVariableIndex = n.getId();
				}
			}

			for (Component i : n.inputs) {
				if (!seen.containsKey(i)) {
					stack.add(i);
					seen.put(i, null);
				}
			}
		}

		return new int[] {
			minVariableIndex,
			maxVariableIndex
		};
	}
}
