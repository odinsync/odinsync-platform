package com.odinsync.organization.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odinsync.organization.domain.exception.InvalidOrganizationValueException;
import org.junit.jupiter.api.Test;

class WebsiteTest {

	@Test
	void acceptsHttpAndHttpsUrlsAndTrimsWhitespace() {
		assertThat(Website.of(" http://example.com ").value()).contains("http://example.com");
		assertThat(Website.of("HTTPS://example.com").value()).contains("HTTPS://example.com");
	}

	@Test
	void normalizesNullAndBlankToAbsent() {
		assertThat(Website.of(null).value()).isEmpty();
		assertThat(Website.of(" ").value()).isEmpty();
		assertThat(Website.empty().value()).isEmpty();
	}

	@Test
	void rejectsInvalidUrls() {
		assertThatThrownBy(() -> Website.of("example.com"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> Website.of("ftp://example.com"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> Website.of("javascript:alert(1)"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> Website.of("https://user:pass@example.com"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> Website.of("https://"))
				.isInstanceOf(InvalidOrganizationValueException.class);
		assertThatThrownBy(() -> Website.of("https://exa mple.com"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void enforcesMaximumLength() {
		String host = "https://" + "a".repeat(488) + ".com";
		assertThat(host).hasSize(500);
		assertThat(Website.of(host).value()).contains(host);
		assertThatThrownBy(() -> Website.of(host + "x"))
				.isInstanceOf(InvalidOrganizationValueException.class);
	}

	@Test
	void usesValueBasedEqualityForPresentAndAbsentValues() {
		assertThat(Website.of("https://example.com")).isEqualTo(Website.of("https://example.com"));
		assertThat(Website.of(null)).isEqualTo(Website.of(" "));
	}
}
