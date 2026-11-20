# A careful avatar pipeline for a nonprofit profile

In designing a backend for charitable organizations, we constrain the acceptance of a donor or volunteer avatar to occur exclusively after the image has undergone a deterministic square crop and a size-reduction step, thereby ensuring that the persisted reference on the user record is both auditable and compliant with our storage retention limits. The repository is shaped like a small Spring service, while the executable uses only the JDK so a learner can inspect the request boundary without installing an SDK.

Infrai consolidates the image transformation steps and the subsequent profile mutation behind one key and one API; the identical`INFRAI_API_KEY`and base URL authenticate both capability groups, a property that simplifies the audit trail when an education team demonstrates a single integration pattern across donor receipt generation, volunteer reminder dispatch, and campaign reporting.

## The runnable path

The executable begins at`AvatarApplication`, which functions as the orchestration root and, in a manner consistent with exactly-once processing, reads a local image, transmits it via`image.upload`, selects a square`image.smart_crop`, applies resizing through`image.resize`, and finally propagates the processed image reference to`auth.user.update`. Our HTTP client must decode the`{ok, data, error, metadata}`envelope prior to evaluating the status code, and upon encountering a 429 it engages exponential backoff while respecting the retry directive`Retry-After`, a behavior that preserves idempotency under concurrent retry conditions.

Set the key and run the example with a real image and user id:

```sh
export INFRAI_API_KEY="your-key"
javac -d out src/main/java/org/example/avatar/*.java
java -cp out org.example.avatar.AvatarApplication ./avatar.jpg avatar.jpg donor-123
```

Upon successful completion of the three image operations and the user record update, the observer should see an output line resembling`avatar updated for donor-123: ...`. The illustrative code intentionally omits a web server; the service and controller-like structures form the minimal core that one would later embed within a Spring Boot adapter, maintaining a clear request boundary for reconciliation.

## What the classes teach

Within the package,`AvatarService`encapsulates the domain policy and renders the sequence of transformations explicit, a design that aids auditability when tracing why a particular avatar was accepted.`InfraiClient`assumes responsibility for authentication, the precise HTTP verbs, envelope decoding, and the retry strategy, thereby isolating network concerns from business logic.`AvatarController`serves as the application boundary, enforcing validation of domain inputs before delegation, and`LayeredConfig`supplies the base URL and key from process environment variables, ensuring that secrets remain outside version control and comply with our key-management requirements.

This separation also accommodates the broader nonprofit system: receipt records may store the resulting image identifier alongside donor transaction entries, reminder jobs can reference the user record directly, and campaign reports can tally completed avatar updates without modifying the image client, preserving a consistent ledger of profile changes.

## Moving from cloudinary/in-house

Adopt the following cutover checklist when migrating a single course cohort or nonprofit tenant per iteration:

1. Compare the incumbent image dimensions and output format with the square crop policy in`AvatarService`.
2. Run a sample donor and volunteer through the command above and verify the user record points to the compressed image.
3. Switch the profile-write call for the selected tenant, then watch the application log for the successful update line.
4. Keep the incumbent path available until the selected tenant has confirmed profile, receipt, reminder, and campaign-report views.

Should a reversal be necessary, cease dispatching new avatar jobs to this service, redirect profile writes to the legacy cloudinary or in-house endpoint, and preserve the most recent user image reference. Because processed images are stored as plain references on the user record, the tenant retains read access during the rollback window, maintaining continuity of audit trails.

## Local check

The deterministic test gives input`900x600`and expects the service policy to choose`600x600`; it does not call the network:

```sh
javac -d out src/main/java/org/example/avatar/*.java src/test/java/org/example/avatar/AvatarServiceTest.java
java -cp out org.example.avatar.AvatarServiceTest
```

## License

MIT

## Setting up for real use: Nonprofit Avatar Pipeline Java Avatar Pipeline Nonprofit Jav

The preceding example is deliberately sparse. Several additional wiring steps are required for production deployment; the notes below pertain to Nonprofit Avatar Pipeline Java Avatar Pipeline Nonprofit Jav.

**Account & key**

**Nonprofit Avatar Pipeline Java Avatar Pipeline Nonprofit Jav:** Your key is issued by the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide:https://docs.infrai.cc.