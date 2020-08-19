package io.quarkus.jdbc.mssql.runtime.graal.com.microsoft.sqlserver.jdbc;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerADAL4JUtils")
@Substitute
final class SQLServerADAL4JUtils {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    static final private java.util.logging.Logger adal4jLogger = null;

    @Substitute
    static QuarkusSqlFedAuthToken getSqlFedAuthToken(QuarkusSqlFedAuthInfo fedAuthInfo, String user, String password,
            String authenticationString) {
        throw new IllegalStateException("Quarkus does not support Active Directory based authentication");
    }

    @Substitute
    static QuarkusSqlFedAuthToken getSqlFedAuthTokenIntegrated(QuarkusSqlFedAuthInfo fedAuthInfo, String authenticationString) {
        throw new IllegalStateException("Quarkus does not support Active Directory based authentication");
    }

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SqlFedAuthToken")
final class QuarkusSqlFedAuthToken {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerConnection", innerClass = "SqlFedAuthInfo")
final class QuarkusSqlFedAuthInfo {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerConnection")
final class QuarkusSQLServerConnection {

    @Substitute
    private QuarkusSqlFedAuthToken getMSIAuthToken(String resource, String msiClientId) {
        throw new IllegalStateException("Quarkus does not support MSI based authentication");
    }

}
//
//@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionAzureKeyVaultProvider")
//final class QuarkusDisablesAzureAuthentication /* extends SQLServerColumnEncryptionKeyStoreProvider */ {
//
//    private String name = "azure-authentication-is-disabled-in-native-image";
//
//    @Substitute
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    @Substitute
//    public String getName() {
//        return this.name;
//    }
//
//    @Substitute
//    public byte[] decryptColumnEncryptionKey(String masterKeyPath, String encryptionAlgorithm,
//            byte[] encryptedColumnEncryptionKey) throws SQLServerException {
//        throw new UnsupportedOperationException("AzureKeyVaultProvider is not available in native-image");
//    }
//
//    @Substitute
//    public byte[] encryptColumnEncryptionKey(String masterKeyPath, String encryptionAlgorithm, byte[] columnEncryptionKey)
//            throws SQLServerException {
//        throw new UnsupportedOperationException("AzureKeyVaultProvider is not available in native-image");
//    }
//
//}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement")
// See https://docs.microsoft.com/en-us/sql/connect/jdbc/using-usefmtonly?view=sql-server-ver15
final class QuarkusMSSQLServerPreparedStatement {

    @Substitute
    public final void setUseFmtOnly(boolean useFmtOnly) throws SQLServerException {
        if (useFmtOnly)
            throw new RuntimeException("Using FMTOnly is not a supported feature in native-image");
    }

    @Substitute
    public final boolean getUseFmtOnly() throws SQLServerException {
        return false;
    }
}
/*
 * @TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerFMTQuery")
 * 
 * @Delete
 * final class QuarkusDisablesFMTOnly {
 * 
 * // Reachability of this class needs to be disabled by one of the other substitutions;
 * // Adding this explicit @Delete so to help with future maintenance as it helps with call site analysis.
 * 
 * }
 */

class SQLServerJDBCSubstitutions {

}
