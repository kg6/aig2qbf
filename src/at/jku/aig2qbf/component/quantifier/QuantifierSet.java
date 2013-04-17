package at.jku.aig2qbf.component.quantifier;

import java.util.ArrayList;
import java.util.List;

import at.jku.aig2qbf.component.Input;

public class QuantifierSet {
	public Quantifier quantifier;
	public List<Input> literals;

	public QuantifierSet(Quantifier quantifier, List<Input> literals) {
		this.quantifier = quantifier;
		this.literals = new ArrayList<Input>(literals);
	}
	
	public QuantifierSet(Quantifier quantifier, Input literal) {
		this.quantifier = quantifier;
		this.literals = new ArrayList<Input>();
		this.literals.add(literal);
	}
}
