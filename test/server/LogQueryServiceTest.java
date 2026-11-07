package server;

import common.DataTransfer.LogEntry;
import common.DataTransfer.LogQuery;
import common.Employee;
import common.LogType;
import common.Role;
import common.ServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks who may read the log, and the filter by action type and the limit on the number of lines. */
class LogQueryServiceTest {

    private final Employee admin = new Employee("root", "System Admin", "300000001", "0501111111",
            "12345001", 0, 1001, Role.ADMIN);
    private final Employee manager = new Employee("moshe", "Moshe Levi", "300000002", "0502222222",
            "12345002", 1, 1002, Role.SHIFT_MANAGER);
    private final Employee cashier = new Employee("moshe", "Moshe Cohen", "300000003", "0503333333",
            "12345003", 1, 1003, Role.CASHIER);
    private final Employee seller = new Employee("avi", "Avi Peretz", "300000004", "0504444444",
            "12345004", 1, 1004, Role.SELLER);

    private final LogQueryService service = new LogQueryService();
    private final LogService log = LogService.getInstance();

    @BeforeAll
    static void useATemporaryDataFolder() {
        TestEnv.init();
    }

    @Test
    void anAdminCanReadTheLog() {
        log.log(LogType.SALE, "admin-test", "a sale");

        List<LogEntry> entries = service.read(admin, new LogQuery(LogType.SALE, 1000));

        assertEquals("admin-test", entries.get(entries.size() - 1).user());
    }

    @Test
    void aShiftManagerCanReadTheLog() {
        service.read(manager, new LogQuery(null, 10));
    }

    @Test
    void aCashierCannotReadTheLog() {
        assertThrows(ServiceException.class, () -> service.read(cashier, new LogQuery(null, 10)));
    }

    @Test
    void aSellerCannotReadTheLog() {
        assertThrows(ServiceException.class, () -> service.read(seller, new LogQuery(null, 10)));
    }

    @Test
    void theTypeFilterKeepsOnlyThatType() {
        log.log(LogType.PRODUCT_ADDED, "filter-test", "added a product");
        log.log(LogType.STOCK_PURCHASE, "filter-test", "bought stock");

        List<LogEntry> entries = service.read(admin, new LogQuery(LogType.PRODUCT_ADDED, 1000));

        for (LogEntry entry : entries) {
            assertEquals(LogType.PRODUCT_ADDED, entry.type());
        }
    }

    @Test
    void maxLinesKeepsTheNewestEntries() {
        for (int i = 1; i <= 5; i++) {
            log.log(LogType.CHAT_STARTED, "max-test", "entry " + i);
        }

        List<LogEntry> entries = service.read(admin, new LogQuery(LogType.CHAT_STARTED, 2));

        assertEquals(2, entries.size());
        assertEquals("entry 4", entries.get(0).details());
        assertEquals("entry 5", entries.get(1).details());
    }

    @Test
    void aMaxBelowOneStillGivesOneEntry() {
        log.log(LogType.CHAT_ENDED, "min-test", "x");

        List<LogEntry> entries = service.read(admin, new LogQuery(LogType.CHAT_ENDED, 0));

        assertEquals(1, entries.size());
    }

    @Test
    void aHugeMaxIsCutToTheLimit() {
        List<LogEntry> entries = service.read(admin, new LogQuery(null, 1_000_000));

        assertTrue(entries.size() <= LogQueryService.MAX_LINES_LOGS);
    }

    @Test
    void aNewLineInTheDetailsDoesNotBreakTheLog() {
        log.log(LogType.LOGIN, "newline-test", "first\nsecond");

        List<LogEntry> entries = service.read(admin, new LogQuery(LogType.LOGIN, 1000));

        assertEquals("first second", entries.get(entries.size() - 1).details());
    }
}
