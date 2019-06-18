package duo.labs.webauthn;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;

import duo.labs.webauthn.exceptions.VirgilException;
import duo.labs.webauthn.models.PublicKeyCredentialSource;
import duo.labs.webauthn.util.CredentialSafe;

import static org.junit.Assert.*;

public class CredentialSafeTest {
    private CredentialSafe credentialSafe;

    @Before
    public void setUp() throws Exception {
        Context ctx = InstrumentationRegistry.getContext();
        this.credentialSafe = new CredentialSafe(ctx, true, false);
    }

    @Test
    public void generateCredential() throws VirgilException {
        PublicKeyCredentialSource cs = this.credentialSafe.generateCredential("myentity", null, "myname");
        assertEquals(cs.rpId, "myentity");
    }

    @Test
    public void getKeyPairByAlias() throws VirgilException {
        PublicKeyCredentialSource cs = this.credentialSafe.generateCredential("myentity", null, "myname");
        KeyPair keyPair = this.credentialSafe.getKeyPairByAlias(cs.keyPairAlias);

        assertTrue(keyPair.getPrivate() != null);
        assertTrue(keyPair.getPublic() != null);
    }
}