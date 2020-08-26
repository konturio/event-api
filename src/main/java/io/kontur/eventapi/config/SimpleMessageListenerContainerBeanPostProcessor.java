package io.kontur.eventapi.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
public class SimpleMessageListenerContainerBeanPostProcessor implements BeanPostProcessor {

    @Value("${aws.sqs.enabled:true}")
    private Boolean awsEnable;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!awsEnable && bean instanceof SimpleMessageListenerContainer) {
            ((SimpleMessageListenerContainer)bean).setAutoStartup(false);
        }

        return bean;
    }
}
