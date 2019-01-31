# Android Webauthn Authenticator Library

This library is meant to serve as an example implementation of the [WebAuthn
authenticator model](https://www.w3.org/TR/webauthn/#sctn-authenticator-model).
While the specification is currently in Candidate Recommendation, this library
conforms as much as possible to the guidelines and implementation procedures
outlined by the document.

## Quickstart

Use [JitPack](https://jitpack.io/) to include this repository in your Android
project.

Add this in your root build.gradle:
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Add this to your dependencies list:
```
	dependencies {
		implementation 'com.github.duo-labs:android-webauthn-authenticator:master-SNAPSHOT'
	}
```

## Usage

### Make Credential

### Get Assertion



