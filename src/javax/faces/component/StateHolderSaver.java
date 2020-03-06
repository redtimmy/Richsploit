package javax.faces.component;

import java.io.Serializable;

public class StateHolderSaver implements Serializable {
	public static final long serialVersionUID = 6470180891722042701L;
	private String className = null;
	private Serializable savedState = null;
	
	public StateHolderSaver(String className, Serializable savedState) {
		this.className = className;
		this.savedState = savedState;
	}
}
