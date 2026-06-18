package kr.go.pmc.agent.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 점검 스케줄러. "every N seconds" 인터벌 또는 일일 HH:mm(간이 cron) 을 지원한다.
 * <p>간이 cron: 6필드 spring 형식 중 "0 0 6 * * *"(매일 06:00) 형태를 파싱한다(분/시 고정,
 * 나머지 와일드카드). 그 외 형식은 인터벌 폴백.</p>
 */
public class InspectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(InspectionScheduler.class);

    /** 점검 실행 콜백(카테고리 null = 전체). */
    public interface InspectionTask {
        void runInspection(List<String> categories);
    }

    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pmc-scheduler");
                t.setDaemon(true);
                return t;
            });

    private final InspectionTask task;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private volatile String scheduleSpec; // cron 또는 "intervalSeconds:N"
    private volatile int intervalSeconds;
    private volatile LocalTime dailyTime;  // cron 일일 시각(없으면 null → 인터벌 모드)
    private ScheduledFuture<?> future;

    public InspectionScheduler(InspectionTask task) {
        this.task = task;
    }

    /**
     * 스케줄 시작. cronOrInterval 이 cron 이면 일일 모드, 아니면 인터벌 모드(초).
     */
    public synchronized void start(String cron, int intervalSeconds) {
        this.intervalSeconds = intervalSeconds <= 0 ? 300 : intervalSeconds;
        setSchedule(cron != null && !cron.trim().isEmpty()
                ? cron : ("intervalSeconds:" + this.intervalSeconds));
    }

    /**
     * 런타임 스케줄 변경. cron(예 "0 0 6 * * *") 또는 "intervalSeconds:N" 또는 순수 숫자(초).
     */
    public synchronized void setSchedule(String spec) {
        this.scheduleSpec = spec;
        this.dailyTime = parseDailyCron(spec);
        if (dailyTime == null) {
            Integer iv = parseInterval(spec);
            if (iv != null) this.intervalSeconds = iv;
        }
        reschedule();
        log.info("스케줄 설정: {} (mode={}, interval={}s, daily={})",
                spec, dailyTime != null ? "DAILY" : "INTERVAL", intervalSeconds, dailyTime);
    }

    private synchronized void reschedule() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        if (dailyTime != null) {
            scheduleNextDaily();
        } else {
            future = exec.scheduleAtFixedRate(this::fire,
                    intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }
    }

    private synchronized void scheduleNextDaily() {
        long delay = secondsUntilNext(dailyTime);
        future = exec.schedule(() -> {
            fire();
            scheduleNextDaily(); // 다음 날 재예약
        }, delay, TimeUnit.SECONDS);
    }

    long secondsUntilNext(LocalTime time) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().atTime(time);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        long secs = ChronoUnit.SECONDS.between(now, next);
        return secs <= 0 ? 1 : secs;
    }

    private void fire() {
        if (paused.get()) {
            log.debug("스케줄러 일시정지 상태 — 점검 건너뜀");
            return;
        }
        try {
            task.runInspection(null);
        } catch (RuntimeException e) {
            log.warn("스케줄 점검 실행 오류: {}", e.getMessage());
        }
    }

    /** 즉시 1회 실행(스케줄 외). */
    public void runNow(List<String> categories) {
        try {
            task.runInspection(categories);
        } catch (RuntimeException e) {
            log.warn("runNow 실행 오류: {}", e.getMessage());
        }
    }

    /** 일시정지(스케줄 유지, 실행만 스킵). */
    public void stop() {
        paused.set(true);
        log.info("스케줄러 일시정지");
    }

    /** 재개. */
    public void resume() {
        paused.set(false);
        log.info("스케줄러 재개");
    }

    public boolean isPaused() {
        return paused.get();
    }

    public String currentSpec() {
        return scheduleSpec;
    }

    public int intervalSeconds() {
        return intervalSeconds;
    }

    /** 완전 종료(데몬 셧다운 시). */
    public void shutdown() {
        exec.shutdownNow();
    }

    /**
     * "0 0 6 * * *" 형태(6필드: 초 분 시 일 월 요일)에서 분/시를 추출해 일일 시각 반환.
     * 분/시가 숫자가 아니거나 형식이 다르면 null(인터벌 모드).
     */
    static LocalTime parseDailyCron(String spec) {
        if (spec == null) return null;
        String s = spec.trim();
        if (!s.contains(" ")) return null;
        String[] f = s.split("\\s+");
        if (f.length != 6) return null;
        // 초 분 시 — 분/시가 정수여야 일일 모드로 인정
        try {
            int sec = "*".equals(f[0]) ? 0 : Integer.parseInt(f[0]);
            int min = Integer.parseInt(f[1]);
            int hour = Integer.parseInt(f[2]);
            if (min < 0 || min > 59 || hour < 0 || hour > 23) return null;
            return LocalTime.of(hour, min, Math.min(Math.max(sec, 0), 59));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "intervalSeconds:N" 또는 순수 숫자에서 초 추출. */
    static Integer parseInterval(String spec) {
        if (spec == null) return null;
        String s = spec.trim();
        if (s.startsWith("intervalSeconds:")) {
            s = s.substring("intervalSeconds:".length()).trim();
        }
        try {
            int v = Integer.parseInt(s);
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
