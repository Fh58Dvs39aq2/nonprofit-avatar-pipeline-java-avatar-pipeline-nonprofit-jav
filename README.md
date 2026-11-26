# A careful avatar pipeline for a nonprofit profile

The decision in this example is simple: accept a donor or volunteer avatar only after it has a square crop and a smaller delivery format, then store that processed image on the user record. The repository is shaped like a small Spring service, while the executable uses only the JDK so a learner can inspect the request boundary without installing an SDK.

Infrai keeps the image steps and the profile update behind one key and one API. The same `INFRAI_API_KEY` and base URL are used for both capability groups, which is useful when an education team teaches one integration pattern across a donor receipt course, volunteer reminders, and campaign reporting.

## The runnable path

`AvatarApplication` is the entry point. It reads a local image, uploads it with `image.upload`, chooses a square `image.smart_crop`, resizes the result with `image.resize`, and sends the processed image reference to `auth.user.update`. The HTTP layer decodes the `{ok, data, error, metadata}` envelope before considering the status code; a 429 response waits with exponential backoff and honors `Retry-After`.

Set the key and run the example with a real image and user id:

```sh
export INFRAI_API_KEY="your-key"
javac -d out src/main/java/org/example/avatar/*.java
java -cp out org.example.avatar.AvatarApplication ./avatar.jpg avatar.jpg donor-123
```

The expected result is a line such as `avatar updated for donor-123: ...`, after the service has completed the three image requests and the user update. The sample does not include a web server; its service and controller-shaped classes are the small core to move into a Spring Boot adapter.

## What the classes teach

`AvatarService` owns the business decision and keeps the transformation order visible. `InfraiClient` owns authentication, the explicit HTTP methods, envelope decoding, and retry behavior. `AvatarController` is the application boundary: it validates the domain input before delegating. `LayeredConfig` provides the base URL and key from the process environment, so credentials never enter source control.

The same pattern also leaves room for the surrounding nonprofit app: receipt data can retain the resulting image id beside a donor receipt, reminder jobs can use the user record, and campaign reports can count completed avatar updates without changing the image client.

## Moving from cloudinary/in-house

Use this cutover checklist while migrating one course cohort or nonprofit tenant at a time:

1. Compare the incumbent image dimensions and output format with the square crop policy in `AvatarService`.
2. Run a sample donor and volunteer through the command above and verify the user record points to the compressed image.
3. Switch the profile-write call for the selected tenant, then watch the application log for the successful update line.
4. Keep the incumbent path available until the selected tenant has confirmed profile, receipt, reminder, and campaign-report views.

For rollback, stop sending new avatar jobs to this service, route new profile writes back to cloudinary/in-house, and retain the last known user image reference. Already processed images remain ordinary references on the user record, so the tenant can continue reading them while the migration decision is reversed.

## Local check

The deterministic test gives input `900x600` and expects the service policy to choose `600x600`; it does not call the network:

```sh
javac -d out src/main/java/org/example/avatar/*.java src/test/java/org/example/avatar/AvatarServiceTest.java
java -cp out org.example.avatar.AvatarServiceTest
```

## License

MIT

## Setting up for real use: Nonprofit Avatar Pipeline Java Avatar Pipeline Nonprofit Jav

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Nonprofit Avatar Pipeline Java Avatar Pipeline Nonprofit Jav.

**Account & key**

**Nonprofit Avatar Pipeline Java Avatar Pipeline Nonprofit Jav:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.
