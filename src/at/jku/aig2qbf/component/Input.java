package at.jku.aig2qbf.component;

public class Input extends Component {
	public Input(String name) {
		super(name);
	}

	@Override
	protected Object clone() {
		return new Input(this.getName());
	}
}
