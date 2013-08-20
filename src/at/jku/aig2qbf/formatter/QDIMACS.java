package at.jku.aig2qbf.formatter;

import java.util.HashMap;
import java.util.List;
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
		Output rootNode = null;

		if (cnfTree.outputs.size() > 0 && cnfTree.outputs.get(0).inputs.size() > 0) {
			rootNode = cnfTree.outputs.get(0);
		}

		if (rootNode != null) {
			StringBuilder qdimacsBuilder = new StringBuilder();

			// determine the number of variables
			final int[] minMaxVariableIndex = getMinMaxVariableIndex(rootNode);
			final int variableOffset = minMaxVariableIndex[0] - 1;

			// define all quantifiers
			for (QuantifierSet quantifier : cnfTree.quantifier) {
				qdimacsBuilder.append(quantifier.quantifier == Quantifier.EXISTENTIAL ? "e" : "a");
				qdimacsBuilder.append(" ");

				for (Input input : quantifier.literals) {
					qdimacsBuilder.append(input.getId() - variableOffset);
					qdimacsBuilder.append(" ");
				}

				qdimacsBuilder.append("0\n");
			}

			// define CNF clauses
			final int numberOfClauses = defineCNFClauses(rootNode.inputs, qdimacsBuilder, variableOffset);

			// define problem line
			qdimacsBuilder.insert(0, String.format("p cnf %s %s\n", minMaxVariableIndex[1] - (minMaxVariableIndex[0] - 1), numberOfClauses));

			return qdimacsBuilder.toString();
		}

		return "p cnf 0 0\n";
	}

	private int defineCNFClauses(List<Component> componentList, StringBuilder builder, int variableOffset) {
		Stack<Component> componentStack = new Stack<Component>();

		for (int i = componentList.size() - 1; i >= 0; i--) {
			componentStack.push(componentList.get(i));
		}

		int clauseCounter = 0;
		boolean lineHasLiterals = false;

		while (!componentStack.isEmpty()) {
			Component c = componentStack.pop();

			if (c instanceof And || c instanceof Or) {
				for (int i = c.inputs.size() - 1; i >= 0; i--) {
					if (c instanceof And) {
						componentStack.push(new NL());
					}

					componentStack.push(c.inputs.get(i));
				}
			}
			else if (c instanceof Not) {
				boolean negated = true;
				Component child = c.inputs.get(0);

				while (child instanceof Not) {
					child = child.inputs.get(0);
					negated = !negated;
				}

				appendClause(builder, child.getId() - variableOffset, negated);

				lineHasLiterals = true;
			}
			else if (c instanceof Input) {
				appendClause(builder, c.getId() - variableOffset, false);

				lineHasLiterals = true;
			}
			else if (c instanceof NL) {
				if (lineHasLiterals) {
					appendNL(builder);
					clauseCounter++;

					lineHasLiterals = false;
				}
			}
			else {
				throw new RuntimeException("Unable to convert tree to QDIMACS format: The  node type '" + c.getClass().getName() + "' is unknown!");
			}

			if (componentStack.isEmpty() && !(c instanceof NL)) {
				clauseCounter++;
			}
		}

		if (builder.charAt(builder.length() - 1) == ' ') {
			appendNL(builder);
		}

		return clauseCounter;
	}

	private void appendClause(StringBuilder builder, int id, boolean negated) {
		if (id == 0) {
			throw new RuntimeException("Unable to convert tree to QDIMACS format: The not id 0 is invalid!");
		}

		if (negated) {
			id = -id;
		}

		builder.append(id);
		builder.append(" ");
	}

	private void appendNL(StringBuilder builder) {
		builder.append("0");
		builder.append("\n");
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

			if(n instanceof Input) {
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

	public class NL extends Component {

		@Override
		protected Object clone() {
			return new NL();
		}
	}
}
