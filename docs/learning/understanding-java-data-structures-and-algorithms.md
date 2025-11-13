# Understanding Java Data Structures and Algorithms

## Table of Contents
1. [Introduction to Data Structures](#introduction-to-data-structures)
2. [Big O Notation - Understanding Performance](#big-o-notation)
3. [Arrays and ArrayLists](#arrays-and-arraylists)
4. [LinkedLists](#linkedlists)
5. [Stacks](#stacks)
6. [Queues](#queues)
7. [HashMaps and HashSets](#hashmaps-and-hashsets)
8. [Trees](#trees)
9. [Sorting Algorithms](#sorting-algorithms)
10. [Searching Algorithms](#searching-algorithms)
11. [Common Algorithm Patterns](#common-algorithm-patterns)
12. [Real Examples from Your Codebase](#real-examples-from-your-codebase)

---

## Introduction to Data Structures

### What is a Data Structure?

A **data structure** is a way of organizing and storing data so that it can be accessed and modified efficiently.

### Simple Analogy

Think of data structures like different ways to organize books:

- **Array**: Books on a shelf in order (position 1, 2, 3...)
- **LinkedList**: Books connected by bookmarks pointing to the next book
- **HashMap**: Card catalog where you look up by author name
- **Stack**: Stack of books where you can only take from the top
- **Queue**: Line at the library - first person in, first person out
- **Tree**: Family tree of book editions and translations

### Why Do We Need Different Data Structures?

Different operations are faster with different structures:

| Operation | Best Data Structure | Why |
|-----------|-------------------|-----|
| Find by ID | HashMap | O(1) instant lookup |
| Keep sorted order | TreeSet | Maintains order automatically |
| Process in order received | Queue | FIFO (First In, First Out) |
| Undo/Redo operations | Stack | LIFO (Last In, First Out) |
| Fast insertion at any position | LinkedList | No shifting needed |

---

## Big O Notation - Understanding Performance

### What is Big O?

**Big O notation** is a mathematical notation that describes how the runtime or memory usage of an algorithm grows as the input size grows.

**Think of it as:** A way to measure how efficient your code is, especially as your data gets larger.

### Why Big O Matters

```
Imagine searching for a name in a phone book:

Method 1: Check every page from start to finish
- 1,000 pages? Check 1,000 pages
- 10,000 pages? Check 10,000 pages
- This is O(n) - linear time

Method 2: Open to middle, check if target is before/after, repeat
- 1,000 pages? Check ~10 pages
- 10,000 pages? Check ~13 pages
- This is O(log n) - logarithmic time

Same problem, HUGE difference in efficiency!
```

### What Does Big O Measure?

Big O measures **two things:**

1. **Time Complexity** - How runtime increases with input size
2. **Space Complexity** - How memory usage increases with input size

**Important:** Big O describes the **worst-case scenario** (unless otherwise specified)

---

## Understanding Big O - Step by Step

### The Basic Idea

Big O notation answers: **"If I double my input size, what happens to my runtime?"**

| Notation | Name | What happens when you double input? | Example |
|----------|------|-------------------------------------|---------|
| O(1) | Constant | Nothing - same time | Array access |
| O(log n) | Logarithmic | Adds 1 operation | Binary search |
| O(n) | Linear | Doubles | Loop through array |
| O(n log n) | Linearithmic | ~Doubles + bit more | Merge sort |
| O(n²) | Quadratic | 4x slower | Nested loops |
| O(2ⁿ) | Exponential | Squares the time | Recursive fibonacci |

### Visual Growth Chart

```
Number of Operations

│ O(2ⁿ)
│                                                              ╱
│                                                         ╱╱╱╱
│ O(n²)                                              ╱╱╱╱
│                                   ╱╱╱╱╱╱╱╱╱╱╱╱╱╱╱╱
│ O(n log n)         ╱╱╱╱╱╱╱╱╱╱╱╱╱╱
│ O(n)        ╱╱╱╱╱╱╱
│ O(log n) ─╱─
│ O(1) ───────────────────────────────────────────────────
└────────────────────────────────────────────────────────> Input Size (n)
```

### Performance Comparison Table

```
Operations needed for different input sizes:

Input Size (n)     10        100       1,000       10,000      1,000,000
──────────────────────────────────────────────────────────────────────────
O(1)               1         1         1           1           1
O(log n)           3         7         10          13          20
O(n)               10        100       1,000       10,000      1,000,000
O(n log n)         30        664       9,966       132,877     19,931,569
O(n²)              100       10,000    1,000,000   100,000,000 1,000,000,000,000
O(2ⁿ)              1,024     ∞         ∞           ∞           ∞
```

**Key Insight:** O(n²) with 1 million items = 1 trillion operations! 💥

---

## How to Calculate Big O - The Rules

### Rule 1: Drop Constants

**Big O cares about growth rate, not exact operations**

```java
// Even though this has 3 operations, it's still O(1)
public int getFirst(int[] arr) {
    int first = arr[0];        // Operation 1
    int doubled = first * 2;   // Operation 2
    return doubled;            // Operation 3
}
// Still O(1) because it doesn't depend on input size!
```

```java
// Even with 2 loops, it's O(n), not O(2n)
public void printTwice(int[] arr) {
    for (int num : arr) {      // O(n)
        System.out.println(num);
    }
    for (int num : arr) {      // O(n)
        System.out.println(num);
    }
}
// Total: O(n) + O(n) = O(2n) → Drop constant → O(n)
```

**Why?** When n = 1,000,000, the difference between 1,000,000 and 2,000,000 operations doesn't matter compared to the difference between 1,000,000 and 1,000,000,000,000 operations.

### Rule 2: Drop Non-Dominant Terms

**Keep only the fastest-growing term**

```java
public void example(int[] arr) {
    // Part 1: O(n)
    for (int i = 0; i < arr.length; i++) {
        System.out.println(arr[i]);
    }

    // Part 2: O(n²)
    for (int i = 0; i < arr.length; i++) {
        for (int j = 0; j < arr.length; j++) {
            System.out.println(arr[i] + arr[j]);
        }
    }
}
// Total: O(n) + O(n²) = O(n² + n) → Drop n → O(n²)
```

**Why?** When n = 1000:
- n = 1,000
- n² = 1,000,000

The n² dominates so much that n becomes irrelevant!

### Rule 3: Different Inputs = Different Variables

```java
public void printBoth(int[] arr1, int[] arr2) {
    for (int num : arr1) {        // O(a) where a = arr1.length
        System.out.println(num);
    }
    for (int num : arr2) {        // O(b) where b = arr2.length
        System.out.println(num);
    }
}
// Total: O(a + b), NOT O(n)
```

### Rule 4: Nested = Multiply, Sequential = Add

**Sequential (one after another) = ADD**
```java
public void sequential(int[] arr) {
    for (int i = 0; i < arr.length; i++) {     // O(n)
        System.out.println(arr[i]);
    }
    for (int i = 0; i < arr.length; i++) {     // O(n)
        System.out.println(arr[i]);
    }
}
// O(n) + O(n) = O(n)
```

**Nested (one inside another) = MULTIPLY**
```java
public void nested(int[] arr) {
    for (int i = 0; i < arr.length; i++) {     // O(n)
        for (int j = 0; j < arr.length; j++) { // O(n)
            System.out.println(arr[i] + arr[j]);
        }
    }
}
// O(n) * O(n) = O(n²)
```

---

## Step-by-Step Big O Analysis Examples

### Example 1: Simple Loop

```java
public int sum(int[] arr) {
    int total = 0;                    // O(1) - one operation
    for (int i = 0; i < arr.length; i++) {  // Loops n times
        total += arr[i];              // O(1) - one operation per loop
    }
    return total;                     // O(1) - one operation
}
```

**Analysis:**
1. `int total = 0` → O(1)
2. Loop runs `n` times → O(n)
3. Inside loop: `total += arr[i]` → O(1) per iteration
4. `return total` → O(1)

**Total:** O(1) + O(n * 1) + O(1) = O(n + 2) → Drop constants → **O(n)**

### Example 2: Nested Loops

```java
public void printPairs(int[] arr) {
    for (int i = 0; i < arr.length; i++) {     // Loops n times
        for (int j = 0; j < arr.length; j++) { // Loops n times for each i
            System.out.println(arr[i] + ", " + arr[j]);
        }
    }
}
```

**Analysis:**
1. Outer loop: runs `n` times
2. Inner loop: runs `n` times for EACH iteration of outer loop
3. Total iterations: n × n = n²

**Total:** **O(n²)**

**Concrete example:** If array has 5 elements:
- Outer loop: 5 iterations
- Inner loop: 5 iterations per outer
- Total prints: 5 × 5 = 25

### Example 3: Two Separate Loops

```java
public void printArrays(int[] arr1, int[] arr2) {
    for (int num : arr1) {       // O(a) where a = arr1.length
        System.out.println(num);
    }

    for (int num : arr2) {       // O(b) where b = arr2.length
        System.out.println(num);
    }
}
```

**Analysis:**
1. First loop: runs `a` times (length of arr1)
2. Second loop: runs `b` times (length of arr2)
3. They're sequential, not nested, so we ADD

**Total:** **O(a + b)**

**Common mistake:** Don't say O(n) because there are two different inputs!

### Example 4: Nested Loops with Different Arrays

```java
public void printPairs(int[] arr1, int[] arr2) {
    for (int num1 : arr1) {      // O(a)
        for (int num2 : arr2) {  // O(b)
            System.out.println(num1 + ", " + num2);
        }
    }
}
```

**Analysis:**
1. Outer loop: runs `a` times
2. Inner loop: runs `b` times for each outer iteration
3. They're nested, so we MULTIPLY

**Total:** **O(a × b)**

### Example 5: Partial Loop

```java
public void printHalf(int[] arr) {
    for (int i = 0; i < arr.length / 2; i++) {
        System.out.println(arr[i]);
    }
}
```

**Analysis:**
1. Loop runs n/2 times
2. n/2 = (1/2) × n
3. Drop constant (1/2)

**Total:** O(n/2) → Drop constant → **O(n)**

### Example 6: Binary Search (The Tricky One!)

```java
public int binarySearch(int[] arr, int target) {
    int left = 0;
    int right = arr.length - 1;

    while (left <= right) {
        int mid = left + (right - left) / 2;

        if (arr[mid] == target) {
            return mid;
        } else if (arr[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    return -1;
}
```

**Analysis:**
1. Each iteration divides the search space in half
2. Start: n elements
3. After 1st iteration: n/2 elements
4. After 2nd iteration: n/4 elements
5. After 3rd iteration: n/8 elements
6. Continue until 1 element remains

**How many times can you divide n by 2 until you get 1?**
- n / 2 / 2 / 2 / ... = 1
- This is log₂(n)!

**Total:** **O(log n)**

**Proof:**
- n = 1000 → 1000, 500, 250, 125, 62, 31, 15, 7, 3, 1 (10 steps)
- log₂(1000) ≈ 10 ✓

### Example 7: Nested Loops with Dependencies

```java
public void printTriangle(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
        for (int j = 0; j < i; j++) {  // Notice: j < i, not j < arr.length
            System.out.println(arr[i] + ", " + arr[j]);
        }
    }
}
```

**Analysis:**
1. Outer loop: n iterations
2. Inner loop iterations depend on i:
   - When i=0: 0 iterations
   - When i=1: 1 iteration
   - When i=2: 2 iterations
   - ...
   - When i=n-1: n-1 iterations
3. Total: 0 + 1 + 2 + 3 + ... + (n-1) = n(n-1)/2 = (n² - n)/2

**Total:** O(n²/2 - n/2) → Drop constants and non-dominant → **O(n²)**

### Example 8: Multiple Operations

```java
public void complex(int[] arr) {
    // Part 1: O(1)
    int first = arr[0];
    int last = arr[arr.length - 1];

    // Part 2: O(n)
    for (int i = 0; i < arr.length; i++) {
        System.out.println(arr[i]);
    }

    // Part 3: O(n²)
    for (int i = 0; i < arr.length; i++) {
        for (int j = 0; j < arr.length; j++) {
            System.out.println(arr[i] + arr[j]);
        }
    }

    // Part 4: O(n)
    for (int i = 0; i < arr.length; i++) {
        System.out.println(arr[i]);
    }
}
```

**Analysis:**
1. Part 1: O(1)
2. Part 2: O(n)
3. Part 3: O(n²)
4. Part 4: O(n)

**Total:** O(1) + O(n) + O(n²) + O(n) = O(n² + 2n + 1)

**Simplify:**
- Drop constants: O(n² + 2n + 1) → O(n² + n)
- Drop non-dominant: O(n² + n) → **O(n²)**

**Key insight:** The nested loop (O(n²)) dominates everything else!

---

## Common Big O Complexities Explained

### O(1) - Constant Time ⚡

**"No matter how big the input, it takes the same time"**

```java
// Examples of O(1) operations:

public int getElement(int[] arr, int index) {
    return arr[index];  // Direct access - always one step
}

public void addToHashMap(Map<Integer, String> map, int key, String value) {
    map.put(key, value);  // Average O(1)
}

public int getFirst(List<Integer> list) {
    return list.get(0);  // First element - always one step
}

public void swap(int[] arr, int i, int j) {
    int temp = arr[i];  // O(1)
    arr[i] = arr[j];    // O(1)
    arr[j] = temp;      // O(1)
    // Total: O(1) even with multiple operations
}
```

**Visual:**
```
Input size: 10 → 1 operation
Input size: 1,000 → 1 operation
Input size: 1,000,000 → 1 operation
```

### O(log n) - Logarithmic Time 📊

**"Dividing the problem in half each time"**

```java
// Binary search - halves the search space each iteration
public int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;

    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}

// Finding element in balanced BST
public TreeNode search(TreeNode root, int value) {
    if (root == null || root.value == value) return root;
    if (value < root.value) return search(root.left, value);
    return search(root.right, value);
}
```

**Why O(log n)?**
```
Search in array of 1,000 elements:
1000 → 500 → 250 → 125 → 62 → 31 → 15 → 7 → 3 → 1
That's about 10 steps for 1,000 elements!

log₂(1,000) ≈ 10
```

**Visual:**
```
Input size: 8 → 3 operations (8→4→2→1)
Input size: 16 → 4 operations (16→8→4→2→1)
Input size: 1,024 → 10 operations
Input size: 1,048,576 → 20 operations!
```

### O(n) - Linear Time 📈

**"Visit each element once"**

```java
// Simple loop
public int sum(int[] arr) {
    int total = 0;
    for (int num : arr) {  // Visits each element exactly once
        total += num;
    }
    return total;
}

// Finding maximum
public int findMax(int[] arr) {
    int max = arr[0];
    for (int num : arr) {  // O(n)
        if (num > max) max = num;
    }
    return max;
}

// Two separate loops - still O(n)
public void printTwice(int[] arr) {
    for (int num : arr) System.out.println(num);  // O(n)
    for (int num : arr) System.out.println(num);  // O(n)
    // Total: O(n) + O(n) = O(2n) → O(n)
}
```

**Visual:**
```
Input size: 10 → 10 operations
Input size: 100 → 100 operations
Input size: 1,000 → 1,000 operations
```

### O(n log n) - Linearithmic Time 🎯

**"Efficient sorting - best you can do for comparison-based sorting"**

```java
// Merge sort
public void mergeSort(int[] arr, int left, int right) {
    if (left < right) {
        int mid = left + (right - left) / 2;
        mergeSort(arr, left, mid);      // T(n/2)
        mergeSort(arr, mid + 1, right); // T(n/2)
        merge(arr, left, mid, right);   // O(n)
    }
}
// Each level is O(n), tree has log(n) levels
// Total: O(n log n)

// Quick sort (average case)
// Heap sort
```

**Why n log n?**
```
Merge sort divides array log(n) times (like binary search)
At each level, it merges all n elements
Levels: log(n)
Work per level: n
Total: n × log(n) = O(n log n)
```

**Visual:**
```
Input size: 10 → ~30 operations
Input size: 100 → ~664 operations
Input size: 1,000 → ~10,000 operations
```

### O(n²) - Quadratic Time ⚠️

**"Nested loops - check all pairs"**

```java
// Classic nested loops
public void printAllPairs(int[] arr) {
    for (int i = 0; i < arr.length; i++) {         // n times
        for (int j = 0; j < arr.length; j++) {     // n times for each i
            System.out.println(arr[i] + ", " + arr[j]);
        }
    }
}
// Total: n × n = n²

// Bubble sort
public void bubbleSort(int[] arr) {
    for (int i = 0; i < arr.length; i++) {         // O(n)
        for (int j = 0; j < arr.length - i - 1; j++) {  // O(n)
            if (arr[j] > arr[j + 1]) {
                swap(arr, j, j + 1);
            }
        }
    }
}
// O(n²)

// Finding duplicates (naive approach)
public boolean hasDuplicate(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            if (arr[i] == arr[j]) return true;
        }
    }
    return false;
}
// O(n²) - but can be O(n) with HashSet!
```

**Visual:**
```
Input size: 10 → 100 operations
Input size: 100 → 10,000 operations
Input size: 1,000 → 1,000,000 operations (😱)
```

### O(2ⁿ) - Exponential Time 💀

**"AVOID AT ALL COSTS - doubles with each additional input"**

```java
// Naive recursive Fibonacci
public int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n - 1) + fibonacci(n - 2);
}
// Each call makes 2 more calls → exponential growth!

// Trying all subsets
public List<List<Integer>> subsets(int[] nums) {
    // Generates 2ⁿ subsets
    // [1,2,3] → 2³ = 8 subsets
}
```

**Why so bad?**
```
fibonacci(5) calls:
                    fib(5)
                   /      \
              fib(4)      fib(3)
             /    \        /    \
        fib(3)  fib(2)  fib(2) fib(1)
       /   \    /   \    /  \
   fib(2) fib(1) ...  ... ... ...

Total calls: 1 + 2 + 4 + 8 + 16 = 31 calls for just n=5!
```

**Visual:**
```
Input: 10 → 1,024 operations
Input: 20 → 1,048,576 operations
Input: 30 → 1,073,741,824 operations (over 1 billion!)
Input: 50 → Would take years to complete! 💀
```

**How to fix:** Use memoization/dynamic programming to reduce to O(n)

---

## Space Complexity

**Space complexity measures how much memory an algorithm uses**

### Common Space Complexities

**O(1) - Constant Space:**
```java
// Only uses a fixed amount of extra variables
public int sum(int[] arr) {
    int total = 0;  // O(1) space - one variable
    for (int num : arr) {
        total += num;
    }
    return total;
}
// Input array doesn't count as "extra" space
```

**O(n) - Linear Space:**
```java
// Creates a new array of size n
public int[] doubleArray(int[] arr) {
    int[] result = new int[arr.length];  // O(n) space
    for (int i = 0; i < arr.length; i++) {
        result[i] = arr[i] * 2;
    }
    return result;
}

// Recursive calls use stack space
public int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}
// Call stack depth = n → O(n) space
```

**O(n²) - Quadratic Space:**
```java
// 2D array
public int[][] create2DArray(int n) {
    int[][] matrix = new int[n][n];  // O(n²) space
    return matrix;
}
```

### Time vs Space Trade-offs

**Often you can trade space for time:**

```java
// Fibonacci - O(2ⁿ) time, O(n) space (call stack)
public int fibSlow(int n) {
    if (n <= 1) return n;
    return fibSlow(n - 1) + fibSlow(n - 2);
}

// Fibonacci with memoization - O(n) time, O(n) space
public int fibFast(int n) {
    return fibHelper(n, new HashMap<>());
}

private int fibHelper(int n, Map<Integer, Integer> memo) {
    if (n <= 1) return n;
    if (memo.containsKey(n)) return memo.get(n);

    int result = fibHelper(n - 1, memo) + fibHelper(n - 2, memo);
    memo.put(n, result);
    return result;
}
// Used extra space (HashMap) to dramatically improve time!
```

---

## Practice Problems - Calculate Big O

### Problem 1
```java
public void mystery1(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
        System.out.println(arr[i]);
    }
    for (int i = 0; i < arr.length; i++) {
        System.out.println(arr[i]);
    }
}
```
<details>
<summary>Answer</summary>

**O(n)**
- Two separate loops: O(n) + O(n) = O(2n) → O(n)
</details>

### Problem 2
```java
public void mystery2(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
        for (int j = i; j < arr.length; j++) {
            System.out.println(arr[i] + arr[j]);
        }
    }
}
```
<details>
<summary>Answer</summary>

**O(n²)**
- Outer loop: n times
- Inner loop: starts at i, goes to n (still O(n) iterations on average)
- Total: n × n = O(n²)
</details>

### Problem 3
```java
public int mystery3(int[] arr) {
    if (arr.length == 0) return -1;
    if (arr.length == 1) return arr[0];

    int mid = arr.length / 2;
    int[] left = Arrays.copyOfRange(arr, 0, mid);
    return mystery3(left);
}
```
<details>
<summary>Answer</summary>

**O(n log n)**
- Each recursive call: Arrays.copyOfRange is O(n)
- Number of recursive calls: log(n) (dividing in half)
- Total: O(n log n)
</details>

### Problem 4
```java
public void mystery4(int n) {
    for (int i = 1; i < n; i *= 2) {
        System.out.println(i);
    }
}
```
<details>
<summary>Answer</summary>

**O(log n)**
- Loop multiplies i by 2 each time (i = 1, 2, 4, 8, 16...)
- How many times can you multiply by 2 before reaching n? log₂(n) times
</details>

### Problem 5
```java
public boolean mystery5(int[] arr, int target) {
    Set<Integer> seen = new HashSet<>();
    for (int num : arr) {
        if (seen.contains(target - num)) {
            return true;
        }
        seen.add(num);
    }
    return false;
}
```
<details>
<summary>Answer</summary>

**O(n) time, O(n) space**
- Loop runs n times: O(n)
- HashSet operations (contains, add): O(1) average
- Total time: O(n)
- Space: HashSet stores up to n elements: O(n)
</details>

---

## Big O Cheat Sheet

### Common Data Structure Operations

| Data Structure | Access | Search | Insertion | Deletion |
|----------------|--------|--------|-----------|----------|
| Array | O(1) | O(n) | O(n) | O(n) |
| ArrayList | O(1) | O(n) | O(1) amortized | O(n) |
| LinkedList | O(n) | O(n) | O(1) | O(1) |
| Stack | O(n) | O(n) | O(1) | O(1) |
| Queue | O(n) | O(n) | O(1) | O(1) |
| HashMap | - | O(1) avg | O(1) avg | O(1) avg |
| TreeMap | - | O(log n) | O(log n) | O(log n) |
| HashSet | - | O(1) avg | O(1) avg | O(1) avg |
| TreeSet | - | O(log n) | O(log n) | O(log n) |

### Common Sorting Algorithms

| Algorithm | Best | Average | Worst | Space |
|-----------|------|---------|-------|-------|
| Bubble Sort | O(n) | O(n²) | O(n²) | O(1) |
| Selection Sort | O(n²) | O(n²) | O(n²) | O(1) |
| Insertion Sort | O(n) | O(n²) | O(n²) | O(1) |
| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n) |
| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) |
| Heap Sort | O(n log n) | O(n log n) | O(n log n) | O(1) |

### How to Recognize Big O

| Code Pattern | Big O | How to spot it |
|--------------|-------|----------------|
| Single loop | O(n) | `for (int i = 0; i < n; i++)` |
| Nested loops | O(n²) | Loop inside loop |
| Halving each iteration | O(log n) | `i *= 2` or `i /= 2` |
| Divide and conquer | O(n log n) | Recursively split + merge |
| Trying all combinations | O(2ⁿ) | Recursive with 2 branches |
| HashMap lookup | O(1) | `map.get(key)` |
| Sorting then searching | O(n log n) | Sort first, then binary search |

---

## Real-World Examples

### Example: Finding Duplicates

**Bad Approach - O(n²):**
```java
public boolean hasDuplicate(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            if (arr[i] == arr[j]) return true;
        }
    }
    return false;
}
// For 1000 elements: ~500,000 comparisons!
```

**Good Approach - O(n):**
```java
public boolean hasDuplicate(int[] arr) {
    Set<Integer> seen = new HashSet<>();
    for (int num : arr) {
        if (seen.contains(num)) return true;  // O(1) lookup
        seen.add(num);  // O(1) insertion
    }
    return false;
}
// For 1000 elements: ~1000 operations!
```

### Example: Two Sum Problem

**Bad - O(n²):**
```java
public int[] twoSum(int[] nums, int target) {
    for (int i = 0; i < nums.length; i++) {
        for (int j = i + 1; j < nums.length; j++) {
            if (nums[i] + nums[j] == target) {
                return new int[]{i, j};
            }
        }
    }
    return null;
}
```

**Good - O(n):**
```java
public int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (map.containsKey(complement)) {
            return new int[]{map.get(complement), i};
        }
        map.put(nums[i], i);
    }
    return null;
}
```

---

## Summary

### Key Takeaways

1. **Big O measures growth rate** - How algorithm scales with input size
2. **Drop constants and non-dominant terms** - O(2n) → O(n), O(n² + n) → O(n²)
3. **Different inputs = different variables** - O(a + b), not O(n)
4. **Nested loops multiply** - Two nested loops of n = O(n²)
5. **Sequential operations add** - Two separate loops = O(n) + O(n) = O(n)

### Priority Order (Best to Worst)

```
O(1) ⚡ > O(log n) 📊 > O(n) 📈 > O(n log n) 🎯 > O(n²) ⚠️ > O(2ⁿ) 💀
```

### Quick Decision Tree

```
Does runtime depend on input size?
├─ No → O(1)
└─ Yes → Does it loop through all elements?
    ├─ Once → O(n)
    ├─ Twice (nested) → O(n²)
    └─ Dividing in half each time → O(log n)
```

---

## Arrays and ArrayLists

### Arrays (Fixed Size)

**Declaration:**
```java
// Fixed size - cannot change after creation
int[] numbers = new int[5];  // Array of 5 integers
numbers[0] = 10;
numbers[1] = 20;

// Initialize with values
String[] names = {"Alice", "Bob", "Charlie"};

// Object arrays
Lecture[] lectures = new Lecture[10];
```

**Characteristics:**
- ✅ O(1) access by index
- ✅ Memory efficient
- ❌ Fixed size (cannot grow)
- ❌ O(n) to insert/delete (need to shift elements)

### ArrayList (Dynamic Array)

**The most commonly used collection in Java!**

```java
// Creates a dynamic array that grows automatically
List<Lecture> lectures = new ArrayList<>();

// Add elements - O(1) amortized
lectures.add(lecture1);
lectures.add(lecture2);
lectures.add(lecture3);

// Access by index - O(1)
Lecture first = lectures.get(0);

// Size
int count = lectures.size();

// Remove element - O(n) because it shifts elements
lectures.remove(0);  // Removes first element, shifts all others left

// Check if contains - O(n)
boolean hasLecture = lectures.contains(lecture1);

// Iterate - O(n)
for (Lecture lecture : lectures) {
    System.out.println(lecture.getTitle());
}

// Stream operations
List<String> titles = lectures.stream()
    .map(Lecture::getTitle)
    .collect(Collectors.toList());
```

**Time Complexities:**

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| `get(index)` | O(1) | Direct access |
| `add(element)` | O(1) amortized | May need to resize array |
| `add(index, element)` | O(n) | Shifts elements |
| `remove(index)` | O(n) | Shifts elements |
| `contains(element)` | O(n) | Linear search |
| `size()` | O(1) | Stores size |

**When to Use ArrayList:**
- ✅ Need random access by index
- ✅ Mostly reading data
- ✅ Adding to end of list
- ❌ Frequent insertions in middle
- ❌ Frequent removals from middle

### Example from Your Codebase

```java
@Service
public class LectureService {

    // ArrayList returned by JPA
    public List<Lecture> getAllLectures() {
        return lectureRepository.findAll();  // Returns ArrayList
    }

    // Efficient: accessing by index
    public Lecture getFirst(List<Lecture> lectures) {
        return lectures.get(0);  // O(1)
    }

    // Inefficient: searching by attribute
    public Lecture findByTitle(List<Lecture> lectures, String title) {
        for (Lecture lecture : lectures) {  // O(n)
            if (lecture.getTitle().equals(title)) {
                return lecture;
            }
        }
        return null;
    }

    // Better: use HashMap for lookups
    public Map<String, Lecture> indexByTitle(List<Lecture> lectures) {
        return lectures.stream()
            .collect(Collectors.toMap(
                Lecture::getTitle,
                lecture -> lecture
            ));
    }
}
```

---

## LinkedLists

### What is a LinkedList?

A **LinkedList** stores elements as nodes, where each node contains:
1. The data
2. A reference to the next node

```
[Data|Next] -> [Data|Next] -> [Data|Next] -> null
   Node 1         Node 2         Node 3
```

### LinkedList in Java

```java
// Create a LinkedList
LinkedList<Lecture> lectures = new LinkedList<>();

// Add elements - O(1) at beginning/end
lectures.addFirst(lecture1);  // Add to beginning
lectures.addLast(lecture2);   // Add to end
lectures.add(lecture3);       // Add to end (same as addLast)

// Remove from beginning/end - O(1)
Lecture first = lectures.removeFirst();
Lecture last = lectures.removeLast();

// Access by index - O(n) - SLOW!
Lecture third = lectures.get(2);  // Must traverse from beginning

// Insert in middle - O(n) to find position, O(1) to insert
lectures.add(1, newLecture);
```

### ArrayList vs LinkedList

| Operation | ArrayList | LinkedList |
|-----------|-----------|------------|
| `get(index)` | O(1) ✅ | O(n) ❌ |
| `add(element)` | O(1) ✅ | O(1) ✅ |
| `add(0, element)` | O(n) ❌ | O(1) ✅ |
| `remove(0)` | O(n) ❌ | O(1) ✅ |
| Memory | Less ✅ | More ❌ (extra pointers) |

### When to Use LinkedList

**Use LinkedList when:**
- ✅ Frequent insertions/deletions at beginning
- ✅ Implementing a Queue or Deque
- ✅ Don't need random access

**Use ArrayList when:**
- ✅ Need random access (get by index)
- ✅ Mostly adding to end
- ✅ Iterating through all elements

**Real Talk:** In practice, ArrayList is almost always better because:
- Modern CPUs cache arrays efficiently
- Random access is very common
- ArrayList's O(n) operations are still fast for small/medium lists

---

## Stacks

### What is a Stack?

A **Stack** is a Last-In-First-Out (LIFO) data structure.

**Analogy:** Stack of plates
- You add plates to the top (push)
- You remove plates from the top (pop)
- You can only see the top plate (peek)

```
    pop/push
        ↓
    [Plate 3]  ← Top
    [Plate 2]
    [Plate 1]
    ─────────
     Bottom
```

### Stack in Java

```java
// Create a stack
Stack<String> history = new Stack<>();

// Push - O(1)
history.push("page1.html");
history.push("page2.html");
history.push("page3.html");

// Peek (look at top without removing) - O(1)
String current = history.peek();  // "page3.html"

// Pop (remove from top) - O(1)
String page = history.pop();  // "page3.html"
page = history.pop();          // "page2.html"

// Check if empty
boolean isEmpty = history.isEmpty();

// Size
int size = history.size();
```

### Common Use Cases

**1. Undo/Redo Operations:**
```java
Stack<Action> undoStack = new Stack<>();
Stack<Action> redoStack = new Stack<>();

public void performAction(Action action) {
    action.execute();
    undoStack.push(action);
    redoStack.clear();  // Clear redo history
}

public void undo() {
    if (!undoStack.isEmpty()) {
        Action action = undoStack.pop();
        action.undo();
        redoStack.push(action);
    }
}

public void redo() {
    if (!redoStack.isEmpty()) {
        Action action = redoStack.pop();
        action.execute();
        undoStack.push(action);
    }
}
```

**2. Browser History:**
```java
Stack<String> backHistory = new Stack<>();
Stack<String> forwardHistory = new Stack<>();

public void visitPage(String url) {
    backHistory.push(currentPage);
    currentPage = url;
    forwardHistory.clear();
}

public void goBack() {
    if (!backHistory.isEmpty()) {
        forwardHistory.push(currentPage);
        currentPage = backHistory.pop();
    }
}

public void goForward() {
    if (!forwardHistory.isEmpty()) {
        backHistory.push(currentPage);
        currentPage = forwardHistory.pop();
    }
}
```

**3. Expression Evaluation:**
```java
// Evaluate postfix expression: 3 4 + 5 *
// Result: (3 + 4) * 5 = 35
public int evaluatePostfix(String[] tokens) {
    Stack<Integer> stack = new Stack<>();

    for (String token : tokens) {
        if (isNumber(token)) {
            stack.push(Integer.parseInt(token));
        } else {
            int b = stack.pop();
            int a = stack.pop();
            int result = applyOperation(token, a, b);
            stack.push(result);
        }
    }

    return stack.pop();
}
```

**4. Checking Balanced Parentheses:**
```java
public boolean isBalanced(String expression) {
    Stack<Character> stack = new Stack<>();

    for (char c : expression.toCharArray()) {
        if (c == '(' || c == '[' || c == '{') {
            stack.push(c);
        } else if (c == ')' || c == ']' || c == '}') {
            if (stack.isEmpty()) return false;
            char open = stack.pop();
            if (!matches(open, c)) return false;
        }
    }

    return stack.isEmpty();
}

private boolean matches(char open, char close) {
    return (open == '(' && close == ')') ||
           (open == '[' && close == ']') ||
           (open == '{' && close == '}');
}
```

---

## Queues

### What is a Queue?

A **Queue** is a First-In-First-Out (FIFO) data structure.

**Analogy:** Line at a coffee shop
- People join at the back (enqueue)
- People leave from the front (dequeue)
- First person in line is served first

```
Front                                    Back
  ↓                                        ↓
[Person 1] [Person 2] [Person 3] [Person 4]
    ↑                                     ↑
  Remove                                 Add
 (dequeue)                            (enqueue)
```

### Queue in Java

```java
// Create a queue (using LinkedList implementation)
Queue<String> queue = new LinkedList<>();

// Enqueue (add to back) - O(1)
queue.offer("customer1");
queue.offer("customer2");
queue.offer("customer3");

// Peek (look at front without removing) - O(1)
String next = queue.peek();  // "customer1"

// Dequeue (remove from front) - O(1)
String served = queue.poll();  // "customer1"

// Size
int size = queue.size();

// Check if empty
boolean isEmpty = queue.isEmpty();
```

### Queue Methods

| Method | Returns null on failure | Throws exception on failure |
|--------|------------------------|----------------------------|
| Add | `offer(e)` | `add(e)` |
| Remove | `poll()` | `remove()` |
| Examine | `peek()` | `element()` |

**Recommendation:** Use `offer()`, `poll()`, and `peek()` (return null instead of throwing)

### Common Use Cases

**1. Task Processing:**
```java
Queue<Task> taskQueue = new LinkedList<>();

// Producer adds tasks
public void submitTask(Task task) {
    taskQueue.offer(task);
}

// Consumer processes tasks
public void processTasks() {
    while (!taskQueue.isEmpty()) {
        Task task = taskQueue.poll();
        task.execute();
    }
}
```

**2. Breadth-First Search (BFS):**
```java
public void breadthFirstSearch(Node root) {
    Queue<Node> queue = new LinkedList<>();
    Set<Node> visited = new HashSet<>();

    queue.offer(root);
    visited.add(root);

    while (!queue.isEmpty()) {
        Node current = queue.poll();
        System.out.println(current.value);

        for (Node neighbor : current.neighbors) {
            if (!visited.contains(neighbor)) {
                queue.offer(neighbor);
                visited.add(neighbor);
            }
        }
    }
}
```

**3. Print Queue:**
```java
class PrintJob {
    String document;
    int priority;
}

Queue<PrintJob> printQueue = new LinkedList<>();

public void addPrintJob(PrintJob job) {
    printQueue.offer(job);
    System.out.println("Added to print queue: " + job.document);
}

public void printNext() {
    if (!printQueue.isEmpty()) {
        PrintJob job = printQueue.poll();
        System.out.println("Printing: " + job.document);
    }
}
```

### PriorityQueue

**A queue where elements are ordered by priority, not insertion order!**

```java
// Natural ordering (smallest first)
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(5);
pq.offer(1);
pq.offer(3);
System.out.println(pq.poll());  // 1 (smallest)
System.out.println(pq.poll());  // 3
System.out.println(pq.poll());  // 5

// Custom ordering (largest first)
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(
    (a, b) -> b - a  // Reverse order
);

// Priority queue with custom objects
PriorityQueue<Lecture> lectureQueue = new PriorityQueue<>(
    Comparator.comparingInt(Lecture::getPlayCount).reversed()
);
```

**Use Cases:**
- Task scheduling (highest priority first)
- Finding top N elements
- Dijkstra's shortest path algorithm
- Merge K sorted lists

---

## HashMaps and HashSets

### HashMap - Key-Value Storage

**HashMap** stores key-value pairs with O(1) average lookup time!

```java
// Create a HashMap
Map<Long, Lecture> lectureMap = new HashMap<>();

// Put (add/update) - O(1) average
lectureMap.put(1L, lecture1);
lectureMap.put(2L, lecture2);
lectureMap.put(3L, lecture3);

// Get - O(1) average
Lecture lecture = lectureMap.get(1L);

// Check if key exists - O(1) average
boolean exists = lectureMap.containsKey(1L);

// Check if value exists - O(n)
boolean hasLecture = lectureMap.containsValue(lecture1);

// Remove - O(1) average
lectureMap.remove(1L);

// Size
int size = lectureMap.size();

// Iterate over entries
for (Map.Entry<Long, Lecture> entry : lectureMap.entrySet()) {
    Long id = entry.getKey();
    Lecture lec = entry.getValue();
    System.out.println(id + ": " + lec.getTitle());
}

// Iterate over keys
for (Long id : lectureMap.keySet()) {
    System.out.println(id);
}

// Iterate over values
for (Lecture lec : lectureMap.values()) {
    System.out.println(lec.getTitle());
}

// Get or default
Lecture lec = lectureMap.getOrDefault(99L, defaultLecture);

// Compute if absent (add if key doesn't exist)
lectureMap.computeIfAbsent(4L, id -> fetchLectureFromDatabase(id));

// Merge (update if exists, add if not)
lectureMap.merge(1L, newLecture, (existing, incoming) -> incoming);
```

### How HashMap Works Internally

**Behind the scenes:**

1. HashMap uses a hash function to convert keys to array indices
2. Stores key-value pairs in "buckets" (array positions)
3. Handles collisions with linked lists (or trees for large collisions)

```
Key: "lecture1" → hash() → 15 → Store at bucket 15
Key: "lecture2" → hash() → 27 → Store at bucket 27
Key: "lecture3" → hash() → 15 → Collision! Chain with linked list

Buckets:
[0]  null
[1]  null
...
[15] ["lecture1", lecture1] → ["lecture3", lecture3]
...
[27] ["lecture2", lecture2]
```

### HashSet - Unique Elements

**HashSet** stores unique elements (no duplicates) with O(1) lookup!

```java
// Create a HashSet
Set<String> genres = new HashSet<>();

// Add - O(1) average
genres.add("Philosophy");
genres.add("History");
genres.add("Science");
genres.add("Philosophy");  // Duplicate - won't be added!

// Size
System.out.println(genres.size());  // 3 (not 4!)

// Contains - O(1) average
boolean hasPhilosophy = genres.contains("Philosophy");  // true

// Remove - O(1) average
genres.remove("History");

// Iterate
for (String genre : genres) {
    System.out.println(genre);
}

// Convert List to Set (removes duplicates)
List<Integer> numbers = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
Set<Integer> uniqueNumbers = new HashSet<>(numbers);
System.out.println(uniqueNumbers);  // [1, 2, 3, 4]
```

### Real-World Examples

**1. Caching:**
```java
public class LectureCache {
    private Map<Long, Lecture> cache = new HashMap<>();

    public Lecture getLecture(Long id) {
        // O(1) cache lookup
        if (cache.containsKey(id)) {
            return cache.get(id);
        }

        // Cache miss - fetch from database
        Lecture lecture = lectureRepository.findById(id).orElse(null);
        if (lecture != null) {
            cache.put(id, lecture);
        }
        return lecture;
    }
}
```

**2. Counting Occurrences:**
```java
public Map<String, Integer> countGenres(List<Lecture> lectures) {
    Map<String, Integer> genreCounts = new HashMap<>();

    for (Lecture lecture : lectures) {
        String genre = lecture.getGenre();
        genreCounts.put(genre, genreCounts.getOrDefault(genre, 0) + 1);
    }

    return genreCounts;
}

// Better with merge:
public Map<String, Integer> countGenres(List<Lecture> lectures) {
    Map<String, Integer> genreCounts = new HashMap<>();

    for (Lecture lecture : lectures) {
        genreCounts.merge(lecture.getGenre(), 1, Integer::sum);
    }

    return genreCounts;
}
```

**3. Removing Duplicates:**
```java
public List<Lecture> removeDuplicates(List<Lecture> lectures) {
    Set<Long> seenIds = new HashSet<>();
    List<Lecture> unique = new ArrayList<>();

    for (Lecture lecture : lectures) {
        if (seenIds.add(lecture.getId())) {  // add() returns false if already exists
            unique.add(lecture);
        }
    }

    return unique;
}
```

**4. Two-Sum Problem:**
```java
// Find two numbers that add up to target
public int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> map = new HashMap<>();

    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (map.containsKey(complement)) {
            return new int[]{map.get(complement), i};
        }
        map.put(nums[i], i);
    }

    return null;
}
```

### HashMap vs TreeMap vs LinkedHashMap

| Type | Ordering | Access Time | When to Use |
|------|----------|-------------|-------------|
| **HashMap** | No order | O(1) | Fast lookups, don't care about order |
| **LinkedHashMap** | Insertion order | O(1) | Need insertion order preserved |
| **TreeMap** | Sorted by key | O(log n) | Need keys sorted |

```java
// HashMap - No guaranteed order
Map<String, Integer> hashMap = new HashMap<>();
hashMap.put("c", 3);
hashMap.put("a", 1);
hashMap.put("b", 2);
System.out.println(hashMap);  // Order not guaranteed: {a=1, b=2, c=3} or {c=3, a=1, b=2}

// LinkedHashMap - Maintains insertion order
Map<String, Integer> linkedMap = new LinkedHashMap<>();
linkedMap.put("c", 3);
linkedMap.put("a", 1);
linkedMap.put("b", 2);
System.out.println(linkedMap);  // {c=3, a=1, b=2} - insertion order!

// TreeMap - Sorted by key
Map<String, Integer> treeMap = new TreeMap<>();
treeMap.put("c", 3);
treeMap.put("a", 1);
treeMap.put("b", 2);
System.out.println(treeMap);  // {a=1, b=2, c=3} - alphabetically sorted!
```

---

## Trees

### What is a Tree?

A **Tree** is a hierarchical data structure with nodes connected by edges.

```
         Root
          |
      ┌───┴───┐
    Node    Node
      |       |
    ┌─┴─┐   ┌─┴─┐
  Leaf Leaf Leaf Leaf
```

### Tree Terminology

- **Root**: Top node (no parent)
- **Parent**: Node with children
- **Child**: Node with a parent
- **Leaf**: Node with no children
- **Height**: Longest path from root to leaf
- **Depth**: Distance from root to a node

### Binary Tree

**Each node has at most 2 children (left and right)**

```java
class TreeNode {
    int value;
    TreeNode left;
    TreeNode right;

    TreeNode(int value) {
        this.value = value;
    }
}
```

### Binary Search Tree (BST)

**A binary tree where:**
- Left child < Parent
- Right child > Parent

```
       8
      / \
     3   10
    / \    \
   1   6   14
      / \  /
     4  7 13
```

**Operations:**
```java
class BinarySearchTree {
    TreeNode root;

    // Insert - O(log n) average, O(n) worst case
    public void insert(int value) {
        root = insertRec(root, value);
    }

    private TreeNode insertRec(TreeNode node, int value) {
        if (node == null) {
            return new TreeNode(value);
        }

        if (value < node.value) {
            node.left = insertRec(node.left, value);
        } else if (value > node.value) {
            node.right = insertRec(node.right, value);
        }

        return node;
    }

    // Search - O(log n) average, O(n) worst case
    public boolean contains(int value) {
        return containsRec(root, value);
    }

    private boolean containsRec(TreeNode node, int value) {
        if (node == null) {
            return false;
        }

        if (value == node.value) {
            return true;
        }

        return value < node.value
            ? containsRec(node.left, value)
            : containsRec(node.right, value);
    }
}
```

### Tree Traversals

**1. Inorder (Left → Root → Right) - Visits nodes in sorted order for BST**
```java
public void inorder(TreeNode node) {
    if (node == null) return;

    inorder(node.left);
    System.out.println(node.value);
    inorder(node.right);
}
// Output for BST above: 1, 3, 4, 6, 7, 8, 10, 13, 14
```

**2. Preorder (Root → Left → Right) - Useful for copying tree**
```java
public void preorder(TreeNode node) {
    if (node == null) return;

    System.out.println(node.value);
    preorder(node.left);
    preorder(node.right);
}
// Output: 8, 3, 1, 6, 4, 7, 10, 14, 13
```

**3. Postorder (Left → Right → Root) - Useful for deleting tree**
```java
public void postorder(TreeNode node) {
    if (node == null) return;

    postorder(node.left);
    postorder(node.right);
    System.out.println(node.value);
}
// Output: 1, 4, 7, 6, 3, 13, 14, 10, 8
```

**4. Level Order (Breadth-First) - Level by level**
```java
public void levelOrder(TreeNode root) {
    if (root == null) return;

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        TreeNode node = queue.poll();
        System.out.println(node.value);

        if (node.left != null) queue.offer(node.left);
        if (node.right != null) queue.offer(node.right);
    }
}
// Output: 8, 3, 10, 1, 6, 14, 4, 7, 13
```

### Common Tree Problems

**1. Find Maximum Depth:**
```java
public int maxDepth(TreeNode root) {
    if (root == null) return 0;

    int leftDepth = maxDepth(root.left);
    int rightDepth = maxDepth(root.right);

    return Math.max(leftDepth, rightDepth) + 1;
}
```

**2. Check if Balanced:**
```java
public boolean isBalanced(TreeNode root) {
    return checkHeight(root) != -1;
}

private int checkHeight(TreeNode node) {
    if (node == null) return 0;

    int leftHeight = checkHeight(node.left);
    if (leftHeight == -1) return -1;

    int rightHeight = checkHeight(node.right);
    if (rightHeight == -1) return -1;

    if (Math.abs(leftHeight - rightHeight) > 1) {
        return -1;
    }

    return Math.max(leftHeight, rightHeight) + 1;
}
```

---

## Sorting Algorithms

### Why Learn Sorting?

- Understand algorithm efficiency
- Common in interviews
- Many problems require sorted data
- Foundation for advanced algorithms

### Bubble Sort - O(n²)

**Idea:** Repeatedly swap adjacent elements if they're in wrong order

```java
public void bubbleSort(int[] arr) {
    int n = arr.length;

    for (int i = 0; i < n - 1; i++) {
        boolean swapped = false;

        for (int j = 0; j < n - i - 1; j++) {
            if (arr[j] > arr[j + 1]) {
                // Swap
                int temp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = temp;
                swapped = true;
            }
        }

        // If no swaps, array is sorted
        if (!swapped) break;
    }
}
```

**Complexity:**
- Time: O(n²) - Two nested loops
- Space: O(1) - In-place sorting
- **Use:** Never in production! Educational only.

### Selection Sort - O(n²)

**Idea:** Find minimum element and put it at the beginning

```java
public void selectionSort(int[] arr) {
    int n = arr.length;

    for (int i = 0; i < n - 1; i++) {
        int minIndex = i;

        // Find minimum in remaining array
        for (int j = i + 1; j < n; j++) {
            if (arr[j] < arr[minIndex]) {
                minIndex = j;
            }
        }

        // Swap
        int temp = arr[i];
        arr[i] = arr[minIndex];
        arr[minIndex] = temp;
    }
}
```

**Complexity:**
- Time: O(n²)
- Space: O(1)
- **Use:** Small datasets, memory constrained

### Insertion Sort - O(n²)

**Idea:** Build sorted array one element at a time

```java
public void insertionSort(int[] arr) {
    int n = arr.length;

    for (int i = 1; i < n; i++) {
        int key = arr[i];
        int j = i - 1;

        // Shift elements greater than key to the right
        while (j >= 0 && arr[j] > key) {
            arr[j + 1] = arr[j];
            j--;
        }

        arr[j + 1] = key;
    }
}
```

**Complexity:**
- Time: O(n²) worst case, O(n) best case (already sorted)
- Space: O(1)
- **Use:** Small datasets, nearly sorted data

### Merge Sort - O(n log n) ⭐

**Idea:** Divide array in half, sort each half, merge sorted halves

```java
public void mergeSort(int[] arr) {
    if (arr.length < 2) return;

    int mid = arr.length / 2;
    int[] left = Arrays.copyOfRange(arr, 0, mid);
    int[] right = Arrays.copyOfRange(arr, mid, arr.length);

    mergeSort(left);
    mergeSort(right);
    merge(arr, left, right);
}

private void merge(int[] arr, int[] left, int[] right) {
    int i = 0, j = 0, k = 0;

    while (i < left.length && j < right.length) {
        if (left[i] <= right[j]) {
            arr[k++] = left[i++];
        } else {
            arr[k++] = right[j++];
        }
    }

    while (i < left.length) {
        arr[k++] = left[i++];
    }

    while (j < right.length) {
        arr[k++] = right[j++];
    }
}
```

**Complexity:**
- Time: O(n log n) - Guaranteed!
- Space: O(n) - Needs extra space for merging
- **Use:** Large datasets, stable sort needed

### Quick Sort - O(n log n) average ⭐

**Idea:** Pick pivot, partition around pivot, recursively sort partitions

```java
public void quickSort(int[] arr, int low, int high) {
    if (low < high) {
        int pivotIndex = partition(arr, low, high);
        quickSort(arr, low, pivotIndex - 1);
        quickSort(arr, pivotIndex + 1, high);
    }
}

private int partition(int[] arr, int low, int high) {
    int pivot = arr[high];
    int i = low - 1;

    for (int j = low; j < high; j++) {
        if (arr[j] < pivot) {
            i++;
            swap(arr, i, j);
        }
    }

    swap(arr, i + 1, high);
    return i + 1;
}

private void swap(int[] arr, int i, int j) {
    int temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
}
```

**Complexity:**
- Time: O(n log n) average, O(n²) worst case
- Space: O(log n) - Recursion stack
- **Use:** General purpose, often fastest in practice

### Java's Built-in Sort

**Use these instead of implementing your own!**

```java
// Arrays.sort() - Uses Dual-Pivot Quicksort for primitives
int[] numbers = {5, 2, 8, 1, 9};
Arrays.sort(numbers);

// Collections.sort() - Uses Timsort for objects
List<Integer> list = Arrays.asList(5, 2, 8, 1, 9);
Collections.sort(list);

// Sort with custom comparator
List<Lecture> lectures = getLectures();
lectures.sort(Comparator.comparingInt(Lecture::getPlayCount).reversed());

// Sort by multiple fields
lectures.sort(
    Comparator.comparing(Lecture::getGenre)
              .thenComparing(Lecture::getTitle)
);

// Stream sorting
List<Lecture> sorted = lectures.stream()
    .sorted(Comparator.comparing(Lecture::getTitle))
    .collect(Collectors.toList());
```

### Sorting Algorithm Comparison

| Algorithm | Time (Average) | Time (Worst) | Space | Stable? |
|-----------|---------------|--------------|-------|---------|
| Bubble Sort | O(n²) | O(n²) | O(1) | Yes |
| Selection Sort | O(n²) | O(n²) | O(1) | No |
| Insertion Sort | O(n²) | O(n²) | O(1) | Yes |
| Merge Sort | O(n log n) | O(n log n) | O(n) | Yes |
| Quick Sort | O(n log n) | O(n²) | O(log n) | No |
| Heap Sort | O(n log n) | O(n log n) | O(1) | No |

**Stable Sort:** Maintains relative order of equal elements

---

## Searching Algorithms

### Linear Search - O(n)

**Idea:** Check every element until found

```java
public int linearSearch(int[] arr, int target) {
    for (int i = 0; i < arr.length; i++) {
        if (arr[i] == target) {
            return i;
        }
    }
    return -1;  // Not found
}
```

**Complexity:**
- Time: O(n)
- Space: O(1)
- **Use:** Unsorted data, small arrays

### Binary Search - O(log n) ⭐

**Idea:** Repeatedly divide sorted array in half

**Requires:** Array must be sorted!

```java
public int binarySearch(int[] arr, int target) {
    int left = 0;
    int right = arr.length - 1;

    while (left <= right) {
        int mid = left + (right - left) / 2;  // Avoid overflow

        if (arr[mid] == target) {
            return mid;
        } else if (arr[mid] < target) {
            left = mid + 1;  // Search right half
        } else {
            right = mid - 1;  // Search left half
        }
    }

    return -1;  // Not found
}

// Recursive version
public int binarySearchRecursive(int[] arr, int target, int left, int right) {
    if (left > right) {
        return -1;
    }

    int mid = left + (right - left) / 2;

    if (arr[mid] == target) {
        return mid;
    } else if (arr[mid] < target) {
        return binarySearchRecursive(arr, target, mid + 1, right);
    } else {
        return binarySearchRecursive(arr, target, left, mid - 1);
    }
}
```

**Why O(log n)?**

```
Array of 1000 elements:
Step 1: Check middle (500) - 500 elements remain
Step 2: Check middle (250) - 250 elements remain
Step 3: Check middle (125) - 125 elements remain
...
Step 10: Found! (1000 → 500 → 250 → 125 → 62 → 31 → 15 → 7 → 3 → 1)

log₂(1000) ≈ 10 steps!
```

**Java's Built-in Binary Search:**
```java
int[] arr = {1, 3, 5, 7, 9, 11, 13};
int index = Arrays.binarySearch(arr, 7);  // Returns 3

List<Integer> list = Arrays.asList(1, 3, 5, 7, 9, 11, 13);
int index2 = Collections.binarySearch(list, 7);  // Returns 3
```

---

## Common Algorithm Patterns

### 1. Two Pointers

**Technique:** Use two pointers moving through array

**Example 1: Check if Palindrome**
```java
public boolean isPalindrome(String s) {
    int left = 0;
    int right = s.length() - 1;

    while (left < right) {
        if (s.charAt(left) != s.charAt(right)) {
            return false;
        }
        left++;
        right--;
    }

    return true;
}
```

**Example 2: Remove Duplicates from Sorted Array**
```java
public int removeDuplicates(int[] nums) {
    if (nums.length == 0) return 0;

    int writeIndex = 1;

    for (int readIndex = 1; readIndex < nums.length; readIndex++) {
        if (nums[readIndex] != nums[readIndex - 1]) {
            nums[writeIndex] = nums[readIndex];
            writeIndex++;
        }
    }

    return writeIndex;
}
```

### 2. Sliding Window

**Technique:** Maintain a window of elements

**Example: Maximum Sum Subarray of Size K**
```java
public int maxSumSubarray(int[] arr, int k) {
    int maxSum = 0;
    int windowSum = 0;

    // Calculate sum of first window
    for (int i = 0; i < k; i++) {
        windowSum += arr[i];
    }
    maxSum = windowSum;

    // Slide window
    for (int i = k; i < arr.length; i++) {
        windowSum += arr[i] - arr[i - k];  // Add new, remove old
        maxSum = Math.max(maxSum, windowSum);
    }

    return maxSum;
}
```

**Example: Longest Substring Without Repeating Characters**
```java
public int lengthOfLongestSubstring(String s) {
    Set<Character> window = new HashSet<>();
    int left = 0;
    int maxLength = 0;

    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);

        // Shrink window while duplicate exists
        while (window.contains(c)) {
            window.remove(s.charAt(left));
            left++;
        }

        window.add(c);
        maxLength = Math.max(maxLength, right - left + 1);
    }

    return maxLength;
}
```

### 3. Prefix Sum

**Technique:** Precompute cumulative sums for fast range queries

**Example: Range Sum Query**
```java
class RangeSumQuery {
    private int[] prefixSum;

    public RangeSumQuery(int[] nums) {
        prefixSum = new int[nums.length + 1];
        for (int i = 0; i < nums.length; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i];
        }
    }

    public int sumRange(int left, int right) {
        return prefixSum[right + 1] - prefixSum[left];
    }
}

// Usage:
int[] nums = {1, 2, 3, 4, 5};
RangeSumQuery query = new RangeSumQuery(nums);
query.sumRange(1, 3);  // Sum of [2,3,4] = 9
```

### 4. Frequency Counter

**Technique:** Use HashMap to count occurrences

**Example: Find First Non-Repeating Character**
```java
public char firstNonRepeating(String s) {
    Map<Character, Integer> freq = new HashMap<>();

    // Count frequencies
    for (char c : s.toCharArray()) {
        freq.put(c, freq.getOrDefault(c, 0) + 1);
    }

    // Find first with frequency 1
    for (char c : s.toCharArray()) {
        if (freq.get(c) == 1) {
            return c;
        }
    }

    return '\0';  // Not found
}
```

### 5. Recursion and Backtracking

**Recursion:** Function calls itself

**Example 1: Factorial**
```java
public int factorial(int n) {
    if (n <= 1) return 1;  // Base case
    return n * factorial(n - 1);  // Recursive case
}
```

**Example 2: Fibonacci**
```java
public int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n - 1) + fibonacci(n - 2);
}

// Optimized with memoization
public int fibonacciMemo(int n) {
    return fibHelper(n, new HashMap<>());
}

private int fibHelper(int n, Map<Integer, Integer> memo) {
    if (n <= 1) return n;
    if (memo.containsKey(n)) return memo.get(n);

    int result = fibHelper(n - 1, memo) + fibHelper(n - 2, memo);
    memo.put(n, result);
    return result;
}
```

**Backtracking Example: Generate All Subsets**
```java
public List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(nums, 0, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] nums, int start, List<Integer> current, List<List<Integer>> result) {
    result.add(new ArrayList<>(current));

    for (int i = start; i < nums.length; i++) {
        current.add(nums[i]);           // Choose
        backtrack(nums, i + 1, current, result);  // Explore
        current.remove(current.size() - 1);  // Unchoose (backtrack)
    }
}
```

---

## Real Examples from Your Codebase

### Example 1: ArrayList in JPA Queries

**LectureRepository.java:**
```java
@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    // Returns ArrayList - O(n) to fetch, O(1) to access by index
    List<Lecture> findAll();

    // O(n) linear search through results
    List<Lecture> findByGenre(String genre);

    // Returns Page<Lecture> - uses ArrayList internally
    Page<Lecture> findAll(Pageable pageable);
}
```

### Example 2: HashMap for Fast Lookups

**Better approach for frequent ID lookups:**
```java
@Service
public class LectureService {

    // Convert List to HashMap for O(1) lookups
    public Map<Long, Lecture> getLectureMap(List<Lecture> lectures) {
        return lectures.stream()
            .collect(Collectors.toMap(
                Lecture::getId,
                lecture -> lecture
            ));
    }

    // O(1) lookup instead of O(n)
    public Lecture findById(Map<Long, Lecture> lectureMap, Long id) {
        return lectureMap.get(id);
    }
}
```

### Example 3: Sorting Lectures

**LectureService.java:**
```java
@Service
public class LectureService {

    // Sort by play count (descending)
    public List<Lecture> getPopularLectures() {
        List<Lecture> lectures = lectureRepository.findAll();
        lectures.sort(
            Comparator.comparingInt(Lecture::getPlayCount).reversed()
        );
        return lectures;
    }

    // Sort by multiple criteria
    public List<Lecture> getSortedLectures() {
        List<Lecture> lectures = lectureRepository.findAll();
        lectures.sort(
            Comparator.comparing(Lecture::getGenre)
                      .thenComparing(Lecture::getTitle)
        );
        return lectures;
    }

    // Using Stream API
    public List<Lecture> getRecentLectures() {
        return lectureRepository.findAll().stream()
            .sorted(Comparator.comparing(Lecture::getUploadedAt).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }
}
```

### Example 4: Set for Unique Values

**Remove duplicate lectures:**
```java
public List<Lecture> getUniqueLectures(List<Lecture> lectures) {
    // LinkedHashSet maintains insertion order while removing duplicates
    Set<Lecture> uniqueSet = new LinkedHashSet<>(lectures);
    return new ArrayList<>(uniqueSet);
}

// Or using Stream
public List<Lecture> getUniqueLecturesByTitle(List<Lecture> lectures) {
    return lectures.stream()
        .collect(Collectors.toMap(
            Lecture::getTitle,
            lecture -> lecture,
            (existing, replacement) -> existing  // Keep first occurrence
        ))
        .values()
        .stream()
        .collect(Collectors.toList());
}
```

### Example 5: Queue for Processing Tasks

**Example: Process lecture uploads sequentially**
```java
@Service
public class LectureProcessingService {

    private Queue<Lecture> processingQueue = new LinkedList<>();

    public void queueForProcessing(Lecture lecture) {
        processingQueue.offer(lecture);
    }

    @Scheduled(fixedRate = 5000)  // Every 5 seconds
    public void processNextLecture() {
        Lecture lecture = processingQueue.poll();
        if (lecture != null) {
            generateWaveform(lecture);
            generateThumbnail(lecture);
            lectureRepository.save(lecture);
        }
    }
}
```

### Example 6: Pagination (Efficient for Large Datasets)

**LectureController.java:**
```java
@RestController
@RequestMapping("/api/v1/lectures")
public class LectureController {

    // Instead of loading all lectures (expensive)
    @GetMapping
    public ResponseEntity<Page<LectureDto>> getLectures(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Lecture> lecturePage = lectureRepository.findAll(pageable);

        Page<LectureDto> dtoPage = lecturePage.map(LectureDto::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }
}
```

### Example 7: Caching with HashMap

**Simple in-memory cache:**
```java
@Service
public class LectureCacheService {

    private final Map<Long, Lecture> cache = new ConcurrentHashMap<>();
    private final LectureRepository repository;

    public Lecture getLecture(Long id) {
        // O(1) cache lookup
        return cache.computeIfAbsent(id, key ->
            repository.findById(key)
                      .orElseThrow(() -> new NotFoundException("Lecture not found"))
        );
    }

    public void invalidate(Long id) {
        cache.remove(id);
    }

    public void clearCache() {
        cache.clear();
    }
}
```

---

## Summary and Quick Reference

### Data Structures Quick Guide

| Data Structure | Access | Search | Insert | Delete | Use Case |
|----------------|--------|--------|--------|--------|----------|
| **Array** | O(1) | O(n) | O(n) | O(n) | Fixed size, index access |
| **ArrayList** | O(1) | O(n) | O(1)* | O(n) | Dynamic array, random access |
| **LinkedList** | O(n) | O(n) | O(1) | O(1) | Frequent insert/delete at ends |
| **Stack** | O(n) | O(n) | O(1) | O(1) | LIFO operations |
| **Queue** | O(n) | O(n) | O(1) | O(1) | FIFO operations |
| **HashMap** | N/A | O(1)* | O(1)* | O(1)* | Key-value lookups |
| **HashSet** | N/A | O(1)* | O(1)* | O(1)* | Unique elements |
| **TreeMap** | N/A | O(log n) | O(log n) | O(log n) | Sorted key-value |
| **TreeSet** | N/A | O(log n) | O(log n) | O(log n) | Sorted unique elements |

*Amortized or average case

### Algorithm Patterns Quick Guide

| Pattern | When to Use | Example Problem |
|---------|-------------|-----------------|
| **Two Pointers** | Sorted array, palindromes | Remove duplicates, find pair sum |
| **Sliding Window** | Contiguous subarray/substring | Max sum subarray, longest substring |
| **Prefix Sum** | Multiple range sum queries | Range sum query |
| **HashMap/Frequency** | Count occurrences | First non-repeating character |
| **Binary Search** | Sorted data | Find element in sorted array |
| **BFS (Queue)** | Shortest path, level-order | Tree level order, shortest path |
| **DFS (Stack/Recursion)** | Explore all paths | Tree traversal, backtracking |
| **Recursion** | Problem can be divided | Factorial, fibonacci |
| **Backtracking** | Generate combinations | All subsets, permutations |

### When to Use What

**Need fast lookups by key?** → HashMap

**Need unique elements?** → HashSet

**Need sorted order?** → TreeMap or TreeSet

**Need to maintain insertion order?** → LinkedHashMap or ArrayList

**Process items in order received?** → Queue

**Undo/redo functionality?** → Stack

**Random access by index?** → ArrayList

**Frequent insert/delete at beginning?** → LinkedList

**Large dataset pagination?** → Page<T> with database queries

---

## Practice Problems

### Beginner

1. Reverse an array
2. Find the maximum element in an array
3. Check if array contains duplicates
4. Count occurrences of each element
5. Merge two sorted arrays

### Intermediate

1. Two Sum (find two numbers that add to target)
2. Valid Parentheses (check balanced brackets)
3. Longest Substring Without Repeating Characters
4. Binary Search implementation
5. Level Order Traversal of Binary Tree

### Advanced

1. Merge K Sorted Lists
2. LRU Cache implementation
3. Serialize and Deserialize Binary Tree
4. Find Median from Data Stream
5. Top K Frequent Elements

---

## Further Learning

### Books
- **"Cracking the Coding Interview"** by Gayle Laakmann McDowell
- **"Introduction to Algorithms"** by CLRS
- **"Data Structures and Algorithms in Java"** by Robert Lafore

### Online Resources
- LeetCode (practice problems)
- HackerRank (practice problems)
- GeeksforGeeks (tutorials and examples)
- Visualgo (algorithm visualizations)

### Related Guides in Your Project
- [Understanding JPA and Hibernate](./understanding-jpa-and-hibernate.md) - Database operations
- [Understanding Spring Boot Configuration](./understanding-spring-boot-configuration.md)
- [Understanding RESTful API Design](./understanding-restful-api-design.md)

---

**Last Updated:** November 2024

**Practice makes perfect!** Start with the basics, understand Big O, and gradually work through more complex data structures. Good luck with your learning journey! 🚀
