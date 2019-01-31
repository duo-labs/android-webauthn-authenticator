package duo.labs.webauthn.util;

import android.hardware.biometrics.BiometricPrompt;
import android.util.Log;

import java.security.Signature;
import java.util.concurrent.Exchanger;

import duo.labs.webauthn.Authenticator;
import duo.labs.webauthn.exceptions.VirgilException;
import duo.labs.webauthn.exceptions.WebAuthnException;
import duo.labs.webauthn.models.AttestationObject;
import duo.labs.webauthn.models.AuthenticatorMakeCredentialOptions;
import duo.labs.webauthn.models.PublicKeyCredentialSource;

public class BiometricMakeCredentialCallback extends BiometricPrompt.AuthenticationCallback {
    private static final String TAG = "BiometricMakeCredentialCallback";

    private Authenticator authenticator;
    private AuthenticatorMakeCredentialOptions options;
    private PublicKeyCredentialSource credentialSource;
    private Exchanger<AttestationObject> exchanger;

    public BiometricMakeCredentialCallback(Authenticator authenticator, AuthenticatorMakeCredentialOptions options, PublicKeyCredentialSource credentialSource, Exchanger<AttestationObject> exchanger) {
        super();
        this.authenticator = authenticator;
        this.options = options;
        this.credentialSource = credentialSource;
        this.exchanger = exchanger;
    }

    @Override
    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        Log.d(TAG, "Authentication Succeeded");

        // retrieve biometricprompt-approved signature
        Signature signature = result.getCryptoObject().getSignature();

        AttestationObject attestationObject;
        try {
            attestationObject = authenticator.makeInternalCredential(options, credentialSource, signature);
        } catch (VirgilException | WebAuthnException exception) {
            Log.w(TAG, "Failed makeInternalCredential: " + exception.toString());
            onAuthenticationFailed();
            return;
        }
        try {
            exchanger.exchange(attestationObject);
        } catch (InterruptedException exception) {
            Log.w(TAG, "Could not send attestationObject from BiometricPrompt: " + exception.toString());
            return;
        }
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        super.onAuthenticationHelp(helpCode, helpString);
        Log.d(TAG, "authentication help");
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        Log.d(TAG, "authentication error");
        try {
            exchanger.exchange(null);
        } catch (InterruptedException exception) {
            Log.w(TAG, "Could not send null (failure) from BiometricPrompt: " + exception.toString());
        }
    }

    @Override
    public void onAuthenticationFailed() {
        // this happens on a bad fingerprint read -- don't cancel/error if this happens
        super.onAuthenticationFailed();
        Log.d(TAG, "authentication failed");
    }

    public void onAuthenticationCancelled() {
        Log.d(TAG, "authentication cancelled");
        try {
            exchanger.exchange(null);
        } catch (InterruptedException exception) {
            Log.w(TAG, "Could not send null (failure) from BiometricPrompt: " + exception.toString());
        }
    }
}
