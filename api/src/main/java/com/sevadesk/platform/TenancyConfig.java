package com.sevadesk.platform;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
class TenancyConfig {

    /** Wraps the application DataSource so every connection carries the current tenant id. */
    @Bean
    static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource ds && "dataSource".equals(beanName) && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(ds);
                }
                return bean;
            }
        };
    }

    /** Runs before the Spring Security filter chain (order -100). */
    @Bean
    FilterRegistrationBean<TenantResolverFilter> tenantResolverFilter(TenantResolver resolver) {
        var registration = new FilterRegistrationBean<>(new TenantResolverFilter(resolver));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
