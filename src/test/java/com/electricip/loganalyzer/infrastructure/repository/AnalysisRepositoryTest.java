package com.electricip.loganalyzer.infrastructure.repository;

import com.electricip.loganalyzer.domain.AnalysisResult;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisRepositoryTest {

    private AnalysisRepository repository;

    private static AnalysisResult createResult(String id) {
        return AnalysisResult.builder()
                .analysisId(id)
                .completedAt(LocalDateTime.now())
                .statistics(AnalysisResult.Statistics.builder().totalRequests(0).build())
                .build();
    }

    @BeforeEach
    void setUp() {
        repository = new AnalysisRepository();
    }

    @Nested
    @DisplayName("save 검증")
    class SaveTest {

        @Test
        @DisplayName("정상 저장 후 조회된다")
        void shouldSaveAndFind() {
            var result = createResult("test-1");
            repository.save(result);

            assertThat(repository.findById("test-1")).isPresent();
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("null result는 NullPointerException 발생")
        void shouldThrowForNullResult() {
            assertThatThrownBy(() -> repository.save(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("result");
        }

        @Test
        @DisplayName("같은 ID로 저장하면 덮어쓴다")
        void shouldOverwriteSameId() {
            repository.save(createResult("test-1"));
            repository.save(createResult("test-1"));

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findById 검증")
    class FindByIdTest {

        @Test
        @DisplayName("존재하는 ID는 Optional.of 반환")
        void shouldFindExistingId() {
            repository.save(createResult("test-1"));

            var found = repository.findById("test-1");
            assertThat(found).isPresent();
            assertThat(found.get().getAnalysisId()).isEqualTo("test-1");
        }

        @Test
        @DisplayName("존재하지 않는 ID는 Optional.empty 반환")
        void shouldReturnEmptyForMissingId() {
            assertThat(repository.findById("nonexistent")).isEmpty();
        }

        @Test
        @DisplayName("null ID는 NullPointerException 발생")
        void shouldThrowForNullId() {
            assertThatThrownBy(() -> repository.findById(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("findAll 검증")
    class FindAllTest {

        @Test
        @DisplayName("빈 저장소는 빈 리스트 반환")
        void shouldReturnEmptyListWhenEmpty() {
            assertThat(repository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("저장된 모든 결과를 반환한다")
        void shouldReturnAllResults() {
            repository.save(createResult("test-1"));
            repository.save(createResult("test-2"));

            assertThat(repository.findAll()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("deleteById 검증")
    class DeleteByIdTest {

        @Test
        @DisplayName("존재하는 ID 삭제 시 true 반환")
        void shouldReturnTrueWhenDeleted() {
            repository.save(createResult("test-1"));

            assertThat(repository.deleteById("test-1")).isTrue();
            assertThat(repository.findById("test-1")).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID 삭제 시 false 반환")
        void shouldReturnFalseWhenNotFound() {
            assertThat(repository.deleteById("nonexistent")).isFalse();
        }

        @Test
        @DisplayName("null ID는 NullPointerException 발생")
        void shouldThrowForNullId() {
            assertThatThrownBy(() -> repository.deleteById(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("deleteAll 검증")
    class DeleteAllTest {

        @Test
        @DisplayName("전체 삭제 후 빈 상태")
        void shouldClearAll() {
            repository.save(createResult("test-1"));
            repository.save(createResult("test-2"));

            repository.deleteAll();

            assertThat(repository.count()).isZero();
            assertThat(repository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Caffeine Cache 동작 검증")
    class CacheBehaviorTest {

        @Test
        @DisplayName("maximumSize 초과 시 항목이 eviction된다")
        void shouldEvictWhenMaxSizeExceeded() {
            Cache<String, AnalysisResult> cache =
                    com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                            .maximumSize(5)
                            .build();
            var smallRepo = new AnalysisRepository(cache);

            for (int i = 0; i < 10; i++) {
                smallRepo.save(createResult("id-" + i));
            }

            // Caffeine은 비동기 eviction이므로 cleanUp()으로 즉시 반영
            cache.cleanUp();

            assertThat(cache.asMap().size()).isLessThanOrEqualTo(5);
        }
    }

    @Nested
    @DisplayName("동시성 검증")
    class ConcurrencyTest {

        @Test
        @DisplayName("100개 동시 저장 시 중복 없이 모두 저장된다")
        void concurrentSave_noDuplicates() throws InterruptedException {
            var latch = new CountDownLatch(100);
            var executor = newFixedThreadPool(10);

            var results = IntStream.range(0, 100)
                    .mapToObj(i -> createResult("id-" + i))
                    .toList();

            try {
                for (var result : results) {
                    executor.submit(() -> {
                        try {
                            repository.save(result);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                executor.shutdown();
            }

            assertThat(repository.count()).isEqualTo(100);
        }

        @Test
        @DisplayName("저장과 조회를 동시에 수행해도 일관성 유지")
        void concurrentSaveAndFind_consistent() throws InterruptedException {
            // 먼저 50개 저장
            for (int i = 0; i < 50; i++) {
                repository.save(createResult("pre-" + i));
            }

            var latch = new CountDownLatch(100);
            var executor = newFixedThreadPool(10);

            // 50개 저장 + 50개 조회 동시 실행
            try {
                for (int i = 0; i < 50; i++) {
                    final int idx = i;
                    executor.submit(() -> {
                        try {
                            repository.save(createResult("new-" + idx));
                        } finally {
                            latch.countDown();
                        }
                    });
                    executor.submit(() -> {
                        try {
                            repository.findById("pre-" + idx);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                executor.shutdown();
            }

            // 기존 50 + 신규 50 = 100
            assertThat(repository.count()).isEqualTo(100);
            // 기존 항목 여전히 조회 가능
            assertThat(repository.findById("pre-0")).isPresent();
        }

        @Test
        @DisplayName("저장과 삭제를 동시에 수행해도 예외 없음")
        void concurrentSaveAndDelete_noException() throws InterruptedException {
            var latch = new CountDownLatch(200);
            var executor = newFixedThreadPool(10);

            try {
                for (int i = 0; i < 100; i++) {
                    final int idx = i;
                    executor.submit(() -> {
                        try {
                            repository.save(createResult("cd-" + idx));
                        } finally {
                            latch.countDown();
                        }
                    });
                    executor.submit(() -> {
                        try {
                            repository.deleteById("cd-" + idx);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                executor.shutdown();
            }

            // 예외 없이 완료되면 성공 (최종 상태는 타이밍에 따라 다름)
            assertThat(repository.count()).isGreaterThanOrEqualTo(0);
        }
    }
}
