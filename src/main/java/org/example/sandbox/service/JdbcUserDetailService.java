package org.example.sandbox.service;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Optional;

public class JdbcUserDetailService extends MappingSqlQuery<UserDetails> implements UserDetailsService {

    public JdbcUserDetailService(DataSource ds){
        super(ds, """
                select
                u.c_username,
                up.c_password,
                array_agg(ua.c_authority) as c_authorities
                from t_user u
                left join t_user_password up on up.id_user = u.id
                left join t_user_authority ua on ua.id_user = u.id
                where u.c_username = :username
                group by u.id, up.id
                """);
        this.declareParameter(new SqlParameter("username", Types.VARCHAR));
        this.compile();
    }

    @Override
    protected UserDetails mapRow(ResultSet rs, int rowNum) throws SQLException{
        return User.builder()
                .username(rs.getString("c_username"))
                .password(rs.getString(("c_password")))
                .authorities(((String[]) rs.getArray("c_authorities").getArray()))
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return Optional.ofNullable(this.findObjectByNamedParam(Map.of("username", username)))
                .orElseThrow(() -> new UsernameNotFoundException("Username %s not found".formatted(username)));
    }
}
