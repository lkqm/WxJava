package com.binarywang.spring.starter.wxjava.mp.config;

import com.binarywang.spring.starter.wxjava.mp.properties.WxMpProperties;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.redis.JedisWxRedisOps;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import me.chanjar.weixin.common.redis.WxRedisOps;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import me.chanjar.weixin.mp.config.impl.WxMpRedisConfigImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 微信公众号存储策略自动配置.
 *
 * @author someone
 */
@Configuration
@RequiredArgsConstructor
public class WxMpStorageAutoConfiguration {

  private final ApplicationContext applicationContext;

  private final WxMpProperties wxMpProperties;

  @Bean
  @ConditionalOnMissingBean(WxMpConfigStorage.class)
  public WxMpConfigStorage wxMpConfigStorage() {
    WxMpProperties.StorageType type = wxMpProperties.getConfigStorage().getType();
    WxMpConfigStorage config;
    if (type == WxMpProperties.StorageType.redis || type == WxMpProperties.StorageType.jedis) {
      config = wxMpInJedisConfigStorage();
    } else if (type == WxMpProperties.StorageType.redistemplate) {
      config = wxMpInRedisTemplateConfigStorage();
    } else {
      config = wxMpInMemoryConfigStorage();
    }
    return config;
  }

  private WxMpConfigStorage wxMpInMemoryConfigStorage() {
    WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
    setWxMpInfo(config);
    return config;
  }

  private WxMpConfigStorage wxMpInJedisConfigStorage() {
    WxMpProperties.RedisProperties redisProperties = wxMpProperties.getConfigStorage().getRedis();

    JedisPool jedisPool;
    if (redisProperties == null || StringUtils.isEmpty(redisProperties.getHost())) {
      jedisPool = applicationContext.getBean(JedisPool.class);
    } else {
      JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
      if (redisProperties.getMaxActive() != null) {
        jedisPoolConfig.setMaxTotal(redisProperties.getMaxActive());
      }
      if (redisProperties.getMaxIdle() != null) {
        jedisPoolConfig.setMaxIdle(redisProperties.getMaxIdle());
      }
      if (redisProperties.getMaxWaitMillis() != null) {
        jedisPoolConfig.setMaxWaitMillis(redisProperties.getMaxWaitMillis());
      }
      if (redisProperties.getMinIdle() != null) {
        jedisPoolConfig.setMinIdle(redisProperties.getMinIdle());
      }
      jedisPoolConfig.setTestOnBorrow(true);
      jedisPoolConfig.setTestWhileIdle(true);

      jedisPool = new JedisPool(jedisPoolConfig, redisProperties.getHost(), redisProperties.getPort(),
        redisProperties.getTimeout(), redisProperties.getPassword(), redisProperties.getDatabase());
    }

    WxRedisOps redisOps = new JedisWxRedisOps(jedisPool);
    WxMpRedisConfigImpl wxMpRedisConfig = new WxMpRedisConfigImpl(redisOps, wxMpProperties.getConfigStorage().getKeyPrefix());
    setWxMpInfo(wxMpRedisConfig);
    return wxMpRedisConfig;
  }

  private WxMpConfigStorage wxMpInRedisTemplateConfigStorage() {
    StringRedisTemplate redisTemplate = applicationContext.getBean(StringRedisTemplate.class);
    WxRedisOps redisOps = new RedisTemplateWxRedisOps(redisTemplate);
    WxMpRedisConfigImpl wxMpRedisConfig = new WxMpRedisConfigImpl(redisOps, wxMpProperties.getConfigStorage().getKeyPrefix());
    setWxMpInfo(wxMpRedisConfig);
    return wxMpRedisConfig;
  }

  private void setWxMpInfo(WxMpDefaultConfigImpl config) {
    WxMpProperties properties = wxMpProperties;
    WxMpProperties.ConfigStorage configStorageProperties = properties.getConfigStorage();
    config.setAppId(properties.getAppId());
    config.setSecret(properties.getSecret());
    config.setToken(properties.getToken());
    config.setAesKey(properties.getAesKey());

    config.setHttpProxyHost(configStorageProperties.getHttpProxyHost());
    config.setHttpProxyUsername(configStorageProperties.getHttpProxyUsername());
    config.setHttpProxyPassword(configStorageProperties.getHttpProxyPassword());
    if (configStorageProperties.getHttpProxyPort() != null) {
      config.setHttpProxyPort(configStorageProperties.getHttpProxyPort());
    }
  }

}
