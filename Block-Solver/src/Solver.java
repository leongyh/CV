/*
 * Solver contains the main method with a debugger menu. The solver loads
 * both the starting confiugration file and the goal file. It then initializes 
 * a start tray and runs a search (using different search methods). A goal object
 * is also initialized and used to check is tray is it goal at each iteration.
 * A Hash Set is used to keep track of all seen tray configurations.
 */

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Stack;

public class Solver {

    private static Debugger debug = new Debugger(); //initializes the debugger

    /*
     * Initializes the solver based on the arugments passed. Initalzes an input,
     * goal, and modify debugger's settings if a debugger option is passed.
     * Prints debugger menu and runs solve method.
     *
     * @param String o is the debugger options @param String ini is the initial
     * config file name @param String fin is the goal file name
     */
    public static void solveInitializer(String o, String ini, String fin) {
        InputSource start = new InputSource(ini);
        InputSource goal = new InputSource(fin);

        if (o != null) {
            if (!((String) o.substring(0, 1)).equals("-o")) {
                throw new IllegalArgumentException("Unrecognized input. Prepend -o into info field.");
            } else {
                o = o.substring(2);
            }

            if (o.equals("info")) {
                System.out.println("Debugging Options Menu:");
                System.out.println("s: Run Empty Space First algorithm.");
                System.out.println("d, b, p, or i: Run with DFS, BFS, priority DFS method, or iterative depth method "
                        + "(default). Select only one.");
                System.out.println("m: Prints all steps.");
                System.out.println("t: Prints time taken to just run the search agorithm. "
                        + "Excludes initialization.");
                System.out.println("e: Prints current tray's depth");
                System.out.println("-------------------------------------------------------");
                System.out.println("Append each command as you like. Example '-osimt' runs the "
                        + "Empty Space First algorithm with iterative depth method and prints moves and time taken.");

            } else {
                if (o.contains("m")) {
                    debug.activateMoves();
                } else if (o.contains("t")) {
                    debug.activateTimer();
                } else if (o.contains("s")) {
                    debug.activateEmptySpaceFirst();
                } else if (o.contains("d")) {
                    debug.activateDFS();
                } else if (o.contains("b")) {
                    debug.activateBFS();
                } else if (o.contains("p")) {
                    debug.activatePDFS();
                }

                debug.isOK();
            }
        }

        if (debug.getEmptySpaceFirst()) {
            solve(start, goal);
        } else {
//            solve(start, goal);  supposedly other tray representation (block first). didnt have time.
        }
    }

    /*
     * Initialize a start tray, a goal object and a hash set. Hash set made is
     * to an optimal size. Based on the debugger options, it will run the
     * specific search method. Prints a solution if it finds it.
     *
     * @param InputSource ini Initial file @param InputSource fin Goal file
     */
    public static void solve(InputSource ini, InputSource fin) {
        Tray currentTray = new Tray(ini, debug.getMoves());
        Goal currentGoal = new Goal(fin);
        HashSet set = new HashSet(findMaxSize(currentTray));

        long initTime = System.nanoTime();

        //If case for no moves needed.
        if (currentGoal.isSolution(currentTray)) {
            long totalTime = System.nanoTime() - initTime;
            System.out.println("Solution Found!!");

            if (debug.getTimer()) {
                System.out.println(totalTime / 1000000 + "ms");
            }

            return;
        }

        /*
         * If case to select what search method is used based on the debugger.
         * All of these use a while loop to store tray configurations. these
         * trays are popped and moved. all moves are ended if a solution is
         * found or if the queuer is empty.
         */
        if (debug.getDFS()) {
            Stack<Tray> stack = new Stack();

            stack.add(currentTray);
            set.add(currentTray);

            while (!stack.isEmpty() && !currentGoal.isSolution(currentTray)) {
                currentTray = stack.pop();

                if (debug.getDepth()) {
                    System.out.println(currentTray.getDepth());
                }

                findAllPossible(currentTray, stack, set, -1);
            }
        } else if (debug.getPDFS()) {
            PriorityQueue priorityQueue = new PriorityQueue();

            priorityQueue.add(currentTray);
            set.add(currentTray);

            while (!priorityQueue.isEmpty() && !currentGoal.isSolution(currentTray)) {
                currentTray = (Tray) priorityQueue.dequeue();

                if (debug.getDepth()) {
                    System.out.println(currentTray.getDepth());
                }

                findAllPossible(currentTray, priorityQueue, set, -1);
            }
        } else if (debug.getBFS()) {
            LinkedList<Tray> queue = new LinkedList();

            queue.addFirst(currentTray);
            set.add(currentTray);

            while (!queue.isEmpty() && !currentGoal.isSolution(currentTray)) {
                currentTray = queue.removeLast();

                if (debug.getDepth()) {
                    System.out.println(currentTray.getDepth());
                }

                findAllPossible(currentTray, queue, set, -1);
            }
        } else if (debug.getIDFS()) {
            PriorityQueue priorityQueue = new PriorityQueue();
            int depth = 81; //default depth as 81

            priorityQueue.add(currentTray);
            set.add(currentTray);

            while (!priorityQueue.isEmpty() && !currentGoal.isSolution(currentTray)) {
                currentTray = (Tray) priorityQueue.dequeue();

                if (currentTray.getDepth() > depth) {
                    depth += 50; //once depth threshold met, increment by 50
                }

                if (debug.getDepth()) {
                    System.out.println(currentTray.getDepth());
                }

                findAllPossible(currentTray, priorityQueue, set, depth);
            }
        }

        //if a solution is found if goal matches tray, else no solution
        if (currentGoal.isSolution(currentTray)) {
            long totalTime = System.nanoTime() - initTime;
            System.out.println("Solution Found!!");
            print(produceSolution(currentTray), debug);//print the moves

            if (debug.getTimer()) {
                System.out.println(totalTime / 1000000 + "ms");
            }

            return;
        } else {
            long totalTime = System.nanoTime() - initTime;
            System.out.println("No Solution Found :(");

            if (debug.getTimer()) {
                System.out.println(totalTime / 1000000 + "ms");
            }

            System.exit(1);
        }
    }

    /*
     * Finds all empty spaces from the tray's occupancy matrix and moves
     * adjacent blocks. Uses a double for loop to iterate through the matrix.
     *
     *
     * @param Tray currTray tray being moved @param Object insert the queuer
     * @param HashSet the hash set @param depth depth threshold that determines
     * priority in the queue. -1 value if the queuer doesnt need depth
     * parameter.
     */
    private static void findAllPossible(Tray currTray, Object insert, HashSet hash, int depth) {
        Block[][] occMatrix = currTray.getOccupancy();

        for (int i = 0; i < currTray.getCol(); i++) {
            for (int j = 0; j < currTray.getRow(); j++) {
                if (occMatrix[i][j] == null) {
                    moveAllDirections(currTray, insert, hash, i - 1, j, depth);
                    moveAllDirections(currTray, insert, hash, i + 1, j, depth);
                    moveAllDirections(currTray, insert, hash, i, j - 1, depth);
                    moveAllDirections(currTray, insert, hash, i, j + 1, depth);
                }
            }
        }
    }

    /*
     * Moves all directions of the block on (x, y) of current tray, up down left
     * right
     *
     * @param Tray t tray being moved @param Object insert the queuer @param
     * HashSet the hash set @param int x the col location of the block @param
     * int y the row of the block @param depth depth threshold that determines
     * priority in the queue. -1 value if the queuer doesnt need depth
     * parameter.
     */
    private static void moveAllDirections(Tray t, Object insert, HashSet hash, int x, int y, int depth) {
        moveIfPossible(t, insert, hash, x, y, -1, 0, depth);
        moveIfPossible(t, insert, hash, x, y, 1, 0, depth);
        moveIfPossible(t, insert, hash, x, y, 0, -1, depth);
        moveIfPossible(t, insert, hash, x, y, 0, 1, depth);
    }

    /*
     * If the block is within boundaries, move. Try and uses tray move function
     * if possible. If not possible, throws IllegalStateException. It is caught
     * nothing happens. If it passes, adds the new move tray to the queuer and
     * hash set.
     *
     * @param Tray t tray being moved @param Object insert the queuer @param
     * HashSet the hash set @param int x the col location of the block @param
     * int y the row of the block @param dirX x direction it moves @param dirY y
     * direction it moves @param depth depth threshold that determines priority
     * in the queue. -1 value if the queuer doesnt need depth parameter.
     */
    private static void moveIfPossible(Tray t, Object insert, HashSet hash, int x, int y, int dirX, int dirY, int depth) {
        if (x >= 0 && y >= 0 && x < t.getCol() && y < t.getRow()) {
            try {
                Tray movedTray = t.move(x, y, dirX, dirY);

                if (!hash.contains(movedTray) && movedTray != null) {
                    movedTray.setPrev(t);

                    //depends on what time of queuer is added, does the insertion appropriately
                    if (debug.getDFS()) {
                        ((Stack) insert).push(movedTray);
                    } else if (debug.getBFS()) {
                        ((LinkedList) insert).addFirst(movedTray);
                    } else if (debug.getPDFS()) {
                        ((PriorityQueue) insert).add(movedTray, dirX, dirY);
                    } else if (debug.getIDFS()) {
                        ((PriorityQueue) insert).add(movedTray, dirX, dirY, depth);
                    } else {
                        throw new IllegalArgumentException("Bad queuer type argument!");
                    }

                    hash.add(movedTray);
                }
            } catch (IllegalStateException e) {
            }
        }
    }

    //a helper function to maximize hashtable size based on the tray being looked at
    private static int findMaxSize(Tray t) {
        int count = 0;
        Tray[] list = new Tray[99999];

        try {
            for (int i = 0; i < list.length; i++) {
                list[i] = new Tray(t);
                count++;
            }
        } catch (OutOfMemoryError e) {
            return count;
        }
        return count;
    }

    /*
     * Makes an ArrayList of moves by just following the goal tray's parent and
     * store the previous moves of each in the list.
     */
    private static ArrayList<String[]> produceSolution(Tray currTray) {
        ArrayList<String[]> history = new ArrayList();

        while (currTray != null) {
            if (currTray.getPrevMove() != null) {
                history.add(currTray.getPrevMove());
                currTray = currTray.getPrev();
            } else {
                break;
            }
        }
        Collections.reverse(history);

        return history;
    }

    //prints the array list and if the debugger's settings says to print the
    // extra info, it does so.  
    private static void print(ArrayList<String[]> list, Debugger debug) {
        for (int i = 1; i < list.size(); i++) {
            String[] currMove = list.get(i);

            if (debug.getMoves()) {
                System.out.println(currMove[0] + " " + currMove[1] + " " + currMove[2] + " " + currMove[3] + "   " + currMove[4]);
            } else {
                System.out.println(currMove[0] + " " + currMove[1] + " " + currMove[2] + " " + currMove[3]);
            }
        }
    }

    public static void main(String[] args) {
        String input;
        String initialConfig;
        String finalConfig;

        if (args.length == 3) {
            input = args[0];
            initialConfig = args[1]; //"F:/Documents/Source Codes/Project 3/src/"
            finalConfig = args[2]; //"F:/Documents/Source Codes/Project 3/src/"

            solveInitializer(input, initialConfig, finalConfig);
        } else if (args.length == 2) {
            initialConfig = args[0]; //"F:/Documents/Source Codes/Project 3/src/"
            finalConfig = args[1]; //"F:/Documents/Source Codes/Project 3/src/"

            solveInitializer(null, initialConfig, finalConfig);
        }
    }
}