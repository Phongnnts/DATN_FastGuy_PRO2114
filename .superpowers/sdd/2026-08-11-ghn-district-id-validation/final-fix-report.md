# Final fix report

## Status

DONE

## Finding resolved

Replaced source-text policy checks with runtime servlet regression tests for POST and PUT. Both send a valid address payload except `ghnDistrictId: "abc"`, assert HTTP 400, assert response contains exact `Quan/huyen GHN khong hop le`, and assert zero `AddressService.create`/`AddressService.update` calls.

## Files

- Modified `Backend/FastGuy-FastFoodSite/src/test/java/servlet/AddressValidationPolicyTest.java`
- Added `.superpowers/sdd/2026-08-11-ghn-district-id-validation/final-fix-report.md`
- Production code unchanged.
- Dependencies unchanged.

## Tests and commands

### Focused servlet regression

Command:

`mvn "-Dtest=AddressValidationPolicyTest" test`

Exact result:

- `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Combined validator and servlet regression

Command:

`mvn "-Dtest=AddressValidatorTest,AddressValidationPolicyTest" test`

Exact result:

- `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend suite

Command:

`mvn test`

Exact result:

- `Tests run: 174, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Diff validation

Command:

`git diff --check`

Exact result: no output; exit code 0.

## Commits

- Fix commit: `93ee4c6 test(address): exercise validation HTTP contract`
- Report commit: recorded by commit containing this report.

## Concerns

None.
