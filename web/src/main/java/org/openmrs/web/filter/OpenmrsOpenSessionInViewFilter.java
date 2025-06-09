package org.openmrs.web.filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.openmrs.web.WebConstants;
import org.springframework.orm.hibernate5.support.OpenSessionInViewFilter;

public class OpenmrsOpenSessionInViewFilter extends OpenSessionInViewFilter
{
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws
		ServletException, IOException {
		String path = request.getServletPath();
		if (path.equals("/" + WebConstants.LIVENESS_URL) || path.equals("/" + WebConstants.READINESS_URL)) {
			filterChain.doFilter(request, response);
		} else {
			super.doFilterInternal(request, response, filterChain);
		}
	}
}
