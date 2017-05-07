package ch.frankel.blog.coroutine;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class FutureTest {

    private ExecutorService executor;
    private String[] services;
    private List<Callable<ContentDuration>> callables;

    @BeforeMethod
    protected void setUp() {
        executor = Executors.newWorkStealingPool();
        services = new String[]{"Service A", "Service B", "Service C", "Service X", "Service Y", "Service Z"};
        callables = Stream.of(services)
                .map(DummyService::new)
                .map(service -> (Callable<ContentDuration>) service::getContent)
                .collect(Collectors.toList());
    }

    @Test
    public void should_be_parallel() throws InterruptedException {
        Instant start = Instant.now();
        List<ContentDuration> results = executor.invokeAll(callables).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        Instant end = Instant.now();
        results.forEach(System.out::println);
        assertThat(results)
                .isNotNull()
                .isNotEmpty()
                .hasSameSizeAs(services);
        Integer maxTimeElapsed = results.stream().map(ContentDuration::getDuration).max(Comparator.comparingInt(e -> e)).orElse(0);
        System.out.println("Time taken by the longest service is  " + maxTimeElapsed + " milliseconds");
        Duration duration = Duration.between(start, end);
        long timeElapsed = duration.toMillis();
        System.out.println("Time taken by the executor service is " + timeElapsed + " milliseconds");
        assertThat(timeElapsed).isGreaterThanOrEqualTo(maxTimeElapsed);
    }
}
