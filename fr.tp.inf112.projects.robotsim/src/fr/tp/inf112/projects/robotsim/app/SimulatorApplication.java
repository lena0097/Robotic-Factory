package fr.tp.inf112.projects.robotsim.app;

import java.awt.Component;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.io.File;
import java.util.Arrays;

import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.io.IOException;

import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import fr.tp.inf112.projects.canvas.view.CanvasViewer;
import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import fr.tp.inf112.projects.robotsim.model.Area;
import fr.tp.inf112.projects.robotsim.model.Battery;
import fr.tp.inf112.projects.robotsim.model.ChargingStation;
import fr.tp.inf112.projects.robotsim.model.Conveyor;
import fr.tp.inf112.projects.robotsim.model.Door;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.Machine;
import fr.tp.inf112.projects.robotsim.model.Robot;
import fr.tp.inf112.projects.robotsim.model.Room;
import fr.tp.inf112.projects.robotsim.model.path.CustomDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.JGraphTDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.BasicPolygonShape;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class SimulatorApplication {

	private static final Logger LOGGER = Logger.getLogger(SimulatorApplication.class.getName());

	public static void main(String[] args) {
		// Diagnostic prints to help locate logging configuration and output file
		System.out.println("java.util.logging.config.file=" + System.getProperty("java.util.logging.config.file"));
		System.out.println("user.dir=" + System.getProperty("user.dir"));
		final File expectedLog = new File("config/simulator.log");
		System.out.println("expected log absolute path=" + expectedLog.getAbsolutePath() + " exists=" + expectedLog.exists());
		final java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
		System.out.println("root logger handlers:");
		for (final Handler h : root.getHandlers()) {
			System.out.println(" - " + h.getClass().getName());
		}

		LOGGER.info("Starting the robot simulator...");
		LOGGER.config("With parameters " + Arrays.toString(args) + ".");
		
		final Factory factory = new Factory(200, 200, "Simple Test Puck Factory");
		final Room room1 = new Room(factory, new RectangularShape(20, 20, 75, 75), "Production Room 1");
		new Door(room1, Room.WALL.BOTTOM, 10, 20, true, "Entrance");
		final Area area1 = new Area(room1, new RectangularShape(35, 35, 50, 50), "Production Area 1");
		final Machine machine1 = new Machine(area1, new RectangularShape(50, 50, 15, 15), "Machine 1");

		final Room room2 = new Room(factory, new RectangularShape( 120, 22, 75, 75 ), "Production Room 2");
		new Door(room2, Room.WALL.LEFT, 10, 20, true, "Entrance");
		final Area area2 = new Area(room2, new RectangularShape( 135, 35, 50, 50 ), "Production Area 1");
		final Machine machine2 = new Machine(area2, new RectangularShape( 150, 50, 15, 15 ), "Machine 1");
		
		final int baselineSize = 3;
		final int xCoordinate = 10;
		final int yCoordinate = 165;
		final int width =  10;
		final int height = 30;
		final BasicPolygonShape conveyorShape = new BasicPolygonShape();
		conveyorShape.addVertex(new BasicVertex(xCoordinate, yCoordinate));
		conveyorShape.addVertex(new BasicVertex(xCoordinate + width, yCoordinate));
		conveyorShape.addVertex(new BasicVertex(xCoordinate + width, yCoordinate + height - baselineSize));
		conveyorShape.addVertex(new BasicVertex(xCoordinate + width + baselineSize, yCoordinate + height - baselineSize));
		conveyorShape.addVertex(new BasicVertex(xCoordinate + width + baselineSize, yCoordinate + height));
		conveyorShape.addVertex(new BasicVertex(xCoordinate - baselineSize, yCoordinate + height));
		conveyorShape.addVertex(new BasicVertex(xCoordinate - baselineSize, yCoordinate + height - baselineSize));
		conveyorShape.addVertex(new BasicVertex(xCoordinate, yCoordinate + height - baselineSize));

		final Room chargingRoom = new Room(factory, new RectangularShape(125, 125, 50, 50), "Charging Room");
		new Door(chargingRoom, Room.WALL.RIGHT, 10, 20, false, "Entrance");
		final ChargingStation chargingStation = new ChargingStation(factory, new RectangularShape(150, 145, 15, 15), "Charging Station");

		final FactoryPathFinder jgraphPahtFinder = new JGraphTDijkstraFactoryPathFinder(factory, 5);
		final Robot robot1 = new Robot(factory, jgraphPahtFinder, new CircularShape(5, 5, 2), new Battery(10), "Robot 1");
		robot1.addTargetComponent(machine1);
		robot1.addTargetComponent(machine2);
		robot1.addTargetComponent(new Conveyor(factory, conveyorShape, "Conveyor 1"));
		robot1.addTargetComponent(chargingStation);

		final FactoryPathFinder customPathFinder = new CustomDijkstraFactoryPathFinder(factory, 5);
		final Robot robot2 = new Robot(factory, customPathFinder, new CircularShape(45, 5, 2), new Battery(10), "Robot 2");
		// Make Robot 2 visit Machine 1 first, then Machine 2 (same order as Robot 1)
		robot2.addTargetComponent(machine1);
		robot2.addTargetComponent(machine2);
		robot2.addTargetComponent(new Conveyor(factory, conveyorShape, "Conveyor 1"));
		robot2.addTargetComponent(chargingStation);
		
		SwingUtilities.invokeLater(new Runnable() {
			  
			@Override
	        public void run() {
		final FileCanvasChooser canvasChooser = new FileCanvasChooser("factory", "Puck Factory");
		// Use remote persistence manager (server must be started separately)
		final fr.tp.inf112.projects.robotsim.persistence.RemoteFactoryPersistenceManager remoteMgr =
			new fr.tp.inf112.projects.robotsim.persistence.RemoteFactoryPersistenceManager(canvasChooser, "localhost", 1957);
		final Component factoryViewer = new CanvasViewer(new SimulatorController(factory, remoteMgr));
				canvasChooser.setViewer(factoryViewer);

				// Add a global quick-save binding (Ctrl+S) to save the canvas without a dialog.
				// This won't change the File->Save Canvas dialog behaviour provided by the
				// external chooser, but it gives a convenient automatic-save-on-action.
				try {
					if (factoryViewer instanceof JComponent) {
						final JComponent jc = (JComponent) factoryViewer;
						jc.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
								KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "quickSave");
						jc.getActionMap().put("quickSave", new AbstractAction() {
							@Override
							public void actionPerformed(ActionEvent e) {
								// perform save in a background thread
								new Thread(() -> {
									try {
										remoteMgr.persist(factory);
										SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(jc,
												"Saved factory id=" + factory.getId()));
									} catch (final IOException ex) {
										SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(jc,
												"Failed to save: " + ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE));
									}
								}, "quick-save-thread").start();
							}
						});
					}
				} catch (final Throwable ignore) {
					// If anything goes wrong (class cast issues or missing libs), don't break startup
				}
				// Also intercept the File->Save Canvas menu action: when the user clicks the
				// menu item (text contains both "save" and "canvas"), perform the same
				// quick-save behaviour (no save dialog).
				try {
					final Component parentComp = factoryViewer instanceof Component ? (Component) factoryViewer : null;
					java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(evt -> {
						if (evt instanceof java.awt.event.ActionEvent) {
							final Object src = ((java.awt.event.ActionEvent) evt).getSource();
							if (src instanceof javax.swing.JMenuItem) {
								final javax.swing.JMenuItem mi = (javax.swing.JMenuItem) src;
								final String txt = mi.getText();
								if (txt != null) {
									final String low = txt.toLowerCase();
									if (low.contains("save") && low.contains("canvas")) {
										// run save in background
										new Thread(() -> {
											try {
												remoteMgr.persist(factory);
												SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parentComp,
														"Saved factory id=" + factory.getId()));
											} catch (final IOException ex) {
												SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parentComp,
														"Failed to save: " + ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE));
											}
										}, "menu-quick-save-thread").start();
									}
								}
							}
						}
					}, java.awt.AWTEvent.ACTION_EVENT_MASK);
				} catch (final Throwable ignore) {
					// non-fatal: if AWT listener cannot be installed, continue without menu interception
				}
				//new CanvasViewer(factory);
			}
		});
	}
}
