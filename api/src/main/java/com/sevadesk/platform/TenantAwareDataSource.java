package com.sevadesk.platform;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Sets the PostgreSQL session variable {@code app.tenant_id} every time a connection is taken from the pool.
 * The row-level-security policies in V1__core.sql read it, so a query can only ever see the current temple's rows,
 * even if application code forgets a WHERE clause. With no tenant in context the variable is empty and
 * tenant-scoped tables return nothing.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return bind(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return bind(super.getConnection(username, password));
    }

    private static Connection bind(Connection connection) throws SQLException {
        var tenantId = TenantContext.currentId();
        try (PreparedStatement ps = connection.prepareStatement("select set_config('app.tenant_id', ?, false)")) {
            ps.setString(1, tenantId == null ? "" : tenantId.toString());
            ps.execute();
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }
}
