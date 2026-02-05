package atoma.test.barrier;

import atoma.api.Lease;
import atoma.api.synchronizer.CyclicBarrier;
import atoma.core.AtomaClient;
import atoma.storage.mongo.MongoCoordinationStore;
import atoma.test.BaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BenchmarkTest extends BaseTest {

  @Test
  public void testConcurrency() throws Exception {
    final String barrierId = "BARRIER-testConcurrency";
    final int parties = 32;
    final int clientCount = 32;

    CountDownLatch latch = new CountDownLatch(clientCount);
    CyclicBarrier[] barriers = new CyclicBarrier[clientCount];
    AtomaClient[] clients = new AtomaClient[clientCount];
    Lease[] leases = new Lease[clientCount];

    ScheduledExecutorService[] executors = new ScheduledExecutorService[clientCount];
    MongoCoordinationStore[] stores = new MongoCoordinationStore[clientCount];

    for (int i = 0; i < clientCount; i++) {
      final int index = i;
      stores[index] = newMongoCoordinationStore();
      executors[index] = newScheduledExecutorService();
      clients[index] = new AtomaClient(executors[index], stores[index]);
      leases[index] = clients[index].grantLease();
    }

    for (int i = 0; i < clientCount; i++) {
      final int index = i;
      new Thread(
              () -> {
                try {
                  barriers[index] = leases[index].getCyclicBarrier(barrierId, parties);
                  Assertions.assertNotNull(barriers[index]);
                } finally {
                  latch.countDown();
                }
              })
          .start();
    }

    Assertions.assertTrue(
        latch.await(5, TimeUnit.SECONDS), "All clients should initialize the barrier");

    try {
      for (int i = 0; i < clientCount; i++) {
        Assertions.assertEquals(
            parties,
            barriers[i].getParties(),
            "Parties should be consistent across all barrier instances");
      }
    } finally {
      for (int i = 0; i < clientCount; i++) {
        if (barriers[i] != null) {
          barriers[i].close();
        }
        leases[i].close();
        clients[i].close();
        executors[i].shutdownNow();
        stores[i].close();
      }
    }
  }
}
