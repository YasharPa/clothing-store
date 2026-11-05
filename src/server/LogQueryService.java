package server;

import shared.Employee;
import shared.dataTransfer.LogEntry;
import shared.dataTransfer.LogQuery;
import shared.Role;
import shared.ServiceException;

import java.util.List;

/**
 * Reading the system log for the log screen. Only admin or shift manager may read it.
 */
public class LogQueryService {
    public static final int MAX_LINES_LOGS = 500;

    private final LogService log = LogService.getInstance();

    public List<LogEntry> read(Employee actor, LogQuery query) {
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.SHIFT_MANAGER) {
            throw new ServiceException("Viewing logs is allowed for admins or shift manager only");
        }
        int max = query.maxLines();
        if (max < 1) {
            max = 1;
        }
        if (max > MAX_LINES_LOGS) {
            max = MAX_LINES_LOGS;
        }
        return log.read(query.type(), max);
    }
}
