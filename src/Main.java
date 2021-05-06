import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Main {
    static final String INPUT_FOLDER_NAME = "input";

    static final String TEST_IMAGE_NAME = "test.jpg";

    static boolean fireDetected = false;

    public static void main(String args[]) throws IOException, InterruptedException {

        //Start time of program
        long startTime = System.currentTimeMillis();

        //Check if program should exit loop
        boolean exit = false;
        
        //Helps store loaded data during intermediate steps
        BufferedImage holder = null;
        Image holder2 = null;
        
        //JFrame for the GUI
        JFrame frame = new JFrame("Charmander.exe");
        ImageIcon img = new ImageIcon(Main.class.getResource("images/charmanderIcon.png"));
        frame.setIconImage(img.getImage());
        frame.setSize(600, 400);
        frame.setLocation(580, 100);
        frame.setResizable(false);
        frame.setLayout(null);
        frame.setUndecorated(true);
        frame.setVisible(true);

        //All panels (ie. containers) used
        JLayeredPane masterPanel = new JLayeredPane();
        masterPanel.setPreferredSize(new Dimension(600, 400));
        masterPanel.setBounds(0, 0, 600, 400);
        frame.add(masterPanel);

        //Background
        JPanel botLayer = new JPanel();
        botLayer.setBounds(0, 0, 600, 400);
        masterPanel.add(botLayer, 0, 0);
        botLayer.setBounds(0, 0, 600, 400);

        //Background graphic
        JPanel middleLayer = new JPanel();
        middleLayer.setBounds(330, -60, 250, 480);
        masterPanel.add(middleLayer, 1, 0);

        //For bottom edge
        JPanel topLayer = new JPanel();
        topLayer.setBounds(0, 385, 600, 15);
        topLayer.setOpaque(true);
        topLayer.setBackground(new Color(255, 145, 11));
        masterPanel.add(topLayer, 2, 0);

        //For close button
        JPanel topLayer2 = new JPanel();
        topLayer2.setBounds(557, 0, 60, 60);
        topLayer2.setOpaque(false);
        masterPanel.add(topLayer2, 2, 0);
        
        setupVisualInterface(masterPanel, topLayer, middleLayer, botLayer, topLayer2);

        //For Charmander
        JPanel charLayer = new JPanel();
        charLayer.setBounds(0, 340, 600, 50);
        charLayer.setOpaque(false);
        masterPanel.add(charLayer, 3, 0);

        //For the text that displays on the left-center side of the screen
        JPanel leftText = new JPanel();
        leftText.setBounds(-140, 160, 600, 200);
        leftText.setOpaque(false);
        masterPanel.add(leftText, 4, 0);

        //For the image that it takes in
        JPanel rightImage = new JPanel();
        rightImage.setBounds(135, 80, 600, 300);
        rightImage.setOpaque(false);
        masterPanel.add(rightImage, 4, 0);

        //Animated Charmander
        //Declares 4 arrays for the four possible animation cycles that Charmander has
        int charFrames = 6;
        JLabel charmander = new JLabel();
        charmander.setBounds(0, 0, 65, 50);
        ImageIcon[] charIcons = new ImageIcon[charFrames + 1];
        ImageIcon[] charInverted = new ImageIcon[charFrames + 1];
        ImageIcon[] panicChar = new ImageIcon[charFrames];
        ImageIcon[] panicCharInverted = new ImageIcon[charFrames];
        for (int i = 0; i < 6; ++i) {
            try {
                holder = ImageIO.read(
                		Main.class.getResource("images/CharN" + Integer.toString(i + 1) + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            holder2 = holder.getScaledInstance(65, 50, Image.SCALE_DEFAULT);
            charIcons[i] = new ImageIcon(holder2);
            holder = flipImage(holder, holder.getWidth(), holder.getHeight());
            holder2 = holder.getScaledInstance(65, 50, Image.SCALE_DEFAULT);
            charInverted[i] = new ImageIcon(holder2);

            try {
                holder = ImageIO.read(
                		Main.class.getResource("images/CharP" + Integer.toString(i + 1) + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            holder2 = holder.getScaledInstance(65, 50, Image.SCALE_DEFAULT);
            panicChar[i] = new ImageIcon(holder2);
            holder = flipImage(holder, holder.getWidth(), holder.getHeight());
            holder2 = holder.getScaledInstance(65, 50, Image.SCALE_DEFAULT);
            panicCharInverted[i] = new ImageIcon(holder2);
        }
        
        try {
            holder = ImageIO.read(Main.class.getResource("images/charThink.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder2 = holder.getScaledInstance(65, 50, Image.SCALE_DEFAULT);
        charIcons[6] = new ImageIcon(holder2);
        holder = flipImage(holder, holder.getWidth(), holder.getHeight());
        holder2 = holder.getScaledInstance(65, 50, Image.SCALE_DEFAULT);
        charInverted[6] = new ImageIcon(holder2);
        charLayer.add(charmander);

        //Setting up text portion of window
        JTextArea labelText = new JTextArea("Waiting to receive image from bot...");
        labelText.setFont(new Font("PrimerPrint-Bold", Font.BOLD, 16));
        labelText.setLineWrap(true);
        labelText.setEditable(false);
        labelText.setWrapStyleWord(true);
        labelText.setForeground(new Color(230, 110, 10));
        labelText.setBounds(0, 0, 240, 400);
        labelText.setOpaque(false);
        leftText.add(labelText);

        //Analyzed image place
        JLabel waitImage = new JLabel();
        try {
            holder = ImageIO.read(Main.class.getResource("images/dashedSquare.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder2 = holder.getScaledInstance(280, 280, Image.SCALE_SMOOTH);
        img = new ImageIcon(holder2);
        waitImage.setIcon(img);
        waitImage.setBounds(0, 0, 300, 300);
        rightImage.add(waitImage);

        JLabel analyzedImage = new JLabel();
        analyzedImage.setBounds(0, 0, 300, 300);
        rightImage.add(analyzedImage);


        //Wait for image and animate Charmander at the bottom of the screen
        boolean movingPos = true;
        int currentMod = 0;
        long timeElapsed = 0;
        int result = 0;

        String folderPath = System.getProperty("user.dir") + "/" + INPUT_FOLDER_NAME;
        String imgPath = folderPath + "/" + TEST_IMAGE_NAME;

        do {
            holder = null;
            //For Charmander moving
            if (((System.currentTimeMillis() - startTime) % 600) / 50 != currentMod) {
                currentMod = (int)(((System.currentTimeMillis() - startTime) % 600) / 50);
                
                if (movingPos) {
                    if (fireDetected) {
                        charmander.setIcon(panicCharInverted[currentMod % 6]);
                        charmander.setLocation(charmander.getX() + 6, 5);
                    } else {
                        charmander.setIcon(charInverted[currentMod / 2]);
                        charmander.setLocation(charmander.getX() + 4, 5);
                    }
                } else {
                    if (fireDetected) {
                        charmander.setIcon(panicChar[currentMod % 6]);
                        charmander.setLocation(charmander.getX() - 6, 5);
                    } else {
                        charmander.setIcon(charIcons[currentMod / 2]);
                        charmander.setLocation(charmander.getX() - 4, 5);
                    }
                }
            }
            
            if (charmander.getX() < 20) {
                movingPos = true;
            } else if (charmander.getX() > 500) {
                movingPos = false;
            }

            //Starts by waiting for image
            if (System.currentTimeMillis() - timeElapsed > 1000 && fireDetected == false) {
                timeElapsed = System.currentTimeMillis();
                File folder = new File(folderPath);
                File[] listOfFiles = folder.listFiles();

                for (int i = 0; listOfFiles != null && i < listOfFiles.length; i++) {

                    if (listOfFiles[i].isFile()) {

                        File f = new File(folderPath + "/" + listOfFiles[0].getName());

                        f.renameTo(new File(imgPath));
                    }
                }

                try {
                    holder = ImageIO.read(new File(imgPath));
                } catch (Exception e) {
                    // This is fine, means image is not there yet
                }
                
                // Image found
                if (holder != null) {
                	
                    if (holder.getHeight() > holder.getWidth()) {
                        holder2 = holder.getScaledInstance(
                        		(int)(((double) holder.getWidth() / holder.getHeight()) * 240), 
                        		240, Image.SCALE_SMOOTH);
                        labelText.setBounds(0, 0, 240, 400);
                        rightImage.setBounds(135, 80, 600, 300);
                        leftText.setBounds(-140, 160, 600, 200);
                    } else {
                        holder2 = holder.getScaledInstance(
                        		340, 
                        		(int)(((double) holder.getHeight() / holder.getWidth()) * 340), 
                        		Image.SCALE_SMOOTH);
                        rightImage.setBounds(85, 25 + (340 - (int)(((double) holder.getHeight() /
                        		holder.getWidth()) * 340)) / 2, 600, 300);
                        labelText.setBounds(0, 0, 150, 400);
                        leftText.setBounds(-185, 160, 600, 200);
                    }
                    
                    img = new ImageIcon(holder2);
                    analyzedImage.setIcon(img);
                    waitImage.setVisible(false);

                    //Changing Charmander animation temporarily
                    if (movingPos) {
                        masterPanel.setLayout(null);
                        charLayer.setLayout(null);
                        charmander.setLocation(charmander.getX(), 5);
                        charmander.setIcon(charInverted[6]);
                        charmander.setLocation(charmander.getX(), 5);
                        labelText.setText("Processing image...");
                        Thread.sleep(1500);
                    } else {
                        masterPanel.setLayout(null);
                        charLayer.setLayout(null);
                        charmander.setLocation(charmander.getX(), 5);
                        charmander.setIcon(charIcons[6]);
                        charmander.setLocation(charmander.getX(), 5);
                        labelText.setText("Processing image...");
                        Thread.sleep(1500);
                    }

                    //Calls on detector class to analyze image
                    result = Detector.detectFire.detect(holder);

                    //Displaying results accordingly
                    if (result == 0) {
                        labelText.setText("No fire detected, all good.");
                    } else {
                        labelText.setText("DANGER: There is likely a fire in the image.");
                        fireDetected = true;
                    }

                    //Delete images after processing
                    Path imagesPath = Paths.get(
                        imgPath);
                    try {
                        Files.delete(imagesPath);
                        System.out.println("File successfully removed!");
                    } catch (IOException e) {
                        System.err.println("Unable to delete...");
                        e.printStackTrace();
                    }
                }
            }
        } while (!exit);
    }

    //Flip image helper
    public static BufferedImage flipImage(BufferedImage Image, int w, int h) {
        BufferedImage flippedImage = new BufferedImage(w, h, Image.getType());
        Graphics2D g = flippedImage.createGraphics();
        g.drawImage(Image, 0, 0, w, h, w, 0, 0, h, null);
        g.dispose();
        return flippedImage;
    }

    //Sets up the portions of the window that are static
    public static void setupVisualInterface(
    		JLayeredPane masterPanel, JPanel topLayer, JPanel middleLayer, 
    		JPanel botLayer, JPanel topLayer2) {
    	
        //Helps store loaded data during intermediate steps
        BufferedImage holder = null;
        Image holder2 = null;
        ImageIcon img = null;
    	
        JLabel bkgd = new JLabel();
        botLayer.setBackground(new Color(255, 178, 78));
        bkgd.setOpaque(true);
        bkgd.setBounds(0, 0, 600, 400);
        botLayer.add(bkgd, Integer.valueOf(0), 0);
    	
        //Bottom line
        JLabel bottom = new JLabel();
        bottom.setBounds(0, 300, 600, 600);
        bottom.setOpaque(true);
        bottom.setBackground(Color.pink);
        topLayer.add(bottom, 0);
        
        //Title
        JPanel titleLayer = new JPanel();
        titleLayer.setBounds(-50, 20, 400, 70);
        titleLayer.setOpaque(false);
        masterPanel.add(titleLayer, 3, 0);
        
        JLabel title = new JLabel();
        try {
            holder = ImageIO.read(Main.class.getResource("images/charText.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder2 = holder.getScaledInstance(250, 40, Image.SCALE_SMOOTH);
        img = new ImageIcon(holder2);
        title.setIcon(img);
        title.setBounds(0, 0, 250, 40);
        titleLayer.add(title);
        
        //Version number
        JPanel versionLayer = new JPanel();
        versionLayer.setBounds(285, 35, 30, 30);
        versionLayer.setOpaque(false);
        masterPanel.add(versionLayer, 4, 0);
        
        JLabel smallText = new JLabel();
        try {
            holder = ImageIO.read(Main.class.getResource("images/versionText.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder2 = holder.getScaledInstance(30, 25, Image.SCALE_SMOOTH);
        img = new ImageIcon(holder2);
        smallText.setIcon(img);
        smallText.setBounds(280, 0, 40, 40);
        versionLayer.add(smallText);
        
        //Fire silhouette background
        JLabel bkgd2 = new JLabel();
        try {
            holder = ImageIO.read(Main.class.getResource("images/fireBckgd1.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        holder2 = holder.getScaledInstance(250, 480, Image.SCALE_SMOOTH);
        img = new ImageIcon(holder2);
        bkgd2.setIcon(img);
        bkgd2.setBounds(0, 0, 300, 400);
        middleLayer.add(bkgd2);
        
        //Exit button
        final JButton exitButton = new JButton(" X ");
        topLayer2.add(exitButton);
        exitButton.setOpaque(true);
        exitButton.setBackground(new Color(226, 130, 40));
        exitButton.setBounds(0, 0, 60, 60);
        exitButton.setFocusPainted(false);
        Border bored = BorderFactory.createLineBorder(new Color(206, 110, 20));
        exitButton.setBorder(bored);
        
        //Listener for hover and click of the button
        exitButton.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                ButtonModel model = (ButtonModel) e.getSource();
                if (model.isRollover()) {
                    exitButton.setBackground(new Color(206, 110, 20));
                } else {
                    exitButton.setBackground(new Color(226, 130, 40));
                }
                if (model.isPressed()) {
                    System.exit(0);
                }
            }
        });
    }
}