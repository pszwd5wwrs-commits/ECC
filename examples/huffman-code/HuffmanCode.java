import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Huffman code exercise.
 *
 * Character frequencies:  A = 50%  B = 20%  C = 30%  D = 10%  E = 40%  F = 5%
 * Task: build a Huffman code for these characters.
 *
 * The program uses the same rules as the lecture example (a..h = 1..8):
 *   1. Every character starts as a one-node tree in a list sorted by frequency.
 *   2. Take the two trees with the smallest frequencies and join them under a
 *      new node whose frequency is their sum, then put it back in the list.
 *      The smaller tree becomes the LEFT child, the other the RIGHT child.
 *      If two frequencies are equal, the tree that was in the list first wins.
 *   3. Repeat until one tree is left: that is the Huffman tree.
 *   4. Read each code from the root down: left branch = 0, right branch = 1.
 *
 * Compile and run:
 *   javac HuffmanCode.java
 *   java HuffmanCode
 */
public class HuffmanCode {

    /** One node of the Huffman tree. */
    static class Node {
        final String name;  // letters under this node, e.g. "FD" (a leaf has one letter)
        final int freq;     // frequency = sum of the leaves below
        final int order;    // creation order, used when two frequencies are equal
        final Node left;    // branch 0
        final Node right;   // branch 1

        /** Leaf for one character. */
        Node(char letter, int freq, int order) {
            this(String.valueOf(letter), freq, order, null, null);
        }

        /** New parent that joins two trees. */
        Node(Node left, Node right, int order) {
            this(left.name + right.name, left.freq + right.freq, order, left, right);
        }

        private Node(String name, int freq, int order, Node left, Node right) {
            this.name = name;
            this.freq = freq;
            this.order = order;
            this.left = left;
            this.right = right;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public String toString() {
            return name + "(" + freq + ")";
        }
    }

    /** Smallest frequency first; on a tie, the node that was created first. */
    static final Comparator<Node> SMALLEST_FIRST =
            Comparator.comparingInt((Node n) -> n.freq).thenComparingInt(n -> n.order);

    public static void main(String[] args) {
        char[] letters = {'A', 'B', 'C', 'D', 'E', 'F'};
        int[] freqs    = { 50,  20,  30,  10,  40,   5};

        Node root = buildTree(letters, freqs);

        System.out.println();
        System.out.println("Huffman tree (left = 0, right = 1):");
        System.out.println(root);
        printTree(root, "");

        Map<Character, String> codes = new HashMap<>();
        buildCodes(root, "", codes);
        printCodeTable(letters, freqs, codes);

        String word = "FACADE";
        String bits = encode(word, codes, "");
        System.out.println();
        System.out.println("Encode: " + word + " -> " + encode(word, codes, " ") + " -> " + bits);
        System.out.println("Decode: " + bits + " -> " + decode(bits, root));
    }

    /** Steps 1-3: build the tree and print the list after every step. */
    static Node buildTree(char[] letters, int[] freqs) {
        PriorityQueue<Node> queue = new PriorityQueue<>(SMALLEST_FIRST);
        int order = 0;
        for (int i = 0; i < letters.length; i++) {
            queue.add(new Node(letters[i], freqs[i], order++));
        }

        System.out.println("Steps of forming the Huffman tree:");
        System.out.println("Start : " + listOf(queue));
        int step = 1;
        while (queue.size() > 1) {
            Node left = queue.poll();   // smallest frequency -> branch 0
            Node right = queue.poll();  // next smallest      -> branch 1
            Node parent = new Node(left, right, order++);
            queue.add(parent);
            System.out.println("Step " + step++ + ": " + left + " + " + right + " = " + parent);
            System.out.println("        " + listOf(queue));
        }
        return queue.poll();
    }

    /** The list as the lecture draws it: smallest frequency on the left. */
    static String listOf(PriorityQueue<Node> queue) {
        List<Node> nodes = new ArrayList<>(queue);
        nodes.sort(SMALLEST_FIRST);
        StringBuilder text = new StringBuilder();
        for (Node node : nodes) {
            text.append(node).append("  ");
        }
        return text.toString().trim();
    }

    /** Prints the tree like a folder listing; each branch shows its bit. */
    static void printTree(Node node, String indent) {
        if (node.isLeaf()) {
            return;
        }
        System.out.println(indent + "|-- 0: " + node.left);
        printTree(node.left, indent + "|   ");
        System.out.println(indent + "`-- 1: " + node.right);
        printTree(node.right, indent + "    ");
    }

    /** Step 4: walk down from the root, adding 0 for left and 1 for right. */
    static void buildCodes(Node node, String code, Map<Character, String> codes) {
        if (node.isLeaf()) {
            codes.put(node.name.charAt(0), code);
            return;
        }
        buildCodes(node.left, code + "0", codes);
        buildCodes(node.right, code + "1", codes);
    }

    static void printCodeTable(char[] letters, int[] freqs, Map<Character, String> codes) {
        System.out.println();
        System.out.println("Huffman code table:");
        System.out.println("Char  Freq  Code   Bits");
        int totalFreq = 0;
        int totalBits = 0;
        for (int i = 0; i < letters.length; i++) {
            String code = codes.get(letters[i]);
            System.out.printf("%-4c  %4d  %-5s  %4d%n", letters[i], freqs[i], code, code.length());
            totalFreq += freqs[i];
            totalBits += freqs[i] * code.length();
        }
        System.out.printf("Average code length = %d / %d = %.2f bits per character%n",
                totalBits, totalFreq, (double) totalBits / totalFreq);
    }

    /** Replaces every character by its code. */
    static String encode(String text, Map<Character, String> codes, String separator) {
        StringBuilder bits = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (bits.length() > 0) {
                bits.append(separator);
            }
            bits.append(codes.get(c));
        }
        return bits.toString();
    }

    /** Follows the bits from the root and writes a letter at every leaf reached. */
    static String decode(String bits, Node root) {
        StringBuilder text = new StringBuilder();
        Node node = root;
        for (char bit : bits.toCharArray()) {
            node = (bit == '0') ? node.left : node.right;
            if (node.isLeaf()) {
                text.append(node.name);
                node = root;
            }
        }
        return text.toString();
    }
}
