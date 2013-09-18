/*
 * Tray is represented in two different ways, by blocks and and by a matrix.
 * Blocks are stored in a hash map and the occupancy matrix stores references to
 * these block objects based on the row, col in the tray. empty spaces are null.
 * Tray objects store the previous move made to get to the current config, a direction
 * variable, a depth, and a reference to its previous tray.
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.lang.Integer;

public class Tray {

    private HashMap<Integer, Block> blocks;
    private Block[][] occupancyMatrix;//2-d array checking empty spaces and occupied spaces in the tray
    private int row;
    private int col;
    private String[] prevMove;
    private Tray previous;
    private int direction = -1; // -1 is null. 0 up, 1 right, 2 down, 3 left
    private int depth;
    private boolean displayDebug;

    public Tray() {
    }
    
    //constructs the very first tray
    public Tray(InputSource in, boolean debug) {
        String s = in.readLine();

        StringTokenizer Token = new StringTokenizer(s, " ");
        this.row = Integer.parseInt(Token.nextToken());
        this.col = Integer.parseInt(Token.nextToken());
        this.blocks = new HashMap();
        this.occupancyMatrix = new Block[this.col][this.row];
        this.prevMove = new String[5];
        this.previous = null;
        this.depth = 0;
        this.displayDebug = debug;

        int blockRow, blockCol, blockLength, blockWidth;

        s = in.readLine();

        while (s != null) {//add blocks into the hashtable
            Token = new StringTokenizer(s, " ");

            blockLength = Integer.parseInt(Token.nextToken());
            blockWidth = Integer.parseInt(Token.nextToken());
            blockRow = Integer.parseInt(Token.nextToken());
            blockCol = Integer.parseInt(Token.nextToken());

            Block b = new Block(blockLength, blockWidth, blockRow, blockCol);
            this.blocks.put(b.hashCode(), b);

            for (int i = blockCol; i < blockCol + blockWidth; i++) {
                for (int j = blockRow; j < blockRow + blockLength; j++) {
                    this.occupancyMatrix[i][j] = b;
                }
            }

            s = in.readLine();
        }
    }

    public Tray(Tray t) {//make a duplicate copy of the tray
        this.row = t.row;
        this.col = t.col;
        this.depth = t.depth;
        this.occupancyMatrix = new Block[this.col][this.row];
        this.blocks = new HashMap();
        this.prevMove = new String[5];

        //makes the HashMap iterable to get all blocks on tray
        Collection<Block> listOfBlocks = t.blocks.values();
        Iterator<Block> iter = listOfBlocks.iterator();

        //begin deep copy of the hashtable and matrix
        while (iter.hasNext()) {
            Block b = iter.next();

            int blockLength = b.getLen();
            int blockWidth = b.getWid();
            int blockRow = b.getRow();
            int blockCol = b.getCol();

            Block newBlock = new Block(blockLength, blockWidth, blockRow, blockCol);
            this.blocks.put(newBlock.hashCode(), newBlock);

            for (int i = blockCol; i < blockCol + blockWidth; i++) {
                for (int j = blockRow; j < blockRow + blockLength; j++) {
                    this.occupancyMatrix[i][j] = newBlock;
                }
            }
        }
    }

    public Tray move(int x, int y, int dirX, int dirY) {//move block horizontally or vertically with one step
        Block b = this.occupancyMatrix[x][y];

        if (this.isOK(b, dirX, dirY)) {
            Tray movedTray = new Tray(this);

            //record last move
            movedTray.prevMove[0] = (new Integer(b.getRow())).toString();
            movedTray.prevMove[1] = (new Integer(b.getCol())).toString();
            movedTray.prevMove[2] = (new Integer(b.getRow() + dirY)).toString();
            movedTray.prevMove[3] = (new Integer(b.getCol() + dirX)).toString();

            //adds the string to the end of solution output showing block moved
            //if debug is on
            if (movedTray.displayDebug) {
                movedTray.prevMove[4] = "Moved " + b.getRow() + " x " + b.getCol()
                        + " block from (" + x + ", " + y + ") to (" + (x + dirX)
                        + ", " + (y + dirY) + ").";
            }

            //saves the moved direction
            if (dirX == 1) {
                movedTray.direction = 1;
            } else if (dirX == -1) {
                movedTray.direction = 3;
            } else if (dirY == 1) {
                movedTray.direction = 2;
            } else if (dirY == -1) {
                movedTray.direction = 0;
            } else {
                throw new IllegalStateException();
            }
            //removes block from hashmap, move block and change properties,
            //re-hash. Also modifies matrix
            Block movedBlock = movedTray.blocks.get(b.hashCode());
            movedTray.blocks.remove(movedBlock.hashCode());
            movedBlock.move(dirX, dirY);
            movedTray.blocks.put(movedBlock.hashCode(), movedBlock);
            movedTray.moveMatrix(movedBlock, dirX, dirY);

            //adds depth
            movedTray.depth++;

            return movedTray;
        }
        return null;
    }
    
    //updates the matrix with the new move
    private void moveMatrix(Block b, int dirX, int dirY) {
        for (int i = b.getCol() - dirX; i < b.getCol() + b.getWid() - dirX; i++) {
            for (int j = b.getRow() - dirY; j < b.getRow() + b.getLen() - dirY; j++) {
                this.occupancyMatrix[i][j] = null;
            }
        }

        for (int i = b.getCol(); i < b.getCol() + b.getWid(); i++) {
            for (int j = b.getRow(); j < b.getRow() + b.getLen(); j++) {
                this.occupancyMatrix[i][j] = b;
            }
        }
    }
    
    //based on the block, see if move is valid. if a collision ooccurs or out
    // of trays bounds, throw an exception.
    private boolean isOK(Block b, int dirX, int dirY) {//check if the next move is valid
        if (b == null) {
            throw new IllegalStateException();
        }
        
        if (b.getCol() + dirX >= 0 && b.getRow() + dirY >= 0 && b.getCol() + b.getWid() + dirX - 1 < this.col && b.getRow() + b.getLen() + dirY - 1 < this.row) {
            if (dirX > 0) {
                for (int i = b.getRow(); i < b.getRow() + b.getLen(); i++) {
                    if (this.occupancyMatrix[b.getCol() + b.getWid() - 1 + dirX][i] != null) {
                        throw new IllegalStateException();
                    }
                }
            } else if (dirX < 0) {
                for (int i = b.getRow(); i < b.getRow() + b.getLen(); i++) {
                    if (this.occupancyMatrix[b.getCol() + dirX][i] != null) {
                        throw new IllegalStateException();
                    }
                }
            } else if (dirY > 0) {
                for (int i = b.getCol(); i < b.getCol() + b.getWid(); i++) {
                    if (this.occupancyMatrix[i][b.getRow() + b.getLen() - 1 + dirY] != null) {
                        throw new IllegalStateException();
                    }
                }
            } else if (dirY < 0) {
                for (int i = b.getCol(); i < b.getCol() + b.getWid(); i++) {
                    if (this.occupancyMatrix[i][b.getRow() + dirY] != null) {
                        throw new IllegalStateException();
                    }
                }
            }
        } else {
            throw new IllegalStateException();
        }
        
        return true;
    }

    public Block[][] getOccupancy() {//get the occupancy information from the 2-d array
        return this.occupancyMatrix;
    }

    public int getRow() {
        return this.row;
    }

    public int getCol() {
        return this.col;
    }

    public HashMap<Integer, Block> getBlocks() {
        return this.blocks;
    }

    public String[] getPrevMove() {
        return this.prevMove;
    }

    public int getDirection() {
        return this.direction;
    }

    public int getDepth() {
        return this.depth;
    }

    public void setPrev(Tray prevTray) {
        this.previous = prevTray;
    }

    public Tray getPrev() {
        return this.previous;
    }
    
    /*
     * Checks if two trays are equal. It iterates through the collection
     * of blocks one one tray and see if it is contained in the other's
     * hash map.
     */
    @Override
    public boolean equals(Object t) {
        Collection<Block> listOfBlocks = ((Tray) t).blocks.values();
        Iterator<Block> iter = listOfBlocks.iterator();

        //checks if all blocks are of equal positions
        while (iter.hasNext()) {
            Block b = iter.next();

            //if one block is not where its suppose to be, return false
            if (!this.blocks.containsKey(b.hashCode())) {
                return false;
            }
        }
        return true;
    }
    
    /*
     * A custom hashcode that represents a tray. This is done by getting the
     * hash code of every block in the hash map, putting into an array, and 
     * sorting the array before calling a deep hash code on it.  
     */
    public int hashCode() {
        Collection<Block> listOfBlocks = this.blocks.values();
        Iterator<Block> iter = listOfBlocks.iterator();

        int[] blockHashCode = new int[listOfBlocks.size()];
        int i = 0;

        while (iter.hasNext()) {
            Block b = iter.next();

            blockHashCode[i] = b.hashCode();
            i++;
        }
        
        //sorting is important as different order of the blocks returned even
        //though they are the same will produce a different deep hash code
        Arrays.sort(blockHashCode);
        
        return Arrays.deepHashCode(new Object[]{blockHashCode});
    }
}