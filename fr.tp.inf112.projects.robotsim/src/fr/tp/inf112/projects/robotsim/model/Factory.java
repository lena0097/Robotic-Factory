package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.tp.inf112.projects.canvas.controller.Observable;
import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.Figure;
import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.notifier.FactoryModelChangedNotifier;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Factory extends Component implements Canvas, Observable {

	private static final long serialVersionUID = 5156526483612458192L;
	
	private static final ComponentStyle DEFAULT = new ComponentStyle(5.0f);


	@com.fasterxml.jackson.annotation.JsonManagedReference
	private final List<Component> components;

	@com.fasterxml.jackson.annotation.JsonIgnore
	private transient List<Observer> observers;

	@com.fasterxml.jackson.annotation.JsonIgnore
	private transient volatile boolean simulationStarted;

	@com.fasterxml.jackson.annotation.JsonIgnore
	private transient FactoryModelChangedNotifier notifier;
	
	
	public Factory(final int width,
				   final int height,
				   final String name ) {
		super(null, new RectangularShape(0, 0, width, height), name);
		
		components = new ArrayList<>();
	observers = null;
	simulationStarted = false;
	notifier = null;
	}

	/** No-arg constructor for Jackson */
	protected Factory() {
		super();
		components = new ArrayList<>();
	observers = null;
	simulationStarted = false;
	notifier = null;
	}
	
	public List<Observer> getObservers() {
		if (observers == null) {
			observers = new ArrayList<>();
		}
		
		return observers;
	}

	@Override
	public boolean addObserver(Observer observer) {
		if (notifier != null) {
			return notifier.addObserver(observer);
		}
		return getObservers().add(observer);
	}

	@Override
	public boolean removeObserver(Observer observer) {
		if (notifier != null) {
			return notifier.removeObserver(observer);
		}
		return getObservers().remove(observer);
	}
	
	public void notifyObservers() {
		if (notifier != null) {
			notifier.notifyObservers();
			return;
		}
		for (final Observer observer : getObservers()) {
			observer.modelChanged();
		}
	}
	
	public boolean addComponent(final Component component) {
		if (components.add(component)) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	public boolean removeComponent(final Component component) {
		if (components.remove(component)) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	public List<Component> getComponents() {
		return components;
	}

	@Override
	@JsonIgnore
	public Collection<Figure> getFigures() {
		return (Collection) components;
	}

	@Override
	public String toString() {
		return super.toString() + " components=" + components + "]";
	}
	
	public boolean isSimulationStarted() {
		return simulationStarted;
	}

	public void startSimulation() {
        if (!isSimulationStarted()) {
            this.simulationStarted = true;
            notifyObservers();

            // Spawn component threads once
            behave();
        }
    }

	public void stopSimulation() {
		if (isSimulationStarted()) {
			this.simulationStarted = false;
			
			notifyObservers();
		}
	}

	public FactoryModelChangedNotifier getNotifier() {
		return notifier;
	}

	public void setNotifier(final FactoryModelChangedNotifier notifier) {
		this.notifier = notifier;
	}

	@Override
    public boolean behave() {
        // Start each component in its own thread
        for (final Component component : getComponents()) {
            new Thread(component, component.getName() + "-thread").start();
        }
        return true;
    }
	
	@Override
	public Style getStyle() {
		return DEFAULT;
	}
	
	public boolean hasObstacleAt(final PositionedShape shape) {
		for (final Component component : getComponents()) {
			if (component.overlays(shape) && !component.canBeOverlayed(shape)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasMobileComponentAt(final PositionedShape shape,
										final Component movingComponent) {
		for (final Component component : getComponents()) {
			if (component != movingComponent && component.isMobile() && component.overlays(shape)) {
				return true;
			}
		}
		
		return false;
	}
	
	public Component getMobileComponentAt(	final Position position,
											final Component ignoredComponent) {
		if (position == null) {
			return null;
		}
		
		return getMobileComponentAt(new RectangularShape(position.getxCoordinate(), position.getyCoordinate(), 2, 2), ignoredComponent);
	}
	
	public Component getMobileComponentAt(	final PositionedShape shape,
											final Component ignoredComponent) {
		if (shape == null) {
			return null;
		}
		
		for (final Component component : getComponents()) {
			if (component != ignoredComponent && component.isMobile() && component.overlays(shape)) {
				return component;
			}
		}
		
		return null;
	}

	// Synchronized move to check and move atomically
    public synchronized int moveComponent(final Motion motion,
                                          final Component componentToMove) {
        if (motion == null || componentToMove == null) {
            return 0;
        }

        // Determine target cell footprint 
        final Position target = motion.getTargetPosition(); 
        if (target == null) {
            return 0;
        }

        final RectangularShape targetShape =
                new RectangularShape(target.getxCoordinate(), target.getyCoordinate(), 2, 2);

        // Block if occupied by obstacle or another mobile component
        if (hasObstacleAt(targetShape) || hasMobileComponentAt(targetShape, componentToMove)) {
            return 0;
        }

        // Safe to move now 
        return motion.moveToTarget();
    }

}