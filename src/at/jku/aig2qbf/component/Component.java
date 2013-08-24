package at.jku.aig2qbf.component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import at.jku.aig2qbf.Configuration;

public abstract class Component implements Cloneable {
	private static final int COMPONENT_ARRAY_SIZE = 32768;

	public static int ComponentId = 0;
	public static Component[] ComponentArray = new Component[COMPONENT_ARRAY_SIZE];

	public static void ResetComponentArray() {
		ComponentId = 0;
		ComponentArray = new Component[COMPONENT_ARRAY_SIZE];
	}

	private static void AddComponentToArray(int id, Component component) {
		if (id >= ComponentArray.length) {
			final int componentArrayLength = ComponentArray.length;

			Component[] tmp = new Component[componentArrayLength * 2];

			for (int i = 0; i < componentArrayLength; i++) {
				tmp[i] = ComponentArray[i];
			}

			ComponentArray = tmp;
		}

		ComponentArray[id] = component;
	}

	private final int id;
	private String name;

	public List<Component> inputs;
	public List<Component> outputs;

	public Component() {
		this.id = ComponentId++;
		this.name = null;

		this.inputs = new ArrayList<Component>();
		this.outputs = new ArrayList<Component>();

		AddComponentToArray(this.id, this);
	}

	public Component(String name) {
		this.id = ComponentId++;
		this.name = name;

		this.inputs = new ArrayList<Component>();
		this.outputs = new ArrayList<Component>();

		AddComponentToArray(this.id, this);
	}

	public int getId() {
		return this.id;
	}

	public void addInput(Component c) {
		if (Configuration.SANTIY) {
			if (!this.inputs.contains(c)) {
				this.inputs.add(c);
			}
			if (!c.outputs.contains(this)) {
				c.outputs.add(this);
			}

			if (this instanceof Input) {
				throw new RuntimeException("Unable to add input to component: Input must not get an additional input!");
			}
			else if (this.equals(c) && !(c instanceof Latch)) {
				throw new RuntimeException("Unable to add input to component: Self loops are only valid for latches!");
			}
		}
		else {
			this.inputs.add(c);
			c.outputs.add(this);
		}
	}

	public Component addNot() {
		Not n = new Not();

		this.swapWith(n);

		n.addInput(this);

		return n;
	}

	@Override
	abstract protected Object clone();

	public Component copy() {
		Component n = (Component) this.clone();

		for (Component i : this.inputs) {
			n.addInput(i);
		}

		for (Component i : this.outputs) {
			i.addInput(n);
		}

		return n;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isLiteral() {
		return this instanceof Input || this instanceof True || this instanceof False;
	}

	public int getNumberOfLiterals() {
		int numLiterals = 0;

		for (int i = 0; i < this.inputs.size(); i++) {
			Component c = this.inputs.get(i);

			if (c.isLiteral()) {
				numLiterals++;
			}
		}

		return numLiterals;
	}

	public void mergeChilds() {
		List<Component> mergeList = new ArrayList<Component>();

		do {
			mergeList.clear();

			for (Component c : this.inputs) {
				if (c.getClass().equals(this.getClass()) && c.outputs.size() == 1) {
					mergeList.add(c);
				}
			}

			for (Component c : mergeList) {
				this.inputs.remove(c);
				c.outputs.remove(this);

				for (Component i : c.inputs) {
					if (!this.inputs.contains(i)) {
						this.addInput(i);
					}

					i.outputs.remove(c);
				}

				c.inputs.clear();
			}
		} while (mergeList.size() > 0);
	}

	private void mergeComponentPair(Component a, Component b) {
		a.outputs.remove(b);
		b.inputs.remove(a);

		// Add outputs of b to outputs of a
		for (Component c : b.outputs) {
			c.inputs.remove(b);

			if (!c.inputs.contains(a)) {
				c.inputs.add(a);
			}

			if (!a.outputs.contains(c)) {
				a.outputs.add(c);
			}
		}

		// Add inputs of b to inputs of a
		for (Component c : b.inputs) {
			c.outputs.remove(b);

			if (!c.outputs.contains(a)) {
				c.outputs.add(a);
			}

			if (!a.inputs.contains(c)) {
				a.inputs.add(c);
			}
		}

		// Isolate b
		b.inputs.clear();
		b.outputs.clear();
	}

	public void mergeWithParent() {
		List<Component> mergeList = new ArrayList<Component>();

		do {
			mergeList.clear();

			for (Component c : this.outputs) {
				if (c.getClass().equals(this.getClass())) {
					mergeList.add(c);
				}
			}

			for (Component c : mergeList) {
				mergeComponentPair(this, c);
			}
		} while (mergeList.size() > 0);
	}

	public void pushDistributive() {
		Stack<TracePair> trace = new Stack<>();

		trace.push(new TracePair(this, 0));

		while (!trace.isEmpty()) {
			TracePair t = trace.pop();

			while (t.c.inputs.size() != t.i) {
				Component child = t.c.inputs.get(t.i++);

				if (!child.isLiteral() && !(child instanceof Not)) {
					trace.push(t);

					t = new TracePair(child, 0);

					trace.push(t);

					break;
				}
			}

			if (t.c.inputs.size() == t.i) {
				if (t.c instanceof And || t.c instanceof Or) {
					t.c.pushDistributiveDo();
				}
			}
		}
	}

	private void pushDistributiveDo() {
		if (this instanceof Output) {
			if (this.inputs.size() != 1) {
				throw new RuntimeException("OUTPUT must have exactly one input");
			}

			this.inputs.get(0).pushDistributive();
		}
		else if (this.isLiteral() || this instanceof Not) {
			return;
		}
		else if (!(this instanceof And || this instanceof Or)) {
			throw new RuntimeException("pushDistributive cannot handle " + this.getName());
		}

		this.mergeChilds();

		Component n = (this instanceof Or) ? new And() : new Or();

		LinkedList<Component> qcl = new LinkedList<>();
		LinkedList<Component> qliterals = new LinkedList<>();

		for (Component i : this.inputs) {
			if (i.getClass().equals(n.getClass())) {
				qcl.push(i);
			}
			else if (i.isLiteral()) {
				qliterals.push(i);
			}
			else if (i instanceof Not) {
				if (i.inputs.size() != 1) {
					throw new RuntimeException("NOT must have exactly one input");
				}

				if (i.inputs.get(0).isLiteral()) {
					qliterals.push(i);
				}
			}
		}

		if (qcl.size() < 2 && (qcl.size() != 1 || qliterals.size() == 0)) {
			return;
		}

		for (Component i : qcl) {
			this.inputs.remove(i);
			i.outputs.remove(this);
		}

		for (Component i : qliterals) {
			this.inputs.remove(i);
			i.outputs.remove(this);
		}

		Component qcl0 = qcl.pollLast();
		ArrayList<Component> ql = new ArrayList<>(qcl0.inputs.size());

		while (!qcl0.inputs.isEmpty()) {
			Component i = qcl0.inputs.remove(0);
			i.outputs.remove(qcl0);

			Component t = (Component) this.clone();
			t.addInput(i);

			ql.add(t);
		}

		while (!qcl.isEmpty()) {
			Component cl = qcl.pollLast();

			ArrayList<Component> nql = new ArrayList<>(ql.size() * cl.inputs.size());

			for (Component i : ql) {
				for (Component j : cl.inputs) {
					Component iCopy = i.copy();

					iCopy.addInput(j);

					nql.add(iCopy);
				}

				while (!i.inputs.isEmpty()) {
					Component j = i.inputs.remove(0);
					j.outputs.remove(i);
				}
			}

			while (!cl.inputs.isEmpty()) {
				Component j = cl.inputs.remove(0);
				j.outputs.remove(cl);
			}

			ql = nql;
		}

		if (!qliterals.isEmpty()) {
			for (Component i : ql) {
				for (Component j : qliterals) {
					i.addInput(j);
				}
			}
		}

		for (Component i : ql) {
			n.addInput(i);
		}

		this.swapWith(n);
	}

	private class TracePair {
		public Component c;
		public int i;

		public TracePair(Component c, int i) {
			this.c = c;
			this.i = i;
		}
	}

	public void pushNots() {
		Component c = this;

		if (c.isLiteral()) {
			return;
		}
		else if (c instanceof Not) {
			if (c.inputs.size() != 1) {
				throw new RuntimeException("NOT must have exactly one input");
			}

			Component n = c;
			c = c.inputs.get(0);

			if (c.isLiteral()) {
				return;
			}
			else {
				c.mergeChilds();

				c = c.swapWith((c instanceof Or) ? new And() : new Or());

				for (int i = 0; i < c.inputs.size(); i++) {
					c.inputs.get(i).addNot();
				}

				n.inputs.remove(c);
				c.outputs.remove(n);

				n.swapWith(c);
			}
		}
		else if (c instanceof Output) {
			if (c.inputs.size() != 1) {
				throw new RuntimeException("OUTPUT must have exactly one input");
			}

			c.inputs.get(0).pushNots();

			return;
		}
		else if (!(c instanceof And || c instanceof Or)) {
			throw new RuntimeException("pushNots cannot handle " + this.getName());
		}

		for (int i = 0; i < c.inputs.size(); i++) {
			c.inputs.get(i).pushNots();
		}

		c.mergeChilds();
	}

	public void remove() {
		while (!this.inputs.isEmpty()) {
			Component r = this.inputs.remove(0);
			r.outputs.remove(this);
		}
		while (!this.outputs.isEmpty()) {
			Component r = this.outputs.remove(0);
			r.inputs.remove(this);
		}
	}

	public Component swapWith(Component n) {
		if (this.equals(n)) {
			return this;
		}

		for (int i = 0; i < this.inputs.size(); i++) {
			Component c = this.inputs.get(i);

			n.inputs.add(c);
			c.outputs.set(c.outputs.indexOf(this), n);
		}

		for (int i = 0; i < this.outputs.size(); i++) {
			Component c = this.outputs.get(i);

			n.outputs.add(c);
			c.inputs.set(c.inputs.indexOf(this), n);
		}

		this.inputs.clear();
		this.outputs.clear();

		return n;
	}
}
