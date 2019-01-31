package duo.labs.webauthn.util.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import java.util.List;

import duo.labs.webauthn.models.PublicKeyCredentialSource;

@Dao
public abstract class CredentialDao {

    @Query("SELECT * FROM credentials")
    public abstract List<PublicKeyCredentialSource> getAll();

    @Query("SELECT * FROM credentials")
    public abstract LiveData<List<PublicKeyCredentialSource>> getAllLive();

    @Query("SELECT * FROM credentials WHERE rpId = :rpId")
    public abstract List<PublicKeyCredentialSource> getAllByRpId(String rpId);

    @Query("SELECT * FROM credentials WHERE id = :id LIMIT 1")
    public abstract PublicKeyCredentialSource getById(byte[] id);

    @Insert
    public abstract void insert(PublicKeyCredentialSource credential);

    @Delete
    public abstract void delete(PublicKeyCredentialSource credential);

    @Update
    public abstract void update(PublicKeyCredentialSource credential);

    @Query("SELECT keyUseCounter FROM credentials WHERE roomUid = :uid LIMIT 1")
    public abstract int getUseCounter(int uid);

    @Transaction
    public int incrementUseCounter(PublicKeyCredentialSource credential) {
        int useCounter = getUseCounter(credential.roomUid);
        credential.keyUseCounter++;
        update(credential);
        return useCounter;
    }
}
