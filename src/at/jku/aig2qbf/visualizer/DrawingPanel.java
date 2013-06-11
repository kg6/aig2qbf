package at.jku.aig2qbf.visualizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import at.jku.aig2qbf.component.And;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.False;
import at.jku.aig2qbf.component.Input;
import at.jku.aig2qbf.component.Latch;
import at.jku.aig2qbf.component.Not;
import at.jku.aig2qbf.component.Or;
import at.jku.aig2qbf.component.Output;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.component.True;

public class DrawingPanel extends JPanel implements ComponentListener {
	private static final long serialVersionUID = 1L;

	private final Tree TREE;

	private final int NODE_SCALE = 32;
	private final int NODE_OFFSET = 10;
	private final int Y_MARGIN = 20;
	private final int ARROW_LENGTH = 5;
	private final int ARROW_START_RADIUS = 6;

	private final Color BACKGROUND_COLOR;
	private final Color NORMAL_COLOR;
	private final Color CIRCLE_COLOR;

	private final float CHARACTER_WIDTH = 3.5f;
	private final int CHARACTER_HEIGHT = 8;

	private final String DEFAULT_OUTPUT_FILENAME = "output";

	private HashMap<Component, Boolean> traversingHash;
	private HashMap<Component, Point> drawingHash;
	private HashMap<Integer, Integer> lineElementHash;
	private HashMap<Point, Boolean> usedPointCoordinatesHash;

	private BufferedImage backgroundImage;

	private int width;
	private int height;
	private int nodeWidth;
	private int nodeHeight;

	public DrawingPanel(Tree tree) {
		TREE = tree;

		BACKGROUND_COLOR = Color.WHITE;
		NORMAL_COLOR = Color.BLACK;
		CIRCLE_COLOR = Color.RED;
		
		this.traversingHash = new HashMap<>();
		this.drawingHash = new HashMap<>();
		this.lineElementHash = new HashMap<>();
		this.usedPointCoordinatesHash = new HashMap<>();

		this.backgroundImage = null;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (this.backgroundImage == null) {
			generateBackgroundImage();
		}

		g.drawImage(this.backgroundImage, 0, 0, this);
		g.finalize();
	}

	private void generateBackgroundImage() {
		this.width = Math.max(getGraphWidth() * NODE_SCALE, getWidth());
		this.height = Math.max(getGraphHeight() * (NODE_SCALE * 2 + Y_MARGIN), getHeight());

		this.nodeWidth = NODE_SCALE;
		this.nodeHeight = NODE_SCALE;

		this.backgroundImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);

		Graphics g = this.backgroundImage.getGraphics();

		this.lineElementHash.clear();
		this.drawingHash.clear();

		g.setColor(BACKGROUND_COLOR);
		g.fillRect(0, 0, this.width, this.height);
		g.setColor(NORMAL_COLOR);

		this.usedPointCoordinatesHash.clear();
		
		rekDrawNodeChildren(TREE.outputs, (Graphics2D) g, 0, -1, -1);

		Dimension dimension = this.getSize();

		if (this.width != dimension.getWidth() || this.height != dimension.getHeight()) {
			this.setPreferredSize(new Dimension(this.width, this.height));
		}
	}

	public JFileChooser getOutputFileChooser() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select a storage location");
		fileChooser.setAcceptAllFileFilterUsed(false);

		fileChooser.addChoosableFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "PNG image (*.png)";
			}

			@Override
			public boolean accept(File file) {
				if (file.isDirectory()) {
					return true;
				}
				else {
					return file.getName().toLowerCase().endsWith(".png");
				}
			}
		});

		return fileChooser;
	}

	public boolean saveBackgroundImageToFile(File file) {
		if (file.isDirectory()) {
			file = new File(file.getAbsoluteFile() + Character.toString(File.separatorChar) + DEFAULT_OUTPUT_FILENAME + ".png");
		}
		else if (! file.getName().endsWith(".png")) {
			file = new File(file.getAbsolutePath() + ".png");
		}

		try {
			return ImageIO.write(this.backgroundImage, "png", file);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private void rekDrawNodeChildren(List<? extends Component> componentList, Graphics2D g, int level, int parentXPos, int parentYPos) {
		final int numNodesAtLevel = getNodesOfLevel(level).size();
		final int yPos = (level + 1) * (this.nodeHeight + NODE_SCALE + Y_MARGIN);
		final int xPos = this.width / (numNodesAtLevel + 1);

		int elementCounter = 1;
		if (this.lineElementHash.containsKey(yPos)) {
			elementCounter = this.lineElementHash.get(yPos);
		}

		for (int i = 0; i < componentList.size(); i++) {
			int currentXPos = xPos * elementCounter;

			Component component = componentList.get(i);

			if (this.drawingHash.containsKey(component)) {
				Point p = this.drawingHash.get(component);
				
				drawConnection(g, parentXPos, parentYPos, p.x, p.y, true);
				
				continue;
			}

			this.drawingHash.put(component, new Point(currentXPos, yPos));

			drawNode(g, currentXPos, yPos, component);

			if (parentXPos >= 0 && parentYPos >= 0) {
				drawConnection(g, parentXPos, parentYPos, currentXPos, yPos, false);
			}

			rekDrawNodeChildren(component.inputs, g, level + 1, currentXPos, yPos);
			elementCounter++;
		}

		this.lineElementHash.put(yPos, elementCounter);
	}

	private void drawNode(Graphics2D g, int x, int y, Component component) {
		if (component instanceof Input || component instanceof Output) {
			g.drawOval(x, y, this.nodeWidth, this.nodeHeight);
		}
		else {
			g.drawRect(x, y, this.nodeWidth, this.nodeHeight);
		}

		String name = component.getName();
		
		if (name == null) {
			if (component instanceof And) {
				name = "AND";
			}
			else if (component instanceof Or) {
				name = "OR";
			}
			else if (component instanceof Not) {
				name = "NOT";
			}
			else if (component instanceof Latch) {
				name = "LATCH";
			}
			else if (component instanceof True) {
				name = "TRUE";
			}
			else if (component instanceof False) {
				name = "FALSE";
			}
			else {
				name = "UNKNOWN";
			}
		}
		else {
			if (component instanceof Input) {
				name = "i-" + name;
			}
			else if (component instanceof Output) {
				name = "o-" + name;
			}
		}

		g.drawString(name, x + this.nodeWidth / 2 - (int) ((float) name.length() * CHARACTER_WIDTH), y + this.nodeHeight / 2 + CHARACTER_HEIGHT / 2);

		final String componentId = Integer.toString(component.getId());
		g.drawString(componentId, x + this.nodeWidth / 2 - (int) ((float) componentId.length() * CHARACTER_WIDTH), y + this.nodeHeight / 2 + 2 * CHARACTER_HEIGHT);
	}

	private void drawConnection(Graphics2D g, int parentXPos, int parentYPos, int currentXPos, int currentYPos, boolean circleClosing) {
		List<Coordinate> coordinates = new ArrayList<Coordinate>();

		if (circleClosing) {
			final int backwardPathConnectionYPos0 = currentYPos + this.nodeHeight / 2;
			final int backwardPathConnectionYPos1 = getNextAvailableYCoordinate(currentXPos, parentYPos + this.nodeHeight / 2);
			
			if (parentXPos < currentXPos) {
				coordinates.add(new Coordinate(parentXPos + this.nodeWidth, parentYPos + this.nodeHeight / 2));
				coordinates.add(new Coordinate(parentXPos + this.nodeWidth + NODE_OFFSET, backwardPathConnectionYPos1));

				coordinates.add(new Coordinate(parentXPos + this.nodeWidth + NODE_OFFSET, backwardPathConnectionYPos1));
				coordinates.add(new Coordinate(currentXPos - NODE_OFFSET, backwardPathConnectionYPos0));

				coordinates.add(new Coordinate(currentXPos - NODE_OFFSET, backwardPathConnectionYPos0));
				coordinates.add(new Coordinate(currentXPos, currentYPos + this.nodeHeight / 2));
			}
			else if (parentXPos > currentXPos) {
				coordinates.add(new Coordinate(parentXPos, parentYPos + this.nodeHeight / 2));
				coordinates.add(new Coordinate(parentXPos - NODE_OFFSET, backwardPathConnectionYPos1));

				coordinates.add(new Coordinate(parentXPos - NODE_OFFSET, backwardPathConnectionYPos1));
				coordinates.add(new Coordinate(currentXPos + this.nodeWidth + NODE_OFFSET, backwardPathConnectionYPos0));

				coordinates.add(new Coordinate(currentXPos + this.nodeWidth + NODE_OFFSET, backwardPathConnectionYPos0));
				coordinates.add(new Coordinate(currentXPos + this.nodeWidth, currentYPos + this.nodeHeight / 2));
			}
			else {
				coordinates.add(new Coordinate(parentXPos, parentYPos + this.nodeHeight / 2));
				coordinates.add(new Coordinate(parentXPos - NODE_OFFSET, backwardPathConnectionYPos1));

				coordinates.add(new Coordinate(parentXPos - NODE_OFFSET, backwardPathConnectionYPos1));
				coordinates.add(new Coordinate(parentXPos - NODE_OFFSET, backwardPathConnectionYPos0));

				coordinates.add(new Coordinate(parentXPos - NODE_OFFSET, backwardPathConnectionYPos0));
				coordinates.add(new Coordinate(parentXPos, currentYPos + this.nodeHeight / 2));
			}
		}
		else {
			coordinates.add(new Coordinate(parentXPos + this.nodeWidth / 2, parentYPos + this.nodeHeight));
			coordinates.add(new Coordinate(parentXPos + this.nodeWidth / 2, parentYPos + this.nodeHeight + NODE_OFFSET));

			coordinates.add(new Coordinate(parentXPos + this.nodeWidth / 2, parentYPos + this.nodeHeight + NODE_OFFSET));
			coordinates.add(new Coordinate(currentXPos + this.nodeWidth / 2, currentYPos - NODE_OFFSET));

			coordinates.add(new Coordinate(currentXPos + this.nodeWidth / 2, currentYPos - NODE_OFFSET));
			coordinates.add(new Coordinate(currentXPos + this.nodeWidth / 2, currentYPos));
		}

		drawArrow(g, coordinates, circleClosing);
	}
	
	private int getNextAvailableYCoordinate(int xPos, int yPos) {
		Point point = new Point(xPos, yPos);
		
		while(this.usedPointCoordinatesHash.containsKey(point)) {
			point.y += 10;
		}
		
		this.usedPointCoordinatesHash.put(point, true);
		
		return point.y;
	}

	private void drawArrow(Graphics g1, List<Coordinate> coordinates, boolean circleClosing) {
		Graphics2D g = (Graphics2D) g1.create();

		if (circleClosing) {
			g.setColor(CIRCLE_COLOR);
		}

		final int corrdinatesCount = coordinates.size();

		if (corrdinatesCount <= 1) {
			return;
		}

		for (int i = 0; i < corrdinatesCount - 1; i += 2) {
			Coordinate start = coordinates.get(i);
			Coordinate end = coordinates.get(i + 1);

			g.drawLine(start.x, start.y, end.x, end.y);
		}

		// Draw the circle to the beginning of the arrow
		Coordinate last = coordinates.get(corrdinatesCount - 1);

		g.fillOval(last.x - ARROW_START_RADIUS / 2, last.y - ARROW_START_RADIUS / 2, ARROW_START_RADIUS, ARROW_START_RADIUS);

		// Draw the arrow
		Coordinate start = coordinates.get(0);
		Coordinate end = coordinates.get(1);

		final double dx = start.x - end.x;
		final double dy = start.y - end.y;
		final double angle = Math.atan2(dy, dx);
		final int len = (int) Math.sqrt(dx * dx + dy * dy);

		AffineTransform at = AffineTransform.getTranslateInstance(end.x, end.y);
		at.concatenate(AffineTransform.getRotateInstance(angle));
		g.transform(at);

		g.drawLine(0, 0, len, 0);
		g.fillPolygon(new int[] {
			len,
			len - ARROW_LENGTH,
			len - ARROW_LENGTH,
			len
		}, new int[] {
			0,
			-ARROW_LENGTH,
			ARROW_LENGTH,
			0
		}, 4);

		// Reset the drawing color
		g.setColor(NORMAL_COLOR);
	}

	private int getGraphWidth() {
		int level = 0;
		int maxNodes = 0;
		List<Component> componentList;

		do {
			componentList = getNodesOfLevel(level);

			if (componentList.size() > maxNodes) {
				maxNodes = componentList.size();
			}

			level++;
		} while (componentList.size() > 0);

		return maxNodes;
	}

	private int getGraphHeight() {
		this.traversingHash.clear();

		return rekGetGraphHeight(TREE.outputs, 0, Integer.MIN_VALUE);
	}

	private int rekGetGraphHeight(List<? extends Component> componentList, int currentLevel, int maxLevel) {
		List<Component> traversingList = new ArrayList<Component>();

		for (Component component : componentList) {
			if (this.traversingHash.containsKey(component)) {
				continue;
			}

			traversingList.add(component);
			this.traversingHash.put(component, true);
		}

		if (currentLevel > maxLevel) {
			maxLevel = currentLevel;
		}

		for (Component component : traversingList) {
			maxLevel = rekGetGraphHeight(component.inputs, currentLevel + 1, maxLevel);
		}

		return maxLevel;
	}

	private List<Component> getNodesOfLevel(int expectedLevel) {
		this.traversingHash.clear();

		List<Component> nodeList = new ArrayList<Component>();
		rekGetNodesOfLevel(0, expectedLevel, TREE.outputs, nodeList);
		return nodeList;
	}

	private void rekGetNodesOfLevel(int currentLevel, final int expectedLevel, List<? extends Component> componentList, List<Component> nodeList) {
		for (Component component : componentList) {
			if (this.traversingHash.containsKey(component)) {
				continue;
			}

			this.traversingHash.put(component, true);

			if (currentLevel == expectedLevel) {
				nodeList.add(component);
			}
			else {
				rekGetNodesOfLevel(currentLevel + 1, expectedLevel, component.inputs, nodeList);
			}
		}
	}

	private class Coordinate {
		private int x;
		private int y;

		public Coordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		if (this.backgroundImage == null) {
			return;
		}

		Dimension dimension = getSize();

		if (this.backgroundImage.getWidth() != dimension.getWidth() || this.backgroundImage.getHeight() != dimension.getHeight()) {
			this.backgroundImage = null;
			invalidate();
		}
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}
}
