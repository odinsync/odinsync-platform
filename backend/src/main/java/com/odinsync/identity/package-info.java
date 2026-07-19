/**
 * Identity and access management bounded context.
 *
 * <p>This module owns tenant authentication, organization registration,
 * credential verification, access-token issuance, refresh-token sessions,
 * role assignment, and user identity state. Other bounded contexts should
 * depend on explicit application contracts rather than reaching into Identity
 * persistence or security internals.
 */
package com.odinsync.identity;
