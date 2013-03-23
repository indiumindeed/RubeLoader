RubeLoader
==========
This is a RUBE scene JSON loader for libGdx.  It reads JSON data output by RUBE and creates and populates
a Box2D world with the bodies, joints, and fixtures defined therein.  It includes support for custom properties
and images.   

This repo contains a fully self-contained Libgdx test for reference.  It also contains a reference
for Box2DControllers (https://github.com/tescott/box2dcontrollers).

See also https://github.com/cvayer/gdx-rube for a similar solution.

About RUBE
==========
From https://www.iforce2d.net/rube/:

R.U.B.E stands for Really Useful Box2D Editor. This editor allows you to graphically manipulate 
a Box2D world and save it to a file. You can then load the saved file in your game/app and run the world.

General
=======
The loader consists of several serializers to read in objects from the RUBE JSON output:

	* Body
	* Fixture
	* Image
	* Joint
	* World
	* RubeWorld
	* Vector2

Creating a physics world populated with Box2D objects only takes two lines:

		RubeSceneLoader loader = new RubeSceneLoader();
		RubeScene scene = loader.loadScene(Gdx.files.internal("data/palm.json"));

Several scene objects are created by the loadScene method.  These objects can be used for post-processing operations:

	* scene.getWorld(): This method returns the Box2D physics world.  After loading, it is populated with the bodies, joints, and fixtures from the JSON file.
	* scene.getBodies(): This method returns an array of bodies created
	* scene.getFixtures(): This method returns an array of fixtures created
	* scene.getJoints(): This method returns an array of joints created
	* scene.getImages(): This method returns an array of RubeImages defined in the JSON file.  Note: it is up to the app to perform all rendering
	* scene.getMappedImage(): This method returns an array of all RubeImages associated with a particular Body.
	* scene.getCustom(): This method allows you to retrieve custom property info from an object.
	
If the scene data is no longer needed, scene.clear() can be executed to free up any references.  Note that this does not alter or delete the world.  It is up
to the underlying application to handle body deletions from the Box2D physics world.

RubeLoaderTest
==============
This loads in a test file that includes custom property and image info.  Use the mouse to pan and zoom.  On Android touch the screen to pan.

The included rendering is for demo purposes only.  A SimpleSpatial class is used to render image data which may or may not be attached to a Box2D body.
It is by no means efficient (textures are created for each image) and requires GL20 support for non-POT.  But, at the very least, should convey
example usage.

The palm.json scene has examples of both kinds of images - ones referenced based on a particular body and others referenced to the world origin. 

General Setup
-------------
1. Clone the repo to a local directory.
2. Open up Eclipse.  Set workspace to that local directory.
3. File > Import > General > Existing projects into workspace > Next > Browse > Ok > Select All > Finish
4. If you do not have the Box2dControllers repo cloned, you may wish to close the projects RubeLoaderTestWithBox2dControllers and RubeLoaderTestWithBox2dControllers-desktop to avoid errors.

Android Setup
-------------
1. You may see an error if you don't have the same SDK installed.  No worries!  Right-click RubeLoaderTest-Android > Properties > Android.  Check installed SDK.  Click ok.
2. Right-click RubleLoaderTest-Android > Run As.. > Android Application
3. If you have an Android device connected to your machine, it should automatically install and launch.

Desktop Setup
-------------
1. Right-click on RubeLoaderTest-desktop > Run As... > Java Application
2. Select "RubeLoaderTestDesktop". 

Box2dController Setup
---------------------
1. Clone https://github.com/tescott/box2dcontrollers
2. Import the Box2DControllers into your workspace.
3. Resolve any dependency issues.
4. Right-click on RubeLoaderTestWithBox2dCotnrollers-desktop > Run As > Java Application > RubeLoaderTestDesktop

Required Software:
==================
- Eclipse
- Android SDK
- RUBE: https://www.iforce2d.net/rube/ (optional)
- Box2dControllers: https://github.com/tescott/box2dcontrollers (optional)

Limitations
===========
Currently does not support retrieving objects by name.

TODO List
=========
1. Add handling via object name
2. Add pinch-zooming for Android target

Screenshot / video of test example
==================================
![Screenshot](https://raw.github.com/tescott/RubeLoader/master/screenshot.png)
[![Youtube](https://raw.github.com/tescott/RubeLoader/master/youtubescreenshot.png)](http://youtu.be/hgo7Pxtcoyk)
