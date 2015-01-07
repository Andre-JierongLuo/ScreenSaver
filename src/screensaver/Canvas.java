package screensaver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import org.w3c.dom.Node;

/**
 * The canvas where the screen saver is drawn.
 */
public class Canvas extends JComponent {

	// A list of nodes.  Only one is used in the sample, but there's
	// no reason there couldn't be more.
	private ArrayList<SSNode> nodes = new ArrayList<SSNode>();
	private static int FPS = 60;	// How often we update the animation.
	private Timer timer;			// The timer to actually cause the animation updates.
	private SSNode selectedNode = null;		// Which node is selected; null if none
	private Timer timer2;			// The timer to handle the update of change when the animation is off.
	private JButton ColorButton = new JButton("COLOUR");
	private JButton addShapeButton = new JButton("ADD SHAPE");
	private JMenuBar changeShapeMenuBar;
	private JPanel container = new JPanel(new GridLayout(1, 3));
	
	public Canvas() {

		/*
		 * The mouse input listener combines MouseListener and MouseMotionListener.
		 * Still need to add it both ways, though.
		 */
		MouseInputListener mil = new MouseHandler();
		this.addMouseListener(mil);
		this.addMouseMotionListener(mil);
				
		this.setOpaque(true);	// we paint every pixel; Java can optimize
		
		ActionListener repainter = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				repaint();
			}
		};
		timer = new Timer(1000/FPS, repainter);
		timer.start();
		
		ActionListener repainter2 = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				repaint();
			}
		};
		timer2 = new Timer(1000/FPS, repainter2);
		timer2.start();
		
		ColorButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(selectedNode != null){
					Color newColor = JColorChooser.showDialog(null, "Choose a Color", selectedNode.getColor());
					if(newColor != null){
						selectedNode.setColor(newColor);
					}
				}
			}
		});
		ColorButton.addMouseListener(new SimpleMouseListener());
		
		addShapeButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				// add node with a random translation.
				SSNode child = new SSNode(screensaver.Main.heart, Color.RED);
				Random rand = new Random();
				double dx = rand.nextInt(100 - 20 + 1) + 20.0;
				double dy = rand.nextInt(100 - 20 + 1) + 20.0;
				child.transform(AffineTransform.getTranslateInstance(dx, dy));
				if(selectedNode != null){
					selectedNode.addChild(child);
				} else {
					nodes.get(0).addChild(child);
				}
			}
		});
		addShapeButton.addMouseListener(new SimpleMouseListener());
		
		this.changeShapeMenuBar = this.createMenuBar();
		
		this.setLayout(new BorderLayout());
		container.add(addShapeButton);
		container.add(ColorButton);
		container.add(changeShapeMenuBar);
		this.add(container,BorderLayout.SOUTH);
	}
	

	/**
	 * Paint this component:  fill in the background and then draw the nodes
	 * recursively.
	 */
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		super.paintComponent(g2);
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, this.getWidth(), this.getHeight());
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		// timer is running when the mouse is outside the window.
		if(timer.isRunning()){
			for(SSNode n : nodes){
				n.tick();
				n.paintNode((Graphics2D) g);
			}
		} else {
			for(SSNode n : nodes){
				n.paintNode((Graphics2D) g);
			}
		}
	}

	/**
	 * Add a new node to the canvas.
	 */
	public void addNode(SSNode n) {
		this.nodes.add(n);
	}

	/**
	 * Get the node containing the point p.  Return null
	 * if no such node exists.
	 */
	public SSNode getNode(Point2D p) {
		SSNode hit = null;
		int i = 0;
		while (hit == null && i < nodes.size()) {
			hit = nodes.get(i).hitNode(p);
			i++;
		}
		return hit;
	}

	/**
	 * Convert p to a Point2D, which has higher precision.
	 */
	private Point2D.Double p2D(Point p) {
		return new Point2D.Double(p.getX(), p.getY());
	}

	/**
	 * Listen for mouse events on the Canvas.  Pass them on to
	 * the selected node (if there is one) in most cases.
	 */
	class MouseHandler implements MouseInputListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			selectedNode = getNode(p2D(e.getPoint()));
			if (selectedNode != null) {
				selectedNode.mouseClicked(e);
				repaint();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if(selectedNode != null) selectedNode.loseFocus();
			selectedNode = getNode(p2D(e.getPoint()));
			if (selectedNode != null) {
				selectedNode.mousePressed(e);
				repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (selectedNode != null) {
				selectedNode.mouseReleased(e);
				repaint();
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			//System.out.format("enter %d,%d\n", e.getX(), e.getY());
			timer.stop();
			timer2.start();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			//System.out.format("exit %d,%d\n", e.getX(), e.getY());
			timer.start();
			timer2.stop();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (selectedNode != null) {
				selectedNode.mouseDragged(e);
				repaint();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

	}
	
	class SimpleMouseListener implements MouseListener{

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			timer.stop();
			timer2.start();
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}
		
	}
	
	
	private JMenuBar createMenuBar(){
		JMenu menu = new JMenu("SHAPE");
		//menu.setText("SHAPE");
		//menu.setHorizontalTextPosition(SwingConstants.CENTER);
		//menu.setHorizontalAlignment(SwingConstants.CENTER);
		menu.addMouseListener(new SimpleMouseListener());
		
		for (String s: new String[] {"heart", "star", "rectangle", "triangle","doubleTriangle" })
		{
			// add this menu item to the menu
			JMenuItem mi = new JMenuItem(s);
			// set the listener when events occur
			mi.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if(selectedNode != null){
						JMenuItem mi = (JMenuItem)e.getSource();
						if(mi.getText() == "heart"){
							selectedNode.setShape(screensaver.Main.heart);
						}
						if(mi.getText() == "star"){
							selectedNode.setShape(screensaver.Main.star);
						}
						if(mi.getText() == "rectangle"){
							selectedNode.setShape(screensaver.Main.rectangle);
						}
						if(mi.getText() == "triangle"){
							selectedNode.setShape(screensaver.Main.triangle);
						}
						if(mi.getText() == "doubleTriangle"){
							selectedNode.setShape(screensaver.Main.doubleTriangle);
						}
					}
				}
			});
			mi.addMouseListener(new SimpleMouseListener());
			// add this menu item to the menu
			menu.add(mi);
		}
		
		JMenuBar menubar = new JMenuBar();
		menubar.setLayout(new GridLayout(1,1));
		menubar.setBorder(BorderFactory.createLineBorder(Color.black));
		menubar.add(menu);
		return menubar;
	}
	

}