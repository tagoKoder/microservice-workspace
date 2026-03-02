package com.tagokoder.identity.infra.security.authz;

public enum AuthzMode {
  PUBLIC,       // no token, no AVP
  AUTHN_ONLY,   // token OK, no AVP
  AUTHZ         // token OK + AVP
}