package ca.shu.ui.lib.world.impl;

import java.awt.geom.Point2D;

import ca.shu.ui.lib.world.World;
import ca.shu.ui.lib.world.WorldLayer;
import ca.shu.ui.lib.world.WorldObject;
import edu.umd.cs.piccolo.util.PBounds;

public class WorldGround extends WorldObjectImpl implements WorldLayer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	World world;

	public WorldGround(World world) {
		super();
		this.world = world;

		this.setDraggable(false);
		this.setFrameVisible(false);
		// this.addInputEventListener(new NodeDragHandler());

	}

	public World getWorld() {
		return world;
	}

	public void addWorldObject(WorldObject node) {

		// PBounds bounds = this.getFullBounds();

		// node.setOffset(bounds.getWidth() + 50, 0);

		addChildW(node);

	}



	
	public double getGroundScale() {
		return world.getSky().getViewScale();
	}

	/*
	 * Adds a little pizzaz while accepting new objects
	 */
	public void catchObject(WorldObject wo) {
		addChildW(wo);

		Point2D finalPosition = world.skyToGround(new Point2D.Double(world
				.getScreenWidth() / 2, world.getScreenHeight() / 2));

		wo.setScale(1 / getGroundScale());

		wo.setOffset(finalPosition.getX(), finalPosition.getY()
				- (100 / getGroundScale()));

		wo.animateToPositionScaleRotation(finalPosition.getX(), finalPosition
				.getY(), 1, 0, 500);
	}
}