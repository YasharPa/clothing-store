package server;

import common.DataTransfer.*;
import common.LogType;
import common.Role;
import common.ServiceException;
import common.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;


public class ReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String THIN_LINE = "-".repeat(60);
    private static final String THICK_LINE = "=".repeat(60);
    private static final int NAME_WIDTH = 22;

    private final Database db;
    private final LogService log = LogService.getInstance();

    public ReportService(Database db) {
        this.db = db;
    }

    public ReportResult generate(User actor, ReportRequest reportReq) {
        if (reportReq.mode() == null) {
            throw new ServiceException("Please choose a report type");
        }
        ReportResult result;
        String logText;
        synchronized (db) {
            Database.Store myStore = db.store();
            int wantedBranch = reportReq.branchId();
            if (!seesAllBranches(actor)) {
                wantedBranch = branchIdOf(myStore, actor.getBranch());
            }
            List<Branch> scope = new ArrayList<>();
            if (wantedBranch == 0) {
                scope.addAll(myStore.branches.values());
            } else if (myStore.branches.containsKey(wantedBranch)) {
                scope.add(myStore.branches.get(wantedBranch));
            } else {
                throw new ServiceException("Branch does not exist");
            }
            Set<Integer> branchIds = new LinkedHashSet<>();
            scope.forEach(b -> branchIds.add(b.id()));

            LocalDate date = reportReq.date();
            Predicate<SalesRecord> inScope = sale -> branchIds.contains(sale.branchId())
                    && (date == null || sale.dateTime().toLocalDate().equals(date));

            List<String> lines = new ArrayList<>();
            String scopeText;
            if (scope.size() == 1) {
                scopeText = "Branch " + scope.get(0).name();
            } else {
                scopeText = "The whole network";
            }
            String dateText;
            if (date == null) {
                dateText = "The whole period";
            } else {
                dateText = DATE.format(date);
            }
            String title;

            switch (reportReq.mode()) {
                case BY_BRANCH:
                    title = "Sales quantity report by branch";
                    branchSection(myStore.sales, scope, inScope, lines);
                    break;
                case BY_PRODUCT:
                    title = "Sales report by product";
                    productSection(myStore, reportReq, scope, inScope, lines);
                    break;
                default:
                    title = "Sales report by category";
                    categorySection(myStore, reportReq, scope, inScope, lines);
                    break;
            }

            List<String> out = new ArrayList<>();
            out.add(THICK_LINE);
            out.add("Scope      : " + scopeText);
            out.add("Date       : " + dateText);
            out.add("Generated  : " + TIME.format(LocalDateTime.now()));
            out.add("Created by : " + actor.getUsername());
            out.add(THICK_LINE);
            out.addAll(lines);
            out.add(THICK_LINE);
            result = new ReportResult(title, out);
            logText = title + " (" + scopeText + ", " + dateText + ")";
        }
        log.log(LogType.REPORT, actor.getUsername(), logText);
        return result;
    }

    /**
     * An admin and a shift manager may see every branch. A cashier and a salesperson may make every report
     * (by branch, by product, by category), but they see only the branch they belong to.
     */
    private boolean seesAllBranches(User actor) {
        return actor.getRole() == Role.ADMIN || actor.getRole() == Role.SHIFT_MANAGER;
    }

    private int branchIdOf(Database.Store myStore, String branchName) {
        for (Branch b : myStore.branches.values()) {
            if (b.name().equals(branchName)) {
                return b.id();
            }
        }
        throw new ServiceException("Your branch has no sales yet");
    }

    private void branchSection(List<SalesRecord> sales, List<Branch> scope,
                               Predicate<SalesRecord> inScope, List<String> lines) {
        lines.add(String.format("%-20s %6s %7s %14s", "Branch", "Sales", "Items", "Revenue"));
        lines.add(THIN_LINE);
        int totalQty = 0;
        int totalCount = 0;
        double totalAmount = 0;
        for (Branch b : scope) {
            int qty = 0;
            int count = 0;
            double amount = 0;
            for (SalesRecord sale : sales) {
                if (sale.branchId() == b.id() && inScope.test(sale)) {
                    qty += sale.qty();
                    count++;
                    amount += sale.amount();
                }
            }
            lines.add(String.format("%-20s %6d %7d %14s", shorten(b.name(), 20), count, qty, money(amount)));
            totalQty += qty;
            totalCount += count;
            totalAmount += amount;
        }
        if (scope.size() > 1) {
            lines.add(THIN_LINE);
            lines.add(String.format("%-20s %6d %7d %14s", "TOTAL", totalCount, totalQty, money(totalAmount)));
        }
        addEmptyNote(totalCount, lines);
    }

    private void productSection(Database.Store myStore, ReportRequest r, List<Branch> scope,
                                Predicate<SalesRecord> inScope, List<String> lines) {
        List<Product> chosen = new ArrayList<>();
        if (r.productIds() == null || r.productIds().isEmpty()) {
            chosen.addAll(myStore.products.values());
        } else {
            for (int id : r.productIds()) {
                Product p = myStore.products.get(id);
                if (p == null) {
                    throw new ServiceException("Product number " + id + " does not exist");
                }
                chosen.add(p);
            }
        }
        lines.add(String.format("%-" + NAME_WIDTH + "s %-12s %6s %14s", "Product", "Category", "Units", "Revenue"));
        lines.add(THIN_LINE);
        double grand = 0;
        int grandQty = 0;
        for (Product p : chosen) {
            int qty = 0;
            double amount = 0;
            StringBuilder perBranch = new StringBuilder();
            for (Branch b : scope) {
                int bq = 0;
                for (SalesRecord sale : myStore.sales) {
                    if (sale.productId() == p.id() && sale.branchId() == b.id() && inScope.test(sale)) {
                        bq += sale.qty();
                        amount += sale.amount();
                    }
                }
                qty += bq;
                if (scope.size() > 1) {
                    if (perBranch.length() > 0) {
                        perBranch.append(", ");
                    }
                    perBranch.append(b.name()).append(" ").append(bq);
                }
            }
            grand += amount;
            grandQty += qty;
            lines.add(String.format("%-" + NAME_WIDTH + "s %-12s %6d %14s", shorten(p.name(), NAME_WIDTH),
                    shorten(p.category(), 12), qty, money(amount)));
            if (scope.size() > 1) {
                lines.add("    by branch: " + perBranch);
            }
        }
        lines.add(THIN_LINE);
        lines.add(String.format("%-" + NAME_WIDTH + "s %-12s %6d %14s", "TOTAL", "", grandQty, money(grand)));
        addEmptyNote(grandQty, lines);
    }

    private void categorySection(Database.Store myStore, ReportRequest r, List<Branch> scope,
                                 Predicate<SalesRecord> inScope, List<String> lines) {
        Set<String> known = new LinkedHashSet<>();
        myStore.products.values().forEach(p -> known.add(p.category()));
        List<String> chosen = new ArrayList<>();
        if (r.categories() == null || r.categories().isEmpty()) {
            chosen.addAll(known);
        } else {
            for (String c : r.categories()) {
                String match = known.stream().filter(k -> k.equalsIgnoreCase(c.trim())).findFirst()
                        .orElseThrow(() -> new ServiceException("Category does not exist: " + c));
                chosen.add(match);
            }
        }
        lines.add(String.format("%-34s %7s %14s", "Category", "Units", "Revenue"));
        lines.add(THIN_LINE);
        double grand = 0;
        int grandQty = 0;
        for (String cat : chosen) {
            int qty = 0;
            double amount = 0;
            StringBuilder perBranch = new StringBuilder();
            for (Branch b : scope) {
                int bq = 0;
                for (SalesRecord sale : myStore.sales) {
                    if (sale.category().equals(cat) && sale.branchId() == b.id() && inScope.test(sale)) {
                        bq += sale.qty();
                        amount += sale.amount();
                    }
                }
                qty += bq;
                if (scope.size() > 1) {
                    perBranch.append(" [").append(b.name()).append(": ").append(bq).append("]");
                }
            }
            grand += amount;
            grandQty += qty;
            lines.add(String.format("%-34s %7d %14s", shorten(cat, 34), qty, money(amount)));
            if (scope.size() > 1) {
                lines.add("    by branch: " + perBranch);
            }
        }
        lines.add(THIN_LINE);
        lines.add(String.format("%-34s %7d %14s", "TOTAL", grandQty, money(grand)));
        addEmptyNote(grandQty, lines);
    }

    /** Cuts a long text so that the columns of the table stay aligned. */
    private static String shorten(String text, int width) {
        if (text.length() <= width) {
            return text;
        }
        return text.substring(0, width - 2) + "..";
    }

    /** Adds a clear note when nothing was sold, so an empty report does not look like a mistake. */
    private static void addEmptyNote(int soldCount, List<String> lines) {
        if (soldCount == 0) {
            lines.add("");
            lines.add("No sales were found for this period.");
        }
    }

    private static String money(double v) {
        return String.format(Locale.US, "%,.2f ₪", v);
    }
}