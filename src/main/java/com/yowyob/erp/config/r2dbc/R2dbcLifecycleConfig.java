package com.yowyob.erp.config.r2dbc;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import reactor.core.publisher.Mono;

@Configuration
public class R2dbcLifecycleConfig {

    @Bean
    public AfterConvertCallback<Object> afterConvertCallback() {
        return (entity, table) -> {
            if (entity instanceof SettablePersistable<?> settable) {
                settable.setNotNew();
            }
            return Mono.just(entity);
        };
    }
}
