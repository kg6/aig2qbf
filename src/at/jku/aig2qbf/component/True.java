package at.jku.aig2qbf.component;

public class True extends Component {
	public True() {
		super();
	}

	@Override
	protected Object clone() {
		throw new RuntimeException("Cannot clone True directly");
	}
}
