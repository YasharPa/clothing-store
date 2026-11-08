package server;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggedInUsersTest {

    @Test
    void firstLoginWorksAndSecondIsRefused() {
        LoggedInUsers users = new LoggedInUsers();

        assertTrue(users.logIn("dana"));
        assertFalse(users.logIn("dana"));
        assertTrue(users.isLoggedIn("dana"));
    }

    @Test
    void afterLoggingOutTheUserCanLogInAgain() {
        LoggedInUsers users = new LoggedInUsers();
        users.logIn("dana");

        users.logOut("dana");

        assertFalse(users.isLoggedIn("dana"));
        assertTrue(users.logIn("dana"));
    }

    @Test
    void loggingOutSomeoneWhoIsNotLoggedInDoesNothing() {
        LoggedInUsers users = new LoggedInUsers();

        users.logOut("nobody");

        assertFalse(users.isLoggedIn("nobody"));
    }

    @Test
    void ofManyThreadsLoggingInTheSameUserAtTheSameMomentExactlyOneWins() throws Exception {
        LoggedInUsers users = new LoggedInUsers();
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startTogether = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                startTogether.await();        // every thread waits here...
                return users.logIn("dana");   // ...then they all try at the same moment
            }));
        }
        startTogether.countDown();

        int winners = 0;
        for (Future<Boolean> result : results) {
            if (result.get(5, TimeUnit.SECONDS)) {
                winners++;
            }
        }
        pool.shutdown();

        assertEquals(1, winners);
    }
}
