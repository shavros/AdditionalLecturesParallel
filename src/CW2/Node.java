package CW2;

import java.util.concurrent.locks.ReentrantLock;

public class Node {
    Node right;
    Node left;
    int key;
    NodeState state;
    boolean deleted;
    ReentrantLock stateLock;


    Node(int key, Node left, Node right) {
        this.key = key;
        this. left = left;
        this.right = right;
        state = NodeState.DATA;
        deleted = false;
        stateLock = new ReentrantLock();
    }

    @Override
    public String toString() {
        return "key: " + key;
    }
}
