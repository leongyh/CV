import java.util.HashMap;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.lang.Integer;

public class Tray1 {
//    private BlockHashtable blocks;//hashtable for the blocks
    private HashMap<Integer, Block> blocks;
    private Block[][] occupancyMatrix;//2-d array checking empty spaces and occupied spaces in the tray
    private int row;
    private int col;
    private String[] prevMove;
    private Tray1 previous;
    private boolean displayDebug;

    public Tray1() {
    }

    public Tray1(InputSource in, boolean debug) {//initialize the tray
        String s = in.readLine();
        StringTokenizer Token = new StringTokenizer(s, " ");
        this.row = Integer.parseInt(Token.nextToken());
        this.col = Integer.parseInt(Token.nextToken());
        this.blocks = new ArrayList();
//        this.blocks = new BlockHashtable(col, row);//input the dimension of the tray and construct a hashtable
        this.occupancyMatrix = new boolean[col][row];
        this.prevMove = new String[5];
        this.previous = null;
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
            this.blocks.add(b);

            for (int i = blockCol; i < blockCol + blockWidth; i++) {
                for (int j = blockRow; j < blockRow + blockLength; j++) {
                    this.occupancyMatrix[i][j] = true;
                }
            }

            s = in.readLine();
        }
    }

    public Tray1(Tray1 t) {//make a duplicate copy of the tray
        this.row = t.row;
        this.col = t.col;
        this.occupancyMatrix = new boolean [this.col][this.row];
        
        
        this.blocks = new ArrayList(t.blocks);
        
        this.occupancyMatrix = new boolean[t.occupancyMatrix.length][t.occupancyMatrix[0].length];
        for (int i = 0; i < t.occupancyMatrix.length; i++) {
            for (int j = 0; j < t.occupancyMatrix[0].length; j++) {
                this.occupancyMatrix[i][j] = t.occupancyMatrix[i][j];
            }
        }
        
        this.prevMove = new String[5];
    }

    public void move(Block b, int dirX, int dirY) {//move block horizontally or vertically with one step
        if (this.isOK(b, dirX, dirY)) {
            Block refBlock = b.dupeBlock();
            //record last move
            this.prevMove[0] = (new Integer(b.getRow())).toString();
            this.prevMove[1] = (new Integer(b.getCol())).toString();
            this.prevMove[2] = (new Integer(b.getRow() + dirY)).toString();
            this.prevMove[3] = (new Integer(b.getCol() + dirX)).toString();
            if(this.displayDebug){
                this.prevMove[4] = ""; //fill info here
            }
            //move the block and rehash the tray
            b.move(dirX, dirY);
            this.blocks.rehash(refBlock, b);
            this.moveMatrix(refBlock, dirX, dirY);
        }
    }

    public boolean[][] getOccupancy() {//get the occupancy information from the 2-d array
        return this.occupancyMatrix;
    }

    public int getRow() {
        return this.row;
    }

    public int getCol() {
        return this.col;
    }
    
    public Block getBlock(int c, int r) {//get the corresponding block to a certain spot
        return this.blocks.getBlock(c, r);
    }

    public HashSet<Block> getBlocks() {
        return this.blocks;
    }

    public String[] getPrevMove() {
        return this.prevMove;
    }

    public void setPrev(Tray1 prevTray) {
        this.previous = prevTray;
    }

    public Tray1 getParent() {
        return this.previous;
    }

    private void moveMatrix(Block b, int dirX, int dirY) {
        for (int i = b.getCol(); i < b.getCol() + b.getWid(); i++) {
            for (int j = b.getRow(); j < b.getRow() + b.getLen(); j++) {
                this.occupancyMatrix[i][j] = false;
            }
        }

        for (int i = b.getCol() + dirX; i < b.getCol() + b.getWid() + dirX; i++) {
            for (int j = b.getRow() + dirY; j < b.getRow() + b.getLen() + dirY; j++) {
                this.occupancyMatrix[i][j] = true;
            }
        }
    }

    private boolean isOK(Block b, int dirX, int dirY) {//check if the next move is valid
        if (b.getCol() + dirX >= 0 && b.getRow() + dirY >= 0 && b.getCol() + b.getWid() + dirX - 1 < this.col && b.getRow() + b.getLen() + dirY - 1 < this.row) {
            if (dirX > 0) {
                for (int i = b.getRow(); i < b.getRow() + b.getLen(); i++) {
                    if (this.occupancyMatrix[b.getCol() + b.getWid() - 1 + dirX][i]) {
                        throw new IllegalStateException();
                    }
                }
            } else if (dirX < 0) {
                for (int i = b.getRow(); i < b.getRow() + b.getLen(); i++) {
                    if (this.occupancyMatrix[b.getCol() + dirX][i]) {
                        throw new IllegalStateException();
                    }
                }
            } else if (dirY > 0) {
                for (int i = b.getCol(); i < b.getCol() + b.getWid(); i++) {
                    if (this.occupancyMatrix[i][b.getRow() + b.getLen() - 1 + dirY]) {
                        throw new IllegalStateException();
                    }
                }
            } else if (dirY < 0) {
                for (int i = b.getCol(); i < b.getCol() + b.getWid(); i++) {
                    if (this.occupancyMatrix[i][b.getRow() + dirY]) {
                        throw new IllegalStateException();
                    }
                }
            }
        } else {
            throw new IllegalStateException();
        }
        return true;
    }
    
    @Override
    public boolean equals(Object t) {
        //check if the two trays have same size
        if (((Tray1)t).col == this.col && ((Tray1)t).row == this.row) {
            //check if the two trays have same blocks inside 
            return this.blocks.equals(((Tray1)t).blocks);
        }
        return false;
    }

    public int hashCode() {
        return this.blocks.hashCode();
    }

    public void printBlocks() {
        this.blocks.printTable();
    }
}