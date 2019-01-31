package duo.labs.webauthn.models;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.List;

import duo.labs.webauthn.util.Base64ByteArrayAdapter;
import rocks.xmpp.precis.PrecisProfile;
import rocks.xmpp.precis.PrecisProfiles;

public class AuthenticatorGetAssertionOptions {
    @SerializedName("rpId")
    public String rpId;
    @SerializedName("clientDataHash")
    public byte[] clientDataHash;
    @SerializedName("allowCredentialDescriptorList")
    public List<PublicKeyCredentialDescriptor> allowCredentialDescriptorList;
    @SerializedName("requireUserPresence")
    public boolean requireUserPresence;
    @SerializedName("requireUserVerification")
    public boolean requireUserVerification;
    // TODO: authenticatorExtensions

    public boolean areWellFormed() {
        if (rpId.isEmpty()) {
            return false;
        }
        if (clientDataHash.length != 32) {
            return false;
        }
        if (!(requireUserPresence ^ requireUserVerification)) { // only one may be set
            return false;
        }
        return true;
    }

    public static AuthenticatorGetAssertionOptions fromJSON(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new Base64ByteArrayAdapter())
                .create();
        return gson.fromJson(json, AuthenticatorGetAssertionOptions.class);
    }
}
