# atoma-java: 分布式协调原子原语库

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/atoma-project/atoma-java)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/tech.atoma-project/atoma-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:tech.atoma-project)

**📖 [English Documentation](./README.en.md)**

**atoma-java** 是一个使用 Java 实现的、轻量级且高性能的分布式协调原语库。它旨在将传统并发编程中广为人知的同步工具（如 `Lock`, `Semaphore`, `CountDownLatch`）引入到分布式环境中，辅助开发者简单、可靠地构建分布式系统。

## 简介

在复杂的分布式系统中，跨多台机器的协调与同步是一个普遍存在的难题。atoma-java 提供了一套与 `java.util.concurrent` 包类似但专为分布式环境设计的 API，使得开发者可以用熟悉的方式解决分布式场景下的资源竞争、任务同步和流程控制等问题。

项目的设计哲学是**API 优先**和**可插拔后端**。核心 API (`atoma-api`) 与具体的存储实现解耦，目前提供了一个基于 **MongoDB** 的实现 (`atoma-storage-mongo`)。

## 核心特性

- **丰富的原语支持**:
  - **分布式锁 (`Lock`)**: 提供互斥访问，确保在任何时刻只有一个客户端可以访问共享资源。
  - **分布式读写锁 (`ReadWriteLock`)**:允许多个读操作同时进行，但写操作是互斥的，适用于读多写少的场景。
  - **分布式信号量 (`Semaphore`)**: 控制对共享资源的并发访问数量。
  - **分布式倒计时门闩 (`CountDownLatch`)**: 允许一个或多个线程等待其他线程完成操作。
  - **分布式循环栅栏 (`CyclicBarrier`)**: 让一组线程互相等待，直到所有线程都到达一个共同的屏障点。
- **可插拔的存储后端**:
  - 核心逻辑与存储层分离。
  - 内置基于 MongoDB 的 `CoordinationStore` 实现。
  - 开发者可以根据需要实现自己的存储后端，以适配不同的基础架构（如 ZooKeeper, Etcd, Redis 等）。
- **高性能与低延迟**:
  - 客户端与协调服务之间的通信经过优化，减少网络往返。
  - 利用后端存储的原子操作，确保分布式环境下操作的一致性和正确性。
- **简单易用的 API**:
  - API 设计模仿 `java.util.concurrent`，降低使用成本。
  - 提供 `AtomaClient` 作为统一的入口点，方便管理所有原语。

## 项目架构

本项目采用模块化设计，主要模块如下：

- `atoma-api`: 定义了所有分布式原语的核心接口和通用异常。这是用户和实现者都应依赖的模块。
- `atoma-core`: 提供了 Atoma 客户端的核心实现，负责与后端协调存储进行通信。
- `atoma-storage-mongo`: 基于 MongoDB 的存储层实现，实现了 `atoma-api` 中定义的 `CoordinationStore` 接口。
- `atoma-benchmark`: 包含一系列 JMH 基准测试，用于评估不同原语的性能。
- `atoma-test`: 包含项目的集成测试和单元测试套件。

## 快速开始

### 1. 先决条件

- Java 11 或更高版本。
- 一个正在运行的 MongoDB 副本集群/分片集群。

### 2. 添加依赖

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

### 3. 使用示例

#### 分布式互斥锁 (`MutexLock`) 的示例：

```java
import atoma.api.Lease;
import atoma.api.lock.Lock;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import java.time.Duration;

public class DistributedLockExample {

    public static void main(String[] args) {
        // 1. 创建并配置一个 MongoDB 存储后端
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. 创建 Atoma 客户端
        AtomaClient client = new AtomaClient(store);

        // 3. 获取租约
        Lease lease = client.grantLease(Duration.ofSeconds(30));

        // 4. 获取一个分布式锁实例
        // "my-critical-task" 是锁的唯一名称
        Lock mutexLock = lease.getLock("my-critical-task");

        // 5. 在 try-finally 块中获取和释放锁，确保锁一定会被释放
        try {
            System.out.println("尝试获取锁...");
            mutexLock.lock(); // 阻塞直到获取锁
            System.out.println("成功获取锁，执行关键任务...");

            // 模拟执行任务
            Thread.sleep(10000);

            System.out.println("任务执行完毕。");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("线程被中断");
        } finally {
            mutexLock.unlock();
            System.out.println("锁已释放。");
        }

        // 6. 关闭客户端，释放资源
        lease.revoke();
        client.close();
    }
}
```


#### 分布式读写锁 (`ReadWriteLock`) 的示例：

```java
import atoma.api.Lease;
import atoma.api.lock.Lock;
import atoma.api.lock.ReadWriteLock;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;

public class DistributedReadWriteLockExample {

    public static void main(String[] args) {
        // 1. 创建并配置一个 MongoDB 存储后端
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. 创建 Atoma 客户端
        AtomaClient client = new AtomaClient(store);

        // 3. 获取租约
        Lease lease = client.grantLease(Duration.ofSeconds(30));

        // 4. 获取读写锁实例
        ReadWriteLock readWriteLock = lease.getReadWriteLock("my-rw-lock");

        // 获取读锁（共享锁）
        Lock readLock = readWriteLock.readLock();

        try {
            System.out.println("尝试获取读锁...");
            readLock.lock(); // 阻塞直到获取读锁
            System.out.println("成功获取读锁，执行读操作...");

            // 模拟读操作
            Thread.sleep(5000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("线程被中断");
        } finally {
            readLock.unlock();
            System.out.println("读锁已释放。");
        }

        // 获取写锁（独占锁）
        Lock writeLock = readWriteLock.writeLock();

        try {
            System.out.println("尝试获取写锁...");
            writeLock.lock(); // 阻塞直到获取写锁
            System.out.println("成功获取写锁，执行写操作...");

            // 模拟写操作
            Thread.sleep(5000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("线程被中断");
        } finally {
            writeLock.unlock();
            System.out.println("写锁已释放。");
        }

        // 5. 关闭客户端，释放资源
        lease.revoke();
        client.close();
    }
}
```

#### 分布式信号量 (`Semaphore`) 的示例：

```java
import atoma.api.Lease;
import atoma.api.synchronizer.Semaphore;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import java.time.Duration;

public class DistributedSemaphoreExample {

    public static void main(String[] args) {
        // 1. 创建并配置一个 MongoDB 存储后端
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. 创建 Atoma 客户端
        AtomaClient client = new AtomaClient(store);

        // 3. 获取租约
        Lease lease = client.grantLease(Duration.ofSeconds(30));

        // 4. 创建信号量，初始许可数为 5
        Semaphore semaphore = lease.getSemaphore("my-semaphore", 5);

        try {
            System.out.println("当前可用许可数: " + semaphore.availablePermits());

            // 获取 2 个许可
            semaphore.acquire(2);
            System.out.println("获取 2 个许可，剩余许可数: " + semaphore.availablePermits());

            // 执行需要许可的操作
            performTaskWithPermits();

            // 释放 2 个许可
            semaphore.release(2);
            System.out.println("释放 2 个许可，剩余许可数: " + semaphore.availablePermits());

            // 尝试获取所有许可
            int drainedPermits = semaphore.drainPermits();
            System.out.println("获取了 " + drainedPermits + " 个许可");

        } finally {
            // 关闭信号量
            semaphore.close();

            // 5. 关闭客户端，释放资源
            lease.revoke();
            client.close();
        }
    }

    private static void performTaskWithPermits() {
        // 模拟需要许可的操作
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 分布式倒计时门闩 (`CountDownLatch`) 的示例：

```java
import atoma.api.synchronizer.CountDownLatch;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DistributedCountDownLatchExample {

    public static void main(String[] args) {
        // 1. 创建并配置一个 MongoDB 存储后端
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. 创建 Atoma 客户端
        AtomaClient client = new AtomaClient(store);

        try {
            // 3. 创建 CountDownLatch，初始计数为 3
            CountDownLatch latch = client.getCountDownLatch("my-latch", 3);

            System.out.println("初始计数: " + latch.getCount());

            // 模拟多个工作线程完成任务
            for (int i = 1; i <= 3; i++) {
                final int taskId = i;
                new Thread(() -> {
                    try {
                        // 模拟任务执行
                        System.out.println("任务 " + taskId + " 开始执行...");
                        Thread.sleep(1000 * taskId);
                        System.out.println("任务 " + taskId + " 完成");

                        // 任务完成，计数减一
                        latch.countDown();
                        System.out.println("当前计数: " + latch.getCount());

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            // 等待所有任务完成（计数变为 0）
            System.out.println("等待所有任务完成...");
            boolean completed = latch.await(10, TimeUnit.SECONDS);

            if (completed) {
                System.out.println("所有任务已完成！");
            } else {
                System.out.println("等待超时！");
            }

            // 关闭门闩
            latch.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 4. 关闭客户端，释放资源
            client.close();
        }
    }
}
```

#### 分布式循环栅栏 (`CyclicBarrier`) 的示例：

```java
import atoma.api.Lease;
import atoma.api.synchronizer.CyclicBarrier;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DistributedCyclicBarrierExample {

    public static void main(String[] args) {
        // 1. 创建并配置一个 MongoDB 存储后端
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "atoma_db";
        MongoCoordinationStore store = new MongoCoordinationStore(connectionString, databaseName);

        // 2. 创建 Atoma 客户端
        AtomaClient client = new AtomaClient(store);

        // 3. 获取租约
        Lease lease = client.grantLease(Duration.ofSeconds(30));

        // 4. 创建 CyclicBarrier，需要 3 个参与者
        CyclicBarrier barrier = lease.getCyclicBarrier("my-barrier", 3);

        try {
            // 创建并启动 3 个工作线程
            for (int i = 1; i <= 3; i++) {
                final int workerId = i;
                new Thread(() -> {
                    try {
                        System.out.println("工作者 " + workerId + " 开始第一阶段工作...");
                        // 模拟第一阶段工作
                        Thread.sleep(1000 * workerId);
                        System.out.println("工作者 " + workerId + " 第一阶段工作完成，等待其他工作者...");

                        // 等待所有工作者完成第一阶段
                        barrier.await(5, TimeUnit.SECONDS);

                        System.out.println("所有工作者都完成了第一阶段，工作者 " + workerId + " 开始第二阶段工作...");
                        // 模拟第二阶段工作
                        Thread.sleep(500);
                        System.out.println("工作者 " + workerId + " 第二阶段工作完成");

                        // 可以重复使用栅栏
                        System.out.println("工作者 " + workerId + " 再次等待...");
                        barrier.await(5, TimeUnit.SECONDS);
                        System.out.println("工作者 " + workerId + " 所有阶段完成！");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            // 等待足够时间让所有线程完成
            Thread.sleep(10000);

            // 关闭栅栏
            barrier.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 5. 关闭客户端，释放资源
            lease.revoke();
            client.close();
        }
    }
}
```

## 性能基准

为了评估 `atoma-java` 原语的性能，使用了 [JMH (Java Microbenchmark Harness)](https://openjdk.java.net/projects/code-tools/jmh/) 进行了一系列基准测试。

### 测试环境

- **CPU**: Apple M2 Pro
- **内存**: 64 GB
- **操作系统**: macOS Sonoma 14.4.1
- **后端存储**: MongoDB 7.0.5 单节点副本集
- **JVM**: Java HotSpot(TM) 64-Bit Server VM 17.0.10 (17.0.10+11-LTS-jvmci-23.0-b27)
- **JMH Version**: 1.36

### 性能数据摘要

#### 分布式互斥锁 (MutexLock)

| 基准测试 (Benchmark) | 场景 (Scenario) | 并发线程 (Threads) | 吞吐量 (Throughput) | 平均耗时 (Avg. Time) |
| :--- | :--- | :--- | :--- | :--- |
| `lockAndUnlock` | 无竞争 | 1 | ≈ 114.18 ops/sec | ≈ 9.28 ms/op |
| `lockAndUnlock` | 高竞争 | 32 | ≈ 118.22 ops/sec | ≈ 895.27 ms/op |

#### 分布式读写锁 (ReadWriteLock)

| 基准测试 (Benchmark) | 场景 (Scenario) | 并发线程 (Threads) | 吞吐量 (Throughput) | 平均耗时 (Avg. Time) |
| :--- | :--- | :--- | :--- | :--- |
| `readLock` | 读锁 | 1 | ≈ 107.71 ops/sec | ≈ 9.46 ms/op |
| `readLock` | 读锁 | 32 | ≈ 1142.16 ops/sec | ≈ 29.91 ms/op |
| `writeLock` | 写锁 | 1 | ≈ 107.38 ops/sec | ≈ 9.09 ms/op |
| `writeLock` | 写锁 | 32 | ≈ 121.03 ops/sec | ≈ 261.29 ms/op |

#### 分布式信号量 (Semaphore)

| 基准测试 (Benchmark) | 场景 (Scenario) | 并发线程 (Threads) | 吞吐量 (Throughput) | 平均耗时 (Avg. Time) |
| :--- | :--- | :--- | :--- | :--- |
| `acquire/release` | 获取/释放 1 个许可 | 1 | ≈ 107.27 ops/sec | ≈ 9.50 ms/op |
| `acquire/release` | 获取/释放 1 个许可 | 32 | ≈ 1128.84 ops/sec | ≈ 31.12 ms/op |
| `acquire/release` | 获取/释放所有许可 | 1 | ≈ 106.35 ops/sec | ≈ 20.63 ms/op |

### 结果分析

- **无竞争场景**: 在单线程、无资源竞争的理想情况下，一次完整的加锁和解锁操作平均耗时在 **9-10 毫秒** 之间。反映了与 MongoDB 后端进行一次协调操作的基本开销。

- **高竞争场景**:
  - **MutexLock**: 在 32 个线程并发争抢同一个锁时，系统总吞吐量约为 **118 ops/sec**。虽然总吞吐量有所提升，但由于线程等待，单次操作的平均耗时显著增加。
  - **ReadWriteLock**:
    - **读锁**: 在 32 个线程并发获取读锁时，吞吐量非常高 (约 **1142 ops/sec**)，因为读锁是共享的。
    - **写锁**: 在 32 个线程并发获取写锁时，性能表现与互斥锁类似，吞吐量约为 **121 ops/sec**。
  - **Semaphore**: 在 32 个线程并发获取信号量（许可为 1）时，性能与互斥锁相当，吞吐量约为 **1128 ops/sec**。

数据表明 `atoma-java` 的分布式原语在提供正确协调保障的同时，其性能开销在分布式场景下是合理的。在设计系统时应考虑到高竞争下延迟会相应增加。

## 从源码构建

1. 克隆本仓库:
   ```sh
   git clone https://github.com/atoma-project/atoma-java.git
   cd atoma
   ```

2. 使用 Gradle Wrapper 构建项目:
   ```sh
   ./gradlew build
   ```
   构建成功后，你可以在各个模块的 `build/libs` 目录下找到生成的 JAR 文件。

## 如何贡献

欢迎社区的贡献！无论是Bug、提出功能建议还是提交代码。

1.  **Fork** 本仓库。
2.  创建一个新的功能分支 (`git checkout -b feature/your-feature-name`)。
3.  进行修改并提交 (`git commit -m 'Add some feature'`)。
4.  将你的分支推送到你的 Fork (`git push origin feature/your-feature-name`)。
5.  创建一个 **Pull Request**。

在提交 Pull Request 之前，请确保你的代码通过了所有测试 (`./gradlew test`)。

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。
