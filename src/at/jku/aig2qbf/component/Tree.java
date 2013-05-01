package at.jku.aig2qbf.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import at.jku.aig2qbf.Configuration;
import at.jku.aig2qbf.component.quantifier.Quantifier;
import at.jku.aig2qbf.component.quantifier.QuantifierSet;

public class Tree implements Cloneable {
	public List<Output> outputs;
	public List<QuantifierSet> quantifier;

	public Component[][] latchOutputs;
	
	public False cFalse;
	public True cTrue;

	public Tree() {
		this.outputs = new ArrayList<Output>();
		this.latchOutputs = new Component[0][0];
		this.quantifier = new ArrayList<QuantifierSet>();
		
		this.cFalse = new False();
		this.cTrue = new True();
	}

	public boolean isCNF() {
		if (this.outputs.size() != 1) {
			return this.outputs.size() == 0;
		}

		Component c = this.outputs.get(0);

		if (c.inputs.size() != 1) {
			throw new RuntimeException("OUTPUT must have exactly one input");
		}

		c = c.inputs.get(0);

		if (c.isLiteral()) {
			return true;
		}
		else if (c instanceof Not) {
			if (c.inputs.size() != 1) {
				throw new RuntimeException("NOT must have exactly one input");
			}

			return c.inputs.get(0).isLiteral();
		}
		else if (! (c instanceof And)) {
			return false;
		}

		if (c.inputs.size() < 2) {
			throw new RuntimeException("AND must have at least two inputs");
		}

		for (Component cl : c.inputs) {
			if (cl.isLiteral()) {
				continue;
			}
			else if (cl instanceof Not) {
				if (cl.inputs.size() != 1) {
					throw new RuntimeException("NOT must have exactly one input");
				}

				if (cl.inputs.get(0).isLiteral()) {
					continue;
				}
				else {
					return false;
				}
			}
			else if (cl instanceof Or) {
				if (cl.inputs.size() < 2) {
					throw new RuntimeException("OR must have at least two inputs");
				}

				for (Component l : cl.inputs) {
					if (l.isLiteral()) {
						continue;
					}
					else if (l instanceof Not) {
						if (l.inputs.size() != 1) {
							throw new RuntimeException("NOT must have exactly one input");
						}

						if (l.inputs.get(0).isLiteral()) {
							continue;
						}
					}

					return false;
				}
			}
			else {
				return false;
			}
		}

		return true;
	}

	@Override
	public Object clone() {
		Tree nTree = new Tree();

		HashMap<Component, Component> components = new HashMap<>();

		components.put(this.cFalse, nTree.cFalse);
		components.put(this.cTrue, nTree.cTrue);

		// clone quantors
		for (QuantifierSet oq : this.quantifier) {
			List<Input> literalList = new ArrayList<Input>();

			for (int i = 0; i < oq.literals.size(); i++) {
				literalList.add((Input) this.cloneGetComponent(components, oq.literals.get(i)));
			}

			nTree.quantifier.add(new QuantifierSet(oq.quantifier, literalList));
		}

		// clone outputs
		LinkedList<Component> q = new LinkedList<>();

		for (Component o : this.outputs) {
			Component n = cloneGetComponent(components, o);

			nTree.outputs.add((Output) n);

			q.push(o);
		}

		while (! q.isEmpty()) {
			Component o = q.pop();

			for (Component i : o.inputs) {
				if (! components.containsKey(i)) {
					Component n = (Component) i.clone();
					
					components.put(i, n);

					q.push(i);
				}
			}
		}

		for (Component o : components.keySet()) {
			Component n = components.get(o);

			for (Component i : o.inputs) {
				n.inputs.add(components.get(i));
			}

			for (Component i : o.outputs) {
				if (components.get(i) == null) {
					// IGNORE this output as it is NOT connected to an component
					// that is the input directly or indirectly for an
					// Tree.output

					continue;
				}

				n.outputs.add(components.get(i));
			}
		}

		// clone latch outputs
		nTree.latchOutputs = new Component[this.latchOutputs.length][this.latchOutputs.length == 0 ? 0 : this.latchOutputs[0].length];
		
		for (int i = 0; i < this.latchOutputs.length; i++) {
			for (int j = 0; j < this.latchOutputs[0].length; j++) {
				Component c = this.latchOutputs[i][j];
				
				// the component can be empty because we are in the middle of unrolling
				if (c == null) {
					continue;
				}
				
				// we have to clone unreferenced components as some latch outputs are not in the tree anymore!
				if (! components.containsKey(c)) {
					components.put(c, (Component) c.clone());
				}
				
				nTree.latchOutputs[i][j] = components.get(c);
			}
		}

		if (Configuration.SANTIY) {
			verify(this.cFalse.inputs.size() == 0, this.cFalse);
			verify(nTree.cFalse.inputs.size() == 0, nTree.cFalse);
			verify(this.cFalse.outputs.size() == nTree.cFalse.outputs.size(), nTree.cFalse);

			verify(this.cTrue.inputs.size() == 0, this.cTrue);
			verify(nTree.cTrue.inputs.size() == 0, nTree.cTrue);
			verify(this.cTrue.outputs.size() == nTree.cTrue.outputs.size(), nTree.cTrue);

			nTree.verifyTreeStructure();
		}

		return nTree;
	}

	public Component cloneComponent(Component in) {
		if (in instanceof False) {
			return this.cFalse;
		}
		else if (in instanceof False) {
			return this.cTrue;
		}
		else {
			return (Component) in.clone();
		}
	}

	private Component cloneGetComponent(HashMap<Component, Component> components, Component o) {
		Component n = components.get(o);

		if (n == null) {
			n = (Component) o.clone();

			components.put(o, n);
		}

		return n;
	}

	public List<Latch> findLatches() {
		List<Latch> latches = new ArrayList<>();

		HashMap<Component, Component> components = new HashMap<>();
		LinkedList<Component> q = new LinkedList<>();

		for (Component o : this.outputs) {
			q.push(o);
		}

		while (! q.isEmpty()) {
			Component o = q.pop();

			for (Component i : o.inputs) {
				if (! i.isLiteral() && ! components.containsKey(i)) {
					components.put(i, null);

					if (i instanceof Latch) {
						if (i.inputs.size() != 1) {
							throw new RuntimeException("LATCH must have exactly one input");
						}

						latches.add((Latch) i);
					}

					q.push(i);
				}
			}
		}

		return latches;
	}

	public void mergeToOneOutput() {
		this.mergeToOneOutput(new Or());
	}

	public void mergeToOneOutput(Component merger) {
		if (this.outputs.size() < 2) {
			return;
		}

		for (Output o : this.outputs) {
			while (! o.inputs.isEmpty()) {
				Component i = o.inputs.remove(0);
				while (i.outputs.remove(o))
					;

				merger.addInput(i);
			}
		}

		this.outputs.clear();

		Output o = new Output("out");
		o.addInput(merger);
		this.outputs.add(o);
	}

	public Tree toEquivalentCNF() {
		Tree tree = (Tree) this.clone();

		if (tree.isCNF()) {
			return tree;
		}

		Output y = new Output("y");
		Or or = new Or();

		for (Output o : tree.outputs) {
			if (o.inputs.size() != 1) {
				throw new RuntimeException("OUTPUT must have exactly one input");
			}

			or.addInput(o.inputs.get(0));

			o.inputs.get(0).pushNots();
		}

		Tree nTree = new Tree();

		if (or.inputs.size() > 1) {
			y.addInput(or);
			nTree.outputs.add(y);
		}
		else if (or.inputs.size() == 1) {
			y.addInput(or.inputs.get(0));
			nTree.outputs.add(y);
		}

		Component c = y.inputs.get(0);

		if (c instanceof And) {
			for (Component i : c.inputs) {
				i.pushDistributive();
			}

			y.mergeChilds();
		}
		else {
			c.pushDistributive();
		}

		if (! nTree.isCNF()) {
			throw new RuntimeException("Tree still not in CNF!");
		}

		return nTree;
	}

	public Tree toTseitinCNF() {
		Tree tree = Configuration.FAST ? this : (Tree) this.clone();

		if (tree.outputs.size() == 0) {
			return tree;
		}
		else if (tree.outputs.size() > 1) {
			throw new RuntimeException("Unable to convert the tree to Tseitin form: Only one output node is expected!");
		}

		Output o = tree.outputs.get(0);

		if (o.inputs.size() != 1) {
			throw new RuntimeException("OUTPUT must have exactly one input");
		}
		
		// replace true and false components with logic that is equal to true and false
		// this is necessary because QDIMACS does not have a symbol for true/false
		tree.replaceTrueWithAig();
		tree.replaceFalseWithAig();
		
		// if input is directly mapped to output, just return the tree
		if (o.inputs.get(0) instanceof Input) {
			return tree;
		}

		// transform the tree from bottom to top using a stack and DFS
		HashMap<Component, Component> seen = new HashMap<>();
		Stack<Component> stack = new Stack<Component>();
		stack.push(o.inputs.get(0));

		// build a global and to connect the pieces of the tree
		Component globalAnd = new And();
		
		int tseitinInputCounter = 0;

		while (! stack.isEmpty()) {
			Component node = stack.pop();
			
			if (! seen.containsKey(node)) {
				// mark this node as seen for the backward path
				seen.put(node, null);

				// push the current node again onto the stack to traverse it
				// again in the backward path
				stack.push(node);

				// go deeper into the tree by pushing all children of the
				// current node onto the stack
				for (Component c : node.inputs) {
					if (c instanceof Latch) {
						throw new RuntimeException("Unable to convert the tree to Tseitin form: There must not be a latch in the tree. Please unroll the tree first!");
					}

					if (c instanceof True || c instanceof False) {
						throw new RuntimeException("Unable to convert the tree to Tseitin form: There must not be True/False in the tree. Please replace these components first!");
					}

					// add every component to the stack that should get a Tseitin node
					if(! (c instanceof Input) && ! stack.contains(c)){
						stack.push(c);
					}
				}
			}
			else {
				if(node.inputs.isEmpty()) {
					throw new RuntimeException("Unable to convert the tree to Tseitin form: The node of type " + node.getClass().getName() + " must have inputs!");
				}
				
				// transform to structure of Tseitin
				Input xi = new Input("x" + tseitinInputCounter);
				tseitinInputCounter++;

				Component notXi = new Not();
				notXi.addInput(xi);

				if (node instanceof And) {
					Component overallOr = new Or();
					overallOr.addInput(xi);

					while (! node.inputs.isEmpty()) {
						Component c = node.inputs.remove(0);
						while (c.outputs.remove(node))
							;

						Component or = new Or();
						or.addInput(notXi);
						or.addInput(c);

						globalAnd.addInput(or);

						Component notC = new Not();
						notC.addInput(c);

						overallOr.addInput(notC);
					}

					globalAnd.addInput(overallOr);
				}
				else if (node instanceof Not) {
					Component or0 = new Or();
					or0.addInput(notXi);

					Component or1 = new Or();
					or1.addInput(xi);

					while (! node.inputs.isEmpty()) {
						Component c = node.inputs.remove(0);
						while (c.outputs.remove(node))
							;

						Component notC = new Not();
						notC.addInput(c);

						or0.addInput(notC);
						or1.addInput(c);
					}

					globalAnd.addInput(or0);
					globalAnd.addInput(or1);
				}
				else if (node instanceof Or) {
					Component overallOr = new Or();
					overallOr.addInput(notXi);

					while (! node.inputs.isEmpty()) {
						Component c = node.inputs.remove(0);
						while (c.outputs.remove(node))
							;

						overallOr.addInput(c);

						Component notC = new Not();
						notC.addInput(c);

						Component or = new Or();
						or.addInput(xi);
						or.addInput(notC);

						globalAnd.addInput(or);
					}

					globalAnd.addInput(overallOr);
				}
				else {
					throw new RuntimeException("Unable to convert the tree to Tseitin form: Node type " + node + " is unknown!");
				}

				// replace node with xi in the tree
				tree.replaceComponent(node, xi);
			}
		}

		// manage the tree output
		if (o.inputs.size() != 1) {
			throw new RuntimeException("Unable to convert the tree to Tseitin form: There must exactly one output variable, which was introduced by the tseitin conversion!");
		}

		if (! o.inputs.isEmpty()) {
			Component c = o.inputs.remove(0);

			if (! (c instanceof Input)) {
				throw new RuntimeException("Unable to convert the tree to Tseitin form: The last node in the tree must be a artificial tseitin variable!");
			}

			while (c.outputs.remove(o))
				;

			globalAnd.addInput(c);
		}

		o.addInput(globalAnd);

		// verify the structure of the tree
		if (Configuration.SANTIY) {
			tree.verifyTreeStructure();
		}

		return tree;
	}
	
	private Component replaceTrueWithAig() {
		Input input = new Input("true");

		Component not = new Not();
		not.addInput(input);

		Component or = new Or();
		or.addInput(input);
		or.addInput(not);

		// replace all true components
		this.replaceComponent(this.cTrue, or);
		
		return or;
	}
	
	private Component replaceFalseWithAig() {
		Input input = new Input("false");

		Component and = new And();
		and.addInput(input);

		Component notInput = new Not();
		notInput.addInput(input);

		and.addInput(notInput);

		// replace all false components
		this.replaceComponent(this.cFalse, and);
		
		return and;
	}

	public void addQuantifier(Tree tree, Input input, Quantifier quantifier) {
		boolean componentFound = false;

		for (QuantifierSet q : tree.quantifier) {
			if (q.literals.contains(input)) {
				componentFound = true;
				break;
			}
		}

		if (! componentFound) {
			QuantifierSet q = getLastTreeQuantifierOfType(tree, quantifier);

			// merge literals with the same quantifier type if possible

			if (q == null) {
				q = new QuantifierSet(quantifier, input);
				tree.quantifier.add(q);
			}
			else {
				q.literals.add(input);
			}
		}
	}

	private QuantifierSet getLastTreeQuantifierOfType(Tree tree, Quantifier quantifier) {
		final int quantifierSize = tree.quantifier.size();

		if (quantifierSize > 0) {
			QuantifierSet q = tree.quantifier.get(quantifierSize - 1);

			if (q.quantifier == quantifier) {
				return q;
			}
		}

		return null;
	}

	public void replaceComponent(Component c1, Component c2) {
		while (! c1.outputs.isEmpty()) {
			Component parent = c1.outputs.remove(0);
			while (parent.inputs.remove(c1))
				;

			parent.addInput(c2);
		}

		while (! c1.inputs.isEmpty()) {
			Component child = c1.inputs.remove(0);
			while (child.outputs.remove(c1))
				;

			c2.addInput(child);
		}

		// replace latch output entry
		for (int i = 0; i < this.latchOutputs.length; i++) {
			for (int j = 0; j < this.latchOutputs[0].length; j++) {
				if (this.latchOutputs[i][j] == c1) {
					this.latchOutputs[i][j] = c2;
				}
			}
		}
	}
	
	public Tree unroll(int k) {
		if (k < 1) {
			throw new RuntimeException("k must be a positive number");
		}
		
		Tree tree = (Tree) this.clone();
		
		// check basic conditions
		if (tree.outputs.size() == 0 || tree.outputs.get(0).inputs.size() == 0) {
			tree.latchOutputs = new Component[0][0];
			
			return tree;
		}
		
		ArrayList<Component> tempLatchInputs;
		
		// find latches for the connections
		List<Latch> latches = tree.findLatches();
		
		tree.latchOutputs = new Component[k][latches.size()];
		
		if (k > 1) {
			ArrayList<Tree> ts = new ArrayList<>(k - 1);

			// clone the Ts
			for (int i = 2; i <= k; i++) {
				ts.add((Tree) tree.clone());
			}

			int tOffset = ts.get(0).outputs.get(0).getId() - tree.outputs.get(0).getId();

			// connect latches
			for (int i = ts.size() - 1; i > -1; i--) {
				int idOffset = tOffset * (i + 1);
				int prevIdOffset = tOffset * i;
				
				tempLatchInputs = new ArrayList<>(latches.size());
				
				int iL = 0;
				for (Latch l : latches) {
					Component cL = Component.componentHash.get(l.getId() + idOffset);
					Component pL = Component.componentHash.get(l.getId() + prevIdOffset);

					if (! (cL instanceof Latch)) {
						throw new RuntimeException("cL should be a latch!");
					}

					if (! (pL instanceof Latch)) {
						throw new RuntimeException("pL should be a latch!");
					}
					
					// remove the input of the current latch
					Component cIn = cL.inputs.remove(0);
					while (cIn.outputs.remove(cL))
						;
					
					tempLatchInputs.add(cIn);
					
					// connect latch.outputs to latch-1.input
					Component pIn = pL.inputs.get(0);

					while (! cL.outputs.isEmpty()) {
						Component o = cL.outputs.remove(0);
						while (o.inputs.remove(cL))
							;
						
						o.addInput(pIn);
					}
					
					// set current latch output
					tree.latchOutputs[i + 1][iL] = pIn;
					for (int x = i + 2; x < k; x++) {
						for (int y = 0; y < latches.size(); y++) {
							if (tree.latchOutputs[x][y] == cL) {
								tree.latchOutputs[x][y] = pIn;
							}
						}
					}
					
					iL++;
				}

				// if the input of the latch has no purpose, remove it
				for (Component in : tempLatchInputs) {
					if (in.outputs.size() == 0) {
						removeComplete(in);
					}
				}
			}
			
			// merge Ts into first t
			for (Tree t : ts) {
				while (! t.outputs.isEmpty()) {
					Output i = t.outputs.remove(0);

					tree.outputs.add(i);
				}

				// convert True and False inputs to new tree
				while (! t.cFalse.outputs.isEmpty()) {
					Component c = t.cFalse.outputs.remove(0);
					c.inputs.remove(t.cFalse);

					c.addInput(tree.cFalse);
				}

				while (! t.cTrue.outputs.isEmpty()) {
					Component c = t.cTrue.outputs.remove(0);
					c.inputs.remove(t.cTrue);

					c.addInput(tree.cTrue);
				}
			}
		}
		
		tempLatchInputs = new ArrayList<>(latches.size());
		
		// set all latch outputs from first T to false
		int iL = 0;
		for (Latch l : latches) {
			// remove the input of the current latch
			Component in = l.inputs.remove(0);
			while (in.outputs.remove(l))
				;
			
			tempLatchInputs.add(in);

			// replace all latch outputs of the current T with false
			// Other Ts component inputs depend on the current T input
			while (! l.outputs.isEmpty()) {
				Component o = l.outputs.remove(0);
				while (o.inputs.remove(l))
					;

				o.addInput(tree.cFalse);
			}

			// set current latch output
			tree.latchOutputs[0][iL] = tree.cFalse;
			for (int x = 1; x < k; x++) {
				for (int y = 0; y < latches.size(); y++) {
					if (tree.latchOutputs[x][y] == l) {
						tree.latchOutputs[x][y] = tree.cFalse;
					}
				}
			}
			
			iL++;
		}

		// if the input of the latch has no purpose, remove it
		for (Component in : tempLatchInputs) {
			if (in.outputs.size() == 0) {
				removeComplete(in);
			}
		}
		
		/*System.out.println("latchOutputs: ");
		for (int x = 0; x < k; x++) {
			System.out.println("T " + x);
			
			for (int y = 0; y < latches.size(); y++) {
				System.out.println("\t" + tree.latchOutputs[x][y]);
			}
		}*/
		
		// verify the structure of the tree
		if (Configuration.SANTIY) {
			tree.verifyTreeStructure();
		}
		
		return tree;
	}

	public void removeComplete(Component c) {
		LinkedList<Component> qin = new LinkedList<>();

		qin.add(c);

		while (! qin.isEmpty()) {
			c = qin.pop();

			while (! c.inputs.isEmpty()) {
				Component j = c.inputs.remove(0);
				while (j.outputs.remove(c))
					;

				if (j.outputs.size() == 0) {
					qin.add(j);
				}
			}
		}
	}

	public void testInTreeFor(TestTreeChecker checker) {
		HashMap<Component, Component> seen = new HashMap<>();
		Stack<Component> stack = new Stack<>();

		for (Component i : this.outputs) {
			stack.add(i);
			seen.put(i, null);
		}

		while (!stack.isEmpty()) {
			Component n = stack.pop();

			checker.check(this, n);

			for (Component i : n.inputs) {
				if (!seen.containsKey(i)) {
					stack.add(i);
					seen.put(i, null);
				}
			}

			for (Component i : n.outputs) {
				if (!seen.containsKey(i)) {
					stack.add(i);
					seen.put(i, null);
				}
			}
		}
	}

	public void testForWrongFalseOrTrue() {
		testInTreeFor(new TestTreeChecker() {
			@Override
			public void check(Tree tree, Component c) {
				verify(c instanceof True && c == tree.cTrue, c);
				verify(c instanceof False && c == tree.cFalse, c);
			}
		});
	}

	public void verifyTreeStructure() {
		testInTreeFor(new TestTreeChecker() {

			@Override
			public void check(Tree tree, Component c) {
				if (c instanceof Input) {
					verify(c.inputs.size() == 0, c);
				}
				else if (c instanceof Output) {
					verify(c.outputs.size() == 0, c);
				}
				else if (c instanceof True || c instanceof False) {
					verify(c.inputs.size() == 0, c);
				}
				else {
					if (c instanceof And || c instanceof Or) {
						verify(c.inputs.size() >= 1, c);
						verify(c.outputs.size() >= 1, c);
					}

					if (c instanceof Latch) {
						verify(c.inputs.size() == 1, c);
					}
					
					if (c instanceof Not) {
						verify(c.inputs.size() > 0, c);
						verify(c.outputs.size() > 0, c);
					}

					for (Component i : c.inputs) {
						verify(i.outputs.contains(c), c);
					}

					for (Component o : c.outputs) {
						verify(o.inputs.contains(c), c);
					}
				}
			}
		});
	}

	public interface TestTreeChecker {
		public void check(Tree tree, Component c);
	}

	private void verify(boolean b, Component c) {
		if (! b) {
			throw new RuntimeException("Tree verification failed!");
		}
	}
}
