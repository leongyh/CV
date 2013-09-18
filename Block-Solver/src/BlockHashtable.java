
import java.util.Arrays;
import java.lang.Integer;

public class BlockHashtable {

    private Block[] table;
    private int row;
    private int col;

    public BlockHashtable(int maxCol, int maxRow) {//construct a new hashtable
        this.row = maxRow;
        this.col = maxCol;
        this.table = new Block[maxRow * maxCol];
    }

    public BlockHashtable(BlockHashtable hash) {//construct a new hashtable
        this.row = hash.row;
        this.col = hash.col;
        this.table = new Block[this.row * this.col];

        for (int i = 0; i < hash.table.length; i++) {
            if (hash.table[i] != null) {
                this.table[i] = new Block(hash.table[i]);
            }
        }
    }

    public void addBlock(Block block) {//add a block into the hashtable	
        for (int i = block.getCol(); i < block.getCol() + block.getWid(); i++) {
            for (int j = block.getRow(); j < block.getRow() + block.getLen(); j++) {
                this.table[this.hashFunction(i, j)] = block;
            }
        }
    }

    public Block getBlock(int c, int r) {//get the block that covers position (c,r)
        return this.table[this.hashFunction(c, r)];
    }

    public Block[] getTable() {
        return this.table;
    }

    public void rehash(Block ini, Block fin) {//after a move, make a new hashtable with new empty spaces and new occupied spaces
        for (int i = ini.getCol(); i < ini.getCol() + ini.getWid(); i++) {
            for (int j = ini.getRow(); j < ini.getRow() + ini.getLen(); j++) {
                this.table[this.hashFunction(i, j)] = null;
            }
        }

        for (int i = fin.getCol(); i < fin.getCol() + fin.getWid(); i++) {
            for (int j = fin.getRow(); j < fin.getRow() + fin.getLen(); j++) {
                this.table[this.hashFunction(i, j)] = fin;
            }
        }
    }

    public boolean containsBlocks(BlockHashtable hash) {
        for (int i = 0; i < this.table.length; i++) {
            if (this.table[i] != null) {
                if (!this.table[i].equals(hash.table[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    private int hashFunction(int c, int r) {
        return (c + this.col * r);
    }

    public boolean equals(BlockHashtable b) {
        for (int i = 0; i < this.table.length; i++) {
            if (this.table[i] == null) {
                if (b.table[i] != null) {
                    return false;
                }
            } else if (!this.table[i].equals(b.table[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        Block currentBlock = null;
        String hashCode = "";

        for (int i = 0; i < table.length; i++) {
            if (currentBlock != table[i]) {
                currentBlock = table[i];

                if (currentBlock != null) {
                    hashCode += new Integer(currentBlock.getCol()).toString();
                    hashCode += new Integer(currentBlock.getRow()).toString();
                    hashCode += new Integer(currentBlock.getWid()).toString();
                    hashCode += new Integer(currentBlock.getLen()).toString();
                } else {
                    hashCode += "x";
                }
            }
        }
        return hashCode.hashCode();
    }

    public void printTable() {
        for (int i = 0; i < table.length; i++) {
            if (table[i] != null) {
                table[i].printBlock();
            } else {
                System.out.println("null");
            }
        }
    }
}