package com.gamblore.androidpunk.entities;

import java.util.Vector;

import net.androidpunk.Entity;
import net.androidpunk.FP;
import net.androidpunk.Graphic;
import net.androidpunk.graphics.atlas.AtlasGraphic;
import net.androidpunk.graphics.atlas.GraphicList;
import net.androidpunk.masks.Hitbox;

public class StaticDanger extends Entity {
	
	private int mAngle;
	private float mEnableTime = 0;
	private float mEnableCounter;
	
	public StaticDanger(int x, int y, int width, int height) {
		this(x, y, width, height, 0);
	}

	public StaticDanger(int x, int y, int width, int height, int angle) {
		super(x, y);
		
		mAngle = angle;
		
		switch(angle) {
		case 0:
			setMask(new Hitbox(width, height));
			break;
		case 90:
			setMask(new Hitbox(height, width, -height, 0));
			break;
		case 180:
			setMask(new Hitbox(width, height, -width, -height));
			break;
		case 270:
			setMask(new Hitbox(height, width, 0, -width));
			break;
		}
		
		setType("danger");
		mEnableCounter = 0;
	}

	@Override
	public void setGraphic(Graphic g) {
		super.setGraphic(g);
		if (g instanceof AtlasGraphic) {
			((AtlasGraphic)g).angle = mAngle;
		} else if (g instanceof GraphicList) {
			GraphicList list = (GraphicList)g;
			Vector<Graphic> vector = list.getChildren();
			for (int i = 0; i <  vector.size(); i++) {
				Graphic item = vector.get(i);
				if (item instanceof AtlasGraphic) {
					((AtlasGraphic)item).angle = mAngle;
				}
			}
		}
	}

	@Override
	public void update() {
		super.update();
		
		if (mEnableTime != 0) {
			mEnableCounter -= FP.elapsed;
			if (mEnableCounter <= 0) {
				toggleEnabled();
				mEnableCounter = mEnableTime;
			}
		}
	}
	
	public void setEnabledTime(float t) {
		mEnableTime = t;
		mEnableCounter = t;
		if (t == 0) {
			collidable = true;
			visible = true;
		}
	}
	
	public void toggleEnabled() {
		collidable = !collidable;
		visible = collidable;
	}
}
