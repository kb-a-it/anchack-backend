package com.kbait.anchack.common.config;

<<<<<<< HEAD
import com.fasterxml.jackson.databind.ObjectMapper;
=======
import com.kbait.anchack.place.config.PlaceConfig;
>>>>>>> origin/dev
import com.kbait.anchack.rental.config.MolitRentConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
@PropertySource(
    value = "classpath:application.properties",
    encoding = "UTF-8"
)
@MapperScan("com.kbait.anchack.*.mapper")
@ComponentScan(basePackages = {
    "com.kbait.anchack.*.service",
    "com.kbait.anchack.common.security",
    "com.kbait.anchack.rental.client"
})
@Import({
    MolitRentConfig.class,
    PlaceConfig.class
})
@EnableTransactionManagement
public class RootConfig {

    @Value("${jdbc.driver}")
    private String driver;

    @Value("${jdbc.url}")
    private String url;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    private final ApplicationContext applicationContext;

    public RootConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .locations("classpath:db/migration")
            .load();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(
        DataSource dataSource,
        Flyway flyway
    ) throws Exception {
        SqlSessionFactoryBean factoryBean =
            new SqlSessionFactoryBean();

        factoryBean.setDataSource(dataSource);

        factoryBean.setConfigLocation(
            applicationContext.getResource(
                "classpath:mybatis-config.xml"
            )
        );

        factoryBean.setMapperLocations(
            applicationContext.getResources(
                "classpath*:mappers/**/*.xml"
            )
        );

        return factoryBean.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(
        DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
