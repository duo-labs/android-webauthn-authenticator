package duo.labs.webauthn;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Base64;
import android.util.Pair;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnicodeString;
import duo.labs.webauthn.models.AttestationObject;
import duo.labs.webauthn.models.AuthenticatorGetAssertionOptions;
import duo.labs.webauthn.models.AuthenticatorGetAssertionResult;
import duo.labs.webauthn.models.AuthenticatorMakeCredentialOptions;
import duo.labs.webauthn.models.PublicKeyCredentialDescriptor;
import duo.labs.webauthn.models.PublicKeyCredentialSource;
import duo.labs.webauthn.models.RpEntity;
import duo.labs.webauthn.models.UserEntity;
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

    @Before
    public void setUp() throws Exception {
        this.ctx = InstrumentationRegistry.getContext();
        this.authenticator = new Authenticator(this.ctx, false, false);
        this.credentialSafe = this.authenticator.credentialSafe;
        this.cryptography = this.authenticator.cryptoProvider;
    }

    @Test
    public void fromJson() throws VirgilException, WebAuthnException, CborException {
        String json =
                "{" +
                        "  \"clientDataHash\": \"y7h2KcGhZNXoxKCJStkCe7jn+tG942N0FYxQsWqmqgI=\"," +
                        "  \"rp\": {" +
                        "    \"name\": \"Acme, Inc\"," +
                        "    \"id\": \"webauthn.io\"" +
                        "  }," +
                        "  \"user\": {" +
                        "    \"name\": \"ff@example.com\"," +
                        "    \"displayName\": \"ff\"," +
                        "    \"id\": \"wC6kYQzxGsz4hCw0yvhQCQ==\"" +
                        "  }," +
                        "  \"requireResidentKey\": false," +
                        "  \"requireUserPresence\": false," +
                        "  \"requireUserVerification\": false," +
                        "  \"credTypesAndPubKeyAlgs\": [" +
                        "    [" +
                        "      \"public-key\"," +
                        "      \"-7\"" +
                        "    ]" +
                        "  ]," +
                        "  \"authenticatorExtensions\": {}" +
                        "}";
        AuthenticatorMakeCredentialOptions options = AuthenticatorMakeCredentialOptions.fromJSON(json);

        AttestationObject attObj = authenticator.makeCredential(options);
        byte[] cborEncoded = attObj.asCBOR();

        ByteArrayInputStream bais = new ByteArrayInputStream(cborEncoded);
        Map decoded = (Map) new CborDecoder(bais).decode().get(0);
        String fmt = ((UnicodeString) decoded.get(new UnicodeString("fmt"))).getString();

        assertEquals(fmt, "none");
    }

    @Test
    public void makeCredential() throws VirgilException, WebAuthnException, CborException {
        AuthenticatorMakeCredentialOptions makeCredentialOptions = new AuthenticatorMakeCredentialOptions();
        makeCredentialOptions.clientDataHash = Base64.decode("y7h2KcGhZNXoxKCJStkCe7jn+tG942N0FYxQsWqmqgI=", Base64.DEFAULT);
        makeCredentialOptions.rpEntity = new RpEntity();
        makeCredentialOptions.rpEntity.id = "webauthn.io";
        makeCredentialOptions.rpEntity.name = "Acme, Inc";
        makeCredentialOptions.userEntity = new UserEntity();
        makeCredentialOptions.userEntity.displayName = "ff";
        makeCredentialOptions.userEntity.id = Base64.decode("wC6kYQzxGsz4hCw0yvhQCQ==", Base64.DEFAULT);
        makeCredentialOptions.requireResidentKey = false;
        makeCredentialOptions.requireUserPresence = false;
        makeCredentialOptions.requireUserVerification = false;
        makeCredentialOptions.credTypesAndPubKeyAlgs = new ArrayList<>();
        makeCredentialOptions.credTypesAndPubKeyAlgs.add(new Pair<>("public-key", (long) -7));

        AttestationObject attObj = authenticator.makeCredential(makeCredentialOptions);
        byte[] cborEncoded = attObj.asCBOR();

        ByteArrayInputStream bais = new ByteArrayInputStream(cborEncoded);
        Map decoded = (Map) new CborDecoder(bais).decode().get(0);
        String fmt = ((UnicodeString) decoded.get(new UnicodeString("fmt"))).getString();

        assertEquals(fmt, "none");

        byte[] authData = ((ByteString) decoded.get(new UnicodeString("authData"))).getBytes();

        int credentialIdLength = (authData[37 + 16] << 8) + authData[37 + 16 + 1];
        byte[] credentialId = Arrays.copyOfRange(authData, 37 + 16 + 2, 37 + 16 + 2 + credentialIdLength);

        // Now let's see if we can generate an assertion based on the returned credential ID
        AuthenticatorGetAssertionOptions getAssertionOptions = new AuthenticatorGetAssertionOptions();
        getAssertionOptions.clientDataHash = WebAuthnCryptography.sha256("this is client data");
        getAssertionOptions.requireUserPresence = false;
        getAssertionOptions.requireUserVerification = false;
        getAssertionOptions.rpId = "webauthn.io";
        getAssertionOptions.allowCredentialDescriptorList = new ArrayList<>();
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
        List<PublicKeyCredentialSource> sources = this.credentialSafe.getKeysForEntity("webauthn.io");
        PublicKeyCredentialSource source = sources.get(sources.size() - 1);
        KeyPair keyPair = this.credentialSafe.getKeyPairByAlias(source.keyPairAlias);
        assertTrue(this.cryptography.verifySignature(keyPair.getPublic(), signedData, getAssertionResult.signature));
    }
}