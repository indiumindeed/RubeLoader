package com.mangecailloux.rube;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;

/**
 * Spatial for drawing polygon based textures.  Useful for fixture definitions.
 * 
 * @author tescott
 *
 */
public class PolySpatial {
	private PolygonSprite mSprite;
	
	public static final float PIXELS_PER_METER = 32f;
	
	public PolySpatial(PolygonRegion region, Color color) {
		mSprite = new PolygonSprite(region);
		mSprite.setColor(color);
		mSprite.setSize(region.getRegion().getRegionWidth()/PIXELS_PER_METER,region.getRegion().getRegionHeight()/PIXELS_PER_METER);
	}

	public void render(PolygonSpriteBatch batch, float delta) {
		mSprite.draw(batch);
	}

}
