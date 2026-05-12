# Deployment

To deploy a new version of the mod we have to

1. Increase the mod version => see [gradle.properties] > mod_version
2. Push the new version to master

The CI/CD pipeline will build and publish a new release to github.

3. Publish the new release manually to [modrinth]


[gradle.properties]: /gradle.properties
[modrinth]: https://modrinth.com/mod/redstone-wire
