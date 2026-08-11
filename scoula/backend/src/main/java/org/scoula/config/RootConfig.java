package org.scoula.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@PropertySource({"classpath:/application.properties"})
@MapperScan(basePackages = {
        "org.scoula.member.mapper",
        "org.scoula.profitLoss.mapper",
        "org.scoula.deposit.mapper",
})
@ComponentScan(basePackages = {"org.scoula.member.service","org.scoula.profitLoss.service", "org.scoula.deposit.service",})

@EnableTransactionManagement
public class RootConfig {
    @Value("${jdbc.driver}")
    String driver;
    @Value("${jdbc.url}")
    String url;
    @Value("${jdbc.username}")
    String username;
    @Value("${jdbc.password}")
    String password;

    // 비밀번호 찾기 인증번호 발송용 SMTP 설정.
    // 값이 없어도 서버가 뜨도록 기본값을 지정했다. 계정이 비어 있으면 메일 대신 로그로 출력된다.
    @Value("${mail.smtp.host:smtp.gmail.com}")
    String mailHost;
    @Value("${mail.smtp.port:587}")
    int mailPort;
    @Value("${mail.username:}")
    String mailUsername;
    @Value("${mail.password:}")
    String mailPassword;

    @Autowired
    ApplicationContext applicationContext;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        HikariDataSource dataSource = new HikariDataSource(config);
        return dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        sqlSessionFactory.setDataSource(dataSource());

        return sqlSessionFactory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());

        return manager;
    }

    /**
     * 비밀번호 찾기 인증번호를 보내는 메일 발송기.
     * Gmail은 2단계 인증을 켠 뒤 발급받는 "앱 비밀번호"로만 SMTP 로그인이 된다.
     */
    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailHost);
        sender.setPort(mailPort);
        sender.setUsername(mailUsername);
        sender.setPassword(mailPassword);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        // 메일 서버가 응답하지 않을 때 요청이 무한정 걸려 있지 않도록 5초로 제한한다.
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return sender;
    }

}
