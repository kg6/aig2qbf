package at.jku.aig2qbf.reduction;

import at.jku.aig2qbf.Configuration;
import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.component.quantifier.Quantifier;

public class SimplePathReduction implements TreeReduction {

	@Override
	public Tree reduceTree(Tree tree, int k) {
		Tree localTree = tree;

		if (localTree.outputs.size() == 0 || localTree.outputs.get(0).inputs.size() == 0) {
			return localTree;
		}

		// because of k=0
		k++;

		if (k > 1) {
			Component simplePathAnd = null;

			if (localTree.latchOutputs.length > 0) {
				simplePathAnd = getSimpleStateConstraints(localTree, k);
			}

			Output o = localTree.outputs.get(0);

			And a = new And();

			while (!o.inputs.isEmpty()) {
				Component c = o.inputs.remove(0);
				while (c.outputs.remove(o))
					;

				a.addInput(c);
			}

			if (simplePathAnd != null && simplePathAnd.inputs.size() != 0) {
				a.addInput(simplePathAnd);
			}

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

	private Component getSimpleStateConstraints(Tree tree, int max_k) {
		And globalAnd = new And();

		// iterate over all branches of the tree and add the simple path constraint
		if (tree.latchOutputs.length > 0) {
			final int latchOutputCount = tree.latchOutputs[0].length;

			int tseitinInputCounter = 0;

			for (int i = 0; i < latchOutputCount; i++) {
				Input li = new Input("l" + i);
				tree.addQuantifier(li, Quantifier.UNIVERSAL);

				Component notLi = new Not();
				notLi.addInput(li);

				Input notLiTseitin = new Input("t" + tseitinInputCounter++);
				tree.addQuantifier(notLiTseitin, Quantifier.EXISTENTIAL);

				tree.addNodeInTseitinForm(notLi, notLiTseitin, globalAnd);

				for (int k = 0; k < max_k - 1; k++) {
					Component si = tree.latchOutputs[k][i];

					Component notSi = new Not();
					notSi.addInput(si);

					Input notSiTseitin = new Input("t" + tseitinInputCounter++);
					tree.addQuantifier(notSiTseitin, Quantifier.EXISTENTIAL);

					tree.addNodeInTseitinForm(notSi, notSiTseitin, globalAnd);

					Component or0 = new Or();
					or0.addInput(notLiTseitin);
					or0.addInput(si);

					Input or0Tseitin = new Input("t" + tseitinInputCounter++);
					tree.addQuantifier(or0Tseitin, Quantifier.EXISTENTIAL);

					Component or1 = new Or();
					or1.addInput(notSiTseitin);
					or1.addInput(li);

					Input or1Tseitin = new Input("t" + tseitinInputCounter++);
					tree.addQuantifier(or1Tseitin, Quantifier.EXISTENTIAL);

					tree.addNodeInTseitinForm(or0, or0Tseitin, globalAnd);
					tree.addNodeInTseitinForm(or1, or1Tseitin, globalAnd);
				}
			}
		}

		// make sure that the component has at least 2 inputs
		if (globalAnd.inputs.size() == 1) {
			globalAnd.addInput(tree.cTrue);
		}

		return globalAnd;
	}

	@Override
	public String toString() {
		return "Simplepath reduction";
	}
}
