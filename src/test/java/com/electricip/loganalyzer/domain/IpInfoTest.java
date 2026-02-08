package com.electricip.loganalyzer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpInfoTest {

    @Nested
    @DisplayName("Compact Constructor 검증")
    class CompactConstructorTest {

        @Test
        @DisplayName("모든 필드가 정상이면 그대로 생성된다")
        void shouldCreateWithValidFields() {
            var info = new IpInfo("1.2.3.4", "KR", "Seoul", "Gangnam", "ISP Corp");

            assertEquals("1.2.3.4", info.ip());
            assertEquals("KR", info.country());
            assertEquals("Seoul", info.region());
            assertEquals("Gangnam", info.city());
            assertEquals("ISP Corp", info.organization());
        }

        @Test
        @DisplayName("ip가 null이면 IllegalArgumentException 발생")
        void shouldThrowWhenIpIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new IpInfo(null, "KR", "Seoul", "Gangnam", "ISP"));
        }

        @Test
        @DisplayName("ip가 blank이면 IllegalArgumentException 발생")
        void shouldThrowWhenIpIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    new IpInfo("  ", "KR", "Seoul", "Gangnam", "ISP"));
        }

        @Test
        @DisplayName("country가 null이면 UNKNOWN으로 치환된다")
        void shouldReplaceNullCountryWithUnknown() {
            var info = new IpInfo("1.2.3.4", null, "Seoul", "Gangnam", "ISP");
            assertEquals("UNKNOWN", info.country());
        }

        @Test
        @DisplayName("region이 blank이면 UNKNOWN으로 치환된다")
        void shouldReplaceBlankRegionWithUnknown() {
            var info = new IpInfo("1.2.3.4", "KR", "  ", "Gangnam", "ISP");
            assertEquals("UNKNOWN", info.region());
        }

        @Test
        @DisplayName("모든 선택 필드가 null이면 전부 UNKNOWN으로 치환된다")
        void shouldReplaceAllNullFieldsWithUnknown() {
            var info = new IpInfo("1.2.3.4", null, null, null, null);

            assertEquals("UNKNOWN", info.country());
            assertEquals("UNKNOWN", info.region());
            assertEquals("UNKNOWN", info.city());
            assertEquals("UNKNOWN", info.organization());
        }
    }

    @Nested
    @DisplayName("팩터리 메서드 검증")
    class FactoryMethodTest {

        @Test
        @DisplayName("of()는 compact constructor와 동일하게 동작한다")
        void ofShouldDelegateToConstructor() {
            var info = IpInfo.of("1.2.3.4", null, "Seoul", null, null);

            assertEquals("1.2.3.4", info.ip());
            assertEquals("UNKNOWN", info.country());
            assertEquals("Seoul", info.region());
            assertEquals("UNKNOWN", info.city());
            assertEquals("UNKNOWN", info.organization());
        }

        @Test
        @DisplayName("of()에서 ip가 null이면 IllegalArgumentException 발생")
        void ofShouldThrowWhenIpIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    IpInfo.of(null, "KR", "Seoul", "Gangnam", "ISP"));
        }

        @Test
        @DisplayName("unknown()은 모든 필드가 UNKNOWN인 IpInfo를 반환한다")
        void unknownShouldReturnAllUnknown() {
            var info = IpInfo.unknown("1.2.3.4");

            assertEquals("1.2.3.4", info.ip());
            assertEquals("UNKNOWN", info.country());
            assertEquals("UNKNOWN", info.region());
            assertEquals("UNKNOWN", info.city());
            assertEquals("UNKNOWN", info.organization());
        }
    }

    @Nested
    @DisplayName("isValid 검증")
    class IsValidTest {

        @Test
        @DisplayName("country가 UNKNOWN이 아니면 유효하다")
        void shouldBeValidWhenCountryIsKnown() {
            var info = IpInfo.of("1.2.3.4", "KR", null, null, null);
            assertTrue(info.isValid());
        }

        @Test
        @DisplayName("country가 UNKNOWN이면 유효하지 않다")
        void shouldBeInvalidWhenCountryIsUnknown() {
            var info = IpInfo.unknown("1.2.3.4");
            assertFalse(info.isValid());
        }
    }
}
