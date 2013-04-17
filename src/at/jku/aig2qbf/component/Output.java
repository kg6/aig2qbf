package at.jku.aig2qbf.component;


public class Output extends Component {
	public Output(String name) {
		super(name);
	}

	@Override
	protected Object clone() {
		return new Output(this.getName());
	}
}
