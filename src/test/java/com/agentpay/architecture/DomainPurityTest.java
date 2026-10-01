package com.agentpay.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 헥사고날 규칙 강제: domain 패키지는 Spring/JPA에 의존하지 않는다.
 * (ArchUnit 도입 전까지 소스 import를 직접 검사)
 */
class DomainPurityTest {

    private static final List<String> FORBIDDEN = List.of(
            "import org.springframework", "import jakarta.persistence", "import jakarta.transaction",
            "import org.hibernate");

    @Test
    void domainHasNoFrameworkImports() throws IOException {
        Path root = Path.of("src/main/java/com/agentpay");
        try (Stream<Path> files = Files.walk(root)) {
            List<String> violations = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().contains("/domain/"))
                    .flatMap(p -> {
                        try {
                            return Files.readAllLines(p).stream()
                                    .filter(line -> FORBIDDEN.stream().anyMatch(line::startsWith))
                                    .map(line -> root.relativize(p) + ": " + line);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            assertThat(violations).isEmpty();
        }
    }
}
