package at.jku.aig2qbf.reduction;

import at.jku.aig2qbf.component.Tree;

public interface TreeReduction {
	public Tree reduceTree(Tree tree, int k);
}
