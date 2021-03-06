package gameplay;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

public class PhantomKey extends GamePhantomObject{

    private Handler handler;

    private Line2D phline;

    private BufferedImageLoader loader;
    private BufferedImage PH_Key_img, phantomGlow;

    private float velY;

    private ID_PHY id_phy;

    private int RCoordinateY, rectHeight, heightR, heightY, heightG, heightB;

    private char c;

    private boolean CAM; //checkAutoMode

    public PhantomKey(float x, float y, ID_PHA id_p, ID_PHY id_r, Handler handler, float velY, char c, int rb, int cb, boolean CAM) {
        super(x, y, id_p);
        this.id_phy = id_r;
        this.handler = handler;
        this.c = c;
        this.velY = velY;
        this.CAM = CAM;

        phline = new Line2D.Double(x, y + 64 / 2, x + 64, y + 64 / 2);

        SpriteSheet ss = new SpriteSheet(Game.spriteSheet);
        loader = new BufferedImageLoader();
        PH_Key_img = ss.grabImage(rb, cb, 64, 64);
        phantomGlow = loader.loadImageV2("res/phantomGlow.png");

        for(int i = 0; i < handler.objects.size(); i++) {
            GameObject tempObject = handler.objects.get(i);
            if (tempObject.getId() == id_phy) {
                RCoordinateY = (int)tempObject.getLine().getP2().getY();
            }
        }

        rectHeight = 48;
        heightR = 10;
        heightY = 5;
        heightG = 5;
        heightB = rectHeight - heightR - heightY - heightG;
    }

    @Override
    public void tick() {
        y += velY;
        updateLine();
        if(CAM) scoreHITAuto();
        else if (getLine().getY1() >= RCoordinateY) checkScoreHIT();
    }

    public void checkScoreHIT(){
        for (int i = 0; i < handler.objects.size(); i++) {
            GameObject tempObject = handler.objects.get(i);
            if (tempObject.getId() == id_phy) {
                if (getLine().getY1() >= tempObject.getLine().getY1()) scoreHIT = 0;
                if (Game.keyInput.pressedKeys.get(c + "") != null && Game.keyInput.pressedKeys.get(c + "")) {
                    if (getLine().getY1() >= RCoordinateY
                            && getLine().getY1() < RCoordinateY + heightR) scoreHIT = 0;
                    else if (getLine().getY1() >= RCoordinateY + heightR
                            && getLine().getY1() < RCoordinateY + heightR + heightY) scoreHIT = 100;
                    else if (getLine().getY1() >= RCoordinateY + heightR + heightY
                            && getLine().getY1() < RCoordinateY + heightR + heightY + heightG) scoreHIT = 250;
                    else if (getLine().getY1() >= RCoordinateY + heightR + heightY + heightG
                            && getLine().getY1() < RCoordinateY + heightR + heightY + heightG + heightB) scoreHIT = 500;
                }
            }
        }
    }

    public void scoreHITAuto(){ //automode
        for (int i = 0; i < handler.objects.size(); i++) {
            GameObject tempObject = handler.objects.get(i);
            if (tempObject.getId() == id_phy) {
                if (getLine().getY1() >= tempObject.getLine().getY1()) scoreHIT = 0;
                if (getLine().getY1() >= RCoordinateY + heightR + heightY + heightG + heightB / 2
                        && getLine().getY1() < RCoordinateY + heightR + heightY + heightG + heightB) {
                    scoreHIT = 500;
                }
            }
        }
    }

    public void updateLine(){
        phline.setLine(x, y + 64 / 2, x + 64, y + 64 / 2);
    }

    @Override
    public void render(Graphics2D g2d) {
        g2d.drawImage(phantomGlow, (int)x, (int)y - 19, null);
        g2d.drawImage(PH_Key_img, (int)x, (int)y, null);
    }

    @Override
    public Line2D getLine() {
        return phline;
    }

}
