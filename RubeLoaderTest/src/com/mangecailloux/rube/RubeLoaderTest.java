package com.mangecailloux.rube;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Array;
import com.mangecailloux.rube.loader.RubeSceneLoader;
import com.mangecailloux.rube.loader.serializers.utils.RubeImage;


/**
 * Use the left-click to pan.  Scroll-wheel zooms.
 * 
 * @author cvayer, tescott
 *
 */
public class RubeLoaderTest implements ApplicationListener, InputProcessor {
	private OrthographicCamera camera;
	private RubeSceneLoader	loader;
	private RubeScene	scene;
	private Box2DDebugRenderer debugRender;
	private SpriteBatch       batch;
	private Array<SimpleSpatial> spatials; // used for rendering rube images
	
	private static final Vector2 mTmp = new Vector2(); // shared by all objects
	
	// used for pan and scanning with the mouse.
	private Vector3 mCamPos;
	private Vector3 mCurrentPos;
	
	@Override
	public void create() {		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		Gdx.input.setInputProcessor(this);
		
		mCamPos = new Vector3();
		mCurrentPos = new Vector3();
		
		camera = new OrthographicCamera(50, 50*h/w);
		
		loader = new RubeSceneLoader();
		
		scene = loader.loadScene(Gdx.files.internal("data/palm.json"));
		
		debugRender = new Box2DDebugRenderer();
		
		batch = new SpriteBatch();
		
		createSpatialsFromRubeImages(scene);
		
		//
		// example of custom property handling
		//
		Array<Body> bodies = scene.getBodies();
		if ((bodies != null) && (bodies.size > 0))
		{
			for (int i=0; i < bodies.size; i++)
			{
				Body body = bodies.get(i);
				String gameInfo = scene.getCustom(body, "GameInfo", "");
				System.out.println("GameInfo custom property: " + gameInfo);
			}
		}
		
		scene.clear(); // no longer need any scene references
	}

	@Override
	public void dispose() 
	{
	}

	@Override
	public void render() {		
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		scene.step();

		if ((spatials != null) && (spatials.size > 0))
		{
			batch.setProjectionMatrix(camera.projection);
			batch.setTransformMatrix(camera.view);
			batch.begin();
			for (int i = 0; i < spatials.size; i++)
			{
				spatials.get(i).render(batch, 0);
			}
			batch.end();
		}
		
		debugRender.render(scene.world, camera.combined);
	}
	
	
	/**
	 * Creates an array of SimpleSpatial objects from RubeImages.
	 * 
	 * @param scene2
	 */
	private void createSpatialsFromRubeImages(RubeScene scene) {

		Array<RubeImage> images = scene.getImages();
		if ((images != null) && (images.size > 0))
		{
			spatials = new Array<SimpleSpatial>();
			for (int i = 0; i < images.size; i++)
			{
				RubeImage image = images.get(i);
				mTmp.set(image.width,image.height);
				SimpleSpatial spatial = new SimpleSpatial(new Texture("data/" + image.file), image.body, Color.WHITE, mTmp, image.center, image.angleInRads * MathUtils.radiansToDegrees);
				spatials.add(spatial);
			}
		}
	}


	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public boolean keyDown(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		mCamPos.set(screenX,screenY,0);
		camera.unproject(mCamPos);
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		mCurrentPos.set(screenX,screenY,0);
		camera.unproject(mCurrentPos);
		camera.position.sub(mCurrentPos.sub(mCamPos));
		camera.update();
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		camera.zoom += (amount * 0.1f);
		if (camera.zoom < 0.1f)
		{
			camera.zoom = 0.1f;
		}
		camera.update();
		return true;
	}
}
