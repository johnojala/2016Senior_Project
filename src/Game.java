import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;
import java.util.HashMap;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioLoader;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;
/**
 * Base game class. This class is the first to run during program startup and acts
 * as the root of the game control logic.
 * @author John
 */
public class Game {
	/* Base game variables. These are needed for the basics of the framework to function. */
	/** Hash map for referencing textures that have been loaded. */
	private HashMap<String, Texture> textureMap = new HashMap<String, Texture>(15);
	/** Hash map for referencing audio files that have been loaded. */
	private HashMap<String, Audio> soundMap = new HashMap<String, Audio>();
	/** Indicates whether the game is to continue running and processing logic. Set to false to end the program. */
	private boolean gameRunning = true;
	/** The time at which the last rendering loop started from the point of view of the game logic. */
	private long lastLoopTime = getTime();
	/** The time since the last record of FPS. */
	private long lastFpsTime;
	/** The recorded fps. */
	private int fps;
	/** indicated whether that game should be running in full-screen mode. Default is false. */
	private boolean fullscreen = false;
	/** Mouse movement left(-) or right(+) */
	private int mouseX = 0;
	/** Mouse movement up(-) or down(+) */
	private int mouseY = 0;

	/* Static game control and logic variables */
	private static long timerTicksPerSecond = Sys.getTimerResolution();
	
	/**
	 * Enumeration for the current game mode when determining logic control within the main loop.
	 * Additional values can be added as new game modes are developed.
	 * @author John Ojala
	 */
	public enum GameControl {
		MainMenu,
		BlockMatchStandard
	}
	
	/** The current game mode within the main logic loop. */
	GameControl activeGameMode = GameControl.MainMenu;
	
	/* game control settings */
	/** Indicates whether the keyboard can be for input in the game */
	@SuppressWarnings("unused")
	private final boolean useKeyboard = true;
	/** Indicates whether the mouse can be used for input in the game */
	private final boolean useMouse = false;
	/** Indicates whether a game controller can be used for input in the game */
	@SuppressWarnings("unused")
	private final boolean useController = false;
	/** Set whether the mouse cursor should be captured during game play */
	private final boolean captureMouse = true;
	/** Indicates the currently selected option at the main menu */
	private int mainMenuSelection = 0;

	/* Game specific settings */
	/** The text that is shown in the title of the window */
	private final String WINDOW_TITLE = "Block Breaker";
	
	/* Define sound variables */ 
	// TODO: Add all sound/music variable objects
	/** List of all sounds that can be played */
	private String[] soundEffectResource = { };
	/** List of all background sounds that will be played */
	private String[] soundBackgroundResource = { };
	
	/** All texture objects that will be used */
	private final String[][] texLoadList = {
			new String[] { "menubar", "media/mbar.png" },
			new String[] { "menucursor", "media/menu_selector.png" },
			new String[] { "blocksheet", "media/puzzleAssets_sheet.png" },
			new String[] { "menubartext", "media/mbartext.png" },
			new String[] { "blue_ui", "media/blueSheet.png" }, 
			new String[] { "green_ui", "media/greenSheet.png" },
			new String[] { "red_ui", "media/redSheet.png" },
			new String[] { "yellow_ui", "media/yellowSheet.png" },
			new String[] { "grey_ui", "media/greysheet.png" },
			new String[] { "yellowtiles", "media/spritesheet_tilesYellow.png" }
	};
	
	/* Menu display and control variables */
	private int cursorPos = 0;
	private Sprite[] selector = new Sprite[2];
	private Sprite optionFrameTop;
	private Sprite optionFrameMid;
	private Sprite optionFrameBottom;
	private Sprite optionBox;
	
	private Thread gameModeLoader = null;
	
	/** The time remaining (milliseconds) until the next movement input can be read. */
	private long movementInputDelay = 0;
	/** The minimum time (milliseconds) to wait after receiving movement input before processing further input.
	 * This is the sensitivity of the input. */
	private long movementInputDelayTimer = 150;   
	
	private GameMode game;
	
	/**
	 * Get the high resolution time in milliseconds
	 * @return The high resolution time in milliseconds
	 */
	public static long getTime() {
		// we get the "timer ticks" from the high resolution timer
		// multiply by 1000 so our end result is in milliseconds
		// then divide by the number of ticks in a second giving
		// a clear time in milliseconds
		return (Sys.getTime() * 1000) / timerTicksPerSecond;
	}
	
	/**
	 * Sleep for a fixed number of milliseconds.
	 * @param duration The amount of time in milliseconds to sleep for
	 */
	public static void sleep(long duration) {
		try {
			Thread.sleep( (duration * timerTicksPerSecond) / 1000);
		} catch (InterruptedException e) {
			// do nothing
		}
	}
	
	private boolean setDisplayMode() {
		try {
			// get modes
			DisplayMode [] dm = org.lwjgl.util.Display.getAvailableDisplayModes(Global.winWidth, Global.winHeight, -1, -1, -1, -1, 60, 60);
			org.lwjgl.util.Display.setDisplayMode(dm, new String[] {
					"width=" + Global.winWidth, 
					"height=" + Global.winHeight,
					"freq=" + 60,
					"bpp=" + org.lwjgl.opengl.Display.getDisplayMode().getBitsPerPixel()
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to enter fullscreen, continuing in windowed mode.");
		}
		return false;
	}

	
	/**
	 * Initialize OpenGL components and set OpenGL environment variables.
	 */
	private void initGL() {
		try {
			setDisplayMode();
			Display.setTitle(WINDOW_TITLE);
			Display.setFullscreen(fullscreen);
			Display.create();
			
			glViewport(0, 0, Global.glEnvWidth, Global.glEnvHeight);

			// Initialize GL matrices
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			// Sets 2D mode; no perspective
			glOrtho(0, Global.glEnvWidth, Global.glEnvHeight, 0, -1, 1);

			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();


			// disable the OpenGL depth test since only 2D graphics are used
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_LIGHTING);
			glDisable(GL_FOG);
			glDisable(GL_CULL_FACE);
			
			// clear the screen
			glClearColor(0f, 0f, 0f, 0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// enable textures
			glEnable(GL_TEXTURE_2D);
			// Enable alpha processing for textures
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_BLEND);
		} catch (LWJGLException glErr) {
			System.out.println("Game exiting - exception in initialization:");
			glErr.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Initialize game-specific variables: player data, enemy data, sounds, and textures.
	 */
	private void initComponents() {
		// grab the mouse (hides the cursor while playing)
		Mouse.setGrabbed(captureMouse);
		// TODO: add all game variables to be loaded/initialized at the start of the program
		// Setup default game controls.
		Global.setKeyMap(Global.GameControl.UP, Keyboard.KEY_UP);
		Global.setKeyMap(Global.GameControl.DOWN, Keyboard.KEY_DOWN);
		Global.setKeyMap(Global.GameControl.LEFT, Keyboard.KEY_LEFT);
		Global.setKeyMap(Global.GameControl.RIGHT, Keyboard.KEY_RIGHT);
		Global.setKeyMap(Global.GameControl.SELECT, Keyboard.KEY_X);
		Global.setKeyMap(Global.GameControl.SELECT, Keyboard.KEY_RETURN);
		Global.setKeyMap(Global.GameControl.SELECT, Keyboard.KEY_SPACE);
		Global.setKeyMap(Global.GameControl.CANCEL, Keyboard.KEY_W);
		Global.setKeyMap(Global.GameControl.CANCEL, Keyboard.KEY_ESCAPE);
		
		
		// Load all used textures into memory so the game will not be slowed down by loading textures later
		Texture tex;
		String type; // holds file type extension
		String source; // absolute file path to resource
		for (String ref[] : texLoadList) {
			
			type = ref[1].substring(ref[1].lastIndexOf('.')).toUpperCase();
			tex = null;
			source = ref[1];
			 try {
				 source = FileResource.requestResource(ref[1]);
				 tex = TextureLoader.getTexture(type, ResourceLoader.getResourceAsStream(ref[1]));
				 if ( textureMap.putIfAbsent(ref[0], tex) != null) {
					 // report error, attempting to add duplicate key entry
					 System.out.printf("Attempting to load multiple textures to key [%s]", ref[0]);
					 System.out.printf("Texture resource [%s] not loaded.", ref[1]);
				 }
			 } catch (IOException e) {
				 System.out.printf("Unable to load texture resource %s\n", source);
				 e.printStackTrace();
				 System.exit(-1);
			 }
			 textureMap.put(source, tex);
		}
		
		// Example code for loading textures with Sprite objects
		/* Loading sprites where the complete texture will be drawn to the screen
		menuBar = new Sprite( 
				textureMap.get("menubar"), // texture reference name
				new int[] { 190, 48 } // draw size in the GL environment
			);
		
		 * Loading sprites where only a subsection of the textures will be drawn on the screen
		testBlock = new Sprite(
				textureMap.get("blocksheet"), // texture reference name
				new int[] { 212, 431 }, // {top, left} stating point
				new int[] { 32, 32}, // {width, height} counting left, down
				new int[] { 32, 32 } // draw size in the GL environment
			); //*/
		// TODO: Load all Sprite objects for menu navigation
		optionFrameTop = new Sprite(
				textureMap.get("blue_ui"),
				new int[] { 0, 49 },
				new int[] { 190, 20 },
				new int[] { 250, 20 }
			);
		optionFrameMid = new Sprite(
				textureMap.get("blue_ui"),
				new int[] { 0, 59 },
				new int[] { 190, 20 },
				new int[] { 250, 300 }
			);
		optionFrameBottom = new Sprite(
				textureMap.get("blue_ui"),
				new int[] { 0, 69 },
				new int[] { 190, -20 },
				new int[] { 250, 20 }
			);
		
		optionBox = new Sprite(
				textureMap.get("green_ui"),
				new int[] { 0, 0 },
				new int[] { 190, 48 },
				new int[] { 190, 48 }
			);
		
		selector[0] = new Sprite( // left-side arrow
				textureMap.get("grey_ui"),
				new int[] { 39, 478 },
				new int[] { 38, 30 },
				new int[] { 38, 30 }
			);
		selector[1] = new Sprite( // right-side arrow
				textureMap.get("grey_ui"),
				new int[] { 0, 478 },
				new int[] { 38, 30 },
				new int[] { 38, 30 }
			);
		
		
		Audio sound;
		for (String ref : soundEffectResource) {
			sound = null;
			source = ref;
			try {
				source = FileResource.requestResource(ref);
				type = source.substring(source.lastIndexOf('.')).toUpperCase(); 
				sound = AudioLoader.getAudio(type, ResourceLoader.getResourceAsStream(source));
				soundMap.put(ref, sound);
			} catch (IOException e) {
				System.out.printf("Unable to load sound resource: %s\n%s", source, e.getMessage());
				System.out.println(e.getMessage());
			}
		}
		
		// TODO: add static class initializers
		Block.initializeBlocks(textureMap);
		
	}
	
	
	/*
	public static String requestResource(String file) throws IOException { 
		Path resourcePath;
		Path reqPath = Paths.get(file).toAbsolutePath();
		System.out.printf("Requesting resource: %s...", file);
		resourcePath = reqPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
		System.out.println("Success.");
		System.out.printf("\t%s\n", resourcePath.toString());
		return resourcePath.toString();
	} //*/

	
	/**
	 * Attempts to get a preloaded texture. Attempting to request a texture that
	 * has not been loaded will return null.
	 * @param reference File reference to the texture to load.
	 * @return The loaded OpenGL texture, null if the referenced texture was not available.
	 */
	public Texture getTexture(String reference) {
		if (textureMap.containsKey(reference)) {
			return textureMap.get(reference);
		}
		return null;
	}
	
	/*
	private void processInput() {
		// Input needs will vary between game modes, and menus within each mode, so input handling will
		// need to be checked in each specific section.
		
		/* Get mouse movement on the X and Y axis
		 * Can only call getDX and getDY once each per game loop.
		 * Additional calls will return 0 or a very small value as there will have been 
		 * zero or minimal movement of the mouse within the specific game loop. 
		 *
		if (useMouse) {
			mouseX = Mouse.getDX();
			mouseY = Mouse.getDY();
			int moveX = 0;
			int moveY = 0;
		}
		
		// This code, in whole or part, should be used in individual control loops 
		// to check against registered input for processing
		if (Global.getControlActive(Global.GameControl.LEFT)) { 
			;
		}
		if (Global.getControlActive(Global.GameControl.RIGHT)) { 
			;
		}
		if (Global.getControlActive(Global.GameControl.UP)) {
			;
		}
		if (Global.getControlActive(Global.GameControl.DOWN)) {
			cursorPos++;
			
		}
		if (Global.getControlActive(Global.GameControl.SELECT)) {
			;
		}
		if (Global.getControlActive(Global.GameControl.CANCEL)) {
			gameRunning = false;
		}
		if (Global.getControlActive(Global.GameControl.SPECIAL)) {
			;
		}
		
	} //*/
	
	/**
	 * Determine the delay time since the last frame render, get the player input,
	 * move and draw entities, add, and remove necessary entities from the lists 
	 * of maintained entities. 
	 */
	private void renderGL() {
		// cap framerate to 60 fps
		Display.sync(60);
		/* determine how long it has been since the last update
		 * this will be used to calculate how far the entities
		 * should move this loop  */
		/* The time in milliseconds since the last update */
		Global.delta = getTime() - lastLoopTime;
		lastLoopTime = getTime();
		lastFpsTime += Global.delta;
		fps++;
		// update the FPS counter if a second has passed
		if (lastFpsTime >= 1000) {
			Display.setTitle( String.format("%s (FPS: %d)", WINDOW_TITLE, fps) );
			lastFpsTime = 0;
			fps = 0;
		}

		/* check and handle input controls */
		// processInput(); 
		/* input checking is handled within individual game loops, where only the necessary
		 * inputs will be checked.
		 */
		
		/* Checks for an active menu that will be drawn in place of normal game code. 
		 * Menu comments are only suggestions, and any number can be added. Each menu
		 * function is responsible for changing the activeMenu variable to represent
		 * the current function call
		 */
		switch (activeGameMode) {
		case MainMenu:
			// TODO: Main menu draw and logic
			
			// This code acts as a proof-of-concept for adjusting screen output from control input
			if (movementInputDelay <= 0) {
				if (Global.getControlActive(Global.GameControl.LEFT)) { ; }
				if (Global.getControlActive(Global.GameControl.RIGHT)) { ; }
				if (Global.getControlActive(Global.GameControl.UP)) {
					cursorPos--;
					if (cursorPos < 0) {
						cursorPos = 2;
					}
					movementInputDelay = movementInputDelayTimer;
				}
				if (Global.getControlActive(Global.GameControl.DOWN)) {
					cursorPos++;
					if (cursorPos > 2) {
						cursorPos = 0;
					}
					movementInputDelay = movementInputDelayTimer;
				}
			} else if (movementInputDelay > 0) {
				movementInputDelay -= Global.delta;
			}
			if (Global.getControlActive(Global.GameControl.SELECT)) {
				switch (cursorPos) {
					case 0:
						game = new BlockBreakStandard();
						break;
					case 1:
						
						break;
					case 2:
						gameRunning = false;
						break;
				}
			}
			if (Global.getControlActive(Global.GameControl.CANCEL)) {
				cursorPos = 2;
			}
			if (Global.getControlActive(Global.GameControl.SPECIAL)) {
				;
			}
			/*
			menuBar.draw(100, 100);
			menuBarWithText.draw(100, 250);
			cursor.draw(150, cursorPos * 50 + 100);
			testBlock.draw(200, 100);
			//*/
			// Draw the frame that will contain the option boxes
			optionFrameTop.draw(150, 150);
			optionFrameMid.draw(150, 170);
			optionFrameBottom.draw(150, 470);
			
			// Draw the option boxes
			optionBox.draw(180, 180);
			optionBox.draw(180, 250);
			optionBox.draw(180, 320);
			
			selector[0].draw(160, 187 + cursorPos * 70);
			selector[1].draw(351, 187 + cursorPos * 70);
			
			
			break;
		case BlockMatchStandard:
			// TODO: function call to class handling this game mode.
			break;
		default:
			switch(game.getState()) { 
				case NOT_LOADED:
					gameModeLoader = new Thread(new GameModeLoader(textureMap, game));
					break;
				case LOADING_ASSETS:
					// TODO: game mode loading indicator
				case LOADING_DONE:
					try {
						gameModeLoader.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// break statement is intentionally missing here
				case READY:
					game.run();
					break;
				case UNLOADING:
					// TODO: handle unloading display
					game.cleanup();
					break;
				case FINALIZED:
					game = null;
					activeGameMode = GameControl.MainMenu;
					break;
				default:
					break;
			}
			break;
		}
		
		// an exit key is strongly recommended if mouse capture is enabled
		if ( Display.isCloseRequested() || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			gameRunning = false; // indicate that the game is no longer running
		}
		
	}
	
	/**
	 * Loads a sound file into memory for faster access at runtime.
	 * @param file 
	 * @return true if the file was loaded successfully
	 */
	private boolean loadSound(String file) {
		String source = file, type;
		Audio sound = soundMap.get(file);
		if (sound != null) { return true; } // sound has already been loaded
		
		try {
			source = FileResource.requestResource(file);
			type = source.substring(source.lastIndexOf('.')).toUpperCase(); 
			sound = AudioLoader.getAudio(type, ResourceLoader.getResourceAsStream(source));
			soundMap.put(file, sound);
		} catch (IOException e) {
			System.out.printf("Unable to load sound resource: %s\n%s", source, e.getMessage());
			return false;
		}
		return true;
	}

	public Audio getSound(String ref) {
		return soundMap.get(ref);
	}
	
	/**
	 * Plays the audio as a sound effect. No effect if the value passed is null.
	 * @param sfx The audio to be played
	 */
	private void playSoundEffect(String ref) {
		Audio sfx = getSound(ref);
		if (sfx == null) { return; } // check that sfx is a defined sound object
		sfx.playAsSoundEffect(1.0f, 1.0f, false);
		return ;
	}
	
	/**
	 * Plays the audio as repeating background music. No effect if the value passed is null.
	 * @param music The audio to be played in the background
	 */
	private void playSoundMusic(Audio music) {
		if (music == null) { return; }
		// TODO: check for and stop previously playing music if any
		music.playAsMusic(1.0f, 1.0f, true);
		return ;
	}

	public Game(boolean runFullscreen) {
		fullscreen = runFullscreen;
		initGL(); // setup OpenGL
		initComponents(); // setup game variables
	}
	
	public void run() {
		while (gameRunning) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			
			renderGL();
			Display.update();
		}
		// release all textures loaded
		for (String ref : textureMap.keySet()) {
			textureMap.get(ref).release();
		}
		textureMap.clear();
	}
	
	public static void main(String[] args) {
		System.out.println("Use -fullscreen for fullscreen mode.");
		new Game( (args.length > 0) && args[0].equalsIgnoreCase("-fullscreen") ).run();

	}

}

class GameModeLoader implements Runnable {
	private final HashMap<String, Texture> textureMap;
	private final GameMode mode;
	
	public GameModeLoader(HashMap<String, Texture> texMap, GameMode gm) {
		textureMap = texMap;
		mode = gm;
	}
	
	public void run() {
		mode.initialize(textureMap);
	}
}
