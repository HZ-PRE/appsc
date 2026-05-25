package com.sync.sc.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


@Component
public class SpringContextUtil implements DisposableBean, ApplicationContextAware {

    public static ApplicationContext applicationContext = null;

    public static <T> T getBean(Class<T> cls){
        return applicationContext.getBean(cls);
    }

    public static <T> T getBean(String name, Class<T> cls){
        return applicationContext.getBean(name, cls);
    }

    public static Object getBean(String beanName){
        return applicationContext.getBean(beanName);
    }

    public static <T> T getBean(Class<T> cls, Object... objects){
        return applicationContext.getBean(cls, objects);
    }

    @Override
    public void destroy() throws Exception {
        SpringContextUtil.applicationContext = null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }
}