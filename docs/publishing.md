# Publishing crappy-java

This guide sets up a release from GitHub Actions. It does not put publishing
credentials in the repository or in Gradle project files.

## What publishes where

| Component | Public name | Destination |
|---|---|---|
| Core library | `com.larseckart:crappy-java-core` | Maven Central |
| Gradle plugin | `com.larseckart.crappy-java` | Gradle Plugin Portal |

The plugin has a runtime dependency on the core library. The release workflow
publishes the core library first and waits until Maven Central serves it before
it publishes the plugin.

## One-time account setup

### Maven Central

1. Create a Central Portal account.
2. Claim and verify the `com.larseckart` namespace.
3. On the Central Portal **Account** page, create a **User Token**. This gives
   a token username and token password. They are not the password used to sign
   in to Central Portal.
4. Use the shared Java release-signing key for `crappy-java` and
   `object-calisthenics-analyzer`. The old `tcr-extension` key expired in July
   2025; keep it for checking older releases, but do not use it to sign new ones.

The current key has fingerprint `45353912409CF4BE37320C1FE012009D3EE97E82`
and expires **2028-09-23 (UTC)**. Its ASCII-armored private key and passphrase
are in 1Password: **Private → Java release signing — object-calisthenics-analyzer + crappy-java**.
Only its public key is on `keyserver.ubuntu.com`. To check the public key:

```bash
curl --fail 'https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x45353912409CF4BE37320C1FE012009D3EE97E82' | gpg --import
gpg --fingerprint 45353912409CF4BE37320C1FE012009D3EE97E82
```

Export the private key in ASCII-armored form when you need to add or rotate the
secret:

```bash
gpg --export-secret-keys --armor <key-id>
```

Keep the whole output, including the `BEGIN` and `END` lines. Do not base64
encode it for this project.

### Gradle Plugin Portal

1. Create a Plugin Portal account at <https://plugins.gradle.org/>.
2. Open the profile's **API Keys** page and create an API key.
3. Save both the publish key and publish secret.

The Portal reviews a new plugin ID before it makes the plugin available. The
first release can therefore publish successfully while the final smoke test
cannot yet resolve the plugin. After the Portal approves the ID, rerun only the
failed smoke job from the GitHub Actions page.

## Store each secret twice

Use a password manager as the long-term source of truth, then copy the values
to GitHub repository secrets. GitHub does not sync secrets back to the password
manager, so update both places when you rotate a credential.

For this repository, use these GitHub repository-secret names:

| GitHub secret | Value to copy | Used by |
|---|---|---|
| `MAVEN_CENTRAL_USERNAME` | Maven Central User Token username | Maven Central upload |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central User Token password | Maven Central upload |
| `SIGNING_IN_MEMORY_KEY` | ASCII-armored PGP private key | Artifact signing |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | PGP key passphrase | Artifact signing |
| `GRADLE_PUBLISH_KEY` | Plugin Portal API key | Portal validation and publish |
| `GRADLE_PUBLISH_SECRET` | Plugin Portal API secret | Portal validation and publish |

Add them in **GitHub → Settings → Secrets and variables → Actions**, or with
`gh secret set <NAME>`. Never commit a secret, add it to `gradle.properties`,
or paste it into an issue, release note, or build log.

## Release flow

1. Push the release-ready commit to `main`.
2. Before making a release, run **Actions → Release → Run workflow** on `main`.
   This preflight builds, signs, and checks the Maven artifacts against the
   public key from Ubuntu's keyserver. It does not publish anything. Check that
   its `validate` job passed.
3. Create a GitHub release with a final version tag, such as `v0.1.0`.
4. The `.github/workflows/release.yml` workflow:
   1. builds the project and validates the Plugin Portal upload;
   2. publishes signed core artifacts to Maven Central;
   3. waits until Maven Central serves the core POM;
   4. publishes the Gradle plugin; and
   5. runs a fresh Gradle build that uses only normal Plugin Portal resolution.

The tag controls the published version: the workflow removes a leading `v`.
It rejects versions ending in `-SNAPSHOT`.

## Local work

Normal local work needs no publishing credentials:

```bash
./gradlew build
```

The GitHub-hosted release workflow reads the repository secrets only while it
runs. A local maintainer may validate or publish manually by passing the same
values as environment variables, but that is optional and should not leave
credentials in a file.

After a successful release, you can repeat the consumer check locally:

```bash
./scripts/smoke-released-plugin.sh 0.1.0
```

The script creates and removes a temporary Gradle project. It has no
`pluginManagement` block, so it verifies the default Plugin Portal path.
