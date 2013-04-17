package at.jku.aig2qbf.component;

public class False extends Component {
	public False() {
		super();
	}

	@Override
	protected Object clone() {
		throw new RuntimeException("Cannot clone False directly");
	}
}
