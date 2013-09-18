/*
 * A goal object stores all blocks that are supposed to be in that position.
 * It also checks if a tray is in its goal config.
 */

import java.util.StringTokenizer;
import java.lang.Integer;
import java.util.ArrayList;

public class Goal {
    private ArrayList<Block> list;
    
    public Goal(){
        
    }
    
    public Goal(InputSource goalFile){
        String s = goalFile.readLine();
        list = new ArrayList();
        
        while (s != null) {//add blocks into the hashtable
            StringTokenizer token = new StringTokenizer(s, " ");

            int blockLength = Integer.parseInt(token.nextToken());
            int blockWidth = Integer.parseInt(token.nextToken());
            int blockRow = Integer.parseInt(token.nextToken());
            int blockCol = Integer.parseInt(token.nextToken());

            Block b = new Block(blockLength, blockWidth, blockRow, blockCol);
            this.list.add(b);

            s = goalFile.readLine();
        }
    }
    
    /*
     * Checks if a tray is in a goal config by calling all goal blocks and 
     * looks up the occupancy matrix.
     */
    public boolean isSolution(Tray t){
        Block[][] occupancy = t.getOccupancy();
        
        for(int i = 0; i < list.size(); i++){
            if(!(list.get(i)).equals(occupancy[list.get(i).getCol()][list.get(i).getRow()])){
                return false;
            }
        }
        return true;
    }
}
