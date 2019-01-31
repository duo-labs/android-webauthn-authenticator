package duo.labs.webauthn.util;

import java.util.List;
import java.util.concurrent.Exchanger;

import duo.labs.webauthn.models.PublicKeyCredentialSource;

public interface CredentialSelector {
    public PublicKeyCredentialSource selectFrom(List<PublicKeyCredentialSource> credentialList);
}
