package com.scs.simple2dgameframework;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.scs.simple2dgameframework.audio.AudioPlayer;
import com.scs.simple2dgameframework.audio.AudioSystem;
import com.scs.simple2dgameframework.graphics.GraphicsUtils;
import com.scs.simple2dgameframework.graphics.Sprite;
import com.scs.simple2dgameframework.input.ControllerManager;

import net.java.games.input.Controller;

public abstract class Simple2DGameFramework extends Thread implements MouseListener, KeyListener, MouseMotionListener, WindowListener, MouseWheelListener, Runnable {

	public static final String ASSETS_FOLDER = "assets/";

	private int logicalWidth, logicalHeight;
	private int physicalWidth, physicalHeight;
	private Color backgroundColor = new Color(255, 255, 255);
	private boolean[] keys = new boolean[255];
	public float delta_seconds;
	private boolean isRunning = true;
	private BufferStrategy strategy;
	private Graphics2D backgroundGraphics;
	private Graphics2D graphics;
	public GameWindow frame;
	private ControllerManager controllerManager;
	private AudioSystem audioSystem;
	private Font defaultFont;
	public  GraphicsUtils graphicsUtils;
	
	private GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

	public Simple2DGameFramework(int _logicalWidth, int _logicalHeight) {
		super(Simple2DGameFramework.class.getSimpleName());

		logicalWidth = _logicalWidth;
		logicalHeight = _logicalHeight;

		System.setProperty("net.java.games.input.librarypath", new File("libs/jinput").getAbsolutePath());

		this.graphicsUtils = new GraphicsUtils();
		audioSystem = new AudioSystem();
	}


	public void setFullScreen() {
		this.physicalWidth = config.getBounds().width;
		this.physicalHeight = config.getBounds().height;
		this.createWindow();
	}


	public void setScreenSize(int w, int h) {
		this.physicalWidth = w;
		this.physicalHeight = h;
		this.createWindow();
	}


	private void createWindow() {
		frame = new GameWindow(this, this.physicalWidth, this.physicalHeight, config);
		frame.requestFocus();
		frame.requestFocusInWindow();
		do {
			strategy = frame.getBufferStrategy();
		} while (strategy == null);

		controllerManager = new ControllerManager();
		this.start();
	}


	private Graphics2D getGraphics() {
		if (graphics == null) {
			graphics = (Graphics2D) strategy.getDrawGraphics();
		}
		return graphics;
	}


	private boolean updateScreen() {
		graphics.dispose();
		graphics = null;
		strategy.show();
		Toolkit.getDefaultToolkit().sync();
		return strategy.contentsLost();
	}


	public void run() {
		BufferedImage background = config.createCompatibleImage(logicalWidth, logicalHeight, Transparency.OPAQUE);
		backgroundGraphics = (Graphics2D) background.getGraphics();

		init();

		main: while (isRunning) {
			long start = System.currentTimeMillis();

			this.controllerManager.checkForControllers();

			// Update Graphics
			do {
				Graphics2D bg = getGraphics();
				if (!isRunning) {
					break main;
				}
				renderGame(backgroundGraphics);
				bg.drawImage(background, 0, 0, this.physicalWidth, this.physicalHeight, 0, 0, logicalWidth, logicalHeight, null);
				bg.dispose();
			} while (updateScreen());

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			delta_seconds = (System.currentTimeMillis() - start)/1000f;

		}
		frame.dispose();
	}


	public void renderGame(Graphics2D g) {
		g.setColor(this.backgroundColor);
		g.fillRect(0, 0, logicalWidth, logicalHeight);

		if (defaultFont != null) {
			g.setFont(defaultFont);
		}

		draw();
		
		if (this.keys[KeyEvent.VK_TAB]) {
			// Show controller info
			int idx = 0;
			for (Controller controller : this.controllerManager.controllers) {
				g.drawString(controller.getName(), 20, 100 + (idx*20));
				idx++;
			}
		}
	}


	protected void init() {
		// To be overridden by parent
	}

	protected void draw() {
		// To be overridden by parent
	}


	protected Controller getNextNewController() {
		if (this.controllerManager.controllersAdded.size() > 0) {
			return this.controllerManager.controllersAdded.remove(0);
		}
		return null;
	}


	protected Controller getRemovedController() {
		if (this.controllerManager.controllersRemoved.size() > 0) {
			return this.controllerManager.controllersRemoved.remove(0);
		}
		return null;
	}


	protected void setBackgroundColour(int r, int g, int b) {
		backgroundColor = new Color(r, g, b);
	}


	public Sprite createSprite(String filename) {
		BufferedImage img = graphicsUtils.loadImage(filename);
		return new Sprite(this, img);
	}


	public Sprite createSprite(BufferedImage bi) {
		return new Sprite(this, bi);
	}


	public Sprite createSprite(String filename, int w, int h) {
		Sprite s = this.createSprite(filename);
		s.setSize(w, h);
		return s;
	}


	public void drawSprite(Sprite s, int x, int y) {
		if (s.img != null) {
			s.setPosition(x, y);
			backgroundGraphics.drawImage(s.img, x, y, null);
		}
	}


	public void drawSprite(Sprite s) {
		if (s.img != null) {
			this.backgroundGraphics.drawImage(s.img, (int)s.pos.x, (int)s.pos.y, this.frame);
		}
	}


	public void drawFont(String text, float x, float y) {
		backgroundGraphics.drawString(text, x, y);
	}


	public void setColour(int r, int g, int b) {
		backgroundGraphics.setColor(new Color(r, g, b));
	}


	public boolean isKeyPressed(int code) {
		return keys[code];
	}


	public void playMusic(String s) {
		audioSystem.playMusic(ASSETS_FOLDER + s);
	}


	@Override
	public void keyPressed(KeyEvent ke) {
		if (ke.getKeyCode() == KeyEvent.VK_F1) {
			// Take screenshot
			try {
				Insets insets = frame.getInsets();
				Rectangle r = new Rectangle(frame.getX() + insets.left, frame.getY() + insets.top, frame.getWidth() - insets.left-insets.right, frame.getHeight() - insets.top-insets.bottom);
				BufferedImage image = new Robot().createScreenCapture(r);
				String filename = "Screenshot_" + System.currentTimeMillis() + ".png";
				ImageIO.write(image, "png", new File(filename));
				p("Screenshot saved as " + filename);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}

		if (ke.getKeyCode() < keys.length) {
			keys[ke.getKeyCode()] = true;
		}
	}


	@Override
	public void keyReleased(KeyEvent ke) {
		if (ke.getKeyCode() < keys.length) {
			keys[ke.getKeyCode()] = false;
		}
	}


	@Override
	public void keyTyped(KeyEvent ke) {
		// For some reason this method desn't get values for the KeyEvent!?
		//throw new RuntimeException("Do not use - keycode is " + ke.getKeyCode());
	}


	@Override
	public void mouseClicked(MouseEvent me) {
	}


	@Override
	public void mouseEntered(MouseEvent me) {

	}


	@Override
	public void mouseExited(MouseEvent me) {

	}


	@Override
	public void mousePressed(MouseEvent me) {
	}


	@Override
	public void mouseReleased(MouseEvent me) {
	}


	@Override
	public void mouseDragged(MouseEvent me) {
	}


	@Override
	public void mouseMoved(MouseEvent me) {

	}


	@Override
	public void windowActivated(WindowEvent arg0) {

	}


	@Override
	public void windowClosed(WindowEvent arg0) {
	}


	@Override
	public void windowClosing(WindowEvent arg0) {
		this.isRunning = false;
		frame.dispose();

	}


	@Override
	public void windowDeactivated(WindowEvent arg0) {

	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {

	}


	@Override
	public void windowIconified(WindowEvent arg0) {

	}


	@Override
	public void windowOpened(WindowEvent arg0) {

	}


	@Override
	public void mouseWheelMoved(MouseWheelEvent mwe) {
		/*if (thread.module != null) {
			thread.module.mouseWheelMoved(mwe);
		}*/
	}


	public void handleException(Exception ex) {
		p(ex.getMessage());
		ex.printStackTrace();
		isRunning = false;
		//frame.setVisible(false);
		//System.exit(-1);
	}


	public static void p(String s) {
		System.out.println(s);
	}


	protected void setDefaultFont(Font _font) {
		defaultFont = _font;
	}


	public void playSound(String filename) {
		new AudioPlayer("assets/sfx/" + filename, false).start();
	}

}
