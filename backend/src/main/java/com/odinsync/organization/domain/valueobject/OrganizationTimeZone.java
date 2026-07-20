package com.odinsync.organization.domain.valueobject;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

public record OrganizationTimeZone(String value) {

	public OrganizationTimeZone {
		String normalized = OrganizationValueValidator.requiredTrimmed(value, "timeZone", 100);
		try {
			ZoneId zoneId = ZoneId.of(normalized);
			if (zoneId instanceof ZoneOffset) {
				throw new InvalidOrganizationValueException("timeZone must be an IANA region identifier");
			}
			value = zoneId.getId();
		} catch (DateTimeException exception) {
			throw new InvalidOrganizationValueException("timeZone must be a valid IANA region identifier");
		}
	}
}
