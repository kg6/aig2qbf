package at.jku.aig2qbf.test.reduction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Latch;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.parser.AAG;
import at.jku.aig2qbf.reduction.SimplePathReduction;
import at.jku.aig2qbf.test.TestUtil;

public class TestSimplePathReduction {
	private final String TEMP_QDIMACS_FILE = "./output/temp.qbf";

	@Before
	public void setUp() throws Exception {
		Component.Reset();
	}

	@After
	public void tearDown() {
		TestUtil.RemoveOutputFile(TEMP_QDIMACS_FILE);
	}

	@Test
	public void SATnot() {
		{ // 0
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			Not not = new Not();
			o.addInput(not);
			not.addInput(t.cFalse);
			
			t = t.toTseitinCNF();
			
			assertTrue(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}

		{ // 1
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			Not not = new Not();
			o.addInput(not);
			not.addInput(t.cTrue);
			
			t = t.toTseitinCNF();
			
			assertFalse(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}

	}

	@Test
	public void SATor() {
		{ // 00
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			Or or = new Or();
			o.addInput(or);
			or.addInput(t.cFalse);
			or.addInput(t.cFalse);
			
			t = t.toTseitinCNF();
			
			assertFalse(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}
		{ // 10
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			Or or = new Or();
			o.addInput(or);
			or.addInput(t.cTrue);
			or.addInput(t.cFalse);
			
			t = t.toTseitinCNF();
			
			assertTrue(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}
		{ // 01
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			Or or = new Or();
			o.addInput(or);
			or.addInput(t.cFalse);
			or.addInput(t.cTrue);
			
			t = t.toTseitinCNF();
			
			assertTrue(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}
		{ // 11
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			Or or = new Or();
			o.addInput(or);
			or.addInput(t.cTrue);
			or.addInput(t.cTrue);
			
			t = t.toTseitinCNF();
			
			assertTrue(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}
	}

	@Test
	public void SATand() {
		{ // 00
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			And and = new And();
			o.addInput(and);
			and.addInput(t.cFalse);
			and.addInput(t.cFalse);
			
			t = t.toTseitinCNF();
			
			assertFalse(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}
		{ // 10
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			And and = new And();
			o.addInput(and);
			and.addInput(t.cTrue);
			and.addInput(t.cFalse);
			
			t = t.toTseitinCNF();
			
			assertFalse(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}
		{ // 01
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			And and = new And();
			o.addInput(and);
			and.addInput(t.cFalse);
			and.addInput(t.cTrue);
			
			t = t.toTseitinCNF();
			
			assertFalse(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}
		{ // 11
			Tree t = new Tree();
			Output o = new Output("a");
			t.outputs.add(o);
			And and = new And();
			o.addInput(and);
			and.addInput(t.cTrue);
			and.addInput(t.cTrue);
			
			t = t.toTseitinCNF();
			
			assertTrue(TestUtil.CheckSatisfiablity(t, TEMP_QDIMACS_FILE));
		}
	}

	@Test
	public void testSat0() {
		List<String> inputFiles = new ArrayList<String>();
		inputFiles.add("input/basic/meetingsample1.aag");

		final int max_k = 10;

		for (String inputFilePath : inputFiles) {
			Tree tree = new AAG().parse(inputFilePath);

			SimplePathReduction reduction = new SimplePathReduction();

			for (int k = 1; k <= max_k; k++) {
				final long startTime = System.currentTimeMillis();
				
				Tree unrolledTree = tree.unroll(k);
				unrolledTree.mergeToOneOutput();
				
				Tree tseitinTree = unrolledTree.toTseitinCNF();
				Tree reducedTree = reduction.reduceTree(tseitinTree, k);
				
				final boolean sat = TestUtil.CheckSatisfiablity(reducedTree, TEMP_QDIMACS_FILE);

				System.out.println(String.format("testSat0: Test simple path constraint using %s and k=%s (%s%%, %sms)", inputFilePath, k, k * 100 / max_k, System.currentTimeMillis() - startTime));
				
				if (k <= 2) {
					assertEquals(true, sat);
				}
				else {
					assertEquals(false, sat);
				}
			}
		}
	}

	@Test
	public void testSat1() {
		final int max_k = 5;

		// Create a tree with max_k latches in a row

		Component a = new Input("a");
		Output b = new Output("b");

		Component parent = a;

		for (int k = 0; k < max_k; k++) {
			Latch latch = new Latch();
			latch.addInput(parent);

			parent = latch;
		}

		b.addInput(parent);

		Tree tree = new Tree();
		tree.outputs.add(b);

		// It is expected that the tree needs to be unrolled max_k + 1 in order
		// to be sat once

		SimplePathReduction reduction = new SimplePathReduction();

		final int max_check = max_k * 2;

		for (int k = 1; k <= max_check; k++) {
			final long startTime = System.currentTimeMillis();
			
			Tree unrolledTree = tree.unroll(k);
			unrolledTree.mergeToOneOutput();
			
			Tree tseitinTree = unrolledTree.toTseitinCNF();
			Tree reducedTree = reduction.reduceTree(tseitinTree, k);
			
			final boolean sat = TestUtil.CheckSatisfiablity(reducedTree, TEMP_QDIMACS_FILE);

			System.out.println(String.format("testSat1: Test simple path constraint k=%s (%s%%, %sms)", k, k * 100 / max_check, System.currentTimeMillis() - startTime));
			
			if (k <= max_k + 1) {
				assertFalse(sat);
			}
			else {
				assertTrue(sat);
			}
		}
	}
}
