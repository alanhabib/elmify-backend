# Java Streams API - Complete Learning Guide

**Mastering functional-style operations on collections of data**

---

## 📚 Table of Contents

1. [Introduction to Streams](#introduction)
2. [Stream Fundamentals](#fundamentals)
3. [Intermediate Operations](#intermediate-operations)
4. [Terminal Operations](#terminal-operations)
5. [Collectors](#collectors)
6. [Advanced Patterns](#advanced-patterns)
7. [Exercises Based on Elmify Codebase](#exercises)
8. [Refactoring Opportunities](#refactoring-opportunities)
9. [Best Practices](#best-practices)
10. [Common Pitfalls](#common-pitfalls)

---

## 🎯 Introduction

### What is a Stream?

A **Stream** is a sequence of elements supporting sequential and parallel aggregate operations. It's NOT a data structure but a **pipeline** for processing data.

**Key characteristics:**
- **No storage** - Streams don't store elements
- **Functional** - Operations produce results without modifying the source
- **Lazy evaluation** - Computation happens only when needed
- **Possibly unbounded** - Can process infinite sequences
- **Consumable** - Can only be used once

### Why Use Streams?

```java
// ❌ Imperative style (how to do it)
List<String> names = new ArrayList<>();
for (Lecture lecture : lectures) {
    if (lecture.getDuration() > 1800) {  // > 30 minutes
        names.add(lecture.getTitle());
    }
}
Collections.sort(names);

// ✅ Declarative style (what to do)
List<String> names = lectures.stream()
    .filter(lecture -> lecture.getDuration() > 1800)
    .map(Lecture::getTitle)
    .sorted()
    .collect(Collectors.toList());
```

**Benefits:**
- More readable and concise
- Easier to parallelize
- Less error-prone (no manual iteration)
- Composable operations

---

## 🔧 Stream Fundamentals

### Creating Streams

```java
// From collection
List<Lecture> lectures = getLectures();
Stream<Lecture> stream1 = lectures.stream();

// From array
String[] speakers = {"Jordan Peterson", "Naval Ravikant"};
Stream<String> stream2 = Arrays.stream(speakers);

// From individual values
Stream<Integer> stream3 = Stream.of(1, 2, 3, 4, 5);

// Empty stream
Stream<String> empty = Stream.empty();

// Infinite streams
Stream<Integer> infinite = Stream.iterate(0, n -> n + 1);
Stream<Double> random = Stream.generate(Math::random);

// From range
IntStream range = IntStream.range(1, 100);  // 1 to 99
```

### Stream Pipeline Structure

```
Source → Intermediate Operations → Terminal Operation → Result
  ↓              ↓                         ↓
List      filter, map, sorted        collect, forEach
```

**Example from Elmify:**
```java
// From StatsService.java
int totalSecondsToday = todayStats.stream()      // Source
    .mapToInt(ListeningStats::getTotalPlayTime)  // Intermediate
    .sum();                                       // Terminal
```

---

## 🔄 Intermediate Operations

Intermediate operations return a **new Stream** and are **lazy** (not executed until terminal operation).

### 1. filter()

**Purpose:** Filter elements based on a predicate

```java
// Basic filtering
lectures.stream()
    .filter(lecture -> lecture.getDuration() > 3600)  // Lectures longer than 1 hour
    .collect(Collectors.toList());

// Multiple filters
lectures.stream()
    .filter(lecture -> lecture.getIsPublic())
    .filter(lecture -> lecture.getPlayCount() > 100)
    .filter(lecture -> lecture.getFileFormat().equals("mp3"))
    .collect(Collectors.toList());

// Combining conditions (more efficient)
lectures.stream()
    .filter(lecture -> lecture.getIsPublic()
        && lecture.getPlayCount() > 100
        && lecture.getFileFormat().equals("mp3"))
    .collect(Collectors.toList());
```

**From Elmify codebase:**
```java
// Find all lectures from a specific speaker
lectures.stream()
    .filter(lecture -> lecture.getSpeaker().getName().equals("Jordan Peterson"))
    .collect(Collectors.toList());
```

### 2. map()

**Purpose:** Transform each element to another type

```java
// Extract titles
List<String> titles = lectures.stream()
    .map(Lecture::getTitle)  // Method reference
    .collect(Collectors.toList());

// Transform to DTO
List<LectureDto> dtos = lectures.stream()
    .map(lecture -> new LectureDto(
        lecture.getId(),
        lecture.getTitle(),
        lecture.getDuration()
    ))
    .collect(Collectors.toList());

// Chain transformations
List<String> upperTitles = lectures.stream()
    .map(Lecture::getTitle)
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

### 3. flatMap()

**Purpose:** Flatten nested structures (Stream of Streams → Single Stream)

```java
// Get all lectures from all collections
List<Lecture> allLectures = speakers.stream()
    .map(Speaker::getCollections)           // Stream<List<Collection>>
    .flatMap(List::stream)                  // Stream<Collection>
    .map(Collection::getLectures)           // Stream<List<Lecture>>
    .flatMap(List::stream)                  // Stream<Lecture>
    .collect(Collectors.toList());

// Flattening Optional
List<Lecture> lecturesWithSpeaker = lectureIds.stream()
    .map(id -> lectureRepository.findById(id))  // Stream<Optional<Lecture>>
    .flatMap(Optional::stream)                   // Stream<Lecture> (filters empty)
    .collect(Collectors.toList());
```

### 4. sorted()

**Purpose:** Sort elements

```java
// Natural ordering
lectures.stream()
    .sorted()  // Requires Comparable
    .collect(Collectors.toList());

// Custom comparator
lectures.stream()
    .sorted(Comparator.comparing(Lecture::getDuration))
    .collect(Collectors.toList());

// Reverse order
lectures.stream()
    .sorted(Comparator.comparing(Lecture::getPlayCount).reversed())
    .collect(Collectors.toList());

// Multiple sorting criteria
lectures.stream()
    .sorted(Comparator.comparing(Lecture::getSpeaker)
        .thenComparing(Lecture::getTitle))
    .collect(Collectors.toList());
```

### 5. distinct()

**Purpose:** Remove duplicates (uses equals/hashCode)

```java
// Unique file formats
List<String> uniqueFormats = lectures.stream()
    .map(Lecture::getFileFormat)
    .distinct()
    .collect(Collectors.toList());

// Unique speakers (by object equality)
List<Speaker> uniqueSpeakers = lectures.stream()
    .map(Lecture::getSpeaker)
    .distinct()
    .collect(Collectors.toList());
```

### 6. limit() and skip()

**Purpose:** Truncate or skip elements

```java
// Top 10 most played lectures
List<Lecture> top10 = lectures.stream()
    .sorted(Comparator.comparing(Lecture::getPlayCount).reversed())
    .limit(10)
    .collect(Collectors.toList());

// Pagination (skip first 20, take 10)
List<Lecture> page3 = lectures.stream()
    .skip(20)
    .limit(10)
    .collect(Collectors.toList());
```

### 7. peek()

**Purpose:** Perform side-effect without modifying stream (debugging)

```java
// Debugging stream pipeline
List<String> result = lectures.stream()
    .filter(lecture -> lecture.getDuration() > 1800)
    .peek(lecture -> System.out.println("After filter: " + lecture.getTitle()))
    .map(Lecture::getTitle)
    .peek(title -> System.out.println("After map: " + title))
    .collect(Collectors.toList());
```

---

## 🎯 Terminal Operations

Terminal operations **trigger computation** and **consume the stream**.

### 1. collect()

**Purpose:** Accumulate elements into a collection or other result

```java
// To List
List<Lecture> list = stream.collect(Collectors.toList());

// To Set (removes duplicates)
Set<String> uniqueTitles = lectures.stream()
    .map(Lecture::getTitle)
    .collect(Collectors.toSet());

// To Map
Map<Long, Lecture> lectureMap = lectures.stream()
    .collect(Collectors.toMap(
        Lecture::getId,      // Key
        lecture -> lecture   // Value
    ));

// Joining strings
String allTitles = lectures.stream()
    .map(Lecture::getTitle)
    .collect(Collectors.joining(", "));
```

### 2. forEach()

**Purpose:** Perform action on each element

```java
// Print all titles
lectures.stream()
    .forEach(lecture -> System.out.println(lecture.getTitle()));

// Method reference
lectures.stream()
    .map(Lecture::getTitle)
    .forEach(System.out::println);

// ⚠️ Don't modify external state!
// ❌ Bad:
List<String> titles = new ArrayList<>();
lectures.stream().forEach(lecture -> titles.add(lecture.getTitle()));

// ✅ Good:
List<String> titles = lectures.stream()
    .map(Lecture::getTitle)
    .collect(Collectors.toList());
```

### 3. Reduction Operations

**reduce()** - Combine elements to produce a single result

```java
// Sum of all durations
int totalDuration = lectures.stream()
    .map(Lecture::getDuration)
    .reduce(0, Integer::sum);

// Same as:
int totalDuration = lectures.stream()
    .mapToInt(Lecture::getDuration)
    .sum();

// Find longest lecture
Optional<Lecture> longest = lectures.stream()
    .reduce((l1, l2) -> l1.getDuration() > l2.getDuration() ? l1 : l2);

// Concatenate all titles
String allTitles = lectures.stream()
    .map(Lecture::getTitle)
    .reduce("", (s1, s2) -> s1 + ", " + s2);
```

### 4. Finding and Matching

```java
// Check if any lecture is longer than 2 hours
boolean hasLongLecture = lectures.stream()
    .anyMatch(lecture -> lecture.getDuration() > 7200);

// Check if all lectures are public
boolean allPublic = lectures.stream()
    .allMatch(Lecture::getIsPublic);

// Check if no lecture is corrupted
boolean noneCorrupted = lectures.stream()
    .noneMatch(lecture -> lecture.getFileSize() == 0);

// Find any lecture from speaker
Optional<Lecture> anyLecture = lectures.stream()
    .filter(lecture -> lecture.getSpeaker().getName().equals("Naval"))
    .findAny();

// Find first lecture in sorted order
Optional<Lecture> first = lectures.stream()
    .sorted(Comparator.comparing(Lecture::getTitle))
    .findFirst();
```

### 5. Counting and Statistics

```java
// Count lectures
long count = lectures.stream()
    .filter(lecture -> lecture.getPlayCount() > 100)
    .count();

// Statistics with primitive streams
IntSummaryStatistics stats = lectures.stream()
    .mapToInt(Lecture::getDuration)
    .summaryStatistics();

System.out.println("Min: " + stats.getMin());
System.out.println("Max: " + stats.getMax());
System.out.println("Average: " + stats.getAverage());
System.out.println("Sum: " + stats.getSum());
System.out.println("Count: " + stats.getCount());
```

---

## 🗂️ Collectors

Collectors are powerful terminal operations for complex accumulations.

### Grouping

```java
// Group lectures by speaker
Map<Speaker, List<Lecture>> bySpeaker = lectures.stream()
    .collect(Collectors.groupingBy(Lecture::getSpeaker));

// Group by file format
Map<String, List<Lecture>> byFormat = lectures.stream()
    .collect(Collectors.groupingBy(Lecture::getFileFormat));

// Count lectures per speaker
Map<Speaker, Long> countBySpeaker = lectures.stream()
    .collect(Collectors.groupingBy(
        Lecture::getSpeaker,
        Collectors.counting()
    ));

// Sum duration per speaker
Map<Speaker, Integer> durationBySpeaker = lectures.stream()
    .collect(Collectors.groupingBy(
        Lecture::getSpeaker,
        Collectors.summingInt(Lecture::getDuration)
    ));

// Multiple grouping levels
Map<Speaker, Map<String, List<Lecture>>> byS peakerAndFormat = lectures.stream()
    .collect(Collectors.groupingBy(
        Lecture::getSpeaker,
        Collectors.groupingBy(Lecture::getFileFormat)
    ));
```

### Partitioning

```java
// Partition by condition (boolean key)
Map<Boolean, List<Lecture>> publicPrivate = lectures.stream()
    .collect(Collectors.partitioningBy(Lecture::getIsPublic));

List<Lecture> publicLectures = publicPrivate.get(true);
List<Lecture> privateLectures = publicPrivate.get(false);

// With downstream collector
Map<Boolean, Long> countByPublic = lectures.stream()
    .collect(Collectors.partitioningBy(
        Lecture::getIsPublic,
        Collectors.counting()
    ));
```

### Advanced Collectors

```java
// Collect to specific collection type
TreeSet<String> sortedTitles = lectures.stream()
    .map(Lecture::getTitle)
    .collect(Collectors.toCollection(TreeSet::new));

// Custom collector with mapping
List<String> upperTitles = lectures.stream()
    .collect(Collectors.mapping(
        Lecture::getTitle,
        Collectors.mapping(
            String::toUpperCase,
            Collectors.toList()
        )
    ));

// Summarizing statistics
DoubleSummaryStatistics durationStats = lectures.stream()
    .collect(Collectors.summarizingDouble(Lecture::getDuration));
```

---

## 🚀 Advanced Patterns

### 1. Optional Integration

```java
// Avoid null checks
Optional<Lecture> lecture = lectureRepository.findById(id);

String title = lecture
    .map(Lecture::getTitle)
    .orElse("Unknown");

// Chain optionals
String speakerName = lecture
    .map(Lecture::getSpeaker)
    .map(Speaker::getName)
    .orElse("Unknown Speaker");

// flatMap for nested optionals
Optional<String> collectionTitle = lecture
    .map(Lecture::getCollection)
    .flatMap(collection -> Optional.ofNullable(collection.getTitle()));
```

### 2. Parallel Streams

```java
// Sequential
long count = lectures.stream()
    .filter(lecture -> lecture.getDuration() > 3600)
    .count();

// Parallel (auto-splits work across threads)
long count = lectures.parallelStream()
    .filter(lecture -> lecture.getDuration() > 3600)
    .count();

// ⚠️ Use with caution!
// - Only for CPU-intensive operations
// - Overhead for small datasets
// - Thread-safety required
```

### 3. Custom Collectors

```java
// Example: Collect to comma-separated string with custom format
String formatted = lectures.stream()
    .collect(Collector.of(
        StringBuilder::new,                          // Supplier
        (sb, l) -> sb.append(l.getTitle()).append(", "),  // Accumulator
        StringBuilder::append,                       // Combiner
        sb -> sb.toString()                         // Finisher
    ));
```

### 4. Stream Composition

```java
// Combine multiple streams
Stream<Lecture> stream1 = collection1.getLectures().stream();
Stream<Lecture> stream2 = collection2.getLectures().stream();

List<Lecture> combined = Stream.concat(stream1, stream2)
    .distinct()
    .sorted(Comparator.comparing(Lecture::getTitle))
    .collect(Collectors.toList());
```

---

## 💪 Exercises Based on Elmify Codebase

### Exercise 1: Basic Filtering and Mapping

**Location:** `StatsService.java:52-55`

**Current code:**
```java
List<ListeningStats> todayStats = listeningStatsRepository.findByUserAndDate(user, today);
int totalSecondsToday = todayStats.stream()
    .mapToInt(ListeningStats::getTotalPlayTime)
    .sum();
```

**Tasks:**
1. Extract all lecture IDs from `todayStats`
2. Get unique lecture IDs (no duplicates)
3. Filter only stats where play time > 300 seconds
4. Calculate average play time per lecture

**Solution template:**
```java
// Task 1: Extract all lecture IDs
List<Long> lectureIds = todayStats.stream()
    // YOUR CODE HERE
    .collect(Collectors.toList());

// Task 2: Get unique lecture IDs
Set<Long> uniqueLectureIds = todayStats.stream()
    // YOUR CODE HERE
    .collect(Collectors.toSet());

// Task 3: Filter play time > 300 seconds
List<ListeningStats> longListens = todayStats.stream()
    // YOUR CODE HERE
    .collect(Collectors.toList());

// Task 4: Calculate average play time
OptionalDouble average = todayStats.stream()
    // YOUR CODE HERE
    .average();
```

---

### Exercise 2: Grouping and Aggregation

**Location:** `StatsService.java:117-121`

**Current code:**
```java
Map<LocalDate, Integer> dailyTotals = weekStats.stream()
    .collect(Collectors.groupingBy(
        ListeningStats::getDate,
        Collectors.summingInt(ListeningStats::getTotalPlayTime)
    ));
```

**Tasks:**
1. Group stats by lecture ID and count how many times each was played
2. Find the lecture with highest total play time
3. Group by date and calculate average completion rate per day
4. Create a map of date → list of lecture titles

**Solution template:**
```java
// Task 1: Count plays per lecture
Map<Long, Long> playCountPerLecture = weekStats.stream()
    // YOUR CODE HERE
    .collect(Collectors.groupingBy(/* ... */));

// Task 2: Lecture with highest total play time
Optional<ListeningStats> mostPlayed = weekStats.stream()
    // YOUR CODE HERE
    .max(/* ... */);

// Task 3: Average completion rate per day
Map<LocalDate, Double> avgCompletionByDay = weekStats.stream()
    // YOUR CODE HERE
    .collect(Collectors.groupingBy(/* ... */));

// Task 4: Date → Lecture titles
Map<LocalDate, List<String>> titlesByDate = weekStats.stream()
    // YOUR CODE HERE
    .collect(Collectors.groupingBy(/* ... */));
```

---

### Exercise 3: Streak Calculation Refactoring

**Location:** `StatsService.java:256-285`

**Current code (imperative):**
```java
private int calculateBestStreak(List<LocalDate> goalMetDates) {
    if (goalMetDates.isEmpty()) {
        return 0;
    }

    List<LocalDate> sortedDates = new ArrayList<>(goalMetDates);
    Collections.sort(sortedDates, Collections.reverseOrder());

    int maxStreak = 1;
    int currentStreakCount = 1;
    LocalDate previousDate = sortedDates.get(0);

    for (int i = 1; i < sortedDates.size(); i++) {
        LocalDate currentDate = sortedDates.get(i);

        if (previousDate.minusDays(1).equals(currentDate)) {
            currentStreakCount++;
            maxStreak = Math.max(maxStreak, currentStreakCount);
        } else {
            currentStreakCount = 1;
        }

        previousDate = currentDate;
    }

    return maxStreak;
}
```

**Task:** Rewrite using streams (challenge: this is complex!)

**Hints:**
- Use `reduce()` to accumulate state
- Consider using a custom accumulator class
- Think about window operations

**Solution template:**
```java
private int calculateBestStreak(List<LocalDate> goalMetDates) {
    if (goalMetDates.isEmpty()) {
        return 0;
    }

    // Sort in descending order
    List<LocalDate> sortedDates = goalMetDates.stream()
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());

    // YOUR STREAM-BASED SOLUTION HERE
    // Hint: You might need a helper class to track state
    // Or consider using IntStream with range
}
```

---

### Exercise 4: Lecture Entity Helper Methods

**Location:** `Lecture.java:143-156`

**Current code (imperative):**
```java
public boolean isAudioFile() {
    if (fileFormat == null) return false;
    switch (fileFormat.toLowerCase()) {
        case "mp3":
        case "wav":
        case "flac":
        case "aac":
        case "ogg":
        case "m4a":
            return true;
        default:
            return false;
    }
}
```

**Task:** Rewrite using streams

**Solution template:**
```java
private static final Set<String> AUDIO_FORMATS = Set.of(
    "mp3", "wav", "flac", "aac", "ogg", "m4a"
);

public boolean isAudioFile() {
    // YOUR STREAM-BASED SOLUTION HERE
    // Hint: Use Optional and stream operations
}
```

---

### Exercise 5: Create New Service Method

**Location:** Create in `LectureService.java`

**Task:** Add a method to get lecture statistics

```java
/**
 * Get statistics for a collection of lectures
 *
 * @param lectureIds List of lecture IDs
 * @return Statistics including total duration, average play count, etc.
 */
public LectureStatisticsDto getLectureStatistics(List<Long> lectureIds) {
    List<Lecture> lectures = lectureRepository.findAllById(lectureIds);

    // Task 1: Calculate total duration in hours
    double totalHours = /* YOUR CODE */;

    // Task 2: Calculate average play count
    double avgPlayCount = /* YOUR CODE */;

    // Task 3: Find most common file format
    String mostCommonFormat = /* YOUR CODE */;

    // Task 4: Group by speaker and count
    Map<String, Long> lecturesBySpeaker = /* YOUR CODE */;

    // Task 5: Get top 5 most played lectures
    List<String> top5Titles = /* YOUR CODE */;

    return new LectureStatisticsDto(
        totalHours,
        avgPlayCount,
        mostCommonFormat,
        lecturesBySpeaker,
        top5Titles
    );
}
```

---

### Exercise 6: Complex Collection Processing

**Location:** Create new service class `ReportService.java`

**Task:** Generate a weekly listening report

```java
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ListeningStatsRepository listeningStatsRepository;

    /**
     * Generate weekly report for user
     *
     * Requirements:
     * 1. Group stats by speaker
     * 2. Calculate total time per speaker
     * 3. Find favorite genre (most listened)
     * 4. List top 10 lectures by play time
     * 5. Calculate listening velocity (minutes per day)
     */
    public WeeklyReportDto generateWeeklyReport(String clerkId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<ListeningStats> weekStats = listeningStatsRepository
            .findByUserAndDateRange(user, weekAgo, today);

        // Task 1: Group by speaker name and sum play time
        Map<String, Integer> timePerSpeaker = weekStats.stream()
            // YOUR CODE HERE
            .collect(/* ... */);

        // Task 2: Find favorite genre
        String favoriteGenre = weekStats.stream()
            // YOUR CODE HERE
            // Hint: group by genre, count, find max
            .collect(/* ... */);

        // Task 3: Top 10 lectures by total play time
        List<String> top10Lectures = weekStats.stream()
            // YOUR CODE HERE
            .collect(/* ... */);

        // Task 4: Calculate daily average
        double minutesPerDay = weekStats.stream()
            // YOUR CODE HERE
            / 7.0;

        // Task 5: Count unique lectures listened
        long uniqueLectures = weekStats.stream()
            // YOUR CODE HERE
            .count();

        return new WeeklyReportDto(/* ... */);
    }
}
```

---

## 🔨 Refactoring Opportunities

### Opportunity 1: FavoriteService Duplicate Code

**Location:** `FavoriteService.java` - getUserByClerkId pattern

**Current:** Repeated code in multiple methods
```java
User user = userRepository.findByClerkId(clerkId)
    .orElseThrow(() -> new RuntimeException("User not found with clerkId: " + clerkId));
```

**Refactor:** Extract to private method with Stream-based validation
```java
private User getUserByClerkId(String clerkId) {
    return Optional.ofNullable(clerkId)
        .filter(id -> !id.isBlank())
        .flatMap(userRepository::findByClerkId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + clerkId));
}
```

---

### Opportunity 2: Lecture Helper Methods

**Location:** `Lecture.java:159-173`

**Current:** Imperative formatting logic

**Refactor:** Use streams and functional approach
```java
private static final List<Long> SIZE_UNITS = List.of(1L, 1024L, 1024*1024L, 1024L*1024*1024);
private static final List<String> SIZE_LABELS = List.of("bytes", "KB", "MB", "GB");

public String getFormattedSize() {
    return Optional.ofNullable(fileSize)
        .map(size -> IntStream.range(0, SIZE_UNITS.size())
            .boxed()
            .sorted(Comparator.reverseOrder())
            .filter(i -> size >= SIZE_UNITS.get(i))
            .findFirst()
            .map(i -> (size / SIZE_UNITS.get(i)) + " " + SIZE_LABELS.get(i))
            .orElse(size + " bytes"))
        .orElse("Unknown Size");
}
```

---

### Opportunity 3: StatsService Date Range Processing

**Location:** `StatsService.java:124-132`

**Current:** Loop to create date map

**Refactor:** Use Stream.iterate
```java
Map<LocalDate, DayProgressDto> weekMap = Stream.iterate(weekAgo, date -> date.plusDays(1))
    .limit(7)
    .collect(Collectors.toMap(
        date -> date,
        date -> {
            int totalSeconds = dailyTotals.getOrDefault(date, 0);
            int minutes = totalSeconds / 60;
            boolean goalMet = minutes >= dailyGoalMinutes;
            return new DayProgressDto(minutes, goalMet);
        },
        (v1, v2) -> v1,
        LinkedHashMap::new
    ));
```

---

## ✅ Best Practices

### 1. Use Method References When Possible

```java
// ❌ Less readable
lectures.stream()
    .map(lecture -> lecture.getTitle())
    .forEach(title -> System.out.println(title));

// ✅ More readable
lectures.stream()
    .map(Lecture::getTitle)
    .forEach(System.out::println);
```

### 2. Avoid Side Effects in Streams

```java
// ❌ Bad: Modifying external state
List<String> titles = new ArrayList<>();
lectures.stream().forEach(lecture -> titles.add(lecture.getTitle()));

// ✅ Good: Collect results
List<String> titles = lectures.stream()
    .map(Lecture::getTitle)
    .collect(Collectors.toList());
```

### 3. Use Primitive Streams for Performance

```java
// ❌ Boxing overhead
int sum = lectures.stream()
    .map(Lecture::getDuration)  // Stream<Integer>
    .reduce(0, Integer::sum);

// ✅ No boxing
int sum = lectures.stream()
    .mapToInt(Lecture::getDuration)  // IntStream
    .sum();
```

### 4. Close Streams When Using Resources

```java
// ✅ Auto-close with try-with-resources
try (Stream<String> lines = Files.lines(Paths.get("file.txt"))) {
    long count = lines
        .filter(line -> line.contains("error"))
        .count();
}
```

### 5. Don't Reuse Streams

```java
// ❌ Stream already consumed
Stream<Lecture> stream = lectures.stream();
stream.findFirst();
stream.count();  // IllegalStateException!

// ✅ Create new stream
lectures.stream().findFirst();
lectures.stream().count();
```

---

## ⚠️ Common Pitfalls

### 1. Modifying Source During Stream

```java
// ❌ ConcurrentModificationException
List<Lecture> lectures = new ArrayList<>(originalLectures);
lectures.stream()
    .forEach(lecture -> {
        if (lecture.getDuration() < 60) {
            lectures.remove(lecture);  // BOOM!
        }
    });

// ✅ Filter and collect new list
List<Lecture> filtered = lectures.stream()
    .filter(lecture -> lecture.getDuration() >= 60)
    .collect(Collectors.toList());
```

### 2. Parallel Stream Thread Safety

```java
// ❌ Not thread-safe
List<Lecture> result = new ArrayList<>();
lectures.parallelStream()
    .forEach(result::add);  // Race condition!

// ✅ Use collect
List<Lecture> result = lectures.parallelStream()
    .collect(Collectors.toList());
```

### 3. Null Values in Streams

```java
// ❌ NullPointerException
lectures.stream()
    .map(Lecture::getSpeaker)  // Might be null
    .map(Speaker::getName)      // NPE here!
    .collect(Collectors.toList());

// ✅ Handle nulls
lectures.stream()
    .map(Lecture::getSpeaker)
    .filter(Objects::nonNull)
    .map(Speaker::getName)
    .collect(Collectors.toList());

// ✅✅ Even better with Optional
lectures.stream()
    .map(Lecture::getSpeaker)
    .flatMap(speaker -> Optional.ofNullable(speaker).stream())
    .map(Speaker::getName)
    .collect(Collectors.toList());
```

### 4. Overusing Parallel Streams

```java
// ❌ Overhead for small lists
List<String> titles = lectures.stream()  // Only 5 items
    .parallel()  // Unnecessary!
    .map(Lecture::getTitle)
    .collect(Collectors.toList());

// ✅ Use parallel only for large datasets and CPU-intensive work
// Rule of thumb: 1000+ elements, CPU-bound operations
```

---

## 🎓 Summary

### When to Use Streams

✅ **Use streams when:**
- Processing collections with multiple transformations
- Filtering and mapping data
- Grouping and aggregating
- Readability improves over loops
- Operations are stateless and independent

❌ **Avoid streams when:**
- Simple iteration is clearer
- Early termination needed (use loop with break)
- Performance is critical and boxing overhead matters
- Debugging complex pipelines is difficult

### Key Takeaways

1. **Streams are pipelines, not data structures**
2. **Intermediate operations are lazy, terminal operations trigger execution**
3. **Streams can only be used once**
4. **Prefer method references over lambdas**
5. **Use primitive streams (IntStream, LongStream, DoubleStream) for performance**
6. **Avoid side effects and state mutations**
7. **Handle nulls explicitly**
8. **Parallel streams need thread-safe operations**

---

## 📚 Additional Resources

- **Java Stream API Docs:** https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html
- **Baeldung Stream Guide:** https://www.baeldung.com/java-8-streams
- **Effective Java (Item 45-48):** Stream best practices

---

**Now go practice! Complete the exercises and refactor opportunities to master streams in your Elmify codebase!** 🚀
