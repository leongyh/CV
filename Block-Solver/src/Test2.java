
import java.util.HashSet;

public class Test2 {

    public static void main(String[] args) {
        
        HashSet<Block> set = new HashSet<Block>();
        Block b1 = new Block(1, 2, 1, 2);
        Block b2 = new Block(1, 2, 1, 2);
        Block b3 = new Block(2, 2, 1, 2);

        set.add(b1);
        System.out.println(set.contains(b3));
    }
}
