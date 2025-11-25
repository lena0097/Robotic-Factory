package fr.tp.inf112.projects.robotsim.model;

import java.io.Serializable;

import fr.tp.inf112.projects.canvas.model.Figure;
import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.canvas.model.Shape;

// Add Runnable implementation
public abstract class Component implements Figure, Serializable, Runnable {

	private static final long serialVersionUID = -5960950869184030220L;

	private String id;

	@com.fasterxml.jackson.annotation.JsonBackReference
	private Factory factory;

	private PositionedShape positionedShape;

	private String name;

	// Loop sleep interval for component threads
	private static final int LOOP_SLEEP_MS = 50;

	protected Component(final Factory factory,
						final PositionedShape shape,
						final String name) {
		this.factory = factory;
		this.positionedShape = shape;
		this.name = name;

		if (factory != null) {
			factory.addComponent(this);
		}
	}

	/** No-arg constructor for Jackson */
	protected Component() {
		// fields will be populated by Jackson
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public PositionedShape getPositionedShape() {
		return positionedShape;
	}
	
	public Position getPosition() {
		return getPositionedShape().getPosition();
	}

	protected Factory getFactory() {
		return factory;
	}

	@com.fasterxml.jackson.annotation.JsonIgnore
	@Override
	public int getxCoordinate() {
		return getPositionedShape() == null ? -1 : getPositionedShape().getxCoordinate();
	}

	protected boolean setxCoordinate(int xCoordinate) {
		if ( getPositionedShape().setxCoordinate( xCoordinate ) ) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	@com.fasterxml.jackson.annotation.JsonIgnore
	@Override
	public int getyCoordinate() {
		return getPositionedShape() == null ? -1 : getPositionedShape().getyCoordinate();
	}

	protected boolean setyCoordinate(final int yCoordinate) {
		if (getPositionedShape().setyCoordinate(yCoordinate) ) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	protected void notifyObservers() {
		getFactory().notifyObservers();
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " [name=" + name + " xCoordinate=" + getxCoordinate() + ", yCoordinate=" + getyCoordinate()
				+ ", shape=" + getPositionedShape();
	}

	public int getWidth() {
		return getPositionedShape().getWidth();
	}

	public int getHeight() {
		return getPositionedShape().getHeight();
	}
	
	public boolean behave() {
		return false;
	}
	
	public boolean isMobile() {
		return false;
	}
	
	public boolean overlays(final Component component) {
		return overlays(component.getPositionedShape());
	}
	
	public boolean overlays(final PositionedShape shape) {
		return getPositionedShape().overlays(shape);
	}
	
	public boolean canBeOverlayed(final PositionedShape shape) {
		return false;
	}
	
	@Override
	public Style getStyle() {
		return ComponentStyle.DEFAULT;
	}
	
	@Override
	public Shape getShape() {
		return getPositionedShape();
	}
	
	public boolean isSimulationStarted() {
		return getFactory().isSimulationStarted();
	}
	
	@Override
    public void run() {
        // Only components attached to a factory participate
        while (isSimulationStarted()) {
            behave();
            try {
                Thread.sleep(LOOP_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}