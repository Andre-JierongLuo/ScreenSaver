package screensaver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Represent one Screen Save Node.
 */
public class SSNode implements MouseMotionListener, MouseListener {

	private AffineTransform trans = new AffineTransform();
	private Shape shape;
	private ArrayList<SSNode> children = new ArrayList<SSNode>();
	private SSNode parent = null;
	private String id; // for debugging printf statements
	private Point2D lastPoint = null;
	private Color color = Color.RED;
	
	private boolean focus = false;
	private Timer timerForBigger;
	private Timer timerForSmaller;
	private static int FPS = 40;
	private double r1 = 1.02;
	private double r2 = 1/r1;

	/**
	 * Create a new SSNode, given a shape and a colour.
	 */
	public SSNode(Shape s, Color color) {
		this.id = "id";
		this.shape = s;
		this.color = color;
		
		timerForBigger = new Timer(1000/FPS, new ActionListener(){
			public void actionPerformed(ActionEvent e){
				AffineTransform S = AffineTransform.getScaleInstance(r1, r1);
				AffineTransform S1 = AffineTransform.getScaleInstance(r2, r2);
				transform(S);
				for (SSNode c : children) {
					c.transformPre(S1);
				}
			}
		});
		
		timerForSmaller = new Timer(1000/FPS, new ActionListener(){
			public void actionPerformed(ActionEvent e){
				AffineTransform S = AffineTransform.getScaleInstance(r1, r1);
				AffineTransform S1 = AffineTransform.getScaleInstance(r2, r2);
				transform(S1);
				for (SSNode c : children) {
					c.transformPre(S);
				}
			}
		});
	}

	/**
	 * Set this node's shape to a new shape.
	 */
	public void setShape(Shape s) {
		this.shape = s;
	}

	/**
	 * Add a child node to this node.
	 */
	public void addChild(SSNode child) {
		child.id = this.id + "." + (this.children.size());
		this.children.add(child);
		child.parent = this;
	}

	/**
	 * Is this node the root node? The root node doesn't have a parent.
	 */
	private boolean isRoot() {
		return this.parent == null;
	}

	/**
	 * Get this node's parent node; null if there is no such parent.
	 */
	public SSNode getParent() {
		return this.parent;
	}

	/**
	 * One tick of the animation timer. What should this node do when a unit of
	 * time has passed?
	 */
	public void tick() {
		// Because the root node doesn't rotate, it'll be a special case.
		if(this.isRoot()){
			for (SSNode c : this.children) {
				c.tick();
			}
		} else {
			try {
				// back to its parent position first.
				AffineTransform t = this.getLocalTransform();
				AffineTransform tp = t.createInverse();
				this.transform(tp);
				// then rotate and translate back.
				AffineTransform R = AffineTransform.getRotateInstance(Math.toRadians(2));
				this.transform(R);
				this.transform(t);

				for (SSNode c : this.children) {
					c.tick();
				}
			} catch (NoninvertibleTransformException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	/**
	 * Does this node contain the given point (which is in window coordinates)?
	 */
	public boolean containsPoint(Point2D p) {
		AffineTransform inverseTransform = this.getFullInverseTransform();
		Point2D pPrime = inverseTransform.transform(p, null);

		return this.shape.contains(pPrime);
	}

	/**
	 * Return the node containing the point. If nodes overlap, child nodes take
	 * precedence over parent nodes.
	 */
	public SSNode hitNode(Point2D p) {
		for (SSNode c : this.children) {
			SSNode hit = c.hitNode(p);
			if (hit != null)
				return hit;
		}
		if (this.containsPoint(p)) {
			return this;
		} else {
			return null;
		}
	}

	/**
	 * Transform this node's transformation matrix by concatenating t to it.
	 */
	public void transform(AffineTransform t) {
		this.trans.concatenate(t);
	}

	public void transformPre(AffineTransform t){
		this.trans.preConcatenate(t);
	}
	
	/**
	 * Convert p to a Point2D.
	 */
	private Point2D.Double p2D(Point p) {
		return new Point2D.Double(p.getX(), p.getY());
	}

	/*************************************************************
	 * 
	 * Handle mouse events directed to this node.
	 * 
	 *************************************************************/

	@Override
	public void mouseClicked(MouseEvent e) {
		this.getFocus();
		/*
		 * double click event
		 */
		if(e.getClickCount() == 2){
			if(SwingUtilities.isLeftMouseButton(e)){
				//System.out.println("left double click => bigger");
				timerForBigger.start();
			}
			else if(SwingUtilities.isRightMouseButton(e)){
				//System.out.println("right double click => smaller");
				timerForSmaller.start();
			}
		} else {
			timerForBigger.stop();
			timerForSmaller.stop();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		this.getFocus();
		this.lastPoint = p2D(e.getPoint());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		this.lastPoint = null;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

    /**
     * Handle mouse drag event, with the assumption that we have already
     * been "selected" as the sprite to interact with.
     * This is a very simple method that only works because we
     * assume that the coordinate system has not been modified
     * by scales or rotations. You will need to modify this method
     * appropriately so it can handle arbitrary transformations.
     */
	@Override
	public void mouseDragged(MouseEvent e) {
		timerForBigger.stop();
		timerForSmaller.stop();
		
		this.getFocus();
		Point2D mouse = p2D(e.getPoint());
		if(this.isRoot()){
			// just in case if the root node rotate.
			AffineTransform t = this.getFullInverseTransform();	

			// transform curosr and lastpoint position to root node coordinate systems.
			Point2D p1 = t.transform(mouse, null);
			Point2D p2 = t.transform(lastPoint, null);
			double dx = p1.getX() - p2.getX();
			double dy = p1.getY() - p2.getY();
			this.trans.translate(dx, dy);
		} else {
			// transform mouse and the lastPoint to node'parent coordinates.
			SSNode parentNode = this.getParent();
			AffineTransform t1 = parentNode.getFullInverseTransform();	
			Point2D p1 = t1.transform(mouse, null);
			Point2D p2 = t1.transform(lastPoint, null);
			
			// get the radius change between mouse and lastPoint in node'parent coordinates.
			double dz = getRadius(p1) - getRadius(p2);
			AffineTransform R = AffineTransform.getRotateInstance(dz);
			
			/*
			 * transform mouse to node coordinates.
			 * translate the node to the correct position,
			 * then rotate itself base on the radius change in node'parent coordinates.
			 */
			AffineTransform t3 = this.getFullInverseTransform();	
			Point2D p3 = t3.transform(mouse, null);
			Point2D p4 = t3.transform(lastPoint, null);
			double dx = p3.getX() - p4.getX();
			double dy = p3.getY() - p4.getY();
			this.trans.translate(dx, dy);
			this.transform(R);
		}
		lastPoint = mouse;
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {
	}

	/**
	 * Paint this node and its children.
	 */
	public void paintNode(Graphics2D g2) {
		/*
		 * You can change this code if you wish. Based on an in-class example
		 * it's going to be really tempting. You are advised, however, not to
		 * change it. Doing so will likely bring you hours of grief and much
		 * frustration.
		 */

		// Remember the transform being used when called
		AffineTransform t = g2.getTransform();
		// transform from node's coordinate system to mouse coordinate system.
		g2.transform(this.getFullTransform());
		g2.setColor(this.color);
		g2.fill(this.shape);
		
		// if the node has the focus, then print its border in white.
		if(isFocus()){
			g2.setStroke(new BasicStroke(3));
			g2.setColor(Color.WHITE);
			g2.draw(this.shape);
		}
		
		g2.setColor(this.color);
		// Restore the transform.
		g2.setTransform(t);

		// Paint each child
		for (SSNode c : this.children) {
			c.paintNode(g2);
		}

		// Restore the transform.
		g2.setTransform(t);
	}

	/*
	 * There are a number of ways in which the handling of the transforms could
	 * be optimized. That said, don't bother. It's not the point of the
	 * assignment.
	 */

	/**
	 * Returns our local transform. Copy it just to make sure it doesn't get
	 * messed up.
	 */
	public AffineTransform getLocalTransform() {
		return new AffineTransform(this.trans);
	}

	/**
	 * Returns the full transform to this node from the root.
	 */
	public AffineTransform getFullTransform() {
		// Start with an identity matrix. Concatenate on the left
		// every local transformation matrix from here to the root.
		AffineTransform at = new AffineTransform();
		SSNode curNode = this;
		while (curNode != null) {
			at.preConcatenate(curNode.getLocalTransform());
			curNode = curNode.getParent();
		}
		return at;
	}

	/**
	 * Return the full inverse transform, starting with the root. That is, get
	 * the full transform from here to the root and then invert it, catching
	 * exceptions (there shouldn't be any).
	 */
	private AffineTransform getFullInverseTransform() {
		try {
			AffineTransform t = this.getFullTransform();
			AffineTransform tp = t.createInverse();
			return tp;
		} catch (NoninvertibleTransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new AffineTransform();
		}
	}
	
	/**
	 *  return the radius of the point in its corresponding coordinate system.
	 */
	private double getRadius(Point2D p){
		double dx = p.getX();
		double dy = p.getY();
		double pi = Math.PI;
		if(dx == 0 && dy == 0){
			return 0;
		} else if(dx > 0 && dy == 0){
			return 0;
		} else if(dx > 0 && dy > 0){
			return Math.atan(dy/dx);
		} else if(dx == 0 && dy > 0){
			return pi/2;
		} else if(dx < 0 && dy > 0){
			return pi - Math.atan(dy/Math.abs(dx));
		} else if(dx < 0 && dy == 0){
			return pi;
		} else if(dx < 0 && dy < 0){
			return pi + Math.atan(Math.abs(dy)/Math.abs(dx));
		} else if(dx == 0 && dy < 0){
			return 3*pi/2;
		} else if(dx > 0 && dy < 0){
			return 2*pi - Math.atan(Math.abs(dy)/dx);
		} 
		else return 0;
	}

	/**
	 * return true if the mouse focus is on this node. 
	 */
	public boolean isFocus(){
		return this.focus;
	}
	
	/**
	 * if mouse press or click on this node, set it has the focus.
	 */
	public void getFocus(){
		this.focus = true;
	}
	/**
	 * if mouse press outside this node, this node lost the focus.
	 */
	public void loseFocus(){
		this.focus = false;
	}
	/**
	 * return the current color.
	 */
	public Color getColor(){
		return this.color;
	}
	
	/**
	 * change the color.
	 */
	public void setColor(Color c){
		this.color = c;
	}
}
