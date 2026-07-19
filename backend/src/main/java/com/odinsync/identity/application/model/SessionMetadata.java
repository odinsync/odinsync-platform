package com.odinsync.identity.application.model;

public record SessionMetadata(
		String deviceName,
		String userAgent,
		String ipAddress) {

	private static final int DEVICE_NAME_LENGTH = 255;
	private static final int USER_AGENT_LENGTH = 512;
	private static final int IP_ADDRESS_LENGTH = 64;

	/**
	 * Normalizes optional session metadata before it is persisted for audit/display.
	 */
	public SessionMetadata {
		deviceName = normalize(deviceName, DEVICE_NAME_LENGTH);
		userAgent = normalize(userAgent, USER_AGENT_LENGTH);
		ipAddress = normalize(ipAddress, IP_ADDRESS_LENGTH);
	}

	/**
	 * Creates an empty metadata value for flows where request details are unavailable.
	 */
	public static SessionMetadata empty() {
		return new SessionMetadata(null, null, null);
	}

	/**
	 * Trims blank metadata to null and truncates values to their database limits.
	 */
	private static String normalize(String value, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
	}
}
