package com.tagokoder.ops.infra.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

public class InternalTokenFilter extends GenericFilter {

  private final String expected;

  public InternalTokenFilter(String expected) {
    this.expected = expected;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String token = req.getHeader("X-Internal-Token");

    if (token != null && token.equals(expected)) {
      var auth = new UsernamePasswordAuthenticationToken("internal", null, List.of());
      SecurityContextHolder.getContext().setAuthentication(auth);
      chain.doFilter(request, response);
      return;
    }
    ((HttpServletResponse) response).setStatus(401);
  }
}
