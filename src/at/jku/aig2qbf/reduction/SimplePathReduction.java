package at.jku.aig2qbf.reduction;

import java.util.ArrayList;
import java.util.List;

import at.jku.aig2qbf.Configuration;
import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.LatchOutput;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;

public class SimplePathReduction implements TreeReduction {

	@Override
	public Tree reduceTree(Tree tree, int k) {
		Tree localTree = (Tree) tree.clone();
		
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
			
			localTree.replaceTrueAndFalseAig();
		}
		
		if (Configuration.SANTIY) {
			localTree.verifyTreeStructure();
		}

		return localTree;
	}

	private Component getSimpleStateConstraints(Tree tree, int max_k) {
		And globalAnd = new And();
		
		// iterate over all branches of the tree and add the transition relation as well as the simple path constraint
		
		List<Relation> transitionRelations = new ArrayList<Relation>();
		List<Relation> simplepathRelations = new ArrayList<Relation>();
		
		for(int k = 1; k < max_k - 1; k++) {
			List<LatchOutput> K = tree.getLatchOutputsOfBranch(k);
			
			for(int l = 0; l < k; l++) {
				List<LatchOutput> L = tree.getLatchOutputsOfBranch(l);
				
				if(K.size() != L.size()) {
					throw new RuntimeException("Unable to apply simple state constraints: Unexpected latch output size");
				}
				
				for(int i = 0; i < K.size(); i++) {
					Component cK = K.get(i).component;
					Component cL = L.get(i).component;
					
					//Transition relation: L <-> K
					transitionRelations.add(new Relation(cK, cL));
					
					//Add simple path constraint: L.outputs != K
					for(Component cLOutput : cL.outputs) {
						simplepathRelations.add(new Relation(cLOutput, cK));
					}
				}
			}
		}
		
		// add all transition relations
		
		for(Relation relation : transitionRelations) {
			Component notA = new Not();
			notA.addInput(relation.a);
			
			Component notB = new Not();
			notB.addInput(relation.b);
			
			Component or0 = new Or();
			or0.addInput(notA);
			or0.addInput(relation.b);
			
			Component or1 = new Or();
			or1.addInput(relation.a);
			or1.addInput(notB);
			
			globalAnd.addInput(or0);
			globalAnd.addInput(or1);
		}
		
		// add all simple path relations
		
		for(Relation relation : simplepathRelations) {
			Component notA = new Not();
			notA.addInput(relation.a);
			
			Component notB = new Not();
			notA.addInput(relation.b);
			
			Component or0 = new Or();
			or0.addInput(relation.a);
			or0.addInput(relation.b);
			
			Component or1 = new Or();
			or1.addInput(notA);
			or1.addInput(notB);
			
			globalAnd.addInput(or0);
			globalAnd.addInput(or1);
		}

		// make sure that the component has at least 2 inputs
		
		while (globalAnd.inputs.size() < 2) {
			globalAnd.inputs.add(tree.cTrue);
			tree.cTrue.outputs.add(globalAnd);
		}

		return globalAnd;
	}

	@Override
	public String toString() {
		return "Simplepath reduction";
	}

	class Relation {
		private Component a;
		private Component b;
		
		public Relation(Component a, Component b) {
			this.a = a;
			this.b = b;
		}
	}
}
