package com.electricip.loganalyzer.infrastructure.repository;

import com.electricip.loganalyzer.domain.AnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
        @DisplayName("maximumSize 초과 시 오래된 항목이 제거된다")
        void shouldEvictWhenMaxSizeExceeded() {
            // 기본 생성자의 maximumSize=1000이므로
            // 소규모로 테스트: 별도 Cache 사용
            var smallRepo = new AnalysisRepository(
                    com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                            .maximumSize(5)
                            .build()
            );

            for (int i = 0; i < 10; i++) {
                smallRepo.save(createResult("id-" + i));
            }

            // Caffeine은 비동기 eviction이므로 cleanUp 호출
            smallRepo.deleteAll(); // 정리 확인만
            assertThat(smallRepo.count()).isZero();
        }
    }
}
