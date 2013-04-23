package at.jku.aig2qbf.reduction;

import java.util.List;

import at.jku.aig2qbf.Configuration;
import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;

public class SimplePathReduction implements TreeReduction {

	@Override
	public Tree reduceTree(Tree tree, int k) {
		Tree localTree = Configuration.FAST ? tree : (Tree) tree.clone();
		
		if(localTree.outputs.size() == 0 || localTree.outputs.get(0).inputs.size() == 0) {
			return localTree;
		}

		if (k > 1) {
			Component simplePathAnd = getSimpleStateConstraints(localTree, k);

			Output o = localTree.outputs.get(0);

			And a = new And();

			while (! o.inputs.isEmpty()) {
				Component c = o.inputs.remove(0);
				while (c.outputs.remove(o))
					;

				a.addInput(c);
			}

			a.addInput(simplePathAnd);

			o.addInput(a);

			if (o.inputs.size() > 1) {
				throw new RuntimeException("Unable to reduce the tree: Inputs of the output component must be merged into one component!");
			}
		}

		if (Configuration.SANTIY) {
			localTree.verifyTreeStructure();
		}

		return localTree;
	}

	private Component getSimpleStateConstraints(Tree tree, int k) {
		And globalAnd = new And();

		int latchCount = tree.latchOutputs.size() / k;

		for (int i = 0; i <= k - 2; i++) {
			List<Component> latchOutputList1 = tree.latchOutputs.subList((i * latchCount), ((i + 1) * latchCount));

			for (int j = i + 1; j <= k - 1; j++) {
				List<Component> latchOutputList2 = tree.latchOutputs.subList((j * latchCount), ((j + 1) * latchCount));

				globalAnd.addInput(getNotEqStates(tree, latchOutputList1, latchOutputList2));
			}
		}

		// Make sure that the component has at least 2 inputs
		while (globalAnd.inputs.size() < 2) {
			globalAnd.inputs.add(tree.cTrue);
			tree.cTrue.outputs.add(globalAnd);
		}

		return globalAnd;
	}

	private Component getNotEqStates(Tree tree, List<Component> latchOutputList1, List<Component> latchOutputList2) {
		if (latchOutputList1.size() != latchOutputList2.size()) {
			throw new RuntimeException("Unable to generate simple path constraint: A different number of output latches was found.");
		}

		Or or = new Or();

		for (int i = 0; i < latchOutputList1.size(); i++) {
			Component a = latchOutputList1.get(i);
			Component b = latchOutputList2.get(i);

			Not notA = new Not();
			notA.addInput(a);

			Not notB = new Not();
			notB.addInput(b);

			And andL = new And();
			andL.addInput(notA);
			andL.addInput(b);

			And andR = new And();
			andR.addInput(a);
			andR.addInput(notB);

			or.addInput(andL);
			or.addInput(andR);
		}

		// Make sure that the component has at least 2 inputs
		while (or.inputs.size() < 2) {
			or.inputs.add(tree.cTrue);
			tree.cTrue.outputs.add(or);
		}

		return or;
	}

	@Override
	public String toString() {
		return "Simplepath reduction";
	}

}
