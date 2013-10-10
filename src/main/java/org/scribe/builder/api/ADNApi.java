package org.scribe.builder.api;

import org.scribe.model.Verb;
import org.scribe.model.OAuthConfig;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.extractors.AccessTokenExtractor;

/*
 * Note that scope must be passed by the end user making use of this builder to
 * define the permissions to request.
 */
public class ADNApi extends DefaultApi20
{
	private static final String AUTHORIZE_URL = "https://account.app.net/oauth/authenticate?client_id=%s&response_type=token&redirect_uri=%s&scope=%s";
	private static final String ACCESS_TOKEN_RESOURCE = "https://account.app.net/oauth/access_token";

	@Override
	public String getAccessTokenEndpoint()
	{
		return ACCESS_TOKEN_RESOURCE;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config)
	{
		return String.format(AUTHORIZE_URL, config.getApiKey(), config.getCallback(), config.getScope());
	}

	@Override
	public AccessTokenExtractor getAccessTokenExtractor()
	{
		return new JsonTokenExtractor();
	}

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.POST;
	}
}
