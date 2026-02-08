package com.electricip.loganalyzer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpInfoTest {

    @Nested
    @DisplayName("Compact Constructor 검증")
    class CompactConstructorTest {

        @Test
        @DisplayName("모든 필드가 정상이면 그대로 생성된다")
        void shouldCreateWithValidFields() {
            var info = new IpInfo("1.2.3.4", "KR", "Seoul", "Gangnam", "ISP Corp");

            assertThat(info.ip()).isEqualTo("1.2.3.4");
            assertThat(info.country()).isEqualTo("KR");
            assertThat(info.region()).isEqualTo("Seoul");
            assertThat(info.city()).isEqualTo("Gangnam");
            assertThat(info.organization()).isEqualTo("ISP Corp");
        }

        @Test
        @DisplayName("ip가 null이면 IllegalArgumentException 발생")
        void shouldThrowWhenIpIsNull() {
            assertThatThrownBy(() -> new IpInfo(null, "KR", "Seoul", "Gangnam", "ISP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP");
        }

        @Test
        @DisplayName("ip가 blank이면 IllegalArgumentException 발생")
        void shouldThrowWhenIpIsBlank() {
            assertThatThrownBy(() -> new IpInfo("  ", "KR", "Seoul", "Gangnam", "ISP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP");
        }

        @Test
        @DisplayName("ip가 빈 문자열이면 IllegalArgumentException 발생")
        void shouldThrowWhenIpIsEmpty() {
            assertThatThrownBy(() -> new IpInfo("", "KR", "Seoul", "Gangnam", "ISP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP");
        }

        @Test
        @DisplayName("country가 null이면 UNKNOWN으로 치환된다")
        void shouldReplaceNullCountryWithUnknown() {
            var info = new IpInfo("1.2.3.4", null, "Seoul", "Gangnam", "ISP");
            assertThat(info.country()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("region이 blank이면 UNKNOWN으로 치환된다")
        void shouldReplaceBlankRegionWithUnknown() {
            var info = new IpInfo("1.2.3.4", "KR", "  ", "Gangnam", "ISP");
            assertThat(info.region()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("city가 null이면 UNKNOWN으로 치환된다")
        void shouldReplaceNullCityWithUnknown() {
            var info = new IpInfo("1.2.3.4", "KR", "Seoul", null, "ISP");
            assertThat(info.city()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("city가 blank이면 UNKNOWN으로 치환된다")
        void shouldReplaceBlankCityWithUnknown() {
            var info = new IpInfo("1.2.3.4", "KR", "Seoul", "  ", "ISP");
            assertThat(info.city()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("organization이 null이면 UNKNOWN으로 치환된다")
        void shouldReplaceNullOrganizationWithUnknown() {
            var info = new IpInfo("1.2.3.4", "KR", "Seoul", "Gangnam", null);
            assertThat(info.organization()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("organization이 blank이면 UNKNOWN으로 치환된다")
        void shouldReplaceBlankOrganizationWithUnknown() {
            var info = new IpInfo("1.2.3.4", "KR", "Seoul", "Gangnam", "  ");
            assertThat(info.organization()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("모든 선택 필드가 null이면 전부 UNKNOWN으로 치환된다")
        void shouldReplaceAllNullFieldsWithUnknown() {
            var info = new IpInfo("1.2.3.4", null, null, null, null);

            assertThat(info.country()).isEqualTo("UNKNOWN");
            assertThat(info.region()).isEqualTo("UNKNOWN");
            assertThat(info.city()).isEqualTo("UNKNOWN");
            assertThat(info.organization()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("모든 선택 필드가 blank이면 전부 UNKNOWN으로 치환된다")
        void shouldReplaceAllBlankFieldsWithUnknown() {
            var info = new IpInfo("1.2.3.4", "", " ", "\t", "\n");

            assertThat(info.country()).isEqualTo("UNKNOWN");
            assertThat(info.region()).isEqualTo("UNKNOWN");
            assertThat(info.city()).isEqualTo("UNKNOWN");
            assertThat(info.organization()).isEqualTo("UNKNOWN");
        }
    }

    @Nested
    @DisplayName("팩터리 메서드 검증")
    class FactoryMethodTest {

        @Test
        @DisplayName("of()는 compact constructor와 동일하게 동작한다")
        void ofShouldDelegateToConstructor() {
            var info = IpInfo.of("1.2.3.4", null, "Seoul", null, null);

            assertThat(info.ip()).isEqualTo("1.2.3.4");
            assertThat(info.country()).isEqualTo("UNKNOWN");
            assertThat(info.region()).isEqualTo("Seoul");
            assertThat(info.city()).isEqualTo("UNKNOWN");
            assertThat(info.organization()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("of()에서 ip가 null이면 IllegalArgumentException 발생")
        void ofShouldThrowWhenIpIsNull() {
            assertThatThrownBy(() -> IpInfo.of(null, "KR", "Seoul", "Gangnam", "ISP"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP");
        }

        @Test
        @DisplayName("unknown()은 모든 필드가 UNKNOWN인 IpInfo를 반환한다")
        void unknownShouldReturnAllUnknown() {
            var info = IpInfo.unknown("1.2.3.4");

            assertThat(info.ip()).isEqualTo("1.2.3.4");
            assertThat(info.country()).isEqualTo("UNKNOWN");
            assertThat(info.region()).isEqualTo("UNKNOWN");
            assertThat(info.city()).isEqualTo("UNKNOWN");
            assertThat(info.organization()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("unknown()에서 ip가 null이면 IllegalArgumentException 발생")
        void unknownShouldThrowWhenIpIsNull() {
            assertThatThrownBy(() -> IpInfo.unknown(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP");
        }
    }

    @Nested
    @DisplayName("isValid() 검증")
    class IsValidTest {

        @Test
        @DisplayName("country가 UNKNOWN이 아니면 유효하다")
        void shouldBeValidWhenCountryIsKnown() {
            var info = IpInfo.of("1.2.3.4", "KR", null, null, null);
            assertThat(info.isValid()).isTrue();
        }

        @Test
        @DisplayName("country가 UNKNOWN이면 유효하지 않다")
        void shouldBeInvalidWhenCountryIsUnknown() {
            var info = IpInfo.unknown("1.2.3.4");
            assertThat(info.isValid()).isFalse();
        }

        @Test
        @DisplayName("country가 null로 전달되어 UNKNOWN 치환된 경우 유효하지 않다")
        void shouldBeInvalidWhenCountryWasNull() {
            var info = new IpInfo("1.2.3.4", null, "Seoul", "Gangnam", "ISP");
            assertThat(info.isValid()).isFalse();
        }
    }
}
