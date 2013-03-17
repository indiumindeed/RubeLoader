package com.mangecailloux.rube;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

/**
 * Spatial for drawing polygon based textures.  Useful for fixture definitions.
 * 
 * @author tescott
 *
 */
public class PolySpatial {
	private PolygonSprite mSprite;
	private Body mBody;
	
	public static final float PIXELS_PER_METER = 32f;
	private static final Vector2 mTmp = new Vector2();
	
	public PolySpatial(PolygonRegion region, Color color) {
		mSprite = new PolygonSprite(region);
		mSprite.setColor(color);
		mSprite.setSize(region.getRegion().getRegionWidth()/PIXELS_PER_METER,region.getRegion().getRegionHeight()/PIXELS_PER_METER);
	}
	
	public PolySpatial(PolygonRegion region, Body body, Color color)
	{
		this(region,color);
		mBody = body;
		mTmp.set(mBody.getPosition());
		mSprite.setOrigin(0, 0);
	}

	public void render(PolygonSpriteBatch batch, float delta) {
		if (mBody != null)
		{
			mTmp.set(mBody.getPosition());
			mSprite.setRotation(mBody.getAngle()*MathUtils.radiansToDegrees);
			mSprite.setPosition(mTmp.x, mTmp.y);
			mSprite.draw(batch);
		}
		else
		{
			mSprite.draw(batch);
		}
	}

}
