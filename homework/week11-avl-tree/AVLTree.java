import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Week 11 Homework - AVL Tree (a self-balancing binary search tree).
 *
 * Operations: insert, delete, search and the four traversals
 * (pre-order, in-order, post-order and level-order / BFS).
 *
 * Every insert and delete prints what the algorithm does, step by step:
 *   Step 1  the BST search path from the root down to the insert/delete position
 *   Step 2  walking back up to the root: the new height and balance factor of
 *           every node, the rotation case (LL, RR, LR, RL) of an unbalanced
 *           node and the subtree after every rotation
 *   Result  the whole tree, drawn top-down like the figures in class
 *
 * Conventions (the same as in the written solution):
 *   height(empty tree) = 0, height(leaf) = 1
 *   BF(node) = height(left subtree) - height(right subtree)
 *   AVL property: every node has BF = -1, 0 or +1
 *   A node with two children is deleted by copying its in-order successor
 *   (the smallest key in its right subtree) into it and then deleting the
 *   successor from the right subtree.
 *
 * Compile and run (JDK 8 or newer):
 *   javac AVLTree.java
 *   java AVLTree       runs Exercise 1 (insert) and Exercise 2 (delete)
 *   java AVLTree -i    interactive menu: insert / delete / search / show
 */
public class AVLTree {

    /** The two trees of the homework, listed level by level (BFS order). */
    static final int[] FIGURE_1 = {30, 20, 60, 10, 25, 70, 5, 15};
    static final int[] FIGURE_2 = {30, 7, 40, 1, 35, 60, 32, 50, 70};

    /** One node of the tree. */
    static final class Node {
        int key;
        int height = 1;
        Node left;
        Node right;

        Node(int key) {
            this.key = key;
        }
    }

    private static final String RULE =
            "======================================================================";

    private Node root;
    private boolean trace = true;                                // print the steps?
    private final List<String> rotations = new ArrayList<>();    // rotations of the current operation
    private String note = "";                                    // remark about the current operation
    private final List<String> history = new ArrayList<>();      // one summary line per operation

    // =====================================================================
    //  Small helpers
    // =====================================================================

    private static int height(Node n) {
        return n == null ? 0 : n.height;
    }

    private static int balanceFactor(Node n) {
        return n == null ? 0 : height(n.left) - height(n.right);
    }

    private static void updateHeight(Node n) {
        n.height = 1 + Math.max(height(n.left), height(n.right));
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private void say(String line) {
        if (trace) {
            System.out.println(line);
        }
    }

    // =====================================================================
    //  Rotations
    //
    //   Right rotation at y                 Left rotation at x
    //
    //        y              x                  x                  y
    //       / \            / \                / \                / \
    //      x   C   ==>    A   y              A   y      ==>     x   C
    //     / \                / \                / \            / \
    //    A   B              B   C              B   C          A   B
    // =====================================================================

    private Node rotateRight(Node y, String prefix) {
        Node x = y.left;
        Node b = x.right;
        x.right = y;
        y.left = b;
        updateHeight(y);
        updateHeight(x);
        say(prefix + "RIGHT rotation at " + y.key + ": " + x.key + " moves up, "
                + y.key + " becomes its right child"
                + (b == null ? "" : ", " + b.key + " moves to the left of " + y.key));
        return x;
    }

    private Node rotateLeft(Node x, String prefix) {
        Node y = x.right;
        Node b = y.left;
        y.left = x;
        x.right = b;
        updateHeight(x);
        updateHeight(y);
        say(prefix + "LEFT rotation at " + x.key + ": " + y.key + " moves up, "
                + x.key + " becomes its left child"
                + (b == null ? "" : ", " + b.key + " moves to the right of " + x.key));
        return y;
    }

    // =====================================================================
    //  Rebalancing - used by both insert and delete
    // =====================================================================

    /**
     * Runs for every node on the way back up to the root: updates the height,
     * prints the balance factor and, when |BF| = 2, repairs the node with one
     * or two rotations. Returns the root of this subtree after the repair.
     */
    private Node rebalance(Node z) {
        updateHeight(z);
        int bf = balanceFactor(z);
        boolean balanced = Math.abs(bf) <= 1;
        say(String.format("    node %-3d: h(L)=%d  h(R)=%d  BF=%-3s %s", z.key,
                height(z.left), height(z.right), signed(bf), balanced ? "ok" : "<-- UNBALANCED"));
        if (balanced) {
            return z;
        }

        String pad = "        ";
        Node newRoot;
        if (bf > 1) {                                   // the left side is too tall
            Node y = z.left;
            int childBf = balanceFactor(y);
            if (childBf >= 0) {
                say(pad + "case LL (left child " + y.key + " has BF " + signed(childBf) + ")");
                say(pad + "fix : single RIGHT rotation at " + z.key);
                newRoot = rotateRight(z, pad);
                rotations.add("LL at " + z.key + " -> right rotation at " + z.key);
            } else {
                say(pad + "case LR (left child " + y.key + " has BF " + signed(childBf) + ")");
                say(pad + "fix : LEFT rotation at " + y.key + ", then RIGHT rotation at " + z.key);
                z.left = rotateLeft(y, pad + "(1) ");
                printSubtree(pad + "    subtree after (1):", z);
                newRoot = rotateRight(z, pad + "(2) ");
                rotations.add("LR at " + z.key + " -> left rotation at " + y.key
                        + ", right rotation at " + z.key);
            }
        } else {                                        // the right side is too tall
            Node y = z.right;
            int childBf = balanceFactor(y);
            if (childBf <= 0) {
                say(pad + "case RR (right child " + y.key + " has BF " + signed(childBf) + ")");
                say(pad + "fix : single LEFT rotation at " + z.key);
                newRoot = rotateLeft(z, pad);
                rotations.add("RR at " + z.key + " -> left rotation at " + z.key);
            } else {
                say(pad + "case RL (right child " + y.key + " has BF " + signed(childBf) + ")");
                say(pad + "fix : RIGHT rotation at " + y.key + ", then LEFT rotation at " + z.key);
                z.right = rotateRight(y, pad + "(1) ");
                printSubtree(pad + "    subtree after (1):", z);
                newRoot = rotateLeft(z, pad + "(2) ");
                rotations.add("RL at " + z.key + " -> right rotation at " + y.key
                        + ", left rotation at " + z.key);
            }
        }
        printSubtree(pad + "balanced subtree:", newRoot);
        return newRoot;
    }

    // =====================================================================
    //  Insert
    // =====================================================================

    /** Inserts a key (duplicates are ignored) and prints every step. */
    public void insert(int key) {
        System.out.println();
        System.out.println(">>> INSERT " + key);
        if (contains(key)) {
            System.out.println("    " + key + " is already in the tree (no duplicates) - nothing to do");
            return;
        }
        rotations.clear();
        note = "";
        say("  Step 1 - BST insert: walk down from the root to an empty position");
        root = insert(root, key, null, "");
        finish("insert " + key, "inserting " + key);
    }

    private Node insert(Node node, int key, Node parent, String side) {
        if (node == null) {
            if (parent == null) {
                say("    the tree is empty, so " + key + " becomes the root");
            } else {
                say("    empty position found: " + key + " becomes the " + side + " child of " + parent.key);
                say("  Step 2 - walk back up to the root: update height, check BF, rotate if |BF| = 2");
            }
            return new Node(key);
        }
        if (key < node.key) {
            say(String.format("    at %-3d: %d < %d, go LEFT", node.key, key, node.key));
            node.left = insert(node.left, key, node, "LEFT");
        } else if (key > node.key) {
            say(String.format("    at %-3d: %d > %d, go RIGHT", node.key, key, node.key));
            node.right = insert(node.right, key, node, "RIGHT");
        } else {
            return node;                                // duplicate key: nothing changes
        }
        return rebalance(node);
    }

    // =====================================================================
    //  Delete
    // =====================================================================

    /** Deletes a key and prints every step. */
    public void delete(int key) {
        System.out.println();
        System.out.println(">>> DELETE " + key);
        if (!contains(key)) {
            System.out.println("    " + key + " is not in the tree - nothing to delete");
            return;
        }
        rotations.clear();
        note = "";
        say("  Step 1 - BST delete: search for " + key + " starting at the root");
        root = delete(root, key);
        finish("delete " + key, "deleting " + key);
    }

    private Node delete(Node node, int key) {
        if (key < node.key) {
            say(String.format("    at %-3d: %d < %d, go LEFT", node.key, key, node.key));
            node.left = delete(node.left, key);
        } else if (key > node.key) {
            say(String.format("    at %-3d: %d > %d, go RIGHT", node.key, key, node.key));
            node.right = delete(node.right, key);
        } else if (node.left == null || node.right == null) {
            Node child = node.left != null ? node.left : node.right;
            if (child == null) {
                say("    found " + key + ": it is a leaf, so simply remove it");
            } else {
                say("    found " + key + ": it has one child, so " + child.key + " takes its place");
            }
            say("  Step 2 - walk back up to the root: update height, check BF, rotate if |BF| = 2");
            return child;                               // this subtree did not change: still AVL
        } else {
            Node successor = node.right;
            while (successor.left != null) {
                successor = successor.left;
            }
            say("    found " + key + ": it has two children, so use its in-order successor");
            say("    successor = smallest key in the right subtree of " + key + " = " + successor.key);
            say("    copy " + successor.key + " into this node, then delete the old "
                    + successor.key + " from the right subtree");
            note = "2 children, successor " + successor.key + " copied up";
            node.key = successor.key;
            node.right = delete(node.right, successor.key);
        }
        return rebalance(node);
    }

    /** Prints the result of an insert/delete and remembers it for the summary. */
    private void finish(String operation, String verb) {
        String what = rotations.isEmpty() ? "no rotation needed" : String.join("; ", rotations);
        if (!note.isEmpty()) {
            what += "   [" + note + "]";
        }
        history.add(String.format("%-10s : %s", operation, what));
        System.out.println("  Result - tree after " + verb + "   (AVL check: "
                + (isValidAVL() ? "OK" : "FAILED") + ", "
                + (rotations.isEmpty() ? "no rotation" : rotations.size() + " rebalance") + ")");
        printTree(root, "      ");
    }

    // =====================================================================
    //  Search, traversals, checks
    // =====================================================================

    public boolean contains(int key) {
        Node n = root;
        while (n != null) {
            if (key == n.key) {
                return true;
            }
            n = key < n.key ? n.left : n.right;
        }
        return false;
    }

    /** Prints the search path for a key. */
    public void search(int key) {
        StringBuilder path = new StringBuilder();
        Node n = root;
        int comparisons = 0;
        while (n != null) {
            comparisons++;
            path.append(n.key);
            if (key == n.key) {
                System.out.println("    found " + key + " after " + comparisons + " comparison(s), path: " + path);
                return;
            }
            path.append(key < n.key ? " -L-> " : " -R-> ");
            n = key < n.key ? n.left : n.right;
        }
        System.out.println("    " + key + " is NOT in the tree, path: " + path + "null");
    }

    public List<Integer> preOrder() {
        List<Integer> out = new ArrayList<>();
        preOrder(root, out);
        return out;
    }

    private static void preOrder(Node n, List<Integer> out) {      // Node, Left, Right
        if (n != null) {
            out.add(n.key);
            preOrder(n.left, out);
            preOrder(n.right, out);
        }
    }

    public List<Integer> inOrder() {
        List<Integer> out = new ArrayList<>();
        inOrder(root, out);
        return out;
    }

    private static void inOrder(Node n, List<Integer> out) {       // Left, Node, Right
        if (n != null) {
            inOrder(n.left, out);
            out.add(n.key);
            inOrder(n.right, out);
        }
    }

    public List<Integer> postOrder() {
        List<Integer> out = new ArrayList<>();
        postOrder(root, out);
        return out;
    }

    private static void postOrder(Node n, List<Integer> out) {     // Left, Right, Node
        if (n != null) {
            postOrder(n.left, out);
            postOrder(n.right, out);
            out.add(n.key);
        }
    }

    /** Breadth-first search: visit the tree level by level using a queue. */
    public List<Integer> levelOrder() {
        List<Integer> out = new ArrayList<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        if (root != null) {
            queue.add(root);
        }
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            out.add(n.key);
            if (n.left != null) {
                queue.add(n.left);
            }
            if (n.right != null) {
                queue.add(n.right);
            }
        }
        return out;
    }

    /** True when the keys are in BST order, every stored height is right and every |BF| <= 1. */
    public boolean isValidAVL() {
        return check(root, Long.MIN_VALUE, Long.MAX_VALUE) >= 0;
    }

    private static int check(Node n, long low, long high) {        // returns the height, or -1 if invalid
        if (n == null) {
            return 0;
        }
        if (n.key <= low || n.key >= high) {
            return -1;
        }
        int hl = check(n.left, low, n.key);
        int hr = check(n.right, n.key, high);
        if (hl < 0 || hr < 0 || Math.abs(hl - hr) > 1 || n.height != 1 + Math.max(hl, hr)) {
            return -1;
        }
        return n.height;
    }

    private static String join(List<Integer> keys) {
        StringBuilder sb = new StringBuilder();
        for (int k : keys) {
            sb.append(sb.length() == 0 ? "" : " ").append(k);
        }
        return sb.toString();
    }

    private String balanceFactors() {
        StringBuilder sb = new StringBuilder();
        appendBalanceFactors(root, sb);
        return sb.toString().trim();
    }

    private static void appendBalanceFactors(Node n, StringBuilder sb) {
        if (n != null) {
            appendBalanceFactors(n.left, sb);
            sb.append(n.key).append('(').append(signed(balanceFactor(n))).append(") ");
            appendBalanceFactors(n.right, sb);
        }
    }

    /** Prints the traversals, balance factors and size of the tree. */
    public void printDetails() {
        System.out.println("  Pre-order   (N L R) : " + join(preOrder()));
        System.out.println("  In-order    (L N R) : " + join(inOrder()));
        System.out.println("  Post-order  (L R N) : " + join(postOrder()));
        System.out.println("  Level-order (BFS)   : " + join(levelOrder()));
        System.out.println("  BF of each node     : " + balanceFactors());
        System.out.println("  nodes = " + inOrder().size() + ", height = " + height(root)
                + ", AVL check: " + (isValidAVL() ? "OK" : "FAILED"));
    }

    public void show() {
        printTree(root, "      ");
        printDetails();
    }

    private void printHistory(String title) {
        System.out.println();
        System.out.println(title + ":");
        for (String line : history) {
            System.out.println("  " + line);
        }
    }

    /** Builds a tree silently (no trace), e.g. from the level order of a figure. */
    static AVLTree build(int... keys) {
        AVLTree tree = new AVLTree();
        tree.trace = false;
        for (int key : keys) {
            tree.root = tree.insert(tree.root, key, null, "");
        }
        tree.rotations.clear();
        tree.trace = true;
        return tree;
    }

    // =====================================================================
    //  Drawing the tree top-down in plain ASCII (works in every console)
    //
    //  Each node gets an x position: a parent is centred above its children
    //  and two neighbouring subtrees are pushed apart until they no longer
    //  overlap on any level (connectors included). Then every node is printed
    //  on row 2*level and its "/---+---\" connector on the row below it.
    // =====================================================================

    /** x position of every node (relative to the subtree root) and the outline of each level. */
    private static final class Layout {
        final Map<Node, Integer> x = new HashMap<>();
        final List<int[]> outline = new ArrayList<>();          // outline.get(level) = {min x, max x}
    }

    private static Layout layout(Node n, int gap) {
        if (n == null) {
            return null;
        }
        Layout left = layout(n.left, gap);
        Layout right = layout(n.right, gap);
        int leftOffset = -gap / 2;
        int rightOffset = gap / 2;
        if (left != null && right != null) {
            int distance = gap;
            int levels = Math.min(left.outline.size(), right.outline.size());
            for (int d = 0; d < levels; d++) {
                distance = Math.max(distance, left.outline.get(d)[1] - right.outline.get(d)[0] + gap);
            }
            distance += distance % 2;                           // keep the parent exactly in the middle
            leftOffset = -distance / 2;
            rightOffset = distance / 2;
        }
        Layout result = new Layout();
        result.x.put(n, 0);
        // the outline of a level also covers the "/---+---\" connector below each node,
        // so a neighbouring node never sits above (or touches) someone else's connector
        result.outline.add(new int[] {left != null ? leftOffset : 0, right != null ? rightOffset : 0});
        attach(result, left, leftOffset);
        attach(result, right, rightOffset);
        return result;
    }

    private static void attach(Layout parent, Layout child, int offset) {
        if (child == null) {
            return;
        }
        for (Map.Entry<Node, Integer> e : child.x.entrySet()) {
            parent.x.put(e.getKey(), e.getValue() + offset);
        }
        for (int d = 0; d < child.outline.size(); d++) {
            int low = child.outline.get(d)[0] + offset;
            int high = child.outline.get(d)[1] + offset;
            if (d + 1 < parent.outline.size()) {
                int[] o = parent.outline.get(d + 1);
                o[0] = Math.min(o[0], low);
                o[1] = Math.max(o[1], high);
            } else {
                parent.outline.add(new int[] {low, high});
            }
        }
    }

    private static int levels(Node n) {
        return n == null ? 0 : 1 + Math.max(levels(n.left), levels(n.right));
    }

    private static int widestKey(Node n) {
        return n == null ? 0
                : Math.max(String.valueOf(n.key).length(), Math.max(widestKey(n.left), widestKey(n.right)));
    }

    /** Returns the drawing of the subtree rooted at n, one string per line. */
    static List<String> draw(Node n) {
        List<String> lines = new ArrayList<>();
        if (n == null) {
            lines.add("(empty tree)");
            return lines;
        }
        int keyWidth = widestKey(n);
        Layout layout = layout(n, Math.max(6, keyWidth + 4));
        int minX = 0;
        int maxX = 0;
        for (int[] o : layout.outline) {
            minX = Math.min(minX, o[0]);
            maxX = Math.max(maxX, o[1]);
        }
        int shift = -minX + (keyWidth - 1) / 2;
        char[][] grid = new char[2 * levels(n) - 1][maxX + shift + keyWidth + 1];
        for (char[] row : grid) {
            Arrays.fill(row, ' ');
        }
        paint(n, 0, layout, shift, grid);
        for (char[] row : grid) {
            lines.add(new String(row).replaceAll("\\s+$", ""));
        }
        return lines;
    }

    private static void paint(Node n, int level, Layout layout, int shift, char[][] grid) {
        int c = layout.x.get(n) + shift;
        String label = String.valueOf(n.key);
        int start = c - (label.length() - 1) / 2;
        for (int i = 0; i < label.length(); i++) {
            grid[2 * level][start + i] = label.charAt(i);
        }
        if (n.left == null && n.right == null) {
            return;
        }
        char[] row = grid[2 * level + 1];
        int from = n.left != null ? layout.x.get(n.left) + shift : c;
        int to = n.right != null ? layout.x.get(n.right) + shift : c;
        for (int i = from; i <= to; i++) {
            row[i] = '-';
        }
        if (n.left != null) {
            row[from] = '/';
            paint(n.left, level + 1, layout, shift, grid);
        }
        if (n.right != null) {
            row[to] = '\\';
            paint(n.right, level + 1, layout, shift, grid);
        }
        row[c] = '+';
    }

    private static void printTree(Node n, String indent) {
        for (String line : draw(n)) {
            System.out.println(indent + line);
        }
    }

    private void printSubtree(String title, Node n) {
        if (trace) {
            System.out.println(title);
            printTree(n, title.replaceAll("\\S.*$", "") + "    ");
        }
    }

    // =====================================================================
    //  Main program
    // =====================================================================

    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("-i") || args[0].equals("--interactive"))) {
            interactive();
            return;
        }
        System.out.println(RULE);
        System.out.println("  AVL TREE - insert and delete with rebalancing      (Week 11 homework)");
        System.out.println("  BF(node) = height(left) - height(right), height of a leaf = 1");
        System.out.println("  a node with 2 children is deleted via its in-order successor");
        System.out.println(RULE);

        heading("EXERCISE 1 : insert 90, then 12, into the AVL tree of figure 1");
        AVLTree tree1 = build(FIGURE_1);
        System.out.println("Starting tree (figure 1):");
        tree1.show();
        tree1.insert(90);
        tree1.insert(12);
        System.out.println();
        System.out.println("Final tree of exercise 1 (drawn above):");
        tree1.printDetails();
        tree1.printHistory("Summary of exercise 1");

        heading("EXERCISE 2 : delete 1, 32, 40, 50, 60 (in sequence) from figure 2");
        AVLTree tree2 = build(FIGURE_2);
        System.out.println("Starting tree (figure 2):");
        tree2.show();
        for (int key : new int[] {1, 32, 40, 50, 60}) {
            tree2.delete(key);
        }
        System.out.println();
        System.out.println("Final tree of exercise 2 (drawn above):");
        tree2.printDetails();
        tree2.printHistory("Summary of exercise 2");
        System.out.println();
        System.out.println(RULE);
        System.out.println("  Run  java AVLTree -i  to insert / delete your own keys.");
        System.out.println(RULE);
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("  " + title);
        System.out.println(RULE);
    }

    /** Simple menu so any key can be inserted, deleted or searched. */
    private static void interactive() {
        Scanner in = new Scanner(System.in);
        AVLTree tree = new AVLTree();
        System.out.println("AVL TREE - interactive mode (keys are integers, several keys may be typed at once)");
        while (true) {
            System.out.println();
            System.out.println("1) Insert  2) Delete  3) Search  4) Show tree  5) Load figure 1  6) Load figure 2");
            System.out.println("7) Clear   8) Step-by-step trace on/off (now " + (tree.trace ? "ON" : "OFF") + ")  0) Exit");
            System.out.print("Choose: ");
            if (!in.hasNextLine()) {
                return;
            }
            String choice = in.nextLine().trim();
            switch (choice) {
                case "1":
                    for (int key : readKeys(in, "Key(s) to insert: ")) {
                        tree.insert(key);
                    }
                    break;
                case "2":
                    for (int key : readKeys(in, "Key(s) to delete: ")) {
                        tree.delete(key);
                    }
                    break;
                case "3":
                    for (int key : readKeys(in, "Key(s) to search: ")) {
                        tree.search(key);
                    }
                    break;
                case "4":
                    tree.show();
                    break;
                case "5":
                case "6":
                    boolean keepTrace = tree.trace;
                    tree = build(choice.equals("5") ? FIGURE_1 : FIGURE_2);
                    tree.trace = keepTrace;
                    System.out.println("Loaded figure " + (choice.equals("5") ? 1 : 2) + ":");
                    tree.show();
                    break;
                case "7":
                    boolean trace = tree.trace;
                    tree = new AVLTree();
                    tree.trace = trace;
                    System.out.println("The tree is now empty.");
                    break;
                case "8":
                    tree.trace = !tree.trace;
                    System.out.println("Step-by-step trace is now " + (tree.trace ? "ON" : "OFF"));
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Please choose 0-8.");
            }
        }
    }

    private static List<Integer> readKeys(Scanner in, String prompt) {
        System.out.print(prompt);
        List<Integer> keys = new ArrayList<>();
        if (!in.hasNextLine()) {
            return keys;
        }
        for (String part : in.nextLine().trim().split("[\\s,]+")) {
            if (part.isEmpty()) {
                continue;
            }
            try {
                keys.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                System.out.println("    skipped '" + part + "' (not an integer)");
            }
        }
        return keys;
    }
}
