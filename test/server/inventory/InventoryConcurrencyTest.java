package server.inventory;

import common.Category;
import common.Product;
import common.Role;
import common.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Many cashiers selling at the same moment (task 5).
 * Without the lock in BranchInventory.removeStock, two threads could both see "1 left" and both sell it.
 */
class InventoryConcurrencyTest {

    private static final int SHIRT = 1;
    private static final int STOCK = 5;
    private static final int CASHIERS = 10;
    private static final int ROUNDS = 300;

    @Test
    void tenCashiersCanNeverSellMoreThanFiveUnits() throws InterruptedException {
        for (int round = 0; round < ROUNDS; round++) {
            InMemoryInventoryRepository repository = new InMemoryInventoryRepository();
            repository.addProduct(new Product(SHIRT, "Shirt", Category.SHIRTS, 50));
            repository.findByBranch("Tel Aviv").addStock(SHIRT, STOCK);
            InventoryService service = new InventoryService(repository);

            AtomicInteger sold = new AtomicInteger();
            CountDownLatch startTogether = new CountDownLatch(1);
            List<Thread> cashiers = new ArrayList<>();
            for (int i = 0; i < CASHIERS; i++) {
                User cashier = new User("cashier" + i, Role.CASHIER, "Tel Aviv");
                Thread thread = new Thread(() -> {
                    try {
                        startTogether.await(); // wait so all threads start at the same moment
                        service.sell(cashier, SHIRT, 1);
                        sold.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        // expected for the cashiers who came too late
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                cashiers.add(thread);
                thread.start();
            }
            startTogether.countDown();
            for (Thread thread : cashiers) {
                thread.join();
            }

            assertEquals(STOCK, sold.get(), "round " + round + ": successful sales");
            assertEquals(0, repository.findByBranch("Tel Aviv").getQuantity(SHIRT), "round " + round + ": stock left");
        }
    }
}
