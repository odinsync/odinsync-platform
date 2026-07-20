package com.odinsync.organization.domain.valueobject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;

public final class Website {

	private static final int MAX_LENGTH = 500;
	private static final Website EMPTY = new Website(null);

	private final String value;

	private Website(String value) {
		this.value = value;
	}

	public static Website of(String value) {
		String normalized = OrganizationValueValidator.optionalTrimmed(value, "website", MAX_LENGTH);
		if (normalized == null) {
			return EMPTY;
		}
		validate(normalized);
		return new Website(normalized);
	}

	public static Website empty() {
		return EMPTY;
	}

	public Optional<String> value() {
		return Optional.ofNullable(value);
	}

	public boolean isPresent() {
		return value != null;
	}

	private static void validate(String value) {
		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			if (scheme == null || !isHttpScheme(scheme)) {
				throw new InvalidOrganizationValueException("website must use http or https");
			}
			if (uri.getUserInfo() != null) {
				throw new InvalidOrganizationValueException("website must not include credentials");
			}
			if (uri.getHost() == null || uri.getHost().isBlank()) {
				throw new InvalidOrganizationValueException("website must include a host");
			}
		} catch (URISyntaxException exception) {
			throw new InvalidOrganizationValueException("website must be a valid URI");
		}
	}

	private static boolean isHttpScheme(String scheme) {
		String normalized = scheme.toLowerCase(Locale.ROOT);
		return normalized.equals("http") || normalized.equals("https");
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof Website that
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
