import java.util.Random;
import java.util.HashMap;

import java.awt.Font;
import java.awt.Color;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Cursor;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.TextField;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferStrategy;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class Game {

    private static final int WIN_WIDTH = 800;
    private static final int WIN_HEIGHT = 540;
    private static Window window;   

    public static void main(String[] args) {
        window = new Window(WIN_WIDTH, WIN_HEIGHT, "Stickman Soccer");

        // ~60 FPS game loop
        // Using a separate Thread to display the window object on the screen
        // for simple games or animations like this one, just using
        // the main thread is also fine
        // gameloop();
        new Thread() {
            @Override
            public void run() {
                gameloop();
            }
        }.start();
    }

    static void gameloop() {
        long lastTime = System.currentTimeMillis();
        double amountOfTicks = 60.0;
        double ms = 1000f / amountOfTicks;
        double delta = 0;
        while (true) { // not a good idea but it's fine
            long now = System.currentTimeMillis();
            delta += (now - lastTime) / ms;
            lastTime = now;
            while (delta >= 1) {
                // rendering and updating the Window object
                window.display();
                delta--;
            }
        }
    }
}

class Window {
    // Main Components of the window
    JFrame windowFrame;
    Panel rightPanel, bottomPanel;
    // Inner components that are used in this window
    JLabel info;
    TextField tfTarget, tfResult;
    Button btnShoot, btnPlayAgain;
    Font font;
    // the football field
    Field field;

    Window(int width, int height, String title) {   	 
        windowFrame = new JFrame();
        windowFrame.setBounds(0, 0, width, height);
        windowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        windowFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
        windowFrame.setTitle(title);
        windowFrame.setResizable(false);
        windowFrame.setLayout(null);

        createRightPanel();
        createBottomPanel();
        field = new Field(600, 400);

        windowFrame.add(rightPanel);
        windowFrame.add(bottomPanel);
        windowFrame.add(field.getCanvas());

        windowFrame.setVisible(true);
    }
    
    void display() {
        if (field.ball.isKicked) {
            if (!field.ball.reached) {
                if (field.keeper.saves) {
                    tfResult.setText("KEEPER SAVES IT !!!");
                }
            } else {
                tfResult.setText("SCORED !!!");
            }
        }
        // TIP: Always render first, update later !!!
        field.render();
        field.update();
    }
    
    private void createRightPanel() {   	 
    	rightPanel = new Panel();
    	rightPanel.setBackground(Color.ORANGE);
    	rightPanel.setBounds(600, 0, 200, 400);
    	rightPanel.setLayout(null);
   	 
    	final String formattedHTMLString = "<html>Where do you want to "
                            	+ "shoot the ball?"
                            	+ "<br>e.g. (left, right, centre, top left,"
                            	+ " top centre, top right).</br></html>";
   	 
    	info = new JLabel(formattedHTMLString);
    	info.setBounds(10, 40, 175, 120);
    	info.setFont(new Font("SansSerif", Font.BOLD, 16));
    	info.setFocusable(false);
    	info.setForeground(Color.BLUE);
   	 
    	tfTarget = new TextField();
    	tfTarget.setBounds(10, 200, 175, 80);
    	tfTarget.setFont(new Font("SansSerif", Font.BOLD, 24));
    	tfTarget.setFocusable(true);
   	 
    	btnShoot = new Button("SHOOT");
    	btnShoot.setBounds(10, 310, 175, 80);
    	btnShoot.setBackground(Color.CYAN);
    	btnShoot.setFocusable(true);
    	btnShoot.setFont(new Font("SansSerif", Font.BOLD, 24));
    	btnShoot.addActionListener(new ActionListener() {
            // when the button is clicked, this method runs
            @Override
            public void actionPerformed(ActionEvent e) {
            	String corner = tfTarget.getText();
            	Point cornerMidPoint = field.goal.getCorner(corner);
            	if (cornerMidPoint != null) {
                    field.ball.setTarget(cornerMidPoint);
                    field.ball.isKicked = true;
                    field.keeper.moveRandomly();
                }
            }
    	});
   	 
    	// creates the panel and adds all the above inner components
    	rightPanel.add(info);
    	rightPanel.add(btnShoot);
    	rightPanel.add(tfTarget);

    	info.setVisible(true);
    	tfTarget.setVisible(true);
    	btnShoot.setVisible(true);   	 
    	rightPanel.setVisible(true);
    }
    
    private void createBottomPanel() {  
    	bottomPanel = new Panel();
    	bottomPanel.setBounds(0, 400, 800, 140);
    	bottomPanel.setBackground(Color.RED);
    	bottomPanel.setLayout(null);
   	 
    	tfResult = new TextField();
    	tfResult.setBounds(10, 10, 580, 80);
    	tfResult.setFont(new Font("SansSerif", Font.BOLD, 24));

    	btnPlayAgain = new Button("Play Again");
    	btnPlayAgain.setBounds(610, 10, 175, 80);
    	btnPlayAgain.setBackground(Color.ORANGE);
    	btnPlayAgain.setFont(new Font("SansSerif", Font.BOLD, 24));
    	btnPlayAgain.addActionListener(new ActionListener() {
            // this method gets called when the user clicks this button
            @Override
            public void actionPerformed(ActionEvent e) {
            	field.reset();
            	tfResult.setText("");
            }
    	});

    	// creating the bottom panel and adding child components to it
    	bottomPanel.add(btnPlayAgain);
    	bottomPanel.add(tfResult);
   	 
    	// if setVisible is set to false, the object won't appear on the screen
    	// it's best practice to call setVisible method after everything about
    	// the UI component has been set. e.g. X, Y, width, height, Color etc.
    	bottomPanel.setVisible(true);
    	tfResult.setVisible(true);
    	btnPlayAgain.setVisible(true);
    }

}

// this class represents the football pitch
// most of the actual game play is handled here
class Field {
    
    private Point area; // size of the field
    // these three objects from awt package are used to render 2D graphics
    private Canvas canvas;
    private Graphics2D gfx;
    private BufferStrategy bfs;
    // game objects
    Goal goal;
    Ball ball;
    Keeper keeper;

    // if you want to play with multiple Fields, it's good idea to
    // create a more flexible constructor here
    Field(int x, int y) {
        area = new Point(x, y); // relative to top left (0, 0)
        canvas = new Canvas();
        canvas.setSize(area.x, area.y);
        canvas.setLocation(new Point(0, 0));
        canvas.setBounds(0, 0, area.x, area.y);
        canvas.setBackground(Color.GREEN);
        canvas.setVisible(true);

        goal = new Goal();
        ball = new Ball();
        keeper = new Keeper();
    }

    void reset() {
        ball.reset();
        keeper.reset();
    }

    Canvas getCanvas() {
        return canvas;
    }

    private void renderGrass() {
        gfx.setColor(Color.GREEN);
        // draw a rectangle on the screen
        // the first two parameter are starting X and Y coordinates
        // and the later two are width and height of the rectangle
        gfx.fillRect(0, 0, 600, 400);
    }
    
    private void renderYellowLine() {
        // stroke is basically width of a point or line segment
        gfx.setStroke(new BasicStroke(4.0f));
        gfx.setColor(Color.YELLOW);
        // draw a line from point (0, 250) to (600, 250)
        gfx.drawLine(0, 250, 600, 250);
    }

    void render() {
        bfs = canvas.getBufferStrategy();
        // this null checking is VERY important because when
        // the game first starts, bfs will be null !!
        if (bfs == null) {
            int buffers = 0x2;
            canvas.createBufferStrategy(buffers);
            return;
        }
        // casting the return type (Graphics) to Graphics2D just to be safe
        gfx = (Graphics2D) bfs.getDrawGraphics();       	 
        // calling all render methods
        renderGrass();  
        goal.render(gfx);
        renderYellowLine();
        ball.render(gfx);
        keeper.render(gfx);

        bfs.show(); // displays all contents on the screen
        gfx.dispose(); // resets the gfx object for the next frame
    }
    
    void update() {
        if (ball.isKicked && !ball.reached) {
            if (!keeper.saves) {
            if (keeper.save(ball))
                ball.bounceBack();
            }
            ball.update();
        }
        keeper.update();
    }
}


class Ball {
    float radius;
    float velocityX, velocityY;
    boolean isKicked, reached;
    Point location, target;

    // if you want to have multiple Ball on the screen, each with its own
    // properties, then may be it's good to create a more flexible constructor
    Ball() { reset(); }

    // this method checks to see if the Ball has hit it's target corner
    private boolean hasHitTheTarget() {
        // the Ball might or might not hit the precise location
        // therefore some acceptable margin need to be set
        int locationOffset = 10;
        // horizontal axis checking
        if (location.x >= target.x-locationOffset &&
            location.x <= target.x+locationOffset) {
            // vertical axis checking
            if (location.y >= target.y-locationOffset &&
                location.y <= target .y+locationOffset) {
                reached = true;
            }
        }
        return reached;
    }
    
    void setTarget(Point midPoint) {
        target = midPoint;    
        // figuring out horizontal and vertical velocities
        // depending on the corner the user whats to hit
        float deltaX = (float)(target.x - location.x);
        float velocityRatio = (target.y - location.y)/deltaX;

        if (deltaX == 0) {
            velocityY = -6.0f;
        } else if (deltaX < 0) {
            velocityX = -4.0f;
            velocityY = velocityRatio * velocityX;
        } else {
            velocityX = 4.0f;
            velocityY = velocityRatio * velocityX;
        }   	 
    }

    void reset() {
        isKicked = false;
        reached = false;
        radius = 50;
        velocityX = 0;
        velocityY = 0;
        location = new Point(300, 400 - (int)radius);
    }
    
    void bounceBack() {
        velocityY *= -1;
    }
    
    void update() {
        if (!hasHitTheTarget()) {
            location.x += velocityX;
            location.y += velocityY;
            radius -= 1/3f;
        }
    }
    
    void render(Graphics2D gfx) {
        int x = location.x-(int)radius/2;
        int y = location.y-(int)radius/4;
        gfx.setColor(new Color(230, 76, 0));
        gfx.fillOval(x, y, (int)radius, (int)radius);
    }
}

final class Keeper {
    // everything moves relative to this 2d point
    Point location;
    float bellySize, width, height;
    float velX, velY;
    boolean isMoving, saves;
    Random RND = new Random(); // used for random movements

    // all the moves the keeper can perform
    // these are basically the direction Vectors
    final int[][] moves = {
        {-1, 0}, // left
        {1,  0}, //right
        {0, -1}, //jump up
        {-1,-1}, // jump left
        {1, -1} // jump right
    };
    
    // again, for multiple Keepers, good idea to write a custom constructor
    Keeper() { reset(); }
    
    void reset() {
    	bellySize = 8;
    	width = 100f;
    	height= 160f;
    	isMoving = false;
    	saves = false;
    	velX = 0;
    	velY = 0;
    	location = new Point(300-(int)bellySize/2, 200);
    }
 
    // this method is called when the user clicks the 'Shoot' button
    void moveRandomly() {
    	isMoving = true;
    	// picks a random move (Direction vector) from all possible moves
    	int randomMove = RND.nextInt(moves.length);
    	velX = moves[randomMove][0] * 5f;
    	velY = moves[randomMove][1] * 4f;
    }
    
    // this method is basically an attempt from the Keeper to save the ball
    boolean save(Ball ball) {
    	if (ball.isKicked && !ball.reached && !saves) {
            Point P = location;
            int X = ball.location.x;
            int Y = ball.location.y;
            // horizontal coordinates checking
            if (X > P.x - width/2f && X < P.x + width/2f) {
                // vertical coordinates checking
                if (Y > P.y - height/2f && Y < P.y + height/2f) {
                    saves = true;
                }
            }
    	}
        return saves;
    }
    
    void update() {
    	if (isMoving) {       	 
            location.x += velX;
            location.y += velY;
            // in case the keeper goes too high, then it needs to drop back
            if (location.y <= 125) { velY *= -1f; }
            // if the keeper try to run away (jajajaa), we catch him
            if (location.x < 150 || location.x >= 450 || location.y >= 205) {
                isMoving = false;
            }
    	}
    }
    
    void render(Graphics2D gfx) {
    	int x = location.x;
    	int y = location.y;
   	 
    	gfx.setColor(Color.BLACK);
    	gfx.setStroke(new BasicStroke(bellySize));
    	// body
    	gfx.drawLine(x,y-40,x,y+20);
    	// legs
    	gfx.drawLine(x,y+20,x-40,y+80);
    	gfx.drawLine(x,y+20,x+40,y+80);
    	// arms
    	gfx.drawLine(x,y-20,x-50,y-30);
    	gfx.drawLine(x,y-20,x+50,y-30);
    	// head
    	gfx.drawOval(x-20, y-80, 40, 40);
    	gfx.setColor(Color.LIGHT_GRAY);
    	gfx.fillOval(x-20, y-80, 40, 40);
    	// eyes
    	gfx.setColor(Color.BLACK);
    	gfx.fillOval(x-10, y-70, 5, 10);
    	gfx.fillOval(x+5, y-70, 5, 10);
    	// mouth
    	gfx.fillOval(x-5, y-52, 12, 3);
    }
}

class Goal {

    int x, y;
    int width, height;
    int cornerWidth;
    private HashMap<String, Point> corners;

    Goal() {
        x = 90; y = 30;
        width = 420; height = 220;
        cornerWidth = width/3;
        corners = new HashMap<>();
        setCorners();
    }

    private void setCorners() {
        // storing names of each corner with its midpoint 2D coordinates
        corners.put("left",       new Point(x+cornerWidth/2, y+height/2));
        corners.put("top left",   new Point(x+cornerWidth/2, y+height/4));
        corners.put("right",  	  new Point((x+width)-cornerWidth/2, y+height/2));
        corners.put("top right",  new Point((x+width)-cornerWidth/2, y+height/4));
        corners.put("centre", 	  new Point(x+width/2, y+height/2));
        corners.put("top centre", new Point(x+width/2, y+height/4));
    }

    Point getCorner(String corner) {
        return corners.get(corner);
    }
    
    void render(Graphics2D gfx) {
        // renders the white goal post on the screen
        gfx.setColor(Color.WHITE);
        gfx.setStroke(new BasicStroke(4.0f));
        gfx.drawRect(x, y, width, height);

        // targets the ball can possibly hit is marked yellow
        gfx.setColor(Color.yellow);
        for (Point p : corners.values()) {
            gfx.fillOval(p.x-2, p.y-2, 4, 4);
        }
    }
}