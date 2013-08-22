package at.jku.aig2qbf.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

	public int[] minMaxTseitinVariables = null;

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
		else if (!(c instanceof And)) {
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

		while (!q.isEmpty()) {
			Component o = q.pop();

			for (Component i : o.inputs) {
				if (!components.containsKey(i)) {
					Component n = (Component) i.clone();

					components.put(i, n);

					q.push(i);
				}
			}
		}

		for (Component o : components.keySet()) {
			cloneAddComponentConnections(o, components.get(o), components);
		}

		// clone latch outputs
		nTree.latchOutputs = new Component[this.latchOutputs.length][this.latchOutputs.length == 0 ? 0 : this.latchOutputs[0].length];

		for (int i = 0; i < this.latchOutputs.length; i++) {
			for (int j = 0; j < this.latchOutputs[0].length; j++) {
				Component o = this.latchOutputs[i][j];

				// the component can be empty because we are in the middle of unrolling
				if (o == null) {
					continue;
				}

				// we have to clone unreferenced components as some latch outputs are not in the tree anymore!
				if (!components.containsKey(o)) {
					Component n = (Component) o.clone();

					cloneAddComponentConnections(o, n, components);

					components.put(o, n);
				}

				nTree.latchOutputs[i][j] = components.get(o);
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

	private void cloneAddComponentConnections(Component o, Component n, HashMap<Component, Component> components) {
		for (Component i : o.inputs) {
			n.inputs.add(components.get(i));
		}

		for (Component i : o.outputs) {
			Component c = components.get(i);
			
			if (c == null) {
				// IGNORE this output as it is NOT connected to an component
				// that is the input directly or indirectly for an
				// Tree.output

				continue;
			}

			n.outputs.add(c);
		}
	}

	public Component cloneComponent(Component in) {
		if (in instanceof False) {
			return this.cFalse;
		}
		else if (in instanceof True) {
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

		while (!q.isEmpty()) {
			Component o = q.pop();

			for (Component i : o.inputs) {
				if (!i.isLiteral() && !components.containsKey(i)) {
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
			while (!o.inputs.isEmpty()) {
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

		if (!nTree.isCNF()) {
			throw new RuntimeException("Tree still not in CNF!");
		}

		return nTree;
	}

	public Tree toTseitinCNF() {
		Tree tree = this;

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

		HashMap<Component, Component> seen = new HashMap<>();
		Stack<Component> stack = new Stack<>();

		Component oi = o.inputs.get(0);

		stack.add(oi);

		int tseitinInputCounter = 0;

		QuantifierSet q = null;
		HashMap<Component, Component> seenInput = new HashMap<>();

		int minVariableIndex = Integer.MAX_VALUE;
		int maxVariableIndex = 0;

		// add all existing quantifier in the seen list
		for (QuantifierSet i : tree.quantifier) {
			for (Input j : i.literals) {
				seenInput.put(j, null);

				if (j.getId() < minVariableIndex) {
					minVariableIndex = j.getId();
				}

				if (j.getId() > maxVariableIndex) {
					maxVariableIndex = j.getId();
				}
			}
		}

		if (oi instanceof Input) {
			q = new QuantifierSet(Quantifier.EXISTENTIAL, (Input) oi);
			seenInput.put(oi, null);

			if (oi.getId() < minVariableIndex) {
				minVariableIndex = oi.getId();
			}

			if (oi.getId() > maxVariableIndex) {
				maxVariableIndex = oi.getId();
			}

			seen.put(oi, oi);
		}
		else {
			Input xi = new Input("x" + tseitinInputCounter);
			tseitinInputCounter++;

			if (xi.getId() < minVariableIndex) {
				minVariableIndex = xi.getId();
			}

			if (xi.getId() > maxVariableIndex) {
				maxVariableIndex = xi.getId();
			}

			q = new QuantifierSet(Quantifier.EXISTENTIAL, xi);

			seen.put(oi, xi);
		}

		while (!stack.isEmpty()) {
			Component n = stack.pop();

			for (Component i : n.inputs) {
				if (!seen.containsKey(i)) {
					if (i instanceof Latch) {
						throw new RuntimeException("Unable to convert the tree to Tseitin form: There must not be a latch in the tree. Please unroll the tree first!");
					}

					if (i instanceof True || i instanceof False) {
						throw new RuntimeException("Unable to convert the tree to Tseitin form: There must not be True/False in the tree. Please replace these components first!");
					}

					if (!i.inputs.isEmpty()) {
						stack.add(i);
					}

					if (i instanceof Input) {
						if (!seenInput.containsKey(i)) {
							seenInput.put(i, null);

							if (i.getId() < minVariableIndex) {
								minVariableIndex = i.getId();
							}

							if (i.getId() > maxVariableIndex) {
								maxVariableIndex = i.getId();
							}

							q.literals.add((Input) i);
						}

						seen.put(i, i);
					}
					else {
						Input xi = new Input("x" + tseitinInputCounter);
						tseitinInputCounter++;

						if (xi.getId() < minVariableIndex) {
							minVariableIndex = xi.getId();
						}

						if (xi.getId() > maxVariableIndex) {
							maxVariableIndex = xi.getId();
						}

						q.literals.add(xi);

						seen.put(i, xi);
					}
				}
			}
		}

		tree.minMaxTseitinVariables = new int[] {
			minVariableIndex,
			maxVariableIndex
		};

		tree.quantifier.add(q);

		And globalAnd = new And();

		// transform to structure of Tseitin
		for (Map.Entry<Component, Component> e : seen.entrySet()) {
			Component node = e.getKey();
			Component xi = e.getValue();

			if (node instanceof Input) {
				continue;
			}

			Component notXi = new Not();
			notXi.addInput(xi);

			if (node instanceof And) {
				Component overallOr = new Or();
				overallOr.addInput(xi);

				for (Component i : node.inputs) {
					Component c = seen.get(i);

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

				for (Component i : node.inputs) {
					Component c = seen.get(i);

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

				for (Component i : node.inputs) {
					Component c = seen.get(i);

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
		}

		globalAnd.addInput(seen.get(oi));

		o = new Output(o.getName());
		o.addInput(globalAnd);

		tree.outputs.clear();
		tree.outputs.add(o);

		// verify the structure of the tree
		if (Configuration.SANTIY) {
			tree.verifyTreeStructure();
		}

		return tree;
	}

	public void addNodeInTseitinForm(Component node, Component xi, And globalAnd) {
		Component notXi = new Not();
		notXi.addInput(xi);

		if (node instanceof And) {
			Component overallOr = new Or();
			overallOr.addInput(xi);

			while (!node.inputs.isEmpty()) {
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

			while (!node.inputs.isEmpty()) {
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

			while (!node.inputs.isEmpty()) {
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

	public void addQuantifier(Input input, Quantifier quantifier, boolean checkContains) {
		boolean componentFound = false;

		if (checkContains) {
			for (QuantifierSet q : this.quantifier) {
				if (q.literals.contains(input)) {
					componentFound = true;
					break;
				}
			}
		}

		if (!componentFound) {
			QuantifierSet q = getLastTreeQuantifierOfType(quantifier);

			// merge literals with the same quantifier type if possible

			if (q == null) {
				q = new QuantifierSet(quantifier, input);
				this.quantifier.add(q);
			}
			else {
				q.literals.add(input);
			}
		}
	}

	public void addQuantifier(Input input, Quantifier quantifier) {
		this.addQuantifier(input, quantifier, true);
	}

	private QuantifierSet getLastTreeQuantifierOfType(Quantifier quantifier) {
		final int quantifierSize = this.quantifier.size();

		if (quantifierSize > 0) {
			QuantifierSet q = this.quantifier.get(quantifierSize - 1);

			if (q.quantifier == quantifier) {
				return q;
			}
		}

		return null;
	}

	public void replaceComponent(Component c1, Component c2) {
		while (!c1.outputs.isEmpty()) {
			Component parent = c1.outputs.remove(0);
			while (parent.inputs.remove(c1))
				;

			parent.addInput(c2);
		}

		while (!c1.inputs.isEmpty()) {
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
		// because of k=0
		k++;

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

		HashMap<Component, Component> usedLatchOutputs = new HashMap<>();
		tree.latchOutputs = new Component[latches.size() > 0 ? k : 0][latches.size()];

		if (k > 1) {
			ArrayList<Tree> ts = new ArrayList<>(k - 1);

			// clone the Ts
			for (int i = 2; i <= k; i++) {
				ts.add((Tree) tree.clone());
			}

			// convert true and false before using them (this is saver)
			for (Tree t : ts) {
				// convert True and False inputs to new tree
				while (!t.cFalse.outputs.isEmpty()) {
					Component c = t.cFalse.outputs.remove(0);
					c.inputs.remove(t.cFalse);

					c.addInput(tree.cFalse);
				}

				while (!t.cTrue.outputs.isEmpty()) {
					Component c = t.cTrue.outputs.remove(0);
					c.inputs.remove(t.cTrue);

					c.addInput(tree.cTrue);
				}
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

					if (!(cL instanceof Latch)) {
						throw new RuntimeException("cL should be a latch!");
					}

					if (!(pL instanceof Latch)) {
						throw new RuntimeException("pL should be a latch!");
					}

					// remove the input of the current latch
					Component cIn = cL.inputs.remove(0);
					while (cIn.outputs.remove(cL))
						;

					tempLatchInputs.add(cIn);

					// connect latch.outputs to latch-1.input
					Component pIn = pL.inputs.get(0);

					while (!cL.outputs.isEmpty()) {
						Component o = cL.outputs.remove(0);
						while (o.inputs.remove(cL))
							;

						o.addInput(pIn);
					}

					// set current latch output
					tree.latchOutputs[i + 1][iL] = pIn;
					usedLatchOutputs.put(pIn, null);

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
					if (in.outputs.size() == 0 && !usedLatchOutputs.containsKey(in)) {
						removeCompletely(in);
					}
				}
			}

			// merge Ts into first t
			for (Tree t : ts) {
				while (!t.outputs.isEmpty()) {
					Output i = t.outputs.remove(0);

					tree.outputs.add(i);
				}
			}
		}

		if (latches.size() > 0) {
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
				while (!l.outputs.isEmpty()) {
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
				if (in.outputs.size() == 0 && !usedLatchOutputs.containsKey(in)) {
					removeCompletely(in);
				}
			}
		}

		// System.out.println("latchOutputs: ");
		// for (int x = 0; x < k; x++) {
		// System.out.println("T " + x);
		//
		// for (int y = 0; y < latches.size(); y++) {
		// System.out.println(String.format("\t%s (in=%d, out=%d)", tree.latchOutputs[x][y], tree.latchOutputs[x][y].inputs.size(), tree.latchOutputs[x][y].outputs.size()));
		// }
		// }

		// verify the structure of the tree
		if (Configuration.SANTIY) {
			for (int x = 0; x < k; x++) {
				for (int y = 0; y < latches.size(); y++) {
					if (tree.latchOutputs[x][y] instanceof False) {
						verify(tree.latchOutputs[x][y] == tree.cFalse, tree.latchOutputs[x][y]);
					}
					else if (tree.latchOutputs[x][y] instanceof True) {
						verify(tree.latchOutputs[x][y] == tree.cTrue, tree.latchOutputs[x][y]);
					}
				}
			}

			tree.verifyTreeStructure();
		}

		return tree;
	}

	public void removeCompletely(Component c) {
		LinkedList<Component> qin = new LinkedList<>();

		qin.add(c);

		while (!qin.isEmpty()) {
			c = qin.pop();

			while (!c.inputs.isEmpty()) {
				Component j = c.inputs.remove(0);
				while (j.outputs.remove(c))
					;

				if (j.outputs.size() == 0) {
					qin.add(j);
				}
			}
		}
	}

	protected boolean containsLatchOutputs(Component c) {
		for (int x = 0; x < this.latchOutputs.length; x++) {
			for (int y = 0; y < this.latchOutputs[0].length; y++) {
				if (this.latchOutputs[x][y] == c) {
					return true;
				}
			}
		}

		return false;
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

					if (c.outputs.size() == 0 && !tree.containsLatchOutputs(c)) {
						verify(c.outputs.size() != 0, c);
					}
				}
				else if (c instanceof Output) {
					verify(c.outputs.size() == 0, c);
				}
				else if (c instanceof True || c instanceof False) {
					verify(c.inputs.size() == 0, c);

					if (c instanceof True) {
						verify(c == tree.cTrue, c);
					}
					else {
						verify(c == tree.cFalse, c);
					}
				}
				else {
					if (c instanceof And || c instanceof Or) {
						verify(c.inputs.size() >= 1, c);

						if (c.outputs.size() == 0 && !tree.containsLatchOutputs(c)) {
							verify(c.outputs.size() != 0, c);
						}
					}

					if (c instanceof Latch) {
						verify(c.inputs.size() == 1, c);
					}

					if (c instanceof Not) {
						verify(c.inputs.size() > 0, c);

						if (c.outputs.size() == 0 && !tree.containsLatchOutputs(c)) {
							verify(c.outputs.size() != 0, c);
						}
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
		if (!b) {
			System.out.println("Failed component " + c + "(" + c.getId() + ")");
			throw new RuntimeException("Tree verification failed!");
		}
	}
}
