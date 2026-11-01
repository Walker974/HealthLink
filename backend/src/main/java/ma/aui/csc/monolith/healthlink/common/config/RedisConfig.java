package ma.aui.csc.monolith.healthlink.common.config;

import ma.aui.csc.monolith.healthlink.availability.live.RedisLiveUpdateBroker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis connection used for the shared cache and for Pub/Sub. Only created when
 * healthlink.redis.enabled=true, so the backend still starts without Redis.
 */
@Configuration
@ConditionalOnProperty(name = "healthlink.redis.enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    LettuceConnectionFactory redisConnectionFactory(@Value("${healthlink.redis.host}") String host,
                                                    @Value("${healthlink.redis.port}") int port,
                                                    @Value("${healthlink.redis.password:}") String password) {
        RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(host, port);
        if (!password.isBlank()) {
            server.setPassword(password);
        }
        return new LettuceConnectionFactory(server);
    }

    @Bean
    StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /** Subscribes this instance to the status channel; messages go to RedisLiveUpdateBroker. */
    @Bean
    RedisMessageListenerContainer liveUpdateListenerContainer(LettuceConnectionFactory connectionFactory,
                                                              RedisLiveUpdateBroker broker) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(broker, new ChannelTopic(RedisLiveUpdateBroker.CHANNEL));
        return container;
    }
}
