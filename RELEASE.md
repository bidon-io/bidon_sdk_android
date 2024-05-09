# Publishing

Bidon versions are described in the [Versions.kt](./buildSrc/src/main/kotlin/Versions.kt).
Publication settings are described in the [publish-adapter.gradle.kts](./buildSrc/src/main/kotlin/publish-adapter.gradle.kts).

## Publication to the `bidon-private` repository is done using the following command:

```shell
./gradlew publishAllPublicationsToBidonRepository -Prepo='bidon-private' -Puname='USER' -Pupassword='PASSWORD'
```

## Publication to the public `bidon` repository is done using the following command:

```shell
./gradlew publishAllPublicationsToBidonRepository -Prepo='bidon' -Puname='USER' -Pupassword='PASSWORD'
```

# Release process

1. Update the version in the [Versions.kt](./buildSrc/src/main/kotlin/Versions.kt) file 
2. Commit and push the changes to `release` branch
3. Run the publication command
4. Create a tag with the version number
5. Push the tag to the `main` branch
6. Update `Docasaurus` with the new version
7. Send a message to the team to announce the new version

# Additional information

- [BDN-46](https://appodeal.atlassian.net/browse/BDN-46) SDK Publishing
- [BDN-214](https://appodeal.atlassian.net/browse/BDN-214) Bidon Artifactory
- [BDN-520](https://appodeal.atlassian.net/browse/BDN-520) MavenCentral (Sonatype) + GooglePlaySDK Console