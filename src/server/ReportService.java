package server;

import shared.Employee;
import shared.LogType;
import shared.dataTransfer.*;
import shared.ServiceException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;


public class ReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Database db;
    private final LogService log = LogService.getInstance();

    public ReportService(Database db) {
        this.db = db;
    }

    public ReportResult generate(Employee actor, ReportRequest reportReq) {
        if (reportReq.mode() == null) {
            throw new ServiceException("Please choose a report type");
        }
        ReportResult result;
        synchronized (db) {
            Database.Store myStore = db.store();
            List<Branch> scope = new ArrayList<>();
            if (reportReq.branchId() == 0) {
                scope.addAll(myStore.branches.values());
            } else if (myStore.branches.containsKey(reportReq.branchId())) {
                scope.add(myStore.branches.get(reportReq.branchId()));
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
            out.add("Scope: " + scopeText + " | Date: " + dateText);
            out.add("Generated: " + TIME.format(LocalDateTime.now()));
            out.add("------------------------------------------------------------");
            out.addAll(lines);
            result = new ReportResult(title, out);
        }
        log.log(LogType.REPORT, actor.getUsername(), result.title() + " (" + result.lines().get(0) + ")");
        return result;
    }

    private void branchSection(List<SalesRecord> sales, List<Branch> scope,
                               Predicate<SalesRecord> inScope, List<String> lines) {
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
            lines.add("Branch " + b.name() + ": " + count + " sales, " + qty + " items, revenue " + money(amount));
            totalQty += qty;
            totalCount += count;
            totalAmount += amount;
        }
        if (scope.size() > 1) {
            lines.add("------------------------------------------------------------");
            lines.add("Network total: " + totalCount + " sales, " + totalQty + " items, revenue " + money(totalAmount));
        }
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
        double grand = 0;
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
                    perBranch.append(" [").append(b.name()).append(": ").append(bq).append("]");
                }
            }
            grand += amount;
            lines.add(p.name() + " (" + p.category() + "): sold " + qty + " units, revenue " + money(amount)
                    + perBranch);
        }
        lines.add("------------------------------------------------------------");
        lines.add("Total revenue: " + money(grand));
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
        double grand = 0;
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
            lines.add("Category " + cat + ": sold " + qty + " units, revenue " + money(amount) + perBranch);
        }
        lines.add("------------------------------------------------------------");
        lines.add("Total revenue: " + money(grand));
    }

    private static String money(double v) {
        return String.format(Locale.US, "%,.2f ₪", v);
    }
}
