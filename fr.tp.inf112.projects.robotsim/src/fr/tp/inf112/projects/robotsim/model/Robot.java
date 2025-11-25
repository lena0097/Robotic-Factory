package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.canvas.model.impl.RGBColor;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Robot extends Component {
	
	private static final long serialVersionUID = -1218857231970296747L;

	private static final Style STYLE = new ComponentStyle(RGBColor.GREEN, RGBColor.BLACK, 3.0f, null);

	private static final Style BLOCKED_STYLE = new ComponentStyle(RGBColor.RED, RGBColor.BLACK, 3.0f, new float[]{4.0f});

	private Battery battery;
	
	private int speed;
	
	private List<Component> targetComponents;
	
	@JsonIgnore
	private transient Iterator<Component> targetComponentsIterator;
	
	private Component currTargetComponent;
	
	@JsonIgnore
	private transient Iterator<Position> currentPathPositionsIter;

	@JsonIgnore
	private transient boolean blocked;

	@JsonIgnore
	private Position memorizedTargetPosition;

	@JsonIgnore
	private FactoryPathFinder pathFinder;

	public Robot(final Factory factory,
				 final FactoryPathFinder pathFinder,
				 final CircularShape shape,
				 final Battery battery,
				 final String name ) {
		super(factory, shape, name);
		
		this.pathFinder = pathFinder;
		
		this.battery = battery;
		
		targetComponents = new ArrayList<>();
		currTargetComponent = null;
		currentPathPositionsIter = null;
		speed = 5;
		blocked = false;
		memorizedTargetPosition = null;
	}

	@Override
	public String toString() {
		return super.toString() + " battery=" + battery + "]";
	}

	protected int getSpeed() {
		return speed;
	}

	protected void setSpeed(final int speed) {
		this.speed = speed;
	}
	
	public Position getMemorizedTargetPosition() {
		return memorizedTargetPosition;
	}
	
	private List<Component> getTargetComponents() {
		if (targetComponents == null) {
			targetComponents = new ArrayList<>();
		}
		
		return targetComponents;
	}
	
	public boolean addTargetComponent(final Component targetComponent) {
		return getTargetComponents().add(targetComponent);
	}
	
	public boolean removeTargetComponent(final Component targetComponent) {
		return getTargetComponents().remove(targetComponent);
	}
	
	@Override
	public boolean isMobile() {
		return true;
	}

	@Override
	public boolean behave() {
		if (getTargetComponents().isEmpty()) {
			return false;
		}
		
		if (currTargetComponent == null || hasReachedCurrentTarget()) {
			currTargetComponent = nextTargetComponentToVisit();
			
			computePathToCurrentTargetComponent();
		}

		return moveToNextPathPosition() != 0;
	}
		
	private Component nextTargetComponentToVisit() {
		if (targetComponentsIterator == null || !targetComponentsIterator.hasNext()) {
			targetComponentsIterator = getTargetComponents().iterator();
		}
		
		return targetComponentsIterator.hasNext() ? targetComponentsIterator.next() : null;
	}
	
	
	private int moveToNextPathPosition() {
	final Motion motion = computeMotion();
	// Delegate movement to the Factory so the check+move can be synchronized there.
	int displacement = motion == null ? 0 : getFactory().moveComponent(motion, this);

        if (displacement != 0) {
            notifyObservers();
        }
        else if (isLivelyLocked()) {
            final Position freeNeighbouringPosition = findFreeNeighbouringPosition();
            if (freeNeighbouringPosition != null) {
                // Use the neighbour as a temporary escape position
                this.memorizedTargetPosition = freeNeighbouringPosition;
                final Motion escapeMotion = computeMotion();
                displacement = escapeMotion == null ? 0 : escapeMotion.moveToTarget();
                if (displacement != 0) {
                    notifyObservers();
                    // Clear memorized position so normal path steps resume
                    this.memorizedTargetPosition = null;
                    // Reset blocked state and recompute path from new position
                    blocked = false;
                    computePathToCurrentTargetComponent();
                }
            }
        }
        return displacement;
    }

	/** No-arg constructor for Jackson */
	protected Robot() {
		super();
		this.battery = null;
		speed = 5;
		targetComponents = new ArrayList<>();
		targetComponentsIterator = null;
		currTargetComponent = null;
		currentPathPositionsIter = null;
		blocked = false;
		memorizedTargetPosition = null;
		pathFinder = null;
	}

    private Position findFreeNeighbouringPosition() {
        final Position curr = getPosition();
        final int step = 2;
        final int[][] deltas = {
                { step, 0 }, { -step, 0 }, { 0, step }, { 0, -step },
                { step, step }, { -step, step }, { step, -step }, { -step, -step }
        };

        for (final int[] d : deltas) {
            final int nx = curr.getxCoordinate() + d[0];
            final int ny = curr.getyCoordinate() + d[1];
            if (nx < 0 || ny < 0) {
                continue;
            }
            final RectangularShape candidateShape = new RectangularShape(nx, ny, 2, 2);
            if (!getFactory().hasObstacleAt(candidateShape)
                    && !getFactory().hasMobileComponentAt(candidateShape, this)) {
                return new Position(nx, ny);
            }
        }
        return null;
    }
	
	private void computePathToCurrentTargetComponent() {
		final List<Position> currentPathPositions = pathFinder.findPath(this, currTargetComponent);
		currentPathPositionsIter = currentPathPositions.iterator();
	}
	
	private Motion computeMotion() {
		// Ensure the path iterator is initialized. It can be null after
		// deserialization because it's transient, or if the path was not
		// computed yet for the current target. Try to compute it on demand.
		if (currentPathPositionsIter == null) {
			if (currTargetComponent == null) {
				// No target to compute a path for
				blocked = true;
				return null;
			}

			try {
				computePathToCurrentTargetComponent();
			} catch (final Exception e) {
				// On any error while computing a path, mark as blocked and abort movement
				blocked = true;
				return null;
			}

			if (currentPathPositionsIter == null) {
				// Path finder returned null or failed
				blocked = true;
				return null;
			}
		}

		if (!currentPathPositionsIter.hasNext()) {
			// There is no free path to the target
			blocked = true;
			return null;
		}
		
		
		final Position targetPosition = getTargetPosition();
		final PositionedShape shape = new RectangularShape(targetPosition.getxCoordinate(),
														   targetPosition.getyCoordinate(),
				   										   2,
				   										   2);
		
		// If there is another robot, memorize the target position for the next run
		if (getFactory().hasMobileComponentAt(shape, this)) {
			this.memorizedTargetPosition = targetPosition;
			
			return null;
		}

		// Reset the memorized position
		this.memorizedTargetPosition = null;
			
		return new Motion(getPosition(), targetPosition);
	}
	
	private Position getTargetPosition() {
		// If a target position was memorized, it means that the robot was blocked during the last iteration 
		// so it waited for another robot to pass. So try to move to this memorized position otherwise move to  
		// the next position from the path
		return this.memorizedTargetPosition == null ? currentPathPositionsIter.next() : this.memorizedTargetPosition;
	}
	
	public boolean isLivelyLocked() {
	    if (memorizedTargetPosition == null) {
	        return false;
	    }
			
	    final Component otherComponent = getFactory().getMobileComponentAt(memorizedTargetPosition,     
	                                                                   this);

	    if (otherComponent instanceof Robot)  {
		    return getPosition().equals(((Robot) otherComponent).getMemorizedTargetPosition());
	    }
	    
	    return false;
	}

	private boolean hasReachedCurrentTarget() {
		return getPositionedShape().overlays(currTargetComponent.getPositionedShape());
	}
	
	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		return true;
	}
	
	@Override
	public Style getStyle() {
		return blocked ? BLOCKED_STYLE : STYLE;
	}
}