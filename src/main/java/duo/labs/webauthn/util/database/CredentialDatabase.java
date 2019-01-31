package duo.labs.webauthn.util.database;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import duo.labs.webauthn.models.PublicKeyCredentialSource;

@Database(entities = {PublicKeyCredentialSource.class}, version = 3)
public abstract class CredentialDatabase extends RoomDatabase {
    private static CredentialDatabase INSTANCE;
    private static final String CREDENTIAL_DB_NAME = "credentialmetadata";

    public static CredentialDatabase getDatabase(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(), CredentialDatabase.class, CREDENTIAL_DB_NAME)
                    .allowMainThreadQueries()
                    .build();
        }
        return INSTANCE;
    }

    public abstract CredentialDao credentialDao();
}
