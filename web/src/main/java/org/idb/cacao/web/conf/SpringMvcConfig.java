/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.conf;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.idb.cacao.api.DocumentSituation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Configuration for SpringBoot for using i18n (internationalization)<BR>
 * <BR>
 * Install a default interceptor for identifying the browser language<BR>
 * <BR>
 * Adds 'lang' parameter in order to allow changing language<BR>
 * <BR>
 * Configures static resources caching<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
public class SpringMvcConfig implements WebMvcConfigurer {
	
	/**
	 * Default timeout for async requests
	 */
	public static final long DEFAULT_ASYNC_TIMEOUT = 600000; // 10 min

	@Autowired
	private Environment env;
	
	@Autowired
	private ApplicationContext app;
	
	/**
	 * Provides a LocaleResolver BEAN for the application to determine which locale is currently being used
	 */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(new Locale(env.getProperty("cacao.user.language"), env.getProperty("cacao.user.country")));
        return slr;
    }

	/**
	 * Provides a LocaleChangeInterceptor BEAN for switch to a new locale based on the value of the 'lang' parameter
	 * appended to a request
	 */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    /**
     * Add the LocaleChangeInterceptor BEAN to the application's interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * Provides a MessageSource BEAN used to translate messages in application.<BR>
     * According to the application property 'auto.reload.properties' this implementation may be configured
     * for reloading changes in runtime in 'messages' properties files, what is usefull for development environment.
     */
	@Bean
	public MessageSource messageSource() {
	    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
	    messageSource.setDefaultEncoding("UTF-8");
	    messageSource.setBasename("classpath:messages");
	    String auto_reload_props = env.getProperty("auto.reload.properties");
	    if (auto_reload_props!=null && auto_reload_props.trim().length()>0 && !"0".equals(auto_reload_props.trim())) {
	    	int seconds = Integer.parseInt(auto_reload_props.trim());
	    	if (seconds>0)
	    		messageSource.setCacheSeconds(seconds); //reload messages every X seconds
	    }
	    return messageSource;
	}
	
	/**
	 * Make static assets cacheable (such as JS and CSS files)
	 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/components/**") 
                .addResourceLocations("/components/") 
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .resourceChain(true)
                .addResolver(new EncodedResourceResolver())
                .addResolver(new PathResourceResolver());
        registry.addResourceHandler("/static/images/**") 
		        .addResourceLocations("/images/") 
		        .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .resourceChain(true)
                .addResolver(new EncodedResourceResolver())
                .addResolver(new PathResourceResolver());
        registry.addResourceHandler("/static/js/**") 
		        .addResourceLocations("/js/") 
		        .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .resourceChain(true)
                .addResolver(new EncodedResourceResolver())
                .addResolver(new PathResourceResolver());
        registry.addResourceHandler("/static/themes/**") 
		        .addResourceLocations("/themes/") 
		        .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
		        .resourceChain(true)
		        .addResolver(new EncodedResourceResolver())
		        .addResolver(new PathResourceResolver());
        registry.addResourceHandler("/static/css/**") 
		        .addResourceLocations("/css/") 
		        .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .resourceChain(true)
                .addResolver(new EncodedResourceResolver())
                .addResolver(new PathResourceResolver());
    }

    /**
     * Provides a ThreadPoolTaskExecutor BEAN for configuration of multi threaded
     * tasks implemented in the application.
     */
    @Bean
    public ThreadPoolTaskExecutor mvcTaskExecutor() {
      ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
      taskExecutor.setCorePoolSize(10);
      taskExecutor.setMaxPoolSize(200); 
      taskExecutor.setQueueCapacity(50);
      taskExecutor.setAllowCoreThreadTimeOut(true);
      taskExecutor.setKeepAliveSeconds(120);
      return taskExecutor;
    }

    /**
     * Add the ThreadPoolTaskExecutor BEAN to the asynchronous request processing
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
      configurer.setTaskExecutor(mvcTaskExecutor());
      configurer.setDefaultTimeout(DEFAULT_ASYNC_TIMEOUT);
    }

    /**
     * Customize the object used by Spring for converting objects into JSON format<BR>
     * We need to do this for representing some enumerations by their corresponding localized messages, not by their
     * internal hardcoded constant names.
     */
    @Bean
    @Primary
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
        	builder.serializerByType(DocumentSituation.class, new JsonSerializer<DocumentSituation>() {

				@Override
				public void serialize(DocumentSituation value, JsonGenerator gen, SerializerProvider serializers)
						throws IOException {
					if (value==null)
						gen.writeNull();
					else {
						String key = value.toString();
						try {
							MessageSource messageSource = app.getBean(MessageSource.class);
							String translated = messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
							gen.writeString(translated);
						}
						catch (Throwable ex) {
							gen.writeString(value.name());
						}
					}
				}
        		
        	});
        };
    }
}
