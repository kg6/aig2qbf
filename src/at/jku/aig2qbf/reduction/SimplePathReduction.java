package at.jku.aig2qbf.reduction;

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
		Tree localTree = tree;
		
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

			if (simplePathAnd.inputs.size() != 0) {
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

	    for(int l = 0; l < max_k - 1; l++) {
	    	for(int k = l + 1; k < max_k; k++) {
	    		Component or = new Or();

				for(int i = 0; i < tree.latchOutputs[0].length; i++) {
					Component cK = tree.latchOutputs[k][i];
					Component cL = tree.latchOutputs[l][i];
					
					or.addInput(getNotEqState(cK, cL));
				}

				if (or.inputs.size() != 0) {
					if (or.inputs.size() == 1) {
						or.addInput(tree.cFalse);
					}

					globalAnd.addInput(or);
				}
	    	}
	    }
		
		// make sure that the component has at least 2 inputs
		if (globalAnd.inputs.size() == 1) {
			globalAnd.addInput(tree.cTrue);
		}

		return globalAnd;
	}
	
	private Component getNotEqState(Component c1, Component c2) {
		Component notC1 = new Not();
		notC1.addInput(c1);
		
		Component notC2 = new Not();
		notC2.addInput(c2);
		
		Component and0 = new And();
		and0.addInput(notC1);
		and0.addInput(c2);
		
		Component and1 = new And();
		and1.addInput(c1);
		and1.addInput(notC2);
		
		Component or = new Or();
		or.addInput(and0);
		or.addInput(and1);
		
		return or;
	}

	@Override
	public String toString() {
		return "Simplepath reduction";
	}
}
