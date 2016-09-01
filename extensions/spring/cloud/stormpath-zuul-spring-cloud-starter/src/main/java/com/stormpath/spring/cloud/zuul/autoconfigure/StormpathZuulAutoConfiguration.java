package com.stormpath.spring.cloud.zuul.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.lang.Collections;
import com.stormpath.sdk.lang.Function;
import com.stormpath.sdk.servlet.account.AccountResolver;
import com.stormpath.sdk.servlet.http.Resolver;
import com.stormpath.sdk.servlet.json.JsonFunction;
import com.stormpath.sdk.servlet.json.ResourceJsonFunction;
import com.stormpath.sdk.servlet.mvc.ResourceToMapConverter;
import com.stormpath.spring.boot.autoconfigure.StormpathWebMvcAutoConfiguration;
import com.stormpath.zuul.account.DefaultAccountHeaderValueResolver;
import com.stormpath.zuul.account.ForwardedAccountHeaderFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.ZuulProxyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

/**
 * @since 1.1.0
 */
@Configuration
@ConditionalOnProperty(name = {"stormpath.enabled", "stormpath.web.enabled", "stormpath.zuul.enabled"}, matchIfMissing = true)
@EnableZuulProxy
@AutoConfigureAfter({ZuulProxyConfiguration.class, StormpathWebMvcAutoConfiguration.class})
public class StormpathZuulAutoConfiguration {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private AccountResolver accountResolver; //provided by StormpathWebMvcAutoConfiguration

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings({"ELValidationInJSP", "SpringElInspection"})
    @Value("${stormpath.zuul.account.filter.type:pre}")
    private String forwardedAccountFilterType = "pre";

    @Value("${stormpath.zuul.account.filter.order:0}")
    private int forwardedAccountFilterOrder = 0;

    @Value("${stormpath.zuul.account.header.name:X-Forwarded-Account}")
    private String forwardedAccountHeaderName = "X-Forwarded-Account";

    @Value("#{ @environment['stormpath.zuul.account.header.includedProperties'] ?: {'groups', 'customData'} }")
    private Set<String> accountToMapConverterIncludedFields = Collections.toSet("groups", "customData");

    //complex objects other than custom data are excluded by default - nothing more to exclude by default:
    @Value("#{ @environment['stormpath.zuul.account.header.excludedProperties'] ?: {} }")
    private Set<String> accountToMapConverterExcludedFields = java.util.Collections.emptySet();

    @Bean
    @ConditionalOnMissingBean(name = "stormpathJsonFactory")
    public Function<Object, String> stormpathJsonFactory() {
        return new JsonFunction<>(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(name = "stormpathForwardedAccountMapConverter")
    public Function<Account, Map<String, Object>> stormpathForwardedAccountMapConverter() {
        ResourceToMapConverter<Account> converter = new ResourceToMapConverter<>();
        converter.setIncludedFields(accountToMapConverterIncludedFields);
        converter.setExcludedFields(accountToMapConverterExcludedFields);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(name = "stormpathForwardedAccountStringConverter")
    public Function<Account, String> stormpathForwardedAccountStringConverter() {
        return new ResourceJsonFunction<>(stormpathForwardedAccountMapConverter(), stormpathJsonFactory());
    }

    @Bean
    @ConditionalOnMissingBean(name = "stormpathForwardedAccountHeaderValueResolver")
    public Resolver<String> stormpathForwardedAccountHeaderValueResolver() {
        DefaultAccountHeaderValueResolver resolver = new DefaultAccountHeaderValueResolver();
        resolver.setAccountResolver(accountResolver);
        resolver.setAccountStringFactory(stormpathForwardedAccountStringConverter());
        return resolver;
    }

    @Bean
    @ConditionalOnMissingBean(name = "stormpathForwardedAccountHeaderFilter")
    public ZuulFilter stormpathForwardedAccountHeaderFilter() {
        ForwardedAccountHeaderFilter filter = new ForwardedAccountHeaderFilter();
        filter.setFilterType(forwardedAccountFilterType);
        filter.setFilterOrder(forwardedAccountFilterOrder);
        filter.setHeaderName(forwardedAccountHeaderName);
        filter.setAccountResolver(accountResolver);
        filter.setValueResolver(stormpathForwardedAccountHeaderValueResolver());
        return filter;
    }
}
