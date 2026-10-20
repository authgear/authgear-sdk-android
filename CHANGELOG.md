# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `AuthenticateOptions.resource` is an optional `resource` (RFC 8707 resource
  indicator). Access tokens of the resulting session, including
  refreshed ones, have `aud` set to that resource URI.
- `AuthenticateOptions.scopes` requests the scopes of the API resource given in
  `resource`. They are merged with the scopes required by the SDK, so the
  scopes required by the SDK are always requested.
- `Authgear.configure` takes optional `resource` and `scopes`. They are the
  defaults of every call that takes them, used only when the call does not
  give its own, which replaces them. The scopes required by the SDK are always
  merged in. `resource` is never sent for biometric setup, and no default is
  applied to the app2app request handled by the authenticated app.
- `authenticateAnonymously`, `PromoteOptions`, and `BiometricOptions` take
  optional `resource` and `scopes`.
- `App2AppAuthenticateOptions` and `App2AppAuthenticateRequest` take an optional
  `resource`. The authenticating app sends it in the app2app authentication
  request, and the authenticated app passes it on unchanged.
- `PreAuthenticatedURLOptions.resource` is an optional `resource` of the new
  session of `makePreAuthenticatedURL`.

## [4.0.0] - 2026-09-03

### Changed

- **Breaking:** `AuthgearDelegate.onSessionStateChanged` now takes an additional
  `error: Throwable?` parameter, so implementations that override it must be
  updated. It is non-null whenever the session was cleared because a request
  failed (e.g. `invalid_grant` or `invalid_dpop_proof`), and null for all other
  reasons. (#235, ref DEV-3680)

### Added

- `UserInfo` now exposes `authenticators` and `recoveryCodeEnabled`, backed by
  new `Authenticator`, `AuthenticatorKind`, and `AuthenticatorType` types,
  including a passkey authenticator type and forward-compatible handling of
  unknown authenticator kinds/types added on the server in the future.
  (ref DEV-3027)

### Fixed

- The SDK now clears the session on `invalid_dpop_proof` during token refresh,
  not just `invalid_grant`. Previously a restored refresh token that no longer
  matched the DPoP key (e.g. after a device-backup restore) left `sessionState`
  stuck at `authenticated` with refresh retrying forever. (#235, ref DEV-3680)

### Deprecated

- `Page.IDENTITY` is deprecated in favor of `Page.SETTINGS` combined with
  `changeEmail`/`changePhone`. Existing callers continue to work. (#234)

## [3.0.0] - 2025-11-05

See git history prior to this file for changes before 4.0.0.
