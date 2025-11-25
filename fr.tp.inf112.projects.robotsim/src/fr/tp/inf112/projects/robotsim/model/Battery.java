package fr.tp.inf112.projects.robotsim.model;

import java.io.Serializable;

public class Battery implements Serializable {
	
	private static final long serialVersionUID = 5744149485828674046L;

	private float capacity;
    
	private float level;

	public Battery(final float capacity) {
		this.capacity = capacity;
		level = capacity;
	}

	/** No-arg constructor for Jackson */
	protected Battery() {
		this.capacity = 0.0f;
		this.level = 0.0f;
	}
	
	public float consume(float energy) {
		level-= energy;
		
		return level;
	}
	
	public float charge(float energy) {
		level+= energy;
		
		return level;
	}

	@Override
	public String toString() {
		return "Battery [capacity=" + capacity + "]";
	}
}
