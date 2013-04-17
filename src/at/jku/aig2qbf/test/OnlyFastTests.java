package at.jku.aig2qbf.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import at.jku.aig2qbf.test.component.TestComponent;
import at.jku.aig2qbf.test.component.TestTree;
import at.jku.aig2qbf.test.formatter.TestQDimacs;
import at.jku.aig2qbf.test.parser.TestAAG;
import at.jku.aig2qbf.test.parser.TestAIG;
import at.jku.aig2qbf.test.reduction.TestSimplePathReduction;

@RunWith(Suite.class)
@SuiteClasses({
	TestComponent.class,
	TestTree.class,
	at.jku.aig2qbf.test.formatter.TestAAG.class,
	TestQDimacs.class,
	TestAAG.class,
	TestAIG.class,
	TestSimplePathReduction.class,
})
public class OnlyFastTests {

}
