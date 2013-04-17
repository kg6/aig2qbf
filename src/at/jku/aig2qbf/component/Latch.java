package at.jku.aig2qbf.component;

public class Latch extends Component {
	public Latch() {
		super();
	}

	@Override
	protected Object clone() {
		return new Latch();
	}
}
