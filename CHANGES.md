### Version 1.0.0
* Initial open-source release of the Lending Partner Gateway Java SDK.
* Removed stdout logging of request and response payloads; excluded credentials and
  verification secrets from `toString()` output.
* Moved `KeyUtil` to `com.amazon.lending.security.util`; the previous `main.java.…`
  package is retained as a deprecated alias and will be removed in 2.0.
* Removed the dependency on `javax.xml.bind.DatatypeConverter` so the SDK runs on
  Java 11 and later. Derived key ids are unchanged (uppercase hex MD5).
* Resource IDs are checked before they are put into a URL path: anything other than
  1 to 128 characters from `[A-Za-z0-9._-]`, starting with a letter or digit, is
  rejected with `LPGInvalidRequestException` before a request is made. An `update*`
  call whose model has a `null` ID now fails this way instead of being sent to the
  collection URL, which the gateway refused with 403.
* The PS256 JWS in every request body carries `iat`, `nbf` (= `iat`), `exp` (`iat` + 300 s)
  and a fresh `jti` for every request, and the header `typ: "lpg-req+jwt"`. LPG may reject
  a request whose claims are missing, expired, not yet valid, or reused; keep your clock
  synchronised (NTP).
  The `payload` claim and the RS256 bank-account `secureHash` are unchanged.
  If you sign requests with your own code instead of the SDK, sign every retry afresh:
  a retried request needs a new `jti`. The SDK already signs each call afresh.
* Request signing is thread-safe, and signing dates no longer depend on the JVM's default locale.
* `createLenderPortfolioData` sends the request's `createdBy`, which was dropped before.
* The HTTP client is google-http-client 1.47.1; the earlier SDK zip was built with 1.19.
  The message of an `LPGRequestException` now also contains the request method and URL.
  For example, the 403 for an expired offer (`LPGForbiddenException`):
  ```
  1.19:    403 Forbidden
           {"message":"Resource OFFER has expired"}
  1.47.1:  403 Forbidden
           GET https://lending-na-beta.amazonservices.com/v1.1/offers/amzn1.lending.offer.us.qphpgc…
           {"message":"Resource OFFER has expired"}
  ```
  Match on the exception type and `getStatusCode()`, not on the message text.
* Dependencies: guava 30.1.1-jre → 33.7.2-jre; jackson-core 2.18.12 is now a direct
  dependency, so it replaces the 2.18.2 that google-http-client-jackson2 brings into your
  project. The new versions fix published security advisories.
* Added the first unit tests (key parsing, key-id derivation, provider round-trips,
  shim delegation, and a JAXB-absence guard).
