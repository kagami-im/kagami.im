package im.kagami;

import im.kagami.security.jwt.JwtProperties;
import im.kagami.security.jwt.RsaKeyProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan({"im.kagami.user.mapper", "im.kagami.post.mapper", "im.kagami.comment.mapper"})
@EnableConfigurationProperties({JwtProperties.class, RsaKeyProperties.class})
public class KagamiApplication {

    static void main(String[] args) {
        SpringApplication.run(KagamiApplication.class, args);
    }
}
