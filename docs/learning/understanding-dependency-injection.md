# Understanding Dependency Injection & Spring Beans

## Table of Contents
1. [What is Dependency Injection?](#what-is-dependency-injection)
2. [The Problem DI Solves](#the-problem-di-solves)
3. [What is a Spring Bean?](#what-is-a-spring-bean)
4. [The IoC Container](#the-ioc-container)
5. [Types of Dependency Injection](#types-of-dependency-injection)
6. [Spring Annotations](#spring-annotations)
7. [Your Application's DI Pattern](#your-applications-di-pattern)
8. [Constructor Injection with Lombok](#constructor-injection-with-lombok)
9. [Bean Scopes](#bean-scopes)
10. [Bean Lifecycle](#bean-lifecycle)
11. [Component Scanning](#component-scanning)
12. [Common Patterns](#common-patterns)
13. [Troubleshooting](#troubleshooting)

---

## What is Dependency Injection?

**Dependency Injection (DI)** is a design pattern where objects receive their dependencies from an external source rather than creating them themselves.

### Simple Analogy

**Without DI (Bad):**
```
You're a chef. Every time you need a knife, you:
1. Go to the blacksmith
2. Forge a new knife yourself
3. Sharpen it
4. Use it
5. Repeat for every dish!
```

**With DI (Good):**
```
You're a chef. The kitchen manager:
1. Provides you with all tools you need
2. You just use them
3. No need to create tools yourself
```

**In code:**

**Without DI:**
```java
public class LectureService {
    private LectureRepository repository;

    public LectureService() {
        // Create dependency yourself ❌
        this.repository = new LectureRepositoryImpl();
    }
}
```

**With DI:**
```java
@Service
public class LectureService {
    private final LectureRepository repository;

    // Spring provides the dependency ✅
    public LectureService(LectureRepository repository) {
        this.repository = repository;
    }
}
```

---

## The Problem DI Solves

### Problem 1: Tight Coupling

**Without DI:**
```java
public class LectureService {
    private LectureRepository repository = new JpaLectureRepository();
    // ❌ Tightly coupled to JpaLectureRepository
    // ❌ Can't easily swap implementations
    // ❌ Hard to test (can't mock the repository)
}
```

**With DI:**
```java
@Service
public class LectureService {
    private final LectureRepository repository;

    public LectureService(LectureRepository repository) {
        this.repository = repository;  // ✅ Works with any implementation!
    }
}
```

---

### Problem 2: Testing Difficulty

**Without DI:**
```java
public class LectureService {
    private LectureRepository repository = new JpaLectureRepository();

    public List<Lecture> getAllLectures() {
        return repository.findAll();  // ❌ Always hits real database!
    }
}

@Test
public void testGetAllLectures() {
    LectureService service = new LectureService();
    // ❌ Can't mock repository - test hits real database!
    service.getAllLectures();
}
```

**With DI:**
```java
@Service
public class LectureService {
    private final LectureRepository repository;

    public LectureService(LectureRepository repository) {
        this.repository = repository;
    }

    public List<Lecture> getAllLectures() {
        return repository.findAll();
    }
}

@Test
public void testGetAllLectures() {
    LectureRepository mockRepo = Mockito.mock(LectureRepository.class);
    when(mockRepo.findAll()).thenReturn(List.of(/* test data */));

    LectureService service = new LectureService(mockRepo);  // ✅ Inject mock!
    service.getAllLectures();  // ✅ Uses mock, not real database
}
```

---

### Problem 3: Configuration Complexity

**Without DI:**
```java
public class LectureService {
    public LectureService() {
        // ❌ How to switch between dev and prod database?
        // ❌ How to configure connection pool settings?
        // ❌ Hardcoded configuration!
        this.repository = new JpaLectureRepository(
            "jdbc:postgresql://localhost:5432/mydb",
            "username",
            "password"
        );
    }
}
```

**With DI:**
```java
@Service
public class LectureService {
    private final LectureRepository repository;

    // ✅ Spring handles configuration
    // ✅ Uses application.yml settings
    // ✅ Automatic dev/prod switching
    public LectureService(LectureRepository repository) {
        this.repository = repository;
    }
}
```

---

## What is a Spring Bean?

A **Bean** is simply an object that is managed by the Spring IoC (Inversion of Control) container.

### Regular Object vs Spring Bean

**Regular Object (you manage):**
```java
LectureService service = new LectureService();  // You create it
// You're responsible for dependencies
// You manage lifecycle
service = null;  // You destroy it
```

**Spring Bean (Spring manages):**
```java
@Service
public class LectureService {
    // Spring creates it
    // Spring injects dependencies
    // Spring manages lifecycle
    // Spring destroys it when app shuts down
}
```

### What Makes Something a Bean?

**Any class with one of these annotations becomes a bean:**
- `@Component` - Generic bean
- `@Service` - Business logic bean
- `@Repository` - Data access bean
- `@Controller` - Web controller bean
- `@RestController` - REST controller bean
- `@Configuration` - Configuration class

---

## The IoC Container

**IoC** = **Inversion of Control**

### What is IoC?

**Normal Control Flow (you control):**
```
You: "I need a LectureRepository"
    ↓
You: Create JpaLectureRepository
    ↓
You: Create DataSource for it
    ↓
You: Create EntityManager for it
    ↓
You: Configure connection pool
    ↓
You: ...and so on
```

**Inverted Control Flow (Spring controls):**
```
You: "I need a LectureRepository"
    ↓
Spring: "I'll handle that!"
    ↓
Spring: Creates DataSource
    ↓
Spring: Creates EntityManager
    ↓
Spring: Creates JpaLectureRepository
    ↓
Spring: Injects it into your service
    ↓
You: Use it!
```

### The Container

The **Spring IoC Container** is responsible for:
1. **Creating beans** (instantiating objects)
2. **Wiring beans** (injecting dependencies)
3. **Managing lifecycle** (initialization, destruction)
4. **Configuring beans** (using configuration files)

---

## Types of Dependency Injection

Spring supports three types of dependency injection:

### 1. Constructor Injection ⭐ RECOMMENDED

**Your application uses this!**

```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class LectureService {
    private final LectureRepository repository;  // Will be injected
    private final StorageService storageService;  // Will be injected

    // Lombok generates this constructor:
    // public LectureService(LectureRepository repository, StorageService storageService) {
    //     this.repository = repository;
    //     this.storageService = storageService;
    // }
}
```

**Why Recommended:**
- ✅ Dependencies are final (immutable)
- ✅ Clear which dependencies are required
- ✅ Easier to test (just call constructor with mocks)
- ✅ Prevents NullPointerException (can't create object without dependencies)

---

### 2. Setter Injection (Not Recommended)

```java
@Service
public class LectureService {
    private LectureRepository repository;

    @Autowired
    public void setRepository(LectureRepository repository) {
        this.repository = repository;
    }
}
```

**Problems:**
- ❌ Dependencies are mutable (can be changed later)
- ❌ Can create object without dependencies (NullPointerException risk)
- ❌ Less clear what's required

---

### 3. Field Injection (Not Recommended)

```java
@Service
public class LectureService {
    @Autowired
    private LectureRepository repository;  // Injected directly into field
}
```

**Problems:**
- ❌ Can't make field final
- ❌ Hard to test (can't inject mocks via constructor)
- ❌ Hidden dependencies (not visible in constructor)
- ❌ Requires reflection (slower)

**Note:** Field injection is popular but considered bad practice!

---

## Spring Annotations

### Bean Declaration Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Component` | Generic bean | General-purpose components |
| `@Service` | Business logic | `LectureService`, `UserService` |
| `@Repository` | Data access | `LectureRepository`, `UserRepository` |
| `@Controller` | MVC controller | HTML pages (not used in your app) |
| `@RestController` | REST API controller | `LectureController`, `UserController` |
| `@Configuration` | Configuration class | `SecurityConfig`, `ValidationConfig` |

**All of these are specializations of `@Component`!**

```
@Component (parent)
    ├─ @Service
    ├─ @Repository
    ├─ @Controller
    │   └─ @RestController
    └─ @Configuration
```

---

### Example from Your Application

**LectureService.java (lines 18-23):**
```java
@Service                        // Marks this as a Spring bean
@Transactional(readOnly = true) // Adds transaction management
@RequiredArgsConstructor        // Lombok: generates constructor
public class LectureService {
    private final LectureRepository lectureRepository;  // Dependency
}
```

**What Happens:**
1. Spring scans and finds `@Service` annotation
2. Creates a `LectureService` bean
3. Sees `LectureRepository` dependency
4. Finds `LectureRepository` bean (interface, implementation is auto-created by Spring Data JPA)
5. Injects it via constructor (generated by `@RequiredArgsConstructor`)

---

### Why Different Annotations?

**Semantics and Clarity:**

```java
@Service
public class LectureService {  // ✅ Clearly business logic
    // ...
}

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    // ✅ Clearly data access
}

@RestController
public class LectureController {  // ✅ Clearly API endpoint
    // ...
}
```

**Spring also adds specific behaviors:**
- `@Repository` adds automatic exception translation (SQL exceptions → Spring's DataAccessException)
- `@RestController` adds `@ResponseBody` automatically (returns JSON)
- `@Service` is semantic only (no special behavior)

---

## Your Application's DI Pattern

### The Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring IoC Container                      │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ LectureController (Bean)                             │  │
│  │ ─────────────────────────────────────────────────────│  │
│  │ Dependencies:                                         │  │
│  │   - LectureService         ← Injected by Spring      │  │
│  │   - StorageService         ← Injected by Spring      │  │
│  └──────────────────┬───────────────────────────────────┘  │
│                     │                                        │
│                     ↓                                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ LectureService (Bean)                                │  │
│  │ ─────────────────────────────────────────────────────│  │
│  │ Dependencies:                                         │  │
│  │   - LectureRepository      ← Injected by Spring      │  │
│  └──────────────────┬───────────────────────────────────┘  │
│                     │                                        │
│                     ↓                                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ LectureRepository (Bean - auto-created by Spring)    │  │
│  │ ─────────────────────────────────────────────────────│  │
│  │ Dependencies:                                         │  │
│  │   - EntityManager          ← Injected by Spring      │  │
│  └──────────────────┬───────────────────────────────────┘  │
│                     │                                        │
│                     ↓                                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ EntityManager (Bean - auto-configured)               │  │
│  │ ─────────────────────────────────────────────────────│  │
│  │ Dependencies:                                         │  │
│  │   - DataSource             ← Injected by Spring      │  │
│  └──────────────────┬───────────────────────────────────┘  │
│                     │                                        │
│                     ↓                                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ DataSource (Bean - configured from application.yml)  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**What Spring Does Automatically:**
1. Creates `DataSource` from `application.yml` configuration
2. Creates `EntityManager` for JPA
3. Creates `LectureRepository` (implements the interface)
4. Creates `LectureService` and injects `LectureRepository`
5. Creates `StorageService` and injects configuration
6. Creates `LectureController` and injects `LectureService` and `StorageService`

**You wrote:** Just the annotations and constructors
**Spring handles:** Everything else!

---

### Real Example from Your Code

**LectureController.java:**
```java
@RestController
@RequestMapping("/api/v1/lectures")
@RequiredArgsConstructor
public class LectureController {
    private final LectureService lectureService;      // Injected
    private final StorageService storageService;      // Injected

    @GetMapping("/{id}")
    public ResponseEntity<LectureDto> getLecture(@PathVariable Long id) {
        // Use injected services
        Lecture lecture = lectureService.getLectureById(id)
            .orElseThrow(() -> new RuntimeException("Not found"));
        LectureDto dto = LectureDto.fromEntity(lecture, storageService);
        return ResponseEntity.ok(dto);
    }
}
```

**LectureService.java:**
```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LectureService {
    private final LectureRepository lectureRepository;  // Injected

    public Optional<Lecture> getLectureById(Long id) {
        return lectureRepository.findById(id);
    }
}
```

**How It Works:**
1. Spring creates `LectureRepository` bean
2. Spring creates `LectureService` bean, injects `LectureRepository`
3. Spring creates `StorageService` bean
4. Spring creates `LectureController` bean, injects `LectureService` and `StorageService`
5. All ready to handle HTTP requests!

---

## Constructor Injection with Lombok

### The `@RequiredArgsConstructor` Pattern

**Your code uses this pattern everywhere!**

**Without Lombok:**
```java
@Service
public class LectureService {
    private final LectureRepository lectureRepository;
    private final StorageService storageService;

    // Must write constructor manually
    public LectureService(LectureRepository lectureRepository,
                         StorageService storageService) {
        this.lectureRepository = lectureRepository;
        this.storageService = storageService;
    }
}
```

**With Lombok:**
```java
@Service
@RequiredArgsConstructor  // Lombok generates the constructor!
public class LectureService {
    private final LectureRepository lectureRepository;
    private final StorageService storageService;

    // Constructor generated automatically for all final fields
}
```

**What Lombok Generates:**
```java
public LectureService(LectureRepository lectureRepository,
                     StorageService storageService) {
    this.lectureRepository = lectureRepository;
    this.storageService = storageService;
}
```

**Benefits:**
- ✅ Less boilerplate code
- ✅ Add/remove dependencies easily (just add/remove final fields)
- ✅ Constructor stays in sync with fields

---

### How Spring Knows to Use the Constructor

**Before Spring 4.3:**
```java
@Service
public class LectureService {
    private final LectureRepository lectureRepository;

    @Autowired  // Required!
    public LectureService(LectureRepository lectureRepository) {
        this.lectureRepository = lectureRepository;
    }
}
```

**Spring 4.3+ (What You Use):**
```java
@Service
@RequiredArgsConstructor
public class LectureService {
    private final LectureRepository lectureRepository;

    // @Autowired is optional if there's only one constructor!
}
```

**Rule:** If a bean has exactly one constructor, Spring automatically uses it for DI.

---

## Bean Scopes

**Bean Scope** determines how many instances of a bean Spring creates.

### 1. Singleton (Default) ⭐

**One instance per Spring container** (most common)

```java
@Service  // Singleton by default
public class LectureService {
    // Only ONE instance created for entire application
}
```

**What Happens:**
```
Application starts
    ↓
Spring creates LectureService (bean1)
    ↓
LectureController needs LectureService
    → Spring injects bean1
    ↓
FavoriteController needs LectureService
    → Spring injects bean1 (same instance!)
    ↓
All controllers share the SAME LectureService instance
```

**Why This Works:**
- Services are stateless (no instance variables that change)
- Thread-safe (each request gets its own thread, but shares service)
- Memory efficient (only one instance)

---

### 2. Prototype

**New instance every time**

```java
@Service
@Scope("prototype")  // Create new instance each time
public class ReportGenerator {
    // New instance for each injection point
}
```

**When to Use:**
- Bean has state that changes per request
- Bean is not thread-safe
- Rare in web applications!

---

### 3. Request (Web Only)

**One instance per HTTP request**

```java
@Component
@Scope(WebApplicationContext.SCOPE_REQUEST)
public class RequestContext {
    // New instance for each HTTP request
}
```

---

### 4. Session (Web Only)

**One instance per HTTP session**

```java
@Component
@Scope(WebApplicationContext.SCOPE_SESSION)
public class UserSession {
    // One instance per user session
}
```

**Note:** Your app uses JWT (stateless), so request/session scopes aren't needed!

---

## Bean Lifecycle

### What Happens When a Bean is Created?

```
1. Spring scans for @Component, @Service, etc.
    ↓
2. Creates BeanDefinition (metadata about the bean)
    ↓
3. Instantiates bean (calls constructor)
    ↓
4. Injects dependencies (via constructor, setter, or field)
    ↓
5. Calls @PostConstruct methods (if any)
    ↓
6. Bean is ready to use!
    ↓
7. Application runs...
    ↓
8. Application shuts down
    ↓
9. Calls @PreDestroy methods (if any)
    ↓
10. Destroys bean
```

### Lifecycle Hooks

**@PostConstruct (after construction):**
```java
@Service
public class LectureService {
    private final LectureRepository repository;

    @PostConstruct
    public void init() {
        logger.info("LectureService initialized!");
        // Perform initialization logic
    }
}
```

**@PreDestroy (before destruction):**
```java
@Service
public class LectureService {
    @PreDestroy
    public void cleanup() {
        logger.info("LectureService shutting down!");
        // Clean up resources
    }
}
```

---

## Component Scanning

### How Spring Finds Beans

**AudibleCloneBackendApplication.java:**
```java
@SpringBootApplication  // Contains @ComponentScan
public class AudibleCloneBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AudibleCloneBackendApplication.class, args);
    }
}
```

**@SpringBootApplication is a combination of:**
```java
@Configuration        // Configuration class
@EnableAutoConfiguration  // Auto-configure based on classpath
@ComponentScan        // Scan for @Component, @Service, etc.
```

### What Gets Scanned?

```
@ComponentScan starts from:
com.elmify.backend (package of main class)

Scans all sub-packages:
├── com.elmify.backend.controller   ← @RestController
├── com.elmify.backend.service      ← @Service
├── com.elmify.backend.repository   ← @Repository
├── com.elmify.backend.config       ← @Configuration
└── com.elmify.backend.security     ← @Component
```

**All classes with these annotations become beans:**
- `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, `@Configuration`

---

### Customizing Component Scan

**If you need to scan additional packages:**
```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.elmifynd",
    "com.example.shared"  // Additional package
})
public class AudibleCloneBackendApplication {
    // ...
}
```

**If you need to exclude packages:**
```java
@SpringBootApplication
@ComponentScan(
    basePackages = "com.elmifynd",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com.elmifynd.legacy.*"
    )
)
public class AudibleCloneBackendApplication {
    // ...
}
```

---

## Common Patterns

### 1. Service Layer Pattern

**Your application follows this pattern!**

```
Controller (handles HTTP)
    ↓ depends on
Service (business logic)
    ↓ depends on
Repository (data access)
    ↓ depends on
Database
```

**Example:**
```java
@RestController
public class LectureController {
    private final LectureService service;  // Injected

    @GetMapping("/lectures/{id}")
    public LectureDto getLecture(@PathVariable Long id) {
        return service.findById(id);  // Delegates to service
    }
}

@Service
public class LectureService {
    private final LectureRepository repository;  // Injected

    public LectureDto findById(Long id) {
        Lecture lecture = repository.findById(id).orElseThrow();
        return LectureDto.fromEntity(lecture);
    }
}

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    // Spring Data JPA implements this
}
```

---

### 2. Configuration Beans

**SecurityConfig.java:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // ... configuration
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // ... configuration
        return source;
    }
}
```

**What Happens:**
1. Spring sees `@Configuration`
2. Spring calls methods annotated with `@Bean`
3. Return values become beans in the container
4. Other beans can depend on these beans

---

### 3. Conditional Beans

**Create beans only in certain conditions:**

```java
@Configuration
public class StorageConfig {

    @Bean
    @Profile("dev")  // Only in dev profile
    public StorageService minioStorageService() {
        return new MinioStorageService();
    }

    @Bean
    @Profile("prod")  // Only in prod profile
    public StorageService r2StorageService() {
        return new R2StorageService();
    }
}
```

---

### 4. Multiple Implementations

**What if there are multiple beans of the same type?**

```java
public interface PaymentService {
    void processPayment();
}

@Service
public class StripePaymentService implements PaymentService {
    // ...
}

@Service
public class PayPalPaymentService implements PaymentService {
    // ...
}
```

**Option 1: Use @Primary**
```java
@Service
@Primary  // This one will be injected by default
public class StripePaymentService implements PaymentService {
    // ...
}
```

**Option 2: Use @Qualifier**
```java
@Service
public class OrderService {
    private final PaymentService paymentService;

    public OrderService(@Qualifier("stripePaymentService") PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

---

## Troubleshooting

### 1. NoSuchBeanDefinitionException

**Error:**
```
No qualifying bean of type 'com.elmify.backend.service.LectureService' available
```

**Causes:**
1. ❌ Missing `@Service` annotation
2. ❌ Class not in scanned package
3. ❌ Typo in class name

**Solution:**
```java
@Service  // Add this!
public class LectureService {
    // ...
}
```

---

### 2. BeanCurrentlyInCreationException (Circular Dependency)

**Error:**
```
The dependencies of some of the beans in the application context form a cycle:
┌─────┐
|  lectureService (LectureService)
↑     ↓
|  favoriteService (FavoriteService)
└─────┘
```

**Cause:**
```java
@Service
public class LectureService {
    private final FavoriteService favoriteService;  // Depends on FavoriteService
}

@Service
public class FavoriteService {
    private final LectureService lectureService;  // Depends on LectureService
}
```

**Solutions:**

**Option 1: Refactor (Best)**
```java
// Extract shared logic to a third service
@Service
public class SharedService {
    // Shared logic
}

@Service
public class LectureService {
    private final SharedService sharedService;
}

@Service
public class FavoriteService {
    private final SharedService sharedService;
}
```

**Option 2: Use @Lazy**
```java
@Service
public class LectureService {
    private final FavoriteService favoriteService;

    public LectureService(@Lazy FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }
}
```

---

### 3. UnsatisfiedDependencyException

**Error:**
```
Error creating bean with name 'lectureService':
Unsatisfied dependency expressed through field 'lectureRepository'
```

**Cause:**
```java
@Service
public class LectureService {
    @Autowired
    private LectureRepositoryImpl lectureRepository;  // Concrete class doesn't exist!
}
```

**Solution:**
```java
@Service
public class LectureService {
    private final LectureRepository lectureRepository;  // Use interface!

    public LectureService(LectureRepository lectureRepository) {
        this.lectureRepository = lectureRepository;
    }
}
```

---

## Summary

### Key Concepts

1. **Dependency Injection** = Objects receive dependencies instead of creating them
2. **Spring Bean** = Object managed by Spring IoC container
3. **IoC Container** = Spring's mechanism for creating and wiring beans
4. **Constructor Injection** = Recommended way to inject dependencies (with `@RequiredArgsConstructor`)
5. **Component Scanning** = Spring automatically finds and creates beans

### The DI Flow

```
1. Spring starts
    ↓
2. Component scanning finds @Service, @Repository, @Controller
    ↓
3. Spring creates bean definitions
    ↓
4. Spring resolves dependencies
    ↓
5. Spring creates beans in dependency order
    ↓
6. Spring injects dependencies (constructor injection)
    ↓
7. Beans are ready to use!
```

### Annotations Summary

| Annotation | Purpose |
|------------|---------|
| `@Service` | Business logic bean |
| `@Repository` | Data access bean |
| `@RestController` | REST API controller bean |
| `@Component` | Generic bean |
| `@Configuration` | Configuration class |
| `@RequiredArgsConstructor` | Lombok: generates constructor for final fields |
| `@Autowired` | Explicit dependency injection (optional for constructors) |

### Best Practices

1. ✅ Use constructor injection (not field injection)
2. ✅ Use `@RequiredArgsConstructor` with final fields
3. ✅ Keep beans stateless (singleton scope)
4. ✅ Use interfaces for dependencies (not concrete classes)
5. ✅ Follow layer pattern: Controller → Service → Repository
6. ✅ Avoid circular dependencies

### Real-World Example from Your App

```java
// 1. Repository (Data Access)
@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    // Spring Data JPA implements this
}

// 2. Service (Business Logic)
@Service
@RequiredArgsConstructor
public class LectureService {
    private final LectureRepository repository;  // Injected by Spring

    public Optional<Lecture> findById(Long id) {
        return repository.findById(id);
    }
}

// 3. Controller (HTTP Handling)
@RestController
@RequiredArgsConstructor
public class LectureController {
    private final LectureService service;  // Injected by Spring

    @GetMapping("/lectures/{id}")
    public LectureDto getLecture(@PathVariable Long id) {
        Lecture lecture = service.findById(id).orElseThrow();
        return LectureDto.fromEntity(lecture);
    }
}
```

**What Spring Does:**
1. Creates `LectureRepository` bean (Spring Data JPA magic)
2. Creates `LectureService` bean, injects `LectureRepository`
3. Creates `LectureController` bean, injects `LectureService`
4. All wired together and ready to handle requests!

---

**Next Steps:**
- Review your service classes and see how dependencies are injected
- Try creating a new service and see Spring wire it automatically
- Read about **Spring AOP** (Aspect-Oriented Programming) for advanced DI concepts

---

**Related Topics:**
- [Understanding Spring Boot Configuration & Profiles](./understanding-spring-boot-configuration.md)
- [Understanding DTOs and the DTO Pattern](./understanding-dtos-pattern.md)
- [Understanding JPA and Hibernate](./understanding-jpa-and-hibernate.md)
