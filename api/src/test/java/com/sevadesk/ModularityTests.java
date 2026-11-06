package com.sevadesk;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails the build if one module reaches into another's internals or modules depend on each other in a cycle. */
class ModularityTests {

    @Test
    void moduleBoundariesAreRespected() {
        ApplicationModules.of(SevaDeskApplication.class).verify();
    }
}
