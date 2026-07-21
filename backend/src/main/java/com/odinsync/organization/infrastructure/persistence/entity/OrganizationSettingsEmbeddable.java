package com.odinsync.organization.infrastructure.persistence.entity;

import com.odinsync.organization.domain.model.DateFormat;
import com.odinsync.organization.domain.model.TimeFormat;
import com.odinsync.organization.domain.model.WeekStart;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class OrganizationSettingsEmbeddable {

	@Column(name = "currency_code", length = 3, nullable = false)
	private String currencyCode;

	@Column(name = "time_zone", length = 100, nullable = false)
	private String timeZone;

	@Column(name = "locale", length = 20, nullable = false)
	private String locale;

	@Enumerated(EnumType.STRING)
	@Column(name = "date_format", length = 30, nullable = false)
	private DateFormat dateFormat;

	@Enumerated(EnumType.STRING)
	@Column(name = "time_format", length = 20, nullable = false)
	private TimeFormat timeFormat;

	@Enumerated(EnumType.STRING)
	@Column(name = "week_start", length = 15, nullable = false)
	private WeekStart weekStart;
}
