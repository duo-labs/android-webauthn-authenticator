package duo.labs.webauthn;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Base64;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.List;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnicodeString;
import duo.labs.webauthn.exceptions.ConstraintError;
import duo.labs.webauthn.exceptions.InvalidStateError;
import duo.labs.webauthn.models.AttestationObject;
import duo.labs.webauthn.models.AuthenticatorGetAssertionOptions;
import duo.labs.webauthn.models.AuthenticatorGetAssertionResult;
import duo.labs.webauthn.models.AuthenticatorMakeCredentialOptions;
import duo.labs.webauthn.models.PublicKeyCredentialDescriptor;
import duo.labs.webauthn.models.PublicKeyCredentialSource;
import duo.labs.webauthn.exceptions.VirgilException;
import duo.labs.webauthn.exceptions.WebAuthnException;
import duo.labs.webauthn.util.CredentialSafe;
import duo.labs.webauthn.util.CredentialSelector;
import duo.labs.webauthn.util.WebAuthnCryptography;

import static org.junit.Assert.*;

public class AuthenticatorTest {
    Authenticator authenticator;
    CredentialSafe credentialSafe;
    WebAuthnCryptography cryptography;
    Context ctx;

    public static final String MAKE_CREDENTIAL_JSON = "{\n" +
            "    \"authenticatorExtensions\": \"\", // optional and currently ignored\n" +
            "    \"clientDataHash\": \"LTCT/hWLtJenIgi0oUhkJz7dE8ng+pej+i6YI1QQu60=\", // base64\n" +
            "    \"credTypesAndPubKeyAlgs\": [\n" +
            "        [\"public-key\", -7]\n" +
            "    ],\n" +
            "    \"excludeCredentials\": [\n" +
            "        {\n" +
            "            \"type\": \"public-key\",\n" +
            "            \"id\": \"lVGyXHwz6vdYignKyctbkIkJto/ADbYbHhE7+ss/87o=\" // base64\n" +
            "            // \"transports\" member optional but ignored\n" +
            "        }\n" +
            "    ],\n" +
            "    \"requireResidentKey\": true,\n" +
            "    \"requireUserPresence\": true,\n" +
            "    \"requireUserVerification\": false,\n" +
            "    \"rp\": {\n" +
            "        \"name\": \"webauthn.io\",\n" +
            "        \"id\": \"webauthn.io\"\n" +
            "    },\n" +
            "    \"user\": {\n" +
            "        \"name\": \"testuser\",\n" +
            "        \"displayName\": \"Test User\",\n" +
            "        \"id\": \"/QIAAAAAAAAAAA==\" // base64\n" +
            "    }\n" +
            "}";

    public static final String GET_ASSERTION_JSON = "{\n" +
            "    \"allowCredentialDescriptorList\": [{\n" +
            "        \"id\": \"jVtTOKLHRMN17I66w48XWuJadCitXg0xZKaZvHdtW6RDCJhxO6Cfff9qbYnZiMQ1pl8CzPkXcXEHwpQYFknN2w==\", // base64\n" +
            "        \"type\": \"public-key\"\n" +
            "    }],\n" +
            "    \"authenticatorExtensions\": \"\", // optional and ignored\n" +
            "    \"clientDataHash\": \"BWlg/oAqeIhMHkGAo10C3sf4U/sy0IohfKB0OlcfHHU=\", // base64\n" +
            "    \"requireUserPresence\": true,\n" +
            "    \"requireUserVerification\": false,\n" +
            "    \"rpId\": \"webauthn.io\"\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        this.ctx = InstrumentationRegistry.getContext();
        this.authenticator = new Authenticator(this.ctx, false, false);
        this.credentialSafe = this.authenticator.credentialSafe;
        this.cryptography = this.authenticator.cryptoProvider;
    }

    /**
     * Ensure that we can decode some JSON and create a credential.
     *
     * @throws VirgilException
     * @throws WebAuthnException
     * @throws CborException
     */
    @Test
    public void fromJson() throws VirgilException, WebAuthnException {
        AuthenticatorMakeCredentialOptions options = AuthenticatorMakeCredentialOptions.fromJSON(MAKE_CREDENTIAL_JSON);
        AttestationObject attObj = authenticator.makeCredential(options);
    }

    /**
     * Go through the whole dance of creating a new credential and generating an assertion
     * from the credential. Ensure that the signature is valid.
     * @throws VirgilException
     * @throws WebAuthnException
     * @throws CborException
     */
    @Test
    public void makeCredentialAndGetAssertionWithAllowCredential() throws VirgilException, WebAuthnException, CborException {
        AuthenticatorMakeCredentialOptions makeCredentialOptions = AuthenticatorMakeCredentialOptions.fromJSON(MAKE_CREDENTIAL_JSON);
        AttestationObject attObj = authenticator.makeCredential(makeCredentialOptions);
        byte[] cborEncoded = attObj.asCBOR();

        ByteArrayInputStream bais = new ByteArrayInputStream(cborEncoded);
        Map decoded = (Map) new CborDecoder(bais).decode().get(0);
        String fmt = ((UnicodeString) decoded.get(new UnicodeString("fmt"))).getString();
        assertEquals(fmt, "none");

        byte[] credentialId = attObj.getCredentialId();

        // Now let's see if we can generate an assertion based on the returned credential ID
        AuthenticatorGetAssertionOptions getAssertionOptions = AuthenticatorGetAssertionOptions.fromJSON(GET_ASSERTION_JSON);
        //getAssertionOptions.allowCredentialDescriptorList.clear();
        getAssertionOptions.allowCredentialDescriptorList.add(new PublicKeyCredentialDescriptor("public-key", credentialId, null));

        AuthenticatorGetAssertionResult getAssertionResult = authenticator.getAssertion(getAssertionOptions, new CredentialSelector() {
            @Override
            public PublicKeyCredentialSource selectFrom(List<PublicKeyCredentialSource> credentialList) {
                return credentialList.get(0);
            }
        });

        ByteBuffer resultBuf = ByteBuffer.allocate(getAssertionOptions.clientDataHash.length + getAssertionResult.authenticatorData.length);
        resultBuf.put(getAssertionResult.authenticatorData);
        resultBuf.put(getAssertionOptions.clientDataHash);
        byte[] signedData = resultBuf.array();
        List<PublicKeyCredentialSource> sources = this.credentialSafe.getKeysForEntity(makeCredentialOptions.rpEntity.id);
        PublicKeyCredentialSource source = sources.get(sources.size() - 1);
        KeyPair keyPair = this.credentialSafe.getKeyPairByAlias(source.keyPairAlias);
        assertTrue(this.cryptography.verifySignature(keyPair.getPublic(), signedData, getAssertionResult.signature));
    }

    /**
     * Ensure that we fail to create a credential if user verification is requested, but we didn't
     * initialize the Authenticator with biometric auth set to true.
     * @throws WebAuthnException
     * @throws VirgilException
     */
    @Test
    public void testFailureOnVerificationRequiredWithoutSupport() throws WebAuthnException, VirgilException {
        AuthenticatorMakeCredentialOptions makeCredentialOptions = AuthenticatorMakeCredentialOptions.fromJSON(MAKE_CREDENTIAL_JSON);
        makeCredentialOptions.requireUserVerification = true;
        makeCredentialOptions.requireUserPresence = false;

        try {
            AttestationObject attObj = authenticator.makeCredential(makeCredentialOptions);
            Assert.fail("makeCredential should have failed without biometric support");
        } catch (ConstraintError e) {
            // success! any other exception is a failure
        }
    }

    /**
     * Ensure that the "exclude credentials" functionality keeps us from creating a new credential
     * when an excluded credential is known.
     * @throws VirgilException
     * @throws WebAuthnException
     */
    @Test
    public void testExcludeCredentials() throws VirgilException, WebAuthnException {
        AuthenticatorMakeCredentialOptions makeCredentialOptions = AuthenticatorMakeCredentialOptions.fromJSON(MAKE_CREDENTIAL_JSON);
        AttestationObject firstAttestationObject = authenticator.makeCredential(makeCredentialOptions);

        // Now we want to pull out the ID of the just-created credential, add it to the exclude list,
        // and ensure that we see a failure when creating a second credential.

        makeCredentialOptions.excludeCredentialDescriptorList.add(new PublicKeyCredentialDescriptor("public-key", firstAttestationObject.getCredentialId(), null));
        try {
            AttestationObject secondAttestationObject = authenticator.makeCredential(makeCredentialOptions);
            Assert.fail("makeCredential should have failed due to a matching credential ID in the exclude list");
        } catch (InvalidStateError e) {
            // good! the matching credential descriptor caused the authenticator to reject the request
        }
    }


    /**
     * Make sure that we can pass an empty allowed credentials list.
     * @throws VirgilException
     * @throws WebAuthnException
     */
    @Test
    public void testAllowCredentialsEmpty() throws VirgilException, WebAuthnException {
        AuthenticatorMakeCredentialOptions makeCredentialOptions = AuthenticatorMakeCredentialOptions.fromJSON(MAKE_CREDENTIAL_JSON);
        AttestationObject attObj = authenticator.makeCredential(makeCredentialOptions);

        AuthenticatorGetAssertionOptions getAssertionOptions = AuthenticatorGetAssertionOptions.fromJSON(GET_ASSERTION_JSON);
        getAssertionOptions.allowCredentialDescriptorList.clear();
        authenticator.getAssertion(getAssertionOptions, new CredentialSelector() {
            @Override
            public PublicKeyCredentialSource selectFrom(List<PublicKeyCredentialSource> credentialList) {
                return credentialList.get(0);
            }
        });
    }
}