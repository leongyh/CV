import java.util.HashSet;
import java.util.Arrays;

public class Test {

    public static void main(String[] args) {
//        TrayEmptySpaceFirst t1 = new TrayEmptySpaceFirst(new InputSource("F:/Documents/Source Codes/Project 3/src/easy/easy"), false);
//        TrayEmptySpaceFirst t2 = new TrayEmptySpaceFirst(new InputSource("F:/Documents/Source Codes/Project 3/src/easy/full.1"), false);
//        TrayEmptySpaceFirst t3 = new TrayEmptySpaceFirst(new InputSource("F:/Documents/Source Codes/Project 3/src/easy/easy"), false);
//        HashSet<TrayEmptySpaceFirst> set =  new HashSet();
//        
////        Block b1 = t.getBlock(1, 0);
////        Block b2 = t2.getBlock(0, 1);
//        
//        System.out.println(t1.equals(t2));
//        System.out.println(t1.equals(t3) + "   " + t1.hashCode() + "   " + t2.hashCode());
//        
//        set.add(t1);
//        System.out.println(set.contains(t3));
//        
//        System.out.println(set.getClass().getName());
//  
//        t.move(b1, 0, 1);
//        t2.move(b2, 1, 0);
//        System.out.println(t.equals(t2) + "   " + t.hashCode() + "   " + t2.hashCode());
//        
//        t.move(b1, 0, -1);
//        t2.move(b2, -1, 0);
//        System.out.println(t.equals(t2) + "   " + t.hashCode() + "   " + t2.hashCode());
        
//        set.add(t);
//        System.out.println(set.contains(t2));
        
        int [] a1 = {0, 1};
        int [] a2 = {1, 0};
        int [] a3 = {1, 0};
        
        System.out.println(Arrays.deepHashCode(new Object[]{a3}));
        System.out.println(Arrays.deepHashCode(new Object[]{a2}));
    }
}
