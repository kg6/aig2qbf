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

public class QDIMACS extends Formatter {
	
	@Override
	public String format(Tree cnfTree) {
		cnfTree = Configuration.FAST ? cnfTree : (Tree) cnfTree.clone();

		Output rootNode = null;

		if (cnfTree.outputs.size() > 0 && cnfTree.outputs.get(0).inputs.size() > 0) {
			rootNode = cnfTree.outputs.get(0);
		}

		if (rootNode != null) {
			StringBuilder qdimacsBuilder = new StringBuilder();
			
			// determine the number of variables
			final int numberOfVariables = getNumberOfVariables(rootNode);

			// define problem line
			qdimacsBuilder.append("p cnf %s %s\n");

			// define all quantifiers
			for (QuantifierSet quantifier : cnfTree.quantifier) {
				qdimacsBuilder.append(quantifier.quantifier == Quantifier.EXISTENTIAL ? "e" : "a");
				qdimacsBuilder.append(" ");

				for (Input input : quantifier.literals) {
					qdimacsBuilder.append(input.getId());
					qdimacsBuilder.append(" ");
				}

				qdimacsBuilder.append("0\n");
			}

			// define CNF clauses
			final int numberOfClauses = defineCNFClauses(rootNode.inputs, qdimacsBuilder);
			
			return String.format(qdimacsBuilder.toString(), numberOfVariables, numberOfClauses);
		}

		return "p cnf 0 0\n";
	}

	private int defineCNFClauses(List<Component> componentList, StringBuilder builder) {
		Stack<Component> componentStack = new Stack<Component>();

		for (int i = componentList.size() - 1; i >= 0; i--) {
			componentStack.push(componentList.get(i));
		}

		int clauseCounter = 0;

		while (! componentStack.isEmpty()) {
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
					negated = ! negated;
				}

				appendClause(builder, child.getId(), negated);
			}
			else if (c instanceof Input) {
				appendClause(builder, c.getId(), false);
			}
			else if (c instanceof NL) {
				appendNL(builder);
				clauseCounter++;
			}
			else {
				throw new RuntimeException("Unable to convert tree to QDIMACS format: The  node type '" + c.getClass().getName() + "' is unknown!");
			}
			
			if(componentStack.isEmpty() && !(c instanceof NL)) {
				clauseCounter++;
			}
		}

		if (builder.charAt(builder.length() - 1) == ' ') {
			appendNL(builder);
		}
		
		return clauseCounter;
	}

	private void appendClause(StringBuilder builder, int id, boolean negated) {
		if (negated) {
			id = -id;
		}

		builder.append(Integer.toString(id));
		builder.append(" ");
	}

	private void appendNL(StringBuilder builder) {
		builder.append("0");
		builder.append("\n");
	}

	private int getNumberOfVariables(Component root) {
		HashMap<Component, Component> inputs = new HashMap<>();
		HashMap<Component, Component> seen = new HashMap<>();
		Stack<Component> stack = new Stack<>();
		
		stack.add(root);
		seen.put(root, null);

		while (!stack.isEmpty()) {
			Component n = stack.pop();

			if (n instanceof Input && ! inputs.containsKey(n)) {
				inputs.put(n, null);
			}

			for (Component i : n.inputs) {
				if (! seen.containsKey(i)) {
					stack.add(i);
					seen.put(i, null);
				}
			}
		}

		return inputs.size();
	}

	public class NL extends Component {

		@Override
		protected Object clone() {
			return new NL();
		}
	}
}
