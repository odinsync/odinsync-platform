package com.odinsync.organization.domain.valueobject;

import java.util.Objects;
import java.util.Optional;

public final class TaxRegistrationNumber {

	private static final int MAX_LENGTH = 50;
	private static final TaxRegistrationNumber EMPTY = new TaxRegistrationNumber(null);

	private final String value;

	private TaxRegistrationNumber(String value) {
		this.value = value;
	}

	public static TaxRegistrationNumber of(String value) {
		String normalized = OrganizationValueValidator.optionalTrimmed(value, "taxRegistrationNumber", MAX_LENGTH);
		return normalized == null ? EMPTY : new TaxRegistrationNumber(normalized);
	}

	public static TaxRegistrationNumber empty() {
		return EMPTY;
	}

	public Optional<String> value() {
		return Optional.ofNullable(value);
	}

	public boolean isPresent() {
		return value != null;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof TaxRegistrationNumber that
				&& Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value().orElse("");
	}
}
