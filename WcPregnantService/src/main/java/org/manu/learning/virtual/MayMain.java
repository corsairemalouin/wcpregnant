package org.manu.learning.virtual;

import static org.manu.learning.virtual.Utils.sleepFor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MayMain {
    private static final Logger log = LoggerFactory.getLogger(MayMain.class);
    private static final Pattern POOL_PATTERN = Pattern.compile("ForkJoinPool-[\\d]");
    private static final Pattern WORKER_PATTERN = Pattern.compile("worker-[\\d?]+");
    private static int counter;

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        main5();
        long endTime = System.currentTimeMillis();
        log.info("Total elapsed time in execution of method callMethod() is :" + (endTime - startTime));

    }

    private static void main5() {
    }

    public static void main4(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        main41();
        long endTime = System.currentTimeMillis();
        log.info("Total elapsed time in execution of method callMethod() is :" + (endTime - startTime));
        startTime = System.currentTimeMillis();
        main42();
        endTime = System.currentTimeMillis();
        log.info("Total elapsed time in execution of method callMethod() is :" + (endTime - startTime));
    }

    public static void main42() throws InterruptedException {
        var lock = new ReentrantLock();

        Runnable task1 = () -> {

            log.info(Thread.currentThread().toString());
            lock.lock();
            try {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            } finally {
                lock.unlock();
            }

            log.info(Thread.currentThread().toString());
            lock.lock();
            try {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            } finally {
                lock.unlock();
            }

            log.info(Thread.currentThread().toString());
            lock.lock();
            try {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            } finally {
                lock.unlock();
            }

            log.info(Thread.currentThread().toString());
        };

        Runnable task2 = () -> {

            lock.lock();
            try {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            } finally {
                lock.unlock();
            }

            lock.lock();
            try {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            } finally {
                lock.unlock();
            }

            lock.lock();
            try {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            } finally {
                lock.unlock();
            }
        };

        int N8THREADS = 1000;
        var threads = new ArrayList<Thread>();

        for (int index = 0; index < N8THREADS; index++) {
            var thread = (index == 0) ? Thread.ofVirtual().unstarted(task1) : Thread.ofVirtual().unstarted(task2);
            threads.add(thread);
        }
        for (var thread : threads) {
            thread.start();
        }
        for (var thread : threads) {
            thread.join();
        }
        log.info("#  threads = {}", N8THREADS);
        log.info("counter = {}", counter);
    }

    public static void main41() throws InterruptedException {
        var lock = new Object();

        Runnable task1 = () -> {
            log.info(Thread.currentThread().toString());
            synchronized (lock) {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            }
            log.info(Thread.currentThread().toString());
            synchronized (lock) {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            }
            log.info(Thread.currentThread().toString());
            synchronized (lock) {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            }
            log.info(Thread.currentThread().toString());
        };

        Runnable task2 = () -> {
            synchronized (lock) {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            }
            synchronized (lock) {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            }
            synchronized (lock) {
                counter++;
                sleepFor(1, ChronoUnit.MICROS);
            }
        };

        int N8THREADS = 1000;
        var threads = new ArrayList<Thread>();

        for (int index = 0; index < N8THREADS; index++) {
            var thread = (index == 0) ? Thread.ofVirtual().unstarted(task1) : Thread.ofVirtual().unstarted(task2);
            threads.add(thread);
        }
        for (var thread : threads) {
            thread.start();
        }
        for (var thread : threads) {
            thread.join();
        }
        log.info("#  threads = {}", N8THREADS);
        log.info("counter = {}", counter);
    }

    public static void main3() throws InterruptedException {
        Set<String> poolNames = ConcurrentHashMap.newKeySet();
        Set<String> pThreadNames = ConcurrentHashMap.newKeySet();
        int N_THREADS = 1_000_000;
        var threads = IntStream.range(0, N_THREADS).mapToObj(i -> Thread.ofVirtual().unstarted(() -> {
            String poolName = readPoolName();
            poolNames.add(poolName);
            String workerName = readWorkerName();
            pThreadNames.add(workerName);
        })).toList();

        Instant begin = Instant.now();
        for (var thread : threads) {
            thread.start();
        }
        for (var thread : threads) {
            thread.join();
        }
        Instant end = Instant.now();
        log.info("# virtual threads = {}", N_THREADS);
        log.info("# cores = {}", Runtime.getRuntime().availableProcessors());
        log.info("Time = {} ms", Duration.between(begin, end).toMillis());
        log.info("### Pools: {}", poolNames.size());
        poolNames.forEach(log::info);
        log.info("# plateform threads = {}", pThreadNames.size());

    }

    private static String readWorkerName() {
        String name = Thread.currentThread().toString();
        Matcher workerMatcher = WORKER_PATTERN.matcher(name);
        if (workerMatcher.find()) {
            return workerMatcher.group();
        }
        return "not found";
    }

    private static String readPoolName() {
        String name = Thread.currentThread().toString();
        Matcher workerMatcher = POOL_PATTERN.matcher(name);
        if (workerMatcher.find()) {
            return workerMatcher.group();
        }
        return "pool not found";
    }

    public static void main22() throws InterruptedException {
        var set = ConcurrentHashMap.<String>newKeySet();
        Runnable task = () -> set.add(Thread.currentThread().toString());
        int N_TASKS = 20000;
        try (var es1 = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int index = 0; index < N_TASKS; index++) {
                es1.submit(task);
            }
        }
        log.info("# treads used = {}", set.size());
    }

    public static void main21() throws InterruptedException {
        var set = ConcurrentHashMap.<String>newKeySet();
        Runnable task = () -> set.add(Thread.currentThread().toString());
        int N_TASKS = 20000;
        try (var es1 = Executors.newFixedThreadPool(100)) {

            for (int index = 0; index < N_TASKS; index++) {
                es1.submit(task);
            }
        }
        log.info("# treads used = {}", set.size());
    }

    public static void main1() throws InterruptedException {

        Runnable task = () -> {
            log.info("i am running in the thread {} [demon thread:{}]", Thread.currentThread().getName(),
                    Thread.currentThread().isDaemon());
        };

        Thread thread1 = new Thread(task);
        thread1.start();
        thread1.join();

        Thread thread2 = Thread.ofPlatform().daemon().name("Plateforme thread 2").unstarted(task);
        thread2.start();
        thread2.join();

        Thread thread3 = Thread.ofVirtual().name("Virtual thread 3").unstarted(task);
        thread3.start();
        thread3.join();

        Thread thread4 = Thread.startVirtualThread(task);
        thread4.join();
    }
}
