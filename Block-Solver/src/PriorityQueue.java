/*
 * A priority queue supports a priority DFS and iterative DFS.
 */

import java.util.LinkedList;

public class PriorityQueue {
    
    private LinkedList<Object> queue;
    
    public PriorityQueue() {
        queue = new LinkedList();
    }
    
    public void add(Tray t) {
        queue.add(t);
    }
    
    /*
     * Adds for PDFS. This adds blocks that are moving in the same direction
     * to the end of the queue and the rest to the back.
     */
    public void add(Tray t, int dirX, int dirY) {
        if (dirX == 1) {
            if (t.getDirection() == 1) {
                queue.addLast(t);
            } else if (t.getDirection() != 3) {
                queue.addFirst(t);
            }
        } else if (dirX == -1) {
            if (t.getDirection() == 3) {
                queue.addLast(t);
            } else if (t.getDirection() != 1) {
                queue.addFirst(t);
            }
        } else if (dirY == 1) {
            if (t.getDirection() == 2) {
                queue.addLast(t);
            } else if (t.getDirection() != 0) {
                queue.addFirst(t);
            }
        } else if (dirY == -1) {
            if (t.getDirection() == 0) {
                queue.addLast(t);
            } else if (t.getDirection() != 2) {
                queue.addFirst(t);
            }
        } else {
            throw new IllegalStateException("Illegal move made!");
        }
    }
    
    /*
     * This add supports IDFS. All blocks over the depth threshold gets
     * added to the front.
     */
    public void add(Tray t, int dirX, int dirY, int depthThreshold) {
        if (dirX == 1) {
            if (t.getDirection() != 3) {
                if (t.getDepth() > depthThreshold) {
                    queue.addFirst(t);
                } else {
                    queue.addLast(t);
                }                
            }
        } else if (dirX == -1) {
            if (t.getDirection() != 1) {
                if (t.getDepth() > depthThreshold) {
                    queue.addFirst(t);
                } else {
                    queue.addLast(t);
                }                
            }
        } else if (dirY == 1) {
            if (t.getDirection() != 0) {
                if (t.getDepth() > depthThreshold) {
                    queue.addFirst(t);
                } else {
                    queue.addLast(t);
                }
            }
        } else if (dirY == -1) {
            if (t.getDirection() != 2) {
                if (t.getDepth() > depthThreshold) {
                    queue.addFirst(t);
                } else {
                    queue.addLast(t);
                }
            }
        } else {
            throw new IllegalStateException("Illegal move made!");
        }
    }
        
    public Object dequeue() {
        return queue.removeLast();
    }
    
    public void enqueue(Tray t) {
        queue.addFirst(t);
    }
    
    public int size() {
        return queue.size();
    }
    
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
