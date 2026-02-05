# atoma-java: Distributed Coordination Atomic Primitives Library

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/atoma-project/atoma-java)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/tech.atoma-project/atoma-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:tech.atoma-project)

**📖 [中文文档](./README.md)**

**atoma-java** is a lightweight and high-performance distributed coordination primitives library implemented in Java. It aims to bring well-known synchronization tools from traditional concurrent programming (such as `Lock`, `Semaphore`, `CountDownLatch`) into distributed environments, helping developers build distributed systems simply and reliably.

## Introduction

In complex distributed systems, coordination and synchronization across multiple machines is a ubiquitous challenge. atoma-java provides a set of APIs similar to `java.util.concurrent` but designed specifically for distributed environments, allowing developers to solve resource contention, task synchronization, and process control problems in distributed scenarios in a familiar way.

The project's design philosophy is **API-first** and **pluggable backend**. The core API (`atoma-api`) is decoupled from specific storage implementations. Currently, it provides a MongoDB-based implementation (`atoma-storage-mongo`).

## Core Features

- **Rich Primitives Support**:
  - **Distributed Lock (`Lock`)**: Provides mutually exclusive access, ensuring only one client can access shared resources at any time.
  - **Distributed ReadWrite Lock (`ReadWriteLock`)**: Allows multiple read operations simultaneously, but write operations are mutually exclusive, suitable for read-heavy scenarios.
  - **Distributed Semaphore (`Semaphore`)**: Controls the number of concurrent accesses to shared resources.
  - **Distributed CountDown Latch (`CountDownLatch`)**: Allows one or more threads to wait for other threads to complete operations.
  - **Distributed Cyclic Barrier (`CyclicBarrier`)**: Allows a group of threads to wait for each other until all threads reach a common barrier point.
- **Pluggable Storage Backend**:
  - Core logic is separated from the storage layer.
  - Built-in MongoDB-based `CoordinationStore` implementation.
  - Developers can implement their own storage backends to adapt to different infrastructures (such as ZooKeeper, Etcd, Redis, etc.).
- **High Performance and Low Latency**:
  - Communication between client and coordination service is optimized to reduce network round trips.
  - Utilizes atomic operations of the backend storage to ensure consistency and correctness of operations in distributed environments.
- **Simple and Easy-to-use API**:
  - API design mimics `java.util.concurrent`, reducing learning costs.
  - Provides `AtomaClient` as a unified entry point for easy management of all primitives.

## Project Architecture

This project adopts a modular design with the following main modules:

- `atoma-api`: Defines all core interfaces and common exceptions for distributed primitives. This is the module that both users and implementers should depend on.
- `atoma-core`: Provides the core implementation of the Atoma client, responsible for communication with the backend coordination storage.
- `atoma-storage-mongo`: MongoDB-based storage layer implementation that implements the `CoordinationStore` interface defined in `atoma-api`.
- `atoma-benchmark`: Contains a series of JMH benchmarks for evaluating the performance of different primitives.
- `atoma-test`: Contains the project's integration and unit test suites.

## Quick Start

### 1. Prerequisites

- Java 11 or higher.
- A running MongoDB replica set/sharded cluster.

### 2. Add Dependencies

**Gradle (Kotlin DSL)**

```kotlin
// build.gradle.kts
dependencies {
  implementation("tech.atoma-project:atoma-core:1.0.0-alpha.1")
  implementation("tech.atoma-project:atoma-storage-mongo:1.0.0-alpha.1")
}
```

**Maven**

```xml
<!-- pom.xml -->
<dependencies>
  <dependency>
    <groupId>tech.atoma-project</groupId>
    <artifactId>atoma-core</artifactId>
    <version>1.0.0-alpha.1</version>
  </dependency>
  <dependency>
    <groupId>tech.atoma-project</groupId>
    <artifactId>atoma-storage-mongo</artifactId>
    <version>1.0.0-alpha.1</version>
  </dependency>
</dependencies>
```

### 3. Usage Examples

#### Distributed Mutex Lock Example:

```java
import atoma.api.Lease;
import atoma.api.lock.Lock;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import java.time.Duration;

public class DistributedLockExample {

    public static void main(String[] args) {
        // 1. Create and configure a MongoDB storage backend
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. Create Atoma client
        AtomaClient client = new AtomaClient(store);

        // 3. Obtain a lease
        Lease lease = client.grantLease(Duration.ofSeconds(30));

        // 4. Get a distributed lock instance
        // "my-critical-task" is the unique name of the lock
        Lock mutexLock = lease.getLock("my-critical-task");

        // 5. Acquire and release the lock in a try-finally block to ensure it's always released
        try {
            System.out.println("Attempting to acquire lock...");
            mutexLock.lock(); // Block until lock is acquired
            System.out.println("Lock acquired successfully, executing critical task...");

            // Simulate task execution
            Thread.sleep(10000);

            System.out.println("Task execution completed.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted");
        } finally {
            mutexLock.unlock();
            System.out.println("Lock released.");
        }

        // 6. Close client and release resources
        lease.revoke();
        client.close();
    }
}
```

#### Distributed ReadWrite Lock Example:

```java
import atoma.api.Lease;
import atoma.api.lock.Lock;
import atoma.api.lock.ReadWriteLock;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;

public class DistributedReadWriteLockExample {

    public static void main(String[] args) {
        // 1. Create and configure a MongoDB storage backend
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. Create Atoma client
        AtomaClient client = new AtomaClient(store);

        // 3. Obtain a lease
        Lease lease = client.grantLease(Duration.ofSeconds(30));

        // 4. Get a ReadWriteLock instance
        ReadWriteLock readWriteLock = lease.getReadWriteLock("my-rw-lock");

        // Acquire read lock (shared lock)
        Lock readLock = readWriteLock.readLock();

        try {
            System.out.println("Attempting to acquire read lock...");
            readLock.lock(); // Block until read lock is acquired
            System.out.println("Read lock acquired successfully, performing read operation...");

            // Simulate read operation
            Thread.sleep(5000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted");
        } finally {
            readLock.unlock();
            System.out.println("Read lock released.");
        }

        // Acquire write lock (exclusive lock)
        Lock writeLock = readWriteLock.writeLock();

        try {
            System.out.println("Attempting to acquire write lock...");
            writeLock.lock(); // Block until write lock is acquired
            System.out.println("Write lock acquired successfully, performing write operation...");

            // Simulate write operation
            Thread.sleep(5000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted");
        } finally {
            writeLock.unlock();
            System.out.println("Write lock released.");
        }

        // 5. Close client and release resources
        lease.revoke();
        client.close();
    }
}
```

#### Distributed Semaphore Example:

```java
import atoma.api.Lease;
import atoma.api.synchronizer.Semaphore;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import java.time.Duration;

public class DistributedSemaphoreExample {

    public static void main(String[] args) {
        // 1. Create and configure a MongoDB storage backend
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. Create Atoma client
        AtomaClient client = new AtomaClient(store);

        // 3. Obtain a lease
        Lease lease = client.grantLease(Duration.ofSeconds(30));

        // 4. Create a semaphore with initial permits of 5
        Semaphore semaphore = lease.getSemaphore("my-semaphore", 5);

        try {
            System.out.println("Current available permits: " + semaphore.availablePermits());

            // Acquire 2 permits
            semaphore.acquire(2);
            System.out.println("Acquired 2 permits, remaining permits: " + semaphore.availablePermits());

            // Perform operation that requires permits
            performTaskWithPermits();

            // Release 2 permits
            semaphore.release(2);
            System.out.println("Released 2 permits, remaining permits: " + semaphore.availablePermits());

            // Try to acquire all permits
            int drainedPermits = semaphore.drainPermits();
            System.out.println("Acquired " + drainedPermits + " permits");

        } finally {
            // Close the semaphore
            semaphore.close();

            // 5. Close client and release resources
            lease.revoke();
            client.close();
        }
    }

    private static void performTaskWithPermits() {
        // Simulate operation that requires permits
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### Distributed CountDownLatch Example:

```java
import atoma.api.synchronizer.CountDownLatch;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DistributedCountDownLatchExample {

    public static void main(String[] args) {
        // 1. Create and configure a MongoDB storage backend
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. Create Atoma client
        AtomaClient client = new AtomaClient(store);

        try {
            // 3. Create CountDownLatch with initial count of 3
            CountDownLatch latch = client.getCountDownLatch("my-latch", 3);

            System.out.println("Initial count: " + latch.getCount());

            // Simulate multiple worker threads completing tasks
            for (int i = 1; i <= 3; i++) {
                final int taskId = i;
                new Thread(() -> {
                    try {
                        // Simulate task execution
                        System.out.println("Task " + taskId + " started...");
                        Thread.sleep(1000 * taskId);
                        System.out.println("Task " + taskId + " completed");

                        // Task completed, decrement count
                        latch.countDown();
                        System.out.println("Current count: " + latch.getCount());

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            // Wait for all tasks to complete (count reaches 0)
            System.out.println("Waiting for all tasks to complete...");
            boolean completed = latch.await(10, TimeUnit.SECONDS);

            if (completed) {
                System.out.println("All tasks have been completed!");
            } else {
                System.out.println("Wait timed out!");
            }

            // Close the latch
            latch.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 4. Close client and release resources
            client.close();
        }
    }
}
```

#### Distributed CyclicBarrier Example:

```java
import atoma.api.Lease;
import atoma.api.synchronizer.CyclicBarrier;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DistributedCyclicBarrierExample {

    public static void main(String[] args) {
        // 1. Create and configure a MongoDB storage backend
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. Create Atoma client
        AtomaClient client = new AtomaClient(store);

        // 3. Obtain a lease
        Lease lease = client.grantLease(Duration.ofSeconds(30));

        // 4. Create CyclicBarrier requiring 3 participants
        CyclicBarrier barrier = lease.getCyclicBarrier("my-barrier", 3);

        try {
            // Create and start 3 worker threads
            for (int i = 1; i <= 3; i++) {
                final int workerId = i;
                new Thread(() -> {
                    try {
                        System.out.println("Worker " + workerId + " starting phase 1 work...");
                        // Simulate phase 1 work
                        Thread.sleep(1000 * workerId);
                        System.out.println("Worker " + workerId + " phase 1 work completed, waiting for other workers...");

                        // Wait for all workers to complete phase 1
                        barrier.await(5, TimeUnit.SECONDS);

                        System.out.println("All workers have completed phase 1, worker " + workerId + " starting phase 2 work...");
                        // Simulate phase 2 work
                        Thread.sleep(500);
                        System.out.println("Worker " + workerId + " phase 2 work completed");

                        // Can reuse the barrier
                        System.out.println("Worker " + workerId + " waiting again...");
                        barrier.await(5, TimeUnit.SECONDS);
                        System.out.println("Worker " + workerId + " all phases completed!");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            // Wait sufficient time for all threads to complete
            Thread.sleep(10000);

            // Close the barrier
            barrier.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 5. Close client and release resources
            lease.revoke();
            client.close();
        }
    }
}
```

## Performance Benchmarks

To evaluate the performance of `atoma-java` primitives, a series of benchmarks were conducted using [JMH (Java Microbenchmark Harness)](https://openjdk.java.net/projects/code-tools/jmh/).

### Test Environment

- **CPU**: Apple M2 Pro
- **Memory**: 64 GB
- **Operating System**: macOS Sonoma 14.4.1
- **Backend Storage**: MongoDB 7.0.5 single-node replica set
- **JVM**: Java HotSpot(TM) 64-Bit Server VM 17.0.10 (17.0.10+11-LTS-jvmci-23.0-b27)
- **JMH Version**: 1.36

### Performance Summary

#### Distributed Mutex Lock (MutexLock)

| Benchmark | Scenario | Concurrent Threads | Throughput | Avg. Time |
| :--- | :--- | :--- | :--- | :--- |
| `lockAndUnlock` | No contention | 1 | ≈ 114.18 ops/sec | ≈ 9.28 ms/op |
| `lockAndUnlock` | High contention | 32 | ≈ 118.22 ops/sec | ≈ 895.27 ms/op |

#### Distributed ReadWrite Lock (ReadWriteLock)

| Benchmark | Scenario | Concurrent Threads | Throughput | Avg. Time |
| :--- | :--- | :--- | :--- | :--- |
| `readLock` | Read lock | 1 | ≈ 107.71 ops/sec | ≈ 9.46 ms/op |
| `readLock` | Read lock | 32 | ≈ 1142.16 ops/sec | ≈ 29.91 ms/op |
| `writeLock` | Write lock | 1 | ≈ 107.38 ops/sec | ≈ 9.09 ms/op |
| `writeLock` | Write lock | 32 | ≈ 121.03 ops/sec | ≈ 261.29 ms/op |

#### Distributed Semaphore (Semaphore)

| Benchmark | Scenario | Concurrent Threads | Throughput | Avg. Time |
| :--- | :--- | :--- | :--- | :--- |
| `acquire/release` | Acquire/Release 1 permit | 1 | ≈ 107.27 ops/sec | ≈ 9.50 ms/op |
| `acquire/release` | Acquire/Release 1 permit | 32 | ≈ 1128.84 ops/sec | ≈ 31.12 ms/op |
| `acquire/release` | Acquire/Release all permits | 1 | ≈ 106.35 ops/sec | ≈ 20.63 ms/op |

### Results Analysis

- **No Contention Scenario**: In the ideal case of single-threaded, no resource contention, a complete lock and unlock operation averages **9-10 milliseconds**. This reflects the basic overhead of a coordination operation with the MongoDB backend.

- **High Contention Scenario**:
  - **MutexLock**: With 32 threads concurrently competing for the same lock, the total system throughput is approximately **118 ops/sec**. While total throughput improves, the average time per operation increases significantly due to thread waiting.
  - **ReadWriteLock**:
    - **Read Lock**: With 32 threads concurrently acquiring read locks, throughput is very high (approximately **1142 ops/sec**) because read locks are shared.
    - **Write Lock**: With 32 threads concurrently acquiring write locks, performance is similar to mutex locks, with throughput of approximately **121 ops/sec**.
  - **Semaphore**: With 32 threads concurrently acquiring semaphores (1 permit), performance is comparable to mutex locks, with throughput of approximately **1128 ops/sec**.

The data shows that `atoma-java` distributed primitives provide reasonable performance overhead in distributed scenarios while ensuring correct coordination guarantees. When designing systems, consider that latency will increase accordingly under high contention.

## Building from Source

1. Clone this repository:
   ```sh
   git clone https://github.com/atoma-project/atoma-java.git
   cd atoma
   ```

2. Build the project using Gradle Wrapper:
   ```sh
   ./gradlew build
   ```
   After successful build, you can find the generated JAR files in the `build/libs` directory of each module.

## How to Contribute

Community contributions are welcome! Whether it's bugs, feature suggestions, or code submissions.

1. **Fork** this repository.
2. Create a new feature branch (`git checkout -b feature/your-feature-name`).
3. Make changes and commit (`git commit -m 'Add some feature'`).
4. Push your branch to your Fork (`git push origin feature/your-feature-name`).
5. Create a **Pull Request**.

Before submitting a Pull Request, ensure your code passes all tests (`./gradlew test`).

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Documentation

- [中文文档](./README.md)