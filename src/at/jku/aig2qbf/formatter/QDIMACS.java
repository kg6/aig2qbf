package at.jku.aig2qbf.formatter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.component.quantifier.Quantifier;
import at.jku.aig2qbf.component.quantifier.QuantifierSet;

public class QDIMACS extends Formatter {
	@Override
	public String format(Tree tree) {
		tree = (Tree)tree.clone();
		
		// replace true and false components with logic that is equal to true and false
		// this is necessary because QDIMACS does not have a symbol for true/false
		
		replaceFalseAig(tree);
		replaceTrueAig(tree);
		
		// convert the tree to CNF using the Tseitin conversion
		tree = tree.toTseitinCNF();
		
		Output rootNode = null;
		
		if(tree.outputs.size() > 0 && tree.outputs.get(0).inputs.size() > 0) {
			rootNode = tree.outputs.get(0);
		}
		
		// create the QDIMCAS output file
		StringBuilder qdimacsBuilder = new StringBuilder();
		
		if (rootNode != null) {
			final int numberOfVariables = getNumberOfVariables(rootNode.inputs);
			final int numberOfClauses = getNumberOfClauses(rootNode.inputs);
			
			// define problem line
			qdimacsBuilder.append("p cnf ");
			qdimacsBuilder.append(numberOfVariables);
			qdimacsBuilder.append(" ");
			qdimacsBuilder.append(numberOfClauses);
			qdimacsBuilder.append("\n");		

			// define all quantifiers
			for(QuantifierSet quantifier : tree.quantifier) {
				qdimacsBuilder.append(quantifier.quantifier == Quantifier.EXISTENTIAL ? "e" : "a");
				qdimacsBuilder.append(" ");
				
				for(Input input : quantifier.literals) {
					qdimacsBuilder.append(input.getId());
					qdimacsBuilder.append(" ");
				}
				
				qdimacsBuilder.append("0\n");
			}			
		
			// define CNF clauses
			defineCNFClauses(rootNode.inputs, qdimacsBuilder, numberOfClauses);
		} 
		else {
			qdimacsBuilder.append("p cnf 0 0\n");
		}
		
		return qdimacsBuilder.toString();
	}
	
	private void defineCNFClauses(List<Component> componentList, StringBuilder builder, int numberOfClauses) {
		Stack<Component> componentStack = new Stack<Component>();
		
		for(int i = componentList.size() - 1; i >= 0; i--) {
			componentStack.push(componentList.get(i));
		}
		
		int clauseCounter = 0;
		
		while(!componentStack.isEmpty()) {
			Component c = componentStack.pop();
			
			if(c instanceof And || c instanceof Or) {
				for(int i = c.inputs.size() - 1; i >= 0; i--) {
					if(c instanceof And) {
						componentStack.push(new NL());
					}
					
					componentStack.push(c.inputs.get(i));
				}
			} 
			else if (c instanceof Not) {
				boolean negated = true;
				Component child = c.inputs.get(0);
				
				while(child instanceof Not) {
					child = child.inputs.get(0);
					negated = !negated;
				}
				
				appendClause(builder, child.getId(), negated);
			} 
			else if (c instanceof Input) {
				appendClause(builder, c.getId(), false);
			} 
			else if (c instanceof NL) {				
				appendNL(builder);
				clauseCounter++;
			} 
			else {
				throw new RuntimeException("Unable to convert tree to QDIMACS format: The  node type '" + c.getClass().getName() + "' is unknown!");
			}
		}
		
		// make plausibility checks to make sure that the problem line represents the QDIMACS file correctly
		
		if(clauseCounter != numberOfClauses) {
			throw new RuntimeException("Unable to convert tree to QDIMACS format: The expected number of clauses was different to the current number of clauses!");
		}
		
		if(builder.charAt(builder.length() - 1) == ' ') {
			appendNL(builder);
		}
	}
	
	private void appendClause(StringBuilder builder, int id, boolean negated) {
		if(negated) {
			id = -id;
		}
		
		builder.append(Integer.toString(id));
		builder.append(" ");
	}
	
	private void appendNL(StringBuilder builder) {
		builder.append("0");
		builder.append("\n");
	}
	
	private void replaceFalseAig(Tree tree) {
		Input input = new Input("false");
		
		Component and = new And();
		and.addInput(input);
		
		Component notInput = new Not();
		notInput.addInput(input);
		
		and.addInput(notInput);
		
		// replace all false components
		tree.replaceComponent(tree.cFalse, and);
	}
	
	private void replaceTrueAig(Tree tree) {
		Input input = new Input("true");
		
		Component not = new Not();
		not.addInput(input);
		
		Component or = new Or();
		or.addInput(input);
		or.addInput(not);
		
		// replace all true components
		tree.replaceComponent(tree.cTrue, or);
	}
	
	private int getNumberOfVariables(List<Component> componentList) {
		Hashtable<Component, Boolean> visitedHash = new Hashtable<Component, Boolean>();
		
		List<Component> inputList = new ArrayList<Component>();
		rekGetNumberOfVariables(componentList, inputList, visitedHash);
		
		return inputList.size();
	}
	
	private void rekGetNumberOfVariables(List<Component> componentList, List<Component> inputList, Hashtable<Component, Boolean> visitedHash) {
		for(Component component : componentList) {
			if(component instanceof Input && !inputList.contains(component)) {
				inputList.add(component);
			} 
			else if(!visitedHash.containsKey(component)) {
				visitedHash.put(component, true);
				rekGetNumberOfVariables(component.inputs, inputList, visitedHash);
			}
		}
	}
	
	private int getNumberOfClauses(List<Component> componentList) {
		if(componentList.size() == 1 && componentList.get(0) instanceof And) {
			return componentList.get(0).inputs.size();
		} 
		else {
			return componentList.size();
		}
	}
	
	public class NL extends Component {

		@Override
		protected Object clone() {
			return new NL();
		}
	}
}