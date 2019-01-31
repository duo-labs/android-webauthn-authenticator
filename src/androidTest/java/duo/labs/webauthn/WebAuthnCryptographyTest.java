package duo.labs.webauthn;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;

import duo.labs.webauthn.exceptions.VirgilException;
import duo.labs.webauthn.models.PublicKeyCredentialSource;
import duo.labs.webauthn.util.CredentialSafe;
import duo.labs.webauthn.util.WebAuthnCryptography;

import static org.junit.Assert.*;

public class WebAuthnCryptographyTest {
    CredentialSafe credentialSafe;
    WebAuthnCryptography crypto;

    @Before
    public void setUp() throws Exception {
        Context ctx = InstrumentationRegistry.getContext();
        this.credentialSafe = new CredentialSafe(ctx, false, false);
        this.crypto = new WebAuthnCryptography(this.credentialSafe);

    }

    @Test
    public void verifySignature() throws VirgilException {
        byte[] toSign = "sign me plz".getBytes();
        PublicKeyCredentialSource credentialSource = this.credentialSafe.generateCredential("mine", null, "myname");
        KeyPair keyPair = this.credentialSafe.getKeyPairByAlias(credentialSource.keyPairAlias);
        byte[] signature = this.crypto.performSignature(keyPair.getPrivate(), toSign, null);
        boolean res = this.crypto.verifySignature(keyPair.getPublic(), toSign, signature);
        assertTrue(res);
    }
}