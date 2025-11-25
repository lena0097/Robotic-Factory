package fr.tp.inf112.projects.robotsim.model.shapes;

import fr.tp.inf112.projects.canvas.model.OvalShape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignore any unexpected properties to be resilient across versions
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.ANY,
				isGetterVisibility = JsonAutoDetect.Visibility.ANY,
				setterVisibility = JsonAutoDetect.Visibility.ANY)
public class CircularShape extends PositionedShape implements OvalShape {
	
	private static final long serialVersionUID = -1912941556210518344L;

	private int radius;
	
	public CircularShape( 	final int xCoordinate,
							final int yCoordinate,
							final int radius ) {
		super( xCoordinate, yCoordinate );
        
		this.radius = radius;
	}

	/** No-arg constructor for Jackson */
	protected CircularShape() {
		super();
		this.radius = 0;
	}

	@Override
	@JsonProperty("width") // Explicitly expose diameter as width for Jackson
	public int getWidth() {
		return 2 * radius;
	}

	@Override
	@JsonProperty("height") // Height equals width for perfect circle
	public int getHeight() {
		return getWidth();
	}

	/**
	 * Setter to allow Jackson to deserialize diameter-based JSON.
	 * Width represents the diameter for circular shapes.
	 */
	@JsonProperty("width")
	public void setWidth(int width) {
		this.radius = width / 2;
	}

	/**
	 * Setter to allow Jackson to deserialize diameter-based JSON.
	 * Height equals width (diameter) for circles; keep consistent with width.
	 */
	@JsonProperty("height")
	public void setHeight(int height) {
		this.radius = height / 2;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	@Override
	public String toString() {
		return super.toString() + " [radius=" + radius + "]";
	}
}
