package com.odinsync.organization.application.port.out;

import java.time.Instant;

public interface TimeProvider {

	Instant now();
}
