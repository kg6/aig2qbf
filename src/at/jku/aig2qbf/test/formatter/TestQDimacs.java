package at.jku.aig2qbf.test.formatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.formatter.QDIMACS;
import at.jku.aig2qbf.parser.AAG;
import at.jku.aig2qbf.reduction.SAT;
import at.jku.aig2qbf.test.TestUtil;

public class TestQDimacs {
	private final String TEMP_QDIMACS_FILE = "./output/temp.qbf";
	
	@Before
	public void setUp() throws Exception {
		Component.Reset();
	}
	
	@After
	public void tearDown() throws Exception {
		TestUtil.RemoveOutputFile(TEMP_QDIMACS_FILE);
	}
	
	@Test
	public void testEmpty() {
		Tree t = new AAG().parse("input/basic/empty.aag");
		
		QDIMACS q = new QDIMACS();
		q.format(t);
	}

	@Test
	public void testUnsat() {
		Input a = new Input("a");
		
		Component notA = new Not();
		notA.addInput(a);
		
		Component andA = new And();
		andA.addInput(a);
		andA.addInput(notA);
		
		Output outA = new Output("out");
		outA.addInput(andA);

		Tree treeA = new Tree();
		treeA.outputs.add(outA);
		
		assertFalse(checkSatisfiablity(treeA));

		Component b = new Input("b");
		Component c = new Input("b");
		
		Component andB = new And();
		andB.addInput(b);
		andB.addInput(c);
		
		Output outB = new Output("out");
		outB.addInput(andB);
		
		Tree treeB = new Tree();
		treeB.outputs.add(outB);

		assertTrue(checkSatisfiablity(treeB));
	}
	
	@Test
	public void testNot() {
		Input a = new Input("a");
		
		Component notA = new Not();
		notA.addInput(a);
		
		Output outA = new Output("out");
		outA.addInput(notA);
		
		Tree treeA = new Tree();
		treeA.outputs.add(outA);
		
		assertTrue(checkSatisfiablity(treeA));
		
		Input b = new Input("b");
		
		Component notB = new Not();
		notB.addInput(b);
		
		Component notNotB = new Not();
		notB.addInput(notB);
		
		Output outB = new Output("out");
		outB.addInput(notNotB);
		
		Tree treeB = new Tree();
		treeB.outputs.add(outB);
		
		assertFalse(checkSatisfiablity(treeB));
	}
	
	@Test
	public void testAnd() {
		Input a = new Input("a");
		Input b = new Input("b");
		
		Component andAB = new And();
		andAB.addInput(a);
		andAB.addInput(b);
		
		Output outA = new Output("out");
		outA.addInput(andAB);
		
		Tree treeA = new Tree();
		treeA.outputs.add(outA);
		
		assertTrue(checkSatisfiablity(treeA));
	}
	
	@Test
	public void testOr() {
		Input a = new Input("a");
		Input b = new Input("b");
		
		Component orAB = new Or();
		orAB.addInput(a);
		orAB.addInput(b);
		
		Output outA = new Output("out");
		outA.addInput(orAB);
		
		Tree treeA = new Tree();
		treeA.outputs.add(outA);
		
		assertTrue(checkSatisfiablity(treeA));
	}
	
	private boolean checkSatisfiablity(Tree tree) {
		QDIMACS q = new QDIMACS();

		if(! q.writeToFile(tree, TEMP_QDIMACS_FILE)) {
			fail("Unable to write SAT temporary file");
			return false;
		}
		
		checkQDIMACSFileStructure();
		
		return SAT.Solve(TEMP_QDIMACS_FILE);
	}
	
	private void checkQDIMACSFileStructure() {
		String fileContent = TestUtil.ReadQDIMACSFile(TEMP_QDIMACS_FILE);
		
		assertTrue(fileContent == null || fileContent.length() > 0);
		
		int expectedVariableCount = 0;
		int expectedClauseCount = 0;
		
		int currentClauseCount = 0;
		Hashtable<Integer, Boolean> currentVariableHash = new Hashtable<Integer, Boolean>();
		
		List<Integer> existentialQuantorList = new ArrayList<Integer>();
		List<Integer> universalQuantorList = new ArrayList<Integer>();
		
		boolean programLinePassed = false;
		
		String[] lines = fileContent.split("\n");
		
		for(String line : lines) {
			if(line.startsWith("p ")) {
				programLinePassed = true;
				
				String[] programLine = line.split("\\s");
				
				assertEquals(4, programLine.length);
				assertEquals("p", programLine[0]);
				assertEquals("cnf", programLine[1]);
				
				expectedVariableCount = Integer.parseInt(programLine[2]);
				expectedClauseCount = Integer.parseInt(programLine[3]);
				
				assertTrue(expectedVariableCount >= 0);
				assertTrue(expectedClauseCount >= 0);
			} else if(line.startsWith("c ") && programLinePassed) {
				fail("Comments after program line are not allowed");
			} else if(line.startsWith("e ")) {
				assertTrue(programLinePassed);
				
				parseQuantifierLine(line, existentialQuantorList);
			} else if(line.startsWith("a ")) {
				assertTrue(programLinePassed);
				
				parseQuantifierLine(line, universalQuantorList);
			} else {
				assertTrue(programLinePassed);
				
				parseClause(line, currentVariableHash);
				
				currentClauseCount++;
			}
		}
		
		assertEquals(expectedVariableCount, currentVariableHash.size());
		assertEquals(expectedClauseCount, currentClauseCount);
	}
	
	private void parseQuantifierLine(String line, List<Integer> quantifierCollection) {
		String[] tmp = line.split("\\s");
		
		assertTrue(tmp.length > 1);
		
		for(int i = 1; i < tmp.length; i++) {
			int val = Integer.parseInt(tmp[i]);
			
			quantifierCollection.add(val);
			
			if(i == tmp.length - 1) {
				assertEquals(0, val);
			}
		}
	}
	
	private void parseClause(String line, Hashtable<Integer, Boolean> variableHash) {
		String[] tmp = line.split("\\s");
		
		assertTrue(tmp.length > 1);
		
		for(int i = 0; i < tmp.length; i++) {
			int val = Math.abs(Integer.parseInt(tmp[i]));
			
			if(i < tmp.length - 1) {
				variableHash.put(val, true);
				
				assertTrue(val != 0);
			} else {
				assertTrue(val == 0);
			}
		}
	}
}
