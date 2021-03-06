package gameplay;

import menuStuff.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;

public class Game extends JPanel implements Runnable, MouseListener, MouseMotionListener{

    private JFrame jFrame;

    private Thread thread;
    private boolean running = false;
    public static int gameLaunched = -1;
    private int frames;

    public static KeyInput keyInput;

    public Handler handler;

    public static BufferedImage background, spriteSheet, game_graphics, abort_h, abort_nh, reset_h, reset_nh;

    private ArrayList<Coordinate>[] LettersTiming;

    private HUD hud;

    private Spawn spawn;

    private ShowHitScores showHitScores;

    private InputStreamReader input;

    public static PhysicalKey[] ArrayLetters;
    private char[] positionLetters;
    private int ColBase, Row, ColHover;
    private int posX, posY, posINX;

    public static int REDLINESY;

    public static StopWatch stopWatch;
    public static StopWatch stopWatch2;
    private boolean started;

    private MusicPlayer song;

    private int checkFPS;

    private String selectedSong;
    private boolean checkAutoMode;

    private Rectangle2D rect1;
    private int x1, y1;
    private int width1, height1;
    private boolean change1;

    private Rectangle2D rect2;
    private int y2;
    private boolean change2;

    private boolean stopClick;

    private MusicPlayer buttonAudio;

    public Game(JFrame jFrame, String selectedSong, boolean checkAutoMode){
        addMouseListener(this);
        addMouseMotionListener(this);

        keyInput = new KeyInput(this);
        Stream.iterate(0, i -> i + 1).limit(65).forEach(i -> keyInput.addAction("" + (char)(i + 65)));
        keyInput.addAction("SPACE");

        BufferedImageLoader loader = new BufferedImageLoader();
        spriteSheet = loader.loadImage("/spriteSheet.png");
        background = loader.loadImageV2("res/images/backgrounds/background_gameplay_2.png");
        game_graphics = loader.loadImageV2("res/images/backgrounds/game_graphics.png");
        abort_h = loader.loadImageV2("res/images/buttons/AbortB/abortB1.png");
        abort_nh = loader.loadImageV2("res/images/buttons/AbortB/abortB0.png");
        reset_h = loader.loadImageV2("res/images/buttons/ResetB/resetB1.png");
        reset_nh = loader.loadImageV2("res/images/buttons/ResetB/resetB0.png");

        buttonAudio = new MusicPlayer("res/audioButton.wav");

        this.jFrame = jFrame;

        handler = new Handler();

        stopWatch = new StopWatch();
        stopWatch2 = new StopWatch();
        started = false;

        LettersTiming = new ArrayList[26];

        this.selectedSong = selectedSong;
        this.checkAutoMode = checkAutoMode;

        REDLINESY = 380 - 110;

        for(int i = 0; i < 26; i++){
            LettersTiming[i] = new ArrayList<>();
        }

        try { //gets notes from a file.txt
            getNotes("res/notes/notes_" + selectedSong + ".txt");
        } catch (IOException e) {
           e.printStackTrace();
        }

        physicalLettersCreation();

        hud = new HUD(handler);

        spawn = new Spawn(handler, LettersTiming, checkAutoMode);

        showHitScores = new ShowHitScores(handler, ArrayLetters);

        song = new MusicPlayer("res/songs/" + selectedSong + ".wav");
        try {
            song.createAudio();
        } catch (Exception e) {
            e.printStackTrace();
        }

        width1 = 155;
        height1 = 60;
        x1 = MyFrame.WIDTH - 185;
        y1 = 450;
        rect1= new Rectangle2D.Float(x1, y1, width1, height1);
        change1 = false;

        y2 = y1 + height1 + 10;
        rect2= new Rectangle2D.Float(x1, y2, width1, height1);
        change2 = false;

        stopClick = false;
        gameLaunched++;
    }

    public synchronized void start() {
        thread = new Thread(this);
        thread.start();
        running = true;
        stopWatch2.start();
    }

    public synchronized void stop() {
        try{
            thread.join();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime(); // get current time to the nanosecond
        double amountOfTicks = 60.0; // set the number of ticks
        double ns = 1000000000 / amountOfTicks; // this determines how many times we can devide 60 into 1e9 of nano seconds or about 1 second
        double delta = 0; // change in time (delta always means a change like delta v is change in velocity)
        long timer = System.currentTimeMillis(); // get current time
        frames = 0; // set frame variable
        while(running){
            long now = System.nanoTime(); // get current time in nonoseconds durring current loop
            delta += (now - lastTime) / ns; // add the amount of change since the last loop
            lastTime = now;  // set lastTime to now to prepare for next loop
            while(delta >= 1){
                // one tick of time has passed in the game this
                //ensures that we have a steady pause in our game loop
                //so we don't have a game that runs way too fast
                //basically we are waiting for  enough time to pass so we
                // have about 60 frames per one second interval determined to the nanosecond (accuracy)
                // once this pause is done we render the next frame
                tick();
                delta--;  // lower our delta back to 0 to start our next frame wait
            }
            if(running) repaint(); // render the visuals of the game
            frames++; // note that a frame has passed
            if(System.currentTimeMillis() - timer > 1000 ){ // if one second has passed
                timer+= 1000; // add a thousand to our timer for next time
                checkFPS = frames;
                //System.out.println("FPS: " + frames); // print out how many frames have happend in the last second
                frames = 0; // reset the frame count for the next second
            }
            try {
                Thread.sleep(1); //breath
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stop(); // no longer running stop the thread
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        render(g);
    }

    private void tick(){
        if (stopWatch2.getTime() >= 2000 && !started) {
            stopWatch.start();
            try {
                song.playTrack();
            } catch (Exception e) {
                e.printStackTrace();
            }
            started = true;
            stopWatch2.stop();
        }

        handler.tick();
        showHitScores.tick();
        hud.tick();
        spawn.tick();

        if (stopWatch.getTime() >= 10000 && song.clip != null && !song.clip.isActive() && !stopClick) { //the game ends (OK)
            stopWatch.stop();
            ResultPane resultPane = new ResultPane(jFrame, selectedSong, checkAutoMode, hud.getScore(), hud.getScoreCounts());
            jFrame.setContentPane(resultPane);
            jFrame.revalidate();
            running = false;
        }
    }

    private void render(Graphics g){
        Graphics2D g2d = (Graphics2D) g;

        RenderingHints rh =
                new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

        rh.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);


        g2d.setRenderingHints(rh);

        g2d.drawImage(background, 0, 0 , null);

        g2d.drawImage(game_graphics, 0, 0, null);

        handler.render(g2d);
        showHitScores.render(g2d);
        hud.render(g2d);

        /*g2d.setColor(new Color(45, 45, 45));
        int yWLine = REDLINESY + 64 / 8 + 10;

        g2d.drawLine(posINX, yWLine + 32, posINX + 70 * 10, yWLine + 32);
        g2d.drawLine(posINX, yWLine - 32, posINX + 70 * 10, yWLine - 32);*/

        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(Color.white);

        g2d.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 23));
        g2d.drawString(stopWatch.toString() + "", 26, 104);

        //g2d.drawLine(posINX, yWLine, posINX + 70 * 10, yWLine);

        if (stopWatch.getTime() <= 6000) {
            g2d.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 25));
            g2d.drawString((((stopWatch.getTime() - 6000) / 1000) * -1) + "", 550, 100);
        }

        if(!checkAutoMode) {
            if (change1) g2d.drawImage(reset_h, x1 - 15, y1 - 6, null);
            else g2d.drawImage(reset_nh, x1 - 15, y1 - 6, null);

            /*g2d.setColor(Color.red);
            g2d.draw(rect1);*/
        }

        if (change2) g2d.drawImage(abort_h, x1 - 15, y2 - 6, null);
        else g2d.drawImage(abort_nh, x1 - 15, y2 - 6, null);

        /*g2d.setColor(Color.cyan);
        g2d.draw(rect2);*/
    }

    public void getNotes(String file) throws IOException {
        input = new InputStreamReader(new FileInputStream(file));
        int data = input.read();
        int indexRow = 0;
        int indexCol = 0;
        String word = "";
        while(data != -1){
            if((char)data != '\n') {
                if ((char) data == '-') {
                    indexRow++;
                    indexCol = 0;
                } else if ((char) data == '.') {
                    LettersTiming[indexRow].add(new Coordinate());
                    LettersTiming[indexRow].get(indexCol).setStart(Long.parseLong(word));
                    word = "";
                } else if ((char) data == '|') {
                    LettersTiming[indexRow].get(indexCol).setFinish(Long.parseLong(word));
                    indexCol++;
                    word = "";
                } else word += (char) data;
            }
            data = input.read();
        } input.close();
    }

    public void physicalLettersCreation(){
        ArrayLetters = new PhysicalKey[26];
        Row = 1; ColBase = 1; ColHover = 2;
        for(int i = 0; i < 26; i++){
            if(Row == 14) {
                ColBase += 3;
                ColHover += 3;
                Row = 1;
            }
            ArrayLetters[i] = new PhysicalKey(0, 0, ID_PHY.values()[i], (char)(i + 65), Row, ColBase, Row, ColHover);
            Row++; //hud and handler for physicalKey are just for testing "AUTOPLAY"
        }
        fillPosLetters();

        posX = 210; posY = 370;
        posINX = posX;
        for(int j = 0; j < 26; j++){
            if(positionLetters[j] == 'A'){
                posY += 70;
                posX = posINX + 25;
            }
            if(positionLetters[j] == 'Z'){
                posY += 70;
                posX = posINX + 55;
            }
            for(int k = 0; k < 26; k++){
                if(positionLetters[j] == ArrayLetters[k].getC()){
                    ArrayLetters[k].setX(posX);
                    ArrayLetters[k].setY(posY);
                    ArrayLetters[k].setLine(posX, posY);
                    handler.addObject(ArrayLetters[k]);
                    posX += 70;
                }
            }
        }

    }

    public void fillPosLetters(){
        positionLetters = new char[26];
        positionLetters[0] = 'Q'; positionLetters[1] = 'W'; positionLetters[2] = 'E'; positionLetters[3] = 'R';
        positionLetters[4] = 'T'; positionLetters[5] = 'Y'; positionLetters[6] = 'U'; positionLetters[7] = 'I';
        positionLetters[8] = 'O'; positionLetters[9] = 'P'; positionLetters[10] = 'A'; positionLetters[11] = 'S';
        positionLetters[12] = 'D'; positionLetters[13] = 'F'; positionLetters[14] = 'G'; positionLetters[15] = 'H';
        positionLetters[16] = 'J'; positionLetters[17] = 'K'; positionLetters[18] = 'L'; positionLetters[19] = 'Z';
        positionLetters[20] = 'X'; positionLetters[21] = 'C'; positionLetters[22] = 'V'; positionLetters[23] = 'B';
        positionLetters[24] = 'N'; positionLetters[25] = 'M';
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        if(started) {
            if (!checkAutoMode) {
                if (rect1.contains(x, y)) {
                    stopClick = true;
                    stopWatch.stop();
                    song.clip.stop();
                    running = false;
                    Game game = new Game(jFrame, selectedSong, false);
                    jFrame.setContentPane(game);
                    jFrame.revalidate();
                    game.start();
                }
            }

            if (rect2.contains(x, y)) {
                stopClick = true;
                stopWatch.stop();
                song.clip.stop();
                SelectGlass selectGlass = new SelectGlass(jFrame);
                jFrame.setContentPane(selectGlass);
                jFrame.revalidate();
                running = false;
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        if(!checkAutoMode) {
            if (rect1.contains(x, y)) {
                if (!change1) {
                    try {
                        buttonAudio.createAudio();
                        buttonAudio.playTrack();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                change1 = true;
            } else change1 = false;
        }

        if (rect2.contains(x, y)) {
            if (!change2) {
                try {
                    buttonAudio.createAudio();
                    buttonAudio.playTrack();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            change2 = true;
        } else change2 = false;
    }

    ////////////////////////////////////////////////////
    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
    @Override public void mouseDragged(MouseEvent e) { }
    ////////////////////////////////////////////////////
}
