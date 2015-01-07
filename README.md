# ScreenSaver
## Learning Goals: 
Use affine transforms and interactor trees in a meaningful way to implement 2D graphics and direct manipulation.

Develop a custom component with its own appearance and interactions.

Demonstrate good user interface and user interaction design skills.

## video:
https://www.youtube.com/watch?v=FdIbFvcGxkA

## Basic Requirements:

Use an instance of javax.swing.Timer.Timer to animate the scene graph in the "running" mode. Note that the root node does not spin around its own origin. Start the animation when the mouse leaves the window; stop it when the mouse re-enters the window.

Enable the user to reposition a node and its descendents by dragging it with the mouse.

Modify the code to reposition a node so that the orientation of the node towards the parent is maintained. In the video, note how the point of the heart continues to point towards the parent node. Children of the repositioned node go along for the ride, keeping their relative position to the node being dragged.

Enable the user to scale an individual node. The children of the node maintain their relative position and size.

Enable the user to add new child nodes to any of the nodes already part of the screensaver.

Enable the user to change the shape and colour of a node to a shape selected from those available in Main.java. . Add at least one new shape.
