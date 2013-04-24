package at.jku.aig2qbf.component;

public class LatchOutput {
	public Component component;
	public int branch;
	
	public LatchOutput(Component component, int branch) {
		this.component = component;
		this.branch = branch;
	}

	@Override
	public boolean equals(Object obj) {
		int peerId = 0;
		
		if(obj instanceof LatchOutput) {
			peerId = ((LatchOutput) obj).component.getId();
		} else if (obj instanceof Component) {
			peerId = ((Component) obj).getId();
		}
		
		return this.component.getId() == peerId;
	}
}
