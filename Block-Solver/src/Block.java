/*
 * A block object holds both the row, col, width, height
 */

public class Block {

    private int row;
    private int col;
    private int length;
    private int width;

    public Block(int len, int wid, int r, int c) {
        this.row = r;
        this.col = c;
        this.length = len;
        this.width = wid;
    }
    
    public Block(Block b) {
        this.row = b.row;
        this.col = b.col;
        this.length = b.length;
        this.width = b.width;
    }
    
    public int getRow() {
        return this.row;
    }

    public int getCol() {
        return this.col;
    }

    public int getLen() {
        return this.length;
    }

    public int getWid() {
        return this.width;
    }

    public Block dupeBlock() {//make a copy of the old block
        return new Block(this.length, this.width, this.row, this.col);
    }

    public void move(int dirX, int dirY) {//move the block in certain direction
        this.row = this.row + dirY;
        this.col = this.col + dirX;
    }
    
    //checks if two blocks are equal by checking all properties
    @Override
    public boolean equals(Object b) {
        Block block = (Block) b;
        
        if (block == null) {
            return false;
        }
        if (this.row != block.getRow() || this.col != block.getCol()) {
            return false;
        }
        if (this.length != block.getLen() || this.width != block.getWid()) {
            return false;
        }
        return true;
    }
    
    /*
     * Adds row, col, length, and width to a string format and calling a hashcode
     * on that string
     */
    public int hashCode(){
        String code = "";
        
        code += (new Integer(this.row)).toString();
        code += (new Integer(this.col)).toString();
        code += (new Integer(this.length)).toString();
        code += (new Integer(this.width)).toString();
        
        return code.hashCode();
    }
    
    public void printBlock() {
        System.out.println(col + "  " + row + "  " + width + "  " + length);
    }
}