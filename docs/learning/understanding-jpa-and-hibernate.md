# Understanding JPA and Hibernate

## Table of Contents
1. [What is JPA?](#what-is-jpa)
2. [What is Hibernate?](#what-is-hibernate)
3. [How They Work Together](#how-they-work-together)
4. [Core Concepts](#core-concepts)
5. [Real Examples from Your Application](#real-examples-from-your-application)
6. [Common Annotations Explained](#common-annotations-explained)
7. [Repository Pattern](#repository-pattern)
8. [Transactions](#transactions)
9. [Lazy vs Eager Loading](#lazy-vs-eager-loading)
10. [Best Practices](#best-practices)

---

## What is JPA?

**JPA** stands for **Java Persistence API**. Think of it as a **set of rules** (a specification) that describes how Java objects should be saved to and retrieved from databases.

### The Problem JPA Solves

Without JPA, you would write code like this to save a user:

```java
// Without JPA - Manual SQL (tedious and error-prone)
public void saveUser(User user) {
    String sql = "INSERT INTO users (clerk_id, email, display_name, profile_image_url, is_premium) VALUES (?, ?, ?, ?, ?)";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, user.getClerkId());
        stmt.setString(2, user.getEmail());
        stmt.setString(3, user.getDisplayName());
        stmt.setString(4, user.getProfileImageUrl());
        stmt.setBoolean(5, user.isPremium());

        stmt.executeUpdate();
    } catch (SQLException e) {
        // Handle errors
    }
}
```

### With JPA - Much Simpler

```java
// With JPA - Just one line!
userRepository.save(user);
```

**JPA** handles all the SQL generation, connection management, and database operations automatically.

### Key Point
> JPA is just a **specification** (a blueprint). It doesn't actually do anything by itself. You need an **implementation** to use it.

---

## What is Hibernate?

**Hibernate** is the **implementation** of JPA. It's the actual code that makes JPA work.

### Analogy
Think of it like this:
- **JPA** = The recipe (instructions)
- **Hibernate** = The chef who follows the recipe and cooks the meal

Other implementations exist (like EclipseLink), but Hibernate is the most popular and is what Spring Boot uses by default.

### What Hibernate Does
1. **Converts Java objects to database rows** (and vice versa)
2. **Generates SQL queries** automatically
3. **Manages database connections**
4. **Handles caching** for better performance
5. **Tracks changes** to objects and updates the database

---

## How They Work Together

```
Your Java Code (Entities)
         ↓
    JPA API (Interface)
         ↓
   Hibernate (Implementation)
         ↓
    JDBC Driver
         ↓
   PostgreSQL Database
```

When you write:
```java
userRepository.save(user);
```

Here's what happens:
1. Spring Data JPA receives your request
2. Hibernate translates the `User` object into SQL
3. JDBC sends the SQL to PostgreSQL
4. PostgreSQL stores the data
5. Hibernate returns the saved user back to you

---

## Core Concepts

### 1. Entity
An **Entity** is a Java class that represents a database table.

```java
@Entity  // This annotation tells JPA: "This class is a database table"
@Table(name = "users")  // The table name in the database
public class User {
    @Id  // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
    private Long id;

    @Column(name = "clerk_id")  // Column name in database
    private String clerkId;

    private String email;  // If no @Column, uses field name as column name
}
```

**Translation:**
- `User` class → `users` table
- `id` field → `id` column (primary key)
- `clerkId` field → `clerk_id` column
- `email` field → `email` column

### 2. Object-Relational Mapping (ORM)
ORM is the **magic** that connects Java objects to database tables.

```
Java Object              Database Table
-----------              --------------
User                  →  users
├─ id                 →  id (BIGINT)
├─ clerkId            →  clerk_id (VARCHAR)
├─ email              →  email (VARCHAR)
└─ isPremium          →  is_premium (BOOLEAN)
```

### 3. Persistence Context
Think of this as Hibernate's **memory** where it keeps track of all entities it's currently managing.

```java
@Transactional
public void updateUser(String clerkId, String newEmail) {
    User user = userRepository.findByClerkId(clerkId).get();
    // User is now in the "persistence context" (Hibernate is watching it)

    user.setEmail(newEmail);
    // Hibernate notices the change!

    // At the end of the transaction, Hibernate automatically
    // executes: UPDATE users SET email = ? WHERE id = ?
    // You don't need to call save()!
}
```

---

## Real Examples from Your Application

### Example 1: User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clerk_id", unique = true, nullable = false)
    private String clerkId;

    private String email;
    private String displayName;

    @Column(name = "is_premium")
    private boolean isPremium = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdatedTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

**What This Means:**
- Creates a table called `users`
- `id` is auto-generated (starts at 1, 2, 3...)
- `clerkId` must be unique and cannot be null
- `createdAt` is set once when created and never changes
- `updatedAt` is automatically updated whenever the entity changes

### Example 2: Favorite Entity (with Relationships)

```java
@Entity
@Table(name = "favorites")
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

**What This Means:**
- Many favorites can belong to one user
- Many favorites can point to one lecture
- `user_id` column stores the foreign key to the `users` table
- `lecture_id` column stores the foreign key to the `lectures` table

**Database Representation:**
```
favorites table
+----+---------+------------+---------------------+
| id | user_id | lecture_id | created_at          |
+----+---------+------------+---------------------+
| 1  | 1       | 86         | 2025-10-06 10:00:00 |
| 2  | 1       | 92         | 2025-10-06 10:05:00 |
| 3  | 2       | 86         | 2025-10-06 10:10:00 |
+----+---------+------------+---------------------+
```

### Example 3: Lecture Entity (Bidirectional Relationship)

```java
@Entity
@Table(name = "lectures")
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Integer duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id")
    private Speaker speaker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;
}
```

**The Relationship:**
```
Collection "Spring Boot Fundamentals"
    ├─ Lecture 1: "Introduction"
    ├─ Lecture 2: "Dependency Injection"
    └─ Lecture 3: "REST APIs"

Speaker "John Doe"
    ├─ Lecture 1: "Introduction"
    ├─ Lecture 4: "Advanced Topics"
    └─ Lecture 7: "Testing"
```

---

## Common Annotations Explained

### Entity-Level Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Entity` | Marks class as a database table | `@Entity` |
| `@Table` | Specifies table name | `@Table(name = "users")` |
| `@Id` | Marks the primary key | `@Id private Long id;` |
| `@GeneratedValue` | Auto-generate ID values | `@GeneratedValue(strategy = GenerationType.IDENTITY)` |

### Field-Level Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Column` | Customize column properties | `@Column(name = "email", unique = true)` |
| `@Transient` | Exclude field from database | `@Transient private String tempData;` |
| `@CreatedDate` | Auto-set creation timestamp | `@CreatedDate private LocalDateTime createdAt;` |
| `@UpdatedTimestamp` | Auto-update modification time | `@UpdatedTimestamp private LocalDateTime updatedAt;` |

### Relationship Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@ManyToOne` | Many entities reference one | `@ManyToOne private User user;` |
| `@OneToMany` | One entity has many references | `@OneToMany private List<Favorite> favorites;` |
| `@OneToOne` | One-to-one relationship | `@OneToOne private Profile profile;` |
| `@ManyToMany` | Many-to-many relationship | `@ManyToMany private List<Tag> tags;` |
| `@JoinColumn` | Specifies foreign key column | `@JoinColumn(name = "user_id")` |

### Special Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Enumerated` | Map Java enum to database | `@Enumerated(EnumType.STRING) private Status status;` |
| `@Lob` | Large object (text/blob) | `@Lob private String description;` |
| `@Temporal` | Date/time type specification | `@Temporal(TemporalType.TIMESTAMP)` |

---

## Repository Pattern

### What is a Repository?

A **Repository** is like a **storage manager** for your entities. Instead of writing SQL, you define methods and JPA generates the queries.

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JPA automatically implements this based on the method name!
    Optional<User> findByClerkId(String clerkId);

    // This too!
    Optional<User> findByEmail(String email);

    // And this!
    List<User> findByIsPremiumTrue();
}
```

### How It Works

JPA reads the method name and generates SQL:

| Method Name | Generated SQL |
|-------------|---------------|
| `findByClerkId(String clerkId)` | `SELECT * FROM users WHERE clerk_id = ?` |
| `findByEmail(String email)` | `SELECT * FROM users WHERE email = ?` |
| `findByIsPremiumTrue()` | `SELECT * FROM users WHERE is_premium = true` |
| `findByEmailAndIsPremiumTrue(String email)` | `SELECT * FROM users WHERE email = ? AND is_premium = true` |

### Custom Queries

For complex queries, use `@Query`:

```java
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    @Query("SELECT f FROM Favorite f " +
           "LEFT JOIN FETCH f.lecture l " +
           "LEFT JOIN FETCH l.speaker " +
           "WHERE f.user = :user")
    List<Favorite> findByUserWithLecture(@Param("user") User user);
}
```

**Why JPQL (not SQL)?**
- JPQL uses **entity names** (Favorite, Lecture) not table names (favorites, lectures)
- JPQL uses **field names** (user, lecture) not column names (user_id, lecture_id)
- JPQL is **database-independent** (works with PostgreSQL, MySQL, etc.)

---

## Transactions

### What is a Transaction?

A **transaction** is a group of database operations that either **all succeed** or **all fail** together.

### Example Without Transactions (BAD)

```java
public void transferMoney(Account from, Account to, int amount) {
    from.setBalance(from.getBalance() - amount);
    accountRepository.save(from);  // ✅ Succeeds

    // 💥 Server crashes here!

    to.setBalance(to.getBalance() + amount);
    accountRepository.save(to);  // ❌ Never executes!
}
// Result: Money disappeared! 😱
```

### Example With Transactions (GOOD)

```java
@Transactional  // This makes it safe!
public void transferMoney(Account from, Account to, int amount) {
    from.setBalance(from.getBalance() - amount);
    accountRepository.save(from);

    // Even if crash happens here, EVERYTHING is rolled back!

    to.setBalance(to.getBalance() + amount);
    accountRepository.save(to);
}
// Result: Either both succeed or both fail. No money lost! ✅
```

### In Your Application

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Default: read-only (faster for queries)
public class FavoriteService {

    // This is read-only (no writes)
    public Page<Favorite> getUserFavorites(String clerkId, Pageable pageable) {
        User user = userRepository.findByClerkId(clerkId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.findByUserWithLecture(user, pageable);
    }

    // This writes to database, so it needs @Transactional
    @Transactional  // Overrides the class-level readOnly
    public Favorite addFavorite(String clerkId, Long lectureId) {
        User user = userRepository.findByClerkId(clerkId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Lecture lecture = lectureRepository.findById(lectureId)
            .orElseThrow(() -> new RuntimeException("Lecture not found"));

        Favorite favorite = new Favorite(user, lecture);
        return favoriteRepository.save(favorite);
        // If anything fails, the entire operation is rolled back
    }
}
```

---

## Lazy vs Eager Loading

### The Problem

When you load a `Favorite`, should it automatically load the related `User` and `Lecture`?

### Eager Loading (Load Everything)

```java
@ManyToOne(fetch = FetchType.EAGER)
private User user;
```

**What Happens:**
```java
Favorite favorite = favoriteRepository.findById(1L).get();
// Hibernate executes:
// SELECT * FROM favorites WHERE id = 1
// SELECT * FROM users WHERE id = <user_id>  ← Automatic!
// SELECT * FROM lectures WHERE id = <lecture_id>  ← Automatic!
```

**Pros:** Data is immediately available
**Cons:** Can load unnecessary data, slower performance

### Lazy Loading (Load When Needed) ⭐ RECOMMENDED

```java
@ManyToOne(fetch = FetchType.LAZY)
private User user;
```

**What Happens:**
```java
Favorite favorite = favoriteRepository.findById(1L).get();
// Hibernate executes:
// SELECT * FROM favorites WHERE id = 1
// User and Lecture are NOT loaded yet!

// Only when you access user, it loads:
String email = favorite.getUser().getEmail();
// NOW Hibernate executes:
// SELECT * FROM users WHERE id = <user_id>
```

**Pros:** Better performance, loads only what you need
**Cons:** Can cause issues if accessing lazy data outside a transaction

### Your Application's Strategy

```java
@Entity
public class Favorite {
    @ManyToOne(fetch = FetchType.LAZY)  // Don't load immediately
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)  // Don't load immediately
    private Lecture lecture;
}

// But when you need the data, fetch it explicitly:
@Query("SELECT f FROM Favorite f " +
       "LEFT JOIN FETCH f.lecture l " +  // Explicitly load lecture
       "LEFT JOIN FETCH l.speaker " +     // And speaker
       "WHERE f.user = :user")
List<Favorite> findByUserWithLecture(@Param("user") User user);
```

This gives you **control** over when data is loaded.

---

## Best Practices

### 1. Always Use `@Transactional(readOnly = true)` for Read Operations

```java
@Service
@Transactional(readOnly = true)  // Class-level: default for all methods
public class UserService {

    public Optional<UserDto> findByClerkId(String clerkId) {
        // This is read-only, uses class-level annotation
        return userRepository.findByClerkId(clerkId)
            .map(UserDto::fromEntity);
    }

    @Transactional  // Override for write operations
    public UserDto syncUser(UserSyncDto userSyncDto) {
        User user = userRepository.save(/* ... */);
        return UserDto.fromEntity(user);
    }
}
```

**Why?**
- Read-only transactions are **faster**
- Database can optimize read-only queries
- Prevents accidental writes

### 2. Use DTOs (Data Transfer Objects)

**Don't:**
```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).get();  // ❌ Exposes entity directly!
}
```

**Do:**
```java
@GetMapping("/users/{id}")
public UserDto getUser(@PathVariable Long id) {
    User user = userRepository.findById(id).get();
    return UserDto.fromEntity(user);  // ✅ Convert to DTO
}
```

**Why?**
- Entities can have lazy-loaded fields that cause errors when serialized to JSON
- DTOs give you control over what data is exposed
- DTOs prevent exposing sensitive internal fields

### 3. Use `Optional` Instead of Returning Null

**Don't:**
```java
public User findUser(String clerkId) {
    return userRepository.findByClerkId(clerkId).orElse(null);  // ❌ Null!
}
```

**Do:**
```java
public Optional<User> findUser(String clerkId) {
    return userRepository.findByClerkId(clerkId);  // ✅ Optional
}

// Usage:
Optional<User> user = findUser("clerk_123");
if (user.isPresent()) {
    // Use user
} else {
    // Handle not found
}

// Or:
User user = findUser("clerk_123")
    .orElseThrow(() -> new RuntimeException("User not found"));
```

### 4. Fetch Only What You Need

**Don't:**
```java
// Fetches ALL users from database!
List<User> users = userRepository.findAll();  // ❌ Could be millions!
```

**Do:**
```java
// Use pagination
Pageable pageable = PageRequest.of(0, 20);  // Page 0, size 20
Page<User> users = userRepository.findAll(pageable);  // ✅ Only 20 users
```

### 5. Use Proper Cascade Types Carefully

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites;
}
```

**What This Means:**
- When you delete a `User`, all their `Favorite` records are automatically deleted
- `orphanRemoval = true`: If you remove a favorite from the list, it's deleted from DB

**Be Careful:**
- Don't use `CascadeType.ALL` unless you really want cascade deletes
- In your app, you use `ON DELETE CASCADE` in the database schema instead

### 6. Always Handle Exceptions

**Don't:**
```java
public User getUser(String clerkId) {
    return userRepository.findByClerkId(clerkId).get();  // ❌ Can throw NoSuchElementException!
}
```

**Do:**
```java
public User getUser(String clerkId) {
    return userRepository.findByClerkId(clerkId)
        .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));  // ✅
}
```

---

## Summary

### JPA in One Sentence
> JPA is a set of rules that lets you work with databases using Java objects instead of SQL.

### Hibernate in One Sentence
> Hibernate is the engine that implements JPA and does all the heavy lifting.

### Key Takeaways

1. **Entities** = Java classes that map to database tables
2. **Repositories** = Interfaces that provide database operations without writing SQL
3. **Relationships** = `@ManyToOne`, `@OneToMany`, etc. connect entities together
4. **Transactions** = Ensure database operations succeed or fail together
5. **Lazy Loading** = Load data only when needed (better performance)
6. **DTOs** = Transfer objects that protect your entities

### The Workflow

```
1. Define Entity (Java class)
     ↓
2. Create Repository (Interface)
     ↓
3. Use in Service (Business logic)
     ↓
4. Call from Controller (API endpoint)
     ↓
5. Hibernate generates SQL
     ↓
6. Database executes SQL
     ↓
7. Hibernate converts result back to Java objects
```

---

## Next Steps

To deepen your understanding:
1. ✅ Read the **@ManyToOne Relationships** guide (already completed)
2. 📚 Experiment with creating your own entity
3. 🔍 Try writing custom queries with `@Query`
4. 🧪 Test different fetch types (LAZY vs EAGER)
5. 💡 Learn about JPA's caching mechanism

---

**Remember:** JPA and Hibernate are designed to make your life easier. Once you understand the basics, you'll rarely need to write SQL by hand!
