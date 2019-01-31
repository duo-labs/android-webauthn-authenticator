# Android Webauthn Authenticator Library

This library is meant to serve as an example implementation of the [WebAuthn
authenticator model](https://www.w3.org/TR/webauthn/#sctn-authenticator-model).
While the specification is currently in Candidate Recommendation, this library
conforms as much as possible to the guidelines and implementation procedures
outlined by the document.

This implementation currently requires Android API level 28 (Android 9.0) due
to the use of the 
[BiometricPrompt](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt).

## Quickstart

Use [JitPack](https://jitpack.io/) to include this repository in your Android
project.

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

## Usage

You must first instantiate an `Authenticator` object.

```java
Authenticator authenticator = new Authenticator(context, true, true);
```
The `Authenticator` object is safe to instantiate multiple times.

It allows specification of whether the keys it generates will require biometric 
authentication (i.e. can be turned off for testing) and if it should store the keys
in [StrongBox](https://developer.android.com/training/articles/keystore).

### Make Credential (User Registration)

Following the [authenticatorMakeCredential](https://www.w3.org/TR/webauthn/#op-make-cred)
operation, the `Authenticator` object can take a json string containin the required 
input parameters and parse it into an `AuthenticatorMakeCredentialOptions` object.

```java
AuthenticatorMakeCredentialOptions makeCredentialOptions = AuthenticatorMakeCredentialOptions.fromJSON(options);
```

It can then use the `AuthenticatorMakeCredentialOptions` object to make a new credential.

```java
AttestationObject attestationObject = authenticator.makeCredential(makeCredentialOptions, context, cancellationSignal);
```
It requires an application context in order to show the 
[BiometricPrompt](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt)
and accepts an optional 
[CancellationSignal](https://developer.android.com/reference/android/os/CancellationSignal)
to allow user-initiated cancellation.

The `AttestationObject` can also be serialized to bytes.

```java
byte[] attestationObjectBytes = attestationObject.asCBOR();
```

### Get Assertion (User Login)

Following the [authenticatorGetAssertion](https://www.w3.org/TR/webauthn/#op-get-assertion)
operation, the `Authenticator` object can take a json string containin the required 
input parameters and parse it into an `AuthenticatorGetAssertionOptions` object.

```java
AuthenticatorGetAssertionOptions getAssertionOptions = AuthenticatorGetAssertionOptions.fromJSON(options);
```

The `getAssertion` operation requires you pass it a 
[DialogFragment](https://developer.android.com/reference/android/app/DialogFragment)
to allow the user to select from among the available credentials, if necessary. You can create
one designed for this task with a `SelectCredentialDialogFragment`.

```java
SelectCredentialDialogFragment credentialSelector = new SelectCredentialDialogFragment();
credentialSelector.populateFragmentActivity(fragmentActivity);
```

The `fragmentActivity` supplied should be the main 
[Activity](https://developer.android.com/reference/android/app/Activity) 
with which the user is currently interacting.

You can then use the `AuthenticatorGetAssertionOptions` and `SelectCredentialDialogFragment` 
objects to get a new assertion result.

```java
AuthenticatorGetAssertionResult assertionObject = authenticator.getAssertion(options, credentialSelector, context, cancellationSignal);
```
As with the `makeCredential` operation, it requires an application context in order to show the 
[BiometricPrompt](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt)
and accepts an optional 
[CancellationSignal](https://developer.android.com/reference/android/os/CancellationSignal)
to allow user-initiated cancellation.


