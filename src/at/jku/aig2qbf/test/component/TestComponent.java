package at.jku.aig2qbf.test.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.test.BaseTest;

public class TestComponent extends BaseTest {

	@Before
	public void setUp() throws Exception {
		Component.Reset();
	}

	@Test
	public void testSwap() {
		Input x = new Input("x");
		Input y = new Input("y");
		And and = new And();
		Output z = new Output("z");

		and.addInput(x);
		and.addInput(y);
		z.addInput(and);

		Or or = new Or();

		and.swapWith(or);

		// and
		assertEquals(0, and.inputs.size());
		assertEquals(0, and.outputs.size());

		// or
		assertEquals(2, or.inputs.size());
		assertEquals(1, or.outputs.size());
		assertSame(x, or.inputs.get(0));
		assertSame(y, or.inputs.get(1));
		assertSame(z, or.outputs.get(0));

		// x
		assertEquals(0, x.inputs.size());
		assertEquals(1, x.outputs.size());
		assertSame(or, x.outputs.get(0));

		// y
		assertEquals(0, y.inputs.size());
		assertEquals(1, y.outputs.size());
		assertSame(or, y.outputs.get(0));

		// z
		assertEquals(1, z.inputs.size());
		assertEquals(0, z.outputs.size());
		assertSame(or, z.inputs.get(0));
	}

	@Test
	public void testAddNot() {
		Input x = new Input("x");
		Input y = new Input("y");
		And and = new And();
		Output z = new Output("z");

		and.addInput(x);
		and.addInput(y);
		z.addInput(and);

		x.addNot();

		// and
		assertEquals(2, and.inputs.size());
		assertEquals(1, and.outputs.size());
		assertNotSame(x, and.inputs.get(0));
		assertSame(y, and.inputs.get(1));
		assertSame(z, and.outputs.get(0));

		Component n = and.inputs.get(0);
		assertTrue(n instanceof Not);
		assertEquals(1, n.inputs.size());
		assertEquals(1, n.outputs.size());
		assertSame(x, n.inputs.get(0));
		assertSame(and, n.outputs.get(0));

		// x
		assertEquals(0, x.inputs.size());
		assertEquals(1, x.outputs.size());
		assertSame(n, x.outputs.get(0));

		// y
		assertEquals(0, y.inputs.size());
		assertEquals(1, y.outputs.size());
		assertSame(and, y.outputs.get(0));

		// z
		assertEquals(1, z.inputs.size());
		assertEquals(0, z.outputs.size());
		assertSame(and, z.inputs.get(0));
	}

	@Test
	public void testMergeWithParents0() {
		Component a = new Input("a");
		Component b = new Input("b");
		Component c = new Input("c");

		Component andAB = new And();
		andAB.addInput(a);
		andAB.addInput(b);

		Component andABC = new And();
		andABC.addInput(andAB);
		andABC.addInput(c);

		// Merge input nodes
		a.mergeWithParent();
		assertEquals(1, a.outputs.size());
		assertEquals(1, b.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertEquals(1, andAB.outputs.size());
		assertEquals(2, andABC.inputs.size());

		// Merge parent and node
		andABC.mergeWithParent();
		assertEquals(1, a.outputs.size());
		assertEquals(1, b.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertEquals(1, andAB.outputs.size());
		assertEquals(2, andABC.inputs.size());

		// Merge root and node
		andAB.mergeWithParent();
		assertEquals(1, a.outputs.size());
		assertEquals(1, b.outputs.size());
		assertEquals(3, andAB.inputs.size());
		assertEquals(0, andAB.outputs.size());
		assertEquals(0, andABC.inputs.size());
	}

	@Test
	public void testMergeWithParents1() {
		Component a = new Input("a");
		Component b = new Input("b");
		Component c = new Input("c");
		Component d = new Input("d");
		Component e = new Output("e");

		Component andAB = new And();
		andAB.addInput(a);
		andAB.addInput(b);

		Component andBC = new And();
		andBC.addInput(b);
		andBC.addInput(c);

		Component or = new Or();
		or.addInput(andAB);
		or.addInput(andBC);

		// Merge disjunct and's
		andAB.mergeWithParent();
		assertEquals(1, a.outputs.size());
		assertEquals(2, b.outputs.size());
		assertEquals(1, c.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertEquals(1, andAB.outputs.size());
		assertEquals(2, andBC.inputs.size());
		assertEquals(1, andBC.outputs.size());

		or.inputs.clear();
		or.outputs.clear();
		andAB.outputs.remove(or);
		andBC.outputs.remove(or);

		Component andABC = new And();
		andABC.addInput(andAB);
		andABC.addInput(andBC);

		or = new Or();
		or.addInput(andABC);
		or.addInput(d);

		e.addInput(or);

		// Initial graph structure
		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertEquals(0, b.inputs.size());
		assertEquals(2, b.outputs.size());
		assertEquals(0, c.inputs.size());
		assertEquals(1, c.outputs.size());
		assertEquals(0, d.inputs.size());
		assertEquals(1, d.outputs.size());
		assertEquals(1, e.inputs.size());
		assertEquals(0, e.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertEquals(1, andAB.outputs.size());
		assertEquals(2, andBC.inputs.size());
		assertEquals(1, andBC.outputs.size());
		assertEquals(2, andABC.inputs.size());
		assertEquals(1, andABC.outputs.size());
		assertEquals(2, or.inputs.size());
		assertEquals(1, or.outputs.size());

		// Only merge from input to output
		andABC.mergeWithParent();
		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertEquals(0, b.inputs.size());
		assertEquals(2, b.outputs.size());
		assertEquals(0, c.inputs.size());
		assertEquals(1, c.outputs.size());
		assertEquals(0, d.inputs.size());
		assertEquals(1, d.outputs.size());
		assertEquals(1, e.inputs.size());
		assertEquals(0, e.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertEquals(1, andAB.outputs.size());
		assertEquals(2, andBC.inputs.size());
		assertEquals(1, andBC.outputs.size());
		assertEquals(2, andABC.inputs.size());
		assertEquals(1, andABC.outputs.size());
		assertEquals(2, or.inputs.size());
		assertEquals(1, or.outputs.size());

		// Merge andBC with andABC
		andBC.mergeWithParent();
		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertEquals(0, b.inputs.size());
		assertEquals(2, b.outputs.size());
		assertEquals(0, c.inputs.size());
		assertEquals(1, c.outputs.size());
		assertEquals(0, d.inputs.size());
		assertEquals(1, d.outputs.size());
		assertEquals(1, e.inputs.size());
		assertEquals(0, e.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertEquals(1, andAB.outputs.size());
		assertEquals(3, andBC.inputs.size());
		assertEquals(1, andBC.outputs.size());
		assertEquals(0, andABC.inputs.size());
		assertEquals(0, andABC.outputs.size());
		assertEquals(2, or.inputs.size());
		assertEquals(1, or.outputs.size());
	}

	@Test
	public void testMergeChilds0() {
		Component a = new Input("a");
		Component b = new Input("b");
		Component c = new Input("c");

		Component andAB = new And();
		andAB.addInput(a);
		andAB.addInput(b);

		Component andABC = new And();
		andABC.addInput(andAB);
		andABC.addInput(c);

		a.mergeChilds();
		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andAB, a.outputs.get(0));

		andAB.mergeChilds();
		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andAB, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andAB, b.outputs.get(0));
		assertEquals(2, andAB.inputs.size());
		assertSame(a, andAB.inputs.get(0));
		assertSame(b, andAB.inputs.get(1));
		assertEquals(1, andAB.outputs.size());
		assertSame(andABC, andAB.outputs.get(0));

		andABC.mergeChilds();
		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andABC, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andABC, b.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andABC, c.outputs.get(0));
		assertEquals(0, andAB.inputs.size());
		assertEquals(0, andAB.outputs.size());
		assertEquals(3, andABC.inputs.size());
		assertSame(c, andABC.inputs.get(0));
		assertSame(a, andABC.inputs.get(1));
		assertSame(b, andABC.inputs.get(2));
		assertEquals(0, andABC.outputs.size());
	}

	@Test
	public void testMergeChilds1() {
		Component a = new Input("a");
		Component b = new Input("b");
		Component c = new Input("c");
		Component d = new Input("d");

		And ab = new And();
		ab.addInput(a);
		ab.addInput(b);

		And ab_d = new And();
		ab_d.addInput(ab);
		ab_d.addInput(d);

		And abc = new And();
		abc.addInput(a);
		abc.addInput(b);
		abc.addInput(c);

		And ab_d_abc = new And();
		ab_d_abc.addInput(ab_d);
		ab_d_abc.addInput(abc);

		ab_d_abc.mergeChilds();

		assertEquals(0, ab.inputs.size());
		assertEquals(0, ab.outputs.size());
		assertEquals(0, ab_d.inputs.size());
		assertEquals(0, ab_d.outputs.size());
		assertEquals(0, abc.inputs.size());
		assertEquals(0, abc.outputs.size());

		assertEquals(4, ab_d_abc.inputs.size());
		assertSame(d, ab_d_abc.inputs.get(0));
		assertSame(a, ab_d_abc.inputs.get(1));
		assertSame(b, ab_d_abc.inputs.get(2));
		assertSame(c, ab_d_abc.inputs.get(3));
		assertEquals(0, ab_d_abc.outputs.size());

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(ab_d_abc, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(ab_d_abc, b.outputs.get(0));
		assertEquals(0, c.inputs.size());
		assertEquals(1, c.outputs.size());
		assertSame(ab_d_abc, c.outputs.get(0));
		assertEquals(0, d.inputs.size());
		assertEquals(1, d.outputs.size());
		assertSame(ab_d_abc, d.outputs.get(0));
	}

	@Test
	public void testPushNots0() throws Exception {
		Component a = new Input("a");
		Component b = new Input("b");

		Component andAB = new And();
		andAB.addInput(a);
		andAB.addInput(b);

		Component c = new Output("c");
		c.addInput(andAB);

		// Pushing nots from input/output should not change anything

		a.pushNots();

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andAB, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andAB, b.outputs.get(0));
		assertEquals(2, andAB.inputs.size());
		assertSame(a, andAB.inputs.get(0));
		assertSame(b, andAB.inputs.get(1));
		assertEquals(1, andAB.outputs.size());
		assertSame(c, andAB.outputs.get(0));
		assertEquals(1, c.inputs.size());
		assertSame(andAB, c.inputs.get(0));
		assertEquals(0, c.outputs.size());

		b.pushNots();

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andAB, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andAB, b.outputs.get(0));
		assertEquals(2, andAB.inputs.size());
		assertSame(a, andAB.inputs.get(0));
		assertSame(b, andAB.inputs.get(1));
		assertEquals(1, andAB.outputs.size());
		assertSame(c, andAB.outputs.get(0));
		assertEquals(1, c.inputs.size());
		assertSame(andAB, c.inputs.get(0));
		assertEquals(0, c.outputs.size());

		c.pushNots();

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andAB, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andAB, b.outputs.get(0));
		assertEquals(2, andAB.inputs.size());
		assertSame(a, andAB.inputs.get(0));
		assertSame(b, andAB.inputs.get(1));
		assertEquals(1, andAB.outputs.size());
		assertSame(c, andAB.outputs.get(0));
		assertEquals(1, c.inputs.size());
		assertSame(andAB, c.inputs.get(0));
		assertEquals(0, c.outputs.size());
	}

	@Test
	public void testPushNots1() throws Exception {
		Component a = new Input("a");
		Component b = new Input("b");
		Component c = new Output("c");

		Component andAB = new And();
		andAB.addInput(a);
		andAB.addInput(b);

		Component notAndAB = new Not();
		notAndAB.addInput(andAB);

		c.addInput(notAndAB);

		// Pushing nots from not negated components should not change anything

		a.pushNots();

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andAB, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andAB, b.outputs.get(0));
		assertEquals(1, c.inputs.size());
		assertSame(notAndAB, c.inputs.get(0));
		assertEquals(0, c.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertSame(a, andAB.inputs.get(0));
		assertSame(b, andAB.inputs.get(1));
		assertEquals(1, andAB.outputs.size());
		assertSame(notAndAB, andAB.outputs.get(0));
		assertEquals(1, notAndAB.inputs.size());
		assertSame(andAB, notAndAB.inputs.get(0));
		assertEquals(1, notAndAB.outputs.size());
		assertSame(c, notAndAB.outputs.get(0));

		b.pushNots();

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andAB, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andAB, b.outputs.get(0));
		assertEquals(1, c.inputs.size());
		assertSame(notAndAB, c.inputs.get(0));
		assertEquals(0, c.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertSame(a, andAB.inputs.get(0));
		assertSame(b, andAB.inputs.get(1));
		assertEquals(1, andAB.outputs.size());
		assertSame(notAndAB, andAB.outputs.get(0));
		assertEquals(1, notAndAB.inputs.size());
		assertSame(andAB, notAndAB.inputs.get(0));
		assertEquals(1, notAndAB.outputs.size());
		assertSame(c, notAndAB.outputs.get(0));

		andAB.pushNots();

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(andAB, a.outputs.get(0));
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(andAB, b.outputs.get(0));
		assertEquals(1, c.inputs.size());
		assertSame(notAndAB, c.inputs.get(0));
		assertEquals(0, c.outputs.size());
		assertEquals(2, andAB.inputs.size());
		assertSame(a, andAB.inputs.get(0));
		assertSame(b, andAB.inputs.get(1));
		assertEquals(1, andAB.outputs.size());
		assertSame(notAndAB, andAB.outputs.get(0));
		assertEquals(1, notAndAB.inputs.size());
		assertSame(andAB, notAndAB.inputs.get(0));
		assertEquals(1, notAndAB.outputs.size());
		assertSame(c, notAndAB.outputs.get(0));

		// Push the not downwards and apply DeMorgan: NOT(a AND b) -> NOT a OR
		// NOT b

		c.pushNots();

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertEquals(1, c.inputs.size());
		assertEquals(0, c.outputs.size());
		assertEquals(0, andAB.inputs.size());
		assertEquals(0, andAB.outputs.size());
		assertEquals(0, notAndAB.inputs.size());
		assertEquals(0, notAndAB.outputs.size());

		Component nA = a.outputs.get(0);
		Component nB = b.outputs.get(0);
		assertTrue(nA instanceof Not);
		assertEquals(1, nA.inputs.size());
		assertSame(a, nA.inputs.get(0));
		assertEquals(1, nA.outputs.size());
		assertTrue(nB instanceof Not);
		assertEquals(1, nB.inputs.size());
		assertSame(b, nB.inputs.get(0));
		assertEquals(1, nB.outputs.size());

		Component orAB = c.inputs.get(0);
		assertTrue(orAB instanceof Or);
		assertEquals(2, orAB.inputs.size());
		assertSame(nA, orAB.inputs.get(0));
		assertSame(nB, orAB.inputs.get(1));
		assertEquals(1, orAB.outputs.size());
		assertSame(c, orAB.outputs.get(0));
	}

	@Test
	public void copy() {
		Input a = new Input("a");
		Input b = new Input("b");
		Input c = new Input("c");

		Or bc = new Or();
		bc.addInput(b);
		bc.addInput(c);

		And a_bc = new And();
		a_bc.addInput(a);
		a_bc.addInput(bc);

		Component copy_bc = bc.copy();

		assertTrue(copy_bc instanceof Or);
		assertEquals(2, copy_bc.inputs.size());
		assertSame(b, copy_bc.inputs.get(0));
		assertSame(c, copy_bc.inputs.get(1));
		assertEquals(1, copy_bc.outputs.size());
		assertSame(a_bc, copy_bc.outputs.get(0));

		assertEquals(3, a_bc.inputs.size());
		assertSame(a, a_bc.inputs.get(0));
		assertSame(bc, a_bc.inputs.get(1));
		assertSame(copy_bc, a_bc.inputs.get(2));
		assertEquals(0, a_bc.outputs.size());

		assertEquals(0, b.inputs.size());
		assertEquals(2, b.outputs.size());
		assertSame(bc, b.outputs.get(0));
		assertSame(copy_bc, b.outputs.get(1));

		assertEquals(0, c.inputs.size());
		assertEquals(2, c.outputs.size());
		assertSame(bc, c.outputs.get(0));
		assertSame(copy_bc, c.outputs.get(1));
	}

	@Test
	public void pushDistributive0() {
		Input a = new Input("a");
		Input b = new Input("b");
		Input c = new Input("c");

		And ab = new And();
		ab.addInput(a);
		ab.addInput(b);

		Or abc = new Or();
		abc.addInput(ab);
		abc.addInput(c);

		Output y = new Output("y");
		y.addInput(abc);

		y.pushDistributive();

		assertEquals(0, y.outputs.size());
		assertEquals(1, y.inputs.size());

		Component nand = y.inputs.get(0);
		assertTrue(nand instanceof And);
		assertEquals(1, nand.outputs.size());
		assertSame(y, nand.outputs.get(0));
		assertEquals(2, nand.inputs.size());

		Component lor = nand.inputs.get(0);
		assertTrue(lor instanceof Or);
		assertEquals(1, lor.outputs.size());
		assertSame(nand, lor.outputs.get(0));
		assertEquals(2, lor.inputs.size());
		assertSame(a, lor.inputs.get(0));
		assertSame(c, lor.inputs.get(1));

		Component ror = nand.inputs.get(1);
		assertTrue(ror instanceof Or);
		assertEquals(1, ror.outputs.size());
		assertSame(nand, ror.outputs.get(0));
		assertEquals(2, ror.inputs.size());
		assertSame(b, ror.inputs.get(0));
		assertSame(c, ror.inputs.get(1));

		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(lor, a.outputs.get(0));

		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
		assertSame(ror, b.outputs.get(0));

		assertEquals(0, c.inputs.size());
		assertEquals(2, c.outputs.size());
		assertSame(lor, c.outputs.get(0));
		assertSame(ror, c.outputs.get(1));
	}

	@Test
	public void pushDistributive1() {
		Input a = new Input("a");
		Input b = new Input("b");
		Input c = new Input("c");
		Input d = new Input("d");

		And ab = new And();
		ab.addInput(a);
		ab.addInput(b);

		And cd = new And();
		cd.addInput(c);
		cd.addInput(d);

		Or abcd = new Or();
		abcd.addInput(ab);
		abcd.addInput(cd);

		Output y = new Output("y");
		y.addInput(abcd);

		y.pushDistributive();

		assertEquals(0, y.outputs.size());
		assertEquals(1, y.inputs.size());

		Component nand = y.inputs.get(0);
		assertTrue(nand instanceof And);
		assertEquals(1, nand.outputs.size());
		assertSame(y, nand.outputs.get(0));
		assertEquals(4, nand.inputs.size());

		Component or1 = nand.inputs.get(0);
		assertTrue(or1 instanceof Or);
		assertEquals(1, or1.outputs.size());
		assertSame(nand, or1.outputs.get(0));
		assertEquals(2, or1.inputs.size());
		assertSame(a, or1.inputs.get(0));
		assertSame(c, or1.inputs.get(1));

		Component or2 = nand.inputs.get(1);
		assertTrue(or2 instanceof Or);
		assertEquals(1, or2.outputs.size());
		assertSame(nand, or2.outputs.get(0));
		assertEquals(2, or2.inputs.size());
		assertSame(a, or2.inputs.get(0));
		assertSame(d, or2.inputs.get(1));

		Component or3 = nand.inputs.get(2);
		assertTrue(or3 instanceof Or);
		assertEquals(1, or3.outputs.size());
		assertSame(nand, or3.outputs.get(0));
		assertEquals(2, or3.inputs.size());
		assertSame(b, or3.inputs.get(0));
		assertSame(c, or3.inputs.get(1));

		Component or4 = nand.inputs.get(3);
		assertTrue(or4 instanceof Or);
		assertEquals(1, or4.outputs.size());
		assertSame(nand, or4.outputs.get(0));
		assertEquals(2, or4.inputs.size());
		assertSame(b, or4.inputs.get(0));
		assertSame(d, or4.inputs.get(1));

		assertEquals(0, a.inputs.size());
		assertEquals(2, a.outputs.size());
		assertSame(or1, a.outputs.get(0));
		assertSame(or2, a.outputs.get(1));

		assertEquals(0, b.inputs.size());
		assertEquals(2, b.outputs.size());
		assertSame(or3, b.outputs.get(0));
		assertSame(or4, b.outputs.get(1));

		assertEquals(0, c.inputs.size());
		assertEquals(2, c.outputs.size());
		assertSame(or1, c.outputs.get(0));
		assertSame(or3, c.outputs.get(1));

		assertEquals(0, d.inputs.size());
		assertEquals(2, d.outputs.size());
		assertSame(or2, d.outputs.get(0));
		assertSame(or4, d.outputs.get(1));
	}
}
