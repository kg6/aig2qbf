package at.jku.aig2qbf.reduction;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import at.jku.aig2qbf.Configuration;
import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
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
		}
		
		if (Configuration.SANTIY) {
			localTree.verifyTreeStructure();
		}

		return localTree;
	}

	private Component getSimpleStateConstraints(Tree tree, int max_k) {
		And globalAnd = new And();
		
		// iterate over all branches of the tree and add the transition relation as well as the simple path constraint
		Hashtable<Relation, Boolean> transitionRelationHash = new Hashtable<Relation, Boolean>();
		Hashtable<Relation, Boolean> simplepathRelationHash = new Hashtable<Relation, Boolean>();
		
		for(int k = 1; k < max_k - 1; k++) {
			for(int l = 0; l < k; l++) {
				for(int i = 0; i < tree.latchOutputs[0].length; i++) {
					Component cK = tree.latchOutputs[k][i];
					Component cL = tree.latchOutputs[l][i];
					
					// transition relation: L <-> K
					transitionRelationHash.put(new Relation(cK, cL), true);
					
					// add simple path constraint: L.outputs != K
					for(Component cLOutput : cL.outputs) {
						simplepathRelationHash.put(new Relation(cLOutput, cK), true);
					}
				}
			}
		}
		
		// add all transition relations
		Set<Relation> transitionRelations = transitionRelationHash.keySet();
		
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
		Set<Relation> simplepathRelationSet = simplepathRelationHash.keySet();
		
		for(Relation relation : simplepathRelationSet) {
			Component notA = new Not();
			notA.addInput(relation.a);
			
			Component notB = new Not();			
			notB.addInput(relation.b);
			
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
			Input input = new Input("true");

			Component not = new Not();
			not.addInput(input);

			Component or = new Or();
			or.addInput(input);
			or.addInput(not);

			globalAnd.inputs.add(or);
			or.outputs.add(globalAnd);
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

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Relation)) {
				return false;
			}
			
			Relation relation = (Relation)obj;
			
			return a.equals(relation.a) && b.equals(relation.b);
		}

		@Override
		public int hashCode() {
			return (Integer.toString(a.getId()) + Integer.toString(b.getId())).hashCode();
		}
	}
}
