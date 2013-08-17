package at.jku.aig2qbf.test.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Latch;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.test.BaseTest;

public class TestAAG extends BaseTest {

	@Before
	public void setUp() throws Exception {
		Component.Reset();
	}

	@Test
	public void toggleRE() {
		Tree tree = this.loadTreeFromFile("input/basic/toggle-re.aag");

		assertNotNull(tree);
		assertFalse(tree.isCNF());

		assertEquals(2, tree.outputs.size());

		// output 1 (Q)
		Component o1 = tree.outputs.get(0);
		assertTrue(o1 instanceof Output);
		testComponent(o1, "3", 1, 0, null);

		// output 2 (! Q)
		Component o2 = tree.outputs.get(1);
		assertTrue(o2 instanceof Output);
		testComponent(o2, "NOT(3)", 1, 0, null);

		// latch
		Component l = o1.inputs.get(0);
		assertTrue(l instanceof Latch);

		// a8 and latch input
		Component a8 = l.inputs.get(0);
		assertTrue(a8 instanceof And);
		testComponent(a8, null, 2, 1, l);

		// a8i1 a10
		Component a10 = a8.inputs.get(1);
		assertTrue(a10 instanceof And);
		testComponent(a10, null, 2, 1, a8);

		// a8i2 i4
		Component i4 = a8.inputs.get(0);
		assertTrue(i4 instanceof Input);
		testComponent(i4, "2", 0, 1, a8);

		// a12not
		Component a12not = a10.inputs.get(0);
		assertTrue(a12not instanceof Not);
		testComponent(a12not, null, 1, 1, a10);

		// a12
		Component a12 = a12not.inputs.get(0);
		assertTrue(a12 instanceof And);
		testComponent(a12, null, 2, 1, a12not);

		// a12i1
		assertEquals(l, a12.inputs.get(1));

		// a12i2 i2
		Component i2 = a12.inputs.get(0);
		assertTrue(i2 instanceof Input);
		testComponent(i2, "1", 0, 2, a12);

		// a14not
		Component a14not = a10.inputs.get(1);
		assertTrue(a14not instanceof Not);
		testComponent(a14not, null, 1, 1, a10);

		// a14
		Component a14 = a14not.inputs.get(0);
		assertTrue(a14 instanceof And);
		testComponent(a14, null, 2, 1, a14not);

		// not before o2
		Component n1 = o2.inputs.get(0);
		assertTrue(n1 instanceof Not);
		assertSame(n1, a14.inputs.get(1));
		testComponent(n1, null, 1, 2, a14);

		assertEquals(l, n1.inputs.get(0));
		assertEquals(o2, n1.outputs.get(1));

		// a14i2not
		Component a14i2not = a14.inputs.get(0);
		assertTrue(a14i2not instanceof Not);
		testComponent(a14i2not, null, 1, 1, a14);

		// a14i2
		assertEquals(i2, a14i2not.inputs.get(0));

		// latch tests
		testComponent(l, null, 1, 3, a12);

		assertEquals(a8, l.inputs.get(0));

		assertEquals(n1, l.outputs.get(1));
		assertEquals(o1, l.outputs.get(2));
	}

	@Test
	public void slicing() {
		Tree tree = this.loadTreeFromFile("input/basic/slicing.aag");

		assertNotNull(tree);

		assertEquals(1, tree.outputs.size());

		Component o = tree.outputs.get(0);
		assertTrue(o instanceof Output);
		testComponent(o, "3", 1, 0, null);

		Component a = o.inputs.get(0);
		assertTrue(a instanceof And);
		testComponent(a, null, 2, 1, o);

		Component x = a.inputs.get(0);
		assertTrue(x instanceof Input);
		testComponent(x, "1", 0, 1, a);

		Component y = a.inputs.get(1);
		assertTrue(y instanceof Input);
		testComponent(y, "2", 0, 1, a);
	}
}
