package at.jku.aig2qbf.component;

public class Not extends Component {
	public Not() {
		super();
	}

	@Override
	protected Object clone() {
		return new Not();
	}
}
