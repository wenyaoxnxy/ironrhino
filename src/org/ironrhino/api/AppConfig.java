package org.ironrhino.api;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ironrhino.core.servlet.HttpErrorHandler;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.fasterxml.jackson.databind.ObjectMapper;

//only for exclude-filter of root ctx
@ControllerAdvice
@Configuration
@ComponentScan(basePackages = "org.ironrhino.api", excludeFilters = @Filter(value = HttpErrorHandler.class, type = FilterType.ASSIGNABLE_TYPE))
@EnableAspectJAutoProxy
public class AppConfig extends WebMvcConfigurationSupport {

	@Override
	protected void configureContentNegotiation(
			ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
		configurer.favorPathExtension(false);
		configurer.ignoreAcceptHeader(true);
	}

	@Override
	protected void configureMessageConverters(
			List<HttpMessageConverter<?>> converters) {
		MappingJackson2HttpMessageConverter jackson2 = new MappingJackson2HttpMessageConverter();
		ObjectMapper objectMapper = JsonUtils.createNewObjectMapper();
		objectMapper
				.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
		jackson2.setObjectMapper(objectMapper);
		converters.add(jackson2);
	}

	@Override
	protected Map<String, MediaType> getDefaultMediaTypes() {
		Map<String, MediaType> map = new HashMap<String, MediaType>();
		map.put("json", MediaType.APPLICATION_JSON);
		return map;
	}

}