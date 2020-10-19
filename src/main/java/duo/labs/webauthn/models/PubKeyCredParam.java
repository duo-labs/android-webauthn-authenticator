package duo.labs.webauthn.models;

import java.util.Objects;

public class PubKeyCredParam {
    public String type;
    public int alg;

    public PubKeyCredParam(String type, int alg) {
        this.type = type;
        this.alg = alg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PubKeyCredParam)) return false;
        PubKeyCredParam that = (PubKeyCredParam) o;
        return alg == that.alg &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, alg);
    }
}
