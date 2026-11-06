package shared;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * it holds the records of the customers, reports, logs part and the shared data records
 */
public final class DataTransfer {
    private DataTransfer() {
    }

    public record CustomerForm(CustomerType type, String id, String fullName, String phone) { }

    public record CustomersUpdate(List<Customer> customers, String description) { }

    public enum ReportMode { BY_BRANCH, BY_PRODUCT, BY_CATEGORY }

    /** branchId 0 = all branches | date null = all dates | Empty lists = everything */
    public record ReportRequest(ReportMode mode, int branchId, LocalDate date,
                                List<Integer> productIds, List<String> categories) { }

    public record ReportResult(String title, List<String> lines) {
        public String asText() {
            return title + System.lineSeparator() + String.join(System.lineSeparator(), lines);
        }
    }

    public record LogQuery(LogType type, int maxLines) { }

    public record LogEntry(LocalDateTime time, LogType type, String user, String details) { }

    public record Branch(int id, String name) {
        @Override
        public String toString() {
            return id + " - " + name;
        }
    }

    public record Product(int id, String name, String category, double price) {
    }

    public record SalesRecord(int branchId, int productId, String productName, String category,
                              int qty, double amount, LocalDateTime dateTime,
                              String soldBy, String customerId) {
    }
}
