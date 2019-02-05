# Android WebAuthn Authenticator Library

This library is meant to serve as an example implementation of the [WebAuthn
authenticator model](https://www.w3.org/TR/webauthn/#sctn-authenticator-model).
While the specification is currently in Candidate Recommendation, this library
conforms as much as possible to the guidelines and implementation procedures
outlined by the document.

This implementation currently requires Android API level 28 (Android 9.0) due
to the use of the 
[BiometricPrompt](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt).

## Quickstart

You can use [JitPack](https://jitpack.io/) to include this module in your Android project, or you can include the source code.

### Using JitPack

Add this in your root build.gradle:

```groovy
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```
Add this to your dependencies list:

```groovy
    dependencies {
        implementation 'com.github.duo-labs:android-webauthn-authenticator:master-SNAPSHOT'
    }
```

### Using Source

#### Pull the source

```
$ cd ~/your/project/src/directory
$ git clone git@github.com:duo-labs/android-webauthn-authenticator.git
```

#### Add the module to your Android project

In Android Studio: `File -> New -> Import Module` and then point it at the `android-webauthn-authenticator` directory.

#### Add the module as a dependency

In Android Studio: `File -> Project Structure -> App -> Dependencies -> + -> Module Dependency`

Select the `android-webauthn-authenticator` module. After a Gradle sync, you should be able to use the
`duo.labs.webauthn` package.

## Usage

You must first instantiate an `Authenticator` object.

```java
// Authenticator(Context ctx, boolean authenticationRequired, boolean strongboxRequired)
Authenticator authenticator = new Authenticator(context, true, true);
```
The `Authenticator` object is safe to instantiate multiple times.

The arguments passed to the constructor determine whether the keys it generates will
require biometric  authentication (i.e. can be turned off for testing) and if keys should
be stored by the [StrongBox Keymaster](https://developer.android.com/training/articles/keystore).

Note that StrongBox is only available on some Android devices.

### Make Credential (User Registration)

You can create a new credential by passing an `AuthenticatorMakeCredentialOptions` object to
`Authenticator.makeCredential()`. You can instantiate an `AuthenticatorMakeCredentialOptions`
object directly and manually set its fields, or use our JSON format.

Our JSON format mostly tracks the arguments to [authenticatorMakeCredential](https://www.w3.org/TR/webauthn/#op-make-cred)
from the WebAuthn specification, with a few changes necessary for the serialization of binary data. Here's an example:

```javascript
{
    "authenticatorExtensions": "", // optional and currently ignored
    "clientDataHash": "LTCT/hWLtJenIgi0oUhkJz7dE8ng+pej+i6YI1QQu60=", // base64
    "credTypesAndPubKeyAlgs": [
        ["public-key", -7]
    ],
    "excludeCredentials": [
        {
            "type": "public-key",
            "id": "lVGyXHwz6vdYignKyctbkIkJto/ADbYbHhE7+ss/87o=" // base64
            // "transports" member optional but ignored
        }
    ],
    "requireResidentKey": true,
    "requireUserPresence": false,
    "requireUserVerification": true,
    "rp": {
        "name": "webauthn.io",
        "id": "webauthn.io"
    },
    "user": {
        "name": "testuser",
        "displayName": "Test User",
        "id": "/QIAAAAAAAAAAA==" // base64
    }
}
```

Note that `requireResidentKey` and `requireUserPresence` are effectively ignored: keys are resident by design, and user presence will always be verified. User verification will always be performed if the `Authenticator` is instantiated with `authenticationRequired` set to `true`; otherwise biometric authentication will not be performed and credential generation will fail if `requireUserVerification` is `true`.

(Per the spec, `requireUserPresence` must be the inverse of `requireUserVerification`)

Create the options object from JSON:

```java
AuthenticatorMakeCredentialOptions makeCredentialOptions = AuthenticatorMakeCredentialOptions.fromJSON(options);
```

Then, make a new credential with the options given.

```java
AttestationObject attestationObject = authenticator.makeCredential(makeCredentialOptions);
// or if you want to require user verification and need the biometric dialog:
AttestationObject attestationObject = authenticator.makeCredential(makeCredentialOptions, context, cancellationSignal);
```

`makeCredential` requires an application context in order to show the 
[BiometricPrompt](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt), and
also accepts an optional [CancellationSignal](https://developer.android.com/reference/android/os/CancellationSignal)
to allow user-initiated cancellation.

Once you have an `AttestationObject`, you can also retrieve its CBOR representation as follows:

```java
byte[] attestationObjectBytes = attestationObject.asCBOR();
```

### Get Assertion (User Login)

Similar to `makeCredential`, `getAssertion` takes an `AuthenticatorGetAssertionOptions` object
which you can either instantiate manually or deserialize from JSON.

The JSON format follows [authenticatorGetAssertion](https://www.w3.org/TR/webauthn/#op-get-assertion) with
some changes made for handling of binary data. Here's an example:

```javascript
{
    "allowCredentialDescriptorList": [{
        "id": "jVtTOKLHRMN17I66w48XWuJadCitXg0xZKaZvHdtW6RDCJhxO6Cfff9qbYnZiMQ1pl8CzPkXcXEHwpQYFknN2w==", // base64
        "type": "public-key"
    }],
    "authenticatorExtensions": "", // optional and ignored
    "clientDataHash": "BWlg/oAqeIhMHkGAo10C3sf4U/sy0IohfKB0OlcfHHU=", // base64
    "requireUserPresence": true,
    "requireUserVerification": false,
    "rpId": "webauthn.io"
}
```

Create the options object from JSON:

```java
AuthenticatorGetAssertionOptions getAssertionOptions = AuthenticatorGetAssertionOptions.fromJSON(options);
```

Step 7 of [authenticatorGetAssertion](https://www.w3.org/TR/webauthn/#op-get-assertion) requires that
the authenticator prompt a credential selection. You can use our provided `SelectCredentialDialogFragment`
to provide an interface for user-selection, or implement the `CredentialSelector` interface to receive a
callback when it is time to select a credential.

#### Programmatic Credential Selection

If you want to programatically select credentials, you'll need to implement `CredentialSelector`, which is a simple interface:

```java
public interface CredentialSelector {
    public PublicKeyCredentialSource selectFrom(List<PublicKeyCredentialSource> credentialList);
}
```

Here's a barebones example:

```java
AuthenticatorGetAssertionResult assertionObject = authenticator.getAssertion(getAssertionOptions, new CredentialSelector() {
    @Override
    public PublicKeyCredentialSource selectFrom(List<PublicKeyCredentialSource> credentialList) {
        return credentialList.get(0);
    }
});
```

#### User-driven Credential Selection

You can also create a credential selector dialog by using the `SelectCredentialDialogFragment`
helper class, which takes a [DialogFragment](https://developer.android.com/reference/android/app/DialogFragment):

```java
SelectCredentialDialogFragment credentialSelector = new SelectCredentialDialogFragment();
credentialSelector.populateFragmentActivity(fragmentActivity);
AuthenticatorGetAssertionResult assertionObject = authenticator.getAssertion(options, credentialSelector, context, cancellationSignal);
```

The `fragmentActivity` supplied should be the main 
[Activity](https://developer.android.com/reference/android/app/Activity) 
with which the user is currently interacting.

As with the `makeCredential` operation, in the user-driven case, `getAssertion` requires an application context in order to show the 
[BiometricPrompt](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt)
and accepts an optional 
[CancellationSignal](https://developer.android.com/reference/android/os/CancellationSignal)
to allow user-initiated cancellation.
