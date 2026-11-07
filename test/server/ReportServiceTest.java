package server;

import common.DataTransfer.ReportMode;
import common.DataTransfer.ReportRequest;
import common.DataTransfer.ReportResult;
import common.Employee;
import common.LogType;
import common.Role;
import common.ServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the sales reports. The test database has branch 1 (Tel Aviv) and branch 2 (Haifa),
 * and products 1 (Cotton T-Shirt, Tops), 2 (Slim Jeans, Pants), 3 (Sneakers, Shoes).
 * Each test starts with three sales: Tel Aviv sold 2 T-Shirts and 1 Jeans, Haifa sold 3 T-Shirts.
 */
class ReportServiceTest {

    private final Employee cashier = new Employee("moshe", "Moshe Cohen", "300000001", "0501111111",
            "12345001", 1, 1001, Role.CASHIER);
    private ReportService service;

    @BeforeAll
    static void useATemporaryDataFolder() {
        TestEnv.init();
    }

    @BeforeEach
    void createServiceWithThreeSales() {
        Database db = new Database();
        db.addSale(1, 1, 2, 179.80, LocalDateTime.of(2026, 1, 5, 10, 0));
        db.addSale(1, 2, 1, 199.00, LocalDateTime.of(2026, 1, 6, 11, 0));
        db.addSale(2, 1, 3, 269.70, LocalDateTime.of(2026, 1, 5, 12, 0));
        service = new ReportService(db);
    }

    /** A report request with no product or category chosen (= all of them). */
    private ReportRequest request(ReportMode mode, int branchId, LocalDate date) {
        return new ReportRequest(mode, branchId, date, new ArrayList<>(), new ArrayList<>());
    }

    /** All the report lines as one text, so a test can check what it contains. */
    private String text(ReportResult report) {
        return String.join("\n", report.lines());
    }

    @Test
    void byBranchForTheWholeNetworkShowsEveryBranchAndATotal() {
        ReportResult report = service.generate(cashier, request(ReportMode.BY_BRANCH, 0, null));

        assertTrue(text(report).contains("Branch Tel Aviv: 2 sales, 3 items"));
        assertTrue(text(report).contains("Branch Haifa: 1 sales, 3 items"));
        assertTrue(text(report).contains("Network total: 3 sales, 6 items"));
    }

    @Test
    void byBranchForOneBranchShowsOnlyThatBranch() {
        ReportResult report = service.generate(cashier, request(ReportMode.BY_BRANCH, 2, null));

        assertTrue(text(report).contains("Branch Haifa"));
        assertFalse(text(report).contains("Tel Aviv"));
        assertFalse(text(report).contains("Network total"));
    }

    @Test
    void aDateKeepsOnlyTheSalesOfThatDay() {
        ReportResult report = service.generate(cashier, request(ReportMode.BY_BRANCH, 0, LocalDate.of(2026, 1, 6)));

        assertTrue(text(report).contains("Branch Tel Aviv: 1 sales, 1 items"));
        assertTrue(text(report).contains("Branch Haifa: 0 sales, 0 items"));
    }

    @Test
    void byProductListsEveryProductWhenNoneAreChosen() {
        ReportResult report = service.generate(cashier, request(ReportMode.BY_PRODUCT, 0, null));

        assertTrue(text(report).contains("Cotton T-Shirt (Tops): sold 5 units"));
        assertTrue(text(report).contains("Slim Jeans (Pants): sold 1 units"));
        assertTrue(text(report).contains("Sneakers (Shoes): sold 0 units"));
    }

    @Test
    void byProductListsOnlyTheChosenProducts() {
        List<Integer> chosen = new ArrayList<>();
        chosen.add(2);
        ReportRequest request = new ReportRequest(ReportMode.BY_PRODUCT, 0, null, chosen, new ArrayList<>());

        ReportResult report = service.generate(cashier, request);

        assertTrue(text(report).contains("Slim Jeans"));
        assertFalse(text(report).contains("Cotton T-Shirt"));
    }

    @Test
    void byCategoryIgnoresUpperAndLowerCase() {
        List<String> chosen = new ArrayList<>();
        chosen.add("tops");
        ReportRequest request = new ReportRequest(ReportMode.BY_CATEGORY, 0, null, new ArrayList<>(), chosen);

        ReportResult report = service.generate(cashier, request);

        assertTrue(text(report).contains("Category Tops: sold 5 units"));
    }

    @Test
    void anUnknownProductIsRefused() {
        List<Integer> chosen = new ArrayList<>();
        chosen.add(99);
        ReportRequest request = new ReportRequest(ReportMode.BY_PRODUCT, 0, null, chosen, new ArrayList<>());

        assertThrows(ServiceException.class, () -> service.generate(cashier, request));
    }

    @Test
    void anUnknownCategoryIsRefused() {
        List<String> chosen = new ArrayList<>();
        chosen.add("Hats");
        ReportRequest request = new ReportRequest(ReportMode.BY_CATEGORY, 0, null, new ArrayList<>(), chosen);

        assertThrows(ServiceException.class, () -> service.generate(cashier, request));
    }

    @Test
    void anUnknownBranchIsRefused() {
        assertThrows(ServiceException.class, () -> service.generate(cashier, request(ReportMode.BY_BRANCH, 77, null)));
    }

    @Test
    void aMissingReportTypeIsRefused() {
        assertThrows(ServiceException.class, () -> service.generate(cashier, request(null, 0, null)));
    }

    @Test
    void theReportNeverShowsWhoSold() {
        ReportResult report = service.generate(cashier, request(ReportMode.BY_PRODUCT, 0, null));

        assertFalse(text(report).contains("demo"));
        assertFalse(text(report).contains("moshe"));
    }

    @Test
    void makingAReportIsWrittenToTheLog() {
        Employee reporter = new Employee("report-user", "Report User", "300000002", "0502222222",
                "12345002", 1, 1002, Role.CASHIER);

        service.generate(reporter, request(ReportMode.BY_BRANCH, 0, null));

        boolean found = false;
        for (common.DataTransfer.LogEntry entry : LogService.getInstance().read(LogType.REPORT, 1000)) {
            if (entry.user().equals("report-user")) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void asTextStartsWithTheTitle() {
        ReportResult report = service.generate(cashier, request(ReportMode.BY_BRANCH, 0, null));

        assertEquals("Sales quantity report by branch", report.title());
        assertTrue(report.asText().startsWith("Sales quantity report by branch"));
    }
}
