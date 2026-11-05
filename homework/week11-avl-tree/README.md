# Week 11 Homework - AVL Tree

Solution of the Week 11 AVL-tree homework (Data Structures): rotations and rebalancing after insert and delete, plus a Java implementation.

| File | What it is |
| --- | --- |
| `avl-tree-week11-solution.pdf` | Written solution: every insert/delete step with tree diagrams and balance factors, converting a BST into an AVL tree and why AVL is faster, DFS/BFS traversals, and the coding report (screenshots + full source) |
| `AVLTree.java` | AVL tree in Java: insert, delete, search and traversals; prints every rebalancing step |
| `screenshots/` | Screen captures of the program output, in 5 parts |
| `output.txt` | The same program output as plain text |

## Run the program

```bash
javac AVLTree.java
java AVLTree        # exercise 1 (insert 90, 12) and exercise 2 (delete 1, 32, 40, 50, 60)
java AVLTree -i     # interactive menu: insert / delete / search your own keys
```

Works with JDK 8 or newer. The output is plain ASCII, so the tree drawings display correctly in any console.

## Conventions

- BF(node) = height(left subtree) - height(right subtree); a leaf has height 1.
- A node with two children is deleted by copying its in-order successor (smallest key in its right subtree) into it.
- The deletions in exercise 2 are applied one after another, each to the tree left by the previous one.

## Answers in short

Exercise 1 - insert 90, then 12, into figure 1:

- insert 90: node 60 becomes unbalanced (BF -2), case RR -> left rotation at 60
- insert 12: node 20 becomes unbalanced (BF +2), case LR -> left rotation at 10, then right rotation at 20
- final tree, level by level: 30 / 15, 70 / 10, 20, 60, 90 / 5, 12, 25

Exercise 2 - delete 1, 32, 40, 50, 60 from figure 2:

- delete 1: node 30 becomes unbalanced (BF -2), case RR -> left rotation at 30
- delete 32: leaf, no rotation
- delete 40: successor 50 copied up, no rotation
- delete 50: successor 60 copied up, no rotation
- delete 60: successor 70 copied up, node 70 becomes unbalanced (BF +2), case LL -> right rotation at 70
- final tree, level by level: 30 / 7, 70 / 35
