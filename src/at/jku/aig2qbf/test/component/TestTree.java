package at.jku.aig2qbf.test.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.False;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Latch;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.parser.AIG;
import at.jku.aig2qbf.test.TestUtil;

public class TestTree {
	private final String TEMP_QDIMACS_FILE = "./output/temp.txt";
	
	@Before
	public void setUp() throws Exception {
		Component.Reset();
	}
	
	@Test
	public void testClone() {
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
		
		Output o = new Output("y");
		o.addInput(ab_d_abc);
		
		Tree t = new Tree();
		
		t.outputs.add(o);
		
		ab_d_abc.addInput(t.cFalse);
		ab_d_abc.addInput(t.cTrue);
		
		Tree nT = (Tree) t.clone();
		
		assertEquals(1, nT.outputs.size());
		
		Component n_o = nT.outputs.get(0);
		assertTrue(n_o instanceof Output);
		assertEquals("y", n_o.getName());
		assertNotSame(o, n_o);
		assertEquals(1, n_o.inputs.size());
		assertEquals(0, n_o.outputs.size());
		
		Component n_ab_d_abc = n_o.inputs.get(0);
		assertTrue(n_ab_d_abc instanceof And);
		assertNotSame(ab_d_abc, n_ab_d_abc);
		assertEquals(4, n_ab_d_abc.inputs.size());
		Component n_ab_d = n_ab_d_abc.inputs.get(0);
		Component n_abc = n_ab_d_abc.inputs.get(1);
		assertSame(nT.cFalse, n_ab_d_abc.inputs.get(2));
		assertSame(nT.cTrue, n_ab_d_abc.inputs.get(3));
		assertEquals(1, n_ab_d_abc.outputs.size());
		assertSame(n_o, n_ab_d_abc.outputs.get(0));
		
		assertTrue(n_ab_d instanceof And);
		assertNotSame(ab_d, n_ab_d);
		assertEquals(2, n_ab_d.inputs.size());
		Component n_ab = n_ab_d.inputs.get(0);
		Component n_d = n_ab_d.inputs.get(1);
		assertEquals(1, n_ab_d.outputs.size());
		assertSame(n_ab_d_abc, n_ab_d.outputs.get(0));
		
		assertTrue(n_d instanceof Input);
		assertEquals("d", n_d.getName());
		assertEquals(0, n_d.inputs.size());
		assertEquals(1, n_d.outputs.size());
		assertSame(n_ab_d, n_d.outputs.get(0));
		
		assertTrue(n_abc instanceof And);
		assertNotSame(abc, n_abc);
		assertEquals(3, n_abc.inputs.size());
		Component n_a = n_abc.inputs.get(0);
		Component n_b = n_abc.inputs.get(1);
		Component n_c = n_abc.inputs.get(2);
		assertEquals(1, n_abc.outputs.size());
		assertSame(n_ab_d_abc, n_abc.outputs.get(0));
		
		assertTrue(n_ab instanceof And);
		assertNotSame(ab, n_ab);
		assertEquals(2, n_ab.inputs.size());
		assertSame(n_a, n_ab.inputs.get(0));
		assertSame(n_b, n_ab.inputs.get(1));
		assertEquals(1, n_ab.outputs.size());
		assertSame(n_ab_d, n_ab.outputs.get(0));
		
		assertTrue(n_a instanceof Input);
		assertEquals("a", n_a.getName());
		assertEquals(0, n_a.inputs.size());
		assertEquals(2, n_a.outputs.size());
		assertSame(n_ab, n_a.outputs.get(0));
		assertSame(n_abc, n_a.outputs.get(1));
		
		assertTrue(n_b instanceof Input);
		assertEquals("b", n_b.getName());
		assertEquals(0, n_b.inputs.size());
		assertEquals(2, n_b.outputs.size());
		assertSame(n_ab, n_b.outputs.get(0));
		assertSame(n_abc, n_b.outputs.get(1));
		
		assertTrue(n_c instanceof Input);
		assertEquals("c", n_c.getName());
		assertEquals(0, n_c.inputs.size());
		assertEquals(1, n_c.outputs.size());
		assertSame(n_abc, n_c.outputs.get(0));
	}
	
	@Test
	public void isCFNBasic() {
		Component a = new Input("a");
		Component b = new Input("b");
		Component c = new Input("c");
		Component d = new Input("d");
		Component e = new Input("e");
		Component f = new Input("f");
		
		Or ab = new Or();
		ab.addInput(a);
		ab.addInput(b);
		
		Or cd = new Or();
		cd.addInput(c);
		cd.addInput(d);
		
		And abcd = new And();
		abcd.addInput(ab);
		abcd.addInput(cd);
		abcd.addInput(e);
		abcd.addInput(f.addNot());
		
		Output o = new Output("y");
		o.addInput(abcd);
		
		Tree t = new Tree();
		t.outputs.add(o);
		
		assertTrue(t.isCNF());
	}
	
	@Test
	public void testTseitinCNFConversionAnd0() {
		Component a = new Input("a");
		Component b = new Input("b");
		Output y = new Output("y");
		
		Component andAB = new And();
		andAB.addInput(a);
		andAB.addInput(b);
		
		y.addInput(andAB);
		
		Tree tree = new Tree();
		tree.outputs.add(y);
		
		Tree tseitinTree = tree.toTseitinCNF();
		assertEquals(1, tseitinTree.outputs.size());
		
		Output output = tseitinTree.outputs.get(0);
		assertEquals(1, output.inputs.size());
		
		Component outputAnd = output.inputs.get(0);
		assertTrue(outputAnd instanceof And);
		assertEquals(4, outputAnd.inputs.size());
		
		Component outputOr0 = outputAnd.inputs.get(0);
		assertTrue(outputOr0 instanceof Or);
		assertEquals(2, outputOr0.inputs.size());
		assertTrue(outputOr0.inputs.get(0) instanceof Not);
		assertTrue(outputOr0.inputs.get(0).inputs.get(0) instanceof Input);
		assertSame(a.getName(), outputOr0.inputs.get(1).getName());
		
		Component outputOr1 = outputAnd.inputs.get(1);
		assertTrue(outputOr1 instanceof Or);
		assertEquals(2, outputOr1.inputs.size());
		assertTrue(outputOr1.inputs.get(0) instanceof Not);
		assertTrue(outputOr1.inputs.get(0).inputs.get(0) instanceof Input);
		assertSame(b.getName(), outputOr1.inputs.get(1).getName());
		
		Component outputOr2 = outputAnd.inputs.get(2);
		assertTrue(outputOr2 instanceof Or);
		assertEquals(3, outputOr2.inputs.size());
		
		Component input = outputAnd.inputs.get(3);
		assertTrue(input instanceof Input);
		
		Component outputNot1 = outputOr2.inputs.get(1);
		assertTrue(outputNot1 instanceof Not);
		assertEquals(1, outputNot1.inputs.size());
		
		Component outputNot2 = outputOr2.inputs.get(2);
		assertTrue(outputNot2 instanceof Not);
		assertEquals(1, outputNot2.inputs.size());
		
		assertSame(a.getName(), outputNot1.inputs.get(0).getName());
		assertSame(b.getName(), outputNot2.inputs.get(0).getName());
	}
	
	@Test
	public void testTseitinCNFConversionNot0() {
		Component a = new Input("a");
		Output y = new Output("y");
		
		Component notA = new Not();
		notA.addInput(a);
		
		y.addInput(notA);
		
		Tree tree = new Tree();
		tree.outputs.add(y);
		
		final Tree tseitinTree = tree.toTseitinCNF();
		assertEquals(1, tseitinTree.outputs.size());
		
		Output output = tseitinTree.outputs.get(0);
		assertEquals(1, output.inputs.size());
		
		Component outputAnd = output.inputs.get(0);
		assertTrue(outputAnd instanceof And);
		assertEquals(3, outputAnd.inputs.size());
		
		Component outputOr0 = outputAnd.inputs.get(0);
		assertTrue(outputOr0 instanceof Or);
		assertEquals(2, outputOr0.inputs.size());
		
		Component outputOr1 = outputAnd.inputs.get(1);
		assertTrue(outputOr1 instanceof Or);
		assertEquals(2, outputOr1.inputs.size());
		assertTrue(outputOr1.inputs.get(0) instanceof Input);
		
		Component input0 = outputAnd.inputs.get(2);
		assertTrue(input0 instanceof Input);
		
		Component outputNotX0 = outputOr0.inputs.get(0);
		assertTrue(outputNotX0 instanceof Not);
		assertEquals(1, outputNotX0.inputs.size());
		assertTrue(outputNotX0.inputs.get(0) instanceof Input);
		
		Component outputNotX1 = outputOr0.inputs.get(1);
		assertTrue(outputNotX1 instanceof Not);
		assertEquals(1, outputNotX1.inputs.size());
		assertTrue(outputNotX1.inputs.get(0) instanceof Input);
		
		Component input1 = outputNotX1.inputs.get(0);
		assertEquals(0, input1.inputs.size());
		assertSame(a.getName(), input1.getName());
	}
	
	@Test
	public void testTseitinCNFConversionOr0() {
		Component a = new Input("a");
		Component b = new Input("b");
		Output y = new Output("y");
		
		Component orAB = new Or();
		orAB.addInput(a);
		orAB.addInput(b);
		
		y.addInput(orAB);
		
		Tree tree = new Tree();
		tree.outputs.add(y);
		
		Tree tseitinTree = tree.toTseitinCNF();
		assertEquals(1, tseitinTree.outputs.size());
		
		Output output = tseitinTree.outputs.get(0);
		assertEquals(1, output.inputs.size());
		
		Component outputAnd = output.inputs.get(0);
		assertTrue(outputAnd instanceof And);
		assertEquals(4, outputAnd.inputs.size());
		
		Component outputOr0 = outputAnd.inputs.get(0);
		assertTrue(outputOr0 instanceof Or);
		assertEquals(2, outputOr0.inputs.size());
		assertTrue(outputOr0.inputs.get(0) instanceof Input);
		
		Component outputNotA = outputOr0.inputs.get(1);
		assertTrue(outputNotA instanceof Not);
		assertEquals(1, outputNotA.inputs.size());
		assertSame(a.getName(), outputNotA.inputs.get(0).getName());
		
		Component outputOr1 = outputAnd.inputs.get(1);
		assertTrue(outputOr1 instanceof Or);
		assertEquals(2, outputOr1.inputs.size());
		assertTrue(outputOr1.inputs.get(0) instanceof Input);
		
		Component outputNotB = outputOr1.inputs.get(1);
		assertTrue(outputNotB instanceof Not);
		assertEquals(1, outputNotB.inputs.size());
		assertSame(b.getName(), outputNotB.inputs.get(0).getName());		
		
		Component outputOr2 = outputAnd.inputs.get(2);
		assertTrue(outputOr2 instanceof Or);
		assertEquals(3, outputOr2.inputs.size());
		assertSame(a.getName(), outputOr2.inputs.get(1).getName());
		assertSame(b.getName(), outputOr2.inputs.get(2).getName());
		
		Component outputNotX = outputOr2.inputs.get(0);
		assertEquals(1, outputNotX.inputs.size());
		assertTrue(outputNotX.inputs.get(0) instanceof Input);
		
		Component input = outputAnd.inputs.get(3);
		assertTrue(input instanceof Input);
	}
	
	@Test
	public void testTseitinCNFConversion0() {
		Component a = new Input("a");
		Component b = new Input("b");
		Component c = new Input("c");
		Output y = new Output("y");
		
		Component andAB = new And();
		andAB.addInput(a);
		andAB.addInput(b);
		
		Component orABC = new Or();
		orABC.addInput(andAB);
		orABC.addInput(c);
		
		y.addInput(orABC);
		
		Tree tree = new Tree();
		tree.outputs.add(y);
		
		Tree tseitinTree = tree.toTseitinCNF();
		assertEquals(1, tseitinTree.outputs.size());
		
		Output output = tseitinTree.outputs.get(0);
		assertEquals(1, output.inputs.size());
		assertTrue(output instanceof Output);
		assertSame(y.getName(), output.getName());
		
		//Global AND
		Component outputAnd = output.inputs.get(0);
		assertEquals(7, outputAnd.inputs.size());
		assertTrue(outputAnd instanceof And);
		
		//OR 0
		Component outputOr0 = outputAnd.inputs.get(0);
		assertEquals(2, outputOr0.inputs.size());
		assertTrue(outputOr0 instanceof Or);
		
		//OR 1
		Component outputOr1 = outputAnd.inputs.get(1);
		assertEquals(2, outputOr1.inputs.size());
		assertTrue(outputOr1 instanceof Or);

		//OR 2
		Component outputOr2 = outputAnd.inputs.get(2);
		assertEquals(3, outputOr2.inputs.size());
		assertTrue(outputOr2 instanceof Or);	
		
		//OR 3
		Component outputOr3 = outputAnd.inputs.get(3);
		assertEquals(2, outputOr3.inputs.size());
		assertTrue(outputOr3 instanceof Or);
		
		//OR 4
		Component outputOr4 = outputAnd.inputs.get(4);
		assertEquals(2, outputOr4.inputs.size());
		assertTrue(outputOr4 instanceof Or);
		
		//OR 5
		Component outputOr5 = outputAnd.inputs.get(5);
		assertEquals(3, outputOr5.inputs.size());
		assertTrue(outputOr5 instanceof Or);
		
		//Input
		Component input = outputAnd.inputs.get(6);
		assertTrue(input instanceof Input);
		
		//Not 0
		Component outputNot0 = outputOr0.inputs.get(0);
		assertEquals(1, outputNot0.inputs.size());
		assertTrue(outputNot0 instanceof Not);
		
		//Not 1
		Component outputNot1 = outputOr2.inputs.get(1);
		assertEquals(1, outputNot1.inputs.size());
		assertTrue(outputNot1 instanceof Not);
		
		//Not 2
		Component outputNot2 = outputOr2.inputs.get(2);
		assertEquals(1, outputNot2.inputs.size());
		assertTrue(outputNot2 instanceof Not);
		
		//Not 3
		Component outputNot3 = outputOr3.inputs.get(1);
		assertEquals(1, outputNot3.inputs.size());
		assertTrue(outputNot3 instanceof Not);
		
		//Not 4
		Component outputNot4 = outputOr4.inputs.get(1);
		assertEquals(1, outputNot4.inputs.size());
		assertTrue(outputNot4 instanceof Not);
		
		//Not 5
		Component outputNot5 = outputOr5.inputs.get(0);
		assertEquals(1, outputNot5.inputs.size());
		assertTrue(outputNot5 instanceof Not);
		
		//a
		Component outputA = outputOr0.inputs.get(1);
		assertTrue(outputA instanceof Input);
		assertSame(outputA, outputNot1.inputs.get(0));
		
		//b
		Component outputB = outputOr1.inputs.get(1);
		assertTrue(outputB instanceof Input);
		assertSame(outputB, outputNot2.inputs.get(0));
		
		//c
		Component outputC = outputOr5.inputs.get(2);
		assertTrue(outputC instanceof Input);
		assertSame(outputC, outputNot4.inputs.get(0));
		
		//x0
		Component outputX0 = outputNot0.inputs.get(0);
		assertTrue(outputX0 instanceof Input);
		assertEquals(outputX0, outputOr2.inputs.get(0));
		assertEquals(outputX0, outputOr5.inputs.get(2));
		
		//x1
		Component outputX1 = outputOr3.inputs.get(0);
		assertTrue(outputX1 instanceof Input);
		assertEquals(outputX1, outputNot5.inputs.get(0));
		assertEquals(outputX1, outputOr4.inputs.get(0));
	}
	
	@Test
	public void testTseiting0() {
		final int max_k = 30;
		
		boolean prevSat = true;
		long time;
		
		for(int k = 1; k <= max_k; k++) {
			time = System.currentTimeMillis();
			
			//Build a tree with a growing number of NOT's
			Input x = new Input("x");
			Component parent = x;
			
			for(int i = 0; i < k; i++) {
				Component not = new Not();
				not.addInput(parent);
				
				parent = not;
			}
			
			Component and1 = new And();
			and1.addInput(x);
			and1.addInput(parent);
			
			Component and2 = new And();
			and2.addInput(x);
			and2.addInput(parent);
			
			Component or = new Or();
			or.addInput(and1);
			or.addInput(and2);
			
			Output o = new Output("y");
			o.addInput(or);
			
			Tree tree = new Tree();
			tree.outputs.add(o);
			
			//Check the satisfiability of the tree
			boolean sat = TestUtil.CheckSatisfiablity(tree, TEMP_QDIMACS_FILE);
			
			assertTrue(prevSat != sat);
			
			prevSat = sat;
			
			System.out.println(String.format("testTseiting0: Test Tseitin conversion using %s * NOT (%s%%, %sms)", k, k * 100 / max_k, System.currentTimeMillis() - time));
		}
	}
	
	@Test
	public void testTreeUnrolling() {
		Component a = new Input("a");
		Component b = new Input("b");
		Output y = new Output("y");
		
		//Test: y = a OR NOT(b AND q), q = output from latch
		
		Component and = new And();
		Component latch = new Latch();
		
		and.addInput(b);
		and.addInput(latch);
		latch.addInput(and);
		
		Component not = new Not();
		not.addInput(and);
		
		Component or = new Or();
		or.addInput(a);
		or.addInput(not);
		
		y.addInput(or);
		
		Tree tree = new Tree();
		tree.outputs.add(y);
		
		final int k = 4;
		Tree unrolledTree = tree.unroll(k);
		unrolledTree.mergeToOneOutput();
		
		Component unrolledOutput = unrolledTree.outputs.get(0);
		assertEquals(1, unrolledOutput.inputs.size());
		assertTrue(unrolledOutput instanceof Output);
		
		Component unrolledGlobalOr = unrolledOutput.inputs.get(0);
		assertEquals(k, unrolledGlobalOr.inputs.size());
		assertTrue(unrolledGlobalOr instanceof Or);
		
		//Check different branches (unroll step i)
		for(int i = 0; i < unrolledGlobalOr.inputs.size(); i++) {
			Component unrolledOr0 = unrolledGlobalOr.inputs.get(i);
			assertEquals(2, unrolledOr0.inputs.size());
			assertTrue(unrolledOr0 instanceof Or);
			assertEquals(a.getName(), unrolledOr0.inputs.get(0).getName());
			
			Component unrolledNot0 = unrolledOr0.inputs.get(1);
			assertEquals(1, unrolledNot0.inputs.size());
			assertTrue(unrolledNot0 instanceof Not);
			
			Component unrolledAnd0 = unrolledNot0.inputs.get(0);
			assertEquals(2, unrolledAnd0.inputs.size());
			assertTrue(unrolledAnd0 instanceof And);
			assertEquals(b.getName(), unrolledAnd0.inputs.get(0).getName());
			
			if(i == 0) {
				assertTrue((unrolledAnd0.inputs.get(1) instanceof False));
			} else {
				assertTrue((unrolledAnd0.inputs.get(1) instanceof And));
			}
		}
	}
	
	@Test
	public void testNewUnrollToggleK1() {
		Tree t = new AIG().parse("input/basic/toggle.aig");
		
		Tree uT = t.unroll(1);
		
		assertEquals(2, uT.outputs.size());
		Component o1 = uT.outputs.get(0);
		Component o2 = uT.outputs.get(1);

		assertEquals(0, o1.outputs.size());
		assertEquals(1, o1.inputs.size());
		assertEquals(uT.cFalse, o1.inputs.get(0));
		
		assertEquals(0, o2.outputs.size());
		assertEquals(1, o2.inputs.size());
		Component n = o2.inputs.get(0);
		
		assertTrue(n instanceof Not);
		assertEquals(1, n.outputs.size());
		assertEquals(o2, n.outputs.get(0));
		assertEquals(1, n.inputs.size());
		assertEquals(uT.cFalse, n.inputs.get(0));
	}
	
	@Test
	public void testNewUnrollToggleK2() {
		Tree t = new AIG().parse("input/basic/toggle.aig");
		
		Tree uT = t.unroll(2);

		assertEquals(4, uT.outputs.size());
		Component o1 = uT.outputs.get(0);
		Component o2 = uT.outputs.get(1);
		Component o3 = uT.outputs.get(2);
		Component o4 = uT.outputs.get(3);

		assertEquals(0, o1.outputs.size());
		assertEquals(1, o1.inputs.size());
		assertEquals(uT.cFalse, o1.inputs.get(0));
		
		assertEquals(0, o2.outputs.size());
		assertEquals(1, o2.inputs.size());
		Component n1 = o2.inputs.get(0);
		
		assertTrue(n1 instanceof Not);
		assertEquals(3, n1.outputs.size());
		assertEquals(o2, n1.outputs.get(0));
		Component n2 = n1.outputs.get(1);
		assertEquals(o3, n1.outputs.get(2));
		assertEquals(1, n1.inputs.size());
		assertEquals(uT.cFalse, n1.inputs.get(0));
		
		assertEquals(0, o3.outputs.size());
		assertEquals(1, o3.inputs.size());
		assertEquals(n1, o3.inputs.get(0));
		
		assertTrue(n2 instanceof Not);
		assertEquals(1, n2.outputs.size());
		assertEquals(o4, n2.outputs.get(0));
		assertEquals(1, n2.inputs.size());
		assertEquals(n1, n2.inputs.get(0));
		
		assertEquals(0, o4.outputs.size());
		assertEquals(1, o4.inputs.size());
		assertEquals(n2, o4.inputs.get(0));
	}
	
	@Test
	public void testNewUnrollToggleK3() {
		Tree t = new AIG().parse("input/basic/toggle.aig");
		
		Tree uT = t.unroll(3);
		
		assertEquals(6, uT.outputs.size());
		Component o1 = uT.outputs.get(0);
		Component o2 = uT.outputs.get(1);
		Component o3 = uT.outputs.get(2);
		Component o4 = uT.outputs.get(3);
		Component o5 = uT.outputs.get(4);
		Component o6 = uT.outputs.get(5);

		assertEquals(0, o1.outputs.size());
		assertEquals(1, o1.inputs.size());
		assertEquals(uT.cFalse, o1.inputs.get(0));
		
		assertEquals(0, o2.outputs.size());
		assertEquals(1, o2.inputs.size());
		Component n1 = o2.inputs.get(0);
		
		assertTrue(n1 instanceof Not);
		assertEquals(3, n1.outputs.size());
		assertEquals(o2, n1.outputs.get(0));
		Component n2 = n1.outputs.get(1);
		assertEquals(o3, n1.outputs.get(2));
		assertEquals(1, n1.inputs.size());
		assertEquals(uT.cFalse, n1.inputs.get(0));
		
		assertEquals(0, o3.outputs.size());
		assertEquals(1, o3.inputs.size());
		assertEquals(n1, o3.inputs.get(0));
		
		assertTrue(n2 instanceof Not);
		assertEquals(3, n2.outputs.size());
		assertEquals(o4, n2.outputs.get(0));
		Component n3 = n2.outputs.get(1);
		assertEquals(o5, n2.outputs.get(2));
		assertEquals(1, n2.inputs.size());
		assertEquals(n1, n2.inputs.get(0));
		
		assertEquals(0, o4.outputs.size());
		assertEquals(1, o4.inputs.size());
		assertEquals(n2, o4.inputs.get(0));
		
		assertEquals(0, o5.outputs.size());
		assertEquals(1, o5.inputs.size());
		assertEquals(n2, o5.inputs.get(0));
		
		assertTrue(n3 instanceof Not);
		assertEquals(1, n3.outputs.size());
		assertEquals(o6, n3.outputs.get(0));
		assertEquals(1, n3.inputs.size());
		assertEquals(n2, n3.inputs.get(0));
		
		assertEquals(0, o6.outputs.size());
		assertEquals(1, o6.inputs.size());
		assertEquals(n3, o6.inputs.get(0));
	}
	
	@Test
	public void testNewUnrollToggleK4() {
		Tree t = new AIG().parse("input/basic/toggle.aig");
		
		Tree uT = t.unroll(4);
		
		assertEquals(8, uT.outputs.size());
		
		//TreeVisualizer.DisplayTree(uT, "HELLO");
		//int a = 0;
	}
	
	@Test
	public void testNewUnrollToggleReK1() {
		Tree t = new AIG().parse("input/basic/toggle-re.aig");
		
		Tree uT = t.unroll(1);
		
		assertEquals(2, uT.outputs.size());
		Component o1 = uT.outputs.get(0);
		Component o2 = uT.outputs.get(1);

		assertEquals(0, o1.outputs.size());
		assertEquals(1, o1.inputs.size());
		assertEquals(uT.cFalse, o1.inputs.get(0));
		
		assertEquals(0, o2.outputs.size());
		assertEquals(1, o2.inputs.size());
		Component n = o2.inputs.get(0);
		
		assertTrue(n instanceof Not);
		assertEquals(1, n.outputs.size());
		assertEquals(o2, n.outputs.get(0));
		assertEquals(1, n.inputs.size());
		assertEquals(uT.cFalse, n.inputs.get(0));
	}
	
	@Test
	public void testNewUnrollToggleReK2() {
		Tree t = new AIG().parse("input/basic/toggle-re.aig");
		
		Tree uT = t.unroll(2);
		
		assertEquals(4, uT.outputs.size());
		Component o1 = uT.outputs.get(0);
		Component o2 = uT.outputs.get(1);
		Component o3 = uT.outputs.get(2);
		Component o4 = uT.outputs.get(3);

		assertEquals(0, o1.outputs.size());
		assertEquals(1, o1.inputs.size());
		assertEquals(uT.cFalse, o1.inputs.get(0));
		
		assertEquals(0, o2.outputs.size());
		assertEquals(1, o2.inputs.size());
		Component n = o2.inputs.get(0);
		
		assertTrue(n instanceof Not);
		assertEquals(2, n.outputs.size());
		Component andNotI1 = n.outputs.get(0);
		assertEquals(o2, n.outputs.get(1));
		assertEquals(1, n.inputs.size());
		assertEquals(uT.cFalse, n.inputs.get(0));

		assertEquals(0, o3.outputs.size());
		assertEquals(1, o3.inputs.size());
		Component andI2 = o3.inputs.get(0);

		assertEquals(0, o4.outputs.size());
		assertEquals(1, o4.inputs.size());
		Component notAndI2 = o4.inputs.get(0);
		
		// notAndI2
		assertTrue(notAndI2 instanceof Not);
		assertEquals(1, notAndI2.outputs.size());
		assertEquals(o4, notAndI2.outputs.get(0));
		assertEquals(1, notAndI2.inputs.size());
		assertEquals(andI2, notAndI2.inputs.get(0));
		
		// andI2
		assertTrue(andI2 instanceof And);
		assertEquals(2, andI2.outputs.size());
		assertEquals(notAndI2, andI2.outputs.get(0));
		assertEquals(o3, andI2.outputs.get(1));
		assertEquals(2, andI2.inputs.size());
		Component andCombinedI1 = andI2.inputs.get(0);
		Component i2 = andI2.inputs.get(1);
		
		// i2
		assertTrue(i2 instanceof Input);
		assertEquals(1, i2.outputs.size());
		assertEquals(andI2, i2.outputs.get(0));
		assertEquals(0, i2.inputs.size());
		
		// andCombinedI1
		assertTrue(andCombinedI1 instanceof And);
		assertEquals(1, andCombinedI1.outputs.size());
		assertEquals(andI2, andCombinedI1.outputs.get(0));
		assertEquals(2, andCombinedI1.inputs.size());
		Component notaNotI1 = andCombinedI1.inputs.get(0);
		Component notaI1 = andCombinedI1.inputs.get(1);
		
		// notaI1
		assertTrue(notaI1 instanceof Not);
		assertEquals(1, notaI1.outputs.size());
		assertEquals(andCombinedI1, notaI1.outputs.get(0));
		assertEquals(1, notaI1.inputs.size());
		Component andI1 = notaI1.inputs.get(0);
		
		// notaNotI1
		assertTrue(notaNotI1 instanceof Not);
		assertEquals(1, notaNotI1.outputs.size());
		assertEquals(andCombinedI1, notaNotI1.outputs.get(0));
		assertEquals(1, notaNotI1.inputs.size());
		assertEquals(andNotI1, notaNotI1.inputs.get(0));
		
		// andI1
		assertTrue(andI1 instanceof And);
		assertEquals(1, andI1.outputs.size());
		assertEquals(notaI1, andI1.outputs.get(0));
		assertEquals(2, andI1.inputs.size());
		Component i1 = andI1.inputs.get(0);
		assertEquals(uT.cFalse, andI1.inputs.get(1));
		
		// andNotI1
		assertTrue(andNotI1 instanceof And);
		assertEquals(1, andNotI1.outputs.size());
		assertEquals(notaNotI1, andNotI1.outputs.get(0));
		assertEquals(2, andNotI1.inputs.size());
		assertEquals(n, andNotI1.inputs.get(0));
		Component notI1 = andNotI1.inputs.get(1);
		
		// notI1
		assertTrue(notI1 instanceof Not);
		assertEquals(1, notI1.outputs.size());
		assertEquals(andNotI1, notI1.outputs.get(0));
		assertEquals(1, notI1.inputs.size());
		assertEquals(i1, notI1.inputs.get(0));
		
		// i1
		assertTrue(i1 instanceof Input);
		assertEquals(2, i1.outputs.size());
		assertEquals(andI1, i1.outputs.get(0));
		assertEquals(notI1, i1.outputs.get(1));
		assertEquals(0, i1.inputs.size());
	}
	
	@Test
	public void testNewUnrollToggleReK3() {
		Tree t = new AIG().parse("input/basic/toggle-re.aig");
		
		Tree uT = t.unroll(3);
		
		assertEquals(6, uT.outputs.size());
	}
	
	@Test
	public void testSimpleLatchUnrolling() {
		Component a = new Input("a");
		
		Component latch = new Latch();
		latch.addInput(latch);
		
		Component and = new And();
		and.addInput(a);
		and.addInput(latch);
		
		Output output = new Output("Y");
		output.addInput(and);
		
		Tree tree = new Tree();
		tree.outputs.add(output);
		
		for(int k = 1; k < 10; k++) {
			Tree unrolledTree = tree.unroll(k);
			
			assertEquals(0, unrolledTree.cFalse.inputs.size());
			assertEquals(k, unrolledTree.cFalse.outputs.size());
			
			assertEquals(0, unrolledTree.cTrue.inputs.size());
			assertEquals(0, unrolledTree.cTrue.outputs.size());
		}
	}
	
	@Test
	public void testMergeToOneOutput() {
		Tree t = new Tree();
		
		Output o1 = new Output("o1");
		Input i1 = new Input("i1");
		o1.addInput(i1);
		t.outputs.add(o1);
		
		Output o2 = new Output("o2");
		Input i2 = new Input("i2");
		o2.addInput(i2);
		t.outputs.add(o2);
		
		t.mergeToOneOutput();
		
		assertEquals(1, t.outputs.size());
		Component o = t.outputs.get(0);

		assertEquals(0, o.outputs.size());
		assertEquals(1, o.inputs.size());
		Component or = o.inputs.get(0);
		
		assertTrue(or instanceof Or);
		assertEquals(1, or.outputs.size());
		assertEquals(o, or.outputs.get(0));
		assertEquals(2, or.inputs.size());
		Component oi1 = or.inputs.get(0);
		Component oi2 = or.inputs.get(1);
		
		assertEquals(i1, oi1);
		assertEquals(1, oi1.outputs.size());
		assertEquals(or, oi1.outputs.get(0));
		assertEquals(0, oi1.inputs.size());
		
		assertEquals(i2, oi2);
		assertEquals(1, oi2.outputs.size());
		assertEquals(or, oi2.outputs.get(0));
		assertEquals(0, oi2.inputs.size());
	}
	
	@Test
	public void removeComplete() {
		Tree t = new Tree();
		
		Output o = new Output("o");
		Latch l1 = new Latch();
		Latch l2 = new Latch();
		Input i = new Input("i");
		
		o.addInput(l1);
		l1.addInput(l1);
		l1.addInput(l2);
		l2.addInput(l2);
		l2.addInput(i);
		
		t.removeComplete(l1);
		
		Component c = o.inputs.remove(0);
		c.outputs.remove(o);

		assertEquals(0, o.inputs.size());
		assertEquals(0, o.outputs.size());
		assertEquals(0, l1.inputs.size());
		assertEquals(0, l1.outputs.size());
		assertEquals(2, l2.inputs.size());
		assertEquals(l2, l2.inputs.get(0));
		assertEquals(i, l2.inputs.get(1));
		assertEquals(1, l2.outputs.size());
		assertEquals(l2, l2.outputs.get(0));
		assertEquals(0, i.inputs.size());
		assertEquals(1, i.outputs.size());
		assertEquals(l2, i.outputs.get(0));
	}
	
	@Test
	public void unrollSanityCheck() {
		Tree t = new Tree();
		
		{
			Output o = new Output("o");
			And a = new And();
			Latch l1 = new Latch();
			Latch l2 = new Latch();
			Latch l3 = new Latch();
			Input i = new Input("i");
			
			o.addInput(a);
			
			a.addInput(i);
			a.addInput(l3);
			
			l3.addInput(l2);
			l2.addInput(l1);
			l1.addInput(i);
			
			t.outputs.add(o);
		}
		
		this.unrollSanityCheckOriginalTreeSanity(t);
		
		Tree k1 = t.unroll(1);
		this.unrollSanityCheckOriginalTreeSanity(t);
		
		{
			assertNotNull(k1);
			
			assertEquals(1, k1.outputs.size());

			Component k1o = k1.outputs.get(0);
			assertTrue(k1o instanceof Output);
			assertEquals(1, k1o.inputs.size());
			assertEquals(0, k1o.outputs.size());
			
			Component k1a = k1o.inputs.get(0);
			assertTrue(k1a instanceof And);
			assertEquals(2, k1a.inputs.size());
			assertEquals(1, k1a.outputs.size());
			assertSame(k1o, k1a.outputs.get(0));
			
			Component k1i = k1a.inputs.get(0);
			assertTrue(k1i instanceof Input);
			assertEquals(0, k1i.inputs.size());
			assertEquals(1, k1i.outputs.size());
			assertSame(k1a, k1i.outputs.get(0));
			
			Component k1f = k1a.inputs.get(1);
			assertTrue(k1f instanceof False);
			assertSame(k1.cFalse, k1f);
			assertEquals(0, k1f.inputs.size());
			assertEquals(1, k1f.outputs.size());
			assertSame(k1a, k1f.outputs.get(0));
		}
		
		Tree k2 = t.unroll(2);
		this.unrollSanityCheckOriginalTreeSanity(t);
		
		{
			assertNotNull(k2);
			
			assertEquals(2, k2.outputs.size());
			
			// k1

			Component k1o = k2.outputs.get(0);
			assertTrue(k1o instanceof Output);
			assertEquals(1, k1o.inputs.size());
			assertEquals(0, k1o.outputs.size());
			
			Component k1a = k1o.inputs.get(0);
			assertTrue(k1a instanceof And);
			assertEquals(2, k1a.inputs.size());
			assertEquals(1, k1a.outputs.size());
			assertSame(k1o, k1a.outputs.get(0));
			
			Component k1i = k1a.inputs.get(0);
			assertTrue(k1i instanceof Input);
			assertEquals(0, k1i.inputs.size());
			assertEquals(1, k1i.outputs.size());
			assertSame(k1a, k1i.outputs.get(0));
			
			Component k1f = k1a.inputs.get(1);
			assertTrue(k1f instanceof False);
			assertSame(k2.cFalse, k1f);
			assertEquals(0, k1f.inputs.size());
			assertEquals(2, k1f.outputs.size());
			assertSame(k1a, k1f.outputs.get(0));
			
			// k2

			Component k2o = k2.outputs.get(1);
			assertTrue(k2o instanceof Output);
			assertEquals(1, k2o.inputs.size());
			assertEquals(0, k2o.outputs.size());
			
			Component k2a = k2o.inputs.get(0);
			assertTrue(k2a instanceof And);
			assertEquals(2, k2a.inputs.size());
			assertEquals(1, k2a.outputs.size());
			assertSame(k2o, k2a.outputs.get(0));
			
			Component k2i = k2a.inputs.get(0);
			assertTrue(k2i instanceof Input);
			assertEquals(0, k2i.inputs.size());
			assertEquals(1, k2i.outputs.size());
			assertSame(k2a, k2i.outputs.get(0));
			assertNotSame(k1i, k2i);
			
			assertSame(k2a, k1f.outputs.get(1));
		}
		
		Tree k3 = t.unroll(3);
		this.unrollSanityCheckOriginalTreeSanity(t);
		
		{
			assertNotNull(k3);
			
			assertEquals(3, k3.outputs.size());
			
			// k1

			Component k1o = k3.outputs.get(0);
			assertTrue(k1o instanceof Output);
			assertEquals(1, k1o.inputs.size());
			assertEquals(0, k1o.outputs.size());
			
			Component k1a = k1o.inputs.get(0);
			assertTrue(k1a instanceof And);
			assertEquals(2, k1a.inputs.size());
			assertEquals(1, k1a.outputs.size());
			assertSame(k1o, k1a.outputs.get(0));
			
			Component k1i = k1a.inputs.get(0);
			assertTrue(k1i instanceof Input);
			assertEquals(0, k1i.inputs.size());
			assertEquals(2, k1i.outputs.size());
			assertSame(k1a, k1i.outputs.get(0));
			
			Component k1f = k1a.inputs.get(1);
			assertTrue(k1f instanceof False);
			assertSame(k3.cFalse, k1f);
			assertEquals(0, k1f.inputs.size());
			assertEquals(2, k1f.outputs.size());
			assertSame(k1a, k1f.outputs.get(0));
			
			// k2

			Component k2o = k3.outputs.get(1);
			assertTrue(k2o instanceof Output);
			assertEquals(1, k2o.inputs.size());
			assertEquals(0, k2o.outputs.size());
			
			Component k2a = k2o.inputs.get(0);
			assertTrue(k2a instanceof And);
			assertEquals(2, k2a.inputs.size());
			assertEquals(1, k2a.outputs.size());
			assertSame(k2o, k2a.outputs.get(0));
			
			Component k2i = k2a.inputs.get(0);
			assertTrue(k2i instanceof Input);
			assertEquals(0, k2i.inputs.size());
			assertEquals(1, k2i.outputs.size());
			assertSame(k2a, k2i.outputs.get(0));
			assertNotSame(k1i, k2i);
			
			assertSame(k2a, k1f.outputs.get(1));
			
			// k3

			Component k3o = k3.outputs.get(2);
			assertTrue(k3o instanceof Output);
			assertEquals(1, k3o.inputs.size());
			assertEquals(0, k3o.outputs.size());
			
			Component k3a = k3o.inputs.get(0);
			assertTrue(k3a instanceof And);
			assertEquals(2, k3a.inputs.size());
			assertEquals(1, k3a.outputs.size());
			assertSame(k3o, k3a.outputs.get(0));
			
			Component k3i = k3a.inputs.get(0);
			assertTrue(k3i instanceof Input);
			assertEquals(0, k3i.inputs.size());
			assertEquals(1, k3i.outputs.size());
			assertSame(k3a, k3i.outputs.get(0));
			assertNotSame(k1i, k3i);
			
			assertSame(k3a, k1i.outputs.get(1));
		}
	}
	
	@Test
	public void testReplaceComponent() {
		Input a = new Input("a");
		Input b = new Input("b");
		Output y = new Output("y");
		
		Component and = new And();
		and.addInput(a);
		and.addInput(b);
		
		y.addInput(and);
		
		Tree tree = new Tree();
		tree.outputs.add(y);
		
		Component or = new Or();
		tree.replaceComponent(and, or);
		
		tree.verifyTreeStructure();
		
		assertEquals(1, y.inputs.size());
		assertEquals(0, y.outputs.size());
		
		assertEquals(2, or.inputs.size());
		assertEquals(1, or.outputs.size());
		assertEquals(a, or.inputs.get(0));
		assertEquals(b, or.inputs.get(1));
		assertEquals(y, or.outputs.get(0));
		
		assertEquals(0, and.inputs.size());
		assertEquals(0, and.outputs.size());
		
		assertEquals(0, a.inputs.size());
		assertEquals(1, a.outputs.size());
		
		assertEquals(0, b.inputs.size());
		assertEquals(1, b.outputs.size());
	}
	
	private void unrollSanityCheckOriginalTreeSanity(Tree t) {
		assertNotNull(t);
		
		assertEquals(1, t.outputs.size());

		Component o = t.outputs.get(0);
		assertTrue(o instanceof Output);
		assertEquals(1, o.inputs.size());
		assertEquals(0, o.outputs.size());
		
		Component a = o.inputs.get(0);
		assertTrue(a instanceof And);
		assertEquals(2, a.inputs.size());
		assertEquals(1, a.outputs.size());
		assertSame(o, a.outputs.get(0));
		
		Component i = a.inputs.get(0);
		assertTrue(i instanceof Input);
		assertEquals(0, i.inputs.size());
		assertEquals(2, i.outputs.size());
		assertSame(a, i.outputs.get(0));
		
		Component l3 = a.inputs.get(1);
		assertTrue(l3 instanceof Latch);
		assertEquals(1, l3.inputs.size());
		assertEquals(1, l3.outputs.size());
		assertSame(a, l3.outputs.get(0));
		
		Component l2 = l3.inputs.get(0);
		assertTrue(l2 instanceof Latch);
		assertEquals(1, l2.inputs.size());
		assertEquals(1, l2.outputs.size());
		assertSame(l3, l2.outputs.get(0));
		
		Component l1 = l2.inputs.get(0);
		assertTrue(l1 instanceof Latch);
		assertEquals(1, l1.inputs.size());
		assertEquals(1, l1.outputs.size());
		assertSame(l2, l1.outputs.get(0));
		
		assertSame(l1, i.outputs.get(1));
	}
}
