/*
 * Debugger object stores how solver will run. It will also show debugging info.
 */

public class Debugger {

    private boolean moves;
    private boolean timer;
    private boolean depth;
    private boolean EmptySpaceFirst;
    private boolean DFS;
    private boolean BFS;
    private boolean PDFS;
    private boolean IDFS;

    public Debugger() {
        this.moves = false;
        this.timer = true;
        this.depth = true;
        this.EmptySpaceFirst = true;
        this.DFS = false;
        this.BFS = false;
        this.PDFS = false;
        this.IDFS = true;
    }
    
    public void loadDefaults(){
        this.IDFS = true;
        this.EmptySpaceFirst = true;
    }

    public void activateMoves() {
        this.moves = true;
    }

    public void activateTimer() {
        this.timer = true;
    }
    
    public void activateDepth(){
        this.depth = true;
    }

    public void activateEmptySpaceFirst() {
        this.EmptySpaceFirst = true;
    }

    public void activateDFS() {
        this.DFS = true;
        this.IDFS = false;
    }

    public void activateBFS() {
        this.BFS = true;
        this.IDFS = false;
    }

    public void activatePDFS() {
        this.PDFS = true;
        this.IDFS = false;
    }
    
    public void activateIDFS() {
        this.IDFS = true;
    }

    public boolean getMoves() {
        return this.moves;
    }

    public boolean getTimer() {
        return this.timer;
    }
    
    public boolean getDepth(){
        return this.depth;
    }

    public boolean getEmptySpaceFirst() {
        return this.EmptySpaceFirst;
    }

    public boolean getDFS() {
        return this.DFS;
    }

    public boolean getBFS() {
        return this.BFS;
    }

    public boolean getPDFS() {
        return this.PDFS;
    }

    public boolean getIDFS() {
        return this.IDFS;
    }
    
    //isOK checks if the debugger is in a valid state. For example, it cant have
    //two different tray search methods. throws illegal argument exception.
    public void isOK() {
        int searchMethod = 0;
        int TrayType = 0;

        if (this.getDFS()) {
            searchMethod++;
        }
        if (this.getBFS()) {
            searchMethod++;
        }
        if (this.getPDFS()) {
            searchMethod++;
        }
        if (this.getEmptySpaceFirst()) {
            TrayType++;
        }

        if (searchMethod > 1 || TrayType > 1) {
            throw new IllegalArgumentException("Bad debugger options input!");
        }
    }
}
