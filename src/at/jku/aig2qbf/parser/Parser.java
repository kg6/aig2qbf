package at.jku.aig2qbf.parser;

import java.io.File;
import java.util.HashMap;
import java.util.Stack;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Latch;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;

public abstract class Parser {
	protected void checkInputFile(String inputFilePath, String expectedExtension) {
		if (inputFilePath == null) {
			throw new IllegalArgumentException("Input file path must not be null.");
		}

		File inputFile = new File(inputFilePath);

		if (!inputFile.exists()) {
			throw new IllegalArgumentException("Input file does not exist.");
		}
	}

	protected Tree createTree(final int numberOfInputs, final int maximumVariableIndex, final int multipliedMaximumVariableIndex, final int[][] fileLatches, final int[] fileOutputs, final int[][] fileAnds) {
		Tree tree = new Tree();

		Component[] components = new Component[maximumVariableIndex];
		Component[] notComponents = new Component[maximumVariableIndex];

		// insert latches
		for (int i = 0; i < fileLatches.length; i++) {
			components[fileLatches[i][0] / 2 - 1] = new Latch();
		}

		// insert ands
		for (int[] a : fileAnds) {
			components[a[0] / 2 - 1] = new And();
		}

		// connect latches
		for (int i = 0; i < fileLatches.length; i++) {
			this.prepareOutputComponent(tree, components, notComponents, components[fileLatches[i][0] / 2 - 1], fileLatches[i][1]);
		}

		// connect ands
		for (int[] a : fileAnds) {
			Component and = components[a[0] / 2 - 1];

			Component t1 = this.prepareInputComponent(tree, components, notComponents, a[1]);
			Component t2 = this.prepareInputComponent(tree, components, notComponents, a[2]);

			and.addInput(t1);
			and.addInput(t2);
		}

		// insert and connect outputs
		for (int i : fileOutputs) {
			String name = Integer.toString(i / 2);

			if (i == 0 || (i % 2 != 0 && i != 1)) {
				name = "NOT(" + name + ")";
			}

			Component o = new Output(name);
			tree.outputs.add((Output) o);

			this.prepareOutputComponent(tree, components, notComponents, o, i);
		}

		// remove unneeded components (slicing)
		HashMap<Component, Component> seen = new HashMap<>();
		Stack<Component> stack = new Stack<>();

		for (Component i : tree.outputs) {
			stack.add(i);
			seen.put(i, null);
		}

		while (!stack.isEmpty()) {
			Component n = stack.pop();

			for (Component i : n.inputs) {
				if (!seen.containsKey(i)) {
					stack.add(i);
					seen.put(i, null);
				}
			}
		}

		for (Component i : components) {
			if (i != null && !seen.containsKey(i)) {
				i.remove();
			}
		}
		for (Component i : notComponents) {
			if (i != null && !seen.containsKey(i)) {
				i.remove();
			}
		}

		return tree;
	}

	private Component prepareInputComponent(Tree tree, Component[] components, Component[] notComponents, int i) {
		if (i == 0) {
			return tree.cFalse;
		}
		else if (i == 1) {
			return tree.cTrue;
		}

		int index = i / 2 - 1;

		if (components[index] == null) {
			components[index] = new Input(Integer.toString(i / 2));
		}

		if (i % 2 == 0) {
			return components[index];
		}

		// lets reuse NOT components
		if (notComponents[index] == null) {
			Component n = notComponents[index] = new Not();

			n.addInput(components[index]);

			return n;
		}
		else {
			return notComponents[index];
		}
	}

	private Component prepareOutputComponent(Tree tree, Component[] components, Component[] notComponents, Component o, int i) {
		Component c = this.prepareInputComponent(tree, components, notComponents, i);

		o.addInput(c);

		return o;
	}

	abstract public Tree parse(String inputFilePath);

	abstract public Tree parse(byte[] input);
}
