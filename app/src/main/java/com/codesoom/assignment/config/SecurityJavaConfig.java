package com.codesoom.assignment.config;

import com.codesoom.assignment.application.AuthenticationService;
import com.codesoom.assignment.filters.AuthenticationErrorFilter;
import com.codesoom.assignment.filters.JwtAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import javax.servlet.Filter;

@Configuration
public class SecurityJavaConfig extends WebSecurityConfigurerAdapter {

	private final AuthenticationService authenticationService;

	public SecurityJavaConfig(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		Filter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager(), authenticationService);

		Filter authenticationErrorFilter = new AuthenticationErrorFilter();
		http
				.csrf().disable()
				.addFilter(jwtAuthenticationFilter)
				.addFilterBefore(authenticationErrorFilter, JwtAuthenticationFilter.class);
	}
}