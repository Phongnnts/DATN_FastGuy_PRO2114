package service;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderScheduler {
    private static ScheduledExecutorService scheduler;
    private static final AutomaticAttendanceService attendanceService = new AutomaticAttendanceService();
    private static final ShiftRolloverService rolloverService = new ShiftRolloverService();
    private static final OrderExpiryService expiryService = new OrderExpiryService();

    public static void start() {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "order-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(OrderScheduler::runCancellationTick, 1, 1, TimeUnit.MINUTES);
    }

    public static void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private static void runCancellationTick() {
        LocalDateTime now = WorkShiftService.businessNow();
        runTick(() -> attendanceService.autoCheckIns(now), () -> rolloverService.rolloverEnded(now),
                () -> expiryService.cancelCutoffCandidates(now), () -> attendanceService.autoCheckOuts(now),
                () -> expiryService.cancelExpiredCandidates(now));
    }

    static void runTick(Runnable checkIns, Runnable rollover, Runnable cutoff, Runnable checkOuts, Runnable expiry) {
        runPhase(checkIns);
        runPhase(rollover);
        runPhase(cutoff);
        runPhase(checkOuts);
        runPhase(expiry);
    }

    private static void runPhase(Runnable phase) {
        try { phase.run(); }
        catch (RuntimeException e) { e.printStackTrace(); }
    }
}
