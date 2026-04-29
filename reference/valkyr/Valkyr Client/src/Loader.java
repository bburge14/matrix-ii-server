import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Loader extends Applet {
	
	private static final long serialVersionUID = 7639088664641445302L;
	
	public static Properties client_parameters = new Properties();
	public JFrame client_frame;
	public JPanel client_panel = new JPanel();

	public static boolean usingRS = false;
	public static boolean useIsaac = false;
	public static boolean useRoute = true;
	public static boolean useMapsTest = false;
	public static boolean checkForUpdates = true;
	public static String BLOCK_IDS = "";
	public static boolean DISABLE_XTEA_CRASH = true;
	public static boolean DISABLE_USELESS_PACKETS = true;
	public static boolean DISABLE_RSA = false;
	public static boolean DISABLE_CS_MAP_CHAR_CHECK = true;
	public static boolean DISABLE_SOFTWARE_MODE = true;
	public static boolean ENABLE_LOBBY = false;
	public static String SERVER_NAME = "Valkyr";
	public static final int REVISION = 718;
	public static String GAME_IP = "127.0.0.1";
	public static String LOBBY_IP = "184.155.245.22";
	public static boolean VB_ENABLED = false;
	public static final int LOBBY_PORT = 43594;
	public static int SET_PORT = 0;
	public static final boolean NSN_ENABLED = true;
	private static final double BUILD_NUMBER = 1.11;
	public static int SUB_REVISION = 1;
	public static Loader instance;
	public static int[] outSizes = new int[256];
	public static boolean localHost;
	public static boolean devBuild = false;

	public static void main(String[] args) throws IOException {
		Loader loader = instance = new Loader();
		if (args.length > 0) {
			
		}
		loader.doFrame();
	}

	public void init() {
		instance = this;
		doApplet();
	}

	void doApplet() {
		setParams();
		startClient();
	}

	public void doFrame() {
		setParams();
		openFrame();
		startClient();
	}

	void setParams() {
		// applet parameters, not client
		client_parameters.put("separate_jvm", "true");
		client_parameters.put("java_arguments",
				"-Xmx256m -Xss2m -Dsun.java2d.noddraw=true -XX:CompileThreshold=1500 -Xincgc -XX:+UseConcMarkSweepGC -XX:+UseParNewGC");

		client_parameters.put("boxbgcolor", "black");
		client_parameters.put("image",
				"http://www.runescape.com/img/game/splash2.gif");
		client_parameters.put("centerimage", "true");
		client_parameters.put("boxborder", "false");
		client_parameters.put("27", "0");
		client_parameters.put("1", "0");
		client_parameters.put("16", "false");
		client_parameters.put("17", "false");
		client_parameters.put("21", "1"); // WORLD ID
		client_parameters.put("30", "false");
		client_parameters.put("20", "lobby17.runescape.com");
		client_parameters.put("29", "");
		client_parameters.put("11", "true");
		client_parameters.put("25", "1378752098");
		client_parameters.put("28", "0");
		client_parameters.put("8", ".runescape.com");
		client_parameters.put("23", "false");
		client_parameters.put("32", "0");
		client_parameters.put("15",
				"wwGlrZHF5gKN6D3mDdihco3oPeYN2KFybL9hUUFqOvk");
		client_parameters.put("0", "IjGJjn4L3q5lRpOR9ClzZQ");
		client_parameters.put("2", "");
		client_parameters.put("4", "1"); // LOBBY ID
		client_parameters.put("14", "");
		client_parameters.put("5", "8194");
		client_parameters.put("-1", "QlwePyRU5GcnAn1lr035ag");
		client_parameters.put("6", "0");
		client_parameters.put("24",
				"true,false,0,43,200,18,0,21,354,-15,Verdana,11,0xF4ECE9,candy_bar_middle.gif,candy_bar_back.gif,candy_bar_outline_left.gif,candy_bar_outline_right.gif,candy_bar_outline_top.gif,candy_bar_outline_bottom.gif,loadbar_body_left.gif,loadbar_body_right.gif,loadbar_body_fill.gif,6");
		client_parameters.put("3", "hAJWGrsaETglRjuwxMwnlA/d5W6EgYWx");
		client_parameters.put("12", "false");
		client_parameters.put("13", "0");
		client_parameters.put("26", "0");
		client_parameters.put("9", "77");
		client_parameters.put("22", "false");
		client_parameters.put("18", "false");
		client_parameters.put("33", "");
		client_parameters.put("haveie6", "false");
	}

	void openFrame() {
		client_frame = new JFrame(SERVER_NAME + " - " + getWorldName() + " - V" + BUILD_NUMBER);
		try {
			 URL localURL = new URL("https://pbs.twimg.com/profile_images/715404685725736962/FJJHPv1w.jpg");
			 BufferedImage localBufferedImage = ImageIO.read(localURL);
			 client_frame.setIconImage(localBufferedImage);
		}
		catch (Exception localException) {
			localException.printStackTrace();
		}
		client_frame.setLayout(new BorderLayout());
		client_panel.setLayout(new BorderLayout());
		client_panel.add(this);//
		client_panel.setPreferredSize(new Dimension(765, 553));
		client_frame.getContentPane().add(client_panel, "Center");
		client_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client_frame.pack();
		client_frame.setVisible(true);

		client_frame.setLocationRelativeTo(null); //sets window to the center
	}
	
	public JFrame world_frame;
	public JPanel world_panel;
	
	private void checkWorld() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
			world_panel = new JPanel();
			
			world_frame = new JFrame("World Select");
			world_frame.setTitle("World Select");
			//orld_frame.setSize(50, 100);
			world_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			world_frame.setBounds(100, 100, 200, 200);
			world_frame.setLocationRelativeTo(null);
			world_frame.setVisible(true);
			world_frame.setContentPane(world_panel);
			//world_frame.setContentPane(world_panel);
				
			JButton world1 = new JButton("World 1");
			world1.setFont(new Font("Tahoma", Font.PLAIN, 11));
			world1.setForeground(Color.BLACK);
			world1.setBounds(10, 10, 151, 23);			
			world_panel.add(world1);
			world1.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					worldSelect1(evt);
					world_frame.setVisible(false);
					world_panel.setVisible(false);
				}
			});
			
			JButton world2 = new JButton("World 2");
			world2.setFont(new Font("Tahoma", Font.PLAIN, 11));
			world2.setForeground(Color.BLACK);
			world2.setBounds(10, 40, 151, 23);
			world_panel.add(world2);
			world2.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					worldSelect2(evt);
					world_frame.setVisible(false);
					world_panel.setVisible(false);
				}
			});
			
			JButton world3 = new JButton("World 3");
			world3.setFont(new Font("Tahoma", Font.PLAIN, 11));
			world3.setForeground(Color.BLACK);
			world3.setBounds(10, 70, 151, 23);
			world_panel.add(world3);
			world3.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					worldSelect3(evt);
					world_frame.setVisible(false);
					world_panel.setVisible(false);
				}
			});
				
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	private void worldSelect1(ActionEvent evt) {
		SET_PORT = 43594;
		System.out.println("Hosting World 1");
		doFrame();
	}
	
	private void worldSelect2(ActionEvent evt) {
		SET_PORT = 43595;
		System.out.println("Hosting World 2");
		doFrame();
	}
	
	private void worldSelect3(ActionEvent evt) {
		SET_PORT = 43596;
		System.out.println("Hosting World 3");
		doFrame();
	}

	void startClient() {
		try {
			client clnt = new client();
			clnt.x(this);
			clnt.g();
			clnt.start();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public String getParameter(String string) {
		return (String) client_parameters.get(string);
	}

	public URL getDocumentBase() {
		return getCodeBase();
	}

	public URL getCodeBase() {
		try {
			if (usingRS)
				return new URL("http://world11.runescape.com");
			else
				return new URL("http://" + GAME_IP);
		} catch (Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}
	
	public String getWorldName() {
		switch (SET_PORT) {
			case 43594:
				return "World 1";
			case 43595:
				return "World 2";
			case 43596:
				return "World 3";
			default:
				return "World 1";
		}
	}
}
