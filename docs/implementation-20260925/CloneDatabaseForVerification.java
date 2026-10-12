import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Creates a NEW local test database. Never drops or updates the source database. */
class CloneDatabaseForVerification {
    public static void main(String[] args) throws Exception {
        String target = "motorent_verify_20260926";
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) { properties.load(reader); }
        String url = properties.getProperty("spring.datasource.url");
        if (!url.startsWith("jdbc:mysql://localhost:") && !url.startsWith("jdbc:mysql://127.0.0.1:"))
            throw new IllegalStateException("Verification only supports the local MySQL server");
        try (var source = DriverManager.getConnection(url, properties.getProperty("spring.datasource.username"), properties.getProperty("spring.datasource.password"));
             var copy = DriverManager.getConnection(url, properties.getProperty("spring.datasource.username"), properties.getProperty("spring.datasource.password"))) {
            if (target.equals(source.getCatalog())) throw new IllegalStateException("Source must differ from target");
            source.setReadOnly(true);
            source.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            source.setAutoCommit(false);
            var names = new ArrayList<String>();
            try (var statement = source.createStatement(); var result = statement.executeQuery("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
                while (result.next()) names.add(result.getString(1));
            }
            try (var statement = copy.createStatement()) {
                // Refuse any nonempty target; retrying an empty setup never overwrites tables.
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + target + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                try (var check = copy.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=?")) {
                    check.setString(1, target);
                    try (var result = check.executeQuery()) { result.next(); if (result.getInt(1) != 0) throw new IllegalStateException("Target is not empty; refusing to overwrite"); }
                }
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                for (String name : names) {
                    if (!name.matches("[a-zA-Z0-9_]+")) throw new IllegalStateException("Unexpected table name");
                    try (var read = source.createStatement(); var result = read.executeQuery("SHOW CREATE TABLE `" + name + "`")) {
                        result.next();
                        String ddl = result.getString(2).replace("CREATE TABLE `" + name + "`", "CREATE TABLE `" + target + "`.`" + name + "`")
                                .replace("REFERENCES `", "REFERENCES `" + target + "`.`");
                        statement.executeUpdate(ddl);
                    }
                    long count = 0;
                    try (var read = source.createStatement(); var result = read.executeQuery("SELECT * FROM `" + name + "`")) {
                        int columns = result.getMetaData().getColumnCount();
                        String placeholders = String.join(",", Collections.nCopies(columns, "?"));
                        try (var insert = copy.prepareStatement("INSERT INTO `" + target + "`.`" + name + "` VALUES (" + placeholders + ")")) {
                            while (result.next()) {
                                for (int col = 1; col <= columns; col++) insert.setObject(col, result.getObject(col));
                                insert.executeUpdate(); count++;
                            }
                        }
                    }
                    System.out.println(name + ": " + count + " rows copied");
                }
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
                statement.executeUpdate("UPDATE `" + target + "`.token_authentication SET is_used=false");
            }
            source.rollback();
            System.out.println("VERIFICATION_DATABASE_READY=" + target);
        }
    }
}
