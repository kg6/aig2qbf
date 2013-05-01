package at.jku.aig2qbf.test.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.False;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Latch;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.component.True;
import at.jku.aig2qbf.test.BaseTest;

public class TestAIG extends BaseTest {

	@Before
	public void setUp() throws Exception {
		Component.Reset();
	}

	@Test
	public void empty() {
		Tree tree = this.loadTreeFromFile("input/basic/empty.aig");

		assertNotNull(tree);
		assertTrue(tree.isCNF());

		assertEquals(0, tree.outputs.size());
	}

	@Test
	public void onlytrue() {
		Tree tree = this.loadTreeFromFile("input/basic/true.aig");

		assertNotNull(tree);
		assertTrue(tree.isCNF());

		assertEquals(1, tree.outputs.size());

		// output
		Component o = tree.outputs.get(0);
		assertTrue(o instanceof Output);
		testComponent(o, "0", 1, 0, null);

		// input
		Component i = o.inputs.get(0);
		assertTrue(i instanceof True);
		testComponent(i, null, 0, 1, o);
	}

	@Test
	public void onlyfalse() {
		Tree tree = this.loadTreeFromFile("input/basic/false.aig");

		assertNotNull(tree);

		assertEquals(1, tree.outputs.size());
		assertTrue(tree.isCNF());

		// output
		Component o = tree.outputs.get(0);
		assertTrue(o instanceof Output);
		testComponent(o, "NOT(0)", 1, 0, null);

		// input
		Component i = o.inputs.get(0);
		assertTrue(i instanceof False);
		testComponent(i, null, 0, 1, o);
	}

	@Test
	public void buffer() {
		Tree tree = this.loadTreeFromFile("input/basic/buffer.aig");

		assertNotNull(tree);
		assertTrue(tree.isCNF());

		assertEquals(1, tree.outputs.size());

		// output
		Component o = tree.outputs.get(0);
		assertTrue(o instanceof Output);
		testComponent(o, "1", 1, 0, null);

		// input
		Component i = o.inputs.get(0);
		assertTrue(i instanceof Input);
		testComponent(i, "1", 0, 1, o);
	}

	@Test
	public void inverter() {
		Tree tree = this.loadTreeFromFile("input/basic/inverter.aig");

		assertNotNull(tree);

		assertEquals(1, tree.outputs.size());
		assertTrue(tree.isCNF());

		// output
		Component o = tree.outputs.get(0);
		assertTrue(o instanceof Output);
		testComponent(o, "NOT(1)", 1, 0, null);

		// not before output
		Component nO = o.inputs.get(0);
		assertTrue(nO instanceof Not);
		testComponent(nO, null, 1, 1, o);

		// input
		Component i = nO.inputs.get(0);
		assertTrue(i instanceof Input);
		testComponent(i, "1", 0, 1, nO);
	}

	@Test
	public void and() {
		Tree tree = this.loadTreeFromFile("input/basic/and.aig");

		assertNotNull(tree);

		assertEquals(1, tree.outputs.size());
		assertTrue(tree.isCNF());

		// output
		Component o = tree.outputs.get(0);
		assertTrue(o instanceof Output);
		testComponent(o, "3", 1, 0, null);

		// and
		Component a = o.inputs.get(0);
		assertTrue(a instanceof And);
		testComponent(a, null, 2, 1, o);

		// input 1
		Component i1 = a.inputs.get(0);
		assertTrue(i1 instanceof Input);
		testComponent(i1, "2", 0, 1, a);

		// input 2
		Component i2 = a.inputs.get(1);
		assertTrue(i2 instanceof Input);
		testComponent(i2, "1", 0, 1, a);
	}

	@Test
	public void andWithAllNots() {
		Tree tree = this.loadTreeFromFile("input/basic/and-with-all-nots.aig");

		assertNotNull(tree);
		assertFalse(tree.isCNF());

		assertEquals(1, tree.outputs.size());

		// output
		Component o = tree.outputs.get(0);
		assertTrue(o instanceof Output);
		testComponent(o, "NOT(3)", 1, 0, null);

		// not before output
		Component nO = o.inputs.get(0);
		assertTrue(nO instanceof Not);
		testComponent(nO, null, 1, 1, o);

		// and
		Component a = nO.inputs.get(0);
		assertTrue(a instanceof And);
		testComponent(a, null, 2, 1, nO);

		// not before input 1
		Component nI1 = a.inputs.get(0);
		assertTrue(nI1 instanceof Not);
		testComponent(nI1, null, 1, 1, a);

		// input 1
		Component i1 = nI1.inputs.get(0);
		assertTrue(i1 instanceof Input);
		testComponent(i1, "2", 0, 1, nI1);

		// not before input 2
		Component nI2 = a.inputs.get(1);
		assertTrue(nI2 instanceof Not);
		testComponent(nI2, null, 1, 1, a);

		// input 2
		Component i2 = nI2.inputs.get(0);
		assertTrue(i2 instanceof Input);
		testComponent(i2, "1", 0, 1, nI2);
	}

	@Test
	public void andWithTrueAndFalse() {
		Tree tree = this.loadTreeFromFile("input/basic/and-with-true-and-false.aig");

		assertNotNull(tree);
		assertTrue(tree.isCNF());

		assertEquals(1, tree.outputs.size());

		// output
		Component o = tree.outputs.get(0);
		assertTrue(o instanceof Output);
		testComponent(o, "1", 1, 0, null);

		// and
		Component a = o.inputs.get(0);
		assertTrue(a instanceof And);
		testComponent(a, null, 2, 1, o);

		// input 1
		Component i1 = a.inputs.get(0);
		assertTrue(i1 instanceof True);
		testComponent(i1, null, 0, 1, a);

		// input 2
		Component i2 = a.inputs.get(1);
		assertTrue(i2 instanceof False);
		testComponent(i2, null, 0, 1, a);
	}

	@Test
	public void toggle() {
		Tree tree = this.loadTreeFromFile("input/basic/toggle.aig");

		assertNotNull(tree);
		assertFalse(tree.isCNF());

		assertEquals(2, tree.outputs.size());

		// output 1 (Q)
		Component o1 = tree.outputs.get(0);
		assertTrue(o1 instanceof Output);
		testComponent(o1, "1", 1, 0, null);

		// output 2 (!  Q)
		Component o2 = tree.outputs.get(1);
		assertTrue(o2 instanceof Output);
		testComponent(o2, "NOT(1)", 1, 0, null);

		// latch
		Component l = o1.inputs.get(0);
		assertTrue(l instanceof Latch);

		// not before o2
		Component n = o2.inputs.get(0);
		assertTrue(n instanceof Not);
		testComponent(n, null, 1, 2, l);

		assertEquals(l, n.inputs.get(0));
		assertEquals(o2, n.outputs.get(1));

		// latch
		testComponent(l, null, 1, 2, n);

		assertEquals(n, l.inputs.get(0));
		assertEquals(o1, l.outputs.get(1));
	}

	@Test
	public void toggleRE() {
		Tree tree = this.loadTreeFromFile("input/basic/toggle-re.aig");

		assertNotNull(tree);
		assertFalse(tree.isCNF());

		assertEquals(2, tree.outputs.size());

		// output 1 (Q)
		Component o1 = tree.outputs.get(0);
		assertTrue(o1 instanceof Output);
		testComponent(o1, "3", 1, 0, null);

		// output 2 (!  Q)
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
		Component a10 = a8.inputs.get(0);
		assertTrue(a10 instanceof And);
		testComponent(a10, null, 2, 1, a8);

		// a8i2 i4
		Component i4 = a8.inputs.get(1);
		assertTrue(i4 instanceof Input);
		testComponent(i4, "2", 0, 1, a8);

		// a12not
		Component a12not = a10.inputs.get(1);
		assertTrue(a12not instanceof Not);
		testComponent(a12not, null, 1, 1, a10);

		// a12
		Component a12 = a12not.inputs.get(0);
		assertTrue(a12 instanceof And);
		testComponent(a12, null, 2, 1, a12not);

		// a12i1
		assertEquals(l, a12.inputs.get(0));

		// a12i2 i2
		Component i2 = a12.inputs.get(1);
		assertTrue(i2 instanceof Input);
		testComponent(i2, "1", 0, 2, a12);

		// a14not
		Component a14not = a10.inputs.get(0);
		assertTrue(a14not instanceof Not);
		testComponent(a14not, null, 1, 1, a10);

		// a14
		Component a14 = a14not.inputs.get(0);
		assertTrue(a14 instanceof And);
		testComponent(a14, null, 2, 1, a14not);

		// not before o2
		Component n1 = o2.inputs.get(0);
		assertTrue(n1 instanceof Not);
		assertSame(n1, a14.inputs.get(0));
		testComponent(n1, null, 1, 2, a14);

		assertEquals(l, n1.inputs.get(0));
		assertEquals(o2, n1.outputs.get(1));

		// a14i2not
		Component a14i2not = a14.inputs.get(1);
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
	public void loadRegressions() {
		File[] inputFiles = this.getBenchmarkInputFiles("input/basic", new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("regression");
			}
		});
		
		for(File file : inputFiles) {
			this.loadTreeFromFile(file.getAbsolutePath());
		}
	}
}
