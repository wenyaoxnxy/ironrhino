package org.ironrhino.core.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.ironrhino.core.struts.MyFreemarkerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.inject.Container;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Component
public class TemplateProvider {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${view.ftl.location:" + MyFreemarkerManager.DEFAULT_FTL_LOCATION + "}")
	private String ftlLocation = MyFreemarkerManager.DEFAULT_FTL_LOCATION;

	@Value("${view.ftl.classpath:" + MyFreemarkerManager.DEFAULT_FTL_CLASSPATH + "}")
	private String ftlClasspath = MyFreemarkerManager.DEFAULT_FTL_CLASSPATH;

	@Value("${base:}")
	private String base;

	@Value("${assetsBase:}")
	private String assetsBase;

	@Value("${ssoServerBase:}")
	private String ssoServerBase;

	private Configuration configuration;

	public String getBase() {
		return base != null ? base : "";
	}

	public String getAssetsBase() {
		return assetsBase != null ? assetsBase : "";
	}

	public String getSsoServerBase() {
		return ssoServerBase != null ? ssoServerBase : "";
	}

	public String getFtlLocation() {
		return org.ironrhino.core.util.StringUtils.trimTailSlash(ftlLocation);
	}

	public String getFtlClasspath() {
		return org.ironrhino.core.util.StringUtils.trimTailSlash(ftlClasspath);
	}

	public Map<String, String> getAllSharedVariables() {
		Map<String, String> allSharedVariables = new HashMap<>(8);
		if (StringUtils.isNotBlank(base))
			allSharedVariables.put("base", base);
		if (StringUtils.isNotBlank(assetsBase))
			allSharedVariables.put("assetsBase", assetsBase);
		if (StringUtils.isNotBlank(ssoServerBase))
			allSharedVariables.put("ssoServerBase", ssoServerBase);
		return allSharedVariables;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		if (StringUtils.isNotBlank(base))
			base = org.ironrhino.core.util.StringUtils.trimTailSlash(base);
		if (StringUtils.isNotBlank(assetsBase))
			assetsBase = org.ironrhino.core.util.StringUtils.trimTailSlash(assetsBase);
		if (StringUtils.isNotBlank(ssoServerBase))
			ssoServerBase = org.ironrhino.core.util.StringUtils.trimTailSlash(ssoServerBase);
	}

	private Configuration getConfiguration() {
		if (configuration == null) {
			try {
				Container con = ActionContext.getContext().getContainer();
				FreemarkerManager freemarkerManager = con
						.getInstance(org.apache.struts2.views.freemarker.FreemarkerManager.class);
				configuration = freemarkerManager.getConfiguration(ServletActionContext.getServletContext());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return configuration;
	}

	public Template getTemplate(String name) throws IOException {
		Locale loc = getConfiguration().getLocale();
		return getTemplate(name, loc, getConfiguration().getEncoding(loc), true);
	}

	public Template getTemplate(String name, Locale locale, String encoding) throws IOException {
		return getTemplate(name, locale, encoding, true);
	}

	public Template getTemplate(String name, Locale locale) throws IOException {
		return getTemplate(name, locale, getConfiguration().getEncoding(locale), true);
	}

	public Template getTemplate(String name, String encoding) throws IOException {
		return getTemplate(name, getConfiguration().getLocale(), encoding, true);
	}

	public Template getTemplate(String name, Locale locale, String encoding, boolean parse) throws IOException {
		if (name.startsWith(ftlLocation) || name.startsWith(ftlClasspath))
			return getConfiguration().getTemplate(name, locale, encoding, parse);
		String templateName = ftlLocation + (name.indexOf('/') != 0 ? "/" : "") + name;
		Template t = null;
		try {
			t = getConfiguration().getTemplate(templateName, locale, encoding, parse);
		} catch (FileNotFoundException e) {
			if (t == null) {
				templateName = ftlClasspath + (name.indexOf('/') != 0 ? "/" : "") + name;
				t = getConfiguration().getTemplate(templateName, locale, encoding, parse);
			}
		}
		return t;
	}

}
