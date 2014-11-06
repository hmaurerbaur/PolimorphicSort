package domain

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.orm.jpa.vendor.Database

import javax.sql.DataSource

@Configuration
@Import([PersistenceConfiguration, ContactRepositoryConfiguration])
@ComponentScan
public class TestConfiguration {
    public final static String DATABASE_NAME = "contact";

    @Bean
    public DataSource dataSource() {
        new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.valueOf(Database.H2.name()))
                .setName(DATABASE_NAME)
                .build();
    }
}


