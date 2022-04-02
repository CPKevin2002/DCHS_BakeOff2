import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import processing.core.PApplet;


public class BakeOffTwo extends PApplet {


    /** Color palette: ARGB format */
    /* TODO: feel free to change those colors as you see fit! */
    int white = 0xFFFFFFFF;
    int correctPositionSquareColor = 0x6400FF00;
    int defaultSquareColor = 0xC03C3CC0;
    int incorrectOrientationOrSizeColor = 0x6400CCCC;
    int correctOrientationOrSizeColor = 0x6400FF00;
    int green = 0xFF00FF00;


    /** Set true to combine rotation and resizing */
    boolean combineRotateResize = true;


    //Variables that control the canvas size
    public final int canvasWidth = 1000;
    public final int canvasHeight = 800;


    //these are variables you should probably leave alone
    float border = 0; //some padding from the sides of window, set later
    int trialCount = 12; //this will be set higher for the bakeoff
    int trialIndex = 0; //what trial are we on
    int errorCount = 0;  //used to keep track of errors
    float errorPenalty = 0.5f; //for every error, add this value to mean time
    int startTime = 0; // time starts when the first click is captured
    int finishTime = 0; //records the time of the final click
    boolean userDone = false; //is the user done

    final int screenPPI = 72; //what is the DPI of the screen you are using
//you can test this by drawing a 72x72 pixel rectangle in code, and then confirming with a ruler it is 1x1 inch.

    //These variables are for my example design. Your input code should modify/replace these!
    float logoX = 500;
    float logoY = 500;
    float logoZ = 50f;
    float logoRotation = 0;


    //Mouse robot
    Robot robot = null;

    Stage S = Stage.POSITION;

    Stage[] allStages = combineRotateResize?
            new Stage[] {Stage.POSITION, Stage.ORIENTATION} :
            new Stage[]{Stage.POSITION, Stage.ORIENTATION, Stage.SIZE};

    int stageIndex = 0;

    boolean canUndo = true;

    /** 3 stages of the program */
    private enum Stage {
        POSITION,
        ORIENTATION,
        SIZE
    }

    /** Information about the destinations */
    private class Destination
    {
        float x = 0;
        float y = 0;
        float rotation = 0;
        float z = 0;
    }

    /** Class that captures information for rotation */
    private class RotationFrame {
        int mouseX;
        float logoRotation;
    }

    /** Class that captures information for resizing */
    private class SizeFrame {
        int mouseY;
        float logoZ;
    }

    /** Class that captures information for position */
    private class PositionFrame {
        boolean canFollow;
    }


    RotationFrame RF = null;
    SizeFrame SF = null;
    PositionFrame PF = null;

    ArrayList<Destination> destinations = new ArrayList<Destination>();

    public static void main(String[] args) {
        PApplet.main("BakeOffTwo");
    }

    public void settings() {
        size(canvasWidth, canvasHeight);
    }

    public void setup() {

        rectMode(CENTER);
        textFont(createFont("Arial", inchToPix(.3f))); //sets the font to Arial that is 0.3" tall
        textAlign(CENTER);
        rectMode(CENTER); //draw rectangles not from upper left, but from the center outwards

        //don't change this!
        border = inchToPix(2f); //padding of 1.0 inches

        for (int i=0; i<trialCount; i++) //don't change this!
        {
            Destination d = new Destination();
            d.x = random(border, width-border); //set a random x with some padding
            d.y = random(border, height-border); //set a random y with some padding
            d.rotation = random(0, 360); //random rotation between 0 and 360
            int j = (int)random(20);
            d.z = ((j%12)+1)*inchToPix(.25f); //increasing size from .25 up to 3.0"
            destinations.add(d);
            println("created target with " + d.x + "," + d.y + "," + d.rotation + "," + d.z);
        }

        Collections.shuffle(destinations); // randomize the order of the button; don't change this.
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }



    public void draw() {
        background(40); //background is dark grey
        fill(200);
        noStroke();

        //shouldn't really modify this printout code unless there is a really good reason to
        if (userDone)
        {
            text("User completed " + trialCount + " trials", width/2, inchToPix(.4f));
            text("User had " + errorCount + " error(s)", width/2, inchToPix(.4f)*2);
            text("User took " + (finishTime-startTime)/1000f/trialCount + " sec per destination", width/2, inchToPix(.4f)*3);
            text("User took " + ((finishTime-startTime)/1000f/trialCount+(errorCount*errorPenalty)) + " sec per destination inc. penalty", width/2, inchToPix(.4f)*4);
            return;
        }


        //===========DRAW DESTINATION SQUARES=================
        for (int i=trialIndex; i<trialCount; i++) // reduces over time
        {
            pushMatrix();
            Destination d = destinations.get(i); //get destination trial
            translate(d.x, d.y); //center the drawing coordinates to the center of the destination trial
            rotate(radians(d.rotation)); //rotate around the origin of the destination trial
            noFill();
            strokeWeight(3f);
            if (trialIndex==i)
                stroke(255, 0, 0, 192); //set color to semi translucent
            else
                stroke(128, 128, 128, 128); //set color to semi translucent
            rect(0, 0, d.z, d.z);
            popMatrix();
        }

        //===========DRAW LOGO SQUARE=================
        pushMatrix();
        translate(logoX, logoY); //translate draw center to the center of the logo square
        rotate(radians(logoRotation)); //rotate using the logo square as the origin
        noStroke();
        setColorWRTPosition();
        rect(0, 0, logoZ, logoZ);
        popMatrix();

        //===========DRAW EXAMPLE CONTROLS=================
        fill(white);
        doControlLogic();
        fill(white);
        text("Trial " + (trialIndex+1) + " of " +trialCount, width/2, inchToPix(.8f));
    }


    /** Overall control logic */
    public void doControlLogic() {
        if (combineRotateResize) {
            do2StepControlLogic();
        }  else {
            do3StepControlLogic();
        }
    }

    public void do3StepControlLogic() {
        switch (S) {
            case POSITION:
                RF = null;
                SF = null;
                doPositionControl();
                break;

            case ORIENTATION:
                doOrientationControl();
                break;

            case SIZE:
                doSizeControl();
                break;
        }
    }

    public void do2StepControlLogic() {
        switch (S) {
            case POSITION:
                RF = null;
                SF = null;
                doPositionControl();
                break;

            case ORIENTATION:
                doOrientationControl();
                doSizeControl();
                break;
        }

    }

    /** Helper function to turn the square green when position is correct */
    public void setColorWRTPosition() {
        if (isPositionCorrect())
        {
            fill(correctPositionSquareColor);
        } else {
            fill(defaultSquareColor);
        }
    }


    //utility function to convert inches into pixels based on screen PPI
    public float inchToPix(float inch)
    {
        return inch*screenPPI;
    }


    /** Below are helper functions for position control */

    public void followMouse() {
        if (PF == null) {
            PF = new PositionFrame();
            PF.canFollow = true;
        }
        // have the mouse snap to the square
        if (abs(mouseX - logoX) <= 10 && abs(mouseY - logoY) <= 10) {
            PF.canFollow = true;
        }
        if (PF.canFollow) {
            logoX = mouseX;
            logoY = mouseY;
        }
    }

    /** Show the center of the target square and the logo square
     * Might want to change this to make it easier to align the centers */
    public void showCorrectCenterLocation() {
        if (isPositionCorrect()) {
            fill(green);
        } else {
            fill(white);
        }
        Destination target = destinations.get(trialIndex);
        circle(logoX, logoY, inchToPix(.05f));
        circle(target.x, target.y, inchToPix(.05f));
    }


    public boolean isPositionCorrect() {
        Destination d = destinations.get(trialIndex);
        boolean closeDist = dist(d.x, d.y, logoX, logoY)<inchToPix(.05f); //has to be within +-0.05"
        return closeDist;
    }

    public void doPositionControl() {
        showCorrectCenterLocation();
        followMouse();
    }


    // Helper functions for orientation control

    /** Draw the vertical rectangle when rotating */
    public void drawSweepingLine() {
        rectMode(CORNER);
        int temp = g.fillColor;
        if (isOrientationCorrect()) {
            fill(correctOrientationOrSizeColor);
        } else {
            fill(incorrectOrientationOrSizeColor);
        }
        rect(0,0, mouseX, canvasHeight);
        rectMode(CENTER);
        g.fillColor = temp;
    }

    public boolean isOrientationCorrect() {
        Destination d = destinations.get(trialIndex);
        return calculateDifferenceBetweenAngles(d.rotation, logoRotation)<=5;
    }


    /** Show the nearest line for rotation */
    public int showNearestRotationTarget() {
        float targetRotation = destinations.get(trialIndex).rotation % 90;
        float diff = targetRotation - RF.logoRotation;
        int X = RF.mouseX;
        if (diff > 0 && diff < 45) {
            X += diff / 360.0 * canvasWidth;
        } else if (diff >= 45) {
            X -= (90 - diff) / 360.0 * canvasWidth;
        } else if (diff < 0 && diff > -45) {
            X -= abs(diff) / 360.0 * canvasWidth;
        } else {
            X += (90 - abs(diff)) / 360.0 * canvasWidth;
        }
        int temp = g.strokeColor;
        boolean haveStroke = g.stroke;
        stroke(255);
        line(X, 0, X, canvasHeight);
        g.strokeColor = temp;
        g.stroke = haveStroke;
        return X;
    }

    /** Map x-axis to rotation */
    public void horizontalToRotation() {
        if (RF == null) {
            RF = new RotationFrame();
            RF.mouseX = mouseX;
            RF.logoRotation = logoRotation % 90;
        }
        float relativeXRatio = (float) (mouseX - RF.mouseX) / canvasWidth;
        logoRotation = RF.logoRotation + relativeXRatio * 360;
    }


    /** Wrapper function for orientation control */
    public void doOrientationControl() {
        PF = null;
        drawSweepingLine();
        horizontalToRotation();
        showNearestRotationTarget();
    }


    // Helper functions for resizing
    public boolean isSizeCorrect() {
        Destination d = destinations.get(trialIndex);
        boolean closeZ = abs(d.z - logoZ)<inchToPix(.1f); //has to be within +-0.1"
        return closeZ;
    }

    /** Draw the horizontal rectangle when rotating */
    public void drawSeaLevel() {
        boolean haveStroke = g.stroke;
        rectMode(CORNER);
        int temp = g.fillColor;
        if (isSizeCorrect()) {
            fill(0,255,0, 100);
        } else {
            fill(0, 204, 204, 100);
        }
        noStroke();
        rect(0, mouseY, canvasWidth, canvasHeight);
        rectMode(CENTER);
        g.fillColor = temp;
        g.stroke = haveStroke;
    }

    /** Map y-axis to z-axis */
    public void verticalToResizing() {
        if (SF == null) {
            SF = new SizeFrame();
            SF.logoZ = logoZ;
            SF.mouseY = mouseY;
        }
        float mappingCoefficient = computeRatios();
        float penalty = 0f;

        // Regularization constant that prevents the square from getting too big
        float regularizationConstant = 0.7f;
        float rawSize = Math.max(0, SF.logoZ  + (SF.mouseY - mouseY) * mappingCoefficient);
        if (rawSize > inchToPix(3f)) {
            penalty = (rawSize - inchToPix(3f)) * regularizationConstant;
        }

        logoZ = Math.max(0, SF.logoZ  + (SF.mouseY - mouseY) * mappingCoefficient) - penalty;
    }

    public void showNearestTargetResizing() {
        float targetZ = destinations.get(trialIndex).z;
        float mappingCoefficient = computeRatios();
        int Y= (int) ((int) SF.mouseY - (targetZ - SF.logoZ) / mappingCoefficient);
        int temp = g.strokeColor;
        stroke(255);
        line(0, Y, canvasWidth, Y);
        g.strokeColor = temp;
    }

    private float computeRatios() {
        float smallestZ = inchToPix(0.25f);
        float largestZ = inchToPix(3f);
        float logoRatio = (SF.logoZ - smallestZ) / (largestZ - smallestZ);
        float upperBound = SF.mouseY +  logoRatio * 1.2f * border;
        float lowerBound = SF.mouseY - (1f - logoRatio) * 1.2f * border;
        return (largestZ - smallestZ) / (upperBound - lowerBound);
    }

    /** Wrapper function for size control */
    public void doSizeControl() {
        drawSeaLevel();
        verticalToResizing();
        showNearestTargetResizing();
    }


   // Event handling helper functions

    /** Handles the undo function.
     * When the mouse exits the current window, the program goes back 1 stage*/
    public void mouseExited() {
        if (S != Stage.POSITION && canUndo) {
            stageIndex = (stageIndex - 1) % (allStages.length);
            S = allStages[stageIndex];
            canUndo = false;
        }
    }
    public void mouseEntered() {
        canUndo = true;
    }

    public void mousePressed()
    {
        if (startTime == 0) //start time on the instant of the first user click
        {
            startTime = millis();
            println("time started!");
        }
    }

    public void mouseReleased()
    {
        if (stageIndex == allStages.length - 1) {
            if (!userDone && !checkForSuccess())
                errorCount++;
            trialIndex++; //and move on to next trial
            if (trialIndex==trialCount && !userDone)
            {
                userDone = true;
                finishTime = millis();
            }
        }
        stageIndex = (stageIndex + 1) % allStages.length;
        S = allStages[stageIndex];

        if (S == Stage.POSITION) {
            int XOffset = 220;
            int YOffset = 53;
            robot.mouseMove((int) logoX + XOffset,(int) logoY + YOffset);
            mouseX = (int) logoX;
            mouseY = (int) logoY;
        }
    }



    // Useless helper functions in the starter code
    public boolean checkForSuccess()
    {
        Destination d = destinations.get(trialIndex);
        boolean closeDist = dist(d.x, d.y, logoX, logoY)<inchToPix(.05f); //has to be within +-0.05"
        boolean closeRotation = calculateDifferenceBetweenAngles(d.rotation, logoRotation)<=5;
        boolean closeZ = abs(d.z - logoZ)<inchToPix(.1f); //has to be within +-0.1"

        println("Close Enough Distance: " + closeDist + " (logo X/Y = " + d.x + "/" + d.y + ", destination X/Y = " + logoX + "/" + logoY +")");
        println("Close Enough Rotation: " + closeRotation + " (rot dist="+calculateDifferenceBetweenAngles(d.rotation, logoRotation)+")");
        println("Close Enough Z: " +  closeZ + " (logo Z = " + d.z + ", destination Z = " + logoZ +")");
        println("Close enough all: " + (closeDist && closeRotation && closeZ));
        return closeDist && closeRotation && closeZ;
    }

    double calculateDifferenceBetweenAngles(float a1, float a2)
    {
        double diff=abs(a1-a2);
        diff%=90;
        if (diff>45)
            return 90-diff;
        else
            return diff;
    }

}
