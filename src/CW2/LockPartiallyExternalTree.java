package CW2;

import java.util.*;

public class LockPartiallyExternalTree {

    static public List<Node> traversal(Node root, int key) {
        Node cur = root;
        List<Node> result = new ArrayList<>();
        result.add(null);
        result.add(null);
        result.add(cur);
        while (cur != null && cur.key != key) {
            Node prev = result.get(1);
            result = new ArrayList<>();
            result.add(prev);
            result.add(cur);
            if (cur.key < key) {
                result.add(cur.right);
                cur = cur.right;
            } else {
                result.add(cur.left);
                cur = cur.left;
            }
        }
        return result;
    }

    static public boolean contains(Node root, int key) {
        List<Node> searchResult = traversal(root, key);
        Node cur = searchResult.get(2);
        return cur != null && cur.state == NodeState.DATA && cur.key == key;
    }

    static public boolean insert(Node root, int key) {
        while (true) {
            List<Node> searchResult = traversal(root, key);
            Node cur = searchResult.get(2);
            Node prev = searchResult.get(1);
            if (prev == null) throw new RuntimeException("key " + key + " " + searchResult + " " + root.left + " " + root.right);
            if (cur == null) {
                if (!prev.deleted) {
                    prev.stateLock.lock();
                    if (!prev.deleted) {
                        if (key > prev.key) {
                            if (prev.right == null) {
                                prev.right = new Node(key, null, null);
                                prev.stateLock.unlock();
                                return true;
                            }
                        } else {
                            if (prev.left == null) {
                                prev.left = new Node(key, null, null);
                                prev.stateLock.unlock();
                                return true;
                            }
                        }
                    }
                    prev.stateLock.unlock();
                }
            } else if (!cur.deleted) {
                cur.stateLock.lock();
                if (!cur.deleted) {
                    if (cur.state == NodeState.ROUTING) {
                        cur.stateLock.unlock();
                        cur.state = NodeState.DATA;
                        return true;
                    } else {
                        cur.stateLock.unlock();
                        return false;
                    }
                }
                cur.stateLock.unlock();
            }
        }
    }

    static public boolean delete(Node root, int key) {
        while (true) {
            if (root == null) return false;
            List<Node> searchResult = traversal(root, key);
            Node cur = searchResult.get(2);
            Node prev = searchResult.get(1);
            Node gPrev = searchResult.get(0);
            if (cur == null) return false;
            else if (cur.left == null && cur.right == null) {
                if (prev.state == NodeState.DATA) {
                    if (!prev.deleted) {
                        prev.stateLock.lock();
                        if (!prev.deleted && (prev.left == cur || prev.right == cur) && !cur.deleted) {
                            cur.stateLock.lock();
                            if (!cur.deleted && cur.left == null && cur.right == null) {
                                if (prev.right == cur) {
                                    prev.right = null;
                                } else {
                                    prev.left = null;
                                }
                                cur.deleted = true;
                                cur.stateLock.unlock();
                                prev.stateLock.unlock();
                                return true;
                            }
                            cur.stateLock.unlock();
                        }
                        prev.stateLock.unlock();
                    }
                } else {
                    if (!gPrev.deleted) {
                        gPrev.stateLock.lock();
                        if (!gPrev.deleted && (gPrev.left == prev || gPrev.right == prev) && !prev.deleted) {
                            prev.stateLock.lock();
                            if (!prev.deleted && prev.state == NodeState.ROUTING &&
                                    (prev.left == cur || prev.right == cur) && !cur.deleted) {
                                cur.stateLock.lock();
                                if (!cur.deleted && cur.left == null && cur.right == null) {
                                    if (gPrev.left == prev && prev.left == cur) {
                                        gPrev.left = prev.right;
                                    } else if (gPrev.left == prev && prev.right == cur) {
                                        gPrev.left = prev.left;
                                    } else if (gPrev.right == prev && prev.left == cur) {
                                        gPrev.right = prev.right;
                                    } else {
                                        gPrev.right = prev.left;
                                    }
                                    cur.deleted = true;
                                    prev.deleted = true;
                                    cur.stateLock.unlock();
                                    prev.stateLock.unlock();
                                    gPrev.stateLock.unlock();
                                    return true;
                                }
                                cur.stateLock.unlock();
                            }
                            prev.stateLock.unlock();
                        }
                        gPrev.stateLock.unlock();
                    }
                }
            } else if (cur.left != null && cur.right != null) {
                if (!cur.deleted) {
                    cur.stateLock.lock();
                    if (!cur.deleted && cur.left != null && cur.right != null && cur.state == NodeState.DATA) {
                        cur.state = NodeState.ROUTING;
                        cur.stateLock.unlock();
                        return true;
                    } else if (!cur.deleted && cur.left != null && cur.right != null && cur.state == NodeState.ROUTING) {
                        cur.stateLock.unlock();
                        return false;
                    }
                    cur.stateLock.unlock();
                }
            } else {
                if (!prev.deleted && !cur.deleted) {
                    prev.stateLock.lock();
                    if (!prev.deleted && (prev.left == cur || prev.right == cur) && !cur.deleted) {
                        cur.stateLock.lock();
                        if (cur.left != null && cur.right == null) {
                            Node curSon = cur.left;
                            cur.deleted = true;
                            if (prev.left == cur) prev.left = curSon;
                            else prev.right = curSon;
                            cur.stateLock.unlock();
                            prev.stateLock.unlock();
                            return true;
                        } else if (cur.left == null && cur.right != null) {
                            Node curSon = cur.right;
                            cur.deleted = true;
                            if (prev.left == cur) prev.left = curSon;
                            else prev.right = curSon;
                            cur.stateLock.unlock();
                            prev.stateLock.unlock();
                            return true;
                        }
                        cur.stateLock.unlock();
                    }
                    prev.stateLock.unlock();
                }
            }
        }
    }


    public static void main(String[] args) {
        warmup();
        double prob = 0.5;
        for (int i = 1; i <= 4; i++) {
            List<TreeWorker> workers = new ArrayList<>();
            Node root = new Node(Integer.MAX_VALUE, null, null);
            prepopulate(root, 0, 100000);
            for (int j = 0; j < i; j++) {
                workers.add(new TreeWorker(root, prob, 5));
            }
            for (Thread worker: workers) {
                worker.start();
            }
            for (Thread worker: workers) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            double throughput = 0d;
            for (TreeWorker worker: workers) {
                throughput += worker.counter / (double) worker.time;
            }
            System.out.println(" workers: " + i  + " ops/sec: " + throughput);
        }
    }

    private static void warmup() {
        Node root = new Node(Integer.MAX_VALUE, null, null);
        prepopulate(root, 0, 100);
        Thread treeWorker = new TreeWorker(root, 0.1, 3);
        treeWorker.start();
        try {
            treeWorker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void prepopulate(Node root, int begin, int end) {
        Random random = new Random();
        for (int i = begin; i < end; i=i+2) {
            insert(root, random.nextInt() % 100000);
        }
    }

    static class TreeWorker extends Thread {
        private final Node root;
        long time;
        private long counter;
        final double x;
        private final int secs;

        TreeWorker(Node root, double x, int secs) {
            this.root = root;
            this.x = x;
            this.secs = secs;
        }

        @Override
        public void run() {
            time = System.currentTimeMillis();
            Random random = new Random();

            while (System.currentTimeMillis() - time < 1000 * secs) {
                int key = Math.abs(random.nextInt()) % 100000;
                double p = Math.random();
                if (p < x) insert(root, key);
                else if (x <= p && p < 2*x) delete(root, key);
                else contains(root, key);
                counter++;
            }
            time = System.currentTimeMillis() - time;
        }
    }

}
