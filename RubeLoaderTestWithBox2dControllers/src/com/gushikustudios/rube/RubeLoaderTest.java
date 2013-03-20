package com.gushikustudios.rube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.gushikustudios.box2d.controllers.B2BuoyancyController;
import com.gushikustudios.box2d.controllers.B2Controller;
import com.gushikustudios.box2d.controllers.B2GravityController;
import com.gushikustudios.rube.RubeScene;
import com.gushikustudios.rube.loader.RubeSceneLoader;
import com.gushikustudios.rube.loader.serializers.utils.RubeImage;

/**
 * Use the left-click to pan. Scroll-wheel zooms.
 * 
 * @author cvayer, tescott
 * 
 */
public class RubeLoaderTest implements ApplicationListener, InputProcessor, ContactListener
{
   private OrthographicCamera camera;
   private RubeSceneLoader loader;
   private RubeScene scene;
   private Box2DDebugRenderer debugRender;

   private Array<SimpleSpatial> spatials; // used for rendering rube images
   private Array<PolySpatial> polySpatials;
   private Map<String, Texture> textureMap;
   private Map<Texture, TextureRegion> textureRegionMap;

   private static final Vector2 mTmp = new Vector2(); // shared by all objects

   private SpriteBatch batch;
   private PolygonSpriteBatch polygonBatch;

   // used for pan and scanning with the mouse.
   private Vector3 mCamPos;
   private Vector3 mCurrentPos;

   private World mWorld;

   private Array<B2Controller> mB2Controllers;

   private float mAccumulator; // time accumulator to fix the physics step.

   private int mVelocityIter = 8;
   private int mPositionIter = 3;
   private float mSecondsPerStep = 1 / 60f;

   private static final float MAX_DELTA_TIME = 0.25f;

   @Override
   public void create()
   {
      float w = Gdx.graphics.getWidth();
      float h = Gdx.graphics.getHeight();

      Gdx.input.setInputProcessor(this);

      mB2Controllers = new Array<B2Controller>();

      mCamPos = new Vector3();
      mCurrentPos = new Vector3();

      camera = new OrthographicCamera(100, 100 * h / w);
      camera.position.set(50,50,0);
      camera.zoom = 1.8f;
      camera.update();
      

      loader = new RubeSceneLoader();

      scene = loader.loadScene(Gdx.files.internal("data/palmcontrollers.json"));

      debugRender = new Box2DDebugRenderer();

      batch = new SpriteBatch();
      polygonBatch = new PolygonSpriteBatch();

      textureMap = new HashMap<String, Texture>();
      textureRegionMap = new HashMap<Texture, TextureRegion>();

      createSpatialsFromRubeImages(scene);
      createPolySpatialsFromRubeFixtures(scene);

      mWorld = scene.world;
      // configure simulation settings
      mVelocityIter = scene.velocityIterations;
      mPositionIter = scene.positionIterations;
      if (scene.stepsPerSecond != 0)
      {
         mSecondsPerStep = 1f / scene.stepsPerSecond;
      }
      mWorld.setContactListener(this);

      //
      // example of custom property handling
      //
      Array<Body> bodies = scene.getBodies();
      if ((bodies != null) && (bodies.size > 0))
      {
         for (int i = 0; i < bodies.size; i++)
         {
            Body body = bodies.get(i);
            String gameInfo = scene.getCustom(body, "GameInfo", (String)null);
            if (gameInfo != null)
            {
            	System.out.println("GameInfo custom property: " + gameInfo);
            }
         }
      }

      // Instantiate any controllers that are in the scene
      Array<Fixture> fixtures = scene.getFixtures();
      if ((fixtures != null) && (fixtures.size > 0))
      {
         for (int i = 0; i < fixtures.size; i++)
         {
            Fixture fixture = fixtures.get(i);
            int controllerType = scene.getCustom(fixture, "ControllerType", 0);
            switch (controllerType)
            {
               case B2Controller.BUOYANCY_CONTROLLER:
                  // only allow polygon buoyancy controllers for now..
                  if (fixture.getShape().getType() == Shape.Type.Polygon)
                  {
                     float bodyHeight = fixture.getBody().getPosition().y;
                     // B2BuoyancyController b2c = new B2BuoyancyController();

                     // need to calculate the fluid surface height for the buoyancy controller
                     PolygonShape shape = (PolygonShape) fixture.getShape();
                     shape.getVertex(0, mTmp);
                     float maxHeight = mTmp.y + bodyHeight; // initialize the height, transforming to 'world' coordinates
                     
                     // find the maxHeight
                     for (int j = 1; j < shape.getVertexCount(); j++)
                     {
                        shape.getVertex(j, mTmp);
                        maxHeight = Math.max(maxHeight, mTmp.y + bodyHeight); // transform to world coordinates
                     }
                     B2BuoyancyController b2c = new B2BuoyancyController(
                              B2BuoyancyController.DEFAULT_SURFACE_NORMAL, // assume up
                              scene.getCustom(fixture, "ControllerVelocity",B2BuoyancyController.DEFAULT_FLUID_VELOCITY),
                              mWorld.getGravity(),
                              maxHeight,
                              fixture.getDensity(),
                              scene.getCustom(fixture, "LinearDrag", B2BuoyancyController.DEFAULT_LINEAR_DRAG),
                              scene.getCustom(fixture, "AngularDrag", B2BuoyancyController.DEFAULT_ANGULAR_DRAG));
                     fixture.setUserData(b2c); // reference back to the controller from the fixture (see beginContact/endContact)
                     mB2Controllers.add(b2c); // add it to the list so it can be stepped later
                  }
                  break;

               case B2Controller.GRAVITY_CONTROLLER:
                  {
                     B2GravityController b2c = new B2GravityController();
                     b2c = new B2GravityController(scene.getCustom(fixture, "ControllerVelocity", B2GravityController.DEFAULT_GRAVITY));
                     fixture.setUserData(b2c);
                     mB2Controllers.add(b2c);
                  }
                  break;
            }
         }
      }
      scene.printStats();
      scene.clear(); // no longer need any scene references
   }

   @Override
   public void dispose()
   {
   }

   @Override
   public void render()
   {
      Gdx.gl.glClearColor(0, 0, 0, 1);
      Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

      float delta = Gdx.graphics.getDeltaTime();

      if (delta > MAX_DELTA_TIME)
      {
         delta = MAX_DELTA_TIME;
      }

      mAccumulator += delta;

      while (mAccumulator >= mSecondsPerStep)
      {
         for (int i = 0; i < mB2Controllers.size; i++)
         {
            mB2Controllers.get(i).step(mSecondsPerStep);
         }
         mWorld.step(mSecondsPerStep, mVelocityIter, mPositionIter);
         mAccumulator -= mSecondsPerStep;
      }

      if ((spatials != null) && (spatials.size > 0))
      {
         batch.setProjectionMatrix(camera.combined);
         batch.begin();
         for (int i = 0; i < spatials.size; i++)
         {
            spatials.get(i).render(batch, 0);
         }
         batch.end();
      }

      if ((polySpatials != null) && (polySpatials.size > 0))
      {
         polygonBatch.setProjectionMatrix(camera.combined);
         polygonBatch.begin();
         for (int i = 0; i < polySpatials.size; i++)
         {
            polySpatials.get(i).render(polygonBatch, 0);
         }
         polygonBatch.end();
      }

      debugRender.render(scene.world, camera.combined);
   }

   /**
    * Creates an array of SimpleSpatial objects from RubeImages.
    * 
    * @param scene2
    */
   private void createSpatialsFromRubeImages(RubeScene scene)
   {

      Array<RubeImage> images = scene.getImages();
      if ((images != null) && (images.size > 0))
      {
         spatials = new Array<SimpleSpatial>();
         for (int i = 0; i < images.size; i++)
         {
            RubeImage image = images.get(i);
            mTmp.set(image.width, image.height);
            String textureFileName = "data/" + image.file;
            Texture texture = textureMap.get(textureFileName);
            if (texture == null)
            {
               texture = new Texture(textureFileName);
               textureMap.put(textureFileName, texture);
            }
            SimpleSpatial spatial = new SimpleSpatial(texture, image.flip, image.body, image.color, mTmp, image.center,
                  image.angleInRads * MathUtils.radiansToDegrees);
            spatials.add(spatial);
         }
      }
   }

   /**
    * Creates an array of PolySpatials based on fixture information from the scene. Note that
    * fixtures create aligned textures.
    * 
    * @param scene
    */
   private void createPolySpatialsFromRubeFixtures(RubeScene scene)
   {
      Array<Body> bodies = scene.getBodies();

      if ((bodies != null) && (bodies.size > 0))
      {
         polySpatials = new Array<PolySpatial>();
         Vector2 bodyPos = new Vector2();
         // for each body in the scene...
         for (int i = 0; i < bodies.size; i++)
         {
            Body body = bodies.get(i);
            bodyPos.set(body.getPosition());

            ArrayList<Fixture> fixtures = body.getFixtureList();

            if ((fixtures != null) && (fixtures.size() > 0))
            {
               // for each fixture on the body...
               for (int j = 0; j < fixtures.size(); j++)
               {
                  Fixture fixture = fixtures.get(j);

                  String textureName = scene.getCustom(fixture, "TextureMask", (String) null);
                  if (textureName != null)
                  {
                     String textureFileName = "data/" + textureName;
                     Texture texture = textureMap.get(textureFileName);
                     TextureRegion textureRegion = null;
                     if (texture == null)
                     {
                        texture = new Texture(textureFileName);
                        texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
                        textureMap.put(textureFileName, texture);
                        textureRegion = new TextureRegion(texture);
                        textureRegionMap.put(texture, textureRegion);
                     }
                     else
                     {
                        textureRegion = textureRegionMap.get(texture);
                     }

                     // only handle polygons at this point -- no chain, edge, or circle fixtures.
                     if (fixture.getType() == Shape.Type.Polygon)
                     {
                        PolygonShape shape = (PolygonShape) fixture.getShape();
                        int vertexCount = shape.getVertexCount();
                        float[] vertices = new float[vertexCount * 2];

                        // static bodies are texture aligned and do not get drawn based off of the related body.
                        if (body.getType() == BodyType.StaticBody)
                        {
                           for (int k = 0; k < vertexCount; k++)
                           {

                              shape.getVertex(k, mTmp);
                              mTmp.rotate(body.getAngle() * MathUtils.radiansToDegrees);
                              mTmp.add(bodyPos); // convert local coordinates to world coordinates to that textures are
                                                 // aligned
                              vertices[k * 2] = mTmp.x * PolySpatial.PIXELS_PER_METER;
                              vertices[k * 2 + 1] = mTmp.y * PolySpatial.PIXELS_PER_METER;
                           }
                           PolygonRegion region = new PolygonRegion(textureRegion, vertices);
                           PolySpatial spatial = new PolySpatial(region, Color.WHITE);
                           polySpatials.add(spatial);
                        }
                        else
                        {
                           // all other fixtures are aligned based on their associated body.
                           for (int k = 0; k < vertexCount; k++)
                           {
                              shape.getVertex(k, mTmp);
                              vertices[k * 2] = mTmp.x * PolySpatial.PIXELS_PER_METER;
                              vertices[k * 2 + 1] = mTmp.y * PolySpatial.PIXELS_PER_METER;
                           }
                           PolygonRegion region = new PolygonRegion(textureRegion, vertices);
                           PolySpatial spatial = new PolySpatial(region, body, Color.WHITE);
                           polySpatials.add(spatial);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void resize(int width, int height)
   {
   }

   @Override
   public void pause()
   {
   }

   @Override
   public void resume()
   {
   }

   @Override
   public boolean keyDown(int keycode)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean keyUp(int keycode)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean keyTyped(char character)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean touchDown(int screenX, int screenY, int pointer, int button)
   {
      mCamPos.set(screenX, screenY, 0);
      camera.unproject(mCamPos);
      return true;
   }

   @Override
   public boolean touchUp(int screenX, int screenY, int pointer, int button)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean touchDragged(int screenX, int screenY, int pointer)
   {
      mCurrentPos.set(screenX, screenY, 0);
      camera.unproject(mCurrentPos);
      camera.position.sub(mCurrentPos.sub(mCamPos));
      camera.update();
      return true;
   }

   @Override
   public boolean mouseMoved(int screenX, int screenY)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean scrolled(int amount)
   {
      camera.zoom += (amount * 0.1f);
      if (camera.zoom < 0.1f)
      {
         camera.zoom = 0.1f;
      }
      System.out.println("zoom: " + camera.zoom);
      camera.update();
      return true;
   }

   @Override
   public void beginContact(Contact contact)
   {
      Fixture fixA = contact.getFixtureA();
      Fixture fixB = contact.getFixtureB();

      if ((fixA.isSensor()) && (fixA.getUserData() != null))
      {
         B2Controller b2c = (B2Controller) fixA.getUserData();
         b2c.addBody(fixB.getBody());
      }
      else if ((fixB.isSensor()) && (fixB.getUserData() != null))
      {
         B2Controller b2c = (B2Controller) fixB.getUserData();
         b2c.addBody(fixA.getBody());
      }
   }

   @Override
   public void endContact(Contact contact)
   {
      Fixture fixA = contact.getFixtureA();
      Fixture fixB = contact.getFixtureB();

      if ((fixA.isSensor()) && (fixA.getUserData() != null))
      {
         B2Controller b2c = (B2Controller) fixA.getUserData();
         b2c.removeBody(fixB.getBody());
      }
      else if ((fixB.isSensor()) && (fixB.getUserData() != null))
      {
         B2Controller b2c = (B2Controller) fixB.getUserData();
         b2c.removeBody(fixA.getBody());
      }
   }

   @Override
   public void preSolve(Contact contact, Manifold oldManifold)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void postSolve(Contact contact, ContactImpulse impulse)
   {
      // TODO Auto-generated method stub

   }
}
